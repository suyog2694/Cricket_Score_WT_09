# Boundary Cricket Score

A live cricket match desk built with Spring Boot, MySQL, and React/Vite. Match deliveries are stored as events; innings totals and player figures are calculated from those records.

## Requirements

- Java 17 or newer
- Node.js 20+
- MySQL Server on `127.0.0.1:3306`

## Run locally

1. The configured schema is `cricket_score`; Hibernate creates or updates the tables at startup. For local setup, copy `.env.example` to `.env` in the repository root and enter your password on the `DB_PASSWORD` line. `.env` is ignored by Git and loaded when the API starts from `backend`.
2. From PowerShell, start the API:

   ```powershell
   cd backend
   .\mvnw.cmd spring-boot:run
   ```

   To avoid storing a password in a local file, you can instead prompt for it into the current process environment:

   ```powershell
   $secure = Read-Host 'MySQL password' -AsSecureString
   $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
   $env:DB_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)
   [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)
   .\mvnw.cmd spring-boot:run
   ```

   The API listens at `http://localhost:8080`. Alternatively, set `DB_PASSWORD` in your OS environment before launching the application. Do not commit the password or place it in `.env.example`.
3. In another terminal, run the dashboard:

   ```powershell
   cd frontend
   npm install
   npm run dev
   ```

   Open `http://localhost:5173`. On a new schema the API inserts a sample live match and rosters. The dashboard polls the API every five seconds.

The backend also accepts standard Spring environment variables `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`. The React API URL can be overridden with `VITE_API_URL`.

## REST API

| Method | Endpoint | Purpose |
| --- | --- | --- |
| `GET` | `/api/matches` | List matches with current scorecards |
| `POST` | `/api/matches` | Create a live match |
| `GET` | `/api/matches/{id}` | Get score, figures, innings, and recent events |
| `POST` | `/api/matches/{id}/events` | Record a delivery |
| `POST` | `/api/matches/{id}/innings/next` | Advance to the second innings or complete the match |
| `POST` | `/api/matches/{id}/finish` | Mark a match completed |
| `GET` | `/api/teams` | List teams |
| `POST` | `/api/teams` | Create a team |
| `GET` | `/api/teams/{teamId}/players` | List a team's players |
| `POST` | `/api/teams/{teamId}/players` | Add a player to a team |

Import `postman/Boundary Cricket Score.postman_collection.json` into Postman. The collection includes API requests and basic response assertions. Use `http://localhost:8080` as the `baseUrl` collection variable.

## Delivery request example

```json
{
  "strikerId": 1,
  "nonStrikerId": 2,
  "bowlerId": 9,
  "batterRuns": 4,
  "extras": 0,
  "extraType": "NONE",
  "wicket": false,
  "wicketPlayerId": null,
  "dismissalType": null,
  "note": "Cover drive"
}
```

`WIDE` and `NO_BALL` do not count as legal balls. Batting and bowling figures, run rate, over labels, and innings totals are derived from the saved delivery events.