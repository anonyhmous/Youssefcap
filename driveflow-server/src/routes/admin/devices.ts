import { Router, Request, Response, NextFunction } from 'express';
import { z } from 'zod';
import { requireAdminAuth } from '../../middleware/auth.js';
import { getDb } from '../../db/database.js';
import { recordAudit } from '../../middleware/audit.js';

export const adminDevicesRouter = Router();

function formatDevice(d: any) {
  return {
    id: d.id,
    deviceId: d.deviceId,
    name: d.name || null,
    phone: d.phone || null,
    governorate: d.governorate || null,
    model: d.model || null,
    androidSdk: d.androidSdk !== null ? Number(d.androidSdk) : null,
    appVersionName: d.appVersionName || null,
    lang: d.lang || null,
    status: d.status,
    trialEndsAt: d.trialEndsAt || null,
    planEndsAt: d.planEndsAt || null,
    giftEndsAt: d.giftEndsAt || null,
    killed: Boolean(d.killed),
    revoked: Boolean(d.revoked),
    keyId: d.keyId !== null ? Number(d.keyId) : null,
    firstSeenAt: d.firstSeenAt,
    lastSeenAt: d.lastSeenAt,
  };
}

// List devices
adminDevicesRouter.get(
  '/admin/api/devices',
  requireAdminAuth,
  async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const db = getDb();
      const page = Math.max(1, parseInt(req.query.page as string, 10) || 1);
      const pageSize = 25;
      const offset = (page - 1) * pageSize;
      const q = (req.query.q as string || '').trim().toLowerCase();
      const status = (req.query.status as string || '').trim();

      const conditions: string[] = [];
      const params: any[] = [];

      if (q) {
        conditions.push('(LOWER(deviceId) LIKE ? OR LOWER(name) LIKE ? OR LOWER(phone) LIKE ? OR LOWER(model) LIKE ?)');
        const searchPattern = `%${q}%`;
        params.push(searchPattern, searchPattern, searchPattern, searchPattern);
      }

      if (status) {
        conditions.push('status = ?');
        params.push(status);
      }

      const whereClause = conditions.length > 0 ? `WHERE ${conditions.join(' AND ')}` : '';

      const countRow = db.prepare(`SELECT COUNT(*) as count FROM devices ${whereClause}`).get(...params) as any;
      const total = countRow.count;

      const rows = db.prepare(`
        SELECT * FROM devices
        ${whereClause}
        ORDER BY lastSeenAt DESC
        LIMIT ? OFFSET ?
      `).all(...params, pageSize, offset);

      res.status(200).json({
        items: rows.map(formatDevice),
        page,
        pageSize,
        total,
      });
    } catch (err) {
      next(err);
    }
  }
);

// Device detail
adminDevicesRouter.get(
  '/admin/api/devices/:id',
  requireAdminAuth,
  async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const id = parseInt(req.params.id, 10);
      if (isNaN(id)) {
        res.status(400).json({ error: 'BAD_REQUEST', message: 'Invalid device ID' });
        return;
      }

      const db = getDb();
      const device = db.prepare('SELECT * FROM devices WHERE id = ?').get(id) as any;
      if (!device) {
        res.status(404).json({ error: 'NOT_FOUND', message: 'Device not found' });
        return;
      }

      // Fetch last 50 events
      const events = db.prepare(`
        SELECT type, value, app, ts
        FROM events
        WHERE deviceId = ?
        ORDER BY ts DESC
        LIMIT 50
      `).all(device.deviceId) as any[];

      // Count reports
      const reportsCountRow = db.prepare('SELECT COUNT(*) as count FROM reports WHERE deviceId = ?').get(device.deviceId) as any;

      const deviceData = formatDevice(device);

      res.status(200).json({
        ...deviceData,
        recentEvents: events.map(e => ({
          type: e.type,
          value: e.value,
          app: e.app,
          ts: e.ts,
        })),
        reports: reportsCountRow.count,
      });
    } catch (err) {
      next(err);
    }
  }
);

// Action: Revoke
adminDevicesRouter.post(
  '/admin/api/devices/:id/revoke',
  requireAdminAuth,
  async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const id = parseInt(req.params.id, 10);
      const db = getDb();
      const device = db.prepare('SELECT * FROM devices WHERE id = ?').get(id) as any;
      if (!device) {
        res.status(404).json({ error: 'NOT_FOUND', message: 'Device not found' });
        return;
      }

      db.prepare("UPDATE devices SET revoked = 1, status = 'REVOKED' WHERE id = ?").run(id);
      recordAudit(req.admin!.username, `revoke device id=${id} (${device.deviceId})`);
      res.status(200).json({ success: true, status: 'REVOKED' });
    } catch (err) {
      next(err);
    }
  }
);

// Action: Restore
adminDevicesRouter.post(
  '/admin/api/devices/:id/restore',
  requireAdminAuth,
  async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const id = parseInt(req.params.id, 10);
      const db = getDb();
      const device = db.prepare('SELECT * FROM devices WHERE id = ?').get(id) as any;
      if (!device) {
        res.status(404).json({ error: 'NOT_FOUND', message: 'Device not found' });
        return;
      }

      // Recompute status based on plan / gift / trial
      const nowMs = Date.now();
      let restoredStatus: string = 'FREE';
      if (device.planEndsAt && new Date(device.planEndsAt).getTime() > nowMs) {
        restoredStatus = 'ACTIVE';
      } else if (device.giftEndsAt && new Date(device.giftEndsAt).getTime() > nowMs) {
        restoredStatus = 'ACTIVE';
      } else if (device.trialEndsAt && new Date(device.trialEndsAt).getTime() > nowMs) {
        restoredStatus = 'TRIAL';
      }

      db.prepare('UPDATE devices SET revoked = 0, status = ? WHERE id = ?').run(restoredStatus, id);
      recordAudit(req.admin!.username, `restore device id=${id} (${device.deviceId})`);
      res.status(200).json({ success: true, status: restoredStatus });
    } catch (err) {
      next(err);
    }
  }
);

