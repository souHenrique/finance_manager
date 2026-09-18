import { describe, expect, it, vi } from 'vitest';

import { loadRuntimeConfig } from './runtime-config';

describe('loadRuntimeConfig', () => {
  it('should use the public HTTPS API URL supplied at runtime', async () => {
    const fetcher = vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ apiBaseUrl: 'https://api.example.com/api/v1/' }), {
        status: 200,
      }),
    );

    await expect(loadRuntimeConfig(fetcher)).resolves.toEqual({
      apiBaseUrl: 'https://api.example.com/api/v1',
    });
    expect(fetcher).toHaveBeenCalledWith('/runtime-config.json', { cache: 'no-store' });
  });

  it.each(['http://api.example.com/api/v1', '//api.example.com/api/v1'])(
    'should reject an invalid or unsafe runtime URL and use the local fallback',
    async (apiBaseUrl) => {
      const fetcher = vi.fn().mockResolvedValue(
        new Response(JSON.stringify({ apiBaseUrl }), {
          status: 200,
        }),
      );

      await expect(loadRuntimeConfig(fetcher)).resolves.toEqual({ apiBaseUrl: '/api/v1' });
    },
  );

  it('should keep the application available when the public configuration cannot be loaded', async () => {
    const fetcher = vi.fn().mockRejectedValue(new Error('Network unavailable'));

    await expect(loadRuntimeConfig(fetcher)).resolves.toEqual({ apiBaseUrl: '/api/v1' });
  });
});
