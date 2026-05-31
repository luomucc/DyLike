package me.lingci.gradle

import org.gradle.api.Project
import java.util.Properties

private const val LOCAL_PROPERTIES_EXTRA_KEY = "me.lingci.localProperties"
private const val LOCAL_PROPERTIES_FILE_NAME = "local.properties"

fun Project.localProperty(name: String, defaultValue: String = ""): String {
    return localProperties().getProperty(name, defaultValue)
}

fun Project.localBuildConfigString(name: String, defaultValue: String = ""): String {
    return localProperty(name, defaultValue).toBuildConfigString()
}

fun String.toBuildConfigString(): String {
    val escaped = replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
    return "\"$escaped\""
}

private fun Project.localProperties(): Properties {
    val extraProperties = rootProject.extensions.extraProperties
    if (extraProperties.has(LOCAL_PROPERTIES_EXTRA_KEY)) {
        return extraProperties.get(LOCAL_PROPERTIES_EXTRA_KEY) as Properties
    }

    val properties = Properties()
    val localPropertiesFile = rootProject.file(LOCAL_PROPERTIES_FILE_NAME)
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { properties.load(it) }
    }

    extraProperties.set(LOCAL_PROPERTIES_EXTRA_KEY, properties)
    return properties
}
