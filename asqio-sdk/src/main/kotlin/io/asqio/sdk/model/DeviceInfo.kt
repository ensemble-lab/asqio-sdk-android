package io.asqio.sdk.model

import android.content.Context
import android.os.Build
import java.util.Locale
import java.util.TimeZone

/**
 * SDK が API に自動付与する端末情報。
 *
 * Android 端末から取得できる情報を [current] で収集します。
 * [appVersion] は context から取得しますが、テストや特別な用途では
 * 任意の値で直接インスタンス化することもできます。
 */
public data class DeviceInfo(
    val platform: Platform = Platform.ANDROID,
    val osVersion: String,
    val appVersion: String,
    val deviceModel: String,
    val locale: String,
    val timezone: String,
) {
    /** API リクエスト用の Map に変換（platform を含む） */
    internal fun toMap(): Map<String, String> = mapOf(
        "platform" to "android",
        "os_version" to osVersion,
        "app_version" to appVersion,
        "device_model" to deviceModel,
        "locale" to locale,
        "timezone" to timezone,
    )

    /** API リクエスト用の Map に変換（platform を除く） */
    internal fun toMapWithoutPlatform(): Map<String, String> = mapOf(
        "os_version" to osVersion,
        "app_version" to appVersion,
        "device_model" to deviceModel,
        "locale" to locale,
        "timezone" to timezone,
    )

    public companion object {
        /**
         * 現在の Android 端末情報を収集します。
         *
         * @param context アプリケーションコンテキスト（アプリのバージョン取得に使用）
         * @param appVersion null の場合は context.packageManager から取得を試み、
         *                   失敗した場合は "unknown" を使用します。
         */
        public fun current(context: Context, appVersion: String? = null): DeviceInfo {
            val resolvedAppVersion = appVersion ?: resolveAppVersion(context)
            return DeviceInfo(
                platform = Platform.ANDROID,
                osVersion = "Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})",
                appVersion = resolvedAppVersion,
                deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
                locale = Locale.getDefault().toLanguageTag(),
                timezone = TimeZone.getDefault().id,
            )
        }

        private fun resolveAppVersion(context: Context): String {
            return try {
                val info = context.packageManager.getPackageInfo(context.packageName, 0)
                info.versionName ?: "unknown"
            } catch (_: Exception) {
                "unknown"
            }
        }
    }
}
