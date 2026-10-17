import { describe, it, expect } from 'vitest';
import { canonicalJson } from '../src/crypto/canonical.js';
import { ECDSAKeySigner, verifyCanonicalSignature } from '../src/crypto/signer.js';
import { generateSigningKeys } from '../scripts/gen-keys.js';

describe('Canonical JSON and ECDSA P-256 Signer', () => {
  it('serializes objects with sorted keys and no whitespace at all levels', () => {
    const input = {
      z: 1,
      b: {
        d: 'world',
        c: 'hello',
        a: [3, 1, 2],
      },
      a: null,
      m: false,
    };

    const expected = '{"a":null,"b":{"a":[3,1,2],"c":"hello","d":"world"},"m":false,"z":1}';
    expect(canonicalJson(input)).toBe(expected);
  });

  it('signs canonical JSON and verifies with matching public key', () => {
    const { privateKeyPem, publicKeySpkiBase64 } = generateSigningKeys();
    const signer = new ECDSAKeySigner(privateKeyPem);

    const payloadWithoutSignature = {
      status: 'ACTIVE',
      planEndsAt: '2026-11-17T00:00:00.000Z',
      trialEndsAt: null,
      serverTime: '2026-10-17T12:00:00.000Z',
      freeDailyList: 20,
      freeDailyDetails: 5,
      minVersionCode: 1,
      latestVersionCode: 1,
      killed: false,
      giftDays: 0,
      priceText: '50 EGP / month',
      supportWhatsapp: '+201000000000',
      message: null,
      nonce: 'random1234567890abcdef',
    };

    const signature = signer.signCanonical(payloadWithoutSignature);
    expect(typeof signature).toBe('string');
    expect(signature.length).toBeGreaterThan(60);

    const isValid = verifyCanonicalSignature(
      payloadWithoutSignature,
      signature,
      publicKeySpkiBase64
    );
    expect(isValid).toBe(true);

    // Tampered payload fails verification
    const tamperedPayload = { ...payloadWithoutSignature, status: 'FREE' };
    const isTamperedValid = verifyCanonicalSignature(
      tamperedPayload,
      signature,
      publicKeySpkiBase64
    );
    expect(isTamperedValid).toBe(false);
  });

  it('supports raw Base64 PKCS#8 private keys', () => {
    const { privateKeyBase64, publicKeySpkiBase64 } = generateSigningKeys();
    const signer = new ECDSAKeySigner(privateKeyBase64);

    const sample = { hello: 'driveflow', count: 42 };
    const sig = signer.signCanonical(sample);
    expect(verifyCanonicalSignature(sample, sig, publicKeySpkiBase64)).toBe(true);
  });
});
