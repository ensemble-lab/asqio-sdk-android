package io.asqio.sdk.service

import io.asqio.sdk.model.User
import io.asqio.sdk.model.UserLocale
import io.asqio.sdk.network.ApiClient
import io.asqio.sdk.network.ApiEndpoint

/**
 * ユーザープロファイル（identify）の登録／更新サービス。
 *
 * JWT で認証されたユーザーに email / name / locale を紐づけることで、
 * Push 通知が届かない端末でもメール通知を受け取れるようにします。
 */
public class UserService internal constructor(
    private val client: ApiClient,
) {
    /**
     * ユーザー情報を登録／更新します。 同じ user_id で複数回呼ばれた場合は
     * 最新の値で上書きされます。
     */
    public suspend fun identify(
        email: String? = null,
        name: String? = null,
        locale: UserLocale? = null,
    ): User {
        return client.request(
            ApiEndpoint.UpdateMe(email = email, name = name, locale = locale),
            User.serializer(),
        )
    }
}
