package io.github.lamba92.openaqua.packaging

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import kotlinx.serialization.json.Json
import nl.adaptivity.xmlutil.serialization.XML

internal val xmlSerializer =
    XML {
        indent = 2
        autoPolymorphic = false
    }

internal val yamlSerializer =
    Yaml(
        configuration =
            YamlConfiguration(
                encodeDefaults = false,
                strictMode = false,
            ),
    )

internal val jsonSerializer =
    Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }
