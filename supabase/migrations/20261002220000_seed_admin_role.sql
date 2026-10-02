-- OcaVenteDz: grant the first administrator role.
-- Run after creating the Auth user with email laminedz.19@gmail.com.
-- The password is managed only by Supabase Auth and is never stored here.
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
