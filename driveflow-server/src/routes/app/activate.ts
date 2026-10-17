import { Router, Request, Response, NextFunction } from 'express';
import { z } from 'zod';
import { getDb } from '../../db/database.js';
import { ECDSAKeySigner } from '../../crypto/signer.js';
import { loadEnv } from '../../config/env.js';
import { activateIpRateLimiter, activateDeviceRateLimiter } from '../../middleware/rateLimit.js';

export const activateRouter = Router();

// Validation schema for /v1/activate
const activateSchema = z.object({
  deviceId: z.string().min(16).max(64),
  licenseKey: z.string().nullable().optional(),
  affiliateCode: z.string().nullable().optional(),
  appVersionCode: z.number().int(),
  appVersionName: z.string().min(1),
  signerSha256: z.string(),
  androidSdk: z.number().int(),
  deviceModel: z.string(),
  lang: z.enum(['ar', 'en']).default('ar'),
  market: z.literal('EG').default('EG'),
  governorate: z.string().nullable().optional(),
  contactName: z.string().nullable().optional(),
  contactPhone: z.string().nullable().optional(),
  nonce: z.string().min(16),
});

// DF-XXXX-XXXX-XXXX alphabet: A-Z without I and O (24 letters) + 2-9 (8 digits)
const KEY_REGEX = /^DF-[A-HJ-NP-Z2-9]{4}-[A-HJ-NP-Z2-9]{4}-[A-HJ-NP-Z2-9]{4}$/;

