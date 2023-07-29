package me.omega.mythbackend

import com.sun.tools.javac.Main
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.slf4j.LoggerFactory
import java.util.logging.LogManager

val logger = LoggerFactory.getLogger(Main::class.java)

fun main() {
    System.setProperty("io.ktor.development", "true")

    val loggingInputStream = {}.javaClass.getResourceAsStream("/logging.properties")
    LogManager.getLogManager().readConfiguration(loggingInputStream)

    embeddedServer(Netty, port = 8080, module = Application::main, watchPaths = listOf(
        "classes",
        "resources"
    )).start(wait = true)

}