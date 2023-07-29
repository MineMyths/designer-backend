package me.omega.mythbackend

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*
import kotlinx.serialization.json.Json
import me.omega.mythbackend.data.DatabaseHandler
import me.omega.mythbackend.data.DatabaseHandler.itemCollection
import me.omega.mythbackend.processor.ParsedValue
import me.omega.mythbackend.processor.getProperties
import me.omega.mythbackend.session.SessionManager
import me.omega.mythstom.rpg.attribute.Attribute
import me.omega.mythstom.rpg.attribute.ElementAttr
import me.omega.mythstom.rpg.attribute.PlayerAttr
import me.omega.mythstom.rpg.attribute.SecondaryAttr
import me.omega.mythstom.rpg.attribute.component.AttributeValue
import me.omega.mythstom.rpg.clazz.RPGClass
import me.omega.mythstom.rpg.item.Item
import me.omega.mythstom.rpg.item.type.*
import me.omega.mythstom.rpg.rarity.Rarity
import net.minestom.server.item.Material
import org.bson.Document
import java.util.*

val secret: String = System.getenv("SECRET")

fun Application.main() {

    DatabaseHandler.init()

    install(DefaultHeaders) {
        header("X-Engine", "Ktor") // will send this header with each response
    }

    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)
        allowHeader(HttpHeaders.Authorization)
        anyHost() // @TODO: Don't do this in production if possible. Try to limit it.
    }

    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }

    routing {

        get("/$secret/texture/{material}") {
            val material = call.parameters["material"]
            val resource = {}.javaClass.getResourceAsStream("/textures/${material!!.lowercase()}.png")
            if (resource == null) {
                call.respond(HttpStatusCode.NotFound)
            } else {
                val imageBytes = resource.readAllBytes()
                val base64Image = Base64.getEncoder().encodeToString(imageBytes)
                call.respondText("data:image/png;base64,$base64Image")
            }
        }


        get("/$secret/session/exists/{uuid}") {
            val session = SessionManager.getSession(UUID.fromString(call.parameters["uuid"]))
            if (session != null) {
                call.respond(true)
                // Update the last activity of the session
                session.lastActivity = System.currentTimeMillis()
            } else {
                call.respond(false)
            }
        }

        get("/$secret/session/new/{playerUuid}") {
            val playerUuid: UUID
            try {
                playerUuid = UUID.fromString(call.parameters["playerUuid"])
            } catch (e: Exception) {
                return@get call.respondText("Invalid UUID", status = HttpStatusCode.BadRequest)
            }
            val session = SessionManager.getFromPlayerUUID(playerUuid) ?: SessionManager.createSession(playerUuid)
            // Update the last activity of the session
            session.lastActivity = System.currentTimeMillis()
            call.respond(session.uuid.toString())
        }

        get("/$secret/session/get/{uuid}") {
            val uuid: UUID
            try {
                uuid = UUID.fromString(call.parameters["uuid"])
            } catch (e: Exception) {
                return@get call.respondText("Invalid UUID", status = HttpStatusCode.BadRequest)
            }
            val session = SessionManager.getSession(uuid) ?: return@get call.respondText("Session not found", status = HttpStatusCode.NotFound)
            call.respond(session)
        }

        delete("/$secret/session/delete/{uuid}") {
            val uuid: UUID
            try {
                uuid = UUID.fromString(call.parameters["uuid"])
            } catch (e: Exception) {
                return@delete call.respondText("Invalid UUID", status = HttpStatusCode.BadRequest)
            }
            SessionManager.deleteSession(uuid)
            call.respond(HttpStatusCode.OK)
        }

        post("/$secret/data/push/{type}/{valueId}/{value}/{new}") {
            val valueId = call.parameters["valueId"]!!
            val type = call.parameters["type"]!!
            val isNew = call.parameters["new"]!!.toBoolean()
            if (isNew && DatabaseHandler.existsWithId(type, valueId)) {
                call.respond(HttpStatusCode.Conflict)
                return@post
            }
            val value = call.parameters["value"]!!
            DatabaseHandler.push(type, valueId, value)
            call.respond(HttpStatusCode.OK)
        }

        delete("/$secret/data/delete/{type}/{valueId}") {
            val valueId = call.parameters["valueId"]!!
            val type = call.parameters["type"]!!
            DatabaseHandler.delete(type, valueId)
            call.respond(HttpStatusCode.OK)
        }

        get("/$secret/data/all/{type}") {
            getData(this, call.parameters["type"]!!)
        }

        get("/$secret/data/get/{type}/{amount}/{startIndex}/{filter?}") {
            val amount = call.parameters["amount"]!!.toInt()
            val startIndex = call.parameters["startIndex"]!!.toInt()
            val filter = call.parameters["filter"]
            getData(this, call.parameters["type"]!!, amount, startIndex, filter)
        }

        get("/$secret/data/count/{type}/{filter?}") {
            val type = call.parameters["type"]!!
            val filter = call.parameters["filter"]
            val collection = when (type) {
                "items" -> {
                    itemCollection
                }
                else -> {
                    call.respondText("Invalid type", status = HttpStatusCode.BadRequest)
                    return@get
                }
            }
            val count = if (filter.isNullOrEmpty()) {
                collection.countDocuments()
            } else {
                collection.countDocuments(Document("_id", Document("\$regex", ".*$filter.*")))
            }
            call.respond(count)
        }

        get("/$secret/data/properties/{type}") {
            call.respond(getProperties(call.parameters["type"]!!))
        }

        get("/$secret/data/types") {
            call.respond(mutableListOf("items", "mobs", "npcs", "quests", "skills", "setbonuses"))
        }

        get("/$secret/data/mappings") {
            call.respond(
                mapOf(
                String::class.qualifiedName!! to "string",
                Int::class.qualifiedName!! to "0",
                Boolean::class.qualifiedName!! to "False",
                Map::class.qualifiedName!! to "*",
                List::class.qualifiedName!! to "*",
                Rarity::class.qualifiedName!! to Rarity.values().joinToString("|") { it.name },
                ItemCategory::class.qualifiedName!! to ItemCategory.values().filter { it != ItemCategory.ALL && it != ItemCategory.UNIDENTIFIED }.joinToString("|") { it.display.uppercase() },
                ElementAttr::class.qualifiedName!! to ElementAttr.values().joinToString("|") { it.display.uppercase() },
                PlayerAttr::class.qualifiedName!! to PlayerAttr.values().joinToString("|") { it.display.uppercase() },
                SecondaryAttr::class.qualifiedName!! to SecondaryAttr.values().joinToString("|") { it.display.uppercase() },
                Attribute::class.qualifiedName!! to Attribute.values().joinToString("|") { it.display.uppercase() },
                AttributeValue::class.qualifiedName!! to "0",
                Material::class.qualifiedName!! to Material.values().joinToString("|") { it.name() },
                RPGClass::class.qualifiedName!! to RPGClass.classes.values.joinToString("|") { it.getPath() },
                "${ItemSubType::class.qualifiedName!!}-${WeaponType::class.qualifiedName!!}" to WeaponType.values().joinToString("|") { it.display.uppercase() },
                "${ItemSubType::class.qualifiedName!!}-${ArmorType::class.qualifiedName!!}" to ArmorType.values().joinToString("|") { it.display.uppercase() },
                "${ItemSubType::class.qualifiedName!!}-${RelicType::class.qualifiedName!!}" to RelicType.values().joinToString("|") { it.display.uppercase() },
                "${ItemSubType::class.qualifiedName!!}-${ToolType::class.qualifiedName!!}" to ToolType.values().joinToString("|") { it.display.uppercase() },
                "${ItemSubType::class.qualifiedName!!}-${NoneType::class.qualifiedName!!}" to NoneType.values().joinToString("|") { it.display.uppercase() },
            )
            )
        }
    }
}

suspend fun getData(context: PipelineContext<*, ApplicationCall>, type: String, amount: Int? = null, startIndex: Int? = null, filter: String? = null): MutableList<ParsedValue> {
    val collection: MutableList<ParsedValue> = when (type) {
        "items" -> {
            DatabaseHandler.get(Item::class, amount, startIndex, filter)
        }
        else -> {
            context.call.respondText("Invalid type", status = HttpStatusCode.BadRequest)
            return mutableListOf()
        }
    }
    context.call.respond(collection)
    return collection
}
