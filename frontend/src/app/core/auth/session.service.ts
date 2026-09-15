import { Injectable, signal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class SessionService {
  private readonly accessTokenState = signal<string | null>(null);

  readonly accessToken = this.accessTokenState.asReadonly();

  start(accessToken: string): void {
    this.accessTokenState.set(accessToken);
  }

  clear(): void {
    this.accessTokenState.set(null);
  }
}
