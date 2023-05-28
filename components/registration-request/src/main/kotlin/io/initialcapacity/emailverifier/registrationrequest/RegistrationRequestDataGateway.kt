package io.initialcapacity.emailverifier.registrationrequest

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

/**
 * Data gateway for registration requests.
 *
 * @param db The database instance.
 */
class RegistrationRequestDataGateway(private val db: Database) {
    /**
     * Find a registration request by email and return the confirmation code.
     *
     * @param email The email to search for.
     * @return The confirmation code associated with the registration request, or null if not found.
     */
    fun find(email: String): UUID? = transaction(db) {
        RegistrationRequestTable
            .select { RegistrationRequestTable.email eq email }
            .singleOrNull()?.get(RegistrationRequestTable.confirmationCode)
    }

    /**
     * Save a registration request with the email and confirmation code.
     *
     * @param email The email of the registration request.
     * @param confirmationCode The confirmation code associated with the registration request.
     */
    fun save(email: String, confirmationCode: UUID) = transaction(db) {
        RegistrationRequestTable.insert {
            it[RegistrationRequestTable.email] = email
            it[RegistrationRequestTable.confirmationCode] = confirmationCode
        }
    }
}

/**
 * Exposed table for the "registration_requests" table.
 */
private object RegistrationRequestTable : LongIdTable() {
    // The "email" column of type text.
    val email = text(name = "email")
    // The "confirmation_code" column of type UUID.
    val confirmationCode = uuid(name = "confirmation_code")
    // The table name is "registration_requests".
    override val tableName = "registration_requests"
}
