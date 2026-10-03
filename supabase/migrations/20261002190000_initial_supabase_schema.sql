-- OcaVenteDz Supabase baseline schema.
-- Safe to run on a new project and safe to re-run on an existing project.

create extension if not exists pgcrypto;

create table if not exists public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  email text not null default '',
  phone text not null default '',
  name text not null default '',
  avatar_url text not null default '',
  wilaya text not null default '',
  commune text not null default '',
  bio text not null default '',
  seller_rating numeric(3,2) not null default 0,
  reviews_count integer not null default 0,
  ads_count integer not null default 0,
  is_verified boolean not null default false,
  verification_requested boolean not null default false,
  is_banned boolean not null default false,
  role text not null default 'USER' check (role in ('USER', 'ADMIN')),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.user_roles (
  user_id uuid not null references auth.users(id) on delete cascade,
  role text not null check (role in ('admin', 'moderator')),
  primary key (user_id, role)
);

-- Grant the first admin role after the Supabase Auth account exists.
-- The password is intentionally managed by Supabase Auth and never stored in SQL.
insert into public.user_roles (user_id, role)
select id, 'admin'
from auth.users
where lower(email) = lower('laminedz.19@gmail.com')
on conflict (user_id, role) do nothing;

update public.profiles
set role = 'ADMIN', updated_at = now()
where id in (
  select id from auth.users where lower(email) = lower('laminedz.19@gmail.com')
);

create or replace function public.is_admin()
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select exists (
    select 1 from public.user_roles
    where user_id = auth.uid() and role = 'admin'
  );
$$;

