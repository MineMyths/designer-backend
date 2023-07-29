package me.omega.mythbackend.processor

import me.omega.mythstom.rpg.item.Item
import org.reflections.Reflections
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberProperties

private val reflections = Reflections("me.omega.mythstom")

fun getProperties(type: String) : Map<String, String> {
    return when (type) {
        "items" -> {
            assembleProperties(getSubtypes(Item::class.java))
        }
        else -> {
            mutableMapOf()
        }
    }
}

fun getSubtypes(clazz: Class<*>): Array<Class<*>> {
    val subclasses = reflections.getSubTypesOf(clazz).toMutableList()
    if (!subclasses.contains(clazz)) {
        subclasses.add(clazz)
    }
    return subclasses.toTypedArray()
}

fun assembleProperties(clazzes: Array<Class<*>>): Map<String, String> {
    val fields = mutableMapOf<String, String>()
    for (clazz in clazzes) {
        for (field in clazz.kotlin.declaredMemberProperties) {
            val generics: MutableList<String> = mutableListOf()
            for (type in field.returnType.arguments) {
                generics.add(type.type?.classifier?.let { (it as KClass<*>).qualifiedName!! } ?: "")
            }
            if (generics.isNotEmpty()) {
                fields[field.name] = "${(field.returnType.classifier as KClass<*>).qualifiedName!!}?${generics.joinToString(",")}"
                continue
            }
            fields[field.name] = (field.returnType.classifier as KClass<*>).qualifiedName!!
        }
    }
    return fields
}

