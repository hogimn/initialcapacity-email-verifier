package io.initialcapacity.emailverifier.fakesendgrid

import io.ktor.server.engine.embeddedServer
import io.ktor.server.jetty.Jetty
import kotlinx.coroutines.runBlocking
import io.initialcapacity.emailverifier.fakesendgridendpoints.fakeSendgridRoutes
import org.slf4j.LoggerFactory

class App

private val logger = LoggerFactory.getLogger(App::class.java)

/**
 * Entry point of the application.
 * Starts the embedded server using Jetty and listens on the specified port.
 */
fun main(): Unit = runBlocking {
    // Get the port from the environment variable or use the default value 9090
    val port = System.getenv("PORT")?.toInt() ?: 9090

    // Log a message indicating that the application is waiting for mail
    logger.info("waiting for mail")

    // Start the embedded server using Jetty
    embeddedServer(
        factory = Jetty,
        port = port,
        // Pass the authentication token to the fakeSendgridRoutes
        module = { fakeSendgridRoutes("super-secret") }
    ).start()
}
