package test.initialcapacity.emailverifier.testsupport

import kotlinx.serialization.json.Json.Default.parseToJsonElement
import kotlin.test.assertEquals

/**
 * Asserts the equality of two JSON strings.
 *
 * @param expected The expected JSON string.
 * @param actual The actual JSON string to be compared. It can be null.
 *
 * @throws AssertionError if the expected and actual JSON strings are not equal.
 */
fun assertJsonEquals(expected: String, actual: String?) {
    // Convert the expected and actual JSON strings to JSON elements
    val expectedJson = parseToJsonElement(expected)
    val actualJson = parseToJsonElement(actual ?: "null")

    // Compare the expected and actual JSON elements for equality using assertEquals
    assertEquals(expectedJson, actualJson,
        "Expected\n$expectedJson\n to equal \n$actualJson\n")
}
