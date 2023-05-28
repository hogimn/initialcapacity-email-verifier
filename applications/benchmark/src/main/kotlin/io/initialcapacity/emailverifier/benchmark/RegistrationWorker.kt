package io.initialcapacity.emailverifier.benchmark

import com.codahale.metrics.MetricRegistry
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.channels.ReceiveChannel

/**
 * Worker class responsible for performing registration based on confirmation objects.
 *
 * @property registrationUrl The URL for the registration endpoint.
 * @property client The HTTP client for sending requests.
 * @property metrics The metric registry for tracking registration metrics.
 */
class RegistrationWorker(
    private val registrationUrl: String,
    private val client: HttpClient,
    private val metrics: MetricRegistry,
) {
    /**
     * Listens for confirmation objects and performs registration based on the confirmation details.
     *
     * @param confirmations The channel for receiving confirmation objects.
     * @return Unit
     */
    suspend fun listen(confirmations: ReceiveChannel<Confirmation>) {
        // Iterate over the received confirmations
        for (confirmation in confirmations) {
            // Register the confirmation
            register(confirmation)
        }
    }

    /**
     * Registers the specified confirmation.
     *
     * @param confirmation The confirmation object containing email and confirmation code.
     * @return Unit
     */
    private suspend fun register(confirmation: Confirmation) = try {
        // Send a POST request to the registration URL with the confirmation details in the request body
        val response = client.post("$registrationUrl/register") {
            headers { contentType(ContentType.Application.Json) }
            setBody("""{"email": "${confirmation.email}", "confirmationCode": "${confirmation.code}"}""")
        }

        // Check if the response status is a success
        if (response.status.isSuccess()) {
            // Increment the "registration - success" counter in the metrics registry
            metrics.counter("registration - success").inc()
            // Increment the "registration - total" counter in the metrics registry
            metrics.counter("registration - total").inc()
        } else {
            // Increment the "registration - failure" counter in the metrics registry
            metrics.counter("registration - failure").inc()
            // Increment the "registration - total" counter in the metrics registry
            metrics.counter("registration - total").inc()
        }
    } catch (e: java.net.ConnectException) {
        // If a connection exception occurs, increment the "registration - failure" counter in the metrics registry
        metrics.counter("registration - failure").inc()
        // Increment the "registration - total" counter in the metrics registry
        metrics.counter("registration - total").inc()
    }
}
