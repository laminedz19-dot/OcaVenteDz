import { Router, Response } from 'express';
import { z } from 'zod';
import prisma from '../db';
import { authenticate, AuthenticatedRequest } from '../middlewares/auth.middleware';

const router = Router();

// Public: Get listings with pagination and filters
router.get('/', async (req, res) => {
  try {
    const { category, wilaya, q, minPrice, maxPrice, page = '1', limit = '20' } = req.query;

    const pageNum = parseInt(page as string) || 1;
    const limitNum = parseInt(limit as string) || 20;
    const skip = (pageNum - 1) * limitNum;

    const where: any = {
      status: 'ACTIVE'
    };

    if (category) {
      where.OR = [
        { categoryId: category as string },
        { category: { slug: category as string } }
      ];
    }

    if (wilaya && (wilaya as string).trim() !== '') {
      where.wilaya = { contains: (wilaya as string).trim(), mode: 'insensitive' };
    }

    if (q && (q as string).trim() !== '') {
      where.OR = [
        { title: { contains: (q as string).trim(), mode: 'insensitive' } },
        { description: { contains: (q as string).trim(), mode: 'insensitive' } }
      ];
    }

    if (minPrice || maxPrice) {
      where.price = {};
      if (minPrice) where.price.gte = parseFloat(minPrice as string);
      if (maxPrice) where.price.lte = parseFloat(maxPrice as string);
    }

    const [total, listings] = await Promise.all([
      prisma.listing.count({ where }),
      prisma.listing.findMany({
        where,
        skip,
        take: limitNum,
        orderBy: [{ isFeatured: 'desc' }, { createdAt: 'desc' }],
        include: {
          category: true,
          images: { orderBy: { order: 'asc' } },
          user: {
            select: { id: true, username: true, phone: true, wilaya: true }
          }
        }
      })
    ]);

    res.json({
      listings,
      meta: {
        page: pageNum,
        limit: limitNum,
        total,
        totalPages: Math.ceil(total / limitNum)
      }
    });
  } catch (err: any) {
    res.status(500).json({ error: err.message });
  }
});

// Public: Get single listing details and increment views
router.get('/:id', async (req, res) => {
  try {
    const { id } = req.params;

    const listing = await prisma.listing.findUnique({
      where: { id },
      include: {
        category: true,
        images: { orderBy: { order: 'asc' } },
        user: {
          select: { id: true, username: true, phone: true, wilaya: true, createdAt: true }
        }
      }
    });

    if (!listing) {
      return res.status(404).json({ error: 'Listing not found' });
    }

    // Increment views async
    prisma.listing.update({
      where: { id },
      data: { views: { increment: 1 } }
    }).catch(() => {});

    res.json({ listing });
  } catch (err: any) {
    res.status(500).json({ error: err.message });
  }
});

const CreateListingSchema = z.object({
  title: z.string().min(3).max(100),
  description: z.string().min(10),
  price: z.number().nonnegative(),
  categoryId: z.string().uuid(),
  wilaya: z.string().min(1),
  commune: z.string().optional(),
  phone: z.string().min(9),
  isNegotiable: z.boolean().default(true),
  isFeatured: z.boolean().default(false),
  images: z.array(z.string().url()).optional()
});

// Authenticated: Create listing (goes to PENDING for moderation)
router.post('/', authenticate, async (req: AuthenticatedRequest, res: Response) => {
  try {
    const parse = CreateListingSchema.safeParse(req.body);
    if (!parse.success) {
      return res.status(400).json({ error: 'Validation failed', details: parse.error.format() });
    }

    const { title, description, price, categoryId, wilaya, commune, phone, isNegotiable, isFeatured, images } = parse.data;
    const userId = req.user!.userId;

    const listing = await prisma.listing.create({
      data: {
        title,
        description,
        price,
        categoryId,
        userId,
        wilaya,
        commune,
        phone,
        isNegotiable,
        isFeatured,
        status: 'PENDING', // All listings submitted by users must be approved by admin
        images: images && images.length > 0 ? {
          create: images.map((url, idx) => ({
            url,
            isPrimary: idx === 0,
            order: idx
          }))
        } : undefined
      },
      include: {
        category: true,
        images: true
      }
    });

    res.status(201).json({
      message: 'Listing created successfully and pending admin moderation',
      listing
    });
  } catch (err: any) {
    res.status(500).json({ error: err.message });
  }
});

// Authenticated: User's own listings
router.get('/my/all', authenticate, async (req: AuthenticatedRequest, res: Response) => {
  try {
    const listings = await prisma.listing.findMany({
      where: { userId: req.user!.userId },
      orderBy: { createdAt: 'desc' },
      include: {
        category: true,
        images: true
      }
    });

    res.json({ listings });
  } catch (err: any) {
    res.status(500).json({ error: err.message });
  }
});

// Authenticated: Delete own listing
router.delete('/:id', authenticate, async (req: AuthenticatedRequest, res: Response) => {
  try {
    const { id } = req.params;
    const listing = await prisma.listing.findUnique({ where: { id } });

    if (!listing) {
      return res.status(404).json({ error: 'Listing not found' });
    }

    // IDOR protection
    if (listing.userId !== req.user!.userId && req.user!.role === 'USER') {
      return res.status(403).json({ error: 'Forbidden. You do not own this listing.' });
    }

    await prisma.listing.delete({ where: { id } });
    res.json({ message: 'Listing deleted successfully' });
  } catch (err: any) {
    res.status(500).json({ error: err.message });
  }
});

// Authenticated: Toggle favorite
router.post('/:id/favorite', authenticate, async (req: AuthenticatedRequest, res: Response) => {
  try {
    const listingId = req.params.id;
    const userId = req.user!.userId;

    const existing = await prisma.favorite.findUnique({
      where: { userId_listingId: { userId, listingId } }
    });

    if (existing) {
      await prisma.favorite.delete({
        where: { userId_listingId: { userId, listingId } }
      });
      return res.json({ favorited: false });
    } else {
      await prisma.favorite.create({
        data: { userId, listingId }
      });
      return res.json({ favorited: true });
    }
  } catch (err: any) {
    res.status(500).json({ error: err.message });
  }
});

// Authenticated: Get user's favorites
router.get('/my/favorites', authenticate, async (req: AuthenticatedRequest, res: Response) => {
  try {
    const favorites = await prisma.favorite.findMany({
      where: { userId: req.user!.userId },
      include: {
        listing: {
          include: {
            category: true,
            images: true,
            user: { select: { username: true, phone: true } }
          }
        }
      }
    });

    res.json({ favorites: favorites.map(f => f.listing) });
  } catch (err: any) {
    res.status(500).json({ error: err.message });
  }
});

export default router;
