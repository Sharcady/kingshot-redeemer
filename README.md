# Kingshot Gift Code Redeemer

Small Spring Boot/Kotlin service for redeeming Kingshot gift codes for registered players.

The application uses browser automation to interact with the Kingshot website:

- reads active gift codes from `http://kingshotwiki.com/giftcodes/`
- opens `https://ks-giftcode.centurygame.com/`
- fills Player ID and Kingdom
- saves players to a local JSON file
- redeems the active gift codes for that player
- periodically repeats redemption for all saved players

Controller responses are shaped for a Discord bot: the redeem/register response includes a display message and, when available, a random image encoded as Base64.

## Requirements

- JDK 25
- Chrome installed locally
- ChromeDriver available to Selenium, or Selenium Manager able to resolve it

## Run

```bash
./gradlew bootRun
```

By default the app starts on port `8080`.

```bash
SERVER_PORT=9090 ./gradlew bootRun
```

## Docker

Build the container image:

```bash
docker build -t kingshot-redeemer .
```

Run it locally:

```bash
docker run --rm -p 8080:8080 \
  -v "$(pwd)/data:/app/data" \
  kingshot-redeemer
```

The image includes Google Chrome for Selenium browser automation. Without a configured volume, runtime data is stored under `/app/data` in the container:

- `/app/data/players.json`
- `/app/data/active_giftcodes.json`
- `/app/data/failed_giftcode_redemptions.json`
- `/app/data/images`

## Deploy on Railway

This repository is prepared for Railway with:

- `Dockerfile` at the repository root
- `railway.json` config-as-code that tells Railway to use the Dockerfile
- `PORT` support in `application.yaml`

To deploy:

1. Push the repository to GitHub.
2. In Railway, create a new project from the GitHub repository.
3. Railway should detect and use the root `Dockerfile`.
4. Open the service Networking settings and generate a public domain.
5. Create and attach a Railway Volume to this service.
6. Set the volume mount path to `/data`.

Railway provides the `PORT` variable automatically. The app reads it with:

```text
server.port=${PORT:${SERVER_PORT:8080}}
```

When a Railway Volume is mounted, Railway provides `RAILWAY_VOLUME_MOUNT_PATH`. The app automatically stores data under that path:

- `${RAILWAY_VOLUME_MOUNT_PATH}/players.json`
- `${RAILWAY_VOLUME_MOUNT_PATH}/active_giftcodes.json`
- `${RAILWAY_VOLUME_MOUNT_PATH}/failed_giftcode_redemptions.json`
- `${RAILWAY_VOLUME_MOUNT_PATH}/images`

If you prefer explicit file path variables, set:

```text
KINGSHOT_PLAYERS_DB_PATH=/data/players.json
KINGSHOT_ACTIVE_GIFT_CODES_DB_PATH=/data/active_giftcodes.json
KINGSHOT_FAILED_GIFT_CODE_REDEMPTIONS_DB_PATH=/data/failed_giftcode_redemptions.json
KINGSHOT_IMAGES_PATH=/data/images
KINGSHOT_BROWSER_HEADLESS=true
KINGSHOT_REGISTERED_PLAYERS_REDEMPTION_CRON="0 0 */6 * * *"
KINGSHOT_ACTIVE_GIFT_CODES_RETRIEVAL_CRON="0 */30 * * * *"
KINGSHOT_FAILED_GIFT_CODE_RETRY_CRON="0 0 * * * *"
KINGSHOT_BROWSER_REDEMPTION_RESULT_SETTLE_MILLIS=1500
KINGSHOT_BROWSER_REDEMPTION_RESULT_TIMEOUT_SECONDS=15
```

Railway volumes are available only at runtime, not during Docker build. The app creates `players.json` automatically if it is missing.

## Configuration

Runtime settings are configured in `src/main/resources/application.yaml` under the `application` root:

