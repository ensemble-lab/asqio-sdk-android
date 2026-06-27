package io.asqio.sdk.network

import kotlinx.serialization.json.Json

internal val asqioJson: Json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    encodeDefaults = false
}
