# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

asqio-sdk-android は asqio カスタマーサポートシステムの Android 向け SDK です。アプリ内で
ユーザーが問い合わせを作成し、運営者と Compose UI 経由でメッセージをやり取りできる機能を提供します。

- **モジュール名**: asqio-sdk（Android library / AAR）
- **パッケージ**: `io.asqio.sdk`
- **minSdk / targetSdk / compileSdk**: 24 / 35 / 35
- **JDK**: 17
- **Kotlin**: 2.1.x
- **AGP**: 8.7.x

## Build & Test Commands

```bash
# Release AAR を生成
./gradlew :asqio-sdk:assembleRelease

# ユニットテスト実行
./gradlew :asqio-sdk:testReleaseUnitTest

# 単一テストクラスを実行
./gradlew :asqio-sdk:testReleaseUnitTest --tests "io.asqio.sdk.network.ApiClientTest"

# 単一テストメソッドを実行
./gradlew :asqio-sdk:testReleaseUnitTest --tests "io.asqio.sdk.network.ApiClientTest.listTickets returns tickets"
```

JDK 17 (`JAVA_HOME=/opt/homebrew/opt/openjdk@17` など) と Android SDK
(`ANDROID_HOME=~/Library/Android/sdk`) が必要です。

## Architecture

```
Compose UI (AsqioSupportScreen, *Screen, ViewModels)
        ↓
Services (TicketService, MessageService, DeviceService, UserService)
        ↓
ApiClient (OkHttp + suspendCancellableCoroutine)
        ↓
Models (kotlinx.serialization, @Serializable)
```

### エントリーポイント

`io.asqio.sdk.AsqioSupport` が public API のシングルトン。`configure(context, tenantKey, jwtProvider)`
で初期化し、Compose の `AsqioSupportScreen()` で UI を呼び出します。

### スレッドセーフティ

- `ApiClient` は OkHttp ベースで thread-safe
- 全モデルは `@Serializable` の immutable `data class`
- ViewModel は `viewModelScope` 内で coroutines を起動
- Service / Client への直接アクセスも safe（不変オブジェクト + OkHttp）

### API 通信フロー

1. Composable → ViewModel が UI イベントを受信
2. ViewModel が `viewModelScope.launch` で Service を呼ぶ
3. Service が `ApiEndpoint` を組み立てて `ApiClient.request` を呼ぶ
4. `ApiClient` が `jwtProvider()` で JWT を取得し、Authorization と X-Tenant-Key を付与
5. レスポンスを kotlinx.serialization でデコードして返却

## Key Files

| ファイル | 役割 |
|---------|------|
| `asqio-sdk/src/main/kotlin/io/asqio/sdk/AsqioSupport.kt` | パブリック API エントリポイント（object） |
| `asqio-sdk/src/main/kotlin/io/asqio/sdk/AsqioConfiguration.kt` | 設定オブジェクト + `JwtProvider` typealias |
| `asqio-sdk/src/main/kotlin/io/asqio/sdk/network/ApiClient.kt` | OkHttp 製の HTTP クライアント |
| `asqio-sdk/src/main/kotlin/io/asqio/sdk/network/ApiEndpoint.kt` | sealed class でエンドポイントを定義 |
| `asqio-sdk/src/main/kotlin/io/asqio/sdk/service/*.kt` | ビジネスロジック層 |
| `asqio-sdk/src/main/kotlin/io/asqio/sdk/ui/AsqioSupportScreen.kt` | UI ビュー切替（List/Detail/New） |
| `asqio-sdk/src/main/kotlin/io/asqio/sdk/ui/*Screen.kt` | 各画面の Compose 実装 |
| `asqio-sdk/src/main/kotlin/io/asqio/sdk/error/AsqioError.kt` | エラー sealed class |

## Conventions

- 日本語の UI テキスト、コメントは日本語可
- すべての public 型は明示的に `public` を付ける（`explicit api mode` 想定）
- JSON キーはスネークケース。 `@SerialName` で変換する
- API レスポンスは `PaginationMeta` でページネーション対応
- 日時は `kotlinx.datetime.Instant`（ISO8601）
- `context` / `device_info` は `Map<String, String>`（iOS SDK と整合）
- エラーは `AsqioError` sealed class に集約

## Backend API Reference

バックエンドの API 仕様は `asqio-backend` リポジトリの `contracts/openapi/openapi.yaml` を参照。
エラーコードは `asqio-backend/contracts/schema/error-codes.json` を参照。

## 添付ファイル機能のスコープ

OpenAPI 仕様には現時点で添付エンドポイントが定義されていないため、Android SDK では
未対応としています。iOS SDK は multipart 経由で対応済みのため、契約が追従したら
フェーズ 2 で `MessageAttachmentUpload` 等を導入する想定です。
