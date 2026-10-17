import { getDb } from '../db/database.js';

export function recordAudit(who: string, what: string): void {
  try {
    const db = getDb();
    const now = new Date().toISOString();
    db.prepare('INSERT INTO admin_audit (who, what, when_timestamp) VALUES (?, ?, ?)').run(
      who,
      what,
      now
    );
  } catch (err) {
    console.error('Failed to write admin audit log:', err);
  }
}
