# Marketing Copy Generator

Spring Boot batch app. Runs daily, but each marketing channel only generates when it's
**due** (per-channel cadence). It writes honest, channel-native drafts via Claude — in
**your** voice, suggesting **your** screenshots, and **different from past posts** — then
emails them to you. You edit and post manually. No Reddit, no scraping, no auto-posting.

## Pipeline

1. **InputFiles** → reads your `pain-points.txt`, `voice.txt`, `assets.txt`.
2. **MarketingJob** → daily; picks channels that are due by cadence (or forced via `--run-now`).
3. **DraftGenerator** → Anthropic `claude-opus-4-8`; one draft per due channel, grounded in
   brief + pain points + voice + assets + the last 3 posts for that channel (to stay fresh).
4. **GeneratedPost (H2 DB)** → records every generated draft (history + de-dup).
5. **EmailService** → emails the digest of what was generated this run.

## Channels & cadence (`application.yml`)

| Channel | Cadence |
|---|---|
| TikTok/Reels, Twitter/X, LinkedIn | weekly |
| Email/Newsletter blurb | biweekly |
| Blog (SEO), r/SideProject, Indie Hackers, Paid ad copy | monthly |
| App Store listing | quarterly |
| Product Hunt, Show HN | **manual only** (launches) |

Cadence is `cadence-days` per channel: `7/14/30/90`, or `0` = manual-only.

## Setup

1. **Product brief** — edit `app.product` in `application.yml` (name, **url**, tagline, …).
   The URL is the CTA in every draft.
2. **Your inputs** — fill `pain-points.txt`, `voice.txt`, `assets.txt`. See
   **[VOICE-AND-ASSETS.md](VOICE-AND-ASSETS.md)** for the prompts to fill voice + assets well.
3. **Gmail App Password** — enable 2FA, create an App Password, use as `MAIL_PASSWORD`.
4. **Env vars:**
   ```bash
   export ANTHROPIC_API_KEY=sk-ant-...
   export MAIL_USERNAME=you@gmail.com
   export MAIL_PASSWORD=your-16-char-app-password
   export PEXELS_API_KEY=...        # optional — appends stock-photo options to each draft
   ```

## Smoke test (no Gmail needed)

Try output before setting up email — needs only `ANTHROPIC_API_KEY`:
```bash
export ANTHROPIC_API_KEY=sk-ant-...
./smoke-test.sh "LinkedIn"     # or no arg = first channel
```
Writes `smoke-test-output.txt` (and prints to console), no email, nothing saved to the DB.
Then the app exits.

## Run

```bash
./run.sh                                   # normal: daily cron, generates due channels
./run.sh --run-now="Product Hunt,Show HN"  # launch run: generate these now, ignore cadence
./run.sh --dry-run --run-now="TikTok / Reels scripts"   # same as smoke-test.sh
```

Scheduled cron is `0 0 9 * * *` (daily 09:00). On a quiet day nothing is due → no email.

## Deploy on Ubuntu

```bash
sudo ./deploy.sh
```
For a one-shot launch run on the server:
```bash
java -jar /opt/marketing-assistant/reddit-marketing-assistant-1.0.0.jar --run-now="Product Hunt"
```

## How the de-dup works

Each run, the generator is shown the last 3 drafts for that channel and told to write
something materially different. History lives in the H2 file DB under `data/`. This is how
"rewrite per place / don't repeat" is enforced — every post is fresh.

## Notes / verify
- `pain-points.txt`, `voice.txt`, `assets.txt`, and `data/` are **git-ignored** (your
  personal content + DB). Keep `.example` copies if you want them tracked.
- **Anthropic SDK version**: `pom.xml` pins `com.anthropic:anthropic-java` 2.9.0 — confirm
  latest on Maven Central; package path is `com.anthropic.models.messages.*`.
- Binds **no port** — safe alongside your dating app.
- Generation only. Editing and posting stay 100% manual — the app never posts anywhere.
