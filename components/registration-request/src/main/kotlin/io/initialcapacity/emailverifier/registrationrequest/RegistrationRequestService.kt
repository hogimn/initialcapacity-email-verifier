package io.initialcapacity.emailverifier.registrationrequest

import io.initialcapacity.emailverifier.rabbitsupport.PublishAction
import io.initialcapacity.serializationsupport.UUIDSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.util.*

/**
 * A type alias for a function that provides a UUID
 */
typealias UuidProvider = () -> UUID

/**
 * Service class for registration requests.
 *
 * @param gateway The data gateway for registration requests.
 * @param publishNotification The action for publishing notifications.
 * @param uuidProvider The function for providing UUIDs.
 */
class RegistrationRequestService(
    private val gateway: RegistrationRequestDataGateway,
    private val publishNotification: PublishAction,
    private val uuidProvider: UuidProvider,
) {
    private val logger = LoggerFactory.getLogger(RegistrationRequestService::class.java)

    /**
     * Generates a confirmation code and publishes a registration request.
     *
     * @param email The email for the registration request.
     */
    fun generateCodeAndPublish(email: String) {
        // Generate a confirmation code using the uuidProvider function
        val confirmationCode = uuidProvider()

        // Save the registration request in the gateway
        gateway.save(email, confirmationCode)

        // Create a ConfirmationMessage object with the email and confirmation code
        val message = Json.encodeToString(ConfirmationMessage(email, confirmationCode))

        // Log the notification request
        logger.debug("publishing notification request {}", message)

        // Publish the notification message using the publishNotification action
        publishNotification(message)
    }
}

/**
 * Serializable data class representing a confirmation message.
 *
 * @param email The email for the confirmation.
 * @param confirmationCode The confirmation code.
 */
@Serializable
data class ConfirmationMessage(
    val email: String,
    @Serializable(with = UUIDSerializer::class)
    val confirmationCode: UUID,
)
