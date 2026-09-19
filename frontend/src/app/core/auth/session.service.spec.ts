import { TestBed } from '@angular/core/testing';
import { SessionService } from './session.service';

const SESSION_STORAGE_KEY = 'nummo.session-expiration';

describe('SessionService', () => {
  let service: SessionService | undefined;

  beforeEach(() => {
    TestBed.resetTestingModule();
    sessionStorage.clear();
    vi.useFakeTimers();
    vi.setSystemTime(new Date('2026-09-15T12:00:00.000Z'));
    TestBed.configureTestingModule({});
  });

  afterEach(() => {
    service?.clear();
    sessionStorage.clear();
    vi.clearAllTimers();
    vi.useRealTimers();
  });

  it('stores only non-sensitive session expiration metadata', () => {
    service = TestBed.inject(SessionService);
    service.start(3600);

    expect(service.hasValidSession()).toBe(true);
    expect(service.isAuthenticated()).toBe(true);
    expect(JSON.parse(sessionStorage.getItem(SESSION_STORAGE_KEY) ?? '')).toEqual({
      expiresAt: Date.now() + 3_600_000,
    });
    expect(sessionStorage.getItem(SESSION_STORAGE_KEY)).not.toContain('token');
  });

  it('restores valid non-sensitive metadata after a page reload', () => {
    sessionStorage.setItem(
      SESSION_STORAGE_KEY,
      JSON.stringify({ expiresAt: Date.now() + 3_600_000 }),
    );
    service = TestBed.inject(SessionService);

    expect(service.hasValidSession()).toBe(true);
  });

  it('discards expired metadata during restoration', () => {
    sessionStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify({ expiresAt: Date.now() - 1 }));
    service = TestBed.inject(SessionService);

    expect(service.hasValidSession()).toBe(false);
    expect(sessionStorage.getItem(SESSION_STORAGE_KEY)).toBeNull();
  });

  it('ends the client session state when its expiration is reached', () => {
    service = TestBed.inject(SessionService);
    service.start(1);
    vi.advanceTimersByTime(1_000);

    expect(service.hasValidSession()).toBe(false);
    expect(sessionStorage.getItem(SESSION_STORAGE_KEY)).toBeNull();
  });

  it('clears the in-memory and persisted metadata on logout', () => {
    service = TestBed.inject(SessionService);
    service.start(3600);
    service.clear();

    expect(service.isAuthenticated()).toBe(false);
    expect(sessionStorage.getItem(SESSION_STORAGE_KEY)).toBeNull();
  });

  it('rejects an invalid authentication response', () => {
    service = TestBed.inject(SessionService);
    service.start(3600);

    expect(() => service?.start(0)).toThrow('Resposta de autenticação inválida.');
    expect(service.hasValidSession()).toBe(false);
  });
});
