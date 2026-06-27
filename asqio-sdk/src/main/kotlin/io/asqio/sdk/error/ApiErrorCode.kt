package io.asqio.sdk.error

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * バックエンドが返す API エラーコード。
 *
 * 既知のコードは [Known] に列挙されており、サーバ側で新たなコードが追加された場合は
 * [Unknown] にラップされます。`contracts/schema/error-codes.json` に対応。
 */
@Serializable(with = ApiErrorCodeSerializer::class)
public sealed class ApiErrorCode {
    public abstract val raw: String

    /** 既知のエラーコード */
    public enum class Known(public val code: String) {
        UNAUTHORIZED("UNAUTHORIZED"),
        TENANT_KEY_REQUIRED("TENANT_KEY_REQUIRED"),
        TENANT_NOT_FOUND("TENANT_NOT_FOUND"),
        NOT_FOUND("NOT_FOUND"),
        VALIDATION_ERROR("VALIDATION_ERROR"),
        BAD_REQUEST("BAD_REQUEST"),
        INTERNAL_SERVER_ERROR("INTERNAL_SERVER_ERROR"),
    }

    public data class KnownCode(val value: Known) : ApiErrorCode() {
        override val raw: String get() = value.code
    }

    public data class Unknown(override val raw: String) : ApiErrorCode()

    public companion object {
        public fun fromRaw(raw: String): ApiErrorCode {
            val known = Known.entries.firstOrNull { it.code == raw }
            return if (known != null) KnownCode(known) else Unknown(raw)
        }
    }
}

internal object ApiErrorCodeSerializer : KSerializer<ApiErrorCode> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("ApiErrorCode", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ApiErrorCode) {
        encoder.encodeString(value.raw)
    }

    override fun deserialize(decoder: Decoder): ApiErrorCode {
        return ApiErrorCode.fromRaw(decoder.decodeString())
    }
}
