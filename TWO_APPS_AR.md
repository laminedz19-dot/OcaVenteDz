# فصل OcaVente DZ إلى تطبيقين

تم تقسيم المشروع إلى وحدتين مستقلتين:

| الوحدة | `applicationId` | الوظيفة | APK |
|---|---|---|---|
| `:app` | `OcaVenteDz.lamine` | تطبيق المستخدمين | `app-debug.apk` |
| `:admin` | `OcaVenteDz.Admin` | تطبيق الإدارة | `admin-debug.apk` |

كل تطبيق يحتوي على Activity تشغيل واحدة فقط. لا يظهر تطبيق الإدارة داخل APK المستخدم.

## قاعدة البيانات المشتركة

التطبيقان يستخدمان **مشروع Firebase نفسه** وCollections نفسها في Firestore.

أما Room فهي قاعدة محلية منفصلة لكل تطبيق بسبب اختلاف `applicationId`؛ لا يمكن لتطبيقين منفصلين مشاركة ملف Room المحلي مباشرة. لذلك يجب اعتبار Firestore مصدر الحقيقة المشترك.

## إعداد Firebase للإدارة

1. في Firebase Console افتح مشروع `ocaventedz`.
2. أضف Android App جديداً بالحزمة:

   ```text
   OcaVenteDz.Admin
   ```

3. نزّل `google-services.json` الخاص بالتطبيق الإداري وضعه في:

   ```text
   admin/google-services.json
   ```

4. اترك `app/google-services.json` لتطبيق المستخدم كما هو.
5. فعّل Firebase Authentication للتطبيقين، وأنشئ حساب المشرف وأضف له Custom Claim:

   ```json
   { "admin": true }
   ```

6. لا تعتمد على PIN محلي لحماية الإدارة؛ قواعد Firestore هي الحماية الفعلية.

## البناء

```bash
gradle :app:assembleDebug
# الناتج: app/build/outputs/apk/debug/app-debug.apk

gradle :admin:assembleDebug
# الناتج: admin/build/outputs/apk/debug/admin-debug.apk
```

إذا تم إنشاء Gradle Wrapper:

```bash
./gradlew :app:assembleDebug
./gradlew :admin:assembleDebug
```

## ملاحظة مهمة

نسخة `admin/google-services.json` الحالية قالب مؤقت لتطابق الحزمة أثناء البناء. يجب استبدالها بملف JSON الذي ينزّل من Firebase بعد تسجيل التطبيق الإداري فعلياً. لا تستخدم النسخة الحالية للإصدار الإنتاجي؛ قد لا يعمل Firebase Authentication أو الخدمات المرتبطة بالتطبيق الإداري حتى يتم تسجيل `OcaVenteDz.Admin` في Firebase.

## البناء بدون Android Studio عبر GitHub

يوجد Workflow باسم `Build OcaVente DZ APKs` في:

```text
.github/workflows/build-two-apks.yml
```

لتشغيله:

1. ارفع التغييرات إلى GitHub.
2. افتح تبويب **Actions**.
3. اختر **Build OcaVente DZ APKs**.
4. اضغط **Run workflow**.
5. حمّل artifact باسم `ocavente-dz-apks`.

سيحتوي الملف المضغوط على APK المستخدم وAPK الإدارة.
