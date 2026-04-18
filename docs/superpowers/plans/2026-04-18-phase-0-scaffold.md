# Phase 0 — Scaffold Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Produce a running-but-featureless Docker Compose stack — Spring Boot backend + Vite/React frontend + PostgreSQL — with a green health-check integration test and all CI primitives in place.

**Architecture:** Spring Boot 3.3 serves `GET /api/health`; PostgreSQL is managed by Docker Compose with a health-check; Flyway creates the `users` table (used by Phase 1 auth); Vite hot-reloads the frontend in dev with a proxy to `:8080`; in production Docker the Maven `frontend-maven-plugin` builds the Vite output into Spring Boot's static resources so one container serves everything.

**Tech Stack:** Java 21, Spring Boot 3.3.5, Maven 3.9 (+ wrapper), Flyway 10, PostgreSQL 16, Testcontainers 1.19.7, Node 20, Vite 5, React 18, TypeScript 5, Tailwind CSS 3, Docker Compose v2.

---

## File map

```
backend/
  pom.xml
  mvnw / mvnw.cmd / .mvn/
  Dockerfile
  src/main/java/com/example/chat/
    ChatApplication.java
    HealthController.java
    config/
      SecurityConfig.java
  src/main/resources/
    application.yml
    db/migration/V1__initial_schema.sql
  src/test/java/com/example/chat/
    integration/
      IntegrationTestBase.java
      HealthIntegrationTest.java
  src/test/resources/
    application-test.yml
frontend/
  package.json
  vite.config.ts
  tailwind.config.js
  postcss.config.js
  index.html
  src/
    main.tsx
    App.tsx
    index.css
docker-compose.yml
.env.example
CLAUDE.md  (Commands section updated)
```

---

### Task 1: Create backend Maven project

**Files:**
- Create: `backend/pom.xml`
- Create: `backend/src/main/java/com/example/chat/ChatApplication.java`
- Create: `backend/.mvn/`, `backend/mvnw`, `backend/mvnw.cmd` (via Maven wrapper)

- [ ] **Step 1: Create directory skeleton**

```bash
mkdir -p backend/src/main/java/com/example/chat/config
mkdir -p backend/src/main/resources/db/migration
mkdir -p backend/src/test/java/com/example/chat/integration
mkdir -p backend/src/test/java/com/example/chat/unit
mkdir -p backend/src/test/resources
```

- [ ] **Step 2: Create `backend/pom.xml`**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.3.5</version>
    <relativePath/>
  </parent>

  <groupId>com.example</groupId>
  <artifactId>chat</artifactId>
  <version>0.0.1-SNAPSHOT</version>
  <name>chat</name>

  <properties>
    <java.version>21</java.version>
    <testcontainers.version>1.19.7</testcontainers.version>
  </properties>

  <dependencies>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-websocket</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.session</groupId>
      <artifactId>spring-session-jdbc</artifactId>
    </dependency>
    <dependency>
      <groupId>org.flywaydb</groupId>
      <artifactId>flyway-core</artifactId>
    </dependency>
    <dependency>
      <groupId>org.flywaydb</groupId>
      <artifactId>flyway-database-postgresql</artifactId>
    </dependency>
    <dependency>
      <groupId>org.postgresql</groupId>
      <artifactId>postgresql</artifactId>
      <scope>runtime</scope>
    </dependency>
    <!-- Test -->
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-test</artifactId>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.springframework.security</groupId>
      <artifactId>spring-security-test</artifactId>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.testcontainers</groupId>
      <artifactId>junit-jupiter</artifactId>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.testcontainers</groupId>
      <artifactId>postgresql</artifactId>
      <scope>test</scope>
    </dependency>
  </dependencies>

  <dependencyManagement>
    <dependencies>
      <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>testcontainers-bom</artifactId>
        <version>${testcontainers.version}</version>
        <type>pom</type>
        <scope>import</scope>
      </dependency>
    </dependencies>
  </dependencyManagement>

  <build>
    <plugins>
      <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
      </plugin>
    </plugins>
  </build>
