# asqio SDK for Android

[![CI](https://github.com/ensemble-lab/asqio-sdk-android/actions/workflows/ci.yml/badge.svg)](https://github.com/ensemble-lab/asqio-sdk-android/actions/workflows/ci.yml)
[![JitPack](https://jitpack.io/v/ensemble-lab/asqio-sdk-android.svg)](https://jitpack.io/#ensemble-lab/asqio-sdk-android)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![minSdk](https://img.shields.io/badge/minSdk-24-brightgreen)](https://developer.android.com/about/versions/nougat)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.0-blue.svg)](https://kotlinlang.org/)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4)](https://developer.android.com/jetpack/compose)

カスタマーサポートサービス asqio の Android 向け SDK です。アプリ内でユーザーが
問い合わせ（チケット）を作成し、運営者とメッセージをやり取りできる機能を提供します。

## 要件

- Android 7.0 (API 24) 以上
- Kotlin 2.0+
- Jetpack Compose（UI を使う場合）
- 利用側ビルド: JDK 17

## 主な機能

- スレッド一覧・詳細・新規作成の Compose UI
- HTTP クライアント (`AsqioClient` 相当のサービス群)
- 未読チケット数の取得（バッジ表示用）
- FCM トークンによる Push 通知用デバイス登録
- ユーザープロファイル（identify）の登録／更新
- 端末情報（OS バージョン、アプリバージョン、ロケール、タイムゾーン等）の自動付与

## インストール

JitPack 経由で配信しています。

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

```kotlin
// app/build.gradle.kts
dependencies {
    implementation("com.github.ensemble-lab:asqio-sdk-android:v0.1.0")
}
```

最新版およびリリース履歴は [JitPack のプロジェクトページ](https://jitpack.io/#ensemble-lab/asqio-sdk-android)
を参照してください。

## 使い方

### 1. SDK の初期化

アプリ起動時（Application#onCreate など）に `AsqioSupport.configure` を呼び出します。

```kotlin
import io.asqio.sdk.AsqioSupport

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AsqioSupport.configure(
            context = this,
            tenantKey = "your-tenant-key",
            jwtProvider = {
                // 現在の JWT トークンを返す suspend ラムダ
                authService.getToken()
            },
            // baseUrl と appVersion は任意
        )
    }
}
```

### 2. サポート UI の表示

任意の Compose 画面に `AsqioSupportScreen` を埋め込みます。スレッド一覧 → 詳細 →
新規作成の遷移は SDK 側で完結します。

```kotlin
@Composable
fun SupportRoute() {
    AsqioSupport.AsqioSupportScreen(
        context = mapOf("source" to "settings"),
    )
}
```

### 3. Push 通知用のデバイス登録

Firebase Cloud Messaging の token を受け取ったタイミングで登録します。

```kotlin
class MessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                AsqioSupport.registerForPushNotifications(token)
            }
        }
    }
}
```

### 4. 未読数の取得

バッジ表示などに使えます。

```kotlin
lifecycleScope.launch {
    val count = AsqioSupport.getUnreadCount()
    badge.value = count
}
```

### 5. ユーザー情報の identify（任意）

Push が届かない端末向けにメール通知の宛先を登録できます。

```kotlin
lifecycleScope.launch {
    AsqioSupport.identify(
        email = user.email,
        name = user.name,
        locale = UserLocale.JA,
    )
}
```

## 個別サービスの直接利用

UI を使わずに API クライアントとして利用したい場合は、サービスを直接取得できます。

```kotlin
val tickets = AsqioSupport.ticketService.listTickets(page = 1, perPage = 20)
val message = AsqioSupport.messageService.postMessage(ticketId, "こんにちは")
```

利用可能なサービス：

| サービス | 主な API |
|---------|--------|
| `TicketService` | `listTickets`, `createTicket`, `getTicket`, `markAsRead`, `getUnreadCount`, `listTopics` |
| `MessageService` | `listMessages`, `postMessage` |
| `DeviceService` | `registerDevice`, `updateDevice`, `deleteDevice` |
| `UserService` | `identify` |

## エラーハンドリング

SDK の例外はすべて `io.asqio.sdk.error.AsqioError` の sealed class です。`when` で分岐できます。

```kotlin
try {
    AsqioSupport.ticketService.listTickets()
} catch (e: AsqioError.ApiError) {
    // 4xx / 5xx レスポンス（e.code, e.statusCode）
} catch (e: AsqioError.NetworkError) {
    // 通信失敗
} catch (e: AsqioError.NotConfigured) {
    // configure() を呼ぶ前に SDK API を使った
} catch (e: AsqioError) {
    // その他（JwtProviderFailed, DecodingError, InvalidResponse）
}
```

## ビルド方法

```sh
# ローカル AAR の生成
./gradlew :asqio-sdk:assembleRelease

# ユニットテスト実行
./gradlew :asqio-sdk:testReleaseUnitTest
```

JDK 17 と Android SDK が必要です。

## 関連リポジトリ

- [`asqio-backend`](https://github.com/aspick/asqio-backend) — Rails 製の Support API + 管理画面
- [`asqio-sdk-ios`](https://github.com/aspick/asqio-sdk-ios) — iOS SDK (Swift Package)
- [`asqio-sdk-web`](https://github.com/ensemble-lab/asqio-sdk-web) — Web SDK (npm package)

## 添付ファイル機能について（現状）

OpenAPI 契約 (`asqio-backend/contracts/openapi/openapi.yaml`) には添付ファイル系の
エンドポイントが現時点で定義されていないため、本 Android SDK では添付機能を未実装
としています。iOS SDK はすでに multipart で添付に対応しており、バックエンドも
おそらく受け付けるため、契約の更新後にフェーズ 2 で追加予定です。

## コントリビュート

バグ報告・機能要望は GitHub の [Issues](https://github.com/ensemble-lab/asqio-sdk-android/issues)
へお願いします。プルリクエストも歓迎です。

## ライセンス

[MIT](LICENSE)
