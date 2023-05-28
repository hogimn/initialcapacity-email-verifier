package io.initialcapacity.emailverifier.registration

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Data gateway class for handling registration data.
 *
 * @param db The database instance.
 */
class RegistrationDataGateway(private val db: Database) {
    /**
     * Save the email in the database.
     *
     * @param email The email to be saved.
     */
    fun save(email: String): Unit = transaction(db) {
        // Insert the email into the "registrations" table
        RegistrationTable.insert {
            it[RegistrationTable.email] = email
        }
    }
}

/**
 * Exposed table for the "registrations" table.
 */
private object RegistrationTable : LongIdTable() {
    // Define the "email" column of type text
    val email = text(name = "email")
    // Specify the table name as "registrations"
    override val tableName = "registrations"
}
