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

create index if not exists payment_orders_user_created_idx on public.payment_orders(user_id, created_at desc);
create index if not exists payment_orders_listing_idx on public.payment_orders(listing_id);
create index if not exists reviews_seller_created_idx on public.reviews(seller_id, created_at desc);

alter table public.payment_orders enable row level security;
alter table public.reviews enable row level security;

drop policy if exists payment_orders_select_own_or_admin on public.payment_orders;
create policy payment_orders_select_own_or_admin on public.payment_orders for select to authenticated using (user_id = auth.uid() or public.is_admin());
drop policy if exists payment_orders_insert_own on public.payment_orders;
create policy payment_orders_insert_own on public.payment_orders for insert to authenticated with check (user_id = auth.uid());
drop policy if exists payment_orders_update_admin on public.payment_orders;
create policy payment_orders_update_admin on public.payment_orders for update to authenticated using (public.is_admin()) with check (public.is_admin());

drop policy if exists reviews_select_public on public.reviews;
create policy reviews_select_public on public.reviews for select to authenticated using (true);
drop policy if exists reviews_insert_buyer on public.reviews;
create policy reviews_insert_buyer on public.reviews for insert to authenticated with check (buyer_id = auth.uid());
drop policy if exists reviews_update_buyer_or_admin on public.reviews;
create policy reviews_update_buyer_or_admin on public.reviews for update to authenticated using (buyer_id = auth.uid() or public.is_admin()) with check (buyer_id = auth.uid() or public.is_admin());
drop policy if exists reviews_delete_buyer_or_admin on public.reviews;
create policy reviews_delete_buyer_or_admin on public.reviews for delete to authenticated using (buyer_id = auth.uid() or public.is_admin());
