import { describe, it, expect, beforeEach } from 'vitest';
import request from 'supertest';
import { setupTestEnvironment } from './test-helper.js';

describe('POST /v1/events Endpoint', () => {
  let app: any;
  let db: any;

  beforeEach(() => {
    const env = setupTestEnvironment();
    app = env.app;
    db = env.db;
  });

  const deviceId = 'a1b2c3d4e5f60718293a4b5c6d7e8f90';

  it('rejects event submission if consentVersion is 0 (consent required)', async () => {
    const res = await request(app)
      .post('/v1/events')
      .set('X-DF-Device', deviceId)
      .send({
        deviceId,
        consentVersion: 0,
        events: [
          {
            type: 'app_launch',
            value: 'v1.0.0',
            app: 'indrive',
            ref: null,
            ts: new Date().toISOString(),
          },
        ],
      });

    expect(res.status).toBe(400);
    expect(res.body.error).toBe('BAD_REQUEST');
  });

  it('rejects events with address-like strings or free text in value', async () => {
    // Address with spaces and Arabic characters
    const resAddress = await request(app)
      .post('/v1/events')
      .set('X-DF-Device', deviceId)
      .send({
        deviceId,
        consentVersion: 1,
        events: [
          {
            type: 'pickup_location',
            value: 'المعادي شارع 9',
            app: 'indrive',
            ref: null,
            ts: new Date().toISOString(),
          },
        ],
      });

    expect(resAddress.status).toBe(400);
    expect(resAddress.body.error).toBe('BAD_REQUEST');

    // Free text English with spaces
    const resText = await request(app)
      .post('/v1/events')
      .set('X-DF-Device', deviceId)
      .send({
        deviceId,
        consentVersion: 1,
        events: [
          {
            type: 'rider_comment',
            value: 'Please hurry up driver!',
            app: 'uber',
            ref: null,
            ts: new Date().toISOString(),
          },
        ],
      });

    expect(resText.status).toBe(400);
    expect(resText.body.error).toBe('BAD_REQUEST');
  });

  it('accepts valid telemetry events with numbers, codes, and enum app identifiers', async () => {
    const validEvents = [
      {
        type: 'screen_read',
        value: 'success_120egp',
        app: 'indrive',
        ref: 'req_001',
        ts: new Date().toISOString(),
      },
      {
        type: 'verdict_eval',
        value: 'GREEN_PRICE_OK',
        app: 'didi',
        ref: null,
        ts: new Date().toISOString(),
      },
    ];

    const res = await request(app)
      .post('/v1/events')
      .set('X-DF-Device', deviceId)
      .send({
        deviceId,
        consentVersion: 1,
        events: validEvents,
      });

    expect(res.status).toBe(200);
    expect(res.body.accepted).toBe(2);

    // Verify stored in DB
    const count = db.prepare('SELECT COUNT(*) as count FROM events WHERE deviceId = ?').get(deviceId) as any;
    expect(count.count).toBe(2);
  });
});
