package io.initialcapacity.emailverifier.rabbitsupport

import com.rabbitmq.client.Channel
import com.rabbitmq.client.Delivery
import kotlinx.coroutines.runBlocking

/**
 * Represents a RabbitMQ queue.
 *
 * @property name The name of the RabbitMQ queue.
 */
data class RabbitQueue(val name: String)

/**
 * Start listening for messages on the specified RabbitMQ channel and queue.
 *
 * @param channel The RabbitMQ channel to listen on.
 * @param queue The RabbitMQ queue to listen on.
 * @param handler The handler function to be invoked with the message content when a new message is received.
 * @return The consumer tag associated with the consumer.
 */
fun listen(channel: Channel, queue: RabbitQueue, handler: suspend (String) -> Unit): String {
    // Define the delivery callback function
    val delivery = { _: String, message: Delivery -> runBlocking { handler(message.body.decodeToString()) } }
    // Define the cancel callback function
    val cancel = { _: String -> }

    // Start consuming messages from the specified queue using the delivery and cancel callbacks
    return channel.basicConsume(queue.name, true, delivery, cancel)
}
