import { z } from 'zod';
import { ApiError, apiJson, apiSend } from './client';
import { getToken, setToken } from './tokenStore';

function respond(status: number, body: unknown) {
  const text = typeof body === 'string' ? body : JSON.stringify(body);
  vi.stubGlobal('fetch', vi.fn(async () => new Response(text, { status })));
}

afterEach(() => {
  vi.unstubAllGlobals();
  setToken(null);
});

describe('api client', () => {
  it('validates successful responses against the schema', async () => {
    respond(200, { id: 1, name: 'Hub' });
    await expect(apiJson('/x', z.object({ id: z.number(), name: z.string() }))).resolves.toEqual({ id: 1, name: 'Hub' });
  });

  it('rejects responses that do not match the schema', async () => {
    respond(200, { id: 'not-a-number' });
    await expect(apiJson('/x', z.object({ id: z.number() }))).rejects.toThrow('unexpected format');
  });

  it("surfaces the server's problem detail with the status", async () => {
    respond(409, { status: 409, title: 'Conflict', detail: 'Every room is already booked' });
    const error = await apiSend('/x', { method: 'POST', body: {} }).catch((e: unknown) => e);
    expect(error).toBeInstanceOf(ApiError);
    expect(error).toMatchObject({ status: 409, message: 'Every room is already booked' });
  });

  it('falls back to a readable message when the body is not JSON', async () => {
    respond(403, 'Forbidden');
    await expect(apiSend('/x')).rejects.toThrow("You don't have permission to do that.");
  });

  it('sends the token and ends the session when it is rejected', async () => {
    setToken('abc');
    const fetchMock = vi.fn(async (_url: string, _init?: RequestInit) => new Response('{}', { status: 401 }));
    vi.stubGlobal('fetch', fetchMock);
    await expect(apiSend('/x')).rejects.toBeInstanceOf(ApiError);
    const init = fetchMock.mock.calls[0]?.[1];
    expect(new Headers(init?.headers).get('Authorization')).toBe('Bearer abc');
    expect(getToken()).toBeNull();
  });

  it('reports network failures as status 0', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => { throw new TypeError('Failed to fetch'); }));
    await expect(apiSend('/x')).rejects.toMatchObject({ status: 0 });
  });
});
