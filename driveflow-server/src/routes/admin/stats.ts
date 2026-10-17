import { Router, Request, Response, NextFunction } from 'express';
import { requireAdminAuth } from '../../middleware/auth.js';
import { getDb } from '../../db/database.js';

export const adminStatsRouter = Router();

adminStatsRouter.get(
  '/admin/api/stats',
  requireAdminAuth,
  async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const db = getDb();
      const startOfTodayIso = new Date(new Date().setUTCHours(0, 0, 0, 0)).toISOString();

      const devicesCount = (db.prepare('SELECT COUNT(*) as count FROM devices').get() as any).count;
      const trialCount = (db.prepare("SELECT COUNT(*) as count FROM devices WHERE status = 'TRIAL'").get() as any).count;
      const activeCount = (db.prepare("SELECT COUNT(*) as count FROM devices WHERE status = 'ACTIVE'").get() as any).count;
      const freeCount = (db.prepare("SELECT COUNT(*) as count FROM devices WHERE status = 'FREE'").get() as any).count;
      const revokedCount = (db.prepare("SELECT COUNT(*) as count FROM devices WHERE status = 'REVOKED' OR revoked = 1 OR killed = 1").get() as any).count;
      const openReportsCount = (db.prepare("SELECT COUNT(*) as count FROM reports WHERE status = 'OPEN'").get() as any).count;
      const eventsTodayCount = (db.prepare('SELECT COUNT(*) as count FROM events WHERE ts >= ?').get(startOfTodayIso) as any).count;
      const keysUnusedCount = (db.prepare("SELECT COUNT(*) as count FROM license_keys WHERE status = 'UNUSED' AND boundDeviceId IS NULL").get() as any).count;

      res.status(200).json({
        devices: devicesCount,
        trial: trialCount,
        active: activeCount,
        free: freeCount,
        revoked: revokedCount,
        openReports: openReportsCount,
        eventsToday: eventsTodayCount,
        keysUnused: keysUnusedCount,
      });
    } catch (err) {
      next(err);
    }
  }
);
