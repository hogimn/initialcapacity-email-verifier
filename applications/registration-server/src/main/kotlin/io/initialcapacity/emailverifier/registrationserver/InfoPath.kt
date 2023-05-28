package io.initialcapacity.emailverifier.registrationserver

import io.ktor.resources.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.resources.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

/**
 * Represents the resource path for retrieving information about the registration server.
 * This class is annotated with @Serializable to enable serialization and deserialization of its instances.
 * The resource is mapped to the root path ("/") using the @Resource("/") annotation.
 */
@Serializable
@Resource("/")
class InfoPath

/**
 * Registers a GET request handler for retrieving information about the registration server.
 * The handler is associated with the [InfoPath] class and responds with a map containing the application information.
 * The response includes a key-value pair with the key "application" and the value "registration server".
 */
fun Route.info() {
    // Registers a GET request handler for the InfoPath class
    get<InfoPath> {
        // Sends a response with a map containing the application information
        call.respond(mapOf("application" to "registration server"))
    }
}
