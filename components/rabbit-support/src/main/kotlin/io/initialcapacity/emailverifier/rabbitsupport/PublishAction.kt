package io.initialcapacity.emailverifier.rabbitsupport

import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.MessageProperties

/**
 * Type-alias for a function that publishes a message to RabbitMQ.
 * It takes a string message as input and does not return any value.
 */
typealias PublishAction = (String) -> Unit

/**
 * Represents a RabbitMQ exchange configuration.
 * It contains properties such as the exchange name, type, routing key generator, and binding key.
 *
 * @param name The name of the exchange.
 * @param type The type of the exchange.
 * @param routingKeyGenerator The function to generate the routing key based on the message.
 * @param bindingKey The binding key to bind the exchange with the queue.
 */
data class RabbitExchange(
    val name: String,
    val type: String,
    val routingKeyGenerator: (String) -> String,
    val bindingKey: String,
)

/**
 * Publishes a message to RabbitMQ using the specified [factory] and [exchange].
 *
 * @param factory The ConnectionFactory instance used to create a channel and publish the message.
 * @param exchange The RabbitMQ exchange configuration to publish the message to.
 * @return A PublishAction function that can be used to publish messages.
 */
fun publish(factory: ConnectionFactory, exchange: RabbitExchange): PublishAction = fun(message: String) =
    factory.useChannel { channel ->
        // Publish the message to the exchange
        channel.basicPublish(exchange.name, exchange.routingKeyGenerator(message), MessageProperties.PERSISTENT_BASIC, message.toByteArray())
    }
