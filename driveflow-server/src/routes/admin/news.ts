import { Router, Request, Response, NextFunction } from 'express';
import { z } from 'zod';
import { requireAdminAuth } from '../../middleware/auth.js';
import { getDb } from '../../db/database.js';
import { recordAudit } from '../../middleware/audit.js';

export const adminNewsRouter = Router();

function formatNews(n: any) {
  return {
    id: n.id,
    title: n.title,
    body: n.body,
    url: n.url || null,
    published: Boolean(n.published),
    createdAt: n.createdAt,
  };
}

const newsCreateSchema = z.object({
  title: z.string().min(1),
  body: z.string().min(1),
  url: z.string().url().nullable().optional(),
  published: z.boolean().default(true),
});

const newsPatchSchema = newsCreateSchema.partial();

// List news
adminNewsRouter.get(
  '/admin/api/news',
  requireAdminAuth,
  async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const db = getDb();
      const rows = db.prepare('SELECT * FROM news ORDER BY createdAt DESC').all() as any[];
      res.status(200).json(rows.map(formatNews));
    } catch (err) {
      next(err);
    }
  }
);

// Create news
adminNewsRouter.post(
  '/admin/api/news',
  requireAdminAuth,
  async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const parsed = newsCreateSchema.safeParse(req.body);
      if (!parsed.success) {
        res.status(400).json({
          error: 'BAD_REQUEST',
          message: `Invalid input: ${parsed.error.issues.map(i => i.message).join(', ')}`,
        });
        return;
      }

      const { title, body, url, published } = parsed.data;
      const db = getDb();
      const nowIso = new Date().toISOString();

      const stmt = db.prepare(`
        INSERT INTO news (title, body, url, published, createdAt)
        VALUES (?, ?, ?, ?, ?)
      `);

      const info = stmt.run(title, body, url || null, published ? 1 : 0, nowIso);
      const created = db.prepare('SELECT * FROM news WHERE id = ?').get(info.lastInsertRowid);
      recordAudit(req.admin!.username, `create news id=${info.lastInsertRowid} ("${title}")`);

      res.status(201).json(formatNews(created));
    } catch (err) {
      next(err);
    }
  }
);

// Update news
adminNewsRouter.patch(
  '/admin/api/news/:id',
  requireAdminAuth,
  async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const id = parseInt(req.params.id, 10);
      const parsed = newsPatchSchema.safeParse(req.body);
      if (!parsed.success) {
        res.status(400).json({
          error: 'BAD_REQUEST',
          message: `Invalid input: ${parsed.error.issues.map(i => i.message).join(', ')}`,
        });
        return;
      }

      const db = getDb();
      const item = db.prepare('SELECT * FROM news WHERE id = ?').get(id) as any;
      if (!item) {
        res.status(404).json({ error: 'NOT_FOUND', message: 'News item not found' });
        return;
      }

      const { title, body, url, published } = parsed.data;
      db.prepare(`
        UPDATE news SET
          title = COALESCE(?, title),
          body = COALESCE(?, body),
          url = COALESCE(?, url),
          published = COALESCE(?, published)
        WHERE id = ?
      `).run(
        title ?? null,
        body ?? null,
        url ?? null,
        published !== undefined ? (published ? 1 : 0) : null,
        id
      );

      const updated = db.prepare('SELECT * FROM news WHERE id = ?').get(id);
      recordAudit(req.admin!.username, `update news id=${id}`);
      res.status(200).json(formatNews(updated));
    } catch (err) {
      next(err);
    }
  }
);

// Delete news
adminNewsRouter.delete(
  '/admin/api/news/:id',
  requireAdminAuth,
  async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const id = parseInt(req.params.id, 10);
      const db = getDb();
      const item = db.prepare('SELECT * FROM news WHERE id = ?').get(id) as any;
      if (!item) {
        res.status(404).json({ error: 'NOT_FOUND', message: 'News item not found' });
        return;
      }

      db.prepare('DELETE FROM news WHERE id = ?').run(id);
      recordAudit(req.admin!.username, `delete news id=${id} ("${item.title}")`);
      res.status(200).json({ success: true });
    } catch (err) {
      next(err);
    }
  }
);
