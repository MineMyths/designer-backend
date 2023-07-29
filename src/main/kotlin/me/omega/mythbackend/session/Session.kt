package me.omega.mythbackend.session

import kotlinx.serialization.Serializable
import world.cepi.kstom.serializer.SerializableUUID

@Serializable
data class Session(
    val uuid: SerializableUUID,
    val playerUUID: SerializableUUID,
    var lastActivity: Long = System.currentTimeMillis()
)