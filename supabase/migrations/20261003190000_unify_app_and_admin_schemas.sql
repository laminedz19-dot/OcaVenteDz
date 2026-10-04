-- Migration: 20261003180000_unify_app_and_admin_schemas.sql
-- Description: Unify database schema, columns, storage policies, and triggers
-- between the User App (:app) and Admin App (:admin) for complete compatibility.

-- 1. Ensure all columns needed by both apps exist on public.listings
alter table public.listings
  add column if not exists user_name text not null default '',
  add column if not exists user_phone text not null default '',
  add column if not exists is_phone_visible boolean not null default true,
  add column if not exists wilaya_name text not null default '',
  add column if not exists images_json text not null default '';

-- 2. Trigger: Bidirectional sync on public.listings for wilaya/wilaya_name and images/images_json
create or replace function public.sync_listing_columns()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
  img_item text;
begin
  -- Sync wilaya and wilaya_name
  if coalesce(new.wilaya, '') = '' and coalesce(new.wilaya_name, '') <> '' then
    new.wilaya := new.wilaya_name;
  elsif coalesce(new.wilaya_name, '') = '' and coalesce(new.wilaya, '') <> '' then
    new.wilaya_name := new.wilaya;
  end if;

  -- Sync images (jsonb) and images_json (comma-separated string)
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

  -- Auto-populate user_name and user_phone from profiles if not supplied
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

-- 3. Trigger: Auto-populate user_name and user_phone on top_up_requests
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

-- 4. Auto-confirm users and provision profile and wallet when a user signs up
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

  -- Check if user is administrator
  if lower(coalesce(new.email, '')) in ('laminedz.19@gmail.com', 'laminedz19@gmail.com') then
    v_is_admin := true;
  end if;

  -- Create or update profile
  insert into public.profiles (id, phone, email, name, role)
  values (new.id, v_phone, v_email, v_name, case when v_is_admin then 'ADMIN' else 'USER' end)
  on conflict (id) do update set
    phone = case when public.profiles.phone = '' then excluded.phone else public.profiles.phone end,
    email = case when public.profiles.email = '' then excluded.email else public.profiles.email end,
    name = case when public.profiles.name = '' or public.profiles.name = 'مستخدم OcaVenteDz' then excluded.name else public.profiles.name end,
    role = case when v_is_admin then 'ADMIN' else public.profiles.role end;

  -- Ensure user role record for admin
  if v_is_admin then
    insert into public.user_roles (user_id, role)
    values (new.id, 'admin')
    on conflict (user_id, role) do nothing;
  end if;

  -- Create wallet
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

-- RPC: Direct phone registration bypassing SMTP requirements
create or replace function public.register_phone_user(
  p_phone text,
  p_password text,
  p_name text,
  p_wilaya text default 'الجزائر',
  p_commune text default 'الجزائر الوسطى'
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

  v_primary_email := 'dz' || v_clean_phone || '@gmail.com';
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
  values (v_user_id, v_clean_phone, '', p_name, p_wilaya, p_commune, 'USER')
  on conflict (id) do update set
    phone = excluded.phone,
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

-- 5. Platform settings synchronization between 'global' and '1'
insert into public.platform_settings (id, standard_ad_fee_dzd, featured_ad_fee_dzd, urgent_ad_fee_dzd, ad_duration_days, auto_publish_after_payment, is_free_promo_active)
values ('global', 400, 600, 1000, 30, true, false)
on conflict (id) do nothing;

insert into public.platform_settings (id, standard_ad_fee_dzd, featured_ad_fee_dzd, urgent_ad_fee_dzd, ad_duration_days, auto_publish_after_payment, is_free_promo_active)
values ('1', 400, 600, 1000, 30, true, false)
on conflict (id) do nothing;

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

-- 6. Storage Buckets and Policies
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

-- Media bucket public read policy
drop policy if exists "oca media read public" on storage.objects;
create policy "oca media read public" on storage.objects
  for select using (bucket_id = 'oca-vente-media');

-- Admin full access to media and private buckets
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

-- 7. Ensure RLS policies allow Admin full control on all tables
drop policy if exists listings_admin_all on public.listings;
create policy listings_admin_all on public.listings
  for all to authenticated
  using (public.is_admin())
  with check (public.is_admin());

drop policy if exists orders_admin_all on public.orders;
create policy orders_admin_all on public.orders
  for all to authenticated
  using (public.is_admin())
  with check (public.is_admin());

drop policy if exists reports_admin_all on public.reports;
create policy reports_admin_all on public.reports
  for all to authenticated
  using (public.is_admin())
  with check (public.is_admin());

drop policy if exists top_up_admin_all on public.top_up_requests;
create policy top_up_admin_all on public.top_up_requests
  for all to authenticated
  using (public.is_admin())
  with check (public.is_admin());

drop policy if exists profiles_admin_all on public.profiles;
create policy profiles_admin_all on public.profiles
  for all to authenticated
  using (public.is_admin())
  with check (public.is_admin());
