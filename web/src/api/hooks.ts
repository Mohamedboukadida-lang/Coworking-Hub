import { useMutation, useQueries, useQuery, useQueryClient } from '@tanstack/react-query';
import { z } from 'zod';
import { apiJson, apiSend } from './client';
import {
  account,
  equipment,
  reservation,
  room,
  space,
  type Account,
  type ReservationStatus,
} from './schemas';

export const keys = {
  me: ['me'] as const,
  spaces: ['spaces'] as const,
  space: (id: number) => ['spaces', id] as const,
  rooms: (id: number) => ['spaces', id, 'rooms'] as const,
  equipment: (id: number) => ['spaces', id, 'equipment'] as const,
  myReservations: ['reservations', 'mine'] as const,
  spaceReservations: (id: number) => ['reservations', 'space', id] as const,
};

// ---- queries ----

export function useMe(enabled: boolean) {
  return useQuery({
    queryKey: keys.me,
    queryFn: ({ signal }) => apiJson('/api/users/me', account, { signal }),
    enabled,
    retry: false,
  });
}

export function useSpaces() {
  return useQuery({
    queryKey: keys.spaces,
    queryFn: ({ signal }) => apiJson('/api/coworking-spaces', z.array(space), { signal }),
  });
}

export function useSpace(id: number) {
  return useQuery({
    queryKey: keys.space(id),
    queryFn: ({ signal }) => apiJson(`/api/coworking-spaces/${id}`, space, { signal }),
  });
}

export function useRooms(spaceId: number) {
  return useQuery({
    queryKey: keys.rooms(spaceId),
    queryFn: ({ signal }) => apiJson(`/api/coworking-spaces/${spaceId}/rooms`, z.array(room), { signal }),
  });
}

/** Rooms of several spaces at once (for "from" prices on the explore page). */
export function useRoomsForSpaces(spaceIds: number[]) {
  return useQueries({
    queries: spaceIds.map((id) => ({
      queryKey: keys.rooms(id),
      queryFn: ({ signal }: { signal: AbortSignal }) =>
        apiJson(`/api/coworking-spaces/${id}/rooms`, z.array(room), { signal }),
    })),
  });
}

export function useEquipment(spaceId: number) {
  return useQuery({
    queryKey: keys.equipment(spaceId),
    queryFn: ({ signal }) => apiJson(`/api/coworking-spaces/${spaceId}/equipment`, z.array(equipment), { signal }),
  });
}

export function useMyReservations(enabled: boolean) {
  return useQuery({
    queryKey: keys.myReservations,
    queryFn: ({ signal }) => apiJson('/api/reservations/me', z.array(reservation), { signal }),
    enabled,
  });
}

export function useSpaceReservations(spaceIds: number[]) {
  return useQueries({
    queries: spaceIds.map((id) => ({
      queryKey: keys.spaceReservations(id),
      queryFn: ({ signal }: { signal: AbortSignal }) =>
        apiJson(`/api/reservations?coworkingSpaceId=${id}`, z.array(reservation), { signal }),
    })),
  });
}

// ---- mutations ----

export interface SpaceInput {
  name: string;
  address: string;
  coordinateX: number;
  coordinateY: number;
}

export function useCreateSpace() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: SpaceInput) => apiJson('/api/coworking-spaces/', space, { method: 'POST', body: input }),
    onSuccess: () => qc.invalidateQueries({ queryKey: keys.spaces }),
  });
}

export function useUpdateSpace(id: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: Partial<SpaceInput>) =>
      apiJson(`/api/coworking-spaces/${id}`, space, { method: 'PUT', body: input }),
    onSuccess: () => qc.invalidateQueries({ queryKey: keys.spaces }),
  });
}

export function useDeleteSpace() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => apiSend(`/api/coworking-spaces/${id}`, { method: 'DELETE' }),
    onSuccess: () => qc.invalidateQueries({ queryKey: keys.spaces }),
  });
}

export function useAddRoom(spaceId: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: { capacity: number; pricePerPersonPerHour: number }) =>
      apiJson(`/api/coworking-spaces/${spaceId}/rooms`, room, { method: 'POST', body: input }),
    onSuccess: () => qc.invalidateQueries({ queryKey: keys.rooms(spaceId) }),
  });
}

export function useStockEquipment(spaceId: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: () => apiSend(`/api/coworking-spaces/${spaceId}/add-random-equipment`, { method: 'POST' }),
    onSuccess: () => qc.invalidateQueries({ queryKey: keys.equipment(spaceId) }),
  });
}

export interface BookingInput {
  coworkingSpaceId: number;
  date: string;
  time: string;
  hours: number;
  numberOfPeople: number;
  equipmentIds: number[];
}

export function useBook() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: BookingInput) => apiJson('/api/reservations/', reservation, { method: 'POST', body: input }),
    onSuccess: (created) => {
      qc.invalidateQueries({ queryKey: keys.myReservations });
      if (created.coworkingSpaceId !== null) {
        qc.invalidateQueries({ queryKey: keys.spaceReservations(created.coworkingSpaceId) });
      }
    },
  });
}

export function useSetReservationStatus() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: { id: number; status: ReservationStatus; reason?: string }) =>
      apiJson(`/api/reservations/${input.id}/status`, reservation, {
        method: 'PUT',
        body: { status: input.status, reason: input.reason },
      }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['reservations'] }),
  });
}

export function useCancelReservation() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => apiSend(`/api/reservations/${id}`, { method: 'DELETE' }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['reservations'] }),
  });
}

export function useUpdateProfile(id: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: Partial<Pick<Account, 'name' | 'surname' | 'age' | 'profession'>>) =>
      apiJson(`/api/users/${id}`, account, { method: 'PUT', body: input }),
    onSuccess: (updated) => qc.setQueryData(keys.me, updated),
  });
}
