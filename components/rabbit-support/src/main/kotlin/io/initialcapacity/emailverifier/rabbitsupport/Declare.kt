package io.initialcapacity.emailverifier.rabbitsupport

import com.rabbitmq.client.Channel
import com.rabbitmq.client.ConnectionFactory
import java.net.URI

/**
 * Builds and configures a new [ConnectionFactory] instance using the specified [rabbitUrl].
 * The [rabbitUrl] should be a valid URI representing the RabbitMQ connection URL.
 * Returns the configured [ConnectionFactory] instance.
 *
 * @param rabbitUrl The RabbitMQ connection URL as a valid URI.
 * @return The configured [ConnectionFactory] instance.
 */
fun buildConnectionFactory(rabbitUrl: URI): ConnectionFactory =
    ConnectionFactory().apply {
        setUri(rabbitUrl)
    }

/**
 * Declares and binds the specified [exchange] and [queue] using the current [ConnectionFactory].
 * The [exchange] represents the RabbitMQ exchange configuration, and the [queue] represents the RabbitMQ queue configuration.
 *
 * @param exchange The RabbitMQ exchange configuration.
 * @param queue The RabbitMQ queue configuration.
 */
fun ConnectionFactory.declareAndBind(exchange: RabbitExchange, queue: RabbitQueue): Unit =
    useChannel {
        // Declare the exchange
        it.exchangeDeclare(exchange.name, exchange.type, false, false, null)
        // Declare the queue
        it.queueDeclare(queue.name, false, false, false, null)
        // Bind the queue to the exchange using the binding key
        it.queueBind(queue.name, exchange.name, exchange.bindingKey)
    }

/**
 * Executes the specified [block] with a new channel obtained from a new connection created by this [ConnectionFactory].
 * The channel is automatically closed after executing the block.
 * Returns the result of the [block] function.
 *
 * @param block The code block to be executed with the channel.
 * @return The result of the [block] function.
 */
fun <T> ConnectionFactory.useChannel(block: (Channel) -> T): T =
    newConnection().use { connection ->
        connection.createChannel()!!.use(block)
    }
