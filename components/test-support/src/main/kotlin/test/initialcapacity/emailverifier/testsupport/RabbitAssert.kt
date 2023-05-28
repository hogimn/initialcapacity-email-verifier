package test.initialcapacity.emailverifier.testsupport

import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.GetResponse
import kotlinx.coroutines.delay
import io.initialcapacity.emailverifier.rabbitsupport.RabbitQueue
import kotlin.test.fail
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Suspends the execution until a message is received from the specified RabbitMQ queue
 * and asserts that the received message matches the expected message.
 *
 * @param queue The RabbitMQ queue to receive the message from.
 * @param message The expected message to be received.
 * @param timeout The timeout duration to wait for the message (default: 50 milliseconds).
 */
suspend fun ConnectionFactory.assertMessageReceived(
    queue: RabbitQueue,
    message: String,
    timeout: Duration = 50.milliseconds
) {
    newConnection().use { connection ->
        connection.createChannel()!!.use { channel ->
            var received: GetResponse? = null
            var elapsed = Duration.ZERO
            val delayDuration = 10.milliseconds

            while (received == null) {
                if (elapsed >= timeout) {
                    fail("No messages received")
                }
                delay(delayDuration)
                elapsed += delayDuration

                received = channel.basicGet(queue.name, true)
            }

            assertJsonEquals(received.body.decodeToString(), message)
        }
    }
}
