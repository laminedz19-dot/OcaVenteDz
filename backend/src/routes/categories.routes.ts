import { Router } from 'express';
import prisma from '../db';

const router = Router();

router.get('/', async (req, res) => {
  try {
    const categories = await prisma.category.findMany({
      orderBy: { order: 'asc' },
      include: {
        _count: {
          select: { listings: { where: { status: 'ACTIVE' } } }
        }
      }
    });

    res.json({ categories });
  } catch (err: any) {
    res.status(500).json({ error: err.message });
  }
});

export default router;
