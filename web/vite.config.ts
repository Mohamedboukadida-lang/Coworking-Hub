/// <reference types="vitest" />
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// The browser talks to the same origin; /api is forwarded to the Spring Boot backend.
// Override the target with API_URL (e.g. API_URL=http://localhost:9000 npm run dev).
const apiTarget = process.env.API_URL ?? 'http://localhost:8081';
const proxy = { '/api': { target: apiTarget, changeOrigin: true } };

export default defineConfig({
  plugins: [react()],
  server: { port: 5173, proxy },
  preview: { port: 4173, proxy },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./src/test-setup.ts'],
    css: false,
  },
});