activateRouter.post(
  '/v1/activate',
  activateIpRateLimiter,
  activateDeviceRateLimiter,
  async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      // 1. Verify header X-DF-Device matches body deviceId
      const headerDevice = req.headers['x-df-device'];
      if (!headerDevice || typeof headerDevice !== 'string') {
        res.status(400).json({
          error: 'BAD_REQUEST',
          message: 'Missing required header X-DF-Device',
        });
        return;
      }

      const parseResult = activateSchema.safeParse(req.body);
      if (!parseResult.success) {
        res.status(400).json({
          error: 'BAD_REQUEST',
          message: `Invalid request body: ${parseResult.error.issues.map(i => i.message).join(', ')}`,
        });
        return;
      }

      const body = parseResult.data;
      if (headerDevice.toLowerCase() !== body.deviceId.toLowerCase()) {
        res.status(400).json({
          error: 'BAD_REQUEST',
          message: 'Header X-DF-Device must match body.deviceId',
        });
        return;
      }

      const db = getDb();
      const now = new Date();
      const nowIso = now.toISOString();
      const nowMs = now.getTime();

      // 2. Fetch current settings
      const settingsRows = db.prepare('SELECT key, value FROM settings').all() as { key: string; value: string }[];
      const settings: Record<string, string> = {};
      for (const row of settingsRows) {
        settings[row.key] = row.value;
      }

      const trialDays = Number(settings.trialDays || '7');
      const freeDailyList = Number(settings.freeDailyList || '20');
      const freeDailyDetails = Number(settings.freeDailyDetails || '5');
      const minVersionCode = Number(settings.minVersionCode || '1');
      const priceText = settings.priceText || null;
      const supportWhatsapp = settings.supportWhatsapp || null;
      const giftDaysDefault = Number(settings.giftDaysDefault || '0');
      const expectedSignerSha256 = settings.expectedSignerSha256 || '';

      // Check latest release versionCode
      const latestRelease = db.prepare('SELECT versionCode FROM releases ORDER BY versionCode DESC LIMIT 1').get() as { versionCode: number } | undefined;
      const latestVersionCode = latestRelease ? latestRelease.versionCode : body.appVersionCode;

      // 3. Lookup or insert device
      let device = db.prepare('SELECT * FROM devices WHERE deviceId = ?').get(body.deviceId) as any;

      if (!device) {
        const trialEndsAt = new Date(nowMs + trialDays * 86400 * 1000).toISOString();
        const insertStmt = db.prepare(`
          INSERT INTO devices (
            deviceId, name, phone, governorate, model, androidSdk, appVersionName, lang,
            status, trialEndsAt, planEndsAt, giftEndsAt, killed, revoked, keyId,
            firstSeenAt, lastSeenAt
          ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'TRIAL', ?, NULL, NULL, 0, 0, NULL, ?, ?)
        `);

        const insertInfo = insertStmt.run(
          body.deviceId,
          body.contactName || null,
          body.contactPhone || null,
          body.governorate || null,
          body.deviceModel,
          body.androidSdk,
          body.appVersionName,
          body.lang,
          trialEndsAt,
          nowIso,
          nowIso
        );

        device = db.prepare('SELECT * FROM devices WHERE id = ?').get(insertInfo.lastInsertRowid) as any;
      } else {
        // Update device metadata
        db.prepare(`
          UPDATE devices SET
            model = ?,
            androidSdk = ?,
            appVersionName = ?,
            lang = ?,
            governorate = COALESCE(?, governorate),
            name = COALESCE(?, name),
            phone = COALESCE(?, phone),
            lastSeenAt = ?
          WHERE id = ?
        `).run(
          body.deviceModel,
          body.androidSdk,
          body.appVersionName,
          body.lang,
          body.governorate || null,
          body.contactName || null,
          body.contactPhone || null,
          nowIso,
          device.id
        );
        // Refresh local device record
        device = db.prepare('SELECT * FROM devices WHERE id = ?').get(device.id) as any;
      }

      let responseMessage: string | null = null;
      let isUnknownSigner = false;

      // 4. Verify signerSha256 if expectedSignerSha256 is set
      if (expectedSignerSha256 && expectedSignerSha256.trim().length > 0) {
        if (body.signerSha256.toLowerCase() !== expectedSignerSha256.toLowerCase()) {
          isUnknownSigner = true;
          responseMessage = 'Untrusted application signature';
        }
      }

      // 5. Handle Affiliate / Gift Code (if submitted)
      if (body.affiliateCode && body.affiliateCode.trim().length > 0) {
        const affCode = body.affiliateCode.trim();
        const affiliate = db.prepare('SELECT * FROM affiliates WHERE code = ?').get(affCode) as any;
        if (!affiliate) {
          res.status(400).json({ error: 'AFFILIATE_UNKNOWN', message: 'Affiliate code not found' });
          return;
        }

        // Check if device already used an affiliate code or this code
        const alreadyUsed = db.prepare('SELECT id FROM affiliate_uses WHERE affiliateId = ? AND deviceId = ?').get(affiliate.id, body.deviceId);
        if (alreadyUsed) {
          res.status(400).json({ error: 'GIFT_USED', message: 'Gift code already used by this device' });
          return;
        }

        // Check expiration
        if (affiliate.expiresAt && new Date(affiliate.expiresAt).getTime() < nowMs) {
          res.status(400).json({ error: 'GIFT_EXPIRED', message: 'Gift code has expired' });
          return;
        }

        // Check max uses
        if (affiliate.maxUses !== null && affiliate.uses >= affiliate.maxUses) {
          res.status(400).json({ error: 'GIFT_FULL', message: 'Gift code maximum uses reached' });
          return;
        }

        // Check self affiliate (device name or phone matching ownerName)
        if (device.name && affiliate.ownerName && device.name.trim().toLowerCase() === affiliate.ownerName.trim().toLowerCase()) {
          res.status(400).json({ error: 'AFFILIATE_SELF', message: 'Cannot redeem your own affiliate code' });
          return;
        }

        // Apply gift days
        const days = affiliate.giftDays > 0 ? affiliate.giftDays : giftDaysDefault;
        const currentGiftMs = device.giftEndsAt ? Math.max(nowMs, new Date(device.giftEndsAt).getTime()) : nowMs;
        const newGiftEndsAt = new Date(currentGiftMs + days * 86400 * 1000).toISOString();

        db.transaction(() => {
          db.prepare('INSERT INTO affiliate_uses (affiliateId, deviceId, usedAt) VALUES (?, ?, ?)').run(
            affiliate.id,
            body.deviceId,
            nowIso
          );
          db.prepare('UPDATE affiliates SET uses = uses + 1 WHERE id = ?').run(affiliate.id);
          db.prepare('UPDATE devices SET giftEndsAt = ? WHERE id = ?').run(newGiftEndsAt, device.id);
        })();

        device = db.prepare('SELECT * FROM devices WHERE id = ?').get(device.id) as any;
      }

      // 6. Handle License Key (if submitted)
      if (body.licenseKey && body.licenseKey.trim().length > 0) {
        const cleanKey = body.licenseKey.trim().toUpperCase();
        if (!KEY_REGEX.test(cleanKey)) {
          res.status(400).json({ error: 'INVALID_KEY', message: 'Invalid license key format' });
          return;
        }

        const keyRecord = db.prepare('SELECT * FROM license_keys WHERE key = ?').get(cleanKey) as any;
        if (!keyRecord) {
          res.status(404).json({ error: 'KEY_NOT_FOUND', message: 'License key not found' });
          return;
        }

        if (keyRecord.status === 'REVOKED') {
          res.status(403).json({ error: 'REVOKED', message: 'License key is revoked' });
          return;
        }

        // If bound to another device
        if (keyRecord.boundDeviceId && keyRecord.boundDeviceId !== body.deviceId) {
          // Check 30-day automatic transfer rule
          const THIRTY_DAYS_MS = 30 * 86400 * 1000;
          const lastBoundMs = keyRecord.lastBoundAt ? new Date(keyRecord.lastBoundAt).getTime() : 0;
          if (nowMs - lastBoundMs < THIRTY_DAYS_MS) {
            res.status(409).json({
              error: 'KEY_ON_OTHER_DEVICE',
              message: 'License key is currently active on another device (minimum 30 days before rebind)',
            });
            return;
          }
          // Older than 30 days -> allow automatic rebind
        }

        // Activate or update key
        const isFirstActivation = !keyRecord.activatedAt;
        const planEndsAt = isFirstActivation
          ? new Date(nowMs + keyRecord.planDays * 86400 * 1000).toISOString()
          : keyRecord.expiresAt;

        db.transaction(() => {
          db.prepare(`
            UPDATE license_keys SET
              status = 'ACTIVE',
              boundDeviceId = ?,
              activatedAt = COALESCE(activatedAt, ?),
              expiresAt = ?,
              lastBoundAt = ?
            WHERE id = ?
          `).run(
            body.deviceId,
            nowIso,
            planEndsAt,
            nowIso,
            keyRecord.id
          );

          db.prepare(`
            UPDATE devices SET
              keyId = ?,
              planEndsAt = ?
            WHERE id = ?
          `).run(
            keyRecord.id,
            planEndsAt,
            device.id
          );
        })();

        device = db.prepare('SELECT * FROM devices WHERE id = ?').get(device.id) as any;
      }

      // 7. Calculate Status and Active Windows
      let computedStatus: 'TRIAL' | 'ACTIVE' | 'FREE' | 'REVOKED' | 'UNKNOWN' = 'FREE';

      if (device.killed) {
        computedStatus = 'REVOKED';
        responseMessage = responseMessage || 'Device has been blocked by administrator';
      } else if (device.revoked) {
        computedStatus = 'REVOKED';
        responseMessage = responseMessage || 'Device access has been revoked';
      } else if (isUnknownSigner) {
        computedStatus = 'UNKNOWN';
      } else if (device.planEndsAt && new Date(device.planEndsAt).getTime() > nowMs) {
        computedStatus = 'ACTIVE';
      } else if (device.giftEndsAt && new Date(device.giftEndsAt).getTime() > nowMs) {
        computedStatus = 'ACTIVE';
      } else if (device.trialEndsAt && new Date(device.trialEndsAt).getTime() > nowMs) {
        computedStatus = 'TRIAL';
      } else {
        computedStatus = 'FREE';
      }

      // Update computed status in database for consistency
      db.prepare('UPDATE devices SET status = ? WHERE id = ?').run(computedStatus, device.id);

      // Remaining gift days
      let remainingGiftDays = 0;
      if (device.giftEndsAt) {
        const giftLeftMs = new Date(device.giftEndsAt).getTime() - nowMs;
        if (giftLeftMs > 0) {
          remainingGiftDays = Math.ceil(giftLeftMs / (86400 * 1000));
        }
      }

      // 8. Construct response payload without signature
      const responsePayload = {
        status: computedStatus,
        planEndsAt: device.planEndsAt || null,
        trialEndsAt: device.trialEndsAt || null,
        serverTime: nowIso,
        freeDailyList: freeDailyList,
        freeDailyDetails: freeDailyDetails,
        minVersionCode: minVersionCode,
        latestVersionCode: latestVersionCode,
        killed: Boolean(device.killed),
        giftDays: remainingGiftDays,
        priceText: priceText,
        supportWhatsapp: supportWhatsapp,
        message: responseMessage,
        nonce: body.nonce,
      };

      // 9. Sign payload with server ECDSA P-256 private key
      const config = loadEnv();
      const signer = new ECDSAKeySigner(config.SERVER_SIGNING_PRIVATE_KEY);
      const signature = signer.signCanonical(responsePayload);

      res.status(200).json({
        ...responsePayload,
        signature,
      });
    } catch (err) {
      next(err);
    }
  }
);
