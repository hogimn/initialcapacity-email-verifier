package io.initialcapacity.emailverifier.notification

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

/**
 * Data gateway for managing notifications in the database.
 */
class NotificationDataGateway(private val db: Database) {
    /**
     * Find a notification by email.
     *
     * @param email The email address to search for.
     * @return The confirmation code (UUID) of the found notification, or null if not found.
     */
    fun find(email: String): UUID? = transaction(db) {
        // Execute a database query to find a notification with the given email
        NotificationTable
            .select { NotificationTable.email eq email }
            .singleOrNull()?.get(NotificationTable.confirmationCode)
    }

    /**
     * Save a new notification.
     *
     * @param email The email address of the notification.
     * @param confirmationCode The confirmation code (UUID) of the notification.
     */
    fun save(email: String, confirmationCode: UUID) = transaction(db) {
        // Execute a database insert operation to save a new notification
        NotificationTable.insert {
            it[NotificationTable.email] = email
            it[NotificationTable.confirmationCode] = confirmationCode
        }
    }
}

// Define the database table for notifications
private object NotificationTable : LongIdTable() {
    val email = text(name = "email")
    val confirmationCode = uuid(name = "confirmation_code")
    override val tableName = "notifications"
}
