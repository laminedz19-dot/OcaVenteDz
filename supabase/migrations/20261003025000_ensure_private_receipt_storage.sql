-- Ensure the private bucket used by wallet receipt uploads exists.
insert into storage.buckets (
  id,
  name,
  public,
  file_size_limit,
  allowed_mime_types
)
values (
  'oca-vente-private',
  'oca-vente-private',
  false,
  5242880,
  array['image/jpeg', 'image/png', 'image/webp']::text[]
)
on conflict (id) do update set
  name = excluded.name,
  public = false,
  file_size_limit = excluded.file_size_limit,
  allowed_mime_types = excluded.allowed_mime_types;

-- Authenticated users may upload only below their own user-id folder.
drop policy if exists "oca private upload own folder" on storage.objects;
create policy "oca private upload own folder"
on storage.objects for insert to authenticated
with check (
  bucket_id = 'oca-vente-private'
  and (storage.foldername(name))[1] = (select auth.uid()::text)
);

-- Users may read their own receipts; admins may read all receipts for review.
drop policy if exists "oca private read own or admin" on storage.objects;
create policy "oca private read own or admin"
on storage.objects for select to authenticated
using (
  bucket_id = 'oca-vente-private'
  and (
    (storage.foldername(name))[1] = (select auth.uid()::text)
    or public.is_admin()
  )
);
