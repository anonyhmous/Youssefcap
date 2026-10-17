import fs from 'node:fs';
import path from 'node:path';
import Database from 'better-sqlite3';
import { runMigrations } from './migrations.js';

let dbInstance: Database.Database | null = null;
let backupInterval: NodeJS.Timeout | null = null;
let purgeInterval: NodeJS.Timeout | null = null;

export function initDatabase(dbFilePath: string): Database.Database {
  if (dbInstance) {
    return dbInstance;
  }

  const dir = path.dirname(dbFilePath);
  if (!fs.existsSync(dir)) {
    fs.mkdirSync(dir, { recursive: true });
  }

  const db = new Database(dbFilePath);

  // Enable WAL mode and foreign key constraints
  db.pragma('journal_mode = WAL');
  db.pragma('synchronous = NORMAL');
  db.pragma('foreign_keys = ON');

  // Run migrations
  runMigrations(db);

  dbInstance = db;
  return db;
}

export function getDb(): Database.Database {
  if (!dbInstance) {
    throw new Error('Database has not been initialized. Call initDatabase first.');
  }
  return dbInstance;
}

export function setTestDb(db: Database.Database): void {
  dbInstance = db;
}

export function closeDatabase(): void {
  if (backupInterval) {
    clearInterval(backupInterval);
    backupInterval = null;
  }
  if (purgeInterval) {
    clearInterval(purgeInterval);
    purgeInterval = null;
  }
  if (dbInstance && dbInstance.open) {
    dbInstance.close();
    dbInstance = null;
  }
}

/**
 * Creates a SQLite backup file in DATA_DIR/backups/ and maintains max 14 backups.
 */
export async function performBackup(dataDir: string): Promise<string | null> {
  if (!dbInstance) return null;
  const backupsDir = path.join(dataDir, 'backups');
  if (!fs.existsSync(backupsDir)) {
    fs.mkdirSync(backupsDir, { recursive: true });
  }

  const dateStr = new Date().toISOString().replace(/[:.]/g, '-');
  const backupFile = path.join(backupsDir, `driveflow-backup-${dateStr}.db`);

  try {
    await dbInstance.backup(backupFile);

    // Prune older backups keeping last 14
    const files = fs.readdirSync(backupsDir)
      .filter(f => f.startsWith('driveflow-backup-') && f.endsWith('.db'))
      .map(f => path.join(backupsDir, f));

    if (files.length > 14) {
      files.sort((a, b) => fs.statSync(a).mtimeMs - fs.statSync(b).mtimeMs);
      const toDelete = files.slice(0, files.length - 14);
      for (const file of toDelete) {
        try {
          fs.unlinkSync(file);
        } catch {}
      }
    }

    return backupFile;
  } catch (err) {
    console.error('Failed to perform DB backup:', err);
    return null;
  }
}

/**
 * Daily purge of events older than 90 days.
 */
export function purgeOldEvents(): number {
  if (!dbInstance) return 0;
  const ninetyDaysAgo = new Date(Date.now() - 90 * 86400 * 1000).toISOString();
  const info = dbInstance.prepare('DELETE FROM events WHERE ts < ?').run(ninetyDaysAgo);
  return info.changes;
}

/**
 * Starts background maintenance timers (Daily backup & Event purge)
 */
export function startMaintenanceTimers(dataDir: string): void {
  // Run once shortly after startup
  setTimeout(() => {
    performBackup(dataDir).catch(console.error);
    purgeOldEvents();
  }, 5000);

  // Every 24 hours
  const TWENTY_FOUR_HOURS = 24 * 60 * 60 * 1000;
  backupInterval = setInterval(() => {
    performBackup(dataDir).catch(console.error);
  }, TWENTY_FOUR_HOURS);

  purgeInterval = setInterval(() => {
    purgeOldEvents();
  }, TWENTY_FOUR_HOURS);
}
