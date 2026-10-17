# DriveFlow Server

The official backend server and REST API for **DriveFlow Driver** (Android app) and **DriveFlow Admin** (web administration panel).

---

## 1. Architecture & Tech Stack

- **Runtime**: Node.js 20 (TypeScript in strict mode).
- **Web Framework**: Express 4 with Helmet, CORS, and Express-Rate-Limit.
- **Database**: SQLite (via `better-sqlite3`) configured in WAL mode with automated migrations.
- **Cryptographic Signing**: ECDSA P-256 (secp256r1) with SHA-256 (DER format) over canonical JSON payloads.
- **Admin Auth**: bcryptjs password hashing and 12-hour signed JWTs.
- **File Storage**: Local filesystem inside `DATA_DIR` for SQLite database, release APKs, and diagnostic report screenshots.

---

## 2. Environment Variables

Create a `.env` file in the `driveflow-server` directory based on `.env.example`:

| Variable | Description | Default |
|---|---|---|
| `PORT` | HTTP port to listen on | `3000` |
| `DATA_DIR` | Storage root directory for SQLite DB, APKs, reports, and backups | `./data` |
| `PUBLIC_BASE_URL` | Fully-qualified public URL (used for APK download links) | `http://localhost:3000` |
| `SERVER_SIGNING_PRIVATE_KEY` | PKCS#8 PEM or Base64 ECDSA P-256 private key | **Required** |
| `ADMIN_USERNAME` | Username for the admin portal | **Required** |
| `ADMIN_PASSWORD_HASH` | bcrypt hash of the admin password | **Required** |
| `JWT_SECRET` | Secret key for signing admin authentication tokens (>= 16 chars) | **Required** |

---

## 3. Cryptographic Key Pair Generation

To generate a new ECDSA P-256 key pair:

```bash
npm run gen-keys
```

This script will output:
1. `SERVER_SIGNING_PRIVATE_KEY`: Paste this into your server `.env`.
2. `SERVER_PUBLIC_KEY`: Paste this into your Android app's `app/build.gradle.kts` inside `defaultConfig`:
   ```kotlin
   buildConfigField("String", "SERVER_PUBLIC_KEY", "\"<SPKI_BASE64_STRING>\"")
   ```

---

## 4. Running Locally

### Prerequisites
- Node.js 20+
- npm 10+

### Development Mode
```bash
# Install dependencies
npm install

# Run TypeScript development server with hot-reload
npm run dev
```

### Production Build & Run
```bash
npm run build
npm start
```

### Running Test Suite
```bash
npm test
```

---

## 5. Docker Deployment

### Building and Running with Docker
```bash
# Build the production image
docker build -t driveflow-server .

# Run container with a persistent volume mounted at /data
docker run -d \
  --name driveflow-server \
  --restart unless-stopped \
  -p 3000:3000 \
  -v /opt/driveflow/data:/data \
  --env-file /opt/driveflow/.env \
  driveflow-server
```

---

## 6. Production Reverse Proxy (HTTPS)

### Option A: Caddy (Recommended - Automatic SSL)
Place this configuration in your `/etc/caddy/Caddyfile`:

```caddy
api.driveflow.app {
    encode gzip zstd

    # Security headers
    header {
        Strict-Transport-Security "max-age=31536000; includeSubDomains; preload"
        X-Content-Type-Options "nosniff"
        X-Frame-Options "DENY"
    }

    # Proxy all traffic to the Node.js application
    reverse_proxy localhost:3000 {
        header_up X-Real-IP {remote_host}
        header_up X-Forwarded-For {remote_host}
        header_up X-Forwarded-Proto {scheme}
    }

    # APK downloads and file uploads max body limit
    request_body {
        max_size 100MB
    }
}
```

### Option B: Nginx
```nginx
server {
    listen 80;
    server_name api.driveflow.app;
    return 301 https://$host$request_uri;
}

server {
    listen 443 ssl http2;
    server_name api.driveflow.app;

    ssl_certificate /etc/letsencrypt/live/api.driveflow.app/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/api.driveflow.app/privkey.pem;

    client_max_body_size 100M;

    location / {
        proxy_pass http://127.0.0.1:3000;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_cache_bypass $http_upgrade;
    }
}
```

---

## 7. Database Backups & Recovery

The server runs SQLite in Write-Ahead Logging (`WAL`) mode:
1. **Automated Daily Backups**: Every 24 hours, the server automatically takes a hot snapshot of the database using SQLite's native online backup API into `$DATA_DIR/backups/driveflow-backup-<timestamp>.db` and automatically prunes old files, retaining the last 14 copies.
2. **Manual Hot Backup**:
   ```bash
   sqlite3 /data/driveflow.db ".backup '/data/backups/manual-backup.db'"
   ```
3. **Database Restoration**:
   To restore from a backup:
   ```bash
   # Stop server
   docker stop driveflow-server
   
   # Replace database file
   cp /data/backups/driveflow-backup-<timestamp>.db /data/driveflow.db
   rm -f /data/driveflow.db-wal /data/driveflow.db-shm
   
   # Restart server
   docker start driveflow-server
   ```

---

## 8. Signing Key Rotation & Client Safety

The Android driver app enforces signature verification on all `/v1/activate` responses using the baked-in `BuildConfig.SERVER_PUBLIC_KEY`. If the signature does not match, the app marks the activation response as `UNKNOWN` or invalid and rejects the plan update.

### What Happens if the Key is Rotated Immediately?
Existing installed Android apps with the old public key will fail signature verification and downgrade to `FREE` mode after 72 hours of offline caching.

### How to Safely Rotate Keys:
1. **Step 1 - Release APK Update with Dual-Key or Updated Key**:
   - Update `app/build.gradle.kts` with the new public key (or a key rotation handler supporting previous + new public keys).
   - Release the new APK version via the Admin Portal (`POST /admin/api/releases`) with `mandatory = true`.
2. **Step 2 - Wait for Fleet Update**:
   - Monitor the Admin devices panel (`GET /admin/api/devices`) until all active drivers have updated to the new `versionCode`.
3. **Step 3 - Switch Server Key**:
   - Update `SERVER_SIGNING_PRIVATE_KEY` in the server's `.env` and restart the server.
