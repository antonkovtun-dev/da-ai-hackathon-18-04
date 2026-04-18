# Product spec

Authoritative description of *what* the chat application does. Implementation guidance (stack, repo layout, pacing) lives in [`../CLAUDE.md`](../CLAUDE.md). Resolved ambiguities live in [`../README.md`](../README.md) under "Assumptions / Clarifications".

---

## Functional requirements

### 1. User accounts

- Register with email (unique), username (unique, immutable), password
- Sign in with email + password
- Persistent login across browser close (session cookie)
- Sign out invalidates current session only
- Password change (authenticated), password reset (token-based, dev: log to console)
- Account deletion: removes owned rooms + their messages/files, removes memberships in other rooms, invalidates all sessions. For messages left in non-owned rooms/PMs: mark author as `[deleted user]` (see README → Assumptions).

### 2. Sessions

- Multiple simultaneous sessions across devices/browsers
- Session list shows browser hint, IP, last-active timestamp
- Revoke any session except — optionally — current one
- Logout never affects other sessions

### 3. Contacts / friends

- Send friend request by username (also from room member list), with optional message
- Recipient accepts, declines, or ignores; sender can cancel
- Accepted friendship is bidirectional
- Either side can remove friend
- Friend removal stops new PMs; existing PM thread becomes read-only (see README → Assumptions)

### 4. User-to-user bans

- Banning another user immediately: terminates friendship, blocks future friend requests, blocks new PMs, freezes existing PM thread (read-only for both sides)
- Unblocking restores contact eligibility but does not auto-restore friendship

### 5. Rooms

- Public rooms: visible in catalog (name, description, member count, search); any authenticated non-banned user may join
- Private rooms: not visible in catalog; join by invitation only
- Room names globally unique
- Owner is singular, always admin, cannot be demoted or leave (must delete room instead)
- Admins: delete messages, remove/ban/unban members, view ban list with who-banned-whom, demote other admins
- Owner additionally: remove any admin/member, delete room
- Kicking a member = ban; they cannot rejoin until explicitly unbanned
- Room deletion permanently removes all messages, files, and attachments
- Public ↔ private conversion is **not supported in v1** (see README → Assumptions)

### 6. Room invitations

- Owner/admin invites user by username
- Cannot invite already-member or room-banned user (fail cleanly)
- Invitation states: pending → accepted / declined / canceled / expired
- Invitations auto-expire after 7 days (see README → Assumptions)
- Accepted invitation creates membership

### 7. Messaging

- Plain text (UTF-8, max 3 KB), multiline, emoji
- Attachments: images (max 3 MB), files (max 20 MB); upload via button or paste
- Optional comment on attachment
- Reply to a message (quoted/outlined in UI)
- Edit own messages (show "edited" indicator, preserve order)
- Delete own messages; admins can delete any message in their room
- Replies to deleted messages degrade gracefully (show placeholder)
- Persistent history, chronological order, cursor-based infinite scroll
- Offline recipients receive messages on next connect

### 8. Unread state

- Unread count badge on each room and personal dialog
- Cleared when user opens that chat
- Updates in real-time via WebSocket

### 9. UI layout

```
[ top navigation bar                                     ]
[ central message timeline | rooms & contacts list       ]
[                           | room members / context     ]
[ message composer (bottom)                              ]
```

Admin actions via menus and modal dialogs. Prefer clarity and robustness over visual polish.

---

## Non-obvious edge cases

Only rules that are not already stated in the sections above. Rules like "username immutable", "duplicate email rejected", "owner cannot leave", "private room hidden from catalog" are already covered by §1, §5, etc. and are omitted here.

1. **Presence, multi-tab active + idle** — one active tab + one idle tab → user status remains **online**.
2. **Presence, all idle** — all tabs idle > 1 min → **AFK**.
3. **Presence, all stale** — all tabs closed or heartbeat expired → **offline**.
4. **Kicked from public room** — a kicked (= room-banned) member cannot rejoin via the public catalog until explicitly unbanned. Unbanning restores join eligibility; membership is not auto-restored.
5. **File access on membership loss** — a user who loses room access immediately loses download access to that room's files, including files they uploaded themselves.
6. **Reply to deleted message** — the reply persists; the quoted-source panel renders a `[message deleted]` placeholder.
7. **Infinite scroll stability** — loading older pages must not reorder or duplicate visible messages across WebSocket inserts.
8. **Account deletion cascade** — removes memberships, owned rooms (and their messages/files/attachments), sessions, friend relationships, and PM eligibility; authored messages in non-owned chats have author rewritten to `[deleted user]`.
