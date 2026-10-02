-- Repair Auth rows created by manual SQL or legacy imports.
-- Supabase Auth expects nullable text fields to contain empty strings where applicable.
update auth.users
set
  aud = coalesce(aud, ''),
  role = coalesce(role, ''),
  email = coalesce(email, ''),
  encrypted_password = coalesce(encrypted_password, ''),
  confirmation_token = coalesce(confirmation_token, ''),
  recovery_token = coalesce(recovery_token, ''),
  email_change_token_new = coalesce(email_change_token_new, ''),
  email_change = coalesce(email_change, ''),
  email_change_token_current = coalesce(email_change_token_current, ''),
  phone = coalesce(phone, ''),
  phone_change = coalesce(phone_change, ''),
  phone_change_token = coalesce(phone_change_token, ''),
  reauthentication_token = coalesce(reauthentication_token, ''),
  raw_app_meta_data = coalesce(raw_app_meta_data, '{}'::jsonb),
  raw_user_meta_data = coalesce(raw_user_meta_data, '{}'::jsonb),
  is_super_admin = coalesce(is_super_admin, false),
  is_anonymous = coalesce(is_anonymous, false),
  is_sso_user = coalesce(is_sso_user, false),
  updated_at = now();
