import { bootstrapApplication } from '@angular/platform-browser';
import { createAppConfig } from './app/app.config';
import { App } from './app/app';
import { loadRuntimeConfig } from './app/core/config/runtime-config';

void loadRuntimeConfig().then(({ apiBaseUrl }) =>
  bootstrapApplication(App, createAppConfig(apiBaseUrl)),
);
