# Supabase setup for OcaVenteDz

The migrations in this directory define the complete cloud data layer for OcaVenteDz.

## Apply the database

1. Open the Supabase SQL Editor for the project configured in `SUPABASE_URL`.
2. Run `20261002190000_initial_supabase_schema.sql`.
3. Run `20261002194500_add_payment_orders_and_reviews.sql`.
4. Create the first administrator by inserting the authenticated user's UUID into `public.user_roles` with role `admin` (use the dashboard SQL editor; never ship a service-role key in the apps):

```sql
insert into public.user_roles (user_id, role)
values ('AUTH_USER_UUID', 'admin')
on conflict do nothing;
```

The scripts are rerunnable: tables, indexes and policies use `if not exists`/`drop policy if exists` guards. Existing production data should be backed up before applying schema changes.

## Android configuration

Set these values in `local.properties` for local builds or GitHub Actions secrets for CI:

```properties
SUPABASE_URL=https://your-project.supabase.co
SUPABASE_ANON_KEY=your-public-anon-key
```

Only the public anon key belongs in the APK. The service-role key must remain server-side and is not used by the Android clients.
