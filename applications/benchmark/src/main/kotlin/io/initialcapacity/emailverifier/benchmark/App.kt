package io.initialcapacity.emailverifier.benchmark

import io.initialcapacity.emailverifier.fakesendgridendpoints.fakeSendgridRoutes
import io.ktor.client.*
import io.ktor.client.engine.java.*
import io.ktor.server.engine.*
import io.ktor.server.jetty.*
import kotlinx.coroutines.runBlocking
import java.util.*

/**
 * Data class representing a confirmation.
 *
 * @property email The email associated with the confirmation.
 * @property code The confirmation code.
 */
data class Confirmation(
    val email: String,
    val code: UUID,
)

/**
 * The main function that runs the benchmark.
 */
fun main(): Unit = runBlocking {
    // Retrieve the port from the environment variable "PORT" or use the default value 9090
    val port = getEnvInt("PORT", 9090)

    // Create a new benchmark instance with the specified configuration
    val benchmark = Benchmark(
        registrationUrl = System.getenv("REGISTRATION_URL") ?: "http://localhost:8081",
        registrationCount = getEnvInt("REGISTRATION_COUNT", 5_000),
        requestWorkerCount = getEnvInt("REQUEST_WORKER_COUNT", 4),
        registrationWorkerCount = getEnvInt("REGISTRATION_WORKER_COUNT", 4),
        client = HttpClient(Java) {
            expectSuccess = false
        }
    )

    // Start the fake email server on the specified port and bind it to the benchmark instance
    val fakeEmailServer = fakeEmailServer(port, benchmark).apply { start() }

    // Start the benchmark
    benchmark.start(this)

    // Stop the fake email server
    fakeEmailServer.stop()
}

/**
 * Retrieves the value of an environment variable as an integer or returns a default value if the variable is not set or cannot be parsed as an integer.
 *
 * @param name The name of the environment variable.
 * @param default The default value to return if the environment variable is not set or cannot be parsed as an integer.
 * @return The value of the environment variable as an integer, or the default value if the variable is not set or cannot be parsed.
 */
private fun getEnvInt(name: String, default: Int): Int = System.getenv(name)?.toInt() ?: default

/**
 * Starts a fake email server using an embedded server.
 *
 * @param port The port number for the server to listen on.
 * @param benchmark The benchmark instance to handle processed confirmations.
 * @return An instance of the embedded server.
 */
private fun fakeEmailServer(
    port: Int,
    benchmark: Benchmark
) = embeddedServer(
    factory = Jetty,
    port = port,
    module = { fakeSendgridRoutes("super-secret") { benchmark.processConfirmation(it) } }
)
