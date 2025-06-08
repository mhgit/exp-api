package com.eaglebank.api.infra.persistence

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID

class UserEntity(id: EntityID<String>) : Entity<String>(id) {
    companion object : EntityClass<String, UserEntity>(UserTable)

    var name by UserTable.name
    var addressLine1 by UserTable.addressLine1
    var addressLine2 by UserTable.addressLine2
    var addressLine3 by UserTable.addressLine3
    var town by UserTable.town
    var county by UserTable.county
    var postcode by UserTable.postcode
    var phoneNumber by UserTable.phoneNumber
    var email by UserTable.email
    var keycloakId by UserTable.keycloakId
    var createdTimestamp by UserTable.createdTimestamp
    var updatedTimestamp by UserTable.updatedTimestamp
}