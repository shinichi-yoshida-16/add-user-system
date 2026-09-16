# ユーザ登録システム

Google スプレッドシートをデータストアとして利用する、Spring Boot 製のユーザ登録・ログインシステムです。
Web 画面（ログイン / 新規登録 / ユーザ一覧）と REST API の両方を提供します。新規登録画面はログイン中の管理者ユーザが
AllowListにユーザを追加するための画面で、本システムへのログインにはadminFlag=TRUEが必須です。

## 技術構成

| 項目 | 内容 |
|---|---|
| 言語/フレームワーク | Java 17 / Spring Boot 3.5 |
| データストア | Google スプレッドシート (Google Sheets API v4) |
| 認証 | Spring Security（フォームログイン + セッション Cookie） |
| デプロイ先 | Google Cloud Run |
| ソース管理 | GitHub（`.github/workflows/ci-cd.yml` で CI/CD） |

## ディレクトリ構成

```
src/main/java/com/fcuro/userreg/
  config/     Spring Security・Google Sheets クライアントの設定
  domain/     ユーザドメインモデル
  repository/ スプレッドシートへのアクセス（読み込み・追記、spreadsheetIdは呼び出し時に指定）
  security/   認証(AuthenticationProvider)・パスワード比較・ログインセッション情報
  service/    登録処理などのユースケース
  web/        Thymeleaf 画面コントローラ
  web/api/    REST API コントローラ
src/main/resources/
  templates/  ログイン・登録・ホーム画面(Thymeleaf)
  application.yml
```

## パスワードの扱いについて（重要）

このシステムはスプレッドシート上に**bcryptでハッシュ化したパスワード**を保存します。アプリ側は Spring Security の
`BCryptPasswordEncoder`（`config/PasswordEncoderConfig.java`）でハッシュ化・比較を行うため、スプレッドシートに
初期ユーザーを手動で登録する場合も、パスワード欄には平文ではなくbcryptハッシュ（`$2a$`または`$2b$`から始まる文字列）を
設定してください。

## スプレッドシートID・シート名の入力について

このシステムは対象スプレッドシート・シート（タブ）を固定設定せず、**ログイン画面・新規登録画面でユーザ自身がスプレッドシートID・シート名を入力**します。
入力された値はログインセッションにのみ保持され（Cookie・DB等への永続化はしない）、ログアウトすると破棄されます。
そのため `application.yml` や環境変数にスプレッドシートID・シート名を設定する項目はありません。

## Google スプレッドシートの準備

1. 新規スプレッドシートを作成し、シート（タブ）を用意する（シート名は任意。ログイン・登録画面で入力するため固定名は不要）。
2. 1行目をヘッダー行とし、以下の列を用意する（A列の見出し文字列自体はシートごとに自由でよく、アプリは列の並び順のみを見る）。

   | A | B | C | D | E | F | G |
   |---|---|---|---|---|---|---|
   | allowId | email | password | targetId | retiredFlag | adminFlag | updatedAt |

   - `allowId`: 行を一意に識別するための内部用ID。ユーザ登録時にアプリが自動採番する（UUID）。ユーザが意識する項目ではない。
   - `email` / `password`: ログインに使用。パスワードはbcryptハッシュ化した文字列で保存する。
   - `targetId`: 別システムが通知メールの紐づけに使うための項目。本システムはこの値を読み書きするだけで解釈はしない。
   - `retiredFlag`: `TRUE` の場合、そのユーザはログイン不可になる（退会・無効化用）。ユーザ一覧画面（`/users`）から切り替えられる。
   - `adminFlag`: `TRUE`の場合、管理者ユーザとして本システムへのログインが可能、`FALSE`の場合、一般ユーザとして本システムのログインを不可とする。ユーザ登録画面（`/register`、要ログイン）でチェックボックスにより指定でき、登録後もユーザ一覧画面（`/users`）から値を切り替えられる。
   - `updatedAt`: 登録・更新日時（日本時間 `Asia/Tokyo`）。

3. 2行目以降に初期ユーザを登録しておく（例: `<UUID>` / `taro@example.com` / `password123`のbcryptハッシュ / 空欄以外の任意の値 / `FALSE` / `TRUE` / 任意の日時文字列）。targetIdが空欄、またはadminFlagが`FALSE`のユーザはログインできないため、**最低1件はadminFlagを`TRUE`にした初期管理者ユーザを手動で登録しておく**こと（ユーザ登録画面`/register`は認証済みユーザのみ利用できるため、以降のユーザはこの初期管理者がログインして登録する）。
4. スプレッドシートの ID（URL の `/d/` と `/edit` の間の文字列）と、手順1で用意したシート（タブ）名を控えておく。いずれもログイン・登録画面で入力する。

