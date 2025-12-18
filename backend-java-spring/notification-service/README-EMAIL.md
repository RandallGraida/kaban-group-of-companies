# Notification Service Email Delivery (Local/Dev)

The `notification-service` sends verification emails when it receives:

- `POST /internal/events/user-registered`

It sends email via `spring.mail.*`.

Local/dev defaults are configured for MailHog (`localhost:1025`) in `application.properties`; for real delivery, override via env.

This repo tracks `application.example.properties` as a safe template. Keep your real local values in
`application.properties` (ignored by git).

## Quick Diagnostics

- `GET http://localhost:8084/internal/diagnostics/mail`
  - Shows whether SMTP env/properties are configured (`spring.mail.host`, `spring.mail.username`, etc.)

## SMTP Configuration (Environment Variables)

Set these before running `notification-service`:

- `SPRING_MAIL_HOST`
- `SPRING_MAIL_PORT`
- `SPRING_MAIL_USERNAME`
- `SPRING_MAIL_PASSWORD`
- `SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH=true`
- `SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE=true`

Optional:

- `MAIL_FROM` (defaults to `no-reply@kaban.local`)
- `AUTH_BASE_URL` (defaults to `http://localhost:8080`)

## Transitioning to AWS SES (Production)

This service currently sends email via SMTP using Spring Boot Mail (`JavaMailSender`). To move from local
MailHog to AWS SES in production, use SES's SMTP interface first (no code changes required), then optionally
move to the SES API later if you need templates, configuration sets, or deeper delivery telemetry.

### 1) SES Setup Checklist (AWS Console)

- Verify your `MAIL_FROM` identity (domain recommended) in SES.
- If your account is in the SES sandbox, request production access or verify all recipient addresses.
- Set up SPF/DKIM for your domain (improves deliverability).

### 2) Create SES SMTP Credentials

- Create SMTP credentials (separate from IAM access keys).
- Choose your SES region and use the region SMTP endpoint:
  - Example: `email-smtp.ap-southeast-1.amazonaws.com`

### 3) Runtime Configuration (Environment Variables)

Configure the service with SES SMTP:

- `SPRING_MAIL_HOST=email-smtp.<region>.amazonaws.com`
- `SPRING_MAIL_PORT=587` (STARTTLS) or `465` (SMTPS)
- `SPRING_MAIL_USERNAME=<ses-smtp-username>`
- `SPRING_MAIL_PASSWORD=<ses-smtp-password>`
- `SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH=true`
- `SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE=true`
- `MAIL_FROM=<verified-from-address-or-domain>`

### 4) Operational Notes

- Ensure outbound traffic to the SMTP port is allowed (local firewall/VPC security groups).
- Monitor bounces/complaints in SES; production setups often route these to SNS (and then to SQS) for handling.
- Keep `/internal/diagnostics/mail` private; it should confirm that SMTP settings are loaded without exposing secrets.

## Manual Trigger (curl)

```bash
curl -i -X POST http://localhost:8084/internal/events/user-registered \
  -H "Content-Type: application/json" \
  -d '{"email":"you@example.com","verificationToken":"debug-token"}'
```
