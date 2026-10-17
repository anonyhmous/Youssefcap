import { describe, it, expect, beforeEach } from 'vitest';
import request from 'supertest';
import { setupTestEnvironment } from './test-helper.js';
import { verifyCanonicalSignature } from '../src/crypto/signer.js';

describe('POST /v1/activate Endpoint', () => {
  let app: any;
  let db: any;
  let keys: any;

  beforeEach(() => {
    const env = setupTestEnvironment();
    app = env.app;
    db = env.db;
    keys = env.keys;
  });

  const validDeviceId = 'e3b0c44298fc1c149afbf4c8996fb924';
  const validBasePayload = {
    deviceId: validDeviceId,
    licenseKey: null,
    affiliateCode: null,
    appVersionCode: 1,
    appVersionName: '1.0.0',
    signerSha256: 'dummy-signer-hash',
    androidSdk: 34,
    deviceModel: 'Samsung SM-A175F',
    lang: 'ar',
    market: 'EG',
    governorate: 'Cairo',
    contactName: 'Ahmed Driver',
    contactPhone: '+201012345678',
    nonce: 'random16characterslongnonce123',
  };

  it('rejects activation if X-DF-Device header is missing or mismatches', async () => {
    // Missing header
    const res1 = await request(app)
      .post('/v1/activate')
      .send(validBasePayload);
    expect(res1.status).toBe(400);
    expect(res1.body.error).toBe('BAD_REQUEST');

    // Mismatched header
    const res2 = await request(app)
      .post('/v1/activate')
      .set('X-DF-Device', 'differentdeviceid12345678901234')
      .send(validBasePayload);
    expect(res2.status).toBe(400);
    expect(res2.body.error).toBe('BAD_REQUEST');
  });

  it('starts 7-day TRIAL for new device with verifiable ECDSA P-256 signature', async () => {
    const res = await request(app)
      .post('/v1/activate')
      .set('X-DF-Device', validDeviceId)
      .send(validBasePayload);

    expect(res.status).toBe(200);
    expect(res.body.status).toBe('TRIAL');
    expect(res.body.trialEndsAt).not.toBeNull();
    expect(res.body.planEndsAt).toBeNull();
    expect(res.body.freeDailyList).toBe(20);
    expect(res.body.freeDailyDetails).toBe(5);
    expect(res.body.nonce).toBe(validBasePayload.nonce);
    expect(typeof res.body.signature).toBe('string');

    // Verify ECDSA signature with client public key
    const responseWithoutSig = { ...res.body };
    delete responseWithoutSig.signature;

    const isVerified = verifyCanonicalSignature(
      responseWithoutSig,
      res.body.signature,
      keys.publicKeySpkiBase64
    );
    expect(isVerified).toBe(true);
  });

  it('returns FREE status when trial has expired and no active key exists', async () => {
    // Insert device with expired trial
    const expiredTrialDate = new Date(Date.now() - 24 * 3600 * 1000).toISOString();
    db.prepare(`
      INSERT INTO devices (deviceId, status, trialEndsAt, firstSeenAt, lastSeenAt)
      VALUES (?, 'TRIAL', ?, ?, ?)
    `).run(validDeviceId, expiredTrialDate, expiredTrialDate, expiredTrialDate);

    const res = await request(app)
      .post('/v1/activate')
      .set('X-DF-Device', validDeviceId)
      .send(validBasePayload);

    expect(res.status).toBe(200);
    expect(res.body.status).toBe('FREE');
  });

  it('handles license key errors: INVALID_KEY, KEY_NOT_FOUND, REVOKED', async () => {
    // 1. INVALID_KEY (bad format, lowercase or invalid characters like 0 or 1 or O)
    const resBad = await request(app)
      .post('/v1/activate')
      .set('X-DF-Device', validDeviceId)
      .send({ ...validBasePayload, licenseKey: 'INVALID-FORMAT' });
    expect(resBad.status).toBe(400);
    expect(resBad.body.error).toBe('INVALID_KEY');

    // 2. KEY_NOT_FOUND (valid syntax but not in DB)
    const resNotFound = await request(app)
      .post('/v1/activate')
      .set('X-DF-Device', validDeviceId)
      .send({ ...validBasePayload, licenseKey: 'DF-AAAA-BBBB-CCCC' });
    expect(resNotFound.status).toBe(404);
    expect(resNotFound.body.error).toBe('KEY_NOT_FOUND');

    // 3. REVOKED key (using valid alphabet characters A-Z without I/O and 2-9)
    db.prepare(`
      INSERT INTO license_keys (key, planDays, status, createdAt)
      VALUES ('DF-REVA-REVB-REVC', 30, 'REVOKED', ?)
    `).run(new Date().toISOString());

    const resRevoked = await request(app)
      .post('/v1/activate')
      .set('X-DF-Device', validDeviceId)
      .send({ ...validBasePayload, licenseKey: 'DF-REVA-REVB-REVC' });
    expect(resRevoked.status).toBe(403);
    expect(resRevoked.body.error).toBe('REVOKED');
  });

  it('enforces KEY_ON_OTHER_DEVICE when bound within 30 days, allows rebind after 30 days', async () => {
    const otherDevice = 'otherdevice00000000000000000000';
    const nowMs = Date.now();
    const tenDaysAgo = new Date(nowMs - 10 * 86400 * 1000).toISOString();
    const fortyDaysAgo = new Date(nowMs - 40 * 86400 * 1000).toISOString();

    // Key bound 10 days ago to other device (using valid alphabet characters)
    db.prepare(`
      INSERT INTO license_keys (key, planDays, status, boundDeviceId, activatedAt, expiresAt, lastBoundAt, createdAt)
      VALUES ('DF-SAFE-TEST-2222', 30, 'ACTIVE', ?, ?, ?, ?, ?)
    `).run(
      otherDevice,
      tenDaysAgo,
      new Date(nowMs + 20 * 86400 * 1000).toISOString(),
      tenDaysAgo,
      tenDaysAgo
    );

    // Activating on new device -> rejected with KEY_ON_OTHER_DEVICE
    const resBlocked = await request(app)
      .post('/v1/activate')
      .set('X-DF-Device', validDeviceId)
      .send({ ...validBasePayload, licenseKey: 'DF-SAFE-TEST-2222' });

    expect(resBlocked.status).toBe(409);
    expect(resBlocked.body.error).toBe('KEY_ON_OTHER_DEVICE');

    // Update lastBoundAt to 40 days ago
    db.prepare("UPDATE license_keys SET lastBoundAt = ? WHERE key = 'DF-SAFE-TEST-2222'").run(fortyDaysAgo);

    // Rebind attempt should now succeed and set status to ACTIVE
    const resAllowed = await request(app)
      .post('/v1/activate')
      .set('X-DF-Device', validDeviceId)
      .send({ ...validBasePayload, licenseKey: 'DF-SAFE-TEST-2222' });

    expect(resAllowed.status).toBe(200);
    expect(resAllowed.body.status).toBe('ACTIVE');

    // Verify key in DB is now bound to validDeviceId
    const keyInDb = db.prepare("SELECT boundDeviceId FROM license_keys WHERE key = 'DF-SAFE-TEST-2222'").get() as any;
    expect(keyInDb.boundDeviceId).toBe(validDeviceId);
  });

  it('handles affiliate and gift code errors: AFFILIATE_UNKNOWN, AFFILIATE_SELF, GIFT_EXPIRED, GIFT_FULL, GIFT_USED', async () => {
    const nowIso = new Date().toISOString();

    // 1. AFFILIATE_UNKNOWN
    const resUnknown = await request(app)
      .post('/v1/activate')
      .set('X-DF-Device', validDeviceId)
      .send({ ...validBasePayload, affiliateCode: 'NOEXIST' });
    expect(resUnknown.status).toBe(400);
    expect(resUnknown.body.error).toBe('AFFILIATE_UNKNOWN');

    // Insert affiliates for testing
    db.prepare(`
      INSERT INTO affiliates (code, ownerName, giftDays, maxUses, uses, expiresAt, createdAt)
      VALUES
        ('SELF01', 'Ahmed Driver', 5, 10, 0, NULL, ?),
        ('EXP001', 'Captain Sam', 5, 10, 0, ?, ?),
        ('FULL01', 'Captain Jack', 5, 2, 2, NULL, ?),
        ('VALID1', 'Captain Tarek', 7, 50, 0, NULL, ?)
    `).run(
      nowIso,
      new Date(Date.now() - 86400 * 1000).toISOString(),
      nowIso,
      nowIso,
      nowIso
    );

    // 2. AFFILIATE_SELF (matching contactName / ownerName)
    const resSelf = await request(app)
      .post('/v1/activate')
      .set('X-DF-Device', validDeviceId)
      .send({ ...validBasePayload, affiliateCode: 'SELF01' });
    expect(resSelf.status).toBe(400);
    expect(resSelf.body.error).toBe('AFFILIATE_SELF');

    // 3. GIFT_EXPIRED
    const resExp = await request(app)
      .post('/v1/activate')
      .set('X-DF-Device', validDeviceId)
      .send({ ...validBasePayload, affiliateCode: 'EXP001' });
    expect(resExp.status).toBe(400);
    expect(resExp.body.error).toBe('GIFT_EXPIRED');

    // 4. GIFT_FULL
    const resFull = await request(app)
      .post('/v1/activate')
      .set('X-DF-Device', validDeviceId)
      .send({ ...validBasePayload, affiliateCode: 'FULL01' });
    expect(resFull.status).toBe(400);
    expect(resFull.body.error).toBe('GIFT_FULL');

    // 5. Valid affiliate code applies gift days
    const resValid = await request(app)
      .post('/v1/activate')
      .set('X-DF-Device', validDeviceId)
      .send({ ...validBasePayload, affiliateCode: 'VALID1' });
    expect(resValid.status).toBe(200);
    expect(resValid.body.giftDays).toBe(7);

    // 6. GIFT_USED (redeeming again by the same device)
    const resUsed = await request(app)
      .post('/v1/activate')
      .set('X-DF-Device', validDeviceId)
      .send({ ...validBasePayload, affiliateCode: 'VALID1' });
    expect(resUsed.status).toBe(400);
    expect(resUsed.body.error).toBe('GIFT_USED');
  });
});
