package com.eaglebank.api.infra.persistence

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID

/**
 * Entity class for bank accounts.
 */
class BankAccountEntity(id: EntityID<String>) : Entity<String>(id) {
    companion object : EntityClass<String, BankAccountEntity>(BankAccountTable)

    var userId by BankAccountTable.userId
    var accountNumber by BankAccountTable.accountNumber
    var sortCode by BankAccountTable.sortCode
    var name by BankAccountTable.name
    var accountType by BankAccountTable.accountType
    var balance by BankAccountTable.balance
    var currency by BankAccountTable.currency
    var createdTimestamp by BankAccountTable.createdTimestamp
    var updatedTimestamp by BankAccountTable.updatedTimestamp
}