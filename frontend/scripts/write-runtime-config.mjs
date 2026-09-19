import { writeFile } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';

const fallbackApiBaseUrl = '/api/v1';
const configuredApiBaseUrl = process.env.API_BASE_URL ?? fallbackApiBaseUrl;
const apiBaseUrl = normalizeApiBaseUrl(configuredApiBaseUrl);

if (!apiBaseUrl) {
  throw new Error(
    'API_BASE_URL must be an HTTPS URL or an application-relative path beginning with /.',
  );
}

const outputPath = fileURLToPath(
  new URL('../dist/nummo-web/browser/runtime-config.json', import.meta.url),
);

await writeFile(outputPath, `${JSON.stringify({ apiBaseUrl }, null, 2)}\n`, 'utf8');

function normalizeApiBaseUrl(value) {
  const normalizedValue = value.trim().replace(/\/+$/, '');

  if (!normalizedValue || /[\s@]/.test(normalizedValue)) {
    return undefined;
  }

  if (normalizedValue.startsWith('/') && !normalizedValue.startsWith('//')) {
    return normalizedValue;
  }

  try {
    const parsedUrl = new URL(normalizedValue);

    return parsedUrl.protocol === 'https:' ? normalizedValue : undefined;
  } catch {
    return undefined;
  }
}
