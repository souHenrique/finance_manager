import { computed, Injectable, signal } from '@angular/core';

interface StoredSession {
  token: string;
  tokenType: string;
  expiresAt: number;
}

const SESSION_STORAGE_KEY = 'nummo.session';

@Injectable({
  providedIn: 'root',
})
export class SessionService {
  private readonly sessionState = signal<StoredSession | null>(this.restoreSession());

  private expirationTimer: ReturnType<typeof setTimeout> | undefined;

  readonly isAuthenticated = computed(() => {
    return this.getAuthorizationHeader() !== null;
  });

  constructor() {
    this.scheduleExpiration();
  }

  start(token: string, tokenType: string, expiresInSeconds: number): void {
    const normalizedToken = token.trim();
    const normalizedTokenType = tokenType.trim();

    if (
      !normalizedToken ||
      !normalizedTokenType ||
      !Number.isFinite(expiresInSeconds) ||
      expiresInSeconds <= 0
    ) {
      this.clear();
      throw new Error('Resposta de autenticação inválida.');
    }

    const session: StoredSession = {
      token: normalizedToken,
      tokenType: normalizedTokenType,
      expiresAt: Date.now() + expiresInSeconds * 1000,
    };

    this.sessionState.set(session);
    sessionStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(session));

    this.scheduleExpiration();
  }

  hasValidSession(): boolean {
    return this.getAuthorizationHeader() !== null;
  }

  getAuthorizationHeader(): string | null {
    const session = this.sessionState();

    if (!session) {
      return null;
    }

    if (session.expiresAt <= Date.now()) {
      this.clear();
      return null;
    }

    return `${session.tokenType} ${session.token}`;
  }

  clear(): void {
    if (this.expirationTimer) {
      clearTimeout(this.expirationTimer);
      this.expirationTimer = undefined;
    }

    this.sessionState.set(null);
    sessionStorage.removeItem(SESSION_STORAGE_KEY);
  }

  private restoreSession(): StoredSession | null {
    const serializedSession = sessionStorage.getItem(SESSION_STORAGE_KEY);

    if (!serializedSession) {
      return null;
    }

    try {
      const session: unknown = JSON.parse(serializedSession);

      if (!this.isStoredSession(session) || session.expiresAt <= Date.now()) {
        sessionStorage.removeItem(SESSION_STORAGE_KEY);
        return null;
      }

      return session;
    } catch {
      sessionStorage.removeItem(SESSION_STORAGE_KEY);
      return null;
    }
  }

  private isStoredSession(value: unknown): value is StoredSession {
    if (typeof value !== 'object' || value === null) {
      return false;
    }

    const candidate = value as Record<string, unknown>;

    return (
      typeof candidate['token'] === 'string' &&
      typeof candidate['tokenType'] === 'string' &&
      typeof candidate['expiresAt'] === 'number'
    );
  }

  private scheduleExpiration(): void {
    if (this.expirationTimer) {
      clearTimeout(this.expirationTimer);
    }

    const session = this.sessionState();

    if (!session) {
      return;
    }

    const delay = session.expiresAt - Date.now();

    if (delay <= 0) {
      this.clear();
      return;
    }

    this.expirationTimer = setTimeout(() => {
      this.clear();
    }, delay);
  }
}
