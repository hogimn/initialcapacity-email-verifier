package io.initialcapacity.emailverifier.benchmark

import com.codahale.metrics.ConsoleReporter
import com.codahale.metrics.MetricRegistry
import io.ktor.client.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.slf4j.LoggerFactory
import java.math.RoundingMode
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.measureTime


/**
 * Benchmark class for testing the registration process.
 * @param registrationUrl The URL for registration.
 * @param registrationCount The total number of registrations to perform.
 * @param requestWorkerCount The number of request worker coroutines to launch.
 * @param registrationWorkerCount The number of registration worker coroutines to launch.
 * @param client The HttpClient instance for making HTTP requests.
 */
class Benchmark(
    private val registrationUrl: String,
    private val registrationCount: Int,
    private val requestWorkerCount: Int,
    private val registrationWorkerCount: Int,
    private val client: HttpClient,
) {
    // Create a logger instance for logging benchmark-related messages
    private val logger = LoggerFactory.getLogger(Benchmark::class.java)

    // Create a MetricRegistry instance for collecting benchmark metrics
    private val metrics = MetricRegistry()

    // Create a channel for passing confirmation messages between workers
    private val confirmations = Channel<Confirmation>(registrationCount)

    // Create a channel for generating and passing email addresses to request workers
    private val emails = Channel<String>(registrationCount)

    // Create a ConsoleReporter for reporting benchmark metrics to the console
    // Configure the reporter to display rates in seconds and durations in milliseconds
    private val reporter = ConsoleReporter.forRegistry(metrics).convertRatesTo(TimeUnit.SECONDS).convertDurationsTo(TimeUnit.MILLISECONDS).build()

    /**
     * Starts the benchmark and measures its duration.
     *
     * @param scope The coroutine scope to launch benchmark workers.
     * @return The duration of the benchmark.
     */
    suspend fun start(scope: CoroutineScope): Duration {
        // Log an informational message indicating the start of the benchmark with the number of registrations
        logger.info("starting benchmark with $registrationCount registrations")

        // Launch the request workers to handle registration requests
        launchRequestWorkers(scope)
        // Launch the registration workers to process confirmations
        launchRegistrationWorkers(scope)
        // Start the metric reporter to report benchmark metrics
        startReporter(scope)

        // Generate and send the specified number of email addresses to the request workers
        emails.generate(registrationCount)

        return measureTime {
            while (metrics.counter("registration - total").count < registrationCount) {
                // Wait until the total number of registrations reaches the desired count
                delay(100.milliseconds)
            }
        }.also { duration ->
            // Stop the benchmark by stopping the metric reporter and closing the channels
            stop()
            // Log an informational message indicating the duration of the benchmark
            logger.info("benchmark finished in $duration")

            // Calculate the registrations per second by dividing the count of successful registrations by the duration in seconds
            var registrationsPerSecond =
                    metrics.counter("registration - success").count.toDouble() / duration.inWholeMilliseconds * 1000

            registrationsPerSecond = String.format("%.2f", registrationsPerSecond).toDouble()

            // Log the calculated registrations per second
            logger.info("registrations per second is $registrationsPerSecond")

            val targetRegistrationPerSecond = 50
            if (registrationsPerSecond < targetRegistrationPerSecond) {
                // If registrations per second is less than target, log a failure message
                logger.info("test failed (registrations per second < $targetRegistrationPerSecond)")
            } else {
                // Otherwise, log a success message
                logger.info("test succeeded")
            }
        }
    }

    /**
     * Processes the confirmation message received from the notification server.
     *
     * @param message The JSON message received from the notification server.
     */
    suspend fun processConfirmation(message: String) {
        // Parse the JSON message into a JSON element
        val jsonBody = Json.parseToJsonElement(message)

        // Extract the email address from the JSON structure
        val email = jsonBody.jsonObject["personalizations"]?.jsonArray?.get(0)?.jsonObject?.get("to")
            ?.jsonArray?.get(0)?.jsonObject?.get("email")?.jsonPrimitive?.content
        // Extract the body content from the JSON structure
        val body = jsonBody.jsonObject["content"]?.jsonArray?.get(0)?.jsonObject?.get("value")?.jsonPrimitive?.content
        // Extract the confirmation code from the body content and convert it to a UUID
        val confirmationCode = UUID.fromString(body?.subSequence(26, 62).toString())

        // Send the confirmation information to the confirmations channel
        confirmations.send(Confirmation(email!!, confirmationCode))
    }

    /**
     * Launches the request workers to handle registration requests.
     *
     * @param scope The coroutine scope to launch the workers.
     */
    private fun launchRequestWorkers(scope: CoroutineScope) {
        repeat(requestWorkerCount) {
            // Create a new instance of RegistrationRequestWorker
            val worker = RegistrationRequestWorker(registrationUrl, client, metrics)

            scope.launch {
                // Start the worker and listen for emails on the channel
                worker.listen(emails)
            }
        }
    }


    /**
     * Launches the registration workers to handle registration confirmations.
     *
     * @param scope The coroutine scope to launch the workers.
     */
    private fun launchRegistrationWorkers(scope: CoroutineScope) {
        repeat(registrationWorkerCount) {
            // Create a new instance of RegistrationWorker
            val worker = RegistrationWorker(registrationUrl, client, metrics)

            scope.launch {
                // Start the worker and listen for confirmations on the channel
                worker.listen(confirmations)
            }
        }
    }

    /**
     * Starts the metrics reporter for logging the benchmark metrics.
     *
     * @param scope The coroutine scope to launch the reporter.
     */
    private fun startReporter(scope: CoroutineScope) {
        scope.launch {
            // Start the reporter with a reporting interval of 1 second
            reporter.start(1, TimeUnit.SECONDS)
        }
    }

    /**
     * Stops the benchmark by stopping the metrics reporter and closing the email and confirmation channels.
     */
    private fun stop() {
        // Stop the metrics reporter
        reporter.stop()
        // Close the email channel
        emails.close()
        // Close the confirmation channel
        confirmations.close()
    }
}

/**
 * Generates and sends random email addresses to the channel.
 *
 * @param count The number of email addresses to generate and send.
 */
private suspend fun Channel<String>.generate(count: Int) = repeat(count) {
    // Generate a random UUID and append it to the example.com domain
    // Send the generated email to the channel
    send("${UUID.randomUUID()}@example.com")
}
