import fs from 'node:fs';
import path from 'node:path';
import { z } from 'zod';

const envSchema = z.object({
  PORT: z.coerce.number().default(3000),
  DATA_DIR: z.string().default('./data'),
  SERVER_SIGNING_PRIVATE_KEY: z.string().min(1, 'SERVER_SIGNING_PRIVATE_KEY is required'),
  ADMIN_USERNAME: z.string().min(1, 'ADMIN_USERNAME is required'),
  ADMIN_PASSWORD_HASH: z.string().min(1, 'ADMIN_PASSWORD_HASH is required'),
  JWT_SECRET: z.string().min(16, 'JWT_SECRET must be at least 16 characters'),
  PUBLIC_BASE_URL: z.string().url().default('http://localhost:3000'),
  NODE_ENV: z.enum(['development', 'production', 'test']).default('development'),
});

export type Config = z.infer<typeof envSchema>;

let cachedConfig: Config | null = null;

export function loadEnv(): Config {
  if (cachedConfig) {
    return cachedConfig;
  }

  // If running in test mode with missing keys, provide fallbacks for unit tests
  const rawEnv: Record<string, string | undefined> = {
    PORT: process.env.PORT,
    DATA_DIR: process.env.DATA_DIR,
    SERVER_SIGNING_PRIVATE_KEY: process.env.SERVER_SIGNING_PRIVATE_KEY,
    ADMIN_USERNAME: process.env.ADMIN_USERNAME,
    ADMIN_PASSWORD_HASH: process.env.ADMIN_PASSWORD_HASH,
    JWT_SECRET: process.env.JWT_SECRET,
    PUBLIC_BASE_URL: process.env.PUBLIC_BASE_URL,
    NODE_ENV: process.env.NODE_ENV,
  };

  const parsed = envSchema.safeParse(rawEnv);
  if (!parsed.success) {
    const errorDetails = parsed.error.issues
      .map(issue => `  - ${issue.path.join('.')}: ${issue.message}`)
      .join('\n');
    console.error(`FATAL: Missing or invalid environment variables:\n${errorDetails}`);
    throw new Error(`Invalid environment configuration:\n${errorDetails}`);
  }

  cachedConfig = parsed.data;

  // Ensure DATA_DIR and required subdirectories exist
  const dataDir = path.resolve(cachedConfig.DATA_DIR);
  if (!fs.existsSync(dataDir)) {
    fs.mkdirSync(dataDir, { recursive: true });
  }
  const reportsDir = path.join(dataDir, 'reports');
  if (!fs.existsSync(reportsDir)) {
    fs.mkdirSync(reportsDir, { recursive: true });
  }
  const releasesDir = path.join(dataDir, 'releases');
  if (!fs.existsSync(releasesDir)) {
    fs.mkdirSync(releasesDir, { recursive: true });
  }
  const backupsDir = path.join(dataDir, 'backups');
  if (!fs.existsSync(backupsDir)) {
    fs.mkdirSync(backupsDir, { recursive: true });
  }

  return cachedConfig;
}

export function resetConfigForTest(config?: Partial<Config>) {
  if (config) {
    cachedConfig = {
      PORT: config.PORT ?? 3000,
      DATA_DIR: config.DATA_DIR ?? './test-data',
      SERVER_SIGNING_PRIVATE_KEY: config.SERVER_SIGNING_PRIVATE_KEY ?? '',
      ADMIN_USERNAME: config.ADMIN_USERNAME ?? 'admin',
      ADMIN_PASSWORD_HASH: config.ADMIN_PASSWORD_HASH ?? '',
      JWT_SECRET: config.JWT_SECRET ?? 'test-secret-at-least-32-chars-long-12345',
      PUBLIC_BASE_URL: config.PUBLIC_BASE_URL ?? 'http://localhost:3000',
      NODE_ENV: config.NODE_ENV ?? 'test',
    };
  } else {
    cachedConfig = null;
  }
}
