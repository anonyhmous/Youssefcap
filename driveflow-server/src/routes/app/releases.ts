import fs from 'node:fs';
import path from 'node:path';
import { Router, Request, Response, NextFunction } from 'express';
import { getDb } from '../../db/database.js';
import { loadEnv } from '../../config/env.js';

export const releasesRouter = Router();

releasesRouter.get(
  '/v1/releases/latest',
  async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const db = getDb();
      const config = loadEnv();
      const release = db.prepare(`
        SELECT id, versionCode, versionName, notes, mandatory, sha256, sizeBytes, createdAt
        FROM releases
        ORDER BY versionCode DESC
        LIMIT 1
      `).get() as any;

      if (!release) {
        res.status(404).json({
          error: 'RELEASE_NOT_FOUND',
          message: 'No published releases found',
        });
        return;
      }

      res.status(200).json({
        versionCode: release.versionCode,
        versionName: release.versionName,
        url: `${config.PUBLIC_BASE_URL}/v1/releases/download/${release.versionCode}`,
        sha256: release.sha256,
        notes: release.notes,
        mandatory: Boolean(release.mandatory),
        createdAt: release.createdAt,
      });
    } catch (err) {
      next(err);
    }
  }
);

releasesRouter.get(
  '/v1/releases/download/:versionCode',
  async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const versionCode = parseInt(req.params.versionCode, 10);
      if (isNaN(versionCode)) {
        res.status(400).json({ error: 'BAD_REQUEST', message: 'Invalid versionCode' });
        return;
      }

      const db = getDb();
      const config = loadEnv();
      const release = db.prepare('SELECT * FROM releases WHERE versionCode = ?').get(versionCode) as any;

      if (!release) {
        res.status(404).json({ error: 'RELEASE_NOT_FOUND', message: 'Release not found' });
        return;
      }

      const filePath = path.join(path.resolve(config.DATA_DIR), 'releases', `${versionCode}.apk`);
      if (!fs.existsSync(filePath)) {
        res.status(404).json({ error: 'FILE_NOT_FOUND', message: 'APK file not found on disk' });
        return;
      }

      const stat = fs.statSync(filePath);
      res.writeHead(200, {
        'Content-Type': 'application/vnd.android.package-archive',
        'Content-Length': stat.size,
        'Content-Disposition': `attachment; filename="driveflow-driver-v${versionCode}.apk"`,
      });

      const readStream = fs.createReadStream(filePath);
      readStream.pipe(res);
    } catch (err) {
      next(err);
    }
  }
);
