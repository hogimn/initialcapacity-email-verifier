package io.initialcapacity.emailverifier.notification

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.utils.io.charsets.*
import org.slf4j.LoggerFactory
import java.net.URL


/**
 * A class for sending emails using the SendGrid API.
 * This class encapsulates the logic for sending emails via SendGrid by making HTTP requests to the SendGrid API.
 * It requires an HTTP client, SendGrid URL, SendGrid API key, and a from address.
 *
 * @param client The HTTP client for making requests to the SendGrid API.
 * @param sendgridUrl The URL of the SendGrid API.
 * @param sendgridApiKey The API key for authenticating with the SendGrid API.
 * @param fromAddress The email address used as the "from" address in the sent emails.
 */
class Emailer(
    private val client: HttpClient,
    private val sendgridUrl: URL,
    private val sendgridApiKey: String,
    private val fromAddress: String,
) {
    private val logger = LoggerFactory.getLogger(Emailer::class.java)

    /**
     * Sends an email using the SendGrid API.
     * @param toAddress The recipient email address.
     * @param subject The email subject.
     * @param message The email message.
     * @return True if the email was sent successfully, false otherwise.
     */
    suspend fun send(toAddress: String, subject: String, message: String): Boolean = try {
        client.post("$sendgridUrl/v3/mail/send") {
            headers {
                // Set the authorization header with the Sendgrid API key
                append(HttpHeaders.Authorization, "Bearer $sendgridApiKey")
                // Set the content type to JSON with UTF-8 charset
                contentType(ContentType.Application.Json.withCharset(Charset.forName("utf-8")))
            }
            // Set the request body as a JSON payload
            setBody("""
                {
                    "personalizations": [{"to":[{"email": "$toAddress"}]}],
                    "from": {"email": "$fromAddress"},
                    "subject": "$subject",
                    "content": [{
                        "type": "text/plain",
                        "value": "$message"
                    }]
                }""".trimIndent())
        }.status.isSuccess()
    } catch (e: java.net.ConnectException) {
        // Log an error if there was a connection issue
        logger.error("Unable to notify $toAddress via $sendgridUrl (${e::class.java}: ${e.message})")
        false
    }
}
