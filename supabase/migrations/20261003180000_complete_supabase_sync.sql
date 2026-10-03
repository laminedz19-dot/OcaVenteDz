-- Migration: Complete Supabase Synchronization for OcaVenteDz (App + Admin)
-- Ensures exact schema, triggers, storage policies, and RPC alignment.

-- 1. Automatic Profile and Wallet Creation on Auth User Signup
create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
  v_name text;
  v_phone text;
begin
  v_name := coalesce(new.raw_user_meta_data->>'name', 'مستخدم OcaVenteDz');
  v_phone := coalesce(new.phone, new.raw_user_meta_data->>'phone', '');

  -- Insert profile if not exists
  insert into public.profiles (
    id, email, phone, name, avatar_url, wilaya, commune, bio, role, created_at
  ) values (
    new.id,
    new.email,
    v_phone,
    v_name,
    '',
    'الجزائر',
    'الجزائر الوسطى',
    'عضو في OcaVenteDz.',
    'USER',
    now()
  ) on conflict (id) do nothing;

  -- Insert initial wallet if not exists
  insert into public.wallets (
    user_id, balance_dzd, updated_at
  ) values (
    new.id,
    0,
    now()
  ) on conflict (user_id) do nothing;

  return new;
end;
$$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
  after insert on auth.users
  for each row execute function public.handle_new_user();

-- 2. Storage Policies: Media Bucket Public Read & Admin Delete
drop policy if exists "oca media read public" on storage.objects;
create policy "oca media read public"
on storage.objects for select to public
using (bucket_id = 'oca-vente-media');

drop policy if exists "oca media delete admin" on storage.objects;
create policy "oca media delete admin"
on storage.objects for delete to authenticated
using (bucket_id = 'oca-vente-media' and public.is_admin());

drop policy if exists "oca private delete admin" on storage.objects;
create policy "oca private delete admin"
on storage.objects for delete to authenticated
using (bucket_id = 'oca-vente-private' and public.is_admin());

-- 3. Wallets: Allow users to insert initial empty wallet if missing
drop policy if exists wallets_insert_self_or_admin on public.wallets;
create policy wallets_insert_self_or_admin
on public.wallets for insert to authenticated
with check (user_id = auth.uid() or public.is_admin());

-- 4. Secure RPCs for Wallet, Top-Up, and Listings

-- 4.1 Approve Top Up (Admin only, transactional, prevents race conditions)
create or replace function public.approve_top_up(
  p_request_id uuid,
  p_admin_note text default 'تم التحقق من الوصل بنجاح'
)
returns boolean
language plpgsql
security definer
set search_path = public
as $$
declare
  v_req record;
  v_now timestamptz := now();
  v_tx_id uuid := gen_random_uuid();
begin
  if not public.is_admin() then
    raise exception 'غير مصرح: يجب أن تكون مشرفاً لتنفيذ هذه العملية.';
  end if;

  select * into v_req
  from public.top_up_requests
  where id = p_request_id
  for update;

  if not found then
    raise exception 'طلب الشحن غير موجود.';
  end if;

  if v_req.status <> 'PENDING' then
    raise exception 'تمت معالجة هذا الطلب مسبقاً (الحالة الحالية: %).', v_req.status;
  end if;

  if v_req.amount_dzd <= 0 then
    raise exception 'مبلغ الشحن غير صالح.';
  end if;

  insert into public.wallets (user_id, balance_dzd, updated_at)
  values (v_req.user_id, v_req.amount_dzd, v_now)
  on conflict (user_id) do update
  set balance_dzd = wallets.balance_dzd + v_req.amount_dzd,
      updated_at = v_now;

  insert into public.wallet_transactions (
    id, user_id, type, amount, description, reference_id, created_at
  ) values (
    v_tx_id,
    v_req.user_id,
    'TOPUP',
    v_req.amount_dzd,
    'شحن رصيد - ' || coalesce(v_req.provider, 'تحويل') || ' (مرجع: ' || coalesce(nullif(v_req.reference, ''), p_request_id::text) || ')',
    p_request_id::text,
    v_now
  );

  update public.top_up_requests
  set status = 'APPROVED',
      admin_note = coalesce(nullif(p_admin_note, ''), 'تمت الموافقة من قبل المشرف'),
      reviewed_at = v_now
  where id = p_request_id;

  return true;
end;
$$;

-- 4.2 Reject Top Up (Admin only, transactional)
create or replace function public.reject_top_up(
  p_request_id uuid,
  p_reason text default 'الوصل غير مطابق أو غير واضح'
)
returns boolean
language plpgsql
security definer
set search_path = public
as $$
declare
  v_req record;
  v_now timestamptz := now();
begin
  if not public.is_admin() then
    raise exception 'غير مصرح: يجب أن تكون مشرفاً لتنفيذ هذه العملية.';
  end if;

  select * into v_req
  from public.top_up_requests
  where id = p_request_id
  for update;

  if not found then
    raise exception 'طلب الشحن غير موجود.';
  end if;

  if v_req.status <> 'PENDING' then
    raise exception 'تمت معالجة هذا الطلب مسبقاً (الحالة الحالية: %).', v_req.status;
  end if;

  update public.top_up_requests
  set status = 'REJECTED',
      admin_note = coalesce(nullif(p_reason, ''), 'تم رفض الطلب'),
      reviewed_at = v_now
  where id = p_request_id;

  return true;
end;
$$;

