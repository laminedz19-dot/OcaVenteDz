export type UserRole = 'USER' | 'ADMIN' | 'SUPER_ADMIN';

export type ListingStatus = 'PENDING' | 'ACTIVE' | 'REJECTED' | 'SOLD';

export type PaymentMethod = 'CCP' | 'BARIDIMOB' | 'CASH' | 'VOUCHER';

export type RechargeStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

export interface UserDTO {
  id: string;
  email: string;
  username: string;
  phone?: string | null;
  wilaya: string;
  role: UserRole;
  balance: number;
  avatarUrl?: string | null;
  isBlocked?: boolean;
  createdAt: string;
}

export interface CategoryDTO {
  id: string;
  nameAr: string;
  nameFr: string;
  slug: string;
  icon: string;
  order: number;
}

export interface ListingImageDTO {
  id: string;
  url: string;
  isPrimary: boolean;
  order: number;
}

export interface ListingDTO {
  id: string;
  title: string;
  description: string;
  price: number;
  categoryId: string;
  category?: CategoryDTO;
  userId: string;
  user?: Partial<UserDTO>;
  wilaya: string;
  commune?: string | null;
  phone: string;
  isNegotiable: boolean;
  status: ListingStatus;
  rejectionReason?: string | null;
  views: number;
  isFeatured: boolean;
  featuredUntil?: string | null;
  images: ListingImageDTO[];
  createdAt: string;
  updatedAt: string;
}

export interface RechargeRequestDTO {
  id: string;
  userId: string;
  user?: Partial<UserDTO>;
  amount: number;
  paymentMethod: PaymentMethod;
  receiptNumber: string;
  receiptImageUrl?: string | null;
  status: RechargeStatus;
  adminNotes?: string | null;
  reviewedBy?: string | null;
  createdAt: string;
  reviewedAt?: string | null;
}

export interface BalanceTransactionDTO {
  id: string;
  userId: string;
  amount: number;
  type: string;
  description: string;
  balanceAfter: number;
  createdAt: string;
}

export interface DashboardStatsDTO {
  totalUsers: number;
  totalActiveListings: number;
  pendingListingsCount: number;
  pendingRechargeCount: number;
  totalRechargedAmount: number;
}