</project>
```

- [ ] **Step 3: Create `backend/src/main/java/com/example/chat/ChatApplication.java`**

```java
package com.example.chat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ChatApplication {
    public static void main(String[] args) {
        SpringApplication.run(ChatApplication.class, args);
    }
}
```

- [ ] **Step 4: Generate Maven wrapper (requires Maven 3.9+ on PATH)**

```bash
cd backend && mvn wrapper:wrapper -Dmaven=3.9.6 && cd ..
```

Expected: `backend/.mvn/wrapper/maven-wrapper.properties`, `backend/mvnw`, `backend/mvnw.cmd` appear.

- [ ] **Step 5: Verify backend compiles**

```bash
cd backend && ./mvnw compile -q && cd ..
```

Expected: `BUILD SUCCESS` with no output.

- [ ] **Step 6: Commit**

```bash
git add backend/
git commit -m "chore: bootstrap Spring Boot 3.3 backend skeleton"
```

---

### Task 2: Application configuration

**Files:**
- Create: `backend/src/main/resources/application.yml`
- Create: `backend/src/test/resources/application-test.yml`

- [ ] **Step 1: Create `backend/src/main/resources/application.yml`**

```yaml
spring:
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/chat}
    username: ${DB_USER:chat}
    password: ${DB_PASSWORD:chat}
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false
    show-sql: false
  flyway:
    enabled: true
    locations: classpath:db/migration
  session:
    store-type: jdbc
    jdbc:
      initialize-schema: always
    timeout: 7d

server:
  port: ${PORT:8080}
  error:
    include-message: always

logging:
  level:
    root: INFO
    com.example.chat: DEBUG
```

- [ ] **Step 2: Create `backend/src/test/resources/application-test.yml`**

Testcontainers overrides `spring.datasource.*` at runtime via `@DynamicPropertySource` — this file just quiets noisy log output in tests.

```yaml
logging:
  level:
    org.flywaydb: WARN
    org.testcontainers: WARN
    com.github.dockerjava: WARN
    tc: WARN
```

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/resources/application.yml \
        backend/src/test/resources/application-test.yml
git commit -m "chore: add application.yml and test logging config"
```

---

### Task 3: Flyway V1 migration

**Files:**
- Create: `backend/src/main/resources/db/migration/V1__initial_schema.sql`

- [ ] **Step 1: Create `backend/src/main/resources/db/migration/V1__initial_schema.sql`**

```sql
-- Users — columns added here so Phase 1 auth has no additional migration
CREATE TABLE users (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    email         VARCHAR(255) NOT NULL,
    username      VARCHAR(30)  NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted_at    TIMESTAMPTZ,
    CONSTRAINT users_email_unique    UNIQUE (email),
    CONSTRAINT users_username_unique UNIQUE (username)
);
```

- [ ] **Step 2: Commit**

```bash
git add backend/src/main/resources/db/migration/V1__initial_schema.sql
git commit -m "db: V1 initial schema — users table"
```

---

### Task 4: SecurityConfig — open for Phase 0

Spring Boot auto-configures HTTP Basic on all endpoints when no `SecurityFilterChain` bean exists. We must create this bean **before** writing integration tests, otherwise tests get `401` instead of the expected `404` / `200`.

**Files:**
- Create: `backend/src/main/java/com/example/chat/config/SecurityConfig.java`

- [ ] **Step 1: Create `backend/src/main/java/com/example/chat/config/SecurityConfig.java`**

```java
package com.example.chat.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // CSRF re-enabled with proper token handling in Phase 1
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()   // locked down in Phase 1
            );
        return http.build();
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add backend/src/main/java/com/example/chat/config/SecurityConfig.java
git commit -m "chore: open SecurityConfig for Phase 0 — all routes permitted"
```

---

### Task 5: IntegrationTestBase

**Files:**
- Create: `backend/src/test/java/com/example/chat/integration/IntegrationTestBase.java`

- [ ] **Step 1: Create `backend/src/test/java/com/example/chat/integration/IntegrationTestBase.java`**

The container is a `static final` field — Testcontainers reuses the same PostgreSQL instance for the whole test suite (one container per JVM, not per test class).

```java
package com.example.chat.integration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
public abstract class IntegrationTestBase {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("chat_test")
                    .withUsername("chat")
                    .withPassword("chat");

    @DynamicPropertySource
    static void overrideDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    protected TestRestTemplate restTemplate;
}
```

- [ ] **Step 2: Verify test-compile succeeds**

