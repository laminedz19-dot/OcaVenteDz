# قوالب البريد الإلكتروني في Supabase (Email + Password)

تعتمد المصادقة في **OcaVenteDz** على **البريد الإلكتروني وكلمة المرور (Email + Password)** فقط، وتستخدم روابط التأكيد والاستعادة الرسمية (`{{ .ConfirmationURL }}`).

## 1. Confirm Signup (تأكيد الحساب الجديد)

**Subject:**
```text
تأكيد حسابك في OcaVenteDz
```

**Body (HTML):**
```html
<div dir="rtl" style="font-family: Arial, sans-serif; line-height: 1.8; color: #1f2937;">
  <h2 style="color: #059669;">مرحباً بك في OcaVenteDz</h2>
  <p>شكراً لتسجيلك في منصة البيع والشراء الجزائرية. لتفعيل حسابك، يرجى الضغط على الرابط أدناه:</p>
  <p style="margin: 24px 0;">
    <a href="{{ .ConfirmationURL }}" style="background: #059669; color: #ffffff; padding: 12px 24px; border-radius: 8px; text-decoration: none; font-weight: bold;">
      تفعيل الحساب الآن
    </a>
  </p>
  <p>إذا لم تقم بإنشاء هذا الحساب، يمكنك تجاهل هذه الرسالة بأمان.</p>
</div>
```

---

## 2. Reset Password (استعادة كلمة المرور)

**Subject:**
```text
استعادة كلمة المرور - OcaVenteDz
```

**Body (HTML):**
```html
<div dir="rtl" style="font-family: Arial, sans-serif; line-height: 1.8; color: #1f2937;">
  <h2 style="color: #059669;">إعادة تعيين كلمة المرور</h2>
  <p>تلقينا طلباً لإعادة تعيين كلمة المرور الخاصة بحسابك في OcaVenteDz. اضغط على الرابط التالي لتعيين كلمة مرور جديدة:</p>
  <p style="margin: 24px 0;">
    <a href="{{ .ConfirmationURL }}" style="background: #059669; color: #ffffff; padding: 12px 24px; border-radius: 8px; text-decoration: none; font-weight: bold;">
      تعيين كلمة مرور جديدة
    </a>
  </p>
  <p>إذا لم تطلب استعادة كلمة المرور، يرجى تجاهل هذه الرسالة.</p>
</div>
```

---

## 3. Change Email Address (تأكيد تغيير البريد الإلكتروني)

**Subject:**
```text
تأكيد تغيير البريد الإلكتروني - OcaVenteDz
```

**Body (HTML):**
```html
<div dir="rtl" style="font-family: Arial, sans-serif; line-height: 1.8; color: #1f2937;">
  <h2 style="color: #059669;">تأكيد البريد الإلكتروني الجديد</h2>
  <p>يرجى الضغط على الرابط أدناه لتأكيد تغيير بريدك الإلكتروني في OcaVenteDz:</p>
  <p style="margin: 24px 0;">
    <a href="{{ .ConfirmationURL }}" style="background: #059669; color: #ffffff; padding: 12px 24px; border-radius: 8px; text-decoration: none; font-weight: bold;">
      تأكيد البريد الإلكتروني
    </a>
  </p>
</div>
```
