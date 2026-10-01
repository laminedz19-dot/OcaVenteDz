import { Router, Response } from 'express';
import { z } from 'zod';
import prisma from '../db';
import {
  hashPassword,
  verifyPassword,
  generateAccessToken,
  generateRefreshToken,
  verifyRefreshToken
} from '../utils/security';
import { authenticate, AuthenticatedRequest } from '../middlewares/auth.middleware';

const router = Router();

const RegisterSchema = z.object({
  email: z.string().email(),
  username: z.string().min(3).max(30),
  password: z.string().min(6),
  phone: z.string().optional(),
  wilaya: z.string().default('16 - الجزائر (Alger)')
});

router.post('/register', async (req, res) => {
  try {
    const parse = RegisterSchema.safeParse(req.body);
    if (!parse.success) {
      return res.status(400).json({ error: 'Validation failed', details: parse.error.format() });
    }

    const { email, username, password, phone, wilaya } = parse.data;

    const existingUser = await prisma.user.findFirst({
      where: {
        OR: [{ email: email.toLowerCase() }, { username: username.toLowerCase() }]
      }
    });

    if (existingUser) {
      return res.status(409).json({ error: 'Email or username already in use' });
    }

    const passwordHash = await hashPassword(password);
    const user = await prisma.user.create({
      data: {
        email: email.toLowerCase(),
        username: username.toLowerCase(),
        passwordHash,
        phone,
        wilaya,
        role: 'USER',
        balance: 0
      },
      select: {
        id: true,
        email: true,
        username: true,
        phone: true,
        wilaya: true,
        role: true,
        balance: true,
        createdAt: true
      }
    });

    const accessToken = generateAccessToken({ userId: user.id, role: user.role, email: user.email });
    const refreshToken = generateRefreshToken({ userId: user.id, role: user.role, email: user.email });

    await prisma.refreshToken.create({
      data: {
        token: refreshToken,
        userId: user.id,
        expiresAt: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000)
      }
    });

    res.status(201).json({ user, accessToken, refreshToken });
  } catch (err: any) {
    res.status(500).json({ error: err.message || 'Internal server error' });
  }
});

const LoginSchema = z.object({
  login: z.string().min(1), // email or username
  password: z.string().min(1)
});

router.post('/login', async (req, res) => {
  try {
    const parse = LoginSchema.safeParse(req.body);
    if (!parse.success) {
      return res.status(400).json({ error: 'Missing credentials' });
    }

    const { login, password } = parse.data;
    const user = await prisma.user.findFirst({
      where: {
        OR: [{ email: login.toLowerCase() }, { username: login.toLowerCase() }]
      }
    });

    if (!user || !(await verifyPassword(password, user.passwordHash))) {
      return res.status(401).json({ error: 'Invalid email/username or password' });
    }

    if (user.isBlocked) {
      return res.status(403).json({ error: 'Account suspended. Contact administration.' });
    }

    const accessToken = generateAccessToken({ userId: user.id, role: user.role, email: user.email });
    const refreshToken = generateRefreshToken({ userId: user.id, role: user.role, email: user.email });

    await prisma.refreshToken.create({
      data: {
        token: refreshToken,
        userId: user.id,
        expiresAt: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000)
      }
    });

    res.json({
      user: {
        id: user.id,
        email: user.email,
        username: user.username,
        phone: user.phone,
        wilaya: user.wilaya,
        role: user.role,
        balance: user.balance
      },
      accessToken,
      refreshToken
    });
  } catch (err: any) {
    res.status(500).json({ error: err.message || 'Internal server error' });
  }
});

router.post('/refresh', async (req, res) => {
  try {
    const { refreshToken } = req.body;
    if (!refreshToken) {
      return res.status(400).json({ error: 'Refresh token required' });
    }

    const payload = verifyRefreshToken(refreshToken);
    if (!payload) {
      return res.status(401).json({ error: 'Invalid or expired refresh token' });
    }

    const savedToken = await prisma.refreshToken.findUnique({
      where: { token: refreshToken }
    });

    if (!savedToken || savedToken.revokedAt || savedToken.expiresAt < new Date()) {
      return res.status(401).json({ error: 'Revoked or non-existent token' });
    }

    const user = await prisma.user.findUnique({ where: { id: payload.userId } });
    if (!user || user.isBlocked) {
      return res.status(403).json({ error: 'User not found or suspended' });
    }

    const newAccessToken = generateAccessToken({ userId: user.id, role: user.role, email: user.email });
    res.json({ accessToken: newAccessToken });
  } catch (err: any) {
    res.status(500).json({ error: err.message || 'Server error' });
  }
});

router.post('/logout', authenticate, async (req: AuthenticatedRequest, res: Response) => {
  try {
    const { refreshToken } = req.body;
    if (refreshToken) {
      await prisma.refreshToken.updateMany({
        where: { token: refreshToken, userId: req.user!.userId },
        data: { revokedAt: new Date() }
      });
    }
    res.json({ message: 'Successfully logged out' });
  } catch (err: any) {
    res.status(500).json({ error: err.message || 'Error logging out' });
  }
});

router.get('/me', authenticate, async (req: AuthenticatedRequest, res: Response) => {
  try {
    const user = await prisma.user.findUnique({
      where: { id: req.user!.userId },
      select: {
        id: true,
        email: true,
        username: true,
        phone: true,
        wilaya: true,
        role: true,
        balance: true,
        avatarUrl: true,
        createdAt: true
      }
    });

    if (!user) {
      return res.status(404).json({ error: 'User not found' });
    }

    res.json({ user });
  } catch (err: any) {
    res.status(500).json({ error: err.message });
  }
});

export default router;
