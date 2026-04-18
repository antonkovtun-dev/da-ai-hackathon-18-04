# Chat App — Incremental Delivery Design

**Date:** 2026-04-18
**Status:** Approved

## Context

Hackathon chat application (see [`../../../CLAUDE.md`](../../../CLAUDE.md) for stack, [`../../SPEC.md`](../../SPEC.md) for full product spec). The goal is incremental delivery: each phase produces a working, tested application. This document defines the technical design choices and phase breakdown.

---

## Decisions

| Topic | Decision |
|---|---|
| MVP scope | Auth + public rooms + messaging (Phases 0–3) |
| Testing | Unit (Mockito) + Integration (Testcontainers + @SpringBootTest) |
| Frontend styling | Tailwind CSS |
| Backend structure | Single Maven module, package-based domain separation |
| Delivery strategy | Scaffold-first — Phase 0 establishes the working foundation |

---

## System Architecture

### Runtime layers

```
Browser (React 18 + TypeScript + Vite + Tailwind + STOMP.js + Zustand)
    :5173 dev / served as static assets via backend in production
        ⇄ HTTP (REST) + WebSocket (STOMP)
Backend (Spring Boot 3 / Java 21 / Spring Web + WebSocket + Security + Session + Data JPA)
    :8080
        ⇄ JDBC
PostgreSQL :5432
```

In development: Vite dev server proxies `/api` and `/ws` to `:8080`. In production (Docker): the Maven `frontend-maven-plugin` builds the Vite output and copies it to `backend/src/main/resources/static`; Spring Boot serves it as static assets from the same `:8080` port. No nginx required.

### Backend package structure

```
com.example.chat/
  auth/           AuthController, AuthService, SessionService
  users/          User (entity), UserRepository, UserService
  rooms/          Room, RoomMembership, RoomController, RoomService
  messages/       Message, MessageController, MessageService, MessageWsController
  readstate/      ReadState, ReadStateService               (Phase 4)
  presence/       PresenceTab, PresenceService              (Phase 4)
  friendships/    FriendRequest, Friendship, FriendshipService  (Phase 5)
  config/         SecurityConfig, WebSocketConfig, JpaConfig
```

Each package owns its own authorization logic. Permission checks live in services, not controllers or the frontend.

---

## Phase Roadmap

### Phase 0 — Scaffold
**Delivers:** A running but featureless application.

- `backend/` — Spring Boot 3 Maven project, Java 21, basic `pom.xml`
- `frontend/` — Vite + React 18 + TypeScript + Tailwind CSS
- `docker-compose.yml` — backend service + postgres service, named volumes
- Flyway migration `V1__initial_schema.sql` — creates `users` table (Phase 1 columns included)
- `GET /api/health` → `200 OK { "status": "up" }`
- Frontend renders a placeholder home page at `localhost:5173`
- `CLAUDE.md` Commands section filled in with real commands

**Done when:** `docker compose up --build` starts both services, health endpoint returns 200, Flyway migration runs cleanly, `npm run dev` hot-reloads.

---

### Phase 1 — Auth
**Delivers:** Register, login, logout, persistent session.

#### Backend
- `users` table (from Phase 0 migration) + `SPRING_SESSION` tables (auto-created)
- `POST /api/auth/register` — validate unique email + username, hash password (BCrypt), create user, create session, return user
- `POST /api/auth/login` — verify credentials, create session, `Set-Cookie: SESSION=...` (HttpOnly, SameSite=Lax)
- `POST /api/auth/logout` — invalidate current session only
- `GET /api/auth/me` — returns authenticated user or 401
- Spring Security: permit `/api/health`, `/api/auth/**`, and static resources (`/`, `/assets/**`, `/index.html`); require authentication on everything else
- Validation: email format, username 3–30 chars alphanumeric+underscore, password min 8 chars
- Error responses: `422` with `{ field, message }` for validation failures, `401` for bad credentials

#### Frontend
- `/register` page — email, username, password fields, submit → redirect to `/`
- `/login` page — email + password, submit → redirect to `/`
- Auth context (Zustand store) — holds current user, loaded from `GET /api/auth/me` on app init
- Protected route wrapper — redirects to `/login` if unauthenticated
- Logout button in nav

