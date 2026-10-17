import { Router, Request, Response, NextFunction } from 'express';
import { getDb } from '../../db/database.js';

export const newsRouter = Router();

newsRouter.get(
  '/v1/news',
  async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const headerDevice = req.headers['x-df-device'];
      if (!headerDevice || typeof headerDevice !== 'string') {
        res.status(400).json({
          error: 'BAD_REQUEST',
          message: 'Missing required header X-DF-Device',
        });
        return;
      }

      const since = req.query.since as string | undefined;
      const db = getDb();

      let rows: any[];
      if (since) {
        rows = db.prepare(`
          SELECT id, title, body, url, createdAt
          FROM news
          WHERE published = 1 AND createdAt > ?
          ORDER BY createdAt DESC
        `).all(since);
      } else {
        rows = db.prepare(`
          SELECT id, title, body, url, createdAt
          FROM news
          WHERE published = 1
          ORDER BY createdAt DESC
        `).all();
      }

      res.status(200).json({
        items: rows.map(r => ({
          id: r.id,
          title: r.title,
          body: r.body,
          url: r.url || null,
          createdAt: r.createdAt,
        })),
      });
    } catch (err) {
      next(err);
    }
  }
);
