import path from 'node:path';
import { loadEnv } from './config/env.js';
import { initDatabase, startMaintenanceTimers, closeDatabase } from './db/database.js';
import { createApp } from './app.js';

async function bootstrap() {
  try {
    const config = loadEnv();
    console.log(`Starting DriveFlow Server in ${config.NODE_ENV} mode...`);

    // Initialize Database
    const dbFilePath = path.join(path.resolve(config.DATA_DIR), 'driveflow.db');
    initDatabase(dbFilePath);
    console.log(`SQLite database connected and migrations applied at: ${dbFilePath}`);

    // Start background tasks (Daily backups and event purge)
    startMaintenanceTimers(path.resolve(config.DATA_DIR));

    // Create Express App
    const app = createApp();

    const server = app.listen(config.PORT, () => {
      console.log(`DriveFlow Server successfully listening on http://localhost:${config.PORT}`);
      console.log(`Healthcheck: http://localhost:${config.PORT}/healthz`);
      console.log(`Admin Portal: http://localhost:${config.PORT}/admin`);
    });

    // Graceful Shutdown
    const shutdown = (signal: string) => {
      console.log(`\nReceived ${signal}. Starting graceful shutdown...`);
      server.close(() => {
        console.log('HTTP server closed.');
        try {
          closeDatabase();
          console.log('Database connection closed safely.');
        } catch (err) {
          console.error('Error during DB closing:', err);
        }
        process.exit(0);
      });

      // Force shutdown after 10s timeout
      setTimeout(() => {
        console.error('Graceful shutdown timeout exceeded. Forcing exit.');
        process.exit(1);
      }, 10000);
    };

    process.on('SIGINT', () => shutdown('SIGINT'));
    process.on('SIGTERM', () => shutdown('SIGTERM'));
  } catch (err) {
    console.error('Failed to start DriveFlow Server:', err);
    process.exit(1);
  }
}

bootstrap();
