import { Router, Response } from 'express';
import { z } from 'zod';
import prisma from '../db';
import { authenticate, requireRole, AuthenticatedRequest } from '../middlewares/auth.middleware';

const router = Router();

// Apply auth + ADMIN/SUPER_ADMIN role check to all admin routes
router.use(authenticate, requireRole('ADMIN', 'SUPER_ADMIN'));

// Admin Dashboard KPI Stats
router.get('/dashboard', async (req: AuthenticatedRequest, res: Response) => {
  try {
    const [
      totalUsers,
      totalListings,
      pendingListings,
      pendingRecharges,
      totalApprovedVolume
    ] = await Promise.all([
      prisma.user.count(),
      prisma.listing.count({ where: { status: 'ACTIVE' } }),
      prisma.listing.count({ where: { status: 'PENDING' } }),
      prisma.rechargeRequest.count({ where: { status: 'PENDING' } }),
      prisma.balanceTransaction.aggregate({
        where: { type: 'RECHARGE' },
        _sum: { amount: true }
      })
    ]);

    res.json({
      stats: {
        totalUsers,
        totalActiveListings: totalListings,
        pendingListingsCount: pendingListings,
        pendingRechargeCount: pendingRecharges,
        totalRechargedAmount: totalApprovedVolume._sum.amount || 0
      }
    });
  } catch (err: any) {
    res.status(500).json({ error: err.message });
  }
});

// Admin: Users List
router.get('/users', async (req: AuthenticatedRequest, res: Response) => {
  try {
    const { page = '1', limit = '20', q } = req.query;
    const pageNum = parseInt(page as string) || 1;
    const limitNum = parseInt(limit as string) || 20;

    const where: any = {};
    if (q && (q as string).trim() !== '') {
      where.OR = [
        { username: { contains: (q as string).trim(), mode: 'insensitive' } },
        { email: { contains: (q as string).trim(), mode: 'insensitive' } },
        { phone: { contains: (q as string).trim(), mode: 'insensitive' } }
      ];
    }

    const [total, users] = await Promise.all([
      prisma.user.count({ where }),
      prisma.user.findMany({
        where,
        skip: (pageNum - 1) * limitNum,
        take: limitNum,
        orderBy: { createdAt: 'desc' },
        select: {
          id: true,
          email: true,
          username: true,
          phone: true,
          wilaya: true,
          role: true,
          balance: true,
          isBlocked: true,
          createdAt: true,
          _count: { select: { listings: true } }
        }
      })
    ]);

    res.json({ users, total });
  } catch (err: any) {
    res.status(500).json({ error: err.message });
  }
});

// Admin: Toggle Block / Change Role
router.put('/users/:id/status', async (req: AuthenticatedRequest, res: Response) => {
  try {
    const { id } = req.params;
    const { isBlocked, role } = req.body;

    const updatedUser = await prisma.user.update({
      where: { id },
      data: {
        ...(typeof isBlocked === 'boolean' && { isBlocked }),
        ...(role && { role })
      }
    });

    // Record audit log
    await prisma.auditLog.create({
      data: {
        adminId: req.user!.userId,
        action: `USER_STATUS_CHANGE_${isBlocked ? 'BLOCKED' : 'UPDATED'}`,
        targetType: 'USER',
        targetId: id,
        details: JSON.stringify({ isBlocked, role }),
        ipAddress: req.ip
      }
    });

    res.json({ message: 'User updated successfully', user: updatedUser });
  } catch (err: any) {
    res.status(500).json({ error: err.message });
  }
});

// Admin: Listings List for moderation
router.get('/listings', async (req: AuthenticatedRequest, res: Response) => {
  try {
    const { status = 'PENDING', page = '1', limit = '20' } = req.query;
    const pageNum = parseInt(page as string) || 1;
    const limitNum = parseInt(limit as string) || 20;

    const where: any = {};
    if (status !== 'ALL') {
      where.status = status;
    }

    const [total, listings] = await Promise.all([
      prisma.listing.count({ where }),
      prisma.listing.findMany({
        where,
        skip: (pageNum - 1) * limitNum,
        take: limitNum,
        orderBy: { createdAt: 'desc' },
        include: {
          category: true,
          images: true,
          user: { select: { id: true, username: true, phone: true, email: true } }
        }
      })
    ]);

    res.json({ listings, total });
  } catch (err: any) {
    res.status(500).json({ error: err.message });
  }
});