## Google Cloud 側の準備

1. GCP プロジェクトを作成し、以下の API を有効化する。
   - Google Sheets API
   - Cloud Run API
   - Artifact Registry API
   - Cloud Build API(Cloud Run の継続的デプロイで使用)
2. アプリ用のサービスアカウントを作成する。
3. 作成したスプレッドシートを「共有」から、サービスアカウントのメールアドレス（例: `xxx@yyy.iam.gserviceaccount.com`）に
   **編集者権限**で共有する（Sheets API の権限は IAM ロールではなく、このスプレッドシート共有設定で決まる）。
4. Artifact Registry にリポジトリを作成する（例: `user-registration-system`, リージョン `asia-northeast1`）。

認証には Application Default Credentials (ADC) を使用します。

- **ローカル開発時**: サービスアカウントの JSON 鍵を発行し、環境変数 `GOOGLE_APPLICATION_CREDENTIALS` にファイルパスを設定する。
- **Cloud Run 上**: JSON 鍵は不要。Cloud Run デプロイ時に「実行するサービスアカウント」として上記サービスアカウントを指定すれば、
  メタデータサーバ経由で自動的に認証される。

## ローカルでの起動

```bash
export GOOGLE_APPLICATION_CREDENTIALS=/path/to/service-account.json

mvn spring-boot:run
```

- Web 画面: http://localhost:8080/login （スプレッドシートID・シート名・メールアドレス・パスワードを入力）
- 新規登録（要ログイン）: http://localhost:8080/register （スプレッドシートID・シート名・メールアドレス・パスワード・任意でtargetId・adminFlagを指定。ログイン中の管理者が新しいAllowListユーザを追加する画面のため未ログインでは利用できない。登録完了後はログイン画面ではなくホーム画面（`/home`）に戻り、登録完了メッセージが表示される）
- ユーザ一覧（要ログイン）: http://localhost:8080/users （ログイン中のスプレッドシート・シートに登録されたユーザ一覧を表示し、retiredFlag・adminFlagのON/OFFを切り替えられる。本システムへのログインにはadminFlag=TRUEが必須のため、事実上、管理者ユーザのみがこの画面を利用する）
- REST API 登録: `POST /api/users/register` （JSON: `spreadsheetId`, `sheetName`, `email`, `password`, `targetId`(任意)。未認証で呼び出せるため、adminFlagは常に`FALSE`で登録される）
- REST API 現在ユーザ確認: `GET /api/users/me`（要ログイン）

## テスト

```bash
mvn test
```

## Docker ビルド

```bash
docker build -t user-registration-system .
docker run -p 8080:8080 \
  -e GOOGLE_APPLICATION_CREDENTIALS=/secrets/key.json \
  -v /path/to/service-account.json:/secrets/key.json:ro \
  user-registration-system
```

## Cloud Run へのデプロイ

Cloud Run コンソールの「リポジトリから継続的にデプロイする(Google Cloud Build を使用)」機能で、本リポジトリ(GitHub)の
`main` ブランチと連携しています。`main` に push されるたびに Cloud Build が `Dockerfile` を自動ビルドし、Artifact Registry
へ push した上で Cloud Run に自動デプロイされます。手動での `gcloud run deploy` やイメージの push は不要です。

Cloud Run サービス作成時の設定:

- リポジトリ: `shinichi-yoshida-16/add-user-system`(ブランチ: `^main$`)
- ビルドタイプ: Dockerfile
- リージョン: `asia-northeast1`
- コンテナポート: `8080`
- 実行サービスアカウント: スプレッドシートへの編集者アクセス権を持つサービスアカウント
- 認証: 未認証の呼び出しを許可(アプリ内の Spring Security でログイン保護)

このサービスアカウントは、ユーザがログイン・登録画面で入力した任意のスプレッドシートIDにアクセスできる必要があるため、
利用が想定される各スプレッドシートに対して個別に共有設定(編集者権限)を行ってください。

## GitHub Actions による CI

`.github/workflows/ci-cd.yml` は、すべての push / PR で `mvn clean verify`(ビルド・テスト)のみを行います。
デプロイは上記の Cloud Run 継続的デプロイ機能が担うため、このワークフローにはデプロイ処理を含めていません。

## 既知の制約

- スプレッドシートを DB として使うため、同時書き込みの競合制御（トランザクション）は行っていません。小規模・低頻度な利用を想定しています。
- パスワードはbcryptでハッシュ化して保存・比較します（上記「パスワードの扱いについて」参照）。
- スプレッドシートIDはログインセッションにのみ保持するため、セッション切れ・ログアウト後は再度IDの入力が必要です。
- サービスアカウントは、ユーザが入力しうる全てのスプレッドシートに対して個別に共有設定されている必要があります。
