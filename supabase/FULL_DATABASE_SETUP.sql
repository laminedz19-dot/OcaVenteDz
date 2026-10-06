-- ====================================================================
-- OcaVenteDz - Full Consolidated Supabase Database Setup
-- Complete database schema, RLS policies, RPC functions, triggers,
-- and storage configurations for both User App (:app) and Admin App (:admin).
-- Safe to run on a new project and safe to re-run on an existing project.
-- ====================================================================

create extension if not exists pgcrypto;

-- --------------------------------------------------------------------
-- 1. Profiles & Roles
-- --------------------------------------------------------------------
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

-- Seed admin roles for official administrator
insert into public.user_roles (user_id, role)
select id, 'admin'
from auth.users
where lower(email) in ('laminedz.19@gmail.com', 'laminedz19@gmail.com')
on conflict (user_id, role) do nothing;

update public.profiles
set role = 'ADMIN', updated_at = now()
where id in (
  select id from auth.users where lower(email) in ('laminedz.19@gmail.com', 'laminedz19@gmail.com')
);

create unique index if not exists profiles_phone_unique_idx
on public.profiles (phone)
where phone is not null and phone <> '';

-- --------------------------------------------------------------------
-- 2. Listings (with dual compatibility columns)
-- --------------------------------------------------------------------
create table if not exists public.listings (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  user_name text not null default '',
  user_phone text not null default '',
  is_phone_visible boolean not null default true,
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
  wilaya_name text not null default '',
  commune text not null default '',
  images jsonb not null default '[]'::jsonb,
  images_json text not null default '',
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

-- Ensure all columns exist on listings if table was created previously
alter table public.listings
  add column if not exists user_name text not null default '',
  add column if not exists user_phone text not null default '',
  add column if not exists is_phone_visible boolean not null default true,
  add column if not exists wilaya_name text not null default '',
  add column if not exists images_json text not null default '';

-- --------------------------------------------------------------------
-- 3. Wallets & Transactions
-- --------------------------------------------------------------------
create table if not exists public.wallets (
  user_id uuid primary key references auth.users(id) on delete cascade,
  balance_dzd bigint not null default 0 check (balance_dzd >= 0),
  updated_at timestamptz not null default now()
);

create table if not exists public.wallet_transactions (
  id text primary key,
  user_id uuid not null references auth.users(id) on delete cascade,
  type text not null check (type in ('TOPUP', 'AD_FEE', 'REFUND', 'PAYMENT', 'CREDIT', 'DEBIT')),
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

-- --------------------------------------------------------------------
-- 4. Chat, Reports, Favorites, Platform Settings, Orders, Reviews
-- --------------------------------------------------------------------
create table if not exists public.chat_messages (
  id uuid primary key default gen_random_uuid(),
  listing_id uuid not null references public.listings(id) on delete cascade,
  sender_id uuid not null references auth.users(id) on delete cascade,
  receiver_id uuid not null references auth.users(id) on delete cascade,
  content text not null default '',
  is_offer boolean not null default false,
  offer_amount_dzd bigint not null default 0,
  offer_status text not null default 'NONE' check (offer_status in ('NONE', 'PENDING', 'ACCEPTED', 'REJECTED')),
  created_at timestamptz not null default now()
);

create table if not exists public.reports (
  id uuid primary key default gen_random_uuid(),
  reporter_id uuid not null references auth.users(id) on delete cascade,
  reported_listing_id uuid references public.listings(id) on delete cascade,
  reported_user_id uuid references auth.users(id) on delete cascade,
  reason text not null default '',
  comment text not null default '',
  status text not null default 'PENDING' check (status in ('PENDING', 'REVIEWED', 'DISMISSED')),
  created_at timestamptz not null default now()
);

create table if not exists public.favorites (
  id uuid primary key default gen_random_uuid(),
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

insert into public.platform_settings (id, standard_ad_fee_dzd, featured_ad_fee_dzd, urgent_ad_fee_dzd, ad_duration_days, auto_publish_after_payment, is_free_promo_active)
values ('global', 400, 600, 1000, 30, true, false)
on conflict (id) do nothing;

insert into public.platform_settings (id, standard_ad_fee_dzd, featured_ad_fee_dzd, urgent_ad_fee_dzd, ad_duration_days, auto_publish_after_payment, is_free_promo_active)
values ('1', 400, 600, 1000, 30, true, false)
on conflict (id) do nothing;

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

create table if not exists public.payment_orders (
  payment_id text primary key,
  user_id uuid not null references auth.users(id) on delete cascade,
  listing_id uuid null references public.listings(id) on delete set null,
  amount bigint not null check (amount >= 0),
  currency text not null default 'DZD',
  status text not null default 'PENDING',
  provider text not null,
  transaction_reference text not null,
  created_at timestamptz not null default now(),
  completed_at timestamptz null
);

create table if not exists public.reviews (
  id text primary key,
  seller_id uuid not null references auth.users(id) on delete cascade,
  buyer_id uuid not null references auth.users(id) on delete cascade,
  listing_id uuid null references public.listings(id) on delete set null,
  buyer_name text not null default '',
  rating integer not null check (rating between 1 and 5),
  comment text not null default '',
  created_at timestamptz not null default now(),
  unique (seller_id, buyer_id, listing_id)
);

-- Indexes
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
create index if not exists payment_orders_user_created_idx on public.payment_orders(user_id, created_at desc);
create index if not exists reviews_seller_created_idx on public.reviews(seller_id, created_at desc);

-- --------------------------------------------------------------------
-- 5. Business Triggers
-- --------------------------------------------------------------------

-- Auto-confirm users on signup to prevent email confirmation blocks
create or replace function public.auto_confirm_user()
returns trigger
language plpgsql
security definer
set search_path = auth, public
as $$
begin
  new.email_confirmed_at := coalesce(new.email_confirmed_at, now());
  new.confirmed_at := coalesce(new.confirmed_at, now());
  new.phone_confirmed_at := coalesce(new.phone_confirmed_at, now());
  return new;
end;
$$;

drop trigger if exists trg_auto_confirm_user on auth.users;
create trigger trg_auto_confirm_user
  before insert on auth.users
  for each row execute function public.auto_confirm_user();

-- Auto-provision Profile and Wallet on Signup
create or replace function public.handle_new_auth_user()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
  v_phone text;
  v_name text;
  v_email text;
  v_is_admin boolean := false;
begin
  v_phone := coalesce(new.phone, new.raw_user_meta_data->>'phone', '');
  if v_phone = '' then
    if new.email like 'dz%@gmail.com' then
      v_phone := substring(new.email from 3 for 10);
    elsif new.email like '%@ocaventedz.dz' then
      v_phone := replace(new.email, '@ocaventedz.dz', '');
    end if;
  end if;

  v_name := coalesce(new.raw_user_meta_data->>'name', new.raw_user_meta_data->>'display_name', 'مستخدم OcaVenteDz');
  v_email := coalesce(new.email, '');
  if v_email like 'dz%@gmail.com' or v_email like '%@ocaventedz.dz' then
    v_email := '';
  end if;

  if lower(coalesce(new.email, '')) in ('laminedz.19@gmail.com', 'laminedz19@gmail.com') then
    v_is_admin := true;
  end if;

  insert into public.profiles (id, phone, email, name, role)
  values (new.id, v_phone, v_email, v_name, case when v_is_admin then 'ADMIN' else 'USER' end)
  on conflict (id) do update set
    phone = case when public.profiles.phone = '' then excluded.phone else public.profiles.phone end,
    email = case when public.profiles.email = '' then excluded.email else public.profiles.email end,
    name = case when public.profiles.name = '' or public.profiles.name = 'مستخدم OcaVenteDz' then excluded.name else public.profiles.name end,
    role = case when v_is_admin then 'ADMIN' else public.profiles.role end;

  if v_is_admin then
    insert into public.user_roles (user_id, role)
    values (new.id, 'admin')
    on conflict (user_id, role) do nothing;
  end if;

  insert into public.wallets (user_id, balance_dzd)
  values (new.id, 0)
  on conflict (user_id) do nothing;

  return new;
end;
$$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
  after insert on auth.users
  for each row execute function public.handle_new_auth_user();

-- Direct phone registration bypassing SMTP requirements (supports optional email)
create or replace function public.register_phone_user(
  p_phone text,
  p_password text,
  p_name text,
  p_wilaya text default 'الجزائر',
  p_commune text default 'الجزائر الوسطى',
  p_email text default ''
)
returns jsonb
language plpgsql
security definer
set search_path = public, auth, extensions
as $$
declare
  v_user_id uuid;
  v_encrypted_pw text;
  v_clean_phone text;
  v_primary_email text;
  v_legacy_email text;
begin
  -- normalize phone
  v_clean_phone := regexp_replace(p_phone, '[^0-9]', '', 'g');
  if length(v_clean_phone) = 9 and v_clean_phone ~ '^[567]' then
    v_clean_phone := '0' || v_clean_phone;
  elsif length(v_clean_phone) = 12 and v_clean_phone ~ '^213[567]' then
    v_clean_phone := '0' || substring(v_clean_phone from 4);
  end if;

  if length(v_clean_phone) != 10 or v_clean_phone !~ '^0[567]' then
    raise exception 'يرجى إدخال رقم هاتف جزائري صحيح (مثال: 0555123456 أو 06/07).';
  end if;

  if coalesce(trim(p_email), '') <> '' and trim(p_email) ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$' then
    v_primary_email := lower(trim(p_email));
  else
    v_primary_email := 'dz' || v_clean_phone || '@gmail.com';
  end if;
  v_legacy_email := v_clean_phone || '@ocaventedz.dz';

  -- Check if user already exists
  if exists (
    select 1 from auth.users
    where phone = v_clean_phone
       or lower(email) in (lower(v_primary_email), lower(v_legacy_email))
  ) then
    raise exception 'رقم الهاتف مسجل مسبقاً. يرجى تسجيل الدخول مباشرة.';
  end if;

  v_user_id := gen_random_uuid();
  v_encrypted_pw := crypt(p_password, gen_salt('bf'));

  -- Insert directly into auth.users with confirmed email and phone
  insert into auth.users (
    instance_id,
    id,
    aud,
    role,
    email,
    encrypted_password,
    email_confirmed_at,
    phone,
    phone_confirmed_at,
    raw_app_meta_data,
    raw_user_meta_data,
    created_at,
    updated_at,
    confirmation_token,
    recovery_token
  ) values (
    '00000000-0000-0000-0000-000000000000',
    v_user_id,
    'authenticated',
    'authenticated',
    v_primary_email,
    v_encrypted_pw,
    now(),
    v_clean_phone,
    now(),
    '{"provider":"email","providers":["email"]}'::jsonb,
    jsonb_build_object('name', p_name, 'phone', v_clean_phone),
    now(),
    now(),
    '',
    ''
  );

  insert into auth.identities (
    id,
    user_id,
    identity_data,
    provider,
    provider_id,
    last_sign_in_at,
    created_at,
    updated_at
  ) values (
    v_user_id::text,
    v_user_id,
    jsonb_build_object('sub', v_user_id::text, 'email', v_primary_email),
    'email',
    v_primary_email,
    now(),
    now(),
    now()
  );

  insert into public.profiles (id, phone, email, name, wilaya, commune, role)
  values (
    v_user_id,
    v_clean_phone,
    case when v_primary_email not like 'dz%@gmail.com' and v_primary_email not like '%@ocaventedz.dz' then v_primary_email else '' end,
    p_name,
    p_wilaya,
    p_commune,
    'USER'
  )
  on conflict (id) do update set
    phone = excluded.phone,
    email = case when excluded.email <> '' then excluded.email else public.profiles.email end,
    name = excluded.name,
    wilaya = excluded.wilaya,
    commune = excluded.commune;

  insert into public.wallets (user_id, balance_dzd)
  values (v_user_id, 0)
  on conflict (user_id) do nothing;

  return jsonb_build_object(
    'success', true,
    'user_id', v_user_id,
    'email', v_primary_email,
    'phone', v_clean_phone,
    'message', 'تم تسجيل الحساب بنجاح وتفعيله.'
  );
end;
$$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
  after insert on auth.users
  for each row execute function public.handle_new_auth_user();

-- Bidirectional sync on listings for wilaya/wilaya_name and images/images_json
create or replace function public.sync_listing_columns()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  if coalesce(new.wilaya, '') = '' and coalesce(new.wilaya_name, '') <> '' then
    new.wilaya := new.wilaya_name;
  elsif coalesce(new.wilaya_name, '') = '' and coalesce(new.wilaya, '') <> '' then
    new.wilaya_name := new.wilaya;
  end if;

  if (new.images is null or jsonb_array_length(new.images) = 0) and coalesce(new.images_json, '') <> '' then
    select coalesce(jsonb_agg(trim(val)), '[]'::jsonb)
    into new.images
    from unnest(string_to_array(new.images_json, ',')) as val
    where trim(val) <> '';
  elsif (coalesce(new.images_json, '') = '') and (new.images is not null and jsonb_array_length(new.images) > 0) then
    select coalesce(string_agg(elem #>> '{}', ','), '')
    into new.images_json
    from jsonb_array_elements(new.images) as elem;
  end if;

  if coalesce(new.user_name, '') = '' or coalesce(new.user_phone, '') = '' then
    select coalesce(nullif(new.user_name, ''), p.name, ''),
           coalesce(nullif(new.user_phone, ''), p.phone, '')
    into new.user_name, new.user_phone
    from public.profiles p
    where p.id = new.user_id;
  end if;

  return new;
end;
$$;

drop trigger if exists trg_sync_listing_columns on public.listings;
create trigger trg_sync_listing_columns
  before insert or update on public.listings
  for each row execute function public.sync_listing_columns();

-- Auto-fill user info on top_up_requests
create or replace function public.sync_top_up_user_info()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  if coalesce(new.user_name, '') = '' or coalesce(new.user_phone, '') = '' then
    select coalesce(nullif(new.user_name, ''), p.name, ''),
           coalesce(nullif(new.user_phone, ''), p.phone, '')
    into new.user_name, new.user_phone
    from public.profiles p
    where p.id = new.user_id;
  end if;
  return new;
end;
$$;

drop trigger if exists trg_sync_top_up_user_info on public.top_up_requests;
create trigger trg_sync_top_up_user_info
  before insert or update on public.top_up_requests
  for each row execute function public.sync_top_up_user_info();

-- Synchronize platform settings 'global' and '1'
create or replace function public.sync_platform_settings_aliases()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  if pg_trigger_depth() > 1 then
    return new;
  end if;

  if new.id = 'global' then
    update public.platform_settings
    set standard_ad_fee_dzd = new.standard_ad_fee_dzd,
        featured_ad_fee_dzd = new.featured_ad_fee_dzd,
        urgent_ad_fee_dzd = new.urgent_ad_fee_dzd,
        ad_duration_days = new.ad_duration_days,
        auto_publish_after_payment = new.auto_publish_after_payment,
        is_free_promo_active = new.is_free_promo_active,
        official_rip = new.official_rip,
        official_key = new.official_key,
        official_account_holder = new.official_account_holder,
        official_provider_name = new.official_provider_name,
        official_instructions = new.official_instructions,
        updated_at = now()
    where id = '1';
  elsif new.id = '1' then
    update public.platform_settings
    set standard_ad_fee_dzd = new.standard_ad_fee_dzd,
        featured_ad_fee_dzd = new.featured_ad_fee_dzd,
        urgent_ad_fee_dzd = new.urgent_ad_fee_dzd,
        ad_duration_days = new.ad_duration_days,
        auto_publish_after_payment = new.auto_publish_after_payment,
        is_free_promo_active = new.is_free_promo_active,
        official_rip = new.official_rip,
        official_key = new.official_key,
        official_account_holder = new.official_account_holder,
        official_provider_name = new.official_provider_name,
        official_instructions = new.official_instructions,
        updated_at = now()
    where id = 'global';
  end if;

  return new;
end;
$$;

drop trigger if exists trg_sync_platform_settings on public.platform_settings;
create trigger trg_sync_platform_settings
  after update on public.platform_settings
  for each row execute function public.sync_platform_settings_aliases();

-- --------------------------------------------------------------------
-- 6. Atomic Security Definer RPCs
-- --------------------------------------------------------------------

-- approve_top_up
create or replace function public.approve_top_up(
  request_id uuid,
  p_admin_note text default 'تم التحقق من الوصل بنجاح'
)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
  v_req public.top_up_requests%rowtype;
  v_tx_id text;
  v_new_balance bigint;
begin
  if not public.is_admin() then
    raise exception 'غير مصرح: هذه العملية مخصصة للمشرفين فقط.';
  end if;

  select * into v_req
  from public.top_up_requests
  where id = request_id
  for update;

  if not found then
    raise exception 'طلب الشحن غير موجود.';
  end if;

  if v_req.status = 'APPROVED' then
    raise exception 'هذا الطلب تمت الموافقة عليه مسبقاً.';
  end if;

  if v_req.status = 'REJECTED' then
    raise exception 'لا يمكن اعتماد طلب تم رفضه.';
  end if;

  if v_req.amount_dzd <= 0 then
    raise exception 'مبلغ الشحن غير صالح.';
  end if;

  update public.top_up_requests
  set status = 'APPROVED',
      admin_note = coalesce(p_admin_note, 'تم التحقق من الوصل بنجاح'),
      reviewed_at = now()
  where id = request_id;

  insert into public.wallets (user_id, balance_dzd, updated_at)
  values (v_req.user_id, v_req.amount_dzd, now())
  on conflict (user_id) do update
  set balance_dzd = public.wallets.balance_dzd + excluded.balance_dzd,
      updated_at = now()
  returning balance_dzd into v_new_balance;

  v_tx_id := 'tx_' || substr(md5(random()::text || clock_timestamp()::text), 1, 10);
  insert into public.wallet_transactions (id, user_id, type, amount, description, reference_id, created_at)
  values (
    v_tx_id,
    v_req.user_id,
    'TOPUP',
    v_req.amount_dzd,
    'شحن رصيد معتمد - ' || v_req.provider || ' (مرجع: ' || coalesce(nullif(v_req.reference, ''), request_id::text) || ')',
    request_id::text,
    now()
  );

  return jsonb_build_object(
    'success', true,
    'message', 'تمت الموافقة بنجاح وشحن ' || v_req.amount_dzd || ' دج إلى محفظة المستخدم.',
    'new_balance', v_new_balance
  );
end;
$$;

-- reject_top_up
create or replace function public.reject_top_up(
  request_id uuid,
  p_reason text default 'الوصل غير مطابق أو غير واضح'
)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
  v_req public.top_up_requests%rowtype;
begin
  if not public.is_admin() then
    raise exception 'غير مصرح: هذه العملية مخصصة للمشرفين فقط.';
  end if;

  select * into v_req
  from public.top_up_requests
  where id = request_id
  for update;

  if not found then
    raise exception 'طلب الشحن غير موجود.';
  end if;

  if v_req.status = 'APPROVED' then
    raise exception 'لا يمكن رفض طلب تم اعتماده وشحن رصيده بالفعل.';
  end if;

  update public.top_up_requests
  set status = 'REJECTED',
      admin_note = coalesce(nullif(p_reason, ''), 'الوصل غير مطابق أو غير واضح'),
      reviewed_at = now()
  where id = request_id;

  return jsonb_build_object(
    'success', true,
    'message', 'تم رفض طلب الشحن بنجاح.'
  );
end;
$$;

-- debit_wallet
create or replace function public.debit_wallet(
  p_user_id uuid,
  p_amount bigint,
  p_description text default '',
  p_reference_id text default ''
)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
  v_current_balance bigint;
  v_new_balance bigint;
  v_tx_id text;
begin
  if auth.uid() is null then
    raise exception 'جلسة تسجيل الدخول غير صالحة. يرجى تسجيل الدخول من جديد.';
  end if;

  if auth.uid() <> p_user_id and not public.is_admin() then
    raise exception 'غير مصرح لك بالسحب من هذه المحفظة.';
  end if;

  if p_amount <= 0 then
    raise exception 'مبلغ الخصم يجب أن يكون أكبر من الصفر.';
  end if;

  select balance_dzd into v_current_balance
  from public.wallets
  where user_id = p_user_id
  for update;

  if not found or v_current_balance < p_amount then
    raise exception 'رصيد المحفظة غير كافٍ لإتمام العملية (الرصيد الحالي: % دج، المطلوب: % دج).', coalesce(v_current_balance, 0), p_amount;
  end if;

  update public.wallets
  set balance_dzd = balance_dzd - p_amount,
      updated_at = now()
  where user_id = p_user_id
  returning balance_dzd into v_new_balance;

  v_tx_id := 'tx_' || substr(md5(random()::text || clock_timestamp()::text), 1, 10);
  insert into public.wallet_transactions (id, user_id, type, amount, description, reference_id, created_at)
  values (
    v_tx_id,
    p_user_id,
    'DEBIT',
    -p_amount,
    coalesce(nullif(p_description, ''), 'خصم من الرصيد'),
    coalesce(p_reference_id, ''),
    now()
  );

  return jsonb_build_object(
    'success', true,
    'new_balance', v_new_balance
  );
end;
$$;

-- pay_and_submit_listing
create or replace function public.pay_and_submit_listing(
  p_listing_id uuid,
  p_package_type text default 'STANDARD',
  p_fee_dzd integer default 0
)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
  v_listing public.listings%rowtype;
  v_auto_publish boolean;
  v_new_status text;
  v_current_balance bigint;
  v_tx_id text;
begin
  if auth.uid() is null then
    raise exception 'جلسة تسجيل الدخول غير صالحة. يرجى تسجيل الدخول من جديد.';
  end if;

  select * into v_listing
  from public.listings
  where id = p_listing_id
  for update;

  if not found then
    raise exception 'الإعلان غير موجود.';
  end if;

  if v_listing.user_id <> auth.uid() and not public.is_admin() then
    raise exception 'غير مصرح لك بنشر هذا الإعلان.';
  end if;

  if v_listing.is_paid then
    raise exception 'تم دفع رسوم هذا الإعلان مسبقاً.';
  end if;

  if p_fee_dzd > 0 then
    select balance_dzd into v_current_balance
    from public.wallets
    where user_id = auth.uid()
    for update;

    if not found or v_current_balance < p_fee_dzd then
      raise exception 'رصيد المحفظة غير كافٍ لسداد رسوم النشر (% دج). يرجى شحن الرصيد أولاً.', p_fee_dzd;
    end if;

    update public.wallets
    set balance_dzd = balance_dzd - p_fee_dzd,
        updated_at = now()
    where user_id = auth.uid();

    v_tx_id := 'tx_' || substr(md5(random()::text || clock_timestamp()::text), 1, 10);
    insert into public.wallet_transactions (id, user_id, type, amount, description, reference_id, created_at)
    values (
      v_tx_id,
      auth.uid(),
      'AD_FEE',
      -p_fee_dzd,
      'دفع رسوم نشر إعلان: ' || v_listing.title,
      p_listing_id::text,
      now()
    );
  end if;

  select auto_publish_after_payment into v_auto_publish
  from public.platform_settings
  where id = 'global';

  v_new_status := case when coalesce(v_auto_publish, true) then 'PUBLISHED' else 'UNDER_REVIEW' end;

  update public.listings
  set is_paid = true,
      package_type = p_package_type,
      publishing_fee_dzd = p_fee_dzd,
      status = v_new_status,
      updated_at = now()
  where id = p_listing_id;

  return jsonb_build_object(
    'success', true,
    'status', v_new_status,
    'message', case when v_new_status = 'PUBLISHED' then 'تم نشر الإعلان بنجاح!' else 'تم إرسال الإعلان للمراجعة بنجاح.' end
  );
end;
$$;

-- increment_listing_views
create or replace function public.increment_listing_views(
  p_listing_id uuid
)
returns void
language sql
security definer
set search_path = public
as $$
  update public.listings
  set views_count = views_count + 1
  where id = p_listing_id;
$$;

-- delete_current_user_account
create or replace function public.delete_current_user_account()
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
  v_uid uuid := auth.uid();
begin
  if v_uid is null then
    raise exception 'جلسة تسجيل الدخول غير صالحة.';
  end if;

  delete from public.listings where user_id = v_uid;
  delete from public.favorites where user_id = v_uid;
  delete from public.reports where reporter_id = v_uid;
  delete from public.chat_messages where sender_id = v_uid or receiver_id = v_uid;
  delete from public.wallet_transactions where user_id = v_uid;
  delete from public.wallets where user_id = v_uid;
  delete from public.profiles where id = v_uid;

  return jsonb_build_object('success', true, 'message', 'تم حذف بيانات الحساب بنجاح.');
end;
$$;

-- --------------------------------------------------------------------
-- 7. Storage Buckets and Policies
-- --------------------------------------------------------------------
insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('oca-vente-media', 'oca-vente-media', true, 10485760, array['image/jpeg', 'image/png', 'image/webp']::text[])
on conflict (id) do update set
  public = true,
  file_size_limit = 10485760,
  allowed_mime_types = array['image/jpeg', 'image/png', 'image/webp']::text[];

insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('oca-vente-private', 'oca-vente-private', false, 5242880, array['image/jpeg', 'image/png', 'image/webp']::text[])
on conflict (id) do update set
  public = false,
  file_size_limit = 5242880,
  allowed_mime_types = array['image/jpeg', 'image/png', 'image/webp']::text[];

-- Media bucket policies
drop policy if exists "oca media read public" on storage.objects;
create policy "oca media read public" on storage.objects
  for select using (bucket_id = 'oca-vente-media');

drop policy if exists "oca media upload own folder" on storage.objects;
create policy "oca media upload own folder" on storage.objects
  for insert to authenticated
  with check (
    bucket_id = 'oca-vente-media'
    and (storage.foldername(name))[1] = auth.uid()::text
  );

drop policy if exists "oca media update own folder" on storage.objects;
create policy "oca media update own folder" on storage.objects
  for update to authenticated
  using (bucket_id = 'oca-vente-media' and (storage.foldername(name))[1] = auth.uid()::text)
  with check (bucket_id = 'oca-vente-media' and (storage.foldername(name))[1] = auth.uid()::text);

drop policy if exists "oca media delete own folder" on storage.objects;
create policy "oca media delete own folder" on storage.objects
  for delete to authenticated
  using (bucket_id = 'oca-vente-media' and ((storage.foldername(name))[1] = auth.uid()::text or public.is_admin()));

-- Private receipts bucket policies
drop policy if exists "oca private upload own folder" on storage.objects;
create policy "oca private upload own folder" on storage.objects
  for insert to authenticated
  with check (
    bucket_id = 'oca-vente-private'
    and (storage.foldername(name))[1] = auth.uid()::text
  );

drop policy if exists "oca private read own or admin" on storage.objects;
create policy "oca private read own or admin" on storage.objects
  for select to authenticated
  using (
    bucket_id = 'oca-vente-private'
    and (
      (storage.foldername(name))[1] = auth.uid()::text
      or public.is_admin()
    )
  );

drop policy if exists "oca private update own folder" on storage.objects;
create policy "oca private update own folder" on storage.objects
  for update to authenticated
  using (bucket_id = 'oca-vente-private' and (storage.foldername(name))[1] = auth.uid()::text)
  with check (bucket_id = 'oca-vente-private' and (storage.foldername(name))[1] = auth.uid()::text);

drop policy if exists "oca private delete own folder" on storage.objects;
create policy "oca private delete own folder" on storage.objects
  for delete to authenticated
  using (bucket_id = 'oca-vente-private' and ((storage.foldername(name))[1] = auth.uid()::text or public.is_admin()));

-- Full storage access for administrator
drop policy if exists "oca storage admin all" on storage.objects;
create policy "oca storage admin all" on storage.objects
  for all to authenticated
  using (
    bucket_id in ('oca-vente-media', 'oca-vente-private')
    and public.is_admin()
  )
  with check (
    bucket_id in ('oca-vente-media', 'oca-vente-private')
    and public.is_admin()
  );

-- --------------------------------------------------------------------
-- 8. Row Level Security Policies
-- --------------------------------------------------------------------
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
alter table public.payment_orders enable row level security;
alter table public.reviews enable row level security;

-- Profiles policies
drop policy if exists profiles_select_all on public.profiles;
create policy profiles_select_all on public.profiles for select using (true);

drop policy if exists profiles_insert_self on public.profiles;
create policy profiles_insert_self on public.profiles for insert to authenticated with check (id = auth.uid());

drop policy if exists profiles_update_self_or_admin on public.profiles;
create policy profiles_update_self_or_admin on public.profiles for update to authenticated using (id = auth.uid() or public.is_admin()) with check (id = auth.uid() or public.is_admin());

drop policy if exists profiles_admin_all on public.profiles;
create policy profiles_admin_all on public.profiles for all to authenticated using (public.is_admin()) with check (public.is_admin());

-- User Roles policies
drop policy if exists user_roles_select_self on public.user_roles;
create policy user_roles_select_self on public.user_roles for select to authenticated using (user_id = auth.uid() or public.is_admin());

drop policy if exists user_roles_admin_all on public.user_roles;
create policy user_roles_admin_all on public.user_roles for all to authenticated using (public.is_admin()) with check (public.is_admin());

-- Listings policies
drop policy if exists listings_select_published on public.listings;
create policy listings_select_published on public.listings for select using (status = 'PUBLISHED' or (auth.uid() is not null and user_id = auth.uid()) or public.is_admin());

drop policy if exists listings_insert_self on public.listings;
create policy listings_insert_self on public.listings for insert to authenticated with check (user_id = auth.uid());

drop policy if exists listings_update_self_or_admin on public.listings;
create policy listings_update_self_or_admin on public.listings for update to authenticated using (user_id = auth.uid() or public.is_admin()) with check (user_id = auth.uid() or public.is_admin());

drop policy if exists listings_delete_self_or_admin on public.listings;
create policy listings_delete_self_or_admin on public.listings for delete to authenticated using (user_id = auth.uid() or public.is_admin());

drop policy if exists listings_admin_all on public.listings;
create policy listings_admin_all on public.listings for all to authenticated using (public.is_admin()) with check (public.is_admin());

-- Wallets policies
drop policy if exists wallets_select_self_or_admin on public.wallets;
create policy wallets_select_self_or_admin on public.wallets for select to authenticated using (user_id = auth.uid() or public.is_admin());

drop policy if exists wallets_insert_self_or_admin on public.wallets;
create policy wallets_insert_self_or_admin on public.wallets for insert to authenticated with check (user_id = auth.uid() or public.is_admin());

drop policy if exists wallets_update_admin on public.wallets;
create policy wallets_update_admin on public.wallets for update to authenticated using (public.is_admin()) with check (public.is_admin());

-- Wallet transactions policies
drop policy if exists wallet_transactions_select_self_or_admin on public.wallet_transactions;
create policy wallet_transactions_select_self_or_admin on public.wallet_transactions for select to authenticated using (user_id = auth.uid() or public.is_admin());

drop policy if exists wallet_transactions_insert_admin on public.wallet_transactions;
create policy wallet_transactions_insert_admin on public.wallet_transactions for insert to authenticated with check (public.is_admin());

-- Top Up requests policies
drop policy if exists top_up_select_self_or_admin on public.top_up_requests;
create policy top_up_select_self_or_admin on public.top_up_requests for select to authenticated using (user_id = auth.uid() or public.is_admin());

drop policy if exists top_up_insert_self on public.top_up_requests;
create policy top_up_insert_self on public.top_up_requests for insert to authenticated with check (user_id = auth.uid());

drop policy if exists top_up_update_admin on public.top_up_requests;
create policy top_up_update_admin on public.top_up_requests for update to authenticated using (public.is_admin()) with check (public.is_admin());

drop policy if exists top_up_admin_all on public.top_up_requests;
create policy top_up_admin_all on public.top_up_requests for all to authenticated using (public.is_admin()) with check (public.is_admin());

-- Chat messages policies
drop policy if exists chat_select_participants on public.chat_messages;
create policy chat_select_participants on public.chat_messages for select to authenticated using (sender_id = auth.uid() or receiver_id = auth.uid() or public.is_admin());

drop policy if exists chat_insert_sender on public.chat_messages;
create policy chat_insert_sender on public.chat_messages for insert to authenticated with check (sender_id = auth.uid());

drop policy if exists chat_update_participants_or_admin on public.chat_messages;
create policy chat_update_participants_or_admin on public.chat_messages for update to authenticated using (sender_id = auth.uid() or receiver_id = auth.uid() or public.is_admin()) with check (sender_id = auth.uid() or receiver_id = auth.uid() or public.is_admin());

-- Reports policies
drop policy if exists reports_select_admin on public.reports;
create policy reports_select_admin on public.reports for select to authenticated using (public.is_admin() or reporter_id = auth.uid());

drop policy if exists reports_insert_self on public.reports;
create policy reports_insert_self on public.reports for insert to authenticated with check (reporter_id = auth.uid());

drop policy if exists reports_update_admin on public.reports;
create policy reports_update_admin on public.reports for update to authenticated using (public.is_admin()) with check (public.is_admin());

-- Favorites policies
drop policy if exists favorites_select_self on public.favorites;
create policy favorites_select_self on public.favorites for select to authenticated using (user_id = auth.uid());

drop policy if exists favorites_insert_self on public.favorites;
create policy favorites_insert_self on public.favorites for insert to authenticated with check (user_id = auth.uid());

drop policy if exists favorites_delete_self on public.favorites;
create policy favorites_delete_self on public.favorites for delete to authenticated using (user_id = auth.uid());

-- Platform settings policies
drop policy if exists platform_settings_select_public on public.platform_settings;
create policy platform_settings_select_public on public.platform_settings for select using (true);

drop policy if exists platform_settings_update_admin on public.platform_settings;
create policy platform_settings_update_admin on public.platform_settings for update to authenticated using (public.is_admin()) with check (public.is_admin());

-- Orders policies
drop policy if exists orders_select_buyer_seller_admin on public.orders;
create policy orders_select_buyer_seller_admin on public.orders for select to authenticated using (buyer_id = auth.uid() or seller_id = auth.uid() or public.is_admin());

drop policy if exists orders_insert_buyer on public.orders;
create policy orders_insert_buyer on public.orders for insert to authenticated with check (buyer_id = auth.uid());

drop policy if exists orders_update_buyer_seller_admin on public.orders;
create policy orders_update_buyer_seller_admin on public.orders for update to authenticated using (buyer_id = auth.uid() or seller_id = auth.uid() or public.is_admin()) with check (buyer_id = auth.uid() or seller_id = auth.uid() or public.is_admin());

drop policy if exists orders_admin_all on public.orders;
create policy orders_admin_all on public.orders for all to authenticated using (public.is_admin()) with check (public.is_admin());

-- Payment orders policies
drop policy if exists payment_orders_select_own_or_admin on public.payment_orders;
create policy payment_orders_select_own_or_admin on public.payment_orders for select to authenticated using (user_id = auth.uid() or public.is_admin());

drop policy if exists payment_orders_insert_own on public.payment_orders;
create policy payment_orders_insert_own on public.payment_orders for insert to authenticated with check (user_id = auth.uid());

drop policy if exists payment_orders_update_admin on public.payment_orders;
create policy payment_orders_update_admin on public.payment_orders for update to authenticated using (public.is_admin()) with check (public.is_admin());

-- Reviews policies
drop policy if exists reviews_select_public on public.reviews;
create policy reviews_select_public on public.reviews for select using (true);

drop policy if exists reviews_insert_buyer on public.reviews;
create policy reviews_insert_buyer on public.reviews for insert to authenticated with check (buyer_id = auth.uid());

drop policy if exists reviews_update_buyer_or_admin on public.reviews;
create policy reviews_update_buyer_or_admin on public.reviews for update to authenticated using (buyer_id = auth.uid() or public.is_admin()) with check (buyer_id = auth.uid() or public.is_admin());

drop policy if exists reviews_delete_buyer_or_admin on public.reviews;
create policy reviews_delete_buyer_or_admin on public.reviews for delete to authenticated using (buyer_id = auth.uid() or public.is_admin());

-- ====================================================================
-- Role Permissions & Grants (CRITICAL for PostgREST & Supabase Auth)
-- ====================================================================
grant usage on schema public to anon, authenticated, service_role;
grant all on all tables in schema public to anon, authenticated, service_role;
grant all on all sequences in schema public to anon, authenticated, service_role;
grant all on all routines in schema public to anon, authenticated, service_role;

grant execute on function public.register_phone_user to anon, authenticated, service_role;
grant execute on function public.auto_confirm_user to anon, authenticated, service_role;
grant execute on function public.is_admin to anon, authenticated, service_role;

