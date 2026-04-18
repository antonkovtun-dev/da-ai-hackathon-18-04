# Phase 2 — Rooms + Real-Time Messaging Design

**Goal:** Deliver the core chat loop end-to-end: public room catalog, full role hierarchy, text messaging with edit/delete, and real-time delivery via WebSocket (STOMP).

**Out of scope for this phase:** attachments, replies, private rooms, invitations, friendships, presence, unread persistence.

---

## Scope

- Public room catalog: browse (search + paginate), create, join, leave
- Room roles: OWNER > ADMIN > MEMBER
- Owner: delete room, kick/ban/unban any member, promote/demote admins
- Admin: delete any message in their room, kick/ban/unban members (not owner/other admins)
- Member: send messages, edit/delete own messages only
- Text messaging: send (max 3 KB), edit own, delete own (soft delete, content cleared)
- Real-time via WebSocket (STOMP): new/edited/deleted messages + membership events broadcast to room subscribers
- Unread badge on sidebar (count only; no persistence — resets on page reload; full persistence in Phase 3)

---

## Database schema — V2 migration

```sql
CREATE TABLE rooms (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    owner_id    UUID NOT NULL REFERENCES users(id),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE room_memberships (
    id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    room_id   UUID NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    user_id   UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role      VARCHAR(10) NOT NULL CHECK (role IN ('OWNER', 'ADMIN', 'MEMBER')),
    joined_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (room_id, user_id)
);

CREATE TABLE room_bans (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    room_id    UUID NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    banned_by  UUID NOT NULL REFERENCES users(id),
    reason     TEXT,
    banned_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (room_id, user_id)
);

CREATE TABLE messages (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    room_id    UUID NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    author_id  UUID NOT NULL REFERENCES users(id),
    content    TEXT CHECK (length(content) <= 3000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    edited_at  TIMESTAMPTZ,
    deleted_at TIMESTAMPTZ
);

CREATE INDEX idx_messages_room_created ON messages(room_id, created_at DESC);
```

One Flyway migration: `V2__rooms_and_messages.sql`.

---

## Backend architecture

### Package structure

```
com.example.chat.
  rooms/         Room, RoomRepository, RoomService, RoomController
  memberships/   RoomMembership, RoomRole (enum), RoomMembershipRepository, MembershipService
  moderation/    RoomBan, RoomBanRepository, ModerationService, ModerationController
  messages/      Message, MessageRepository, MessageService, MessageController
  websocket/     WebSocketConfig, RoomEventPublisher
```

Each package owns its own authorization — no cross-package permission checks.

### REST API

**Rooms (`RoomController`):**
| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/rooms` | any member | Catalog; `?search=&page=&size=` |
| POST | `/api/rooms` | authenticated | Create; requester auto-joins as OWNER |
| GET | `/api/rooms/{id}` | authenticated | Room detail + member count |
| DELETE | `/api/rooms/{id}` | OWNER | Delete room and all contents |
| POST | `/api/rooms/{id}/join` | authenticated | Join public room (not banned) |
| POST | `/api/rooms/{id}/leave` | MEMBER/ADMIN | Leave room (OWNER cannot leave) |

**Memberships (`RoomController`):**
| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/rooms/{id}/members` | member | List members with roles |
| PUT | `/api/rooms/{id}/members/{userId}/role` | OWNER | Promote/demote to ADMIN |

**Moderation (`ModerationController`):**
| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/rooms/{id}/bans` | OWNER or ADMIN | Kick + ban; body: `{ userId, reason? }` |
| DELETE | `/api/rooms/{id}/bans/{userId}` | OWNER or ADMIN | Unban |
| GET | `/api/rooms/{id}/bans` | OWNER or ADMIN | List bans |

Admin cannot ban/unban OWNER or other ADMINs (403).

**Messages (`MessageController`):**
| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/rooms/{id}/messages` | member | History; `?before={uuid}&limit=50` |
| POST | `/api/rooms/{id}/messages` | member | Send message |
| PATCH | `/api/messages/{id}` | author | Edit own message |
| DELETE | `/api/messages/{id}` | author or OWNER/ADMIN | Soft-delete |

Deleted message: `content` set to `null`, `deleted_at` set to now. Response body for deleted messages returns `{ ..., content: null, deleted: true }`.

---

## WebSocket real-time model

**Config:**
- STOMP endpoint: `/ws` (SockJS enabled)
- App destination prefix: `/app`
- Simple in-memory broker on `/topic`
- Auth: STOMP CONNECT is rejected if no valid `SESSION` cookie

**Client flow:**
1. On entering `/rooms/:id` — subscribe to `/topic/rooms/{roomId}`
2. On leaving — unsubscribe
3. Send messages via HTTP POST (not STOMP send)