// Action: Extend plan
adminDevicesRouter.post(
  '/admin/api/devices/:id/extend',
  requireAdminAuth,
  async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const id = parseInt(req.params.id, 10);
      const days = parseInt(req.body.days, 10);
      if (isNaN(days) || days <= 0) {
        res.status(400).json({ error: 'BAD_REQUEST', message: "Field 'days' must be a positive integer" });
        return;
      }

      const db = getDb();
      const device = db.prepare('SELECT * FROM devices WHERE id = ?').get(id) as any;
      if (!device) {
        res.status(404).json({ error: 'NOT_FOUND', message: 'Device not found' });
        return;
      }

      const nowMs = Date.now();
      const baseMs = device.planEndsAt ? Math.max(nowMs, new Date(device.planEndsAt).getTime()) : nowMs;
      const newPlanEndsAt = new Date(baseMs + days * 86400 * 1000).toISOString();

      db.prepare("UPDATE devices SET planEndsAt = ?, status = 'ACTIVE' WHERE id = ?").run(newPlanEndsAt, id);
      recordAudit(req.admin!.username, `extend device id=${id} by ${days} days (endsAt: ${newPlanEndsAt})`);
      res.status(200).json({ success: true, planEndsAt: newPlanEndsAt, status: 'ACTIVE' });
    } catch (err) {
      next(err);
    }
  }
);

// Action: Gift days
adminDevicesRouter.post(
  '/admin/api/devices/:id/gift',
  requireAdminAuth,
  async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const id = parseInt(req.params.id, 10);
      const days = parseInt(req.body.days, 10);
      if (isNaN(days) || days <= 0) {
        res.status(400).json({ error: 'BAD_REQUEST', message: "Field 'days' must be a positive integer" });
        return;
      }

      const db = getDb();
      const device = db.prepare('SELECT * FROM devices WHERE id = ?').get(id) as any;
      if (!device) {
        res.status(404).json({ error: 'NOT_FOUND', message: 'Device not found' });
        return;
      }

      const nowMs = Date.now();
      const baseMs = device.giftEndsAt ? Math.max(nowMs, new Date(device.giftEndsAt).getTime()) : nowMs;
      const newGiftEndsAt = new Date(baseMs + days * 86400 * 1000).toISOString();

      const newStatus = device.status === 'REVOKED' ? 'REVOKED' : 'ACTIVE';
      db.prepare('UPDATE devices SET giftEndsAt = ?, status = ? WHERE id = ?').run(newGiftEndsAt, newStatus, id);
      recordAudit(req.admin!.username, `gift device id=${id} ${days} days (giftEndsAt: ${newGiftEndsAt})`);
      res.status(200).json({ success: true, giftEndsAt: newGiftEndsAt, status: newStatus });
    } catch (err) {
      next(err);
    }
  }
);

// Action: Kill
adminDevicesRouter.post(
  '/admin/api/devices/:id/kill',
  requireAdminAuth,
  async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const id = parseInt(req.params.id, 10);
      const db = getDb();
      const device = db.prepare('SELECT * FROM devices WHERE id = ?').get(id) as any;
      if (!device) {
        res.status(404).json({ error: 'NOT_FOUND', message: 'Device not found' });
        return;
      }

      db.prepare("UPDATE devices SET killed = 1, status = 'REVOKED' WHERE id = ?").run(id);
      recordAudit(req.admin!.username, `kill device id=${id}`);
      res.status(200).json({ success: true, killed: true, status: 'REVOKED' });
    } catch (err) {
      next(err);
    }
  }
);

// Action: Unkill
adminDevicesRouter.post(
  '/admin/api/devices/:id/unkill',
  requireAdminAuth,
  async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const id = parseInt(req.params.id, 10);
      const db = getDb();
      const device = db.prepare('SELECT * FROM devices WHERE id = ?').get(id) as any;
      if (!device) {
        res.status(404).json({ error: 'NOT_FOUND', message: 'Device not found' });
        return;
      }

      db.prepare('UPDATE devices SET killed = 0 WHERE id = ?').run(id);
      recordAudit(req.admin!.username, `unkill device id=${id}`);
      res.status(200).json({ success: true, killed: false });
    } catch (err) {
      next(err);
    }
  }
);

// Action: Unbind key from device
adminDevicesRouter.post(
  '/admin/api/devices/:id/unbind',
  requireAdminAuth,
  async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const id = parseInt(req.params.id, 10);
      const db = getDb();
      const device = db.prepare('SELECT * FROM devices WHERE id = ?').get(id) as any;
      if (!device) {
        res.status(404).json({ error: 'NOT_FOUND', message: 'Device not found' });
        return;
      }

      db.transaction(() => {
        if (device.keyId) {
          db.prepare('UPDATE license_keys SET boundDeviceId = NULL WHERE id = ?').run(device.keyId);
        }
        db.prepare('UPDATE devices SET keyId = NULL, planEndsAt = NULL WHERE id = ?').run(id);
      })();

      recordAudit(req.admin!.username, `unbind key from device id=${id}`);
      res.status(200).json({ success: true });
    } catch (err) {
      next(err);
    }
  }
);
