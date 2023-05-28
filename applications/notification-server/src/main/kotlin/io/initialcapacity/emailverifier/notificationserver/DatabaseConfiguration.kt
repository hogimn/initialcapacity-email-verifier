package io.initialcapacity.emailverifier.notificationserver

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database

/**
 * Configuration for the database connection.
 *
 * @param dbUrl The URL for the database connection.
 */
class DatabaseConfiguration(private val dbUrl: String) {
    // Create an instance of HikariConfig and configure the JDBC URL
    private val config = HikariConfig().apply { jdbcUrl = dbUrl }
    // Create an instance of HikariDataSource using the config
    private val ds = HikariDataSource(config)

    // Define a lazy-initialized property 'db' for the Database instance
    val db by lazy {
        Database.connect(ds)
    }
}
