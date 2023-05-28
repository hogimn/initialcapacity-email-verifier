package io.initialcapacity.emailverifier.registration

import io.initialcapacity.serializationsupport.UUIDSerializer
import io.ktor.http.*
import io.ktor.resources.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.resources.post
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory
import java.util.*

/**
 * Represents a registration event or action.
 */
class Register

/**
 * The logger instance for the Register class.
 * It is used to log messages and events related to the Register class.
 */
private val logger = LoggerFactory.getLogger(Register::class.java)

/**
 * Defines a serializable class for the registration path.
 * This class represents the registration endpoint ("/register").
 *
 * @property email The email address associated with the registration.
 * @property confirmationCode The confirmation code for the registration, serialized using the UUIDSerializer.
 */
@Serializable
@Resource("/register")
class RegisterPath(
    val email: String? = null,
    @Serializable(with = UUIDSerializer::class)
    val confirmationCode: UUID? = null,
)

/**
 * Defines a route for handling registration requests.
 * It receives a [registrationConfirmationService] for confirming registrations.
 *
 * @param registrationConfirmationService The service responsible for confirming registrations.
 */
fun Route.register(registrationConfirmationService: RegistrationConfirmationService) {
    post<RegisterPath> {
        // Receive the RegisterPath parameters from the request body
        val parameters = call.receive<RegisterPath>()

        // Check if email or confirmationCode is null
        if (parameters.email == null || parameters.confirmationCode == null) {
            call.respond(HttpStatusCode.BadRequest)
            return@post
        }

        // Confirm the registration using the registrationConfirmationService
        val success = registrationConfirmationService.confirm(parameters.email, parameters.confirmationCode)

        // Respond with the appropriate HTTP status code based on the success of the registration confirmation
        if (success) {
            logger.info("successful registered ${parameters.email}")
            call.respond(HttpStatusCode.NoContent)
        } else {
            call.respond(HttpStatusCode.Unauthorized)
        }
    }
}
