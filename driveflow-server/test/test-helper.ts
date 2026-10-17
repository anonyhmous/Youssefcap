import Database from 'better-sqlite3';
import bcrypt from 'bcryptjs';
import { runMigrations } from '../src/db/migrations.js';
import { setTestDb } from '../src/db/database.js';
import { resetConfigForTest } from '../src/config/env.js';
import { createApp } from '../src/app.js';
import { generateSigningKeys } from '../scripts/gen-keys.js';

export function setupTestEnvironment() {
  process.env.NODE_ENV = 'test';
  const keys = generateSigningKeys();
  const testPassword = 'adminpassword123';
  const passwordHash = bcrypt.hashSync(testPassword, 10);

  resetConfigForTest({
    PORT: 0,
    DATA_DIR: './test-data',
    SERVER_SIGNING_PRIVATE_KEY: keys.privateKeyPem,
    ADMIN_USERNAME: 'testadmin',
    ADMIN_PASSWORD_HASH: passwordHash,
    JWT_SECRET: 'super-secret-jwt-test-key-32-chars-long',
    PUBLIC_BASE_URL: 'http://localhost:3000',
    NODE_ENV: 'test',
  });

  const db = new Database(':memory:');
  db.pragma('journal_mode = WAL');
  db.pragma('foreign_keys = ON');
  runMigrations(db);
  setTestDb(db);

  const app = createApp();

  return {
    app,
    db,
    keys,
    testPassword,
  };
}
