import crypto from 'node:crypto';
import { canonicalJson } from './canonical.js';

export class ECDSAKeySigner {
  private privateKeyObject: crypto.KeyObject;

  constructor(privateKeyInput: string) {
    const cleanKey = privateKeyInput.trim();
    if (cleanKey.includes('BEGIN PRIVATE KEY')) {
      this.privateKeyObject = crypto.createPrivateKey({
        key: cleanKey,
        format: 'pem',
      });
    } else {
      // Treat as raw Base64 DER
      const derBuffer = Buffer.from(cleanKey, 'base64');
      this.privateKeyObject = crypto.createPrivateKey({
        key: derBuffer,
        format: 'der',
        type: 'pkcs8',
      });
    }
  }

  /**
   * Signs the canonical JSON of the input object using ECDSA P-256 + SHA-256 (DER format)
   * and returns the signature as a base64 string.
   */
  signCanonical(data: unknown): string {
    const canonicalString = canonicalJson(data);
    const sign = crypto.createSign('SHA256');
    sign.update(canonicalString, 'utf8');
    sign.end();
    // In Node.js, ECDSA signatures with createSign are encoded in DER format by default
    const signatureBuffer = sign.sign(this.privateKeyObject);
    return signatureBuffer.toString('base64');
  }

  /**
   * Exports the matching public key as an SPKI base64 string
   */
  getPublicKeySpkiBase64(): string {
    const pubKey = crypto.createPublicKey(this.privateKeyObject);
    return pubKey.export({ type: 'spki', format: 'der' }).toString('base64');
  }
}

/**
 * Verifies that the base64 ECDSA signature matches the canonical JSON of data.
 */
export function verifyCanonicalSignature(
  data: unknown,
  signatureBase64: string,
  publicKeySpkiBase64OrPem: string
): boolean {
  try {
    const canonicalString = canonicalJson(data);
    let pubKeyObject: crypto.KeyObject;
    const cleanKey = publicKeySpkiBase64OrPem.trim();

    if (cleanKey.includes('BEGIN PUBLIC KEY')) {
      pubKeyObject = crypto.createPublicKey({
        key: cleanKey,
        format: 'pem',
      });
    } else {
      pubKeyObject = crypto.createPublicKey({
        key: Buffer.from(cleanKey, 'base64'),
        format: 'der',
        type: 'spki',
      });
    }

    const verify = crypto.createVerify('SHA256');
    verify.update(canonicalString, 'utf8');
    verify.end();
    const sigBuffer = Buffer.from(signatureBase64, 'base64');
    return verify.verify(pubKeyObject, sigBuffer);
  } catch {
    return false;
  }
}