-- 4.3 Debit Wallet (prevents negative balance, user debits self or admin debits)
create or replace function public.debit_wallet(
  p_user_id uuid,
  p_amount integer,
  p_description text default 'خصم رصيد',
  p_reference_id text default null
)
returns boolean
language plpgsql
security definer
set search_path = public
as $$
declare
  v_wallet record;
  v_now timestamptz := now();
  v_tx_id uuid := gen_random_uuid();
begin
  if auth.uid() <> p_user_id and not public.is_admin() then
    raise exception 'غير مصرح: لا يمكنك خصم رصيد مستخدم آخر.';
  end if;

  if p_amount <= 0 then
    raise exception 'مبلغ الخصم يجب أن يكون أكبر من الصفر.';
  end if;

  select * into v_wallet
  from public.wallets
  where user_id = p_user_id
  for update;

  if not found or v_wallet.balance_dzd < p_amount then
    raise exception 'رصيد المحفظة غير كافٍ. الرصيد الحالي: % دج والمطلوب: % دج', coalesce(v_wallet.balance_dzd, 0), p_amount;
  end if;

  update public.wallets
  set balance_dzd = balance_dzd - p_amount,
      updated_at = v_now
  where user_id = p_user_id;

  insert into public.wallet_transactions (
    id, user_id, type, amount, description, reference_id, created_at
  ) values (
    v_tx_id,
    p_user_id,
    'AD_PAYMENT',
    -p_amount,
    p_description,
    p_reference_id,
    v_now
  );

  return true;
end;
$$;

-- 4.4 Pay and Submit Listing (Atomic listing payment and publishing status update)
create or replace function public.pay_and_submit_listing(
  p_listing_id uuid,
  p_package_type text default 'STANDARD',
  p_payment_method text default 'WALLET'
)
returns json
language plpgsql
security definer
set search_path = public
as $$
declare
  v_listing record;
  v_settings record;
  v_fee integer;
  v_now timestamptz := now();
  v_payment_id text := 'PAY_' || upper(substring(replace(gen_random_uuid()::text, '-', '') from 1 for 8));
  v_new_status text;
  v_is_featured boolean := false;
  v_is_urgent boolean := false;
begin
  select * into v_listing
  from public.listings
  where id = p_listing_id
  for update;

  if not found then
    raise exception 'الإعلان غير موجود.';
  end if;

  if v_listing.user_id <> auth.uid() and not public.is_admin() then
    raise exception 'غير مصرح: لا تملك هذا الإعلان.';
  end if;

  select * into v_settings
  from public.platform_settings
  where id = 'global'
  limit 1;

  v_fee := case upper(p_package_type)
    when 'FEATURED' then coalesce(v_settings.featured_ad_fee_dzd, 600)
    when 'URGENT' then coalesce(v_settings.urgent_ad_fee_dzd, 1000)
    else coalesce(v_settings.standard_ad_fee_dzd, 400)
  end;

  v_is_featured := (upper(p_package_type) in ('FEATURED', 'URGENT'));
  v_is_urgent := (upper(p_package_type) = 'URGENT');
  v_new_status := case when coalesce(v_settings.auto_publish_after_payment, false) then 'PUBLISHED' else 'UNDER_REVIEW' end;

  if upper(p_payment_method) = 'WALLET' then
    perform public.debit_wallet(
      v_listing.user_id,
      v_fee,
      'دفع رسوم نشر إعلان (' || upper(p_package_type) || ')',
      v_payment_id
    );
  end if;

  insert into public.payment_orders (
    payment_id, user_id, listing_id, amount, currency, status, provider, transaction_reference, created_at, completed_at
  ) values (
    v_payment_id,
    v_listing.user_id,
    p_listing_id,
    v_fee,
    'DZD',
    'SUCCESS',
    upper(p_payment_method),
    v_payment_id,
    v_now,
    v_now
  );

  update public.listings
  set is_paid = true,
      status = v_new_status,
      publishing_fee_dzd = v_fee,
      package_type = upper(p_package_type),
      is_featured = v_is_featured,
      is_urgent = v_is_urgent,
      updated_at = v_now
  where id = p_listing_id;

  return json_build_object(
    'success', true,
    'payment_id', v_payment_id,
    'status', v_new_status,
    'amount', v_fee
  );
end;
$$;

-- 4.5 Increment Listing Views
create or replace function public.increment_listing_views(
  p_listing_id uuid
)
returns void
language sql
security definer
set search_path = public
as $$
  update public.listings
  set views_count = coalesce(views_count, 0) + 1
  where id = p_listing_id;
$$;

-- 5. Grant Permissions on RPC Functions
grant execute on function public.is_admin() to authenticated, anon;
grant execute on function public.approve_top_up(uuid, text) to authenticated;
grant execute on function public.reject_top_up(uuid, text) to authenticated;
grant execute on function public.debit_wallet(uuid, integer, text, text) to authenticated;
grant execute on function public.pay_and_submit_listing(uuid, text, text) to authenticated;
grant execute on function public.increment_listing_views(uuid) to authenticated, anon;

-- 6. Ensure Platform Settings Default Row
insert into public.platform_settings (
  id, standard_ad_fee_dzd, featured_ad_fee_dzd, urgent_ad_fee_dzd,
  ad_duration_days, auto_publish_after_payment, is_free_promo_active
) values (
  'global', 400, 600, 1000, 30, true, false
) on conflict (id) do nothing;
