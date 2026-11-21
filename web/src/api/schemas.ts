import { z } from 'zod';

// Runtime schemas for every backend response. Types are derived from them, never written by hand.

export const accountType = z.enum(['ADMIN', 'USER', 'OWNER']);
export type AccountType = z.infer<typeof accountType>;

export const account = z.object({
  id: z.number(),
  name: z.string().nullable(),
  surname: z.string().nullable(),
  age: z.number().nullable(),
  profession: z.string().nullable(),
  typeCompte: accountType.nullable(),
  email: z.string().nullable().optional(),
});
export type Account = z.infer<typeof account>;

export const authResponse = z.object({ token: z.string(), user: account });
export type AuthResponse = z.infer<typeof authResponse>;

export const space = z.object({
  id: z.number(),
  name: z.string().nullable(),
  address: z.string().nullable(),
  coordinateX: z.number(),
  coordinateY: z.number(),
  ownerId: z.number().nullable(),
});
export type Space = z.infer<typeof space>;

export const room = z.object({
  id: z.number(),
  capacity: z.number(),
  pricePerPersonPerHour: z.number(),
});
export type Room = z.infer<typeof room>;

export const equipmentType = z.enum(['COMPUTER', 'PROJECTOR', 'WHITEBOARD', 'CHAIR', 'TABLE', 'COFFEE_MACHINE']);
export type EquipmentType = z.infer<typeof equipmentType>;

export const equipment = z.object({
  id: z.number(),
  name: z.string().nullable(),
  price: z.number(),
  typeOfEquipment: equipmentType.nullable(),
});
export type Equipment = z.infer<typeof equipment>;

export const reservationStatus = z.enum(['PENDING', 'DECLINED', 'ACCEPTED']);
export type ReservationStatus = z.infer<typeof reservationStatus>;

export const reservation = z.object({
  id: z.number(),
  date: z.string().nullable(),
  time: z.string().nullable(),
  hours: z.number().nullable(),
  numberOfPeople: z.number().nullable(),
  statusChangeReason: z.string().nullable(),
  totalPrice: z.number(),
  status: reservationStatus.nullable(),
  coworkingSpaceId: z.number().nullable(),
  userId: z.number().nullable(),
  roomId: z.number().nullable(),
});
export type Reservation = z.infer<typeof reservation>;

export const problem = z.object({
  status: z.number().optional(),
  title: z.string().optional(),
  detail: z.string().optional(),
});
