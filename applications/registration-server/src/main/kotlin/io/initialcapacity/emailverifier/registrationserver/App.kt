package io.initialcapacity.emailverifier.registrationserver

import com.rabbitmq.client.ConnectionFactory
import io.initialcapacity.emailverifier.rabbitsupport.*
import io.initialcapacity.emailverifier.registration.RegistrationConfirmationService
import io.initialcapacity.emailverifier.registration.RegistrationDataGateway
import io.initialcapacity.emailverifier.registration.register
import io.initialcapacity.emailverifier.registrationrequest.RegistrationRequestDataGateway
import io.initialcapacity.emailverifier.registrationrequest.RegistrationRequestService
import io.initialcapacity.emailverifier.registrationrequest.UuidProvider
import io.initialcapacity.emailverifier.registrationrequest.registrationRequest
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.jetty.*
import io.ktor.server.plugins.autohead.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.resources.*
import io.ktor.server.routing.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.*

/**
 * Main application class for the registration system.
 */
class App

// Create a logger instance for the App class
private val logger = LoggerFactory.getLogger(App::class.java)

/**
 * Entry point of the application.
 */
fun main(): Unit = runBlocking {
    // Read the port from the environment variable or use a default value of 8081
    val port = System.getenv("PORT")?.toInt() ?: 8081
    // Read the RabbitMQ URL from the environment variable
    val rabbitUrl = System.getenv("RABBIT_URL")?.let(::URI)
        ?: throw RuntimeException("Please set the RABBIT_URL environment variable")
    // Read the database URL from the environment variable
    val databaseUrl = System.getenv("DATABASE_URL")
        ?: throw RuntimeException("Please set the DATABASE_URL environment variable")

    // Configure the database
    val dbConfig = DatabaseConfiguration(databaseUrl)

    // Build the connection factory for RabbitMQ
    val connectionFactory = buildConnectionFactory(rabbitUrl)
    // Create the data gateways for registration requests and registrations
    val registrationRequestGateway = RegistrationRequestDataGateway(dbConfig.db)
    val registrationGateway = RegistrationDataGateway(dbConfig.db)

    // Define the registration notification exchange
    val registrationNotificationExchange = RabbitExchange(
        name = "registration-notification-exchange",
        type = "direct",
        routingKeyGenerator = { _: String -> "42" },
        bindingKey = "42",
    )
    // Define the registration notification queue
    val registrationNotificationQueue = RabbitQueue("registration-notification")
    // Define the registration request exchange
    val registrationRequestExchange = RabbitExchange(
        // TODO - rename the request exchange (since you've already declared a direct exchange under the current name)
        name = "registration-request-consistent-hash-exchange",
        // TODO - use a consistent hash exchange (x-consistent-hash)
        type = "x-consistent-hash",
        // TODO - calculate a routing key based on message content
        routingKeyGenerator = { message: String -> message.hashCode().toString() },
        // TODO - read the binding key from the environment
        bindingKey = System.getenv("BINDING_KEY") ?: ""
    )

    // Define the registration request queue
    // TODO - read the queue name from the environment
    val registrationRequestQueue = RabbitQueue("registration-request")

    // Declare and bind the exchanges and queues
    connectionFactory.declareAndBind(exchange = registrationNotificationExchange, queue = registrationNotificationQueue)
    connectionFactory.declareAndBind(exchange = registrationRequestExchange, queue = registrationRequestQueue)

    // Start listening for registration requests
    listenForRegistrationRequests(
        connectionFactory,
        registrationRequestGateway,
        registrationNotificationExchange,
        registrationRequestQueue
    )
    // Start the registration server
    registrationServer(
        port,
        registrationRequestGateway,
        registrationGateway,
        connectionFactory,
        registrationRequestExchange
    ).start()
}

