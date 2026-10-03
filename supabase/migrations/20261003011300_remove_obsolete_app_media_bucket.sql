-- Remove policies from the obsolete bucket without touching Storage tables directly.
drop policy if exists storage_read_app_media on storage.objects;
drop policy if exists storage_insert_app_media_authenticated on storage.objects;
drop policy if exists storage_update_app_media_authenticated on storage.objects;
drop policy if exists storage_delete_app_media_admin on storage.objects;

-- Keep the intended bucket privacy model explicit.
insert into storage.buckets (id, name, public)
values ('oca-vente-media', 'oca-vente-media', true)
on conflict (id) do update set public = true;
insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('oca-vente-private', 'oca-vente-private', false, 5242880, array['image/jpeg', 'image/png', 'image/webp']::text[])
on conflict (id) do update set public = false;

drop policy if exists "oca media upload own folder" on storage.objects;
create policy "oca media upload own folder" on storage.objects for insert to authenticated
with check (bucket_id = 'oca-vente-media' and (storage.foldername(name))[1] = (select auth.uid()::text));
drop policy if exists "oca media update own folder" on storage.objects;
create policy "oca media update own folder" on storage.objects for update to authenticated
using (bucket_id = 'oca-vente-media' and (storage.foldername(name))[1] = (select auth.uid()::text))
with check (bucket_id = 'oca-vente-media' and (storage.foldername(name))[1] = (select auth.uid()::text));
drop policy if exists "oca media delete own folder" on storage.objects;
create policy "oca media delete own folder" on storage.objects for delete to authenticated
using (bucket_id = 'oca-vente-media' and (storage.foldername(name))[1] = (select auth.uid()::text));

drop policy if exists "oca private upload own folder" on storage.objects;
create policy "oca private upload own folder" on storage.objects for insert to authenticated
with check (bucket_id = 'oca-vente-private' and (storage.foldername(name))[1] = (select auth.uid()::text));
drop policy if exists "oca private read own or admin" on storage.objects;
create policy "oca private read own or admin" on storage.objects for select to authenticated
using (bucket_id = 'oca-vente-private' and ((storage.foldername(name))[1] = (select auth.uid()::text) or public.is_admin()));
drop policy if exists "oca private update own folder" on storage.objects;
create policy "oca private update own folder" on storage.objects for update to authenticated
using (bucket_id = 'oca-vente-private' and (storage.foldername(name))[1] = (select auth.uid()::text))
with check (bucket_id = 'oca-vente-private' and (storage.foldername(name))[1] = (select auth.uid()::text));
drop policy if exists "oca private delete own folder" on storage.objects;
create policy "oca private delete own folder" on storage.objects for delete to authenticated
using (bucket_id = 'oca-vente-private' and (storage.foldername(name))[1] = (select auth.uid()::text));
