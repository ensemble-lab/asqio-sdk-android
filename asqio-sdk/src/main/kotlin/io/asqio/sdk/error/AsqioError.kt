package io.asqio.sdk.error

import kotlinx.serialization.Serializable

/**
 * SDK で発生する全エラーのベースクラス。
 *
 * 利用側は `when` で分岐できるよう sealed class として提供します。
 */
public sealed class AsqioError(message: String, cause: Throwable? = null) : Exception(message, cause) {

    /** API からのエラーレスポンス（4xx / 5xx） */
    public class ApiError(
        public val code: ApiErrorCode,
        public val errorMessage: String,
        public val statusCode: Int,
    ) : AsqioError("API Error ($statusCode): [${code.raw}] $errorMessage")

    /** ネットワーク到達性などの低レベルエラー */
    public class NetworkError(cause: Throwable) : AsqioError("Network Error: ${cause.message}", cause)

    /** JSON デコード時の失敗 */
    public class DecodingError(cause: Throwable) : AsqioError("Decoding Error: ${cause.message}", cause)

    /** SDK が configure されていない */
    public object NotConfigured : AsqioError(
        "AsqioSupport SDK is not configured. Call AsqioSupport.configure() first."
    ) {
        private fun readResolve(): Any = NotConfigured
    }

    /** JWT Provider が例外を投げた */
    public class JwtProviderFailed(cause: Throwable?) : AsqioError(
        "Failed to obtain JWT token from provider.",
        cause,
    )

    /** HTTP レスポンスが不正（HTTPURLResponse でない、status コードが解釈不能、等） */
    public class InvalidResponse(message: String = "Invalid response from server.") :
        AsqioError(message)
}

@Serializable
internal data class ApiErrorBody(
    val error: String,
    val code: ApiErrorCode,
)
