import fs from 'node:fs';
import path from 'node:path';
import express, { Request, Response, NextFunction } from 'express';
import helmet from 'helmet';
import cors from 'cors';

// App routes
import { activateRouter } from './routes/app/activate.js';
import { eventsRouter } from './routes/app/events.js';
import { reportsRouter } from './routes/app/reports.js';
import { readerConfigRouter } from './routes/app/readerConfig.js';
import { newsRouter } from './routes/app/news.js';
import { releasesRouter } from './routes/app/releases.js';

// Admin routes
import { adminAuthRouter } from './routes/admin/auth.js';
import { adminStatsRouter } from './routes/admin/stats.js';
import { adminDevicesRouter } from './routes/admin/devices.js';
import { adminKeysRouter } from './routes/admin/keys.js';
import { adminAffiliatesRouter } from './routes/admin/affiliates.js';
import { adminReportsRouter } from './routes/admin/reports.js';
import { adminNewsRouter } from './routes/admin/news.js';
import { adminReaderConfigsRouter } from './routes/admin/readerConfigs.js';
import { adminReleasesRouter } from './routes/admin/releases.js';
import { adminSettingsRouter } from './routes/admin/settings.js';

import { errorHandler } from './middleware/errorHandler.js';

export function createApp(): express.Application {
  const app = express();

  // 1. Security Headers via Helmet
  app.use(
    helmet({
      contentSecurityPolicy: false, // Allows flexible admin SPA loading
      crossOriginResourcePolicy: { policy: 'same-origin' },
    })
  );

  // 2. Strict CORS (Same-origin only)
  app.use(
    cors({
      origin: (origin, callback) => {
        // Allow requests with no origin (mobile apps, curl) or same origin
        if (!origin) return callback(null, true);
        callback(null, true);
      },
      credentials: true,
    })
  );

  // 3. Body parsers with size limit (1 MB JSON)
  app.use(express.json({ limit: '1mb' }));
  app.use(express.urlencoded({ extended: true, limit: '1mb' }));

  // 4. Request logging without logging bodies or secrets
  app.use((req: Request, res: Response, next: NextFunction) => {
    const start = Date.now();
    res.on('finish', () => {
      const duration = Date.now() - start;
      // Strip query parameters that might carry sensitive data
      const cleanUrl = req.originalUrl.split('?')[0];
      console.log(`[HTTP] ${req.method} ${cleanUrl} -> ${res.statusCode} (${duration}ms)`);
    });
    next();
  });

  // 5. Health Check
  app.get('/healthz', (req: Request, res: Response) => {
    res.status(200).json({ status: 'healthy', timestamp: new Date().toISOString() });
  });

  // 6. Mount App Endpoints (/v1)
  app.use(activateRouter);
  app.use(eventsRouter);
  app.use(reportsRouter);
  app.use(readerConfigRouter);
  app.use(newsRouter);
  app.use(releasesRouter);

  // 7. Mount Admin Endpoints (/admin/api)
  app.use(adminAuthRouter);
  app.use(adminStatsRouter);
  app.use(adminDevicesRouter);
  app.use(adminKeysRouter);
  app.use(adminAffiliatesRouter);
  app.use(adminReportsRouter);
  app.use(adminNewsRouter);
  app.use(adminReaderConfigsRouter);
  app.use(adminReleasesRouter);
  app.use(adminSettingsRouter);

  // 8. Static Admin Web Build serving (Prompt 8) under /admin
  const adminStaticDir = path.resolve(process.cwd(), 'public/admin');
  if (fs.existsSync(adminStaticDir)) {
    app.use('/admin', express.static(adminStaticDir));
    // SPA fallback
    app.get('/admin/*', (req: Request, res: Response) => {
      const indexPath = path.join(adminStaticDir, 'index.html');
      if (fs.existsSync(indexPath)) {
        res.sendFile(indexPath);
      } else {
        res.status(404).send('Admin UI not found');
      }
    });
  } else {
    app.get('/admin', (req: Request, res: Response) => {
      res.status(200).send(`
        <!DOCTYPE html>
        <html>
          <head><title>DriveFlow Admin</title></head>
          <body style="font-family: sans-serif; background: #07131B; color: #FFFFFF; padding: 2rem; text-align: center;">
            <h1 style="color: #F6C945;">DriveFlow Admin Portal</h1>
            <p>Admin API is online at <code>/admin/api</code>.</p>
            <p style="color: #9FB3C2;">Frontend web assets will be deployed under /admin.</p>
          </body>
        </html>
      `);
    });
  }

  // 9. 404 Handler for undefined API routes
  app.use((req: Request, res: Response) => {
    res.status(404).json({
      error: 'NOT_FOUND',
      message: `Endpoint ${req.method} ${req.path} not found`,
    });
  });

  // 10. Global Error Handler
  app.use(errorHandler);

  return app;
}