```bash
cd backend && ./mvnw test-compile -q && cd ..
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 3: Commit**

```bash
git add backend/src/test/java/com/example/chat/integration/IntegrationTestBase.java
git commit -m "test: add IntegrationTestBase with shared Testcontainers PostgreSQL"
```

---

### Task 6: Health endpoint — TDD

**Files:**
- Create: `backend/src/test/java/com/example/chat/integration/HealthIntegrationTest.java`
- Create: `backend/src/main/java/com/example/chat/HealthController.java`

- [ ] **Step 1: Write the failing test**

Create `backend/src/test/java/com/example/chat/integration/HealthIntegrationTest.java`:

```java
package com.example.chat.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HealthIntegrationTest extends IntegrationTestBase {

    @Test
    void health_returns_200_and_status_up() {
        ResponseEntity<Map> response = restTemplate.getForEntity("/api/health", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("status", "up");
    }
}
```

- [ ] **Step 2: Run the test — expect FAIL (endpoint not yet created)**

```bash
cd backend && ./mvnw test -Dtest=HealthIntegrationTest 2>&1 | tail -20 && cd ..
```

Expected output includes: `Tests run: 1, Failures: 1, Errors: 0` — the assertion fails because the actual status is `404 NOT_FOUND` (endpoint doesn't exist yet). If you see a startup error instead, check the SecurityConfig and Testcontainers logs.

- [ ] **Step 3: Create `backend/src/main/java/com/example/chat/HealthController.java`**

```java
package com.example.chat;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/api/health")
    public Map<String, String> health() {
        return Map.of("status", "up");
    }
}
```

- [ ] **Step 4: Run the test — expect PASS**

```bash
cd backend && ./mvnw test -Dtest=HealthIntegrationTest && cd ..
```

Expected:
```
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/example/chat/HealthController.java \
        backend/src/test/java/com/example/chat/integration/HealthIntegrationTest.java
git commit -m "feat: add GET /api/health endpoint with integration test (TDD)"
```

---

### Task 7: Frontend scaffold

**Files:**
- Create: `frontend/` (via npm)
- Modify: `frontend/vite.config.ts`
- Modify: `frontend/tailwind.config.js`
- Modify: `frontend/src/index.css`
- Modify: `frontend/src/App.tsx`

- [ ] **Step 1: Scaffold Vite project (run from repo root)**

```bash
npm create vite@latest frontend -- --template react-ts
```

Accept all defaults.

- [ ] **Step 2: Install dependencies and Tailwind**

```bash
cd frontend
npm install
npm install -D tailwindcss postcss autoprefixer
npx tailwindcss init -p
cd ..
```

Expected: `frontend/tailwind.config.js` and `frontend/postcss.config.js` created by `tailwindcss init -p`.

- [ ] **Step 3: Replace `frontend/tailwind.config.js`**

```js
/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {},
  },
  plugins: [],
}
```

- [ ] **Step 4: Replace `frontend/src/index.css` with Tailwind directives**

```css
@tailwind base;
@tailwind components;
@tailwind utilities;
```

- [ ] **Step 5: Replace `frontend/vite.config.ts` with proxy config**

```ts
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': 'http://localhost:8080',
      '/ws': {
        target: 'http://localhost:8080',
        ws: true,
      },
    },
  },
})
```

- [ ] **Step 6: Replace `frontend/src/App.tsx` with placeholder**

```tsx
function App() {
  return (
    <div className="min-h-screen bg-gray-900 text-white flex flex-col items-center justify-center gap-2">
      <h1 className="text-4xl font-bold">Chat App</h1>
      <p className="text-gray-400 text-lg">Phase 0 — Scaffold</p>
    </div>
  )
}

export default App
```

- [ ] **Step 7: Verify the frontend builds without errors**

```bash
cd frontend && npm run build && cd ..
```

Expected: `frontend/dist/` created, no TypeScript or build errors.

- [ ] **Step 8: Add frontend artifacts to `.gitignore`**

Append to `.gitignore`:

```
# Frontend
frontend/node_modules/
frontend/dist/
```

- [ ] **Step 9: Commit**

```bash
git add frontend/ .gitignore
git commit -m "chore: add Vite + React + TypeScript + Tailwind frontend scaffold"
```

---

### Task 8: Docker artifacts

**Files:**
- Create: `backend/Dockerfile`
- Create: `docker-compose.yml`
- Create: `.env.example`

- [ ] **Step 1: Create `backend/Dockerfile`**

```dockerfile
# --- Build ---
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -q
COPY src src
RUN ./mvnw package -DskipTests -q

# --- Run ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

- [ ] **Step 2: Create `docker-compose.yml` in the repo root**

