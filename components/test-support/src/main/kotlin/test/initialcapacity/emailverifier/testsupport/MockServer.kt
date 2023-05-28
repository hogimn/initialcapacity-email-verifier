package test.initialcapacity.emailverifier.testsupport

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.engine.*
import io.ktor.server.jetty.*
import io.ktor.server.plugins.doublereceive.*
import kotlinx.coroutines.delay
import kotlin.test.fail
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Mock server for testing HTTP requests and capturing request bodies.
 *
 * @param port The port number on which the server should listen.
 * @param module The module defining the server behavior.
 */
class MockServer(
    port: Int,
    module: Application.() -> Unit
) {
    /**
     * List to store the received request bodies
     */
    private val calls = mutableListOf<String>()

    /**
     * Create an embedded server using Jetty
     */
    private val server = embeddedServer(
        factory = Jetty,
        port = port,
        module = {
            // Install DoubleReceive feature to enable receiving request bodies multiple times
            install(DoubleReceive)
            // Invoke the provided module to define the server behavior
            module()
            // Intercept the monitoring phase of the application call pipeline
            intercept(ApplicationCallPipeline.Monitoring) {
                // Add the received request body to the list of calls
                calls.add(context.request.call.receiveText())
            }
        }
    )

    /**
     * Starts the mock server.
     */
    fun start() = server.start(wait = false)

    /**
     * Stops the mock server.
     */
    fun stop() = server.stop(50, 50)

    /**
     * Retrieves the body of the last received call.
     *
     * @return The body of the last received call.
     * @throws AssertionError if no calls have been received.
     */
    fun lastCallBody() = calls.lastOrNull() ?: fail("No calls received")

    /**
     * Waits for a call to be received, up to the specified timeout duration.
     *
     * @param timeout The maximum duration to wait for a call.
     * @return The body of the received call.
     * @throws AssertionError if no calls are received within the timeout duration.
     */
    suspend fun waitForCall(timeout: Duration = 200.milliseconds): String {
        var call = calls.lastOrNull()
        var elapsed = Duration.ZERO

        while (call == null) {
            if (elapsed >= timeout) {
                fail("No calls received")
            }

            val delayDuration = 50.milliseconds
            delay(delayDuration)
            elapsed += delayDuration

            call = calls.lastOrNull()
        }

        return call
    }
}