create table if not exists public.listings (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  title text not null default '',
  description text not null default '',
  category_id text not null default '',
  category_name_ar text not null default '',
  subcategory text not null default '',
  price_dzd bigint not null default 0 check (price_dzd >= 0),
  is_negotiable boolean not null default false,
  condition text not null default 'NEW',
  wilaya_code integer not null default 16,
  wilaya text not null default '',
  commune text not null default '',
  images jsonb not null default '[]'::jsonb,
  video_url text not null default '',
  status text not null default 'DRAFT',
  rejection_reason text not null default '',
  package_type text not null default 'STANDARD',
  publishing_fee_dzd integer not null default 0,
  is_paid boolean not null default false,
  is_featured boolean not null default false,
  is_urgent boolean not null default false,
  views_count integer not null default 0,
  expires_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.wallets (
  user_id uuid primary key references auth.users(id) on delete cascade,
  balance_dzd bigint not null default 0 check (balance_dzd >= 0),
  updated_at timestamptz not null default now()
);

create table if not exists public.wallet_transactions (
  id text primary key,
  user_id uuid not null references auth.users(id) on delete cascade,
  type text not null,
  amount bigint not null,
  description text not null default '',
  reference_id text not null default '',
  created_at timestamptz not null default now()
);

create table if not exists public.top_up_requests (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  user_name text not null default '',
  user_phone text not null default '',
  amount_dzd bigint not null check (amount_dzd > 0),
  provider text not null,
  reference text not null default '',
  receipt_path text not null default '',
  status text not null default 'PENDING' check (status in ('PENDING', 'APPROVED', 'REJECTED')),
  admin_note text not null default '',
  created_at timestamptz not null default now(),
  reviewed_at timestamptz
);

create table if not exists public.chat_messages (
  id uuid primary key default gen_random_uuid(),
  listing_id uuid not null references public.listings(id) on delete cascade,
  sender_id uuid not null references auth.users(id) on delete cascade,
  receiver_id uuid not null references auth.users(id) on delete cascade,
  content text not null default '',
  is_offer boolean not null default false,
  offer_amount_dzd bigint not null default 0,
  offer_status text not null default 'NONE',
  created_at timestamptz not null default now()
);

create table if not exists public.reports (
  id uuid primary key default gen_random_uuid(),
  reporter_id uuid not null references auth.users(id) on delete cascade,
  reported_listing_id uuid references public.listings(id) on delete set null,
  reported_user_id uuid references auth.users(id) on delete set null,
  reason text not null,
  comment text not null default '',
  status text not null default 'PENDING',
  created_at timestamptz not null default now()
);

create table if not exists public.favorites (
  id text primary key,
  user_id uuid not null references auth.users(id) on delete cascade,
  listing_id uuid not null references public.listings(id) on delete cascade,
  notify_price_drop boolean not null default true,
  notify_similar boolean not null default true,
  created_at timestamptz not null default now(),
  unique (user_id, listing_id)
);

create table if not exists public.platform_settings (
  id text primary key default 'global',
  standard_ad_fee_dzd integer not null default 400,
  featured_ad_fee_dzd integer not null default 600,
  urgent_ad_fee_dzd integer not null default 1000,
  ad_duration_days integer not null default 30,
  auto_publish_after_payment boolean not null default true,
  is_free_promo_active boolean not null default false,
  official_rip text not null default '',
  official_key text not null default '',
  official_account_holder text not null default '',
  official_provider_name text not null default '',
  official_instructions text not null default '',
  updated_at timestamptz not null default now()
);

create table if not exists public.orders (
  id uuid primary key default gen_random_uuid(),
  order_number text not null,
  listing_id uuid references public.listings(id) on delete set null,
  listing_title text not null default '',
  listing_image_url text not null default '',
  seller_id uuid not null references auth.users(id) on delete cascade,
  seller_name text not null default '',
  seller_phone text not null default '',
  buyer_id uuid not null references auth.users(id) on delete cascade,
  buyer_name text not null default '',
  buyer_phone text not null default '',
  buyer_wilaya text not null default '',
  buyer_commune text not null default '',
  buyer_address text not null default '',
  quantity integer not null default 1 check (quantity > 0),
  unit_price_dzd bigint not null default 0,
  delivery_fee_dzd bigint not null default 0,
  total_amount_dzd bigint not null default 0,
  payment_method text not null default 'COD',
  is_paid boolean not null default false,
  status text not null default 'PENDING',
  tracking_number text not null default '',
  buyer_notes text not null default '',
  status_note text not null default '',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  delivered_at timestamptz,
  cancelled_at timestamptz
);

create index if not exists listings_status_created_idx on public.listings(status, created_at desc);
create index if not exists listings_user_created_idx on public.listings(user_id, created_at desc);
create index if not exists listings_search_idx on public.listings(wilaya_code, category_id, status, created_at desc);
create index if not exists top_up_status_created_idx on public.top_up_requests(status, created_at desc);
create index if not exists top_up_user_created_idx on public.top_up_requests(user_id, created_at desc);
create index if not exists wallet_transactions_user_created_idx on public.wallet_transactions(user_id, created_at desc);
create index if not exists chat_listing_created_idx on public.chat_messages(listing_id, created_at asc);
create index if not exists reports_status_created_idx on public.reports(status, created_at desc);
create index if not exists orders_buyer_created_idx on public.orders(buyer_id, created_at desc);
create index if not exists orders_seller_created_idx on public.orders(seller_id, created_at desc);

insert into public.platform_settings (id)
values ('global')
on conflict (id) do nothing;

alter table public.profiles enable row level security;
alter table public.user_roles enable row level security;
alter table public.listings enable row level security;
alter table public.wallets enable row level security;
alter table public.wallet_transactions enable row level security;
alter table public.top_up_requests enable row level security;
alter table public.chat_messages enable row level security;
alter table public.reports enable row level security;
alter table public.favorites enable row level security;
alter table public.platform_settings enable row level security;
alter table public.orders enable row level security;

-- A user may read only their own role; public.is_admin() remains security-definer and bypasses this policy.
drop policy if exists user_roles_select_self on public.user_roles;
create policy user_roles_select_self on public.user_roles for select to authenticated using (user_id = auth.uid());

-- Profiles and listings are readable by authenticated users; writes are owner/admin scoped.
drop policy if exists profiles_select_authenticated on public.profiles;
create policy profiles_select_authenticated on public.profiles for select to authenticated using (true);
drop policy if exists profiles_insert_self on public.profiles;
create policy profiles_insert_self on public.profiles for insert to authenticated with check (id = auth.uid());
drop policy if exists profiles_update_self_or_admin on public.profiles;
create policy profiles_update_self_or_admin on public.profiles for update to authenticated using (id = auth.uid() or public.is_admin()) with check (id = auth.uid() or public.is_admin());
drop policy if exists listings_select_authenticated on public.listings;
create policy listings_select_authenticated on public.listings for select to authenticated using (status = 'PUBLISHED' or user_id = auth.uid() or public.is_admin());
drop policy if exists listings_insert_owner on public.listings;
create policy listings_insert_owner on public.listings for insert to authenticated with check (user_id = auth.uid());
drop policy if exists listings_update_owner_or_admin on public.listings;
create policy listings_update_owner_or_admin on public.listings for update to authenticated using (user_id = auth.uid() or public.is_admin()) with check (user_id = auth.uid() or public.is_admin());
drop policy if exists listings_delete_owner_or_admin on public.listings;
create policy listings_delete_owner_or_admin on public.listings for delete to authenticated using (user_id = auth.uid() or public.is_admin());

drop policy if exists wallets_select_self_or_admin on public.wallets;
create policy wallets_select_self_or_admin on public.wallets for select to authenticated using (user_id = auth.uid() or public.is_admin());
drop policy if exists wallets_update_admin on public.wallets;
create policy wallets_update_admin on public.wallets for update to authenticated using (public.is_admin()) with check (public.is_admin());
drop policy if exists wallets_insert_self_or_admin on public.wallets;
create policy wallets_insert_self_or_admin on public.wallets for insert to authenticated with check (user_id = auth.uid() or public.is_admin());
drop policy if exists wallet_transactions_select_self_or_admin on public.wallet_transactions;
create policy wallet_transactions_select_self_or_admin on public.wallet_transactions for select to authenticated using (user_id = auth.uid() or public.is_admin());
drop policy if exists wallet_transactions_insert_admin on public.wallet_transactions;
create policy wallet_transactions_insert_admin on public.wallet_transactions for insert to authenticated with check (public.is_admin());

drop policy if exists top_up_select_self_or_admin on public.top_up_requests;
create policy top_up_select_self_or_admin on public.top_up_requests for select to authenticated using (user_id = auth.uid() or public.is_admin());
drop policy if exists top_up_insert_self on public.top_up_requests;
create policy top_up_insert_self on public.top_up_requests for insert to authenticated with check (user_id = auth.uid());
drop policy if exists top_up_update_admin on public.top_up_requests;
create policy top_up_update_admin on public.top_up_requests for update to authenticated using (public.is_admin()) with check (public.is_admin());

drop policy if exists messages_select_participants_or_admin on public.chat_messages;
create policy messages_select_participants_or_admin on public.chat_messages for select to authenticated using (sender_id = auth.uid() or receiver_id = auth.uid() or public.is_admin());
drop policy if exists messages_insert_sender on public.chat_messages;
create policy messages_insert_sender on public.chat_messages for insert to authenticated with check (sender_id = auth.uid());
drop policy if exists messages_update_participants_or_admin on public.chat_messages;
create policy messages_update_participants_or_admin on public.chat_messages for update to authenticated using (sender_id = auth.uid() or receiver_id = auth.uid() or public.is_admin()) with check (sender_id = auth.uid() or receiver_id = auth.uid() or public.is_admin());

drop policy if exists reports_select_admin_or_reporter on public.reports;
create policy reports_select_admin_or_reporter on public.reports for select to authenticated using (reporter_id = auth.uid() or public.is_admin());
drop policy if exists reports_insert_authenticated on public.reports;
create policy reports_insert_authenticated on public.reports for insert to authenticated with check (reporter_id = auth.uid());
drop policy if exists reports_update_admin on public.reports;
create policy reports_update_admin on public.reports for update to authenticated using (public.is_admin()) with check (public.is_admin());

drop policy if exists favorites_select_self on public.favorites;
create policy favorites_select_self on public.favorites for select to authenticated using (user_id = auth.uid());
drop policy if exists favorites_insert_self on public.favorites;
create policy favorites_insert_self on public.favorites for insert to authenticated with check (user_id = auth.uid());
drop policy if exists favorites_update_self on public.favorites;
create policy favorites_update_self on public.favorites for update to authenticated using (user_id = auth.uid()) with check (user_id = auth.uid());
drop policy if exists favorites_delete_self on public.favorites;
create policy favorites_delete_self on public.favorites for delete to authenticated using (user_id = auth.uid());

drop policy if exists settings_select_authenticated on public.platform_settings;
create policy settings_select_authenticated on public.platform_settings for select to authenticated using (true);
drop policy if exists settings_update_admin on public.platform_settings;
create policy settings_update_admin on public.platform_settings for update to authenticated using (public.is_admin()) with check (public.is_admin());
drop policy if exists orders_select_buyer_seller_admin on public.orders;
create policy orders_select_buyer_seller_admin on public.orders for select to authenticated using (buyer_id = auth.uid() or seller_id = auth.uid() or public.is_admin());
drop policy if exists orders_insert_buyer on public.orders;
create policy orders_insert_buyer on public.orders for insert to authenticated with check (buyer_id = auth.uid());
drop policy if exists orders_update_buyer_seller_admin on public.orders;
create policy orders_update_buyer_seller_admin on public.orders for update to authenticated using (buyer_id = auth.uid() or seller_id = auth.uid() or public.is_admin()) with check (buyer_id = auth.uid() or seller_id = auth.uid() or public.is_admin());

-- Public marketplace media and private payment receipts use separate buckets.
insert into storage.buckets (id, name, public)
values ('oca-vente-media', 'oca-vente-media', true)
on conflict (id) do update set public = true;
insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('oca-vente-private', 'oca-vente-private', false, 5242880, array['image/jpeg', 'image/png', 'image/webp']::text[])
on conflict (id) do update set public = false;

drop policy if exists "oca media upload own folder" on storage.objects;
create policy "oca media upload own folder" on storage.objects for insert to authenticated
with check (bucket_id = 'oca-vente-media' and (storage.foldername(name))[1] = (select auth.uid()::text));
drop policy if exists "oca media update own folder" on storage.objects;
create policy "oca media update own folder" on storage.objects for update to authenticated
using (bucket_id = 'oca-vente-media' and (storage.foldername(name))[1] = (select auth.uid()::text))
with check (bucket_id = 'oca-vente-media' and (storage.foldername(name))[1] = (select auth.uid()::text));
drop policy if exists "oca media delete own folder" on storage.objects;
create policy "oca media delete own folder" on storage.objects for delete to authenticated
using (bucket_id = 'oca-vente-media' and (storage.foldername(name))[1] = (select auth.uid()::text));

drop policy if exists "oca private upload own folder" on storage.objects;
create policy "oca private upload own folder" on storage.objects for insert to authenticated
with check (bucket_id = 'oca-vente-private' and (storage.foldername(name))[1] = (select auth.uid()::text));
drop policy if exists "oca private read own or admin" on storage.objects;
create policy "oca private read own or admin" on storage.objects for select to authenticated
using (bucket_id = 'oca-vente-private' and ((storage.foldername(name))[1] = (select auth.uid()::text) or public.is_admin()));
drop policy if exists "oca private update own folder" on storage.objects;
create policy "oca private update own folder" on storage.objects for update to authenticated
using (bucket_id = 'oca-vente-private' and (storage.foldername(name))[1] = (select auth.uid()::text))
with check (bucket_id = 'oca-vente-private' and (storage.foldername(name))[1] = (select auth.uid()::text));
drop policy if exists "oca private delete own folder" on storage.objects;
create policy "oca private delete own folder" on storage.objects for delete to authenticated
using (bucket_id = 'oca-vente-private' and (storage.foldername(name))[1] = (select auth.uid()::text));
