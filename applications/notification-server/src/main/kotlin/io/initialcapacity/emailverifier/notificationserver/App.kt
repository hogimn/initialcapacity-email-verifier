package io.initialcapacity.emailverifier.notificationserver

import com.rabbitmq.client.ConnectionFactory
import io.initialcapacity.emailverifier.notification.Emailer
import io.initialcapacity.emailverifier.notification.NotificationDataGateway
import io.initialcapacity.emailverifier.notification.Notifier
import io.initialcapacity.emailverifier.rabbitsupport.*
import io.initialcapacity.serializationsupport.UUIDSerializer
import io.ktor.client.*
import io.ktor.client.engine.java.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.URL
import java.util.*

/**
 * The main application class.
 */
class App

/**
 * The logger for the application.
 */
private val logger = LoggerFactory.getLogger(App::class.java)

/**
 * Entry point of the application.
 */
fun main() = runBlocking {
    // Get the RabbitMQ URL from the environment variable
    val rabbitUrl = System.getenv("RABBIT_URL")?.let(::URI)
        ?: throw RuntimeException("Please set the RABBIT_URL environment variable")
    // Get the SendGrid URL from the environment variable
    val sendgridUrl = System.getenv("SENDGRID_URL")?.let(::URL)
        ?: throw RuntimeException("Please set the SENDGRID_URL environment variable")
    // Get the SendGrid API key from the environment variable
    val sendgridApiKey = System.getenv("SENDGRID_API_KEY")
        ?: throw RuntimeException("Please set the SENDGRID_API_KEY environment variable")
    // Get the 'from' email address from the environment variable
    val fromAddress = System.getenv("FROM_ADDRESS")
        ?: throw RuntimeException("Please set the FROM_ADDRESS environment variable")
    // Get the database URL from the environment variable
    val databaseUrl = System.getenv("DATABASE_URL")
        ?: throw RuntimeException("Please set the DATABASE_URL environment variable")
    // Build the connection factory for RabbitMQ using the RabbitMQ URL
    val connectionFactory = buildConnectionFactory(rabbitUrl)

    // Create a DatabaseConfiguration instance using the database URL
    val dbConfig = DatabaseConfiguration(databaseUrl)

    // Start the process of listening for registration notifications
    start(
        // Passes the SendGrid URL to the start function
        sendgridUrl = sendgridUrl,
        // Passes the SendGrid API key to the start function
        sendgridApiKey = sendgridApiKey,
        // Passes the 'from' email address to the start function
        fromAddress = fromAddress,
        // Passes the RabbitMQ connection factory to the start function
        connectionFactory = connectionFactory,
        registrationNotificationExchange = RabbitExchange(
            // Sets the name of the registration notification exchange
            name = "registration-notification-exchange",
            // Sets the type of the registration notification exchange as 'direct'
            type = "direct",
            // Specifies a routing key generator function that always returns "42
            routingKeyGenerator = { _: String -> "42" },
            // Sets the binding key for the registration notification exchange as "42"
            bindingKey = "42",
        ),
        // Creates a RabbitMQ queue for registration notifications
        registrationNotificationQueue = RabbitQueue("registration-notification"),
        // Passes the database configuration to the start function
        dbConfig = dbConfig,
    )
}

/**
 * Starts the registration notification process.
 * It listens for registration notification requests and sends notifications using the specified configurations.
 *
 * @param sendgridUrl The URL for the SendGrid service.
 * @param sendgridApiKey The API key for accessing the SendGrid service.
 * @param fromAddress The email address from which notifications will be sent.
 * @param connectionFactory The connection factory for creating RabbitMQ connections.
 * @param registrationNotificationExchange The exchange used for registration notifications.
 * @param registrationNotificationQueue The queue used for registration notifications.
 * @param dbConfig The configuration for the database.
 */
suspend fun start(
    sendgridUrl: URL,
    sendgridApiKey: String,
    fromAddress: String,
    connectionFactory: ConnectionFactory,
    registrationNotificationExchange: RabbitExchange,
    registrationNotificationQueue: RabbitQueue,
    dbConfig: DatabaseConfiguration,
) {
    // Create a notifier using the specified configurations
    val notifier = createNotifier(sendgridUrl, sendgridApiKey, fromAddress, dbConfig)
    // Declare and bind the registrationNotificationExchange to the registrationNotificationQueue
    connectionFactory.declareAndBind(exchange = registrationNotificationExchange, queue = registrationNotificationQueue)

    // Log that we are listening for registration notifications
    logger.info("listening for registration notifications")
    // Start listening for notification requests
    listenForNotificationRequests(connectionFactory, notifier, registrationNotificationQueue)
}

/**
 * Creates a Notifier instance with the specified configurations.
 *
 * @param sendgridUrl The URL for the SendGrid service.
 * @param sendgridApiKey The API key for accessing the SendGrid service.
 * @param fromAddress The email address from which notifications will be sent.
 * @param dbConfig The configuration for the database.
 * @return A new instance of the Notifier with the created dependencies.
 */
private fun createNotifier(
    sendgridUrl: URL,
    sendgridApiKey: String,
    fromAddress: String,
    dbConfig: DatabaseConfiguration,
): Notifier {
    // Create an instance of the Emailer with the specified configurations
    val emailer = Emailer(
        client = HttpClient(Java) { expectSuccess = false },
        sendgridUrl = sendgridUrl,
        sendgridApiKey = sendgridApiKey,
        fromAddress = fromAddress,
    )
    // Create an instance of the NotificationDataGateway using the database configuration
    val gateway = NotificationDataGateway(dbConfig.db)
    // Return a new instance of the Notifier with the created dependencies
    return Notifier(gateway, emailer)
}

/**
 * Listens for registration notification requests and notifies the notifier accordingly.
 *
 * @param connectionFactory The connection factory for creating RabbitMQ connections.
 * @param notifier The notifier instance to handle the notifications.
 * @param registrationNotificationQueue The queue used for registration notifications.
 */
private suspend fun listenForNotificationRequests(
    connectionFactory: ConnectionFactory,
    notifier: Notifier,
    registrationNotificationQueue: RabbitQueue
) {
    // Create a new channel using the connection factory
    val channel = connectionFactory.newConnection().createChannel()

    // Listen to the registrationNotificationQueue
    listen(queue = registrationNotificationQueue, channel = channel) {
        // Deserialize the message from JSON to NotificationMessage object
        val message = Json.decodeFromString<NotificationMessage>(it)

        // Log the received registration notification
        logger.debug("received registration notification {}", message)
        // Notify the notifier with the email and confirmation code from the message
        notifier.notify(message.email, message.confirmationCode)
    }
}

/**
 * Data class representing a notification message.
 *
 * @property email The email address to which the notification should be sent.
 * @property confirmationCode The confirmation code associated with the notification.
 */
@Serializable
private data class NotificationMessage(
    val email: String,
    @Serializable(with = UUIDSerializer::class)
    val confirmationCode: UUID,
)
