import { Router, Request, Response, NextFunction } from 'express';
import { z } from 'zod';
import { requireAdminAuth } from '../../middleware/auth.js';
import { getDb } from '../../db/database.js';
import { recordAudit } from '../../middleware/audit.js';

export const adminSettingsRouter = Router();

const settingsSchema = z.object({
  trialDays: z.number().int().min(0).default(7),
  freeDailyList: z.number().int().min(0).default(20),
  freeDailyDetails: z.number().int().min(0).default(5),
  minVersionCode: z.number().int().min(1).default(1),
  priceText: z.string().default(''),
  supportWhatsapp: z.string().default(''),
  giftDaysDefault: z.number().int().min(0).default(0),
  expectedSignerSha256: z.string().default(''),
});

function readSettingsObject(db: any) {
  const rows = db.prepare('SELECT key, value FROM settings').all() as { key: string; value: string }[];
  const map: Record<string, string> = {};
  for (const row of rows) {
    map[row.key] = row.value;
  }

  return {
    trialDays: parseInt(map.trialDays || '7', 10),
    freeDailyList: parseInt(map.freeDailyList || '20', 10),
    freeDailyDetails: parseInt(map.freeDailyDetails || '5', 10),
    minVersionCode: parseInt(map.minVersionCode || '1', 10),
    priceText: map.priceText || '',
    supportWhatsapp: map.supportWhatsapp || '',
    giftDaysDefault: parseInt(map.giftDaysDefault || '0', 10),
    expectedSignerSha256: map.expectedSignerSha256 || '',
  };
}

// Get settings
adminSettingsRouter.get(
  '/admin/api/settings',
  requireAdminAuth,
  async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const db = getDb();
      const settings = readSettingsObject(db);
      res.status(200).json(settings);
    } catch (err) {
      next(err);
    }
  }
);

// Update settings
adminSettingsRouter.put(
  '/admin/api/settings',
  requireAdminAuth,
  async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const parsed = settingsSchema.safeParse(req.body);
      if (!parsed.success) {
        res.status(400).json({
          error: 'BAD_REQUEST',
          message: `Invalid settings format: ${parsed.error.issues.map(i => i.message).join(', ')}`,
        });
        return;
      }

      const db = getDb();
      const updates = parsed.data;

      const upsertStmt = db.prepare(`
        INSERT INTO settings (key, value) VALUES (?, ?)
        ON CONFLICT(key) DO UPDATE SET value = excluded.value
      `);

      db.transaction(() => {
        upsertStmt.run('trialDays', updates.trialDays.toString());
        upsertStmt.run('freeDailyList', updates.freeDailyList.toString());
        upsertStmt.run('freeDailyDetails', updates.freeDailyDetails.toString());
        upsertStmt.run('minVersionCode', updates.minVersionCode.toString());
        upsertStmt.run('priceText', updates.priceText);
        upsertStmt.run('supportWhatsapp', updates.supportWhatsapp);
        upsertStmt.run('giftDaysDefault', updates.giftDaysDefault.toString());
        upsertStmt.run('expectedSignerSha256', updates.expectedSignerSha256);
      })();

      recordAudit(req.admin!.username, 'updated system settings');

      const updated = readSettingsObject(db);
      res.status(200).json(updated);
    } catch (err) {
      next(err);
    }
  }
);
