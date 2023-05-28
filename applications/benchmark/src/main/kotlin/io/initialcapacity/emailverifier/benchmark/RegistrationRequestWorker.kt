package io.initialcapacity.emailverifier.benchmark

import com.codahale.metrics.MetricRegistry
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.channels.ReceiveChannel

/**
 * Worker responsible for sending registration requests.
 *
 * @property registrationUrl The URL of the registration endpoint.
 * @property client The HTTP client to send the requests.
 * @property metrics The metrics registry for tracking request statistics.
 */
class RegistrationRequestWorker(
    private val registrationUrl: String,
    private val client: HttpClient,
    private val metrics: MetricRegistry,
) {
    /**
     * Listens to the emails channel and sends registration requests for each email received.
     *
     * @param emails The channel of emails to process.
     */
    suspend fun listen(emails: ReceiveChannel<String>) {
        for (email in emails) {
            requestRegistration(email)
        }
    }

    /**
     * Sends a registration request for the specified email.
     *
     * @param email The email address to register.
     * @return Unit
     */
    private suspend fun requestRegistration(email: String) = try {
        // Send a POST request to the registration URL with the email in the request body
        val response = client.post("$registrationUrl/request-registration") {
            headers { contentType(ContentType.Application.Json) }
            setBody("""{"email": "$email"}""")
        }

        // Check if the response status is a success
        if (response.status.isSuccess()) {
            // Increment the "request - success" counter in the metrics registry
            metrics.counter("request - success").inc()
        } else {
            // Increment the "request - failure" counter in the metrics registry
            metrics.counter("request - failure").inc()
        }
    } catch (e: java.net.ConnectException) {
        // If a connection exception occurs, increment the "request - failure" counter in the metrics registry
        metrics.counter("request - failure").inc()
    }
}
