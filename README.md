# da-ai-hackathon-04-18 — Chat

Classic web-based real-time chat application built for the DA AI Hackathon (2026-04-18).

---

## Overview

Full-featured group and direct-message chat: user accounts, persistent sessions, friend requests, user bans, public and private rooms with role-based moderation (owner / admin / member), real-time messaging via WebSocket, file/image attachments, unread counts, and online/AFK/offline presence.

---

## Feature checklist — spec vs. implementation

### §1 User accounts
| Requirement | Status |
|---|---|
| Register with unique email / username / password | ✅ Done |
| Sign in with email + password | ✅ Done |
| Persistent login across browser close | ✅ Done |
| Sign out invalidates current session only | ✅ Done |
| Password change (authenticated) | ✅ Done |
| Password reset (token logged to backend stdout) | ✅ Done |
| Account deletion with full cascade + `[deleted user]` for authored messages | ✅ Done |

### §2 Sessions
| Requirement | Status |
|---|---|
| Multiple simultaneous sessions | ✅ Done |
| Session list (browser hint, IP, last-active) | ✅ Done |
| Revoke any individual session | ✅ Done |
| Logout only affects current session | ✅ Done |

### §3 Contacts / friends
| Requirement | Status |
|---|---|
| Send friend request by username (with optional message) | ✅ Done |
| Send friend request from room member list | ✅ Done |
| Accept / decline / cancel requests | ✅ Done |
| Remove friend | ✅ Done |
| Friend removal makes existing DM thread read-only | ✅ Done |

### §4 User-to-user bans
| Requirement | Status |
|---|---|
| Block user (terminates friendship, freezes DM, blocks future contact) | ✅ Done |
| Unblock restores eligibility (no auto-restore of friendship) | ✅ Done |

### §5 Rooms
| Requirement | Status |
|---|---|
| Public room catalog (browse, search, paginate, join) | ✅ Done |
| Private rooms hidden from catalog | ✅ Done |
| Globally unique room names | ✅ Done |
| Owner is singular, cannot be demoted or leave | ✅ Done |
| Admins can delete messages, kick/ban/unban members, view ban list | ✅ Done |
| Owner can promote/demote admins and delete the room | ✅ Done |
| Kicking = ban; kicked user cannot rejoin until unbanned | ✅ Done |
| Room deletion removes all messages, files, attachments | ✅ Done |
| Public ↔ private conversion | ⏭ Skipped (out of scope per spec) |

### §6 Room invitations
| Requirement | Status |
|---|---|
| Invite user by username (owner/admin only) | ❌ Not built |
| Invitation lifecycle (pending → accepted / declined / canceled / expired) | ❌ Not built |
| Invitations auto-expire after 7 days | ❌ Not built |
| Private rooms joinable via invitation | ❌ Not built |

### §7 Messaging
| Requirement | Status |
|---|---|
| Plain text, multiline, emoji (max 3 KB) | ✅ Done |
| File attachments (images ≤ 3 MB, files ≤ 20 MB) via button or paste | ✅ Done |
| Optional comment on attachment | ✅ Done |
| Edit own messages (shows "edited" indicator) | ✅ Done |
| Delete own messages; admins can delete any message | ✅ Done |
| Cursor-based infinite scroll (stable under live inserts) | ✅ Done |
| Offline delivery (messages persist; delivered on reconnect) | ✅ Done |
| Reply to a message (quoted/outlined in UI) | ❌ Not built |

### §8 Unread state
| Requirement | Status |
|---|---|
| Unread count badge per room and DM thread | ✅ Done |
| Cleared when user opens the chat | ✅ Done |
| Real-time increment via WebSocket | ✅ Done |

### §9 UI layout
| Requirement | Status |
|---|---|
| Sidebar (rooms + DMs + unread badges) always visible | ✅ Done |
| Central message timeline | ✅ Done |
| Right member panel (room members + presence dots) | ✅ Done |
| Bottom message composer | ✅ Done |
| Admin modals (ban list, role management) | ✅ Done |

### Presence
| Requirement | Status |
|---|---|
| Per-tab heartbeat model | ✅ Done |
| Online / AFK / offline derivation | ✅ Done |
| Status propagates to other clients within ~2 s | ✅ Done |
| Visual presence indicators (dots in member panel + DM sidebar) | ✅ Done |

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
- **Frontend** → http://localhost
- **Backend API** → http://localhost:8080
- **PostgreSQL** → `localhost:5432` (database: `chat`)

Flyway migrations run automatically on first backend startup. No seed data is loaded; register the first user through the UI.

```bash
docker compose down           # stop and remove containers (data volumes preserved)
docker compose down -v        # also remove volumes (wipes database and uploads)
```

### Local development (hot-reload)

Run backend and frontend separately for fast iteration:

**Prerequisites:** JDK 21 and Node 18+ must be installed on the host.

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

| Variable | Local dev default | Description |
|---|---|---|
| `DB_USER` | `chat` | PostgreSQL username |
| `DB_PASSWORD` | `chat` | PostgreSQL password |
| `DB_URL` | `jdbc:postgresql://localhost:5432/chat` | JDBC URL — override when running the backend on the host; inside Docker Compose this is automatically set to `jdbc:postgresql://postgres:5432/chat` |
| `UPLOAD_DIR` | `/data/uploads` | Directory where the backend stores uploaded files — mapped to the `uploads_data` Docker volume |

See `.env.example` for the full file with comments.

---

## Seed / demo data

No seed data is shipped. To reach a meaningful demo state after `docker compose up`:

1. Open [http://localhost](http://localhost) and register a first user (e.g. `alice`).
2. In a second browser (or incognito tab), register a second user (e.g. `bob`).
3. As `alice`, create a public room named `general`.
4. As `bob`, browse the room catalog and join `general`.
5. Send messages between the two accounts to verify real-time delivery. Use separate browsers (or one normal + one incognito window) — sessions are cookie-scoped, so two tabs in the same browser share the same login.
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

- **No email delivery.** Password reset tokens are logged to backend stdout. No SMTP integration.
- **Local file storage only.** Uploads live in a Docker named volume — no object storage, no CDN, no antivirus scan. `docker compose down -v` permanently deletes all uploads.
- **Presence granularity is ~30 s.** The heartbeat fires every 30 s; worst-case lag for a tab-close → offline transition is ~32 s.
- **Single-region, single-instance.** No horizontal scaling, no Redis session store. Suitable for demo / hackathon use.
- **Private rooms are API-only.** You can create a private room via `POST /api/rooms` with `"isPrivate": true`, but there is no UI to invite members or join one.
