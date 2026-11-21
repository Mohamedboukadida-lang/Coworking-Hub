// Fills a running backend with demo data through the public API (so every access rule applies).
// Usage: API_URL=http://localhost:8081 node scripts/seed.mjs
// Safe to re-run: accounts are reused, and owners who already have spaces are skipped.

const API = process.env.API_URL ?? 'http://localhost:8081';
const PASSWORD = 'demo-pass-123';

async function call(method, path, { body, token } = {}) {
  const headers = { 'Content-Type': 'application/json' };
  if (token) headers.Authorization = `Bearer ${token}`;
  const res = await fetch(API + path, { method, headers, body: body ? JSON.stringify(body) : undefined });
  const text = await res.text();
  let json = null;
  try {
    json = text ? JSON.parse(text) : null;
  } catch {
    json = text; // some endpoints answer with plain text
  }
  if (!res.ok) {
    const err = new Error(`${method} ${path} -> ${res.status}: ${json?.detail ?? text}`);
    err.status = res.status;
    throw err;
  }
  return json;
}

async function account(name, surname, email, typeCompte, profession) {
  try {
    return await call('POST', '/api/auth/register', { body: { name, surname, email, password: PASSWORD, typeCompte, profession } });
  } catch (e) {
    if (e.status !== 409) throw e;
    return call('POST', '/api/auth/login', { body: { email, password: PASSWORD } });
  }
}

function dayFromNow(n) {
  const d = new Date();
  d.setDate(d.getDate() + n);
  return d.toISOString().slice(0, 10);
}

const owners = [
  {
    who: ['Sana', 'Trabelsi', 'sana@demo.coworkinghub.tn', 'OWNER', 'Community manager'],
    spaces: [
      { name: 'Hub Lac', address: 'Rue du Lac Biwa, Les Berges du Lac', coordinateX: 36.8325, coordinateY: 10.2331,
        rooms: [[1, 6], [4, 9], [8, 11]], equipment: true },
      { name: 'Medina Loft', address: 'Rue Jamaa Zitouna, Medina of Tunis', coordinateX: 36.7992, coordinateY: 10.1706,
        rooms: [[2, 5.5], [6, 8]], equipment: true },
      { name: 'Ennasr Workhouse', address: 'Avenue Hédi Nouira, Ennasr, Ariana', coordinateX: 36.8625, coordinateY: 10.1647,
        rooms: [[1, 4.5], [10, 7]], equipment: false },
    ],
  },
  {
    who: ['Karim', 'Ben Salah', 'karim@demo.coworkinghub.tn', 'OWNER', 'Architect'],
    spaces: [
      { name: 'La Marsa Studio', address: 'Avenue Habib Bourguiba, La Marsa', coordinateX: 36.8782, coordinateY: 10.3247,
        rooms: [[2, 7], [5, 10]], equipment: true },
      { name: 'Carthage Commons', address: 'Rue Hannibal, Carthage', coordinateX: 36.8528, coordinateY: 10.3233,
        rooms: [[1, 5], [12, 9.5]], equipment: true },
      { name: 'Sidi Bou Said Atelier', address: 'Rue Habib Thameur, Sidi Bou Said', coordinateX: 36.871, coordinateY: 10.347,
        rooms: [[3, 12]], equipment: false },
    ],
  },
];

const created = {};
for (const owner of owners) {
  const { token, user } = await account(...owner.who);
  const existing = (await call('GET', '/api/coworking-spaces')).filter((s) => s.ownerId === user.id);
  if (existing.length) {
    console.log(`${user.name}: already has ${existing.length} spaces, skipping`);
    for (const s of existing) created[s.name] = { ...s, ownerToken: token };
    continue;
  }
  for (const s of owner.spaces) {
    const space = await call('POST', '/api/coworking-spaces/', { token, body: { name: s.name, address: s.address, coordinateX: s.coordinateX, coordinateY: s.coordinateY } });
    for (const [capacity, price] of s.rooms) {
      await call('POST', `/api/coworking-spaces/${space.id}/rooms`, { token, body: { capacity, pricePerPersonPerHour: price } });
    }
    if (s.equipment) await call('POST', `/api/coworking-spaces/${space.id}/add-random-equipment`, { token });
    created[s.name] = { ...space, ownerToken: token };
    console.log(`${user.name}: created ${s.name}`);
  }
}

// A member with a few bookings in different states.
const member = await account('Youssef', 'Haddad', 'youssef@demo.coworkinghub.tn', 'USER', 'Mobile developer');
const mine = await call('GET', '/api/reservations/me', { token: member.token });
if (mine.length === 0) {
  const book = (space, date, time, hours, people) =>
    call('POST', '/api/reservations/', { token: member.token, body: { coworkingSpaceId: created[space].id, date, time, hours, numberOfPeople: people } });
  const accepted = await book('Hub Lac', dayFromNow(3), '09:00', 4, 3);
  await call('PUT', `/api/reservations/${accepted.id}/status`, { token: created['Hub Lac'].ownerToken, body: { status: 'ACCEPTED', reason: 'Welcome! Ask for Sana at the front desk.' } });
  await book('La Marsa Studio', dayFromNow(6), '14:00', 2, 2);
  const declined = await book('Medina Loft', dayFromNow(9), '10:00', 3, 5);
  await call('PUT', `/api/reservations/${declined.id}/status`, { token: created['Medina Loft'].ownerToken, body: { status: 'DECLINED', reason: 'Closed for a private event that day, sorry.' } });
  await book('Carthage Commons', dayFromNow(12), '08:30', 6, 1);
  console.log('Youssef: 4 bookings');
}

// Another member, so owners have more than one person's requests to review.
const leila = await account('Leila', 'Mansour', 'leila@demo.coworkinghub.tn', 'USER', 'UX researcher');
if ((await call('GET', '/api/reservations/me', { token: leila.token })).length === 0) {
  await call('POST', '/api/reservations/', { token: leila.token, body: { coworkingSpaceId: created['Hub Lac'].id, date: dayFromNow(5), time: '13:00', hours: 3, numberOfPeople: 6 } });
  await call('POST', '/api/reservations/', { token: leila.token, body: { coworkingSpaceId: created['Medina Loft'].id, date: dayFromNow(4), time: '10:00', hours: 2, numberOfPeople: 2 } });
  console.log('Leila: 2 bookings');
}

console.log(`\nDemo accounts (password "${PASSWORD}"):`);
console.log('  owner   sana@demo.coworkinghub.tn, karim@demo.coworkinghub.tn');
console.log('  member  youssef@demo.coworkinghub.tn, leila@demo.coworkinghub.tn');