#### Tests
- `AuthServiceTest` — duplicate email rejected, duplicate username rejected, wrong password returns empty, password stored as hash not plaintext
- `AuthIntegrationTest` — register → login → `GET /api/auth/me` returns user, logout invalidates session (subsequent `GET /api/auth/me` returns 401), duplicate register returns 422

**Done when:** All tests pass, user can register and login in the browser, session persists across page refresh.

---

### Phase 2 — Public Rooms
**Delivers:** Create rooms, browse catalog, join and leave.

#### Backend
- Flyway `V2__rooms.sql` — `rooms`, `room_memberships` tables
- `POST /api/rooms` — create room (name unique, owner auto-joined as OWNER)
- `GET /api/rooms?q=&page=0&size=20` — paginated catalog of public rooms, filter by name substring
- `GET /api/rooms/{id}` — room detail + member list (authenticated, no membership required for public rooms)
- `POST /api/rooms/{id}/join` — adds MEMBER membership; idempotent if already member
- `POST /api/rooms/{id}/leave` — removes membership; owner cannot leave (returns 422)
- Room roles: `OWNER`, `ADMIN`, `MEMBER` — stored as enum in `room_memberships`

#### Frontend
- `/rooms` — catalog page with search input, room cards (name, description, member count), join button
- `/rooms/{id}` — room page shell (message area in Phase 3), member list sidebar, leave button
- `/rooms/new` — create room form
- Nav: "Rooms" link, shows joined rooms list

#### Tests
- `RoomServiceTest` — owner cannot leave, duplicate room name rejected, catalog excludes private rooms (for when Phase 7 adds private rooms)
- `RoomIntegrationTest` — create → appears in catalog → join → membership exists → leave → membership removed; owner leave attempt returns 422

**Done when:** All tests pass, user can create a room, browse the catalog, join and leave.

---

### Phase 3 — Messaging (MVP complete)
**Delivers:** Real-time messaging, history, edit, delete, reply.

#### Backend
- Flyway `V3__messages.sql` — `messages` table with index on `(room_id, created_at)`
- `GET /api/rooms/{id}/messages?before={uuid}&limit=50` — cursor-based pagination; returns messages older than `before` (exclusive), ordered by `created_at` DESC, max 50. Requires room membership.
- `PATCH /api/messages/{id}` — edit own message content; sets `edited_at`; returns updated message
- `DELETE /api/messages/{id}` — soft-delete (sets `deleted_at`); own message or room ADMIN/OWNER
- WebSocket endpoint `/ws` (SockJS) — authenticated via session cookie on HTTP upgrade
- STOMP subscribe `/topic/rooms/{roomId}` — member-only; enforced in `MessageWsController`
- STOMP send `/app/rooms/{roomId}/messages` — creates message, broadcasts `MESSAGE_CREATED` event to `/topic/rooms/{roomId}`
- Broadcast events: `MESSAGE_CREATED`, `MESSAGE_EDITED`, `MESSAGE_DELETED`
- Deleted messages: `content` replaced with `null`, `deleted_at` set — UI shows `[message deleted]` placeholder

#### WebSocket event payloads
```json
MESSAGE_CREATED: { type, id, roomId, authorId, authorUsername, content, replyToId, createdAt }
MESSAGE_EDITED:  { type, id, roomId, content, editedAt }
MESSAGE_DELETED: { type, id, roomId }
```

#### Frontend
- Message timeline in `/rooms/{id}` — renders messages oldest-to-newest, infinite scroll upward loads older pages via cursor
- Message composer — textarea, Enter sends, Shift+Enter newline
- Message actions — hover to show edit / delete (own messages), delete only (admin)
- Reply — click reply on a message, shows quoted preview in composer, sends `replyToId`
- Edited indicator — `(edited)` label on messages with `editedAt`
- Deleted placeholder — `[message deleted]` for soft-deleted messages
- WebSocket connection managed in a React context; reconnect on disconnect, resubscribe to active room
- Optimistic update: message appears immediately on send, replaced by server event