```yaml
services:
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: chat
      POSTGRES_USER: ${DB_USER:-chat}
      POSTGRES_PASSWORD: ${DB_PASSWORD:-chat}
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${DB_USER:-chat} -d chat"]
      interval: 5s
      timeout: 5s
      retries: 10

  backend:
    build:
      context: ./backend
      dockerfile: Dockerfile
    ports:
      - "8080:8080"
    environment:
      DB_URL: jdbc:postgresql://postgres:5432/chat
      DB_USER: ${DB_USER:-chat}
      DB_PASSWORD: ${DB_PASSWORD:-chat}
    depends_on:
      postgres:
        condition: service_healthy

volumes:
  postgres_data:
```

- [ ] **Step 3: Create `.env.example`**

```
# Copy to .env for local overrides (never commit .env)
DB_URL=jdbc:postgresql://localhost:5432/chat
DB_USER=chat
DB_PASSWORD=chat
```

- [ ] **Step 4: Add `.env` to `.gitignore`**

Append to `.gitignore`:

```
.env
```

- [ ] **Step 5: Commit**

```bash
git add backend/Dockerfile docker-compose.yml .env.example .gitignore
git commit -m "chore: add Docker Compose stack and backend Dockerfile"
```

---

### Task 9: Update CLAUDE.md Commands section

**Files:**
- Modify: `CLAUDE.md`

- [ ] **Step 1: Replace the `## Commands` section in `CLAUDE.md`**

Find the section that starts with `## Commands` and ends before the next `---` line, and replace it entirely with:

````markdown
## Commands

```bash
# Full stack
docker compose up                # start postgres + backend (pulls images on first run)
docker compose up --build        # rebuild backend image after code changes
docker compose down              # stop
docker compose down -v           # stop and wipe volumes (resets DB)

# Backend — local dev (requires postgres running, e.g. via docker compose up postgres)
cd backend && ./mvnw spring-boot:run              # hot-reload dev server on :8080
cd backend && ./mvnw test                         # all tests (Testcontainers auto-starts postgres)
cd backend && ./mvnw test -Dtest=SomeServiceTest  # single test class
cd backend && ./mvnw flyway:migrate               # apply migrations manually

# Frontend — local dev
cd frontend && npm run dev    # Vite dev server on :5173, proxies /api and /ws to :8080
cd frontend && npm run build  # production build → frontend/dist/
cd frontend && npm run lint   # ESLint
```

Migrations run automatically on backend startup via Flyway. Spring Session JDBC tables (`spring_session`, `spring_session_attributes`) are auto-created by Spring Session on first startup.
````

- [ ] **Step 2: Commit**

```bash
git add CLAUDE.md
git commit -m "docs: fill in real Commands in CLAUDE.md"
```

---

### Task 10: End-to-end smoke test

- [ ] **Step 1: Build and start the full stack**

```bash
docker compose up --build -d
```

Expected: both containers start with no errors. If the backend exits, check `docker compose logs backend`.

- [ ] **Step 2: Wait for backend startup and hit the health endpoint**

```bash
sleep 20 && curl -s http://localhost:8080/api/health
```

Expected:
```json
{"status":"up"}
```

- [ ] **Step 3: Verify Flyway migrated the DB**

```bash
docker compose exec postgres psql -U chat -d chat -c "\dt"
```

Expected: table list includes `users`, `flyway_schema_history`, `spring_session`, `spring_session_attributes`.

- [ ] **Step 4: Run all backend tests**

```bash
cd backend && ./mvnw test && cd ..
```

Expected:
```
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

- [ ] **Step 5: Verify frontend dev server**

```bash
cd frontend && npm run dev
```

Open http://localhost:5173 — dark page with "Chat App" / "Phase 0 — Scaffold" in white text. Stop with `Ctrl+C`.

- [ ] **Step 6: Stop the stack**

```bash
docker compose down
```

- [ ] **Step 7: Final commit**

```bash
git status   # should be clean; commit any leftovers if present
git log --oneline -10
```

Expected: clean working tree. Phase 0 is done.

---

## Phase 0 — Done criteria

- [ ] `docker compose up --build` starts cleanly, `GET /api/health` → `{"status":"up"}`
- [ ] `docker compose exec postgres psql -U chat -d chat -c "\dt"` shows `users` table
- [ ] `cd backend && ./mvnw test` → 1 test, 0 failures, BUILD SUCCESS
- [ ] `cd frontend && npm run dev` → Tailwind-styled placeholder at localhost:5173
- [ ] `CLAUDE.md` Commands section has real, runnable commands

**Next step:** Once all criteria above are green, run `superpowers:writing-plans` to generate the Phase 1 — Auth plan.
