package io.asqio.sdk.network

import io.asqio.sdk.AsqioConfiguration
import io.asqio.sdk.JwtProvider
import io.asqio.sdk.error.ApiErrorBody
import io.asqio.sdk.error.ApiErrorCode
import io.asqio.sdk.error.AsqioError
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * SDK の HTTP クライアント。 OkHttp ベース。
 *
 * Authorization (JWT) と X-Tenant-Key ヘッダーを毎リクエストで付与し、
 * JSON ボディは kotlinx.serialization でエンコード／デコードします。
 */
internal class ApiClient(
    private val configuration: AsqioConfiguration,
    private val json: Json = asqioJson,
    httpClient: OkHttpClient? = null,
) {
    private val jwtProvider: JwtProvider get() = configuration.jwtProvider
    private val tenantKey: String get() = configuration.tenantKey
    private val baseUrl: String get() = configuration.baseUrl.trimEnd('/')

    private val client: OkHttpClient = httpClient ?: OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun <T> request(
        endpoint: ApiEndpoint,
        deserializer: DeserializationStrategy<T>,
    ): T {
        val response = execute(endpoint)
        response.use { r ->
            val bodyString = r.body?.string().orEmpty()
            if (r.isSuccessful) {
                return try {
                    json.decodeFromString(deserializer, bodyString)
                } catch (e: SerializationException) {
                    throw AsqioError.DecodingError(e)
                } catch (e: IllegalArgumentException) {
                    throw AsqioError.DecodingError(e)
                }
            } else {
                throw parseError(bodyString, r.code)
            }
        }
    }

    suspend fun requestVoid(endpoint: ApiEndpoint) {
        val response = execute(endpoint)
        response.use { r ->
            if (!r.isSuccessful) {
                val bodyString = r.body?.string().orEmpty()
                throw parseError(bodyString, r.code)
            }
        }
    }

    private suspend fun execute(endpoint: ApiEndpoint): Response {
        val request = buildRequest(endpoint)
        return try {
            client.newCall(request).await()
        } catch (e: AsqioError) {
            throw e
        } catch (e: IOException) {
            throw AsqioError.NetworkError(e)
        }
    }

    private suspend fun buildRequest(endpoint: ApiEndpoint): Request {
        val jwt = try {
            jwtProvider()
        } catch (e: Exception) {
            throw AsqioError.JwtProviderFailed(e)
        }

        val urlBuilder = (baseUrl + endpoint.path).toHttpUrl().newBuilder()
        endpoint.query.forEach { (k, v) -> urlBuilder.addQueryParameter(k, v) }

        val builder = Request.Builder()
            .url(urlBuilder.build())
            .header("Authorization", "Bearer $jwt")
            .header("X-Tenant-Key", tenantKey)
            .header("Accept", "application/json")

        val requestBody: RequestBody? = endpoint.body?.let {
            json.encodeToString(JsonObject.serializer(), it).toJsonBody()
        }

        when (endpoint.method) {
            HttpMethod.GET -> builder.get()
            HttpMethod.POST -> builder.post(requestBody ?: EMPTY_JSON_BODY)
            HttpMethod.PUT -> builder.put(requestBody ?: EMPTY_JSON_BODY)
            HttpMethod.DELETE -> {
                if (requestBody != null) builder.delete(requestBody) else builder.delete()
            }
        }
        return builder.build()
    }

    private fun parseError(bodyString: String, statusCode: Int): AsqioError {
        return try {
            val errorBody = json.decodeFromString(ApiErrorBody.serializer(), bodyString)
            AsqioError.ApiError(errorBody.code, errorBody.error, statusCode)
        } catch (_: SerializationException) {
            AsqioError.ApiError(
                code = ApiErrorCode.fromRaw("UNKNOWN"),
                errorMessage = bodyString.ifBlank { "Unknown error" },
                statusCode = statusCode,
            )
        } catch (_: IllegalArgumentException) {
            AsqioError.ApiError(
                code = ApiErrorCode.fromRaw("UNKNOWN"),
                errorMessage = bodyString.ifBlank { "Unknown error" },
                statusCode = statusCode,
            )
        }
    }

    private companion object {
        private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
        private val EMPTY_JSON_BODY: RequestBody = "{}".toRequestBody(JSON_MEDIA)
        private fun String.toJsonBody(): RequestBody = toRequestBody(JSON_MEDIA)
    }
}

/** OkHttp Call を coroutine に橋渡し */
private suspend fun Call.await(): Response = suspendCancellableCoroutine { cont ->
    enqueue(object : Callback {
        override fun onResponse(call: Call, response: Response) {
            cont.resume(response)
        }

        override fun onFailure(call: Call, e: IOException) {
            if (cont.isActive) cont.resumeWithException(e)
        }
    })
    cont.invokeOnCancellation {
        runCatching { cancel() }
    }
}
