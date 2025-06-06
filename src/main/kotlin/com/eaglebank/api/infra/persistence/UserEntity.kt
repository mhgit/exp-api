package com.eaglebank.api.infra.persistence

import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import java.util.UUID

class UserEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<UserEntity>(UserTable)

    var name by UserTable.name
    var addressLine1 by UserTable.addressLine1
    var addressLine2 by UserTable.addressLine2
    var addressLine3 by UserTable.addressLine3
    var town by UserTable.town
    var county by UserTable.county
    var postcode by UserTable.postcode
    var phoneNumber by UserTable.phoneNumber
    var email by UserTable.email
    var createdTimestamp by UserTable.createdTimestamp
    var updatedTimestamp by UserTable.updatedTimestamp
}