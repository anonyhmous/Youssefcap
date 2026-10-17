import crypto from 'node:crypto';
import { Router, Request, Response, NextFunction } from 'express';
import { z } from 'zod';
import { requireAdminAuth } from '../../middleware/auth.js';
import { getDb } from '../../db/database.js';
import { recordAudit } from '../../middleware/audit.js';

export const adminKeysRouter = Router();

// Alphabet: A-Z without I and O (24 characters) and 2-9 (8 digits) -> 32 characters
export const KEY_ALPHABET = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';

export function generateSingleLicenseKey(): string {
  const segment = (len: number) => {
    const bytes = crypto.randomBytes(len);
    let result = '';
    for (let i = 0; i < len; i++) {
      result += KEY_ALPHABET[bytes[i] % KEY_ALPHABET.length];
    }
    return result;
  };
  return `DF-${segment(4)}-${segment(4)}-${segment(4)}`;
}

function computeKeyStatus(k: any, nowMs: number): 'UNUSED' | 'ACTIVE' | 'EXPIRED' | 'REVOKED' {
  if (k.status === 'REVOKED') return 'REVOKED';
  if (!k.activatedAt) return 'UNUSED';
  if (k.expiresAt && new Date(k.expiresAt).getTime() < nowMs) return 'EXPIRED';
  return 'ACTIVE';
}

function formatKey(k: any, nowMs: number) {
  return {
    id: k.id,
    key: k.key,
    planDays: k.planDays,
    status: computeKeyStatus(k, nowMs),
    note: k.note || null,
    boundDeviceId: k.boundDeviceId || null,
    activatedAt: k.activatedAt || null,
    expiresAt: k.expiresAt || null,
    lastBoundAt: k.lastBoundAt || null,
    createdAt: k.createdAt,
  };
}

// List keys
adminKeysRouter.get(
  '/admin/api/keys',
  requireAdminAuth,
  async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const db = getDb();
      const page = Math.max(1, parseInt(req.query.page as string, 10) || 1);
      const pageSize = 25;
      const offset = (page - 1) * pageSize;
      const filterStatus = (req.query.status as string || '').trim().toUpperCase();
      const nowMs = Date.now();

      // Read all or paginate
      const allRows = db.prepare('SELECT * FROM license_keys ORDER BY createdAt DESC').all() as any[];
      let filtered = allRows.map(k => formatKey(k, nowMs));

      if (filterStatus) {
        filtered = filtered.filter(k => k.status === filterStatus);
      }

      const total = filtered.length;
      const paginated = filtered.slice(offset, offset + pageSize);

      res.status(200).json({
        items: paginated,
        page,
        pageSize,
        total,
      });
    } catch (err) {
      next(err);
    }
  }
);

const createKeysSchema = z.object({
  count: z.number().int().min(1).max(200),
  planDays: z.number().int().min(1).default(30),
  note: z.string().nullable().optional(),
});

// Generate keys
adminKeysRouter.post(
  '/admin/api/keys',
  requireAdminAuth,
  async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const parsed = createKeysSchema.safeParse(req.body);
      if (!parsed.success) {
        res.status(400).json({
          error: 'BAD_REQUEST',
          message: `Invalid input: ${parsed.error.issues.map(i => i.message).join(', ')}`,
        });
        return;
      }

      const { count, planDays, note } = parsed.data;
      const db = getDb();
      const nowIso = new Date().toISOString();

      const insertStmt = db.prepare(`
        INSERT INTO license_keys (key, planDays, status, note, boundDeviceId, activatedAt, expiresAt, lastBoundAt, createdAt)
        VALUES (?, ?, 'UNUSED', ?, NULL, NULL, NULL, NULL, ?)
      `);

      const createdKeys: { id: number; key: string; planDays: number }[] = [];

      db.transaction(() => {
        for (let i = 0; i < count; i++) {
          let uniqueKey = generateSingleLicenseKey();
          let attempts = 0;
          while (db.prepare('SELECT id FROM license_keys WHERE key = ?').get(uniqueKey)) {
            uniqueKey = generateSingleLicenseKey();
            attempts++;
            if (attempts > 50) throw new Error('Failed to generate a unique key after 50 attempts');
          }

          const info = insertStmt.run(uniqueKey, planDays, note || null, nowIso);
          createdKeys.push({
            id: Number(info.lastInsertRowid),
            key: uniqueKey,
            planDays,
          });
        }
      })();

      recordAudit(req.admin!.username, `generated ${count} license keys (${planDays} days)`);

      res.status(200).json({
        keys: createdKeys,
      });
    } catch (err) {
      next(err);
    }
  }
);

// Revoke key
adminKeysRouter.post(
  '/admin/api/keys/:id/revoke',
  requireAdminAuth,
  async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const id = parseInt(req.params.id, 10);
      const db = getDb();
      const key = db.prepare('SELECT * FROM license_keys WHERE id = ?').get(id) as any;
      if (!key) {
        res.status(404).json({ error: 'NOT_FOUND', message: 'License key not found' });
        return;
      }

      db.transaction(() => {
        db.prepare("UPDATE license_keys SET status = 'REVOKED' WHERE id = ?").run(id);
        if (key.boundDeviceId) {
          db.prepare("UPDATE devices SET status = 'REVOKED' WHERE deviceId = ?").run(key.boundDeviceId);
        }
      })();

      recordAudit(req.admin!.username, `revoke key id=${id} (${key.key})`);
      res.status(200).json({ success: true, status: 'REVOKED' });
    } catch (err) {
      next(err);
    }
  }
);

// Unbind key
adminKeysRouter.post(
  '/admin/api/keys/:id/unbind',
  requireAdminAuth,
  async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const id = parseInt(req.params.id, 10);
      const db = getDb();
      const key = db.prepare('SELECT * FROM license_keys WHERE id = ?').get(id) as any;
      if (!key) {
        res.status(404).json({ error: 'NOT_FOUND', message: 'License key not found' });
        return;
      }

      const previousDevice = key.boundDeviceId;
      db.transaction(() => {
        db.prepare('UPDATE license_keys SET boundDeviceId = NULL WHERE id = ?').run(id);
        if (previousDevice) {
          db.prepare('UPDATE devices SET keyId = NULL WHERE deviceId = ?').run(previousDevice);
        }
      })();

      recordAudit(req.admin!.username, `unbind key id=${id} (${key.key}) from device ${previousDevice || 'none'}`);
      res.status(200).json({ success: true, boundDeviceId: null });
    } catch (err) {
      next(err);
    }
  }
);
