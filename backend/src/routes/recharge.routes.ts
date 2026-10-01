import { Router, Request, Response } from 'express';
import { z } from 'zod';
import prisma from '../db';
import { authenticate, AuthenticatedRequest } from '../middlewares/auth.middleware';

const router = Router();

// Public: Get official payment transfer instructions (BaridiMob RIP and CCP)
router.get('/instructions', (_req: Request, res: Response) => {
  res.json({
    ccp: {
      accountNumber: "0008761821",
      key: "94",
      display: "0008761821 مفتاح 94"
    },
    baridiMob: {
      rip: "00799999000876182194",
      display: "00799999000876182194"
    },
    notes: "يرجى إدخال رقم الوصل بدقة بعد إتمام التحويل لتأكيد إضافة الرصيد لحسابك فوراً."
  });
});

const RechargeRequestSchema = z.object({
  amount: z.number().positive(),
  paymentMethod: z.enum(['CCP', 'BARIDIMOB', 'CASH', 'VOUCHER']),
  receiptNumber: z.string().min(3),
  receiptImageUrl: z.string().optional()
});

// User: Submit recharge request
router.post('/request', authenticate, async (req: AuthenticatedRequest, res: Response) => {
  try {
    const parse = RechargeRequestSchema.safeParse(req.body);
    if (!parse.success) {
      return res.status(400).json({ error: 'Invalid data', details: parse.error.format() });
    }

    const { amount, paymentMethod, receiptNumber, receiptImageUrl } = parse.data;
    const userId = req.user!.userId;

    const request = await prisma.rechargeRequest.create({
      data: {
        userId,
        amount,
        paymentMethod,
        receiptNumber,
        receiptImageUrl,
        status: 'PENDING'
      }
    });

    res.status(201).json({
      message: 'Recharge request submitted successfully. Pending admin verification.',
      request
    });
  } catch (err: any) {
    res.status(500).json({ error: err.message });
  }
});

// User: Get recharge requests history
router.get('/my-requests', authenticate, async (req: AuthenticatedRequest, res: Response) => {
  try {
    const requests = await prisma.rechargeRequest.findMany({
      where: { userId: req.user!.userId },
      orderBy: { createdAt: 'desc' }
    });
    res.json({ requests });
  } catch (err: any) {
    res.status(500).json({ error: err.message });
  }
});

// User: Get wallet balance and transaction history
router.get('/wallet', authenticate, async (req: AuthenticatedRequest, res: Response) => {
  try {
    const userId = req.user!.userId;
    const [user, transactions] = await Promise.all([
      prisma.user.findUnique({
        where: { id: userId },
        select: { id: true, balance: true }
      }),
      prisma.balanceTransaction.findMany({
        where: { userId },
        orderBy: { createdAt: 'desc' },
        take: 50
      })
    ]);

    res.json({
      balance: user?.balance ?? 0,
      transactions
    });
  } catch (err: any) {
    res.status(500).json({ error: err.message });
  }
});

// User: Get notifications
router.get('/notifications', authenticate, async (req: AuthenticatedRequest, res: Response) => {
  try {
    const notifications = await prisma.notification.findMany({
      where: { userId: req.user!.userId },
      orderBy: { createdAt: 'desc' },
      take: 50
    });
    res.json({ notifications });
  } catch (err: any) {
    res.status(500).json({ error: err.message });
  }
});

// User: Mark notification as read
router.put('/notifications/:id/read', authenticate, async (req: AuthenticatedRequest, res: Response) => {
  try {
    const { id } = req.params;
    await prisma.notification.updateMany({
      where: { id, userId: req.user!.userId },
      data: { isRead: true }
    });
    res.json({ success: true });
  } catch (err: any) {
    res.status(500).json({ error: err.message });
  }
});

export default router;
