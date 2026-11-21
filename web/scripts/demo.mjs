// Screenshots and a walkthrough video of the running web app, driven by Playwright.
// Needs the API seeded (npm run seed) and the app served (npm run preview).
// Usage: OUT=../docs/demo node scripts/demo.mjs
// Playwright is not a project dependency; install it once with `npm i -g playwright && npx playwright install chromium`.

import { mkdirSync, renameSync } from 'node:fs';
import { createRequire } from 'node:module';
import path from 'node:path';

const require = createRequire(import.meta.url);
let playwright;
try {
  playwright = require('playwright');
} catch {
  console.error('Playwright is not installed. Run: npm i -g playwright && npx playwright install chromium');
  process.exit(1);
}
const { chromium, devices } = playwright;

const WEB = process.env.WEB_URL ?? 'http://localhost:4173';
const API = process.env.API_URL ?? 'http://localhost:8081';
const OUT = path.resolve(process.env.OUT ?? 'demo-output');
const PASSWORD = 'demo-pass-123';
mkdirSync(OUT, { recursive: true });

function dayFromNow(n) {
  const d = new Date();
  d.setDate(d.getDate() + n);
  return d.toISOString().slice(0, 10);
}

async function login(email) {
  const res = await fetch(`${API}/api/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password: PASSWORD }),
  });
  if (!res.ok) throw new Error(`login ${email}: ${res.status}`);
  return (await res.json()).token;
}

async function spaceId(name) {
  const spaces = await (await fetch(`${API}/api/coworking-spaces`)).json();
  const found = spaces.find((s) => s.name === name);
  if (!found) throw new Error(`space ${name} not found; run the seed first`);
  return found.id;
}

/** Waits until the map tiles in view have loaded, so screenshots never show a grey map. */
async function settle(page) {
  await page.waitForLoadState('networkidle');
  await page.waitForFunction(() =>
    [...document.querySelectorAll('.leaflet-tile')].every((img) => img.complete),
  ).catch(() => {});
  await page.waitForTimeout(400);
}

async function context(browser, { token, ...options } = {}) {
  const ctx = await browser.newContext({ viewport: { width: 1440, height: 900 }, deviceScaleFactor: 2, ...options });
  if (token) await ctx.addInitScript((t) => localStorage.setItem('coworking-hub.token', t), token);
  return ctx;
}

const browser = await chromium.launch();
const youssef = await login('youssef@demo.coworkinghub.tn');
const sana = await login('sana@demo.coworkinghub.tn');
const hubLac = await spaceId('Hub Lac');
const atelier = await spaceId('Sidi Bou Said Atelier');
const shot = (page, name, opts = {}) => page.screenshot({ path: path.join(OUT, `${name}.png`), ...opts });

// ---------- Screenshots ----------
{
  const ctx = await context(browser);
  const page = await ctx.newPage();
  await page.goto(WEB);
  await settle(page);
  await page.locator('.space-card').first().hover();
  await page.waitForTimeout(300);
  await shot(page, '01-explore');
  await page.goto(`${WEB}/register`);
  await page.getByText('I want to list a space').click();
  await shot(page, '10-sign-up');
  await ctx.close();
}
{
  const ctx = await context(browser, { token: youssef });
  const page = await ctx.newPage();
  await page.goto(`${WEB}/spaces/${hubLac}`);
  await settle(page);
  await page.getByLabel('Date').fill(dayFromNow(14));
  await page.getByLabel('Start time').fill('10:00');
  await page.getByLabel('Duration').selectOption('3');
  await page.getByLabel('People').fill('4');
  await page.locator('label.chip', { hasText: 'Projector' }).click();
  await page.locator('label.chip', { hasText: 'Coffee machine' }).click();
  await page.waitForTimeout(200);
  // Full-page captures must start at the top, or the sticky header lands mid-page.
  await page.evaluate(() => window.scrollTo(0, 0));
  await page.waitForTimeout(200);
  await shot(page, '02-space-and-booking', { fullPage: true });
  await page.getByRole('button', { name: 'Request booking' }).click();
  await page.getByText('Request sent.').waitFor();
  await shot(page, '03-booking-requested');

  // The atelier has a single room: booking the same slot twice shows the conflict message.
  const slot = { date: dayFromNow(15), time: '11:00' };
  await page.goto(`${WEB}/spaces/${atelier}`);
  await settle(page);
  for (let attempt = 0; attempt < 2; attempt++) {
    await page.getByLabel('Date').fill(slot.date);
    await page.getByLabel('Start time').fill(slot.time);
    await page.getByRole('button', { name: 'Request booking' }).click();
    if (attempt === 0) {
      await page.getByText('Request sent.').waitFor();
      await page.getByRole('button', { name: 'Book another slot' }).click();
    }
  }
  await page.getByText('That time is taken.').waitFor();
  await page.locator('.booking-panel').scrollIntoViewIfNeeded();
  await shot(page, '04-slot-already-booked');

  await page.goto(`${WEB}/bookings`);
  await page.getByRole('heading', { name: 'My bookings' }).waitFor();
  await page.waitForTimeout(300);
  await shot(page, '05-my-bookings', { fullPage: true });
  await ctx.close();
}
{
  const ctx = await context(browser, { token: sana });
  const page = await ctx.newPage();
  await page.goto(`${WEB}/manage`);
  await page.getByText('Requests to review').first().waitFor();
  await page.waitForTimeout(400);
  await shot(page, '06-owner-dashboard', { fullPage: true });
  await page.goto(`${WEB}/manage/spaces/${hubLac}`);
  await page.getByRole('heading', { name: 'Rooms & prices' }).waitFor();
  await page.waitForTimeout(400);
  await shot(page, '07-manage-space', { fullPage: true });
  await page.goto(`${WEB}/manage/new`);
  await settle(page);
  await page.getByLabel('Name').fill('Gammarth Garden Desk');
  await page.getByLabel('Address').fill('Route Touristique, Gammarth');
  const map = page.locator('.picker-map');
  const box = await map.boundingBox();
  await page.mouse.click(box.x + box.width * 0.72, box.y + box.height * 0.3);
  await page.waitForTimeout(300);
  await shot(page, '08-new-space');
  await ctx.close();
}
{
  const ctx = await browser.newContext({ ...devices['iPhone 13'] });
  const page = await ctx.newPage();
  await page.goto(WEB);
  await settle(page);
  await shot(page, '09-mobile-explore');
  await page.goto(`${WEB}/spaces/${hubLac}`);
  await settle(page);
  await shot(page, '09-mobile-space', { fullPage: true });
  await ctx.close();
}
{
  const ctx = await context(browser, { colorScheme: 'dark', token: youssef });
  const page = await ctx.newPage();
  await page.goto(WEB);
  await settle(page);
  await shot(page, '11-explore-dark');
  await ctx.close();
}

// ---------- Walkthrough video ----------
{
  const ctx = await browser.newContext({
    viewport: { width: 1280, height: 800 },
    recordVideo: { dir: OUT, size: { width: 1280, height: 800 } },
  });
  const page = await ctx.newPage();
  const pause = (ms = 1200) => page.waitForTimeout(ms * 1.4);

  // A member explores and books.
  await page.goto(WEB);
  await settle(page);
  await pause(1200);
  await page.getByPlaceholder('Search by name or neighbourhood').pressSequentially('lac', { delay: 140 });
  await pause();
  await page.getByPlaceholder('Search by name or neighbourhood').fill('');
  await pause(500);
  for (const card of (await page.locator('.space-card').all()).slice(0, 3)) {
    await card.hover();
    await pause(500);
  }
  await page.getByRole('link', { name: /Hub Lac/ }).first().click();
  await settle(page);
  await pause();
  await page.getByRole('link', { name: 'Log in to book' }).click();
  await page.getByLabel('Email').pressSequentially('youssef@demo.coworkinghub.tn', { delay: 25 });
  await page.getByLabel('Password').pressSequentially(PASSWORD, { delay: 25 });
  await page.getByRole('button', { name: 'Log in' }).click();
  await page.getByRole('heading', { name: 'Book a room' }).waitFor();
  await settle(page);
  await pause();
  await page.getByLabel('Date').fill(dayFromNow(20));
  await pause(500);
  await page.getByLabel('Start time').fill('09:30');
  await page.getByLabel('Duration').selectOption('4');
  await pause(500);
  await page.getByLabel('People').fill('5');
  await pause(900);
  await page.locator('label.chip', { hasText: 'Whiteboard' }).click();
  await pause(500);
  await page.locator('label.chip', { hasText: 'Projector' }).click();
  await pause(1200);
  await page.getByRole('button', { name: 'Request booking' }).click();
  await page.getByText('Request sent.').waitFor();
  await pause(1800);
  await page.getByRole('link', { name: 'View my bookings' }).click();
  await page.getByRole('heading', { name: 'My bookings' }).waitFor();
  await pause(2200);

  // The owner reviews and accepts.
  await page.getByRole('button', { name: 'Log out' }).click();
  await pause(600);
  await page.getByRole('link', { name: 'Log in' }).first().click();
  await page.getByLabel('Email').pressSequentially('sana@demo.coworkinghub.tn', { delay: 25 });
  await page.getByLabel('Password').pressSequentially(PASSWORD, { delay: 25 });
  await page.getByRole('button', { name: 'Log in' }).click();
  await page.getByRole('link', { name: 'Manage' }).click();
  await page.getByText('Requests to review').first().waitFor();
  await pause(2000);
  // The request made above: match its exact date and time, as the UI formats them.
  const bookedOn = new Intl.DateTimeFormat('en-GB', { weekday: 'short', day: 'numeric', month: 'short', year: 'numeric', timeZone: 'UTC' })
    .format(new Date(`${dayFromNow(20)}T00:00:00Z`));
  const request = page.locator('.list-item').filter({ hasText: bookedOn }).filter({ hasText: '09:30' }).first();
  await request.scrollIntoViewIfNeeded();
  await pause(800);
  await request.getByRole('button', { name: 'Accept' }).click();
  await pause(2200);
  await page.getByRole('link', { name: /Hub Lac/ }).last().click();
  await page.getByRole('heading', { name: 'Rooms & prices' }).waitFor();
  await pause(2500);

  const video = page.video();
  await ctx.close();
  renameSync(await video.path(), path.join(OUT, 'walkthrough.webm'));
}

await browser.close();
console.log(`Saved screenshots and walkthrough.webm to ${OUT}`);
