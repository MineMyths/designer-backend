package me.omega.mythbackend.processor

import kotlinx.serialization.Serializable
import org.bson.Document
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.KType
import kotlin.reflect.full.memberProperties

@Serializable
data class ParsedValue(
    val type: String,
    val value: String
) {
    companion object {
        inline fun <reified T : Any> parse(clazz: KClass<T>, value: Document): ParsedValue {
            val builtDocument = Document()
            clazz.memberProperties.forEach {
                builtDocument[it.name] = parseProperty(it, value)
            }
            return ParsedValue(T::class.qualifiedName!!, builtDocument.toJson())
        }

        fun fromJsonData(json: String): Document {
            val document = Document.parse(json)
            val builtDocument = Document()
            document.forEach { (key, value) ->
                value as Document
                if (value["value"] is Document) {
                    builtDocument[key] = fromJsonData((value["value"] as Document).toJson())
                    return@forEach
                }
                builtDocument[key] = value["value"]
            }
            return builtDocument
        }

    }
}

fun parseProperty(property: KProperty<*>, value: Document): Document {
    val builtDocument = Document()
    builtDocument["type"] = parseType(property.returnType)
    builtDocument["name"] = property.name
    val propertyValue = (value["data"] as Document).get(property.name)
    if (propertyValue is Document) {
        val doc = Document()
        propertyValue.forEach { key, value ->
            doc[key] = value
        }
        builtDocument["value"] = doc
    } else {
        val valueStr = propertyValue.toString()
        builtDocument["value"] = (if (valueStr.contains("-")) valueStr.split("-")[1] else valueStr)
    }
    return builtDocument
}

fun parseType(type: KType): Any {
    return when (type.classifier) {
        MutableMap::class -> {
            val builtDocument = Document()
            builtDocument["type"] = MutableMap::class.qualifiedName!!
            builtDocument["keyType"] = parseType(type.arguments[0].type!!)
            builtDocument["valueType"] = parseType(type.arguments[1].type!!)
            builtDocument
        }
        MutableList::class -> {
            val builtDocument = Document()
            builtDocument["type"] = MutableList::class.qualifiedName!!
            builtDocument["valueType"] = parseType(type.arguments[0].type!!)
            builtDocument
        }
        else -> {
            (type.classifier as KClass<*>).qualifiedName!!
        }
    }
}

//http://localhost:8080/hVDjgSPEE4ty/data/push/excalibur/items/{"attributes":{"type":{"keyType":"me.omega.mythstom.rpg.attribute.Attribute","valueType":"me.omega.mythstom.rpg.attribute.component.AttributeValue","type":"Map"},"name":"attributes","value":{}},"requirements":{"type":{"keyType":"me.omega.mythstom.rpg.attribute.PlayerAttr","valueType":"kotlin.Int","type":"Map"},"name":"requirements","value":{}},"damage":{"type":{"keyType":"me.omega.mythstom.rpg.attribute.ElementAttr","valueType":"kotlin.Int","type":"Map"},"name":"damage","value":{"NEUTRAL":{"value":"10000","name":"damage.value.NEUTRAL"}}},"rpgClass":{"value":"Warrior","type":"me.omega.mythstom.rpg.clazz.RPGClass","name":"rpgClass"},"id":{"value":"excalibur","type":"kotlin.String","name":"id"},"category":{"value":"WEAPON","type":"me.omega.mythstom.rpg.item.type.ItemCategory","name":"category"},"description":{"value":"The holy blade, excalibur.","type":"kotlin.String","name":"description"},"material":{"value":"minecraft:iron_sword","type":"net.minestom.server.item.Material","name":"material"},"modelData":{"value":"1","type":"kotlin.Int","name":"modelData"},"name":{"value":"Excalibur","type":"kotlin.String","name":"name"},"rarity":{"value":"UNIQUE","type":"me.omega.mythstom.rpg.rarity.Rarity","name":"rarity"},"shiny":{"value":"true","type":"kotlin.Boolean","name":"shiny"},"subtype":{"value":"GREATSWORD","type":"me.omega.mythstom.rpg.item.type.ItemSubType","name":"subtype"}}/true