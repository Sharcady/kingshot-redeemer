# Kingshot Gift Code Redeemer

Small Spring Boot/Kotlin service for redeeming Kingshot gift codes for registered players.

The application uses browser automation to interact with the Kingshot website:

- reads active gift codes from `https://kingshot.net/gift-codes`
- opens `https://kingshot.net/gift-codes/redeem`
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
  -v "$(pwd)/data:/data" \
  kingshot-redeemer
```

The image includes Google Chrome for Selenium browser automation. Runtime data is stored under `/data` in the container:

- `/data/players.json`
- `/data/images`

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

When a Railway Volume is mounted, Railway provides `RAILWAY_VOLUME_MOUNT_PATH`. By default this app stores data under that path:

- `${RAILWAY_VOLUME_MOUNT_PATH}/players.json`
- `${RAILWAY_VOLUME_MOUNT_PATH}/images`

If you mount the volume at `/data`, you do not need to set explicit file path variables. If you prefer explicit variables, set:

```text
KINGSHOT_PLAYERS_DB_PATH=/data/players.json
KINGSHOT_IMAGES_PATH=/data/images
KINGSHOT_BROWSER_HEADLESS=true
KINGSHOT_REDEEMER_CRON="0 0 */2 * * *"
KINGSHOT_BROWSER_REDEMPTION_RESULT_SETTLE_MILLIS=1500
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
    redeem-url: ${KINGSHOT_REDEEM_URL:https://kingshot.net/gift-codes/redeem}
    gift-codes-url: ${KINGSHOT_GIFT_CODES_URL:https://kingshot.net/gift-codes}
    players-db-path: ${KINGSHOT_PLAYERS_DB_PATH:${RAILWAY_VOLUME_MOUNT_PATH:/data}/players.json}
    images-path: ${KINGSHOT_IMAGES_PATH:${RAILWAY_VOLUME_MOUNT_PATH:/data}/images}
    scheduler:
      # cron: ${KINGSHOT_REDEEMER_CRON:*/10 * * * * *}
      cron: ${KINGSHOT_REDEEMER_CRON:0 0 */2 * * *}
    browser:
      headless: ${KINGSHOT_BROWSER_HEADLESS:true}
      timeout-seconds: ${KINGSHOT_BROWSER_TIMEOUT_SECONDS:30}
      redemption-result-settle-millis: ${KINGSHOT_BROWSER_REDEMPTION_RESULT_SETTLE_MILLIS:1500}
```

To test the scheduler every 10 seconds, comment the 2-hour cron and uncomment the 10-second cron.

## Data Files

Players are stored in:

```text
data/players.json
```

Player IDs are unique. Saving a player with an existing ID replaces the existing entry.

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
2. fetches active gift codes from the Kingshot gift-codes page
3. redeems those codes for every saved player
4. logs how many players succeeded and which players failed

Default schedule:

```text
0 0 */2 * * *
```

That runs every 2 hours.

## Test

```bash
./gradlew test
```
