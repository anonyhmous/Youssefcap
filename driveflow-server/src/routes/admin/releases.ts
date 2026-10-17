import crypto from 'node:crypto';
import fs from 'node:fs';
import path from 'node:path';
import { Router, Request, Response, NextFunction } from 'express';
import multer from 'multer';
import { z } from 'zod';
import { requireAdminAuth } from '../../middleware/auth.js';
import { getDb } from '../../db/database.js';
import { loadEnv } from '../../config/env.js';
import { recordAudit } from '../../middleware/audit.js';

export const adminReleasesRouter = Router();

// Configure multer for file uploads up to 100 MB
const upload = multer({
  limits: {
    fileSize: 100 * 1024 * 1024, // 100 MB
  },
  storage: multer.memoryStorage(),
});

function formatRelease(r: any) {
  return {
    id: r.id,
    versionCode: r.versionCode,
    versionName: r.versionName,
    notes: r.notes,
    mandatory: Boolean(r.mandatory),
    sha256: r.sha256,
    sizeBytes: r.sizeBytes,
    createdAt: r.createdAt,
  };
}

// List all releases
adminReleasesRouter.get(
  '/admin/api/releases',
  requireAdminAuth,
  async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const db = getDb();
      const rows = db.prepare('SELECT * FROM releases ORDER BY versionCode DESC').all() as any[];
      res.status(200).json(rows.map(formatRelease));
    } catch (err) {
      next(err);
    }
  }
);

// Upload new release APK
adminReleasesRouter.post(
  '/admin/api/releases',
  requireAdminAuth,
  upload.single('apk'),
  async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const file = req.file;
      if (!file) {
        res.status(400).json({ error: 'BAD_REQUEST', message: "Multipart 'apk' file is required" });
        return;
      }

      const versionCode = parseInt(req.body.versionCode, 10);
      const versionName = (req.body.versionName || '').trim();
      const notes = (req.body.notes || '').trim();
      const mandatory = req.body.mandatory === 'true' || req.body.mandatory === true;

      if (isNaN(versionCode) || versionCode < 1) {
        res.status(400).json({ error: 'BAD_REQUEST', message: 'Valid integer versionCode is required' });
        return;
      }
      if (!versionName) {
        res.status(400).json({ error: 'BAD_REQUEST', message: 'versionName is required' });
        return;
      }

      const db = getDb();
      const existing = db.prepare('SELECT id FROM releases WHERE versionCode = ?').get(versionCode);
      if (existing) {
        res.status(409).json({ error: 'CONFLICT', message: `Release versionCode ${versionCode} already exists` });
        return;
      }

      // Compute sha256 of uploaded file
      const hash = crypto.createHash('sha256').update(file.buffer).digest('hex');
      const sizeBytes = file.buffer.length;

      // Save APK to disk
      const config = loadEnv();
      const releasesDir = path.join(path.resolve(config.DATA_DIR), 'releases');
      if (!fs.existsSync(releasesDir)) {
        fs.mkdirSync(releasesDir, { recursive: true });
      }
      const destPath = path.join(releasesDir, `${versionCode}.apk`);
      fs.writeFileSync(destPath, file.buffer);

      const nowIso = new Date().toISOString();
      const stmt = db.prepare(`
        INSERT INTO releases (versionCode, versionName, notes, mandatory, sha256, sizeBytes, createdAt)
        VALUES (?, ?, ?, ?, ?, ?, ?)
      `);

      const info = stmt.run(
        versionCode,
        versionName,
        notes,
        mandatory ? 1 : 0,
        hash,
        sizeBytes,
        nowIso
      );

      const created = db.prepare('SELECT * FROM releases WHERE id = ?').get(info.lastInsertRowid);
      recordAudit(req.admin!.username, `uploaded release v${versionCode} (${versionName})`);

      res.status(201).json(formatRelease(created));
    } catch (err) {
      next(err);
    }
  }
);

const patchReleaseSchema = z.object({
  mandatory: z.boolean(),
});

// Update mandatory status
adminReleasesRouter.patch(
  '/admin/api/releases/:id',
  requireAdminAuth,
  async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const id = parseInt(req.params.id, 10);
      const parsed = patchReleaseSchema.safeParse(req.body);
      if (!parsed.success) {
        res.status(400).json({
          error: 'BAD_REQUEST',
          message: `Invalid input: ${parsed.error.issues.map(i => i.message).join(', ')}`,
        });
        return;
      }

      const db = getDb();
      const release = db.prepare('SELECT * FROM releases WHERE id = ?').get(id) as any;
      if (!release) {
        res.status(404).json({ error: 'NOT_FOUND', message: 'Release not found' });
        return;
      }

      db.prepare('UPDATE releases SET mandatory = ? WHERE id = ?').run(
        parsed.data.mandatory ? 1 : 0,
        id
      );

      const updated = db.prepare('SELECT * FROM releases WHERE id = ?').get(id);
      recordAudit(req.admin!.username, `updated release id=${id} mandatory=${parsed.data.mandatory}`);

      res.status(200).json(formatRelease(updated));
    } catch (err) {
      next(err);
    }
  }
);
