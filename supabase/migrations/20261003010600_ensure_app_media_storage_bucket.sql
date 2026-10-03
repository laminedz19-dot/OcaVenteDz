-- Ensure the storage bucket used by listing and receipt images exists.
insert into storage.buckets (id, name, public)
values ('app-media', 'app-media', true)
on conflict (id) do update set public = true;

drop policy if exists storage_read_app_media on storage.objects;
create policy storage_read_app_media
on storage.objects for select
using (bucket_id = 'app-media');

drop policy if exists storage_insert_app_media_authenticated on storage.objects;
create policy storage_insert_app_media_authenticated
on storage.objects for insert to authenticated
with check (bucket_id = 'app-media');

drop policy if exists storage_update_app_media_authenticated on storage.objects;
create policy storage_update_app_media_authenticated
on storage.objects for update to authenticated
using (bucket_id = 'app-media')
with check (bucket_id = 'app-media');

drop policy if exists storage_delete_app_media_admin on storage.objects;
create policy storage_delete_app_media_admin
on storage.objects for delete to authenticated
using (bucket_id = 'app-media' and public.is_admin());
