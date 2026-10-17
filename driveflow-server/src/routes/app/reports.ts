import fs from 'node:fs';
import path from 'node:path';
import { Router, Request, Response, NextFunction } from 'express';
import { z } from 'zod';
import { getDb } from '../../db/database.js';
import { loadEnv } from '../../config/env.js';

export const reportsRouter = Router();

const reportSchema = z.object({
  deviceId: z.string().min(16).max(64),
  category: z.enum(['reading', 'verdict', 'overlay', 'subscription', 'idea', 'other']),
  message: z.string().min(1).max(2000),
  facts: z.record(z.string(), z.number()).nullable().optional(),
  imageBase64: z.string().nullable().optional(),
});

reportsRouter.post(
  '/v1/reports',
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

      const parsed = reportSchema.safeParse(req.body);
      if (!parsed.success) {
        res.status(400).json({
          error: 'BAD_REQUEST',
          message: `Validation failed: ${parsed.error.issues.map(i => i.message).join(', ')}`,
        });
        return;
      }

      const { deviceId, category, message, facts, imageBase64 } = parsed.data;

      if (headerDevice.toLowerCase() !== deviceId.toLowerCase()) {
        res.status(400).json({
          error: 'BAD_REQUEST',
          message: 'Header X-DF-Device must match body.deviceId',
        });
        return;
      }

      // Check facts values: all values must be numbers
      if (facts) {
        for (const [key, val] of Object.entries(facts)) {
          if (typeof val !== 'number' || isNaN(val)) {
            res.status(400).json({
              error: 'BAD_REQUEST',
              message: `Fact '${key}' must be a valid number`,
            });
            return;
          }
        }
      }

      // Validate image if provided
      let imageBuffer: Buffer | null = null;
      if (imageBase64 && imageBase64.trim().length > 0) {
        // Strip data URI prefix if present
        const base64Data = imageBase64.replace(/^data:image\/[a-zA-Z]+;base64,/, '');
        imageBuffer = Buffer.from(base64Data, 'base64');

        // Check size: max 300 KB
        if (imageBuffer.length > 300 * 1024) {
          res.status(400).json({
            error: 'BAD_REQUEST',
            message: 'Image size exceeds maximum limit of 300 KB',
          });
          return;
        }

        // Verify JPEG magic bytes: FF D8 FF
        if (
          imageBuffer.length < 3 ||
          imageBuffer[0] !== 0xff ||
          imageBuffer[1] !== 0xd8 ||
          imageBuffer[2] !== 0xff
        ) {
          res.status(400).json({
            error: 'BAD_REQUEST',
            message: 'Invalid image format. Only JPEG images are accepted.',
          });
          return;
        }
      }

      const db = getDb();
      const config = loadEnv();
      const nowIso = new Date().toISOString();
      const hasImage = imageBuffer ? 1 : 0;
      const factsJson = facts ? JSON.stringify(facts) : null;

      const stmt = db.prepare(`
        INSERT INTO reports (deviceId, category, message, facts, hasImage, status, adminNote, createdAt)
        VALUES (?, ?, ?, ?, ?, 'OPEN', NULL, ?)
      `);

      const info = stmt.run(deviceId, category, message, factsJson, hasImage, nowIso);
      const reportId = Number(info.lastInsertRowid);

      // Save image to disk if present
      if (imageBuffer) {
        const reportsDir = path.join(path.resolve(config.DATA_DIR), 'reports');
        if (!fs.existsSync(reportsDir)) {
          fs.mkdirSync(reportsDir, { recursive: true });
        }
        const filePath = path.join(reportsDir, `${reportId}.jpg`);
        fs.writeFileSync(filePath, imageBuffer);
      }

      res.status(200).json({
        id: reportId,
        status: 'OPEN',
      });
    } catch (err) {
      next(err);
    }
  }
);
