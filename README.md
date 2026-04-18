# da-ai-hackathon-04-18 — Chat

Classic web-based real-time chat application built for the DA AI Hackathon (2026-04-18).

## Stack

Java 21 + Spring Boot 3 on the backend, React + TypeScript (Vite) on the frontend, PostgreSQL for storage, Flyway for migrations, Spring Session (DB-backed) for auth, WebSocket (STOMP) for real-time deltas, local filesystem (Docker volume) for uploads. See [`CLAUDE.md`](CLAUDE.md) for stack rationale and repo layout; see [`docs/SPEC.md`](docs/SPEC.md) for the full product spec.

## Getting started

```bash
docker compose up          # start backend + postgres + frontend
docker compose up --build  # rebuild images after dependency changes
```

Once running: backend on `http://localhost:8080`, frontend on `http://localhost:5173` (dev) or served as static assets from the backend container in prod mode. See [`CLAUDE.md`](CLAUDE.md) for detailed commands once scaffolding lands.

## Environment

Copy `.env.example` to `.env` and fill in values. Required variables will be documented in `.env.example` as the project is scaffolded.

## Assumptions / Clarifications

The spec left several behaviors open; the implementation commits to these resolutions. Change them only with a corresponding spec update.

| Topic | Decision |
|---|---|
| Messages authored by a deleted user (in rooms/PMs they did not own) | The message body is preserved; the author is displayed as `[deleted user]`. |
| PM thread after one side unfriends the other | The thread becomes read-only for both participants; history remains visible; new messages are blocked until the friendship is restored. |
| Private-room invitation expiry | Pending invitations auto-expire **7 days** after creation and move to state `expired`. Sender may issue a new invitation. |
| Password reset delivery in local development | The reset token is written to backend stdout (log level `INFO`). No SMTP is configured in dev. Production delivery is out of scope for v1. |
| Room visibility conversion (public ↔ private) | **Not supported in v1.** Visibility is fixed at room creation. This sidesteps the question of what happens to pending join requests / catalog listings on conversion. |

## Known limitations

- No email delivery in any environment; password reset is dev-only.
- Room visibility is immutable (see above).
- Attachments are stored on a local Docker volume — no object storage, no CDN, no antivirus scan.
- Presence granularity is ~30 s (heartbeat interval) plus up to 2 s propagation delay.
