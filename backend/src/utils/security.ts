import jwt from 'jsonwebtoken';
import bcrypt from 'bcryptjs';

const JWT_ACCESS_SECRET = process.env.JWT_ACCESS_SECRET || 'ocavente_super_secret_jwt_access_key_dz_2026';
const JWT_REFRESH_SECRET = process.env.JWT_REFRESH_SECRET || 'ocavente_super_secret_jwt_refresh_key_dz_2026';

export interface TokenPayload {
  userId: string;
  role: 'USER' | 'ADMIN' | 'SUPER_ADMIN';
  email: string;
}

export async function hashPassword(password: string): Promise<string> {
  const salt = await bcrypt.genSalt(12);
  return bcrypt.hash(password, salt);
}

export async function verifyPassword(password: string, hash: string): Promise<boolean> {
  return bcrypt.compare(password, hash);
}

export function generateAccessToken(payload: TokenPayload): string {
  return jwt.sign(payload, JWT_ACCESS_SECRET, { expiresIn: '2h' });
}

export function generateRefreshToken(payload: TokenPayload): string {
  return jwt.sign(payload, JWT_REFRESH_SECRET, { expiresIn: '30d' });
}

export function verifyAccessToken(token: string): TokenPayload | null {
  try {
    return jwt.verify(token, JWT_ACCESS_SECRET) as TokenPayload;
  } catch {
    return null;
  }
}

export function verifyRefreshToken(token: string): TokenPayload | null {
  try {
    return jwt.verify(token, JWT_REFRESH_SECRET) as TokenPayload;
  } catch {
    return null;
  }
}
