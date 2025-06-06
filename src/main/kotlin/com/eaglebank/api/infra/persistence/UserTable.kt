package com.eaglebank.api.infra.persistence

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.timestamp

object UserTable : UUIDTable("users") {
    val name = varchar("name", 255)
    val addressLine1 = varchar("address_line1", 255)
    val addressLine2 = varchar("address_line2", 255).nullable()
    val addressLine3 = varchar("address_line3", 255).nullable()
    val town = varchar("town", 255)
    val county = varchar("county", 255)
    val postcode = varchar("postcode", 20)
    val phoneNumber = varchar("phone_number", 20)
    val email = varchar("email", 255).uniqueIndex()
    val createdTimestamp = timestamp("created_timestamp")
    val updatedTimestamp = timestamp("updated_timestamp")
}