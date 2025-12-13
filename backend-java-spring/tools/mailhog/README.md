# MailHog (Local SMTP for Kaban)

Run a local SMTP server + web UI for dev testing.

## Start

From `C:\Users\Randall\Documents\kaban\tools\mailhog`:

```bash
docker compose up
```

## Configure `notification-service`

`notification-service` defaults to MailHog (`localhost:1025`) via `application.properties`.

Optional env overrides before starting `notification-service`:

```powershell
$env:SPRING_MAIL_HOST="localhost"
$env:SPRING_MAIL_PORT="1025"
$env:MAIL_FROM="no-reply@kaban.local"
$env:AUTH_BASE_URL="http://localhost:8080"
```

Then run the service and open the MailHog UI:

- UI: `http://localhost:8025`
