package io.asqio.sdk

/**
 * JWT トークンを供給する suspend 関数の型エイリアス。
 *
 * SDK は API リクエストのたびにこれを呼び出すため、リフレッシュトークンによる
 * 自動更新フローに対応できます。
 */
public typealias JwtProvider = suspend () -> String

/**
 * SDK の設定。
 *
 * @property tenantKey バックエンドが発行する `X-Tenant-Key` の値
 * @property jwtProvider 認証 JWT を取得する suspend 関数
 * @property baseUrl API サーバの URL（末尾のスラッシュは無視されます）
 * @property appVersion アプリのバージョン文字列。null なら端末から自動取得
 */
public data class AsqioConfiguration(
    val tenantKey: String,
    val jwtProvider: JwtProvider,
    val baseUrl: String = DEFAULT_BASE_URL,
    val appVersion: String? = null,
) {
    public companion object {
        public const val DEFAULT_BASE_URL: String = "https://api.asqio.example.com"
    }
}
