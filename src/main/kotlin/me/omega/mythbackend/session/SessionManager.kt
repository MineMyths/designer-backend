package me.omega.mythbackend.session

import me.omega.mythbackend.logger
import java.util.*

object SessionManager {

    val sessions = mutableListOf<Session>()

    init {
        Timer().schedule(object : TimerTask() {
            override fun run() {
                logger.info("Cleaning up idle sessions")
                cleanupIdleSessions()
            }
        }, 0, 60 * 1000) // Run every minute

    }

    fun getSession(uuid: UUID): Session? {
        return sessions.find { it.uuid == uuid }
    }

    fun deleteSession(uuid: UUID) {
        sessions.removeIf { it.uuid == uuid }
    }

    fun getFromPlayerUUID(playerUUID: UUID): Session? {
        return sessions.find { it.playerUUID == playerUUID }
    }

    fun deleteFromPlayerUUID(playerUUID: UUID) {
        sessions.removeIf { it.playerUUID == playerUUID }
    }

    fun createSession(playerUUID: UUID): Session {
        val session = Session(UUID.randomUUID(), playerUUID)
        sessions.add(session)
        return session
    }

    fun cleanupIdleSessions() {
        val currentTime = System.currentTimeMillis()
        sessions.removeIf { session ->
            val idleTime = currentTime - session.lastActivity
            idleTime >= 5 * 60 * 1000
        }
    }

}