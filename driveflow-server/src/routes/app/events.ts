import { Router, Request, Response, NextFunction } from 'express';
import { z } from 'zod';
import { getDb } from '../../db/database.js';

export const eventsRouter = Router();

const EVENT_TYPE_REGEX = /^[a-z0-9_]{1,40}$/;
const EVENT_VALUE_REGEX = /^[A-Za-z0-9_.:\-]{0,64}$/;

const eventItemSchema = z.object({
  type: z.string().regex(EVENT_TYPE_REGEX, 'Invalid event type format'),
  value: z.string().regex(EVENT_VALUE_REGEX, 'Invalid event value format (no free text or spaces)'),
  app: z.enum(['indrive', 'uber', 'didi', 'unknown']),
  ref: z.string().nullable().optional(),
  ts: z.string().datetime(),
});

const eventsBatchSchema = z.object({
  deviceId: z.string().min(16).max(64),
  consentVersion: z.number().int(),
  events: z.array(eventItemSchema).max(100, 'Batch capped at 100 events'),
});

eventsRouter.post(
  '/v1/events',
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

      const parsed = eventsBatchSchema.safeParse(req.body);
      if (!parsed.success) {
        res.status(400).json({
          error: 'BAD_REQUEST',
          message: `Validation failed: ${parsed.error.issues.map(i => i.message).join(', ')}`,
        });
        return;
      }

      const { deviceId, consentVersion, events } = parsed.data;

      if (headerDevice.toLowerCase() !== deviceId.toLowerCase()) {
        res.status(400).json({
          error: 'BAD_REQUEST',
          message: 'Header X-DF-Device must match body.deviceId',
        });
        return;
      }

      // Check consent
      if (consentVersion < 1) {
        res.status(400).json({
          error: 'BAD_REQUEST',
          message: 'Consent version >= 1 required to collect telemetry events',
        });
        return;
      }

      const db = getDb();
      const insertStmt = db.prepare(`
        INSERT INTO events (deviceId, type, value, app, ref, ts)
        VALUES (?, ?, ?, ?, ?, ?)
      `);

      const insertMany = db.transaction((items) => {
        for (const item of items) {
          insertStmt.run(
            deviceId,
            item.type,
            item.value,
            item.app,
            item.ref || null,
            item.ts
          );
        }
      });

      insertMany(events);

      res.status(200).json({
        accepted: events.length,
      });
    } catch (err) {
      next(err);
    }
  }
);
