import { Router, Request, Response, NextFunction } from 'express';
import { z } from 'zod';
import { requireAdminAuth } from '../../middleware/auth.js';
import { getDb } from '../../db/database.js';
import { recordAudit } from '../../middleware/audit.js';

export const adminReaderConfigsRouter = Router();

const putConfigSchema = z.object({
  config: z.record(z.string(), z.unknown()),
});

// List all reader configs
adminReaderConfigsRouter.get(
  '/admin/api/reader-configs',
  requireAdminAuth,
  async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const db = getDb();
      const rows = db.prepare('SELECT app, revision, config_json, updatedAt FROM reader_configs ORDER BY app ASC').all() as any[];

      res.status(200).json(
        rows.map(r => ({
          app: r.app,
          revision: r.revision,
          config: JSON.parse(r.config_json),
          updatedAt: r.updatedAt,
        }))
      );
    } catch (err) {
      next(err);
    }
  }
);

// Update reader config for an app
adminReaderConfigsRouter.put(
  '/admin/api/reader-configs/:app',
  requireAdminAuth,
  async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const appCode = req.params.app.toLowerCase();
      if (!['indrive', 'uber', 'didi'].includes(appCode)) {
        res.status(400).json({
          error: 'BAD_REQUEST',
          message: "App parameter must be one of: indrive, uber, didi",
        });
        return;
      }

      const parsed = putConfigSchema.safeParse(req.body);
      if (!parsed.success) {
        res.status(400).json({
          error: 'BAD_REQUEST',
          message: `Invalid input: ${parsed.error.issues.map(i => i.message).join(', ')}`,
        });
        return;
      }

      const db = getDb();
      const existing = db.prepare('SELECT revision FROM reader_configs WHERE app = ?').get(appCode) as any;
      const nextRevision = existing ? existing.revision + 1 : 1;
      const nowIso = new Date().toISOString();
      const jsonStr = JSON.stringify(parsed.data.config);

      db.prepare(`
        INSERT INTO reader_configs (app, revision, config_json, updatedAt)
        VALUES (?, ?, ?, ?)
        ON CONFLICT(app) DO UPDATE SET
          revision = excluded.revision,
          config_json = excluded.config_json,
          updatedAt = excluded.updatedAt
      `).run(appCode, nextRevision, jsonStr, nowIso);

      recordAudit(req.admin!.username, `updated reader config for ${appCode} (revision ${nextRevision})`);

      res.status(200).json({
        app: appCode,
        revision: nextRevision,
        config: parsed.data.config,
        updatedAt: nowIso,
      });
    } catch (err) {
      next(err);
    }
  }
);
