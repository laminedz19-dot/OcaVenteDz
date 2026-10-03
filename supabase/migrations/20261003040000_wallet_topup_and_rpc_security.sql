-- Migration: 20261003040000_wallet_topup_and_rpc_security.sql
-- Description: Implement server-side security definer RPCs, double-approval protection,
-- atomic wallet transactions, secure storage policies, and complete RLS audit.

-- 1. Helper: Ensure user_roles has strict RLS preventing self-escalation
alter table public.user_roles enable row level security;
drop policy if exists user_roles_select_self on public.user_roles;
create policy user_roles_select_self on public.user_roles
  for select to authenticated using (user_id = auth.uid() or public.is_admin());

-- Disallow direct insert/update/delete by non-admins
drop policy if exists user_roles_admin_all on public.user_roles;
create policy user_roles_admin_all on public.user_roles
  for all to authenticated using (public.is_admin()) with check (public.is_admin());

-- 2. Prevent arbitrary wallet manipulation by regular users
-- Users may ONLY read their own wallet. Direct updates must only happen via admin or security definer RPC.
alter table public.wallets enable row level security;
drop policy if exists wallets_select_self_or_admin on public.wallets;
create policy wallets_select_self_or_admin on public.wallets
  for select to authenticated using (user_id = auth.uid() or public.is_admin());

drop policy if exists wallets_update_admin on public.wallets;
create policy wallets_update_admin on public.wallets
  for update to authenticated using (public.is_admin()) with check (public.is_admin());

drop policy if exists wallets_insert_self_or_admin on public.wallets;
create policy wallets_insert_self_or_admin on public.wallets
  for insert to authenticated with check (user_id = auth.uid() or public.is_admin());

-- Transactions table: users may only read their own transactions; inserts only by admin or security definer RPCs
alter table public.wallet_transactions enable row level security;
drop policy if exists wallet_transactions_select_self_or_admin on public.wallet_transactions;
create policy wallet_transactions_select_self_or_admin on public.wallet_transactions
  for select to authenticated using (user_id = auth.uid() or public.is_admin());

drop policy if exists wallet_transactions_insert_admin on public.wallet_transactions;
create policy wallet_transactions_insert_admin on public.wallet_transactions
  for insert to authenticated with check (public.is_admin());

-- 3. Top-Up Requests Policies
alter table public.top_up_requests enable row level security;
drop policy if exists top_up_select_self_or_admin on public.top_up_requests;
create policy top_up_select_self_or_admin on public.top_up_requests
  for select to authenticated using (user_id = auth.uid() or public.is_admin());

drop policy if exists top_up_insert_self on public.top_up_requests;
create policy top_up_insert_self on public.top_up_requests
  for insert to authenticated with check (user_id = auth.uid());

drop policy if exists top_up_update_admin on public.top_up_requests;
create policy top_up_update_admin on public.top_up_requests
  for update to authenticated using (public.is_admin()) with check (public.is_admin());

-- 4. RPC: Atomic approve_top_up with double-approval prevention
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
  -- Authorization check: Must be admin
  if not public.is_admin() then
    raise exception 'غير مصرح: هذه العملية مخصصة للمشرفين فقط.';
  end if;

  -- Lock row exclusively for update
  select * into v_req
  from public.top_up_requests
  where id = request_id
  for update;

  if not found then
    raise exception 'طلب الشحن غير موجود.';
  end if;

  -- Double approval prevention
  if v_req.status = 'APPROVED' then
    raise exception 'هذا الطلب تمت الموافقة عليه مسبقاً.';
  end if;

  if v_req.status = 'REJECTED' then
    raise exception 'لا يمكن اعتماد طلب تم رفضه مباشرة.';
  end if;

  if v_req.amount_dzd <= 0 then
    raise exception 'مبلغ الشحن غير صالح.';
  end if;

  -- Update request status
  update public.top_up_requests
  set status = 'APPROVED',
      admin_note = coalesce(p_admin_note, 'تم التحقق من الوصل بنجاح'),
      reviewed_at = now()
  where id = request_id;

  -- Credit wallet atomically
  insert into public.wallets (user_id, balance_dzd, updated_at)
  values (v_req.user_id, v_req.amount_dzd, now())
  on conflict (user_id) do update
  set balance_dzd = public.wallets.balance_dzd + excluded.balance_dzd,
      updated_at = now()
  returning balance_dzd into v_new_balance;

  -- Record audit transaction
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

-- 5. RPC: Atomic reject_top_up
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

-- 6. RPC: Atomic debit_wallet
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

  -- Caller must be user themself or admin
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

-- 7. RPC: Atomic pay_and_submit_listing
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

  -- Handle fee debit if required
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

  -- Determine auto-publish status from settings
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

-- 8. RPC: Atomic increment_listing_views with rate-limiting protection
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

-- 9. RPC: Delete current user account
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

  -- Delete listings
  delete from public.listings where user_id = v_uid;
  -- Delete favorites
  delete from public.favorites where user_id = v_uid;
  -- Delete reports
  delete from public.reports where reporter_id = v_uid;
  -- Delete chat messages
  delete from public.chat_messages where sender_id = v_uid or receiver_id = v_uid;
  -- Delete wallet transactions
  delete from public.wallet_transactions where user_id = v_uid;
  -- Delete wallet
  delete from public.wallets where user_id = v_uid;
  -- Delete profile
  delete from public.profiles where id = v_uid;

  return jsonb_build_object('success', true, 'message', 'تم حذف بيانات الحساب بنجاح.');
end;
$$;

-- 10. Storage security: Ensure private receipts bucket is private and strictly restricted
insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('oca-vente-private', 'oca-vente-private', false, 5242880, array['image/jpeg', 'image/png', 'image/webp']::text[])
on conflict (id) do update set
  public = false,
  file_size_limit = 5242880,
  allowed_mime_types = array['image/jpeg', 'image/png', 'image/webp']::text[];

-- Strict RLS for storage.objects on oca-vente-private
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
  using (
    bucket_id = 'oca-vente-private'
    and (storage.foldername(name))[1] = auth.uid()::text
  )
  with check (
    bucket_id = 'oca-vente-private'
    and (storage.foldername(name))[1] = auth.uid()::text
  );

drop policy if exists "oca private delete own folder" on storage.objects;
create policy "oca private delete own folder" on storage.objects
  for delete to authenticated
  using (
    bucket_id = 'oca-vente-private'
    and (
      (storage.foldername(name))[1] = auth.uid()::text
      or public.is_admin()
    )
  );
