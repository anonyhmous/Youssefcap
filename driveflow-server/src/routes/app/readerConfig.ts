import { Router, Request, Response, NextFunction } from 'express';
import { getDb } from '../../db/database.js';

export const readerConfigRouter = Router();

readerConfigRouter.get(
  '/v1/reader-config',
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

      const appCode = req.query.app as string;
      if (!appCode || !['indrive', 'uber', 'didi'].includes(appCode.toLowerCase())) {
        res.status(400).json({
          error: 'BAD_REQUEST',
          message: "Query parameter 'app' must be one of: indrive, uber, didi",
        });
        return;
      }

      const db = getDb();
      const record = db.prepare('SELECT app, revision, config_json FROM reader_configs WHERE app = ?').get(appCode.toLowerCase()) as any;

      if (!record) {
        res.status(404).json({
          error: 'CONFIG_NOT_FOUND',
          message: `Configuration for app '${appCode}' not found`,
        });
        return;
      }

      let parsedConfig: Record<string, unknown>;
      try {
        parsedConfig = JSON.parse(record.config_json);
      } catch {
        parsedConfig = {};
      }

      res.status(200).json({
        app: record.app,
        revision: record.revision,
        config: parsedConfig,
      });
    } catch (err) {
      next(err);
    }
  }
);
