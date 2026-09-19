import { computed, Injectable, signal } from '@angular/core';

interface SessionMetadata {
  expiresAt: number;
}

const SESSION_STORAGE_KEY = 'nummo.session-expiration';

@Injectable({
  providedIn: 'root',
})
export class SessionService {
  private readonly sessionState = signal<SessionMetadata | null>(this.restoreSession());

  private expirationTimer: ReturnType<typeof setTimeout> | undefined;

  readonly isAuthenticated = computed(() => {
    return this.hasValidSession();
  });

  constructor() {
    this.scheduleExpiration();
  }

  start(expiresInSeconds: number): void {
    if (!Number.isFinite(expiresInSeconds) || expiresInSeconds <= 0) {
      this.clear();
      throw new Error('Resposta de autenticação inválida.');
    }

    const session: SessionMetadata = {
      expiresAt: Date.now() + expiresInSeconds * 1000,
    };

    this.sessionState.set(session);
    sessionStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(session));

    this.scheduleExpiration();
  }

  hasValidSession(): boolean {
    const session = this.sessionState();

    if (!session) {
      return false;
    }

    if (session.expiresAt <= Date.now()) {
      this.clear();
      return false;
    }

    return true;
  }

  clear(): void {
    if (this.expirationTimer) {
      clearTimeout(this.expirationTimer);
      this.expirationTimer = undefined;
    }

    this.sessionState.set(null);
    sessionStorage.removeItem(SESSION_STORAGE_KEY);
  }

  private restoreSession(): SessionMetadata | null {
    const serializedSession = sessionStorage.getItem(SESSION_STORAGE_KEY);

    if (!serializedSession) {
      return null;
    }

    try {
      const session: unknown = JSON.parse(serializedSession);

      if (!this.isSessionMetadata(session) || session.expiresAt <= Date.now()) {
        sessionStorage.removeItem(SESSION_STORAGE_KEY);
        return null;
      }

      return session;
    } catch {
      sessionStorage.removeItem(SESSION_STORAGE_KEY);
      return null;
    }
  }

  private isSessionMetadata(value: unknown): value is SessionMetadata {
    if (typeof value !== 'object' || value === null) {
      return false;
    }

    const candidate = value as Record<string, unknown>;

    return typeof candidate['expiresAt'] === 'number';
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
