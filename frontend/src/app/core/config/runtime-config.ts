import { environment } from '../../../environments/environment';

interface RuntimeConfigPayload {
  apiBaseUrl?: unknown;
}

export interface RuntimeConfig {
  apiBaseUrl: string;
}

const RUNTIME_CONFIG_PATH = '/runtime-config.json';

export async function loadRuntimeConfig(fetcher: typeof fetch = fetch): Promise<RuntimeConfig> {
  try {
    const response = await fetcher(RUNTIME_CONFIG_PATH, { cache: 'no-store' });

    if (!response.ok) {
      return fallbackConfig();
    }

    const payload: unknown = await response.json();
    const apiBaseUrl = normalizeApiBaseUrl(payload);

    return { apiBaseUrl: apiBaseUrl ?? environment.apiBaseUrl };
  } catch {
    return fallbackConfig();
  }
}

function fallbackConfig(): RuntimeConfig {
  return { apiBaseUrl: environment.apiBaseUrl };
}

function normalizeApiBaseUrl(payload: unknown): string | undefined {
  if (!isRuntimeConfigPayload(payload) || typeof payload.apiBaseUrl !== 'string') {
    return undefined;
  }

  const apiBaseUrl = payload.apiBaseUrl.trim().replace(/\/+$/, '');

  if (!apiBaseUrl || /[\s@]/.test(apiBaseUrl)) {
    return undefined;
  }

  if (apiBaseUrl.startsWith('/') && !apiBaseUrl.startsWith('//')) {
    return apiBaseUrl;
  }

  try {
    const parsedUrl = new URL(apiBaseUrl);

    return parsedUrl.protocol === 'https:' ? apiBaseUrl : undefined;
  } catch {
    return undefined;
  }
}

function isRuntimeConfigPayload(value: unknown): value is RuntimeConfigPayload {
  return typeof value === 'object' && value !== null;
}
