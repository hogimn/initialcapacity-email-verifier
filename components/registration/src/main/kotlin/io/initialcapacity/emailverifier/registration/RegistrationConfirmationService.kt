package io.initialcapacity.emailverifier.registration

import io.initialcapacity.emailverifier.registrationrequest.RegistrationRequestDataGateway
import java.util.*

/**
 * Service class responsible for confirming registrations.
 *
 * @param requestGateway The data gateway for retrieving registration request information.
 * @param registrationGateway The data gateway for saving registration information.
 */
class RegistrationConfirmationService(
    private val requestGateway: RegistrationRequestDataGateway,
    private val registrationGateway: RegistrationDataGateway,
) {
    /**
     * Confirms a registration by verifying the email and confirmation code.
     *
     * @param email The email associated with the registration.
     * @param confirmationCode The confirmation code provided for the registration.
     * @return `true` if the registration is successfully confirmed, `false` otherwise.
     */
    fun confirm(email: String, confirmationCode: UUID): Boolean {
        // Retrieve the stored confirmation code for the given email
        val storedCode = requestGateway.find(email)

        // Compare the stored code with the provided confirmation code
        val success = storedCode == confirmationCode

        // If the confirmation code matches, save the email in the registration gateway
        if (success) {
            registrationGateway.save(email)
        }

        // Return the success status indicating whether the confirmation was successful
        return success
    }
}
