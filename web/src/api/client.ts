import { z } from 'zod';
import { problem } from './schemas';
import { getToken, setToken } from './tokenStore';

/** An HTTP error from the API, carrying the server's problem-details message. */
export class ApiError extends Error {
  constructor(
    readonly status: number,
    message: string,
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

type Method = 'GET' | 'POST' | 'PUT' | 'DELETE';

interface RequestOptions {
  method?: Method;
  body?: unknown;
  signal?: AbortSignal;
}

const FALLBACK_MESSAGES: Record<number, string> = {
  401: 'Please log in to continue.',
  403: "You don't have permission to do that.",
  404: 'Not found.',
  409: 'That conflicts with existing data.',
};

/** Sends the request and returns the response text. Non-2xx becomes {@link ApiError}. */
async function request(path: string, options: RequestOptions): Promise<string> {
  const token = getToken();
  const headers: Record<string, string> = { Accept: 'application/json' };
  if (options.body !== undefined) headers['Content-Type'] = 'application/json';
  if (token) headers.Authorization = `Bearer ${token}`;

  let response: Response;
  try {
    response = await fetch(path, {
      method: options.method ?? 'GET',
      headers,
      body: options.body === undefined ? undefined : JSON.stringify(options.body),
      signal: options.signal,
    });
  } catch (error) {
    if (error instanceof DOMException && error.name === 'AbortError') throw error;
    throw new ApiError(0, 'Could not reach the server. Check your connection and try again.');
  }

  const text = await response.text();
  if (!response.ok) {
    // A rejected token means the session is over; the UI then shows the logged-out state.
    if (response.status === 401 && token) setToken(null);
    let message = FALLBACK_MESSAGES[response.status] ?? `Something went wrong (${response.status}).`;
    try {
      const parsed = problem.safeParse(JSON.parse(text));
      if (parsed.success && parsed.data.detail) message = parsed.data.detail;
    } catch {
      // not JSON: keep the fallback message
    }
    throw new ApiError(response.status, message);
  }
  return text;
}

/** Calls the API and validates the JSON response against {@code schema}. */
export async function apiJson<T>(path: string, schema: z.ZodType<T>, options: RequestOptions = {}): Promise<T> {
  const text = await request(path, options);
  let json: unknown;
  try {
    json = JSON.parse(text);
  } catch {
    throw new ApiError(500, 'The server sent an unexpected response.');
  }
  const parsed = schema.safeParse(json);
  if (!parsed.success) throw new ApiError(500, 'The server sent data in an unexpected format.');
  return parsed.data;
}

/** Calls the API when no response body is needed. */
export async function apiSend(path: string, options: RequestOptions = {}): Promise<void> {
  await request(path, options);
}

export function errorMessage(error: unknown): string {
  if (error instanceof ApiError) return error.message;
  if (error instanceof Error) return error.message;
  return 'Something went wrong.';
}
