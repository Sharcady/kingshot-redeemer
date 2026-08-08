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

## Configuration

Runtime settings are configured in `src/main/resources/application.yaml` under the `application` root:

```yaml
server:
  port: ${SERVER_PORT:8080}

application:
  kingshot:
    base-url: ${KINGSHOT_BASE_URL:https://kingshot.net}
    redeem-url: ${KINGSHOT_REDEEM_URL:https://kingshot.net/gift-codes/redeem}
    gift-codes-url: ${KINGSHOT_GIFT_CODES_URL:https://kingshot.net/gift-codes}
    players-db-path: ${KINGSHOT_PLAYERS_DB_PATH:data/players.json}
    images-path: ${KINGSHOT_IMAGES_PATH:data/images}
    scheduler:
      # cron: ${KINGSHOT_REDEEMER_CRON:*/10 * * * * *}
      cron: ${KINGSHOT_REDEEMER_CRON:0 0 */2 * * *}
    browser:
      headless: ${KINGSHOT_BROWSER_HEADLESS:true}
      timeout-seconds: ${KINGSHOT_BROWSER_TIMEOUT_SECONDS:30}
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
GET /kingshot/register?playerId=123456789&kingdom=23
```

This saves the player to `players.json`, fetches current active gift codes, redeems them, and returns a Discord-friendly response.

Example response:

```json
{
  "message": "Redeemed 3 gift code(s) for player 123456789 in kingdom 23: CODE1, CODE2, CODE3.",
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
  "kingdom": 23
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
    "kingdom": 23
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
