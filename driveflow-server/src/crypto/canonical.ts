/**
 * Formats a JavaScript value into canonical JSON:
 * - Object keys sorted alphabetically at every level.
 * - No whitespace between tokens.
 * - Primitive types formatted standardly (null, boolean, number, string).
 * - Arrays maintain element order and canonicalize their items.
 */
export function canonicalJson(value: unknown): string {
  if (value === null || typeof value !== 'object') {
    return JSON.stringify(value);
  }

  if (Array.isArray(value)) {
    const items = value.map(item => canonicalJson(item));
    return '[' + items.join(',') + ']';
  }

  const obj = value as Record<string, unknown>;
  const sortedKeys = Object.keys(obj).sort();
  const pairs = sortedKeys.map(k => `${JSON.stringify(k)}:${canonicalJson(obj[k])}`);
  return '{' + pairs.join(',') + '}';
}
