import fs from 'node:fs';
import path from 'node:path';
import { Router, Request, Response, NextFunction } from 'express';
import { z } from 'zod';
import { requireAdminAuth } from '../../middleware/auth.js';
import { getDb } from '../../db/database.js';
import { loadEnv } from '../../config/env.js';
import { recordAudit } from '../../middleware/audit.js';

export const adminReportsRouter = Router();

function formatReport(r: any) {
  let parsedFacts: Record<string, number> | null = null;
  if (r.facts) {
    try {
      parsedFacts = JSON.parse(r.facts);
    } catch {
      parsedFacts = null;
    }
  }

  return {
    id: r.id,
    deviceId: r.deviceId,
    category: r.category,
    message: r.message,
    facts: parsedFacts,
    hasImage: Boolean(r.hasImage),
    status: r.status,
    adminNote: r.adminNote || null,
    createdAt: r.createdAt,
  };
}

// List reports
adminReportsRouter.get(
  '/admin/api/reports',
  requireAdminAuth,
  async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const db = getDb();
      const page = Math.max(1, parseInt(req.query.page as string, 10) || 1);
      const pageSize = 25;
      const offset = (page - 1) * pageSize;
      const status = (req.query.status as string || '').trim().toUpperCase();

      let countSql = 'SELECT COUNT(*) as count FROM reports';
      let selectSql = 'SELECT * FROM reports';
      const params: any[] = [];

      if (status) {
        countSql += ' WHERE status = ?';
        selectSql += ' WHERE status = ?';
        params.push(status);
      }

      selectSql += ' ORDER BY createdAt DESC LIMIT ? OFFSET ?';

      const total = (db.prepare(countSql).get(...params) as any).count;
      const rows = db.prepare(selectSql).all(...params, pageSize, offset) as any[];

      res.status(200).json({
        items: rows.map(formatReport),
        page,
        pageSize,
        total,
      });
    } catch (err) {
      next(err);
    }
  }
);

const patchReportSchema = z.object({
  status: z.enum(['OPEN', 'DONE']).optional(),
  adminNote: z.string().optional(),
});

// Update report status / admin note
adminReportsRouter.patch(
  '/admin/api/reports/:id',
  requireAdminAuth,
  async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const id = parseInt(req.params.id, 10);
      const parsed = patchReportSchema.safeParse(req.body);
      if (!parsed.success) {
        res.status(400).json({
          error: 'BAD_REQUEST',
          message: `Invalid input: ${parsed.error.issues.map(i => i.message).join(', ')}`,
        });
        return;
      }

      const db = getDb();
      const report = db.prepare('SELECT * FROM reports WHERE id = ?').get(id) as any;
      if (!report) {
        res.status(404).json({ error: 'NOT_FOUND', message: 'Report not found' });
        return;
      }

      const { status, adminNote } = parsed.data;
      db.prepare(`
        UPDATE reports SET
          status = COALESCE(?, status),
          adminNote = COALESCE(?, adminNote)
        WHERE id = ?
      `).run(status ?? null, adminNote ?? null, id);

      const updated = db.prepare('SELECT * FROM reports WHERE id = ?').get(id);
      recordAudit(req.admin!.username, `update report id=${id} (status: ${status || report.status})`);

      res.status(200).json(formatReport(updated));
    } catch (err) {
      next(err);
    }
  }
);

// Serve report screenshot
adminReportsRouter.get(
  '/admin/api/reports/:id/image',
  requireAdminAuth,
  async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const id = parseInt(req.params.id, 10);
      const db = getDb();
      const report = db.prepare('SELECT * FROM reports WHERE id = ?').get(id) as any;
      if (!report) {
        res.status(404).json({ error: 'NOT_FOUND', message: 'Report not found' });
        return;
      }

      if (!report.hasImage) {
        res.status(404).json({ error: 'NOT_FOUND', message: 'This report does not contain an image' });
        return;
      }

      const config = loadEnv();
      const imagePath = path.join(path.resolve(config.DATA_DIR), 'reports', `${id}.jpg`);
      if (!fs.existsSync(imagePath)) {
        res.status(404).json({ error: 'NOT_FOUND', message: 'Report image file missing from disk' });
        return;
      }

      res.setHeader('Content-Type', 'image/jpeg');
      fs.createReadStream(imagePath).pipe(res);
    } catch (err) {
      next(err);
    }
  }
);
