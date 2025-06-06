package com.eaglebank.api.infra.persistence

import com.eaglebank.api.config.DatabaseConfig
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {

    fun init(config: DatabaseConfig) {
        val database = Database.connect(
            url = config.url,
            driver = config.driver,
            user = config.user,
            password = config.password
        )

        transaction(database) {
            SchemaUtils.create(UserTable)
            // You can add other tables here if you create them
        }
    }
}