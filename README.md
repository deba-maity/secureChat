# Privac Secure Chat

Privacy-first secure chat built as a production-style monorepo:

- Frontend: Next.js, React, TypeScript, Tailwind CSS, shadcn/ui-style primitives
- Backend: Java 21, Spring Boot, Spring Security, JWT, Spring WebSocket + STOMP
- Database: PostgreSQL with Flyway migrations
- Message storage: AES-GCM encrypted ciphertext only

## Features

- Register, login, logout, BCrypt password hashing, JWT auth
- Search users by username or phone number
- No default chat list: home shows search, favorites, and settings only
- Temporary conversations are purged when the user leaves unless favorited
- Favorite conversations remain encrypted and are restored after login
- Realtime text, emojis, timestamps, delivered and seen indicators
- Typing indicator, unread counts, desktop notification support
- Privacy settings: last seen, online status, read receipts, screenshot warning demo, locked favorites
- Settings: dark mode, auto delete toggle, change password, delete account
- Bonus flows: self-destruct timers, favorite PIN lock, encrypted export/import, favorite message search, PWA shell

## Run Locally

Start PostgreSQL:

```bash
docker compose up -d
```

Run the backend:

```bash
cd backend
mvn spring-boot:run
```

Run the frontend:

```bash
cd frontend
npm install
npm run dev
```

Open `http://localhost:3000`.

## Start Every Time On Windows

Use this flow on a personal laptop when Docker is installed and Maven is not set up locally.

Terminal 1, from PowerShell:

```powershell
cd C:\Users\Dmaity\Desktop\secureChat
docker compose up -d postgres

$backendPath = "$PWD\backend"

docker run --rm -it `
  --network securechat_default `
  -p 8080:8080 `
  -v "${backendPath}:/app" `
  -w /app `
  -e DATABASE_URL="jdbc:postgresql://postgres:5432/securechat" `
  -e DATABASE_USERNAME="securechat" `
  -e DATABASE_PASSWORD="securechat" `
  -e JWT_SECRET="replace-with-at-least-32-random-characters" `
  -e CRYPTO_SECRET="replace-with-a-long-random-message-encryption-secret" `
  -e CORS_ALLOWED_ORIGINS="http://localhost:3000,http://localhost:3001" `
  maven:3.9-eclipse-temurin-21 `
  mvn spring-boot:run
```

Keep Terminal 1 open while using the app.

Terminal 2:

```powershell
cd C:\Users\Dmaity\Desktop\secureChat\frontend
npm install
npm run dev
```

Open `http://localhost:3000`.

To stop:

```powershell
# Press Ctrl+C in the backend and frontend terminals first.
cd C:\Users\Dmaity\Desktop\secureChat
docker compose down
```

Note: run the Docker backend command in PowerShell. Git Bash can rewrite `/app` into a Windows Git path and break Docker's `-w /app` setting.

## Important Environment Variables

Backend defaults are local-development friendly, but set strong values before any shared deployment:

```bash
JWT_SECRET=replace-with-at-least-32-random-characters
CRYPTO_SECRET=replace-with-a-long-random-message-encryption-secret
CORS_ALLOWED_ORIGINS=http://localhost:3000
```

Frontend:

```bash
# Leave NEXT_PUBLIC_API_URL unset for local Next.js development.
# The frontend calls /api/* and Next.js proxies those requests to http://localhost:8080.
BACKEND_URL=http://localhost:8080
NEXT_PUBLIC_SOCKET_URL=http://localhost:8080
```

## API Shape

- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/logout`
- `GET /api/users/search?q=...`
- `POST /api/conversations/start`
- `GET /api/conversations/favorites`
- `POST /api/conversations/{id}/favorite`
- `DELETE /api/conversations/{id}/favorite`
- `DELETE /api/conversations/{id}/temporary`
- `GET /api/conversations/{id}/messages`
- `POST /api/conversations/{id}/seen`
- `POST /api/messages`
- `GET /api/settings`
- `PUT /api/settings`
- `POST /api/settings/favorites/export`
- `POST /api/settings/favorites/import`
- `POST /api/settings/favorite-pin/verify`
- `DELETE /api/settings/account`
- WebSocket STOMP endpoint: `/ws`, publish to `/app/chat.send`, subscribe to `/user/queue/messages`

## Privacy Notes

Messages are encrypted with AES-GCM before persistence. The database stores `iv` and `cipher_text`; plaintext is produced only in API responses while viewing a conversation. Temporary local browser cache is removed when the conversation is left, and non-favorite server conversations are deleted on the same leave action.
