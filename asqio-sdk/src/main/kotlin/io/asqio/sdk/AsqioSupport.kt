package io.asqio.sdk

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import io.asqio.sdk.error.AsqioError
import io.asqio.sdk.model.Device
import io.asqio.sdk.model.DeviceInfo
import io.asqio.sdk.model.User
import io.asqio.sdk.model.UserLocale
import io.asqio.sdk.network.ApiClient
import io.asqio.sdk.service.DeviceService
import io.asqio.sdk.service.MessageService
import io.asqio.sdk.service.TicketService
import io.asqio.sdk.service.UserService
import io.asqio.sdk.ui.AsqioSupportInternalScreen

/**
 * asqio Android SDK のパブリックエントリポイント。
 *
 * ## 使用例
 *
 * アプリ起動時（Application onCreate 等）に [configure] を呼び出して初期化します。
 *
 * ```kotlin
 * AsqioSupport.configure(
 *     context = applicationContext,
 *     tenantKey = "your-tenant-key",
 *     jwtProvider = { authService.getToken() },
 * )
 * ```
 *
 * Compose 画面で UI を表示するには [AsqioSupportScreen] を呼び出します。
 *
 * ```kotlin
 * @Composable
 * fun SupportRoute() {
 *     AsqioSupport.AsqioSupportScreen(context = mapOf("source" to "settings"))
 * }
 * ```
 *
 * FCM トークン受信時に Push 通知用のデバイス登録を行います。
 *
 * ```kotlin
 * FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
 *     lifecycleScope.launch {
 *         AsqioSupport.registerForPushNotifications(token)
 *     }
 * }
 * ```
 *
 * 未読数をバッジ等で表示する場合は [getUnreadCount] を使用します。
 *
 * ```kotlin
 * val count = AsqioSupport.getUnreadCount()
 * ```
 */
public object AsqioSupport {

    private val lock = Any()

    @Volatile
    private var appContext: Context? = null

    @Volatile
    private var configuration: AsqioConfiguration? = null

    @Volatile
    private var _ticketService: TicketService? = null

    @Volatile
    private var _messageService: MessageService? = null

    @Volatile
    private var _deviceService: DeviceService? = null

    @Volatile
    private var _userService: UserService? = null

    /**
     * SDK を初期化します。 同じ AsqioSupport インスタンスに対して複数回呼ぶと
     * 設定が上書きされ、内部のクライアント／サービスは再生成されます。
     *
     * @param context Application Context もしくは Activity Context。 内部では
     *                applicationContext を取り出して保持します。
     * @param tenantKey バックエンドから払い出された `X-Tenant-Key` の値
     * @param jwtProvider 現在の JWT を返す suspend 関数
     * @param baseUrl API サーバの URL（省略時は [AsqioConfiguration.DEFAULT_BASE_URL]）
     * @param appVersion アプリのバージョン文字列。 null の場合は端末から自動取得します
     */
    @JvmStatic
    @JvmOverloads
    public fun configure(
        context: Context,
        tenantKey: String,
        jwtProvider: JwtProvider,
        baseUrl: String = AsqioConfiguration.DEFAULT_BASE_URL,
        appVersion: String? = null,
    ) {
        val config = AsqioConfiguration(
            tenantKey = tenantKey,
            jwtProvider = jwtProvider,
            baseUrl = baseUrl,
            appVersion = appVersion,
        )
        synchronized(lock) {
            appContext = context.applicationContext
            configuration = config
            val client = ApiClient(config)
            _ticketService = TicketService(client)
            _messageService = MessageService(client)
            _deviceService = DeviceService(client)
            _userService = UserService(client)
        }
    }

    /** SDK が configure 済みかどうか */
    public val isConfigured: Boolean
        get() = configuration != null

    /** チケット操作サービス */
    public val ticketService: TicketService
        get() = _ticketService ?: throw AsqioError.NotConfigured

    /** メッセージ操作サービス */
    public val messageService: MessageService
        get() = _messageService ?: throw AsqioError.NotConfigured

    /** デバイス操作サービス */
    public val deviceService: DeviceService
        get() = _deviceService ?: throw AsqioError.NotConfigured

    /** ユーザー情報サービス（identify） */
    public val userService: UserService
        get() = _userService ?: throw AsqioError.NotConfigured

    /**
     * 未読チケット数を取得します。 アプリのバッジ表示等に利用してください。
     */
    public suspend fun getUnreadCount(): Int = ticketService.getUnreadCount()

    /**
     * FCM トークンでデバイスを登録します。 ユーザーへの Push 通知に使われます。
     *
     * @param fcmToken Firebase Cloud Messaging で取得したトークン文字列
     * @return 登録された [Device]
     */
    public suspend fun registerForPushNotifications(fcmToken: String): Device {
        val ctx = appContext ?: throw AsqioError.NotConfigured
        return deviceService.registerDevice(
            fcmToken = fcmToken,
            deviceInfo = DeviceInfo.current(ctx, configuration?.appVersion),
        )
    }

    /**
     * ユーザー情報（email / name / locale）をサーバに紐付けます。
     * 同じユーザーに対して複数回呼ぶと最新の値で上書きされます。
     */
    public suspend fun identify(
        email: String? = null,
        name: String? = null,
        locale: UserLocale? = null,
    ): User = userService.identify(email = email, name = name, locale = locale)

    /**
     * チケット一覧・詳細・新規作成を含むサポート UI を表示します。
     *
     * @param context 新規チケット作成時にチケットメタデータとして付与する key-value
     * @param modifier Compose の [Modifier]
     */
    @Composable
    public fun AsqioSupportScreen(
        context: Map<String, String>? = null,
        modifier: Modifier = Modifier,
    ) {
        val androidContext = LocalContext.current.applicationContext
        val deviceInfo = remember(androidContext, this) {
            DeviceInfo.current(androidContext, configuration?.appVersion)
        }

        val ticketSvc = _ticketService
        val messageSvc = _messageService
        if (ticketSvc == null || messageSvc == null) {
            NotConfiguredView(modifier)
            return
        }

        AsqioSupportInternalScreen(
            ticketService = ticketSvc,
            messageService = messageSvc,
            deviceInfo = deviceInfo,
            context = context,
            modifier = modifier.fillMaxSize(),
        )
    }

    /** テスト時などに状態をリセットするための内部 API */
    internal fun resetForTesting() {
        synchronized(lock) {
            appContext = null
            configuration = null
            _ticketService = null
            _messageService = null
            _deviceService = null
            _userService = null
        }
    }
}

@Composable
private fun NotConfiguredView(modifier: Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "SDK が初期化されていません。 AsqioSupport.configure() を呼んでください。",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
