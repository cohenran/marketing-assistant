# Transparent Growth — Reddit Marketing & Listening Assistant

Spring Boot batch app. Hourly: listens to dating subreddits for pain points (read-only
research), generates **honest, disclosed** promo drafts via Claude for places that allow
self-promotion, and emails you the digest. Nothing auto-posts — you review and paste.

## Pipeline

1. **PainPointService** → Reddit OAuth (app-only) keyword search, score filter.
2. **PromotionTargetService** → curated allow-list + Facebook-theme manual fallback.
3. **DraftGenerator** → Anthropic `claude-opus-4-8`, one disclosed draft per target.
4. **EmailService** → Gmail SMTP digest to `duodatingapplication@gmail.com`.

## Setup

### 1. Reddit app (script/confidential)
Create at https://www.reddit.com/prefs/apps → type "script". Note the client id + secret.
Set a real username in `app.reddit.user-agent` (Reddit blocks generic agents).

### 2. Gmail App Password
Enable 2FA, then create an App Password (Google Account → Security → App passwords).
Use that as `MAIL_PASSWORD`, not your login password.

### 3. Environment variables
```bash
export ANTHROPIC_API_KEY=sk-ant-...        # read by the Anthropic SDK (fromEnv)
export REDDIT_CLIENT_ID=...
export REDDIT_CLIENT_SECRET=...
export MAIL_USERNAME=you@gmail.com
export MAIL_PASSWORD=your-16-char-app-password
```

### 4. Run
```bash
mvn spring-boot:run
```
Runs on the cron in `application.yml` (`0 0 * * * *` = top of every hour).
To test immediately, temporarily set it to `0 * * * * *` (every minute) or trigger
`MarketingJob.run()`.

## Deploy on Ubuntu
Build `mvn clean package`, copy the jar, run under systemd with the env vars set in the
unit file. `java -jar reddit-marketing-assistant-1.0.0.jar`.

## Notes / things to verify
- **Anthropic SDK version**: `pom.xml` pins `com.anthropic:anthropic-java` 2.9.0 — confirm
  the latest on Maven Central and adjust. If imports don't resolve, the model package path
  may differ between SDK majors (`com.anthropic.models.messages.*`).
- **Reddit search**: `q` caps near 512 chars — keep the keyword list modest.
- **Facebook**: Graph API can't read arbitrary public groups, so the app uses the
  theme-based manual fallback (`app.facebook-themes`) instead of scraping.
- Only post where the platform's rules explicitly allow self-promotion.
```
