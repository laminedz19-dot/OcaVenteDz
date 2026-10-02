# Supabase setup for OcaVenteDz

The migrations in this directory define the complete cloud data layer for OcaVenteDz.

## Apply the database

1. In **Authentication → Users**, create or confirm the user `laminedz.19@gmail.com` and set its initial password there. The password is intentionally not committed to Git.
2. Open the Supabase SQL Editor for the project configured in `SUPABASE_URL`.
3. Run `20261002190000_initial_supabase_schema.sql`.
4. Run `20261002194500_add_payment_orders_and_reviews.sql`.
5. Run `20261002220000_seed_admin_role.sql`. It grants the `admin` role to the authenticated user matching `laminedz.19@gmail.com` and updates the profile role to `ADMIN`.

The admin application verifies the server-side `user_roles` record on every admin login. The **Security** section in the admin app lets the authenticated administrator change the Supabase password after re-entering the current password.

The scripts are rerunnable: tables, indexes and policies use `if not exists`/`drop policy if exists` guards. Existing production data should be backed up before applying schema changes.

## Android configuration

Set these values in `local.properties` for local builds or GitHub Actions secrets for CI:

```properties
SUPABASE_URL=https://your-project.supabase.co
SUPABASE_ANON_KEY=your-public-anon-key
```

Only the public anon key belongs in the APK. The service-role key must remain server-side and is not used by the Android clients. Never put an Auth password in SQL, source code, GitHub Actions, or the APK.
