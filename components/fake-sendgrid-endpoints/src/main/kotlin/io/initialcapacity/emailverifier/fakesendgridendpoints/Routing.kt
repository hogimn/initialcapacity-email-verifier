package io.initialcapacity.emailverifier.fakesendgridendpoints

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory

/**
 * A fake implementation of the SendGrid API.
 * This class provides a simulated behavior of the SendGrid API functionality.
 */
class FakeSendgrid

/**
 * Create a logger for logging events related to the fake SendGrid functionality.
 */
private val logger = LoggerFactory.getLogger(FakeSendgrid::class.java)

/**
 * Sets up fake routes to simulate the behavior of the SendGrid API.
 * The routes include a root path handler that responds with "Fake Sendgrid" and a handler for the "/v3/mail/send" path
 * that simulates sending emails by invoking the provided [mailCallback] function with the request body.
 * The function also performs authorization and content type checks based on the provided [authToken].
 *
 * @param authToken The authorization token used for authentication.
 * @param mailCallback The callback function that is invoked with the request body when an email is "sent".
 */
fun Application.fakeSendgridRoutes(authToken: String, mailCallback: suspend (String) -> Unit =  {}) {
    routing {
        // Handler for the root path
        get("/") { call.respond("Fake Sendgrid") }
        // Handler for the /v3/mail/send path
        post("/v3/mail/send") {
            val headers = call.request.headers
            // Check if the authorization token is correct
            if (headers["Authorization"] != "Bearer $authToken") {
                return@post call.respond(HttpStatusCode.Unauthorized)
            }
            // Check if the content type is correct
            if (headers["Content-Type"]?.lowercase() != "application/json; charset=utf-8") {
                return@post call.respond(HttpStatusCode.BadRequest)
            }

            // Receive the request body
            val body = call.receive<String>()
            // Invoke the mailCallback function with the request body
            mailCallback(body)
            // Log the sent email
            logger.debug("email sent {}", body)

            // Respond with a success status code
            call.respond(HttpStatusCode.Created)
        }
    }
}