#### Tests
- `MessageServiceTest` — edit by non-author returns 403, delete by non-author non-admin returns 403, soft-delete sets deleted_at, reply_to validates message is in same room
- `MessagingIntegrationTest` — send message → appears in `GET /messages`, edit updates content, delete soft-deletes, cursor pagination returns correct page (boundary conditions)
- `MessagingWebSocketTest` — two users connect, user A sends → user B receives `MESSAGE_CREATED`, edit → both receive `MESSAGE_EDITED`, delete → both receive `MESSAGE_DELETED`

**Done when:** All tests pass, two browser tabs can exchange messages in real time, history loads on scroll, edit and delete work.

---

## Post-MVP Phases (summary)

| Phase | Delivers |
|---|---|
| 4 | Presence (heartbeat, online/AFK/offline) + unread count badges |
| 5 | Friend requests, user-to-user bans, direct messages |
| 6 | File/image attachments, secure download with access control |
| 7 | Private rooms, invitations, room bans, moderation UI, account deletion |

Each phase follows the same pattern: Flyway migration → backend service + controller → integration tests → unit tests → frontend.

---

## Data Model (MVP)

```sql
-- Phase 1
users (
  id uuid PK,
  email varchar UNIQUE NOT NULL,
  username varchar UNIQUE NOT NULL,
  password_hash varchar NOT NULL,
  created_at timestamptz NOT NULL,
  deleted_at timestamptz
)

-- Phase 2
rooms (
  id uuid PK,
  name varchar UNIQUE NOT NULL,
  description text,
  is_public boolean NOT NULL DEFAULT true,
  owner_id uuid → users NOT NULL,
  created_at timestamptz NOT NULL
)

room_memberships (
  id uuid PK,
  room_id uuid → rooms NOT NULL,
  user_id uuid → users NOT NULL,
  role varchar NOT NULL,  -- OWNER | ADMIN | MEMBER
  joined_at timestamptz NOT NULL,
  UNIQUE(room_id, user_id)
)

-- Phase 3
messages (
  id uuid PK,
  room_id uuid → rooms NOT NULL,
  author_id uuid → users,  -- nullable: NULL = deleted user
  content text,             -- nullable: NULL = deleted message
  reply_to_id uuid → messages,
  created_at timestamptz NOT NULL,
  edited_at timestamptz,
  deleted_at timestamptz,
  INDEX(room_id, created_at)
)
```

**Key design decisions:**
- UUIDs everywhere — no sequential ID guessing
- Soft-delete on `users` and `messages` via `deleted_at` — preserves FK integrity
- `author_id` nullable — deleted users' messages display as `[deleted user]`
- `content` nullable — soft-deleted messages display as `[message deleted]`
- Cursor pagination uses `(room_id, created_at)` index

---

## Testing Strategy

### Structure
```
src/test/java/com/example/chat/
  unit/
    AuthServiceTest
    RoomServiceTest
    MessageServiceTest
  integration/
    IntegrationTestBase     ← @Testcontainers, static PG container
    TestDataBuilder         ← fluent builders for User, Room, Message
    AuthIntegrationTest
    RoomIntegrationTest
    MessagingIntegrationTest
    MessagingWebSocketTest
```

### Rules
- Unit tests: JUnit 5 + Mockito, no Spring context, no DB
- Integration tests: `@SpringBootTest` + Testcontainers PostgreSQL, `MockMvc` for REST, STOMP test client for WebSocket
- `IntegrationTestBase` starts one Postgres container per test suite (static field) — fast
- Each phase adds its own test classes; existing tests must not break
- Every phase is considered "done" only when all tests in its classes pass

---

## Verification Checklist (per phase)

- [ ] `docker compose up --build` starts cleanly
- [ ] Flyway migration applies without error
- [ ] All unit tests pass (`./mvnw test`)
- [ ] All integration tests pass
- [ ] Golden path works manually in browser (described in each phase's "Done when")
- [ ] No regressions in prior phases' tests