// Admin: Approve or Reject Listing
router.put('/listings/:id/review', async (req: AuthenticatedRequest, res: Response) => {
  try {
    const { id } = req.params;
    const { action, rejectionReason } = req.body; // action: 'APPROVE' or 'REJECT'

    if (!['APPROVE', 'REJECT'].includes(action)) {
      return res.status(400).json({ error: 'Action must be APPROVE or REJECT' });
    }

    const listing = await prisma.listing.findUnique({ where: { id } });
    if (!listing) {
      return res.status(404).json({ error: 'Listing not found' });
    }

    const newStatus = action === 'APPROVE' ? 'ACTIVE' : 'REJECTED';
    const updated = await prisma.listing.update({
      where: { id },
      data: {
        status: newStatus,
        rejectionReason: action === 'REJECT' ? rejectionReason || 'Violates terms' : null
      }
    });

    // Notify user
    await prisma.notification.create({
      data: {
        userId: listing.userId,
        title: action === 'APPROVE' ? 'تمت الموافقة على إعلانك!' : 'تم رفض إعلانك',
        message: action === 'APPROVE'
          ? `إعلانك "${listing.title}" أصبح الآن نشطاً ويظهر للمشترين.`
          : `تم رفض إعلانك "${listing.title}". السبب: ${rejectionReason || 'عدم مطابقة الشروط'}`,
        type: 'LISTING_STATUS'
      }
    });

    // Audit log
    await prisma.auditLog.create({
      data: {
        adminId: req.user!.userId,
        action: `LISTING_${action}`,
        targetType: 'LISTING',
        targetId: id,
        details: JSON.stringify({ action, rejectionReason }),
        ipAddress: req.ip
      }
    });

    res.json({ message: `Listing ${action.toLowerCase()}d successfully`, listing: updated });
  } catch (err: any) {
    res.status(500).json({ error: err.message });
  }
});

// Admin: Recharge Requests List
router.get('/recharge-requests', async (req: AuthenticatedRequest, res: Response) => {
  try {
    const { status = 'PENDING' } = req.query;
    const where: any = {};
    if (status !== 'ALL') {
      where.status = status;
    }

    const requests = await prisma.rechargeRequest.findMany({
      where,
      orderBy: { createdAt: 'desc' },
      include: {
        user: { select: { id: true, username: true, email: true, phone: true, balance: true } }
      }
    });

    res.json({ requests });
  } catch (err: any) {
    res.status(500).json({ error: err.message });
  }
});

