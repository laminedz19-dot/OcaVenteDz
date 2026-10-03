# إشعارات طلبات الشحن

1. شغّل migration `20261002221000_admin_push_tokens.sql`.
2. أضف `admin/google-services.json` من مشروع Firebase إلى نسخة البناء الخاصة بتطبيق الإدارة. لا تضع ملف حساب الخدمة هنا.
3. انشر الدالة:

```bash
supabase functions deploy notify-top-up --no-verify-jwt
supabase secrets set FIREBASE_SERVICE_ACCOUNT_JSON="$(cat firebase-service-account.json)"
supabase secrets set NOTIFY_WEBHOOK_SECRET="ضع-قيمة-عشوائية-قوية"
```

4. أنشئ Database Webhook في Supabase:
   - Table: `public.top_up_requests`
   - Event: `INSERT`
   - URL: `https://<project-ref>.supabase.co/functions/v1/notify-top-up`
   - أرسل ترويسة `x-webhook-secret: <نفس القيمة>`.

الدالة تستخدم `SUPABASE_SERVICE_ROLE_KEY` وبيانات حساب Firebase داخل Edge Function فقط. لا تُضمّن هذه القيم في Android أو Git.