**Event envelope** (all events on `/topic/rooms/{roomId}`):
```json
{ "type": "MESSAGE_NEW",    "message": { id, authorId, authorUsername, content, createdAt } }
{ "type": "MESSAGE_EDITED", "messageId": "...", "content": "...", "editedAt": "..." }
{ "type": "MESSAGE_DELETED","messageId": "..." }
{ "type": "MEMBER_JOINED",  "userId": "...", "username": "..." }
{ "type": "MEMBER_LEFT",    "userId": "...", "username": "..." }
{ "type": "MEMBER_KICKED",  "userId": "...", "username": "..." }
```

`RoomEventPublisher` wraps `SimpMessagingTemplate` and is called by `MessageService` and `ModerationService` after DB writes.

---

## Frontend

### Routes

| Path | Component | Description |
|---|---|---|
| `/` | redirect to `/rooms` | |
| `/rooms` | `RoomCatalogPage` | Browse + search public rooms, create button |
| `/rooms/new` | `CreateRoomPage` | Name + description form |
| `/rooms/:id` | `ChatPage` | Two-panel shell |

### Layout (`ChatPage`)

```
┌────────────────────────────────────────────────────────┐
│ Chat App            [Browse Rooms]    @username  Logout │
├─────────────────┬──────────────────────────────────────┤
│ My Rooms        │  #room-name              [⋮ actions] │
│ ─────────────   │  ──────────────────────────────────  │
│ # general       │  alice: hello                        │
│ # random   (3)  │  bob: hey!                           │
│ # dev           │  [edited] alice: never mind          │
│                 │                    [message deleted]  │
│ [+ Create Room] │  ────────────────────────────────── │
│                 │  [ type a message...          Send ] │
└─────────────────┴──────────────────────────────────────┘
```

### Key frontend files

```
src/
  api/
    rooms.ts          — CRUD + join/leave/members/bans
    messages.ts       — send/edit/delete/paginate
  store/
    roomStore.ts      — joined rooms list, active room
    messageStore.ts   — messages per room, pagination cursor
  hooks/
    useRoomSocket.ts  — STOMP subscribe/unsubscribe, dispatch events to messageStore
  pages/
    RoomCatalogPage.tsx
    CreateRoomPage.tsx
    ChatPage.tsx
  components/
    Sidebar.tsx           — room list + unread badges
    MessageList.tsx       — virtualized timeline, infinite scroll up
    MessageItem.tsx       — text, edit/delete actions, deleted placeholder
    MessageComposer.tsx   — textarea + send button
    MemberPanel.tsx       — member list + role/kick/ban actions (collapsible)
```

### Frontend behaviors

- **On entering room:** fetch last 50 messages (HTTP), subscribe to STOMP topic, scroll to bottom
- **New message event:** append to bottom; scroll to bottom only if user was already at bottom
- **Edit:** click own message → inline textarea → PATCH → replace content locally
- **Delete:** click → confirmation → DELETE → replace with `[message deleted]` placeholder
- **Infinite scroll:** scrolling to top triggers `GET /api/rooms/{id}/messages?before={oldestId}`, prepends results
- **Unread badge:** increment on `MESSAGE_NEW` event if room not currently active; clear on room open (in-memory only for Phase 2)
- **Kick event:** if kicked user is self → redirect to `/rooms` with toast "You were removed from this room"

---

## Authorization summary

| Action | MEMBER | ADMIN | OWNER |
|---|---|---|---|
| Read messages | ✅ | ✅ | ✅ |
| Send message | ✅ | ✅ | ✅ |
| Edit own message | ✅ | ✅ | ✅ |
| Delete own message | ✅ | ✅ | ✅ |
| Delete any message | ❌ | ✅ | ✅ |
| Kick/ban member | ❌ | ✅ (not OWNER/ADMIN) | ✅ (anyone) |
| Unban | ❌ | ✅ | ✅ |
| Promote/demote admin | ❌ | ❌ | ✅ |
| Delete room | ❌ | ❌ | ✅ |
| Leave room | ✅ | ✅ | ❌ |

---

## Testing priorities

**Room access control:**
- Non-member cannot GET messages or POST message (403)
- Banned user cannot join room (403)
- Kicked user cannot rejoin until unbanned

**Role permission matrix:**
- Member cannot kick, ban, promote, or delete others' messages (403)
- Admin can delete any message; can kick/ban members; cannot touch owner/other admins
- Only owner can delete room; only owner can promote/demote admins

**Message lifecycle:**
- Edit returns 403 on others' messages
- Delete soft-deletes: content null, deleted_at set
- Cursor pagination: correct ordering, no duplicates across pages

**WebSocket:**
- Unauthenticated STOMP connect rejected
- Message send triggers broadcast to `/topic/rooms/{roomId}`
