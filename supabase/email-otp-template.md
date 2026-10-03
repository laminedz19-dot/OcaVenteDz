# قالب تأكيد البريد برمز OTP

حتى تصل رسالة التسجيل كرمز رقمي بدل رابط، يجب تعديل قالب **Confirm signup** في Supabase Dashboard:

- Authentication → Email Templates → Confirm signup
- Subject: `رمز تأكيد حسابك في OcaVenteDz`
- Body:

```html
<h2>تأكيد البريد الإلكتروني</h2>
<p>أدخل رمز التحقق التالي داخل تطبيق OcaVenteDz:</p>
<p style="font-size: 28px; font-weight: bold; letter-spacing: 8px;">{{ .Token }}</p>
<p>ينتهي الرمز خلال مدة قصيرة. إذا لم تطلب إنشاء الحساب فتجاهل هذه الرسالة.</p>
```

لا تستخدم `{{ .ConfirmationURL }}` في هذا القالب، لأن التطبيق ينتظر رمزًا من 6 أرقام.

يجب أن يبقى خيار **Confirm email** مفعّلًا من Authentication → Providers → Email.
