package io.asqio.sdk.service

import io.asqio.sdk.model.Device
import io.asqio.sdk.model.DeviceInfo
import io.asqio.sdk.model.TokenType
import io.asqio.sdk.network.ApiClient
import io.asqio.sdk.network.ApiEndpoint

/** Push 通知のためのデバイス登録 */
public class DeviceService internal constructor(
    private val client: ApiClient,
) {
    /**
     * FCM トークンでデバイスを登録します。
     *
     * Android では Push 通知に Firebase Cloud Messaging を使用します。
     *
     * @param fcmToken FCM が発行したトークン
     * @param deviceInfo 端末情報（[DeviceInfo.current] を渡してください）
     */
    public suspend fun registerDevice(
        fcmToken: String,
        deviceInfo: DeviceInfo,
    ): Device {
        return client.request(
            ApiEndpoint.RegisterDevice(
                pushToken = fcmToken,
                tokenType = TokenType.FCM,
                deviceInfo = deviceInfo,
            ),
            Device.serializer(),
        )
    }

    /**
     * 登録済みデバイスの情報を更新します。 push token のローテーションや
     * アプリバージョン更新時に呼び出してください。
     */
    public suspend fun updateDevice(
        id: String,
        pushToken: String? = null,
        osVersion: String? = null,
        appVersion: String? = null,
    ): Device {
        return client.request(
            ApiEndpoint.UpdateDevice(
                id = id,
                pushToken = pushToken,
                osVersion = osVersion,
                appVersion = appVersion,
            ),
            Device.serializer(),
        )
    }

    /** 登録済みデバイスを削除します（Push 通知の解除） */
    public suspend fun deleteDevice(id: String) {
        client.requestVoid(ApiEndpoint.DeleteDevice(id))
    }
}
