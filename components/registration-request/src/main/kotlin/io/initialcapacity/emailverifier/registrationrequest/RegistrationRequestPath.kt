package io.initialcapacity.emailverifier.registrationrequest

import io.initialcapacity.emailverifier.rabbitsupport.PublishAction
import io.ktor.http.*
import io.ktor.resources.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.resources.post
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

/**
 * Represents the path for registration requests.
 *
 * @param email The email for the registration request.
 */
@Serializable
@Resource("/request-registration")
class RegistrationRequestPath(val email: String? = null)

/**
 * Define a route for handling registration requests.
 *
 * @param publishRequest The function for publishing registration requests.
 */
fun Route.registrationRequest(publishRequest: PublishAction) {
    // Define a POST route with the path "/request-registration"
    post<RegistrationRequestPath> {
        // Receive the request body and deserialize it into a RegistrationRequestPath object
        val parameters = call.receive<RegistrationRequestPath>()

        // Check if the email parameter is not null
        if (parameters.email != null) {
            // Call the publishRequest function to publish the registration request
            publishRequest(parameters.email)
            // Respond with HTTP status code 204 (No Content)
            call.respond(HttpStatusCode.NoContent)
        } else {
            // Respond with HTTP status code 400 (Bad Request)
            call.respond(HttpStatusCode.BadRequest)
        }
    }
}
