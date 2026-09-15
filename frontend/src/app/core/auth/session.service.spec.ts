import { TestBed } from '@angular/core/testing';
import { SessionService } from './session.service';

const SESSION_STORAGE_KEY = 'finance-manager.session';

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

  it('should store a valid session and expose its authorization header', () => {
    service = TestBed.inject(SessionService);

    service.start(' jwt-token ', ' Bearer ', 3600);

    expect(service.getAuthorizationHeader()).toBe('Bearer jwt-token');
    expect(service.hasValidSession()).toBe(true);
    expect(service.isAuthenticated()).toBe(true);
    expect(JSON.parse(sessionStorage.getItem(SESSION_STORAGE_KEY) ?? '')).toEqual({
      token: 'jwt-token',
      tokenType: 'Bearer',
      expiresAt: Date.now() + 3_600_000,
    });
  });

  it('should restore a valid session stored by a previous page load', () => {
    sessionStorage.setItem(
      SESSION_STORAGE_KEY,
      JSON.stringify({
        token: 'restored-token',
        tokenType: 'Bearer',
        expiresAt: Date.now() + 3_600_000,
      }),
    );

    service = TestBed.inject(SessionService);

    expect(service.getAuthorizationHeader()).toBe('Bearer restored-token');
  });

  it('should discard an expired session during restoration', () => {
    sessionStorage.setItem(
      SESSION_STORAGE_KEY,
      JSON.stringify({
        token: 'expired-token',
        tokenType: 'Bearer',
        expiresAt: Date.now() - 1,
      }),
    );

    service = TestBed.inject(SessionService);

    expect(service.getAuthorizationHeader()).toBeNull();
    expect(sessionStorage.getItem(SESSION_STORAGE_KEY)).toBeNull();
  });

  it('should end the session when its expiration is reached', () => {
    service = TestBed.inject(SessionService);
    service.start('jwt-token', 'Bearer', 1);

    vi.advanceTimersByTime(1_000);

    expect(service.hasValidSession()).toBe(false);
    expect(service.getAuthorizationHeader()).toBeNull();
    expect(sessionStorage.getItem(SESSION_STORAGE_KEY)).toBeNull();
  });

  it('should clear the in-memory and persisted session on logout', () => {
    service = TestBed.inject(SessionService);
    service.start('jwt-token', 'Bearer', 3600);

    service.clear();

    expect(service.isAuthenticated()).toBe(false);
    expect(sessionStorage.getItem(SESSION_STORAGE_KEY)).toBeNull();
  });

  it('should reject an invalid authentication response', () => {
    service = TestBed.inject(SessionService);
    service.start('jwt-token', 'Bearer', 3600);

    expect(() => service?.start('', 'Bearer', 3600)).toThrow('Resposta de autenticação inválida.');
    expect(service.getAuthorizationHeader()).toBeNull();
  });
});
