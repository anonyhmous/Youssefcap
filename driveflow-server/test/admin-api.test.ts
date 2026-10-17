import { describe, it, expect, beforeEach } from 'vitest';
import request from 'supertest';
import { setupTestEnvironment } from './test-helper.js';
import { KEY_ALPHABET } from '../src/routes/admin/keys.js';

describe('Admin API Endpoints (/admin/api)', () => {
  let app: any;
  let db: any;
  let testPassword: string;
  let adminToken: string;

  beforeEach(async () => {
    const env = setupTestEnvironment();
    app = env.app;
    db = env.db;
    testPassword = env.testPassword;

    // Login to get a valid admin token
    const loginRes = await request(app)
      .post('/admin/api/login')
      .send({
        username: 'testadmin',
        password: testPassword,
      });

    adminToken = loginRes.body.token;
  });

  it('requires admin auth on every /admin/api route except login', async () => {
    const routesToTest = [
      { method: 'get', path: '/admin/api/stats' },
      { method: 'get', path: '/admin/api/devices' },
      { method: 'get', path: '/admin/api/keys' },
      { method: 'get', path: '/admin/api/affiliates' },
      { method: 'get', path: '/admin/api/reports' },
      { method: 'get', path: '/admin/api/news' },
      { method: 'get', path: '/admin/api/reader-configs' },
      { method: 'get', path: '/admin/api/releases' },
      { method: 'get', path: '/admin/api/settings' },
    ];

    for (const route of routesToTest) {
      // @ts-ignore
      const res = await request(app)[route.method](route.path);
      expect(res.status).toBe(401);
      expect(res.body.error).toBe('UNAUTHORIZED');
    }
  });

  it('authenticates admin with valid credentials and rejects bad credentials', async () => {
    // Valid login
    const resGood = await request(app)
      .post('/admin/api/login')
      .send({ username: 'testadmin', password: testPassword });
    expect(resGood.status).toBe(200);
    expect(resGood.body.token).toBeDefined();
    expect(resGood.body.expiresAt).toBeDefined();

    // Invalid password
    const resBad = await request(app)
      .post('/admin/api/login')
      .send({ username: 'testadmin', password: 'wrongpassword' });
    expect(resBad.status).toBe(401);
    expect(resBad.body.error).toBe('INVALID_CREDENTIALS');
  });

  it('generates license keys conforming to DF-XXXX-XXXX-XXXX and allowed alphabet', async () => {
    const res = await request(app)
      .post('/admin/api/keys')
      .set('Authorization', `Bearer ${adminToken}`)
      .send({
        count: 50,
        planDays: 30,
        note: 'Batch 1 for VIP drivers',
      });

    expect(res.status).toBe(200);
    expect(res.body.keys).toHaveLength(50);

    const generatedSet = new Set<string>();
    const KEY_REGEX = /^DF-[A-HJ-NP-Z2-9]{4}-[A-HJ-NP-Z2-9]{4}-[A-HJ-NP-Z2-9]{4}$/;

    for (const item of res.body.keys) {
      expect(item.key).toMatch(KEY_REGEX);
      expect(item.planDays).toBe(30);

      // Verify no 'I' or 'O' or '0' or '1'
      const rawChars = item.key.replace(/-/g, '').replace('DF', '');
      for (const char of rawChars) {
        expect(KEY_ALPHABET).toContain(char);
      }

      // Check uniqueness in generated batch
      expect(generatedSet.has(item.key)).toBe(false);
      generatedSet.add(item.key);
    }
  });

  it('supports device actions: revoke, restore, extend, gift, kill, unbind', async () => {
    // Seed device
    const nowIso = new Date().toISOString();
    const info = db.prepare(`
      INSERT INTO devices (deviceId, name, status, firstSeenAt, lastSeenAt)
      VALUES ('actiondevice111111111111111111', 'Tamer', 'FREE', ?, ?)
    `).run(nowIso, nowIso);
    const deviceIdNum = Number(info.lastInsertRowid);

    // 1. Extend
    const resExtend = await request(app)
      .post(`/admin/api/devices/${deviceIdNum}/extend`)
      .set('Authorization', `Bearer ${adminToken}`)
      .send({ days: 30 });
    expect(resExtend.status).toBe(200);
    expect(resExtend.body.status).toBe('ACTIVE');

    // 2. Gift
    const resGift = await request(app)
      .post(`/admin/api/devices/${deviceIdNum}/gift`)
      .set('Authorization', `Bearer ${adminToken}`)
      .send({ days: 7 });
    expect(resGift.status).toBe(200);

    // 3. Revoke
    const resRevoke = await request(app)
      .post(`/admin/api/devices/${deviceIdNum}/revoke`)
      .set('Authorization', `Bearer ${adminToken}`)
      .send({});
    expect(resRevoke.status).toBe(200);
    expect(resRevoke.body.status).toBe('REVOKED');

    // 4. Restore
    const resRestore = await request(app)
      .post(`/admin/api/devices/${deviceIdNum}/restore`)
      .set('Authorization', `Bearer ${adminToken}`)
      .send({});
    expect(resRestore.status).toBe(200);
    expect(resRestore.body.status).toBe('ACTIVE');

    // 5. Kill & Unkill
    const resKill = await request(app)
      .post(`/admin/api/devices/${deviceIdNum}/kill`)
      .set('Authorization', `Bearer ${adminToken}`)
      .send({});
    expect(resKill.status).toBe(200);
    expect(resKill.body.killed).toBe(true);

    const resUnkill = await request(app)
      .post(`/admin/api/devices/${deviceIdNum}/unkill`)
      .set('Authorization', `Bearer ${adminToken}`)
      .send({});
    expect(resUnkill.status).toBe(200);
    expect(resUnkill.body.killed).toBe(false);

    // Verify audit logs were written
    const audits = db.prepare('SELECT * FROM admin_audit').all();
    expect(audits.length).toBeGreaterThanOrEqual(6);
  });

  it('updates and increments reader-configs revisions', async () => {
    const resGet = await request(app)
      .get('/admin/api/reader-configs')
      .set('Authorization', `Bearer ${adminToken}`);
    expect(resGet.status).toBe(200);
    expect(resGet.body).toHaveLength(3);

    const initialRev = resGet.body.find((c: any) => c.app === 'indrive').revision;

    const resPut = await request(app)
      .put('/admin/api/reader-configs/indrive')
      .set('Authorization', `Bearer ${adminToken}`)
      .send({
        config: { customField: 'test_value' },
      });

    expect(resPut.status).toBe(200);
    expect(resPut.body.revision).toBe(initialRev + 1);
    expect(resPut.body.config.customField).toBe('test_value');
  });

  it('reads and updates global settings with exact keys', async () => {
    const resGet = await request(app)
      .get('/admin/api/settings')
      .set('Authorization', `Bearer ${adminToken}`);

    expect(resGet.status).toBe(200);
    expect(resGet.body.trialDays).toBe(7);
    expect(resGet.body.freeDailyList).toBe(20);
    expect(resGet.body.freeDailyDetails).toBe(5);

    const resPut = await request(app)
      .put('/admin/api/settings')
      .set('Authorization', `Bearer ${adminToken}`)
      .send({
        trialDays: 14,
        freeDailyList: 25,
        freeDailyDetails: 10,
        minVersionCode: 2,
        priceText: '80 EGP / شهر',
        supportWhatsapp: '+201011112222',
        giftDaysDefault: 3,
        expectedSignerSha256: '',
      });

    expect(resPut.status).toBe(200);
    expect(resPut.body.trialDays).toBe(14);
    expect(resPut.body.priceText).toBe('80 EGP / شهر');
  });
});
