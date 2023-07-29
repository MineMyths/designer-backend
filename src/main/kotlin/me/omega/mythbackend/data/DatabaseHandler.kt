package me.omega.mythbackend.data

import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.coroutine.MongoCollection
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import kotlinx.coroutines.flow.toList
import me.omega.mythbackend.logger
import me.omega.mythbackend.processor.ParsedValue
import me.omega.mythstom.getConfiguredClient
import me.omega.mythstom.lib.capitalizeWords
import me.omega.mythstom.rpg.item.Item
import me.omega.mythstom.rpg.item.items.WeaponItem
import org.bson.Document
import kotlin.reflect.KClass

object DatabaseHandler {

    lateinit var client: MongoClient
    lateinit var database: MongoDatabase

    lateinit var itemCollection: MongoCollection<Document>

    fun init() {
        this.database = load()

        this.itemCollection = database.getCollection("items")

    }

    suspend fun existsWithId(type: String, id: String): Boolean {
        return when (type) {
            "items" -> {
                itemCollection.find(Filters.eq("_id", id)).toList().isNotEmpty()
            }
            else -> {
                logger.error("No data handler found for type $type")
                false
            }
        }
    }

    suspend fun <T : Any> get(clazz: KClass<T>, amount: Int? = null, startIndex: Int? = null, filter: String? = null): MutableList<ParsedValue> {
        val collection: MutableList<ParsedValue> = mutableListOf()
        return when (clazz) {
            Item::class -> {
                var items = itemCollection.find()
                if (!filter.isNullOrEmpty()) {
                    items = items.filter(Document("_id", Document("\$regex", ".*$filter.*")))
                }
                if (startIndex != null) {
                    items = items.skip(startIndex)
                }
                if (amount != null) {
                    items = items.limit(amount)
                }
                items.toList().forEach {
                    when (it["_type"]) {
                        "me.omega.mythstom.rpg.item.items.WeaponItem" -> {
                            collection.add(ParsedValue.parse(WeaponItem::class, it))
                        }
                    }
                }
                collection
            }
            else -> {
                logger.error("No data handler found for class $clazz")
                mutableListOf()
            }
        }
    }



    suspend fun push(type: String, id: String, json: String) {
        val fromJsonDocument = ParsedValue.fromJsonData(json)
        logger.info(fromJsonDocument.toJson())
        when (type) {
            "items" -> {
                val documentType = "me.omega.mythstom.rpg.item.items.${(fromJsonDocument["category"] as String).capitalizeWords()}Item"
                if (fromJsonDocument["subtype"] != "NONE") {
                    val subtype = "me.omega.mythstom.rpg.item.type.${(fromJsonDocument["category"] as String).capitalizeWords()}Type-${fromJsonDocument["subtype"]}"
                    fromJsonDocument["subtype"] = subtype
                }
                itemCollection.replaceOne(Filters.eq("_id", id), Document("_id", id).append("_type", documentType).append("data", fromJsonDocument), ReplaceOptions().upsert(true))
            }
            else -> {
                logger.error("No data handler found for type $type")
                return
            }
        }
    }

    suspend fun delete(type: String, id: String) {
        when (type) {
            "items" -> {
                itemCollection.deleteOne(Filters.eq("_id", id))
            }
            else -> {
                logger.error("No data handler found for type $type")
                return
            }
        }
    }

    private fun load(
        database: String = "minemyths"
    ): MongoDatabase {
        val now = System.currentTimeMillis()

        val mongoURI = System.getenv("MONGO_URI")
        this.client = getConfiguredClient(mongoURI)
        val mongoDatabase = client.getDatabase(database)

        logger.info("Successfully connected to mongodb in ${System.currentTimeMillis() - now}ms.")
        return mongoDatabase
    }


}