```yaml
server:
  port: ${PORT:${SERVER_PORT:8080}}

application:
  kingshot:
    base-url: ${KINGSHOT_BASE_URL:https://kingshot.net}
    redeem-url: ${KINGSHOT_REDEEM_URL:https://ks-giftcode.centurygame.com/}
    gift-codes-url: ${KINGSHOT_GIFT_CODES_URL:http://kingshotwiki.com/giftcodes/}
    players-db-path: ${KINGSHOT_PLAYERS_DB_PATH:${RAILWAY_VOLUME_MOUNT_PATH:data}/players.json}
    active-gift-codes-db-path: ${KINGSHOT_ACTIVE_GIFT_CODES_DB_PATH:${RAILWAY_VOLUME_MOUNT_PATH:data}/active_giftcodes.json}
    failed-gift-code-redemptions-db-path: ${KINGSHOT_FAILED_GIFT_CODE_REDEMPTIONS_DB_PATH:${RAILWAY_VOLUME_MOUNT_PATH:data}/failed_giftcode_redemptions.json}
    images-path: ${KINGSHOT_IMAGES_PATH:${RAILWAY_VOLUME_MOUNT_PATH:data}/images}
    scheduler:
      registered-players-redemption-cron: ${KINGSHOT_REGISTERED_PLAYERS_REDEMPTION_CRON:0 0 */6 * * *}
      # registered-players-redemption-cron: ${KINGSHOT_REGISTERED_PLAYERS_REDEMPTION_CRON:*/10 * * * * *} # Manual debug schedule
      active-gift-codes-retrieval-cron: ${KINGSHOT_ACTIVE_GIFT_CODES_RETRIEVAL_CRON:0 */30 * * * *}
      failed-gift-code-retry-cron: ${KINGSHOT_FAILED_GIFT_CODE_RETRY_CRON:0 0 * * * *}
    browser:
      headless: ${KINGSHOT_BROWSER_HEADLESS:true}
      timeout-seconds: ${KINGSHOT_BROWSER_TIMEOUT_SECONDS:30}
      redemption-result-timeout-seconds: ${KINGSHOT_BROWSER_REDEMPTION_RESULT_TIMEOUT_SECONDS:15}
      redemption-result-settle-millis: ${KINGSHOT_BROWSER_REDEMPTION_RESULT_SETTLE_MILLIS:1500}
```

The default schedules are: active-code refresh every 30 minutes, failed-pair retries every hour, and registered-player redemption every 6 hours. A commented 10-second redeemer cron is retained for manual debugging.

## Data Files

Players are stored in:

```text
data/players.json
```

Player IDs are unique. Saving a player with an existing ID replaces the existing entry.

The currently active codes are replacement-synced to `data/active_giftcodes.json`: each successful retrieval writes the complete current list, so codes no longer reported by the source are removed. Failed `(player, gift code)` redemption pairs are stored in `data/failed_giftcode_redemptions.json`. The retry job only retries a pair while its code is still present in the active-code snapshot; stale pairs are removed. It makes at most 10 retry attempts for each eligible pair.

Random response images are read from:

```text
data/images
```

Supported image extensions:

- `png`
- `jpg`
- `jpeg`
- `gif`
- `webp`

If the image directory is empty, the response contains `"image": null`.

## API

Interactive Swagger UI is available at `http://localhost:8080/swagger-ui/index.html` when the service is running. The OpenAPI specification is available at `/v3/api-docs`.

### Register Player and Redeem Current Codes

```http
GET /kingshot/register?playerId=123456789&kingdom=23&name=Arkadiy
```

This saves the player to `players.json`, fetches current active gift codes, redeems them, and returns a Discord-friendly response.

Example response:

```json
{
  "message": "Processed 3 gift code(s) for Arkadiy (123456789) in kingdom 23. Redeemed: CODE1. Already redeemed: CODE2. Failed or invalid: CODE3.",
  "image": {
    "fileName": "success.png",
    "mediaType": "image/png",
    "base64Content": "..."
  }
}
```

### Redeem and Register via POST

```http
POST /kingshot/redeem-and-register
Content-Type: application/json
```

```json
{
  "playerId": 123456789,
  "kingdom": 23,
  "name": "Arkadiy"
}
```

This performs the same flow as `GET /kingshot/register`.

### List Registered Players

```http
GET /kingshot/listplayers
```

Example response:

```json
[
  {
    "id": "123456789",
    "kingdom": 23,
    "name": "Arkadiy"
  }
]
```

### Remove Registered Player

```http
GET /kingshot/removeplayer?playerId=123456789
```

Example response:

```text
Removed player 123456789.
```

## Scheduler

`KingshotRedeemerScheduler` runs on the configured cron schedule. On each run it:

1. loads all players from `data/players.json`
2. reads the active-code snapshot from `data/active_giftcodes.json`
3. redeems that same list for every saved player
4. logs how many players succeeded and which players failed

Two additional jobs are configured:

- `ActiveGiftCodesRefreshScheduler` retrieves active codes from the website and replacement-syncs `active_giftcodes.json` without redeeming codes.
- `FailedGiftCodeRedemptionRetryScheduler` retries failed `(player, gift code)` pairs up to 10 times.

The registered-player scheduler never retrieves codes from the website; it uses the snapshot created by the retrieval job. The controller follows a separate sequence for a new player: retrieve codes, redeem them, then synchronize `active_giftcodes.json`.

## Test

```bash
./gradlew test
```
