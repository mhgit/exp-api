package com.eaglebank.api.infra.persistence

import org.jetbrains.exposed.dao.id.IdTable

/**
 * Database table definition for transactions.
 */
object TransactionTable : IdTable<String>("transactions") {
    override val id = varchar("id", 255).entityId()
    val accountNumber = varchar("account_number", 20).index()
    val userId = varchar("user_id", 255)
    val amount = double("amount")
    val currency = varchar("currency", 3)
    val type = varchar("type", 20)
    val reference = varchar("reference", 255).nullable()
    val createdTimestamp = varchar("created_timestamp", 30)
}