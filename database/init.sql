-- ocaventeDz Initial PostgreSQL Schema & Seed

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Enums
CREATE TYPE "Role" AS ENUM ('USER', 'ADMIN', 'SUPER_ADMIN');
CREATE TYPE "ListingStatus" AS ENUM ('PENDING', 'ACTIVE', 'REJECTED', 'SOLD');
CREATE TYPE "PaymentMethod" AS ENUM ('CCP', 'BARIDIMOB', 'CASH', 'VOUCHER');
CREATE TYPE "RechargeStatus" AS ENUM ('PENDING', 'APPROVED', 'REJECTED');
CREATE TYPE "TransactionType" AS ENUM ('RECHARGE', 'FEATURED_AD_FEE', 'LISTING_FEE', 'REFUND', 'ADMIN_ADJUSTMENT');
CREATE TYPE "NotificationType" AS ENUM ('LISTING_STATUS', 'RECHARGE_STATUS', 'SYSTEM');

-- Users Table
CREATE TABLE IF NOT EXISTS "User" (
    "id" UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    "email" VARCHAR(255) UNIQUE NOT NULL,
    "username" VARCHAR(100) UNIQUE NOT NULL,
    "passwordHash" VARCHAR(255) NOT NULL,
    "phone" VARCHAR(50),
    "wilaya" VARCHAR(100) DEFAULT '16 - الجزائر (Alger)',
    "role" "Role" DEFAULT 'USER',
    "balance" NUMERIC(12, 2) DEFAULT 0.00,
    "avatarUrl" TEXT,
    "isBlocked" BOOLEAN DEFAULT FALSE,
    "createdAt" TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Categories Table
CREATE TABLE IF NOT EXISTS "Category" (
    "id" UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    "nameAr" VARCHAR(150) NOT NULL,
    "nameFr" VARCHAR(150) NOT NULL,
    "slug" VARCHAR(100) UNIQUE NOT NULL,
    "icon" VARCHAR(100) NOT NULL,
    "order" INT DEFAULT 0,
    "createdAt" TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Listings Table
CREATE TABLE IF NOT EXISTS "Listing" (
    "id" UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    "title" VARCHAR(255) NOT NULL,
    "description" TEXT NOT NULL,
    "price" NUMERIC(12, 2) NOT NULL,
    "categoryId" UUID REFERENCES "Category"("id") ON DELETE RESTRICT,
    "userId" UUID REFERENCES "User"("id") ON DELETE CASCADE,
    "wilaya" VARCHAR(100) NOT NULL,
    "commune" VARCHAR(100),
    "phone" VARCHAR(50) NOT NULL,
    "isNegotiable" BOOLEAN DEFAULT TRUE,
    "status" "ListingStatus" DEFAULT 'PENDING',
    "rejectionReason" TEXT,
    "views" INT DEFAULT 0,
    "isFeatured" BOOLEAN DEFAULT FALSE,
    "featuredUntil" TIMESTAMP WITH TIME ZONE,
    "createdAt" TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS "idx_listing_status" ON "Listing"("status");
CREATE INDEX IF NOT EXISTS "idx_listing_wilaya" ON "Listing"("wilaya");
CREATE INDEX IF NOT EXISTS "idx_listing_category" ON "Listing"("categoryId");
CREATE INDEX IF NOT EXISTS "idx_listing_user" ON "Listing"("userId");

-- Seed Default Categories
INSERT INTO "Category" ("nameAr", "nameFr", "slug", "icon", "order") VALUES
('مركبات وسيارات', 'Véhicules & Voitures', 'vehicles', 'directions_car', 1),
('عقارات وأراضي', 'Immobilier', 'real-estate', 'apartment', 2),
('هواتف وإلكترونيات', 'Téléphones & High-Tech', 'phones-tech', 'smartphone', 3),
('أجهزة كهرومنزلية', 'Electroménager', 'home-appliances', 'kitchen', 4),
('أزياء وملابس', 'Mode & Beauté', 'fashion', 'checkroom', 5),
('أثاث وديكور', 'Maison & Jardin', 'home-decor', 'chair', 6),
('وظائف وخدمات', 'Emploi & Services', 'jobs-services', 'work', 7),
('ألعاب ورياضة', 'Sports & Loisirs', 'sports-leisure', 'sports_soccer', 8)
ON CONFLICT ("slug") DO NOTHING;
