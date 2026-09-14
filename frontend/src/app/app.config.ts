import { DialogModule } from '@angular/cdk/dialog';
import { provideHttpClient } from '@angular/common/http';
import {
  ApplicationConfig,
  importProvidersFrom,
  provideBrowserGlobalErrorListeners,
} from '@angular/core';
import { provideRouter } from '@angular/router';
import { environment } from '../environments/environment';
import { API_BASE_URL } from './core/config/api-base-url';
import { routes } from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideHttpClient(),
    importProvidersFrom(DialogModule),
    {
      provide: API_BASE_URL,
      useValue: environment.apiBaseUrl,
    },
  ],
};
