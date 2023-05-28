package io.initialcapacity.emailverifier.notification

import org.slf4j.LoggerFactory
import java.util.*

/**
 * Notifier class responsible for sending email notifications to users.
 *
 * @param gateway The data gateway for managing notifications in the database.
 * @param emailer The emailer component used to send emails.
 */
class Notifier(
    private val gateway: NotificationDataGateway,
    private val emailer: Emailer
) {
    private val logger = LoggerFactory.getLogger(Notifier::class.java)

    /**
     * Sends a notification email to a user with the provided email address and confirmation code.
     *
     * @param email The email address of the user.
     * @param confirmationCode The confirmation code for the user.
     */
    suspend fun notify(email: String, confirmationCode: UUID) {
        // Save the notification in the data gateway
        gateway.save(email, confirmationCode)
        // Prepare the subject and message for the email
        val subject = "Confirmation code"
        val message = "Your confirmation code is $confirmationCode"

        // Log the details of the notification being sent
        logger.debug("sending notification to: {}, subject: {}, message: {}", email, subject, message)
        // Send the email notification using the emailer
        emailer.send(
            toAddress = email,
            subject = subject,
            message = message
        )
    }
}
