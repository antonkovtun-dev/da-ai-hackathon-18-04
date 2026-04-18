# CLAUDE.md

Guidance for Claude Code (claude.ai/code) when working in this repository. Describes *how* to build this project. For *what* to build, see [`docs/SPEC.md`](docs/SPEC.md).

---

## Project

Classic web-based real-time chat application, built under hackathon time pressure. The goal is a complete, runnable, production-honest system — not a demo. `docker compose up` from the repo root must bring the whole thing online.

---

## Stack

| Layer | Choice |
|---|---|
| Backend | Java 21 + Spring Boot 3 |
| Frontend | React + TypeScript (Vite) |
| Real-time | WebSocket (Spring WebSocket / STOMP) |
| Database | PostgreSQL |
| ORM | Spring Data JPA (Hibernate) |
| Migrations | Flyway |
| Auth | Database-backed sessions (Spring Session + PostgreSQL) |
| Build | Maven (backend), npm/Vite (frontend) |
| File storage | Local filesystem (Docker volume mount) |
| Styling | Minimal component library or plain CSS |

Deviate from this stack only with a clear rationale that keeps Docker Compose startup intact.

---

## Expected repo layout

```
.
├── backend/            # Spring Boot app (Maven, Java 21)
│   ├── pom.xml
│   └── src/
├── frontend/           # React + TypeScript (Vite)
│   ├── package.json
│   └── src/
├── docker-compose.yml  # backend + postgres + (optional) nginx serving built frontend
├── .env.example
├── docs/SPEC.md        # product spec (functional requirements, edge cases)
├── README.md
└── CLAUDE.md
```

Ports: backend `:8080`, frontend dev server `:5173` (proxies `/api` and `/ws` to backend), Postgres `:5432`. In Docker, frontend is built and served as static assets; the dev server is for local hot-reload only.

---

## Commands

> Scaffolding not yet generated. Once `backend/pom.xml` and `frontend/package.json` exist, this section must list:
> - how to start the full stack (`docker compose up`, `docker compose up --build`)
> - how to run backend only in dev (`./mvnw spring-boot:run` inside `backend/`)
> - how to run frontend only in dev (`npm run dev` inside `frontend/`)
> - how to run tests: all (`./mvnw test`) and single class (`./mvnw test -Dtest=SomeServiceTest`)
> - how to apply migrations manually (`./mvnw flyway:migrate`)
>
> Update this section as part of the scaffolding PR.

Migrations run automatically on application startup via Flyway. Seed data via `data.sql` or a Spring Boot `ApplicationRunner` bean.

---

## Architecture

### Backend modules (domain boundaries)

Each module owns its own authorization — do not scatter permission logic into the frontend or middleware only.

- **auth** — registration, login, logout, password hashing, password reset
- **sessions** — multi-session lifecycle, revocation, browser/IP/last-activity metadata
- **users** — profile, account deletion, username immutability
- **friendships** — friend request lifecycle (pending → accepted / declined / canceled), friend removal
- **bans** — user-to-user bans (blocks future contact, freezes existing PM history)
- **rooms** — creation, visibility (public/private), catalog, search, roles (owner > admin > member)
- **memberships** — join, leave, kick (= ban), role management
- **invitations** — private room invite lifecycle
- **messages** — create, edit, delete, reply, cursor-based pagination
- **attachments** — upload, secure download, size/type validation, path safety
- **read-state** — per-user per-chat unread tracking, cleared on open
- **presence** — tab heartbeat model, online / AFK / offline derivation
- **moderation** — room bans, message deletion by admins, ban audit log

### Database schema (key entities)

```
users                  sessions               password_reset_tokens
friend_requests        friendships            user_blocks
rooms                  room_memberships       room_bans
room_invitations       messages               attachments
read_states            presence_tabs
```

### Real-time model

- Historical pages load over HTTP (cursor-paginated)
- Live deltas arrive over WebSocket: new messages, edits/deletes, unread counts, presence, membership/moderation events
- WebSocket reconnect must resync unread state and re-establish presence heartbeat

### Presence model

- Each browser tab sends a heartbeat every ~30 s
- Tab inactive (no interaction) > 1 min → tab marked AFK
- All tabs AFK → user status: AFK
- All tabs closed / heartbeat expired → user status: offline
- At least one active tab → user status: online
- Status propagates to other connected clients within 2 s

### File storage

Files are stored on a local mounted volume. Download endpoints enforce authorization at request time (not just in UI). If a user loses room access, they lose download access for that room's files immediately — including files they uploaded themselves.

---

## Security requirements

- Passwords hashed with bcrypt or argon2
- Secure, HttpOnly, SameSite session cookies
- All authorization checks on the server side — never trust client claims
- Input validation on all API endpoints
- Upload: validate file size and MIME type server-side
- Safe file path construction (prevent path traversal)
- No history, file, or membership access via ID-guessing (use opaque tokens or verify membership before serving)
- Sanitize or safely render user-generated content (prevent XSS)

---

## Hackathon pacing

- End-to-end runnable beats per-module polish. A thin slice (register → login → create room → send message → see it in another browser) delivered early is worth more than any one module done thoroughly.
- Write integration tests for the items in *Testing priorities*; skip unit-test exhaustiveness elsewhere.
- Smoke-test every vertical slice by hand through the UI before calling it done.
- When forced to choose: correctness > security > ergonomics > visual polish.
- Defer unless a slice depends on them: admin audit log UI, presence AFK precision below 1 minute, bulk moderation tools.

---

## Testing priorities

Cover these in automated tests or integration assertions:

- Session isolation (logout/revoke only affects target session)
- Room permission matrix (member vs admin vs owner)
- Room ban: kicked user cannot rejoin
- User-to-user ban: PM blocked, history frozen
- PM eligibility (friends-only + no mutual ban)
- File download authorization (non-member gets 403)
- Account deletion cleanup (cascade correctness)
- Presence derivation (multi-tab scenarios)
- Unread count increment and clear

---

## Implementation order

1. Scaffold backend (Spring Boot 3 + Java 21 + Maven) and frontend (Vite + React + TS) skeletons
2. Docker Compose scaffolding + health checks
3. Database schema + Flyway migrations
4. Auth + sessions (register, login, logout, persistent cookie, multi-session revoke)
5. Users (profile, account deletion)
6. Rooms + membership + roles + moderation
7. Friendships + user bans + PM eligibility
8. Messaging (create, edit, delete, reply, cursor pagination, offline delivery)
9. Attachments (upload, secure download, access control)
10. Presence (heartbeat model, tab tracking, status propagation)
11. Unread state + real-time WebSocket events
12. Admin modals + room management UI
13. README polish, env samples, smoke validation

---

## README requirements

The README must include:

- Project overview
- Architecture summary and stack rationale
- `docker compose up` instructions
- Environment variables reference (with a `.env.example`)
- Seed/demo data instructions if applicable
- **Assumptions / Clarifications** section resolving every ambiguous spec point (deleted-user message policy, PM thread behavior after friend removal, invite expiry, password reset delivery in dev, room visibility conversion)
- Known limitations
