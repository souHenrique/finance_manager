import { inject, Injectable } from '@angular/core';
import { API_BASE_URL } from '../config/api-base-url';

@Injectable({ providedIn: 'root' })
export class ApiUrlService {
  private readonly baseUrl = inject(API_BASE_URL);

  build(path: string): string {
    const normalizedPath = path.trim();

    if (/^https?:\/\//i.test(normalizedPath)) {
      throw new Error('ApiUrlService aceita somente caminhos relativos.');
    }

    const baseUrl = this.baseUrl.replace(/\/+$/, '');
    const resourcePath = normalizedPath.replace(/^\/+/, '');

    return resourcePath ? `${baseUrl}/${resourcePath}` : baseUrl;
  }
}
