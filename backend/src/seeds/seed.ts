import prisma from '../db';
import { hashPassword } from '../utils/security';

async function main() {
  console.log('🌱 Seeding ocaventeDz Database...');

  // 1. Seed Categories
  const categories = [
    { nameAr: 'مركبات وسيارات', nameFr: 'Véhicules & Voitures', slug: 'vehicles', icon: 'directions_car', order: 1 },
    { nameAr: 'عقارات وأراضي', nameFr: 'Immobilier', slug: 'real-estate', icon: 'apartment', order: 2 },
    { nameAr: 'هواتف وتكنولوجيا', nameFr: 'Téléphones & High-Tech', slug: 'phones-tech', icon: 'smartphone', order: 3 },
    { nameAr: 'أجهزة كهرومنزلية', nameFr: 'Electroménager', slug: 'home-appliances', icon: 'kitchen', order: 4 },
    { nameAr: 'أزياء وملابس', nameFr: 'Mode & Beauté', slug: 'fashion', icon: 'checkroom', order: 5 },
    { nameAr: 'أثاث وديكور', nameFr: 'Maison & Jardin', slug: 'home-decor', icon: 'chair', order: 6 },
    { nameAr: 'وظائف وخدمات', nameFr: 'Emploi & Services', slug: 'jobs-services', icon: 'work', order: 7 }
  ];

  for (const cat of categories) {
    await prisma.category.upsert({
      where: { slug: cat.slug },
      update: cat,
      create: cat
    });
  }

  // 2. Seed Super Admin
  const adminPasswordHash = await hashPassword('Admin@123456');
  const admin = await prisma.user.upsert({
    where: { email: 'admin@ocavente.dz' },
    update: {},
    create: {
      email: 'admin@ocavente.dz',
      username: 'superadmin',
      passwordHash: adminPasswordHash,
      phone: '0550123456',
      wilaya: '16 - الجزائر (Alger)',
      role: 'SUPER_ADMIN',
      balance: 50000.00
    }
  });

  // 3. Seed Normal Demo User
  const userPasswordHash = await hashPassword('User@123456');
  const demoUser = await prisma.user.upsert({
    where: { email: 'demo@ocavente.dz' },
    update: {},
    create: {
      email: 'demo@ocavente.dz',
      username: 'karim_dz',
      passwordHash: userPasswordHash,
      phone: '0661987654',
      wilaya: '31 - وهران (Oran)',
      role: 'USER',
      balance: 2500.00
    }
  });

  console.log('✅ Seeding completed:');
  console.log(`   - Super Admin: admin@ocavente.dz (password: Admin@123456)`);
  console.log(`   - Demo User: demo@ocavente.dz (password: User@123456)`);
}

main()
  .catch((e) => {
    console.error(e);
    process.exit(1);
  })
  .finally(async () => {
    await prisma.$disconnect();
  });
