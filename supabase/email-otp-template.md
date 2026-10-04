# دليل تعديل قوالب البريد الإلكتروني في Supabase لإظهار رمز التحقق (OTP) بدلاً من الرابط

الرسالة التي ظهرت لك بعنوان:
`Your sign-in link (Votre lien de connexion)`
تحدث لأن القالب الافتراضي لـ **Magic Link** في Supabase يحتوي على رابط `{{ .ConfirmationURL }}` ولا يحتوي على المتغير `{{ .Token }}` المخصص لكود التحقق (6 أرقام).

لجعل Supabase يرسل **رمز التحقق الرقمي (OTP)** مباشرة إلى بريد المستخدم، اتبع الخطوات التالية في **لوحة تحكم Supabase**:

---

## 1. قالب تسجيل الدخول برمز التحقق (Magic Link) - [الأهم للقطة الشاشة]

ادخل إلى:
**Supabase Dashboard** ← **Authentication** ← **Email Templates** ← اختر تبويب **Magic Link**.

- **Message Subject (عنوان الرسالة):**
```text
رمز تسجيل الدخول الخاص بك في OcaVenteDz
```

- **Message Body (نص الرسالة):**
استبدل المحتوى بالكامل بهذا الكود الجميل:
```html
<div dir="rtl" style="font-family: Arial, sans-serif; max-width: 520px; margin: 0 auto; padding: 24px; border: 1px solid #e2e8f0; border-radius: 12px; background-color: #ffffff;">
  <div style="text-align: center; margin-bottom: 20px;">
    <h2 style="color: #047857; margin: 0; font-size: 24px;">OcaVenteDz</h2>
    <p style="color: #64748b; font-size: 14px; margin-top: 4px;">سوق التجارة الإلكترونية الجزائري</p>
  </div>
  
  <h3 style="color: #1e293b; font-size: 18px; margin-bottom: 8px;">رمز المصادقة وتسجيل الدخول</h3>
  <p style="color: #475569; font-size: 14px; line-height: 1.6;">
    أهلاً بك! استخدم رمز التحقق التالي لتسجيل الدخول إلى حسابك داخل تطبيق OcaVenteDz:
  </p>
  
  <div style="background-color: #ecfdf5; border: 2px dashed #059669; border-radius: 10px; padding: 18px; text-align: center; margin: 24px 0;">
    <span style="font-size: 36px; font-weight: bold; letter-spacing: 12px; color: #047857; font-family: monospace;">{{ .Token }}</span>
  </div>
  
  <p style="color: #64748b; font-size: 13px; line-height: 1.5;">
    ⏱ هذا الرمز صالح للاستخدام لمرة واحدة فقط وينتهي خلال دقائق معدودة.<br>
    🔒 إذا لم تطلب تسجيل الدخول، يمكنك تجاهل هذه الرسالة بأمان.
  </p>
</div>
```
ثم اضغط **Save Changes**.

---

## 2. قالب تأكيد الحساب الجديد (Confirm signup)

من نفس الصفحة **Email Templates** ← اختر تبويب **Confirm signup**:

- **Message Subject (عنوان الرسالة):**
```text
رمز تأكيد حسابك الجديد في OcaVenteDz
```

- **Message Body (نص الرسالة):**
```html
<div dir="rtl" style="font-family: Arial, sans-serif; max-width: 520px; margin: 0 auto; padding: 24px; border: 1px solid #e2e8f0; border-radius: 12px; background-color: #ffffff;">
  <div style="text-align: center; margin-bottom: 20px;">
    <h2 style="color: #047857; margin: 0; font-size: 24px;">OcaVenteDz</h2>
    <p style="color: #64748b; font-size: 14px; margin-top: 4px;">تأكيد البريد الإلكتروني</p>
  </div>
  
  <h3 style="color: #1e293b; font-size: 18px; margin-bottom: 8px;">تأكيد إنشاء الحساب</h3>
  <p style="color: #475569; font-size: 14px; line-height: 1.6;">
    شكراً لانضمامك إلى OcaVenteDz! أدخل رمز التحقق التالي لإكمال إنشاء حسابك:
  </p>
  
  <div style="background-color: #ecfdf5; border: 2px dashed #059669; border-radius: 10px; padding: 18px; text-align: center; margin: 24px 0;">
    <span style="font-size: 36px; font-weight: bold; letter-spacing: 12px; color: #047857; font-family: monospace;">{{ .Token }}</span>
  </div>
  
  <p style="color: #64748b; font-size: 13px;">
    ⏱ ينتهي هذا الرمز خلال مدة قصيرة. لا تشارك هذا الرمز مع أي شخص.
  </p>
</div>
```
ثم اضغط **Save Changes**.

---

## 3. قالب استعادة كلمة المرور (Reset Password)

من نفس الصفحة **Email Templates** ← اختر تبويب **Reset Password**:

- **Message Subject (عنوان الرسالة):**
```text
رمز استعادة كلمة المرور في OcaVenteDz
```

- **Message Body (نص الرسالة):**
```html
<div dir="rtl" style="font-family: Arial, sans-serif; max-width: 520px; margin: 0 auto; padding: 24px; border: 1px solid #e2e8f0; border-radius: 12px; background-color: #ffffff;">
  <div style="text-align: center; margin-bottom: 20px;">
    <h2 style="color: #047857; margin: 0; font-size: 24px;">OcaVenteDz</h2>
    <p style="color: #64748b; font-size: 14px; margin-top: 4px;">استعادة كلمة المرور</p>
  </div>
  
  <h3 style="color: #1e293b; font-size: 18px; margin-bottom: 8px;">رمز إعادة تعيين كلمة المرور</h3>
  <p style="color: #475569; font-size: 14px; line-height: 1.6;">
    أدخل الرمز التالي في التطبيق لتعيين كلمة مرور جديدة لحسابك:
  </p>
  
  <div style="background-color: #ecfdf5; border: 2px dashed #059669; border-radius: 10px; padding: 18px; text-align: center; margin: 24px 0;">
    <span style="font-size: 36px; font-weight: bold; letter-spacing: 12px; color: #047857; font-family: monospace;">{{ .Token }}</span>
  </div>
  
  <p style="color: #64748b; font-size: 13px;">
    🔒 إذا لم تطلب استعادة كلمة المرور، يرجى تجاهل هذه الرسالة.
  </p>
</div>
```
ثم اضغط **Save Changes**.

---

### النتيجة بعد التعديل:
بمجرد حفظ القالب في Supabase، لن تظهر رسالة الرابط `Your sign-in link (Follow the link below to sign in)`، بل ستصل رسالة عربية أنيقة تحتوي على رمز الـ OTP المكون من 6 أرقام بخط واضح، يدخله المستخدم في خانة رمز التحقق في التطبيق ويسجل الدخول فوراً!
