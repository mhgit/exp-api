package com.eaglebank.api.infra.persistence

import org.jetbrains.exposed.dao.id.IdTable

/**
 * Database table definition for bank accounts.
 */
object BankAccountTable : IdTable<String>("bank_accounts") {
    override val id = varchar("id", 255).entityId()
    val userId = varchar("user_id", 255)
    val accountNumber = varchar("account_number", 20).uniqueIndex()
    val sortCode = varchar("sort_code", 10)
    val name = varchar("name", 255)
    val accountType = varchar("account_type", 50)
    val balance = double("balance")
    val currency = varchar("currency", 3)
    val createdTimestamp = varchar("created_timestamp", 30)
    val updatedTimestamp = varchar("updated_timestamp", 30)
}