/**
 * Creates and configures the registration server.
 *
 * @param port The port number for the server to listen on.
 * @param registrationRequestGateway The data gateway for registration requests.
 * @param registrationGateway The data gateway for registrations.
 * @param connectionFactory The RabbitMQ connection factory.
 * @param registrationRequestExchange The RabbitMQ exchange for registration requests.
 * @return The configured registration server.
 */
fun registrationServer(
    port: Int,
    registrationRequestGateway: RegistrationRequestDataGateway,
    registrationGateway: RegistrationDataGateway,
    connectionFactory: ConnectionFactory,
    registrationRequestExchange: RabbitExchange,
) = embeddedServer(
    factory = Jetty,
    port = port,
    // Configure the server module using the provided dependencies
    module = { module(registrationRequestGateway, registrationGateway, connectionFactory, registrationRequestExchange) }
)

/**
 * Configures the application module.
 *
 * @param registrationRequestGateway The data gateway for registration requests.
 * @param registrationGateway The data gateway for registrations.
 * @param connectionFactory The RabbitMQ connection factory.
 * @param registrationRequestExchange The RabbitMQ exchange for registration requests.
 */
fun Application.module(
    registrationRequestGateway: RegistrationRequestDataGateway,
    registrationGateway: RegistrationDataGateway,
    connectionFactory: ConnectionFactory,
    registrationRequestExchange: RabbitExchange,
) {
    // Install the Resources feature to serve static resources
    install(Resources)
    // Install the CallLogging feature for logging HTTP requests and responses
    install(CallLogging)
    // Install the AutoHeadResponse feature to automatically handle HEAD requests
    install(AutoHeadResponse)
    // Install the ContentNegotiation feature to handle content negotiation
    install(ContentNegotiation) {
        json()
    }

    // Define the publishRequest action for publishing registration requests
    val publishRequest = publish(connectionFactory, registrationRequestExchange)

    // Configure the routing and handlers for the application
    install(Routing) {
        // Register the info endpoint
        info()
        // Register the registration request endpoint and provide the publishRequest action
        registrationRequest(publishRequest)
        // Register the RegistrationConfirmationService for handling registration confirmations
        register(RegistrationConfirmationService(registrationRequestGateway, registrationGateway))
    }
}

/**
 * Sets up a coroutine to listen for registration requests and process them.
 *
 * @param connectionFactory The RabbitMQ connection factory.
 * @param registrationRequestDataGateway The data gateway for registration requests.
 * @param registrationNotificationExchange The RabbitMQ exchange for registration notifications.
 * @param registrationRequestQueue The RabbitMQ queue for registration requests.
 * @param uuidProvider The UUID provider function. (Optional, default: generates a random UUID)
 */
fun CoroutineScope.listenForRegistrationRequests(
    connectionFactory: ConnectionFactory,
    registrationRequestDataGateway: RegistrationRequestDataGateway,
    registrationNotificationExchange: RabbitExchange,
    registrationRequestQueue: RabbitQueue,
    uuidProvider: UuidProvider = { UUID.randomUUID() },
) {
    // Create a function to publish notifications using the provided connection factory and registration notification exchange
    val publishNotification = publish(connectionFactory, registrationNotificationExchange)

    // Create an instance of RegistrationRequestService with the provided gateway, publishNotification function, and uuidProvider
    val registrationRequestService = RegistrationRequestService(
        gateway = registrationRequestDataGateway,
        publishNotification = publishNotification,
        uuidProvider = uuidProvider,
    )

    // Launch a coroutine to listen for registration requests
    launch {
        logger.info("listening for registration requests")
        // Create a new channel using the connection factory
        val channel = connectionFactory.newConnection().createChannel()
        // Start listening to the registration request queue and process incoming requests
        listen(queue = registrationRequestQueue, channel = channel) { email ->
            logger.debug("received registration request for {}", email)
            // Generate a code and publish the registration request
            registrationRequestService.generateCodeAndPublish(email)
        }
    }
}
