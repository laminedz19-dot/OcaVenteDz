-- Stores only public FCM device tokens. Firebase service-account credentials stay server-side.
create table if not exists public.admin_push_tokens (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  token text not null unique,
  platform text not null default 'android',
  enabled boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index if not exists admin_push_tokens_user_idx on public.admin_push_tokens(user_id) where enabled;
alter table public.admin_push_tokens enable row level security;

drop policy if exists admin_push_tokens_select_self on public.admin_push_tokens;
create policy admin_push_tokens_select_self on public.admin_push_tokens
  for select to authenticated using (user_id = auth.uid() and public.is_admin());

drop policy if exists admin_push_tokens_insert_self on public.admin_push_tokens;
create policy admin_push_tokens_insert_self on public.admin_push_tokens
  for insert to authenticated with check (user_id = auth.uid() and public.is_admin());

drop policy if exists admin_push_tokens_update_self on public.admin_push_tokens;
create policy admin_push_tokens_update_self on public.admin_push_tokens
  for update to authenticated using (user_id = auth.uid() and public.is_admin())
  with check (user_id = auth.uid() and public.is_admin());

drop policy if exists admin_push_tokens_delete_self on public.admin_push_tokens;
create policy admin_push_tokens_delete_self on public.admin_push_tokens
  for delete to authenticated using (user_id = auth.uid() and public.is_admin());