// Admin: Review Recharge Request (APPROVE or REJECT)
// Critical transactional requirement: Safe balance addition, prevent double crediting
router.put('/recharge-requests/:id/review', async (req: AuthenticatedRequest, res: Response) => {
  try {
    const { id } = req.params;
    const { action, adminNotes } = req.body; // action: 'APPROVE' or 'REJECT'

    if (!['APPROVE', 'REJECT'].includes(action)) {
      return res.status(400).json({ error: 'Action must be APPROVE or REJECT' });
    }

    const rechargeReq = await prisma.rechargeRequest.findUnique({ where: { id } });
    if (!rechargeReq) {
      return res.status(404).json({ error: 'Recharge request not found' });
    }

    if (rechargeReq.status !== 'PENDING') {
      return res.status(400).json({ error: `Request already processed: ${rechargeReq.status}` });
    }

    if (action === 'APPROVE') {
      // Execute atomic transaction for balance crediting
      const result = await prisma.$transaction(async (tx) => {
        // 1. Update recharge request status
        const updatedReq = await tx.rechargeRequest.update({
          where: { id },
          data: {
            status: 'APPROVED',
            adminNotes: adminNotes || 'تم التحقق من الوصل بنجاح',
            reviewedBy: req.user!.userId,
            reviewedAt: new Date()
          }
        });

        // 2. Increment user balance
        const updatedUser = await tx.user.update({
          where: { id: rechargeReq.userId },
          data: {
            balance: { increment: rechargeReq.amount }
          }
        });

        // 3. Create balance transaction record
        await tx.balanceTransaction.create({
          data: {
            userId: rechargeReq.userId,
            amount: rechargeReq.amount,
            type: 'RECHARGE',
            description: `شحن رصيد بواسطة ${rechargeReq.paymentMethod} (وصل #${rechargeReq.receiptNumber})`,
            referenceId: rechargeReq.id,
            balanceAfter: updatedUser.balance
          }
        });

        // 4. Send notification to user
        await tx.notification.create({
          data: {
            userId: rechargeReq.userId,
            title: 'تم شحن رصيدك بنجاح!',
            message: `تمت الموافقة على طلب الشحن بقيمة ${rechargeReq.amount} د.ج وإضافتها لرصيدك.`,
            type: 'RECHARGE_STATUS'
          }
        });

        // 5. Create audit log
        await tx.auditLog.create({
          data: {
            adminId: req.user!.userId,
            action: 'RECHARGE_APPROVED',
            targetType: 'RECHARGE_REQUEST',
            targetId: id,
            details: JSON.stringify({ amount: rechargeReq.amount, userId: rechargeReq.userId, adminNotes }),
            ipAddress: req.ip
          }
        });

        return { updatedReq, newBalance: updatedUser.balance };
      });

      return res.json({
        message: 'Recharge request approved and balance credited successfully',
        ...result
      });
    } else {
      // REJECT branch
      const updatedReq = await prisma.rechargeRequest.update({
        where: { id },
        data: {
          status: 'REJECTED',
          adminNotes: adminNotes || 'رقم الوصل غير صحيح أو لم يصل التحويل',
          reviewedBy: req.user!.userId,
          reviewedAt: new Date()
        }
      });

      await prisma.notification.create({
        data: {
          userId: rechargeReq.userId,
          title: 'تم رفض طلب شحن الرصيد',
          message: `عذراً، تم رفض طلب شحن الرصيد (${rechargeReq.amount} د.ج). السبب: ${adminNotes || 'عدم تطابق الوصل'}`,
          type: 'RECHARGE_STATUS'
        }
      });

      await prisma.auditLog.create({
        data: {
          adminId: req.user!.userId,
          action: 'RECHARGE_REJECTED',
          targetType: 'RECHARGE_REQUEST',
          targetId: id,
          details: JSON.stringify({ amount: rechargeReq.amount, userId: rechargeReq.userId, adminNotes }),
          ipAddress: req.ip
        }
      });

      return res.json({ message: 'Recharge request rejected', request: updatedReq });
    }
  } catch (err: any) {
    res.status(500).json({ error: err.message });
  }
});

// Admin: Financial Transactions Log
router.get('/transactions', async (req: AuthenticatedRequest, res: Response) => {
  try {
    const transactions = await prisma.balanceTransaction.findMany({
      orderBy: { createdAt: 'desc' },
      take: 100,
      include: {
        user: { select: { id: true, username: true, email: true } }
      }
    });
    res.json({ transactions });
  } catch (err: any) {
    res.status(500).json({ error: err.message });
  }
});

// Admin: Audit Logs
router.get('/audit-logs', async (req: AuthenticatedRequest, res: Response) => {
  try {
    const logs = await prisma.auditLog.findMany({
      orderBy: { createdAt: 'desc' },
      take: 100,
      include: {
        admin: { select: { id: true, username: true, email: true } }
      }
    });
    res.json({ logs });
  } catch (err: any) {
    res.status(500).json({ error: err.message });
  }
});

// Admin: Manage Categories (Create, Update, Delete)
router.post('/categories', async (req: AuthenticatedRequest, res: Response) => {
  try {
    const { nameAr, nameFr, slug, icon, order } = req.body;
    const category = await prisma.category.create({
      data: { nameAr, nameFr, slug, icon, order: order || 0 }
    });
    res.status(201).json({ category });
  } catch (err: any) {
    res.status(500).json({ error: err.message });
  }
});

router.put('/categories/:id', async (req: AuthenticatedRequest, res: Response) => {
  try {
    const { id } = req.params;
    const { nameAr, nameFr, slug, icon, order } = req.body;
    const category = await prisma.category.update({
      where: { id },
      data: { nameAr, nameFr, slug, icon, order }
    });
    res.json({ category });
  } catch (err: any) {
    res.status(500).json({ error: err.message });
  }
});

router.delete('/categories/:id', async (req: AuthenticatedRequest, res: Response) => {
  try {
    const { id } = req.params;
    await prisma.category.delete({ where: { id } });
    res.json({ message: 'Category deleted' });
  } catch (err: any) {
    res.status(500).json({ error: err.message });
  }
});

export default router;
