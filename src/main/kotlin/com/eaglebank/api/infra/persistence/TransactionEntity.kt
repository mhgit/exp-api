package com.eaglebank.api.infra.persistence

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID

/**
 * Entity class for transactions.
 */
class TransactionEntity(id: EntityID<String>) : Entity<String>(id) {
    companion object : EntityClass<String, TransactionEntity>(TransactionTable)

    var accountNumber by TransactionTable.accountNumber
    var userId by TransactionTable.userId
    var amount by TransactionTable.amount
    var currency by TransactionTable.currency
    var type by TransactionTable.type
    var reference by TransactionTable.reference
    var createdTimestamp by TransactionTable.createdTimestamp
}