import crypto from 'node:crypto';

export function generateSigningKeys() {
  const { privateKey, publicKey } = crypto.generateKeyPairSync('ec', {
    namedCurve: 'prime256v1', // NIST P-256 / secp256r1
  });

  const privateKeyPem = privateKey.export({ type: 'pkcs8', format: 'pem' }).toString();
  const privateKeyDer = privateKey.export({ type: 'pkcs8', format: 'der' });
  const privateKeyBase64 = privateKeyDer.toString('base64');

  const publicKeyDer = publicKey.export({ type: 'spki', format: 'der' });
  const publicKeySpkiBase64 = publicKeyDer.toString('base64');

  return {
    privateKeyPem,
    privateKeyBase64,
    publicKeySpkiBase64,
  };
}

if (process.argv[1]?.endsWith('gen-keys.ts') || process.argv[1]?.endsWith('gen-keys.js')) {
  const keys = generateSigningKeys();
  console.log('================================================================');
  console.log('DRIVEFLOW ECDSA P-256 SIGNING KEYS');
  console.log('================================================================');
  console.log('\n[1] Server Private Key (for SERVER_SIGNING_PRIVATE_KEY in .env):');
  console.log(keys.privateKeyPem.trim());
  console.log('\nAlternative (single-line Base64 PKCS#8):');
  console.log(keys.privateKeyBase64);
  console.log('\n[2] Client Public Key (SPKI Base64 for Android BuildConfig.SERVER_PUBLIC_KEY):');
  console.log(keys.publicKeySpkiBase64);
  console.log('\n================================================================\n');
}
