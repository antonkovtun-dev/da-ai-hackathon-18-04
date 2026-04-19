# da-ai-hackathon-04-18 — Chat

Classic web-based real-time chat application built for the DA AI Hackathon (2026-04-18).

---

## Overview

Full-featured group and direct-message chat: user accounts, persistent sessions, friend requests, user bans, public and private rooms with role-based moderation (owner / admin / member), real-time messaging via WebSocket, file/image attachments, unread counts, and online/AFK/offline presence.

---

## Architecture

| Layer | Choice | Rationale |
|---|---|---|
| Backend | Java 21 + Spring Boot 3 | Mature ecosystem, strong typing, built-in DI, battle-tested under load |
| Frontend | React 19 + TypeScript + Vite | Fast HMR in dev, minimal runtime, strong typing end-to-end |
| Real-time | Spring WebSocket / STOMP | Built into Spring; client library is small; sufficient for hackathon scale |
| Database | PostgreSQL 16 | Reliable, FK constraints enforced, full ACID, Testcontainers support |
| ORM | Spring Data JPA (Hibernate) | Reduces boilerplate; Flyway handles schema evolution |
| Auth | Spring Session (DB-backed) | HttpOnly SameSite cookies; sessions survive backend restarts |
| Migrations | Flyway | Version-controlled, auto-applied on startup |
| File storage | Local Docker volume | No object-storage dependency; files are served through a secured Spring endpoint |
| Styling | Tailwind CSS 3 | Utility-first; no design-system overhead in a hackathon |

All authorization checks are enforced server-side. The frontend never makes trust decisions — it only renders what the API returns.

Historical messages load over HTTP (cursor-paginated). Live deltas (new messages, edits/deletes, unread increments, presence changes, membership events) arrive over WebSocket. On reconnect the client resyncs unread state and re-registers the presence heartbeat.

---

## Getting started

### Docker Compose (recommended)

```bash
cp .env.example .env          # edit DB_USER / DB_PASSWORD if desired
docker compose up --build     # build images and start everything
```

Once healthy:
- **Frontend** → http://localhost:80
- **Backend API** → http://localhost:8080
- **PostgreSQL** → `localhost:5432` (database: `chat`)

Flyway migrations run automatically on first backend startup. No seed data is loaded; register the first user through the UI.

```bash
docker compose down           # stop and remove containers (data volumes preserved)
docker compose down -v        # also remove volumes (wipes database and uploads)
```

### Local development (hot-reload)

Run backend and frontend separately for fast iteration:

```bash
# Terminal 1 — backend (requires Docker for Postgres)
docker compose up postgres -d          # start only Postgres
cd backend
./mvnw spring-boot:run                 # hot-reload at :8080

# Terminal 2 — frontend (proxies /api and /ws to :8080)
cd frontend
npm install
npm run dev                            # Vite dev server at :5173
```

### Running tests

```bash
cd backend
./mvnw test                            # requires Docker (Testcontainers starts a Postgres container)
./mvnw test -Dtest=SomeServiceTest     # run a single test class
```

---

## Environment variables

Copy `.env.example` to `.env` before starting. All variables have working defaults for local development; override only what you need.

| Variable | Default (compose) | Description |
|---|---|---|
| `DB_USER` | `chat` | PostgreSQL username |
| `DB_PASSWORD` | `chat` | PostgreSQL password |
| `DB_URL` | `jdbc:postgresql://localhost:5432/chat` | JDBC URL — override when running the backend on the host; inside Docker Compose this is automatically set to `jdbc:postgresql://postgres:5432/chat` |
| `UPLOAD_DIR` | `/data/uploads` | Directory where the backend stores uploaded files — mapped to the `uploads_data` Docker volume |

See `.env.example` for the full file with comments.

---

## Seed / demo data

No seed data is shipped. To reach a meaningful demo state after `docker compose up`:

1. Open [http://localhost:80](http://localhost:80) and register a first user (e.g. `alice`).
2. In a second browser (or incognito tab), register a second user (e.g. `bob`).
3. As `alice`, create a public room named `general`.
4. As `bob`, browse the room catalog and join `general`.
5. Send messages between the two accounts to verify real-time delivery.
6. To test direct messages: as `alice`, send `bob` a friend request; accept it as `bob`; then open the DM from the contacts panel.
7. To test file attachments: use the 📎 button or paste an image into the composer.

---

## Assumptions / Clarifications

The spec left several behaviors open; the implementation commits to these resolutions.

| Topic | Decision |
|---|---|
| Messages authored by a deleted user (in rooms/PMs they did not own) | The message body is preserved; the author is displayed as `[deleted user]`. |
| PM thread after one side unfriends the other | The thread becomes read-only for both participants; history remains visible; new messages are blocked until the friendship is restored. |
| Private-room invitation expiry | Pending invitations auto-expire **7 days** after creation and move to state `expired`. The sender may issue a new invitation. |
| Password reset delivery in local development | The reset token is written to backend stdout (log level `INFO`). No SMTP is configured. Production email delivery is out of scope for v1. |
| Room visibility conversion (public ↔ private) | **Not supported in v1.** Visibility is fixed at room creation. This sidesteps the question of what happens to pending join requests and catalog listings on conversion. |
| Admin demotion of peer admins | Only the room **owner** can demote admins. Admins cannot demote each other. |
| File access on membership loss | Losing room access (kick, ban, room deletion) immediately revokes download access to that room's files — including files the user uploaded themselves. This is enforced at the backend download endpoint, not only in the UI. |

---

## Known limitations

- **No email delivery in any environment.** Password reset tokens are logged to stdout. Wiring an SMTP provider is out of scope for v1.
- **Room visibility is immutable.** Public/private is set at creation and cannot be changed.
- **Local file storage only.** Uploads live in a Docker named volume. There is no object storage, CDN, antivirus scan, or automatic backup. Removing the volume (`docker compose down -v`) permanently deletes all uploaded files.
- **No invite UI.** The invitation system is fully implemented in the backend but the frontend does not yet expose invite sending/accepting flows. Users can only join private rooms if an admin adds them directly (future work).
- **Presence granularity is ~30 s.** The heartbeat fires every 30 seconds. Status transitions (online → AFK → offline) propagate within ~2 s after the heartbeat is processed, so the worst-case lag for a tab-close detection is ~32 s.
- **Single-region, single-instance.** No horizontal scaling, no Redis session store, no shared upload volume. Suitable for demo / hackathon use.
- **Password reset is one-shot in dev.** The token is printed once to the log; there is no resend flow in v1.
