# دليل تشغيل نظام شحن الرصيد وقواعد الحماية السحابية (OcaVenteDz)

تم إصلاح وهيكلة نظام شحن الرصيد المالي بالكامل ليعمل بصورة متزامنة وسحابية آمنة بين **تطبيق العميل (`:app`)** و**تطبيق الإدارة (`:admin`)** عبر **Firebase Firestore** و**Firebase Storage** و**Firebase Authentication**.

---

## 1. ما تم إنجازه في الكود البرمجي

### أ. تطبيق العميل (`:app`)
1. **المصادقة الحقيقية:** تم إلغاء الاعتماد على الهويات الوهمية (`user_me`) في عمليات الشحن، والاعتماد الحصري على `FirebaseAuth.currentUser.uid`.
2. **التحقق من صحة الطلب:**
   - الحد الأدنى للمبلغ: **200 دج**.
   - مزودو الدفع المسموح بهم: `BARIDIMOB`, `CCP`, `EDAHABIA`, `CIB`.
   - إلزامية إرفاق صورة الوصل أو كتابة رقم المرجع (أو كلاهما).
3. **التدفق الآمن لرفع الوصل وتسجيل الطلب:**
   - توليد `requestId` أولاً.
   - رفع الصورة إلى المسار المحمي: `topUpReceipts/{uid}/{requestId}/receipt.{ext}` في Firebase Storage.
   - تخزين مسار التخزين الداخلي (Storage Path) وليس رابطاً عاماً (No Public URLs).
   - تسجيل الطلب في مجموعة `topUpRequests/{requestId}` في Firestore بالحالة `PENDING`.
   - **آلية التراجع (Rollback):** في حال تعثر الحفظ في Firestore، يتم تلقائياً حذف صورة الوصل من Storage وإرجاع رسالة خطأ حقيقية للمستخدم دون اعتبارها نجاحاً.
   - تحديث قاعدة بيانات Room المحلية كـ Cache فقط بعد نجاح Firestore.
4. **المزامنة في الوقت الفعلي وحماية الرصيد:**
   - الاستماع الفوري لطلبات المستخدم: `whereEqualTo("userId", uid).orderBy("createdAt", DESCENDING)`.
   - إظهار حالة الخطأ (Error State) بوضوح للمستخدم عند تعثر المزامنة السحابية بدلاً من عرض قائمة فارغة مضللة.
   - تطبيق شحن الرصيد محلياً للمستخدم بطريقة أحادية قطعية (**Idempotent Credit**)؛ حيث يُشحن الرصيد مرة واحدة فقط فور تلقي حالة `APPROVED` دون أي تكرار.

---

### ب. تطبيق الإدارة (`:admin`)
1. **المصادقة الصارمة للمشرفين:**
   - إزالة التحقق المحلي بـ PIN وكلمات المرور المدمجة في الكود.
   - تسجيل الدخول بواسطة البريد الإلكتروني وكلمة المرور عبر Firebase Authentication.
   - تنفيذ `getIdToken(true)` لإجبار تجديد الرمز وقراءة الـ Custom Claims.
   - التحقق من وجود الصلاحية: `claims["admin"] == true`. وفي حال غيابها يتم تسجيل الخروج الفوري `signOut()` وعرض رسالة رفض الدخول لعدم امتلاك صلاحية المسؤول.
2. **شاشة مراجعة الطلبات المباشرة:**
   - الاستماع المباشر لمجموعة `topUpRequests` عبر Firestore Snapshot Listener مع ترتيب تنازلي حسب التاريخ.
   - توفير فلاتر سريعة: قيد المراجعة (`PENDING`)، المقبولة (`APPROVED`)، المرفوضة (`REJECTED`)، والكل.
   - عرض كامل تفاصيل الطلب: اسم العميل، الهاتف، المبلغ، وسيلة الدفع، المرجع، التاريخ، وملاحظات المشرف.
   - معالجة حالات: التحميل (`Loading`)، القائمة الفارغة (`Empty`)، وحالات الخطأ أو حظر الصلاحيات (`Error / Permission Denied`).
3. **معاينة صور الوصولات المحمية:**
   - جلب الرابط الآمن المؤقت لصورة الوصل عبر Firebase Storage Reference للمشرف المصرح له فقط.
4. **المعالجة الذرية للقبول أو الرفض:**
   - تنفيذ العملية داخل **Firestore Transaction** لضمان عدم إمكانية تعديل أي طلب إذا لم تكن حالته الحالية `PENDING`.
   - منع التعديل على الطلبات المقبولة أو المرفوضة نهائياً.
   - منع الضغط المزدوج (Double-Click Prevention) وتعطيل الأزرار أثناء معالجة الطلب في الشبكة.

---

### ج. قواعد الأمان السحابية
- **`firestore.rules`:**
  - قراءة طلبات الشحن: العميل يقرأ طلباته الخاصة فقط (`resource.data.userId == request.auth.uid`)، والمشرف (`request.auth.token.admin == true`) يقرأ جميع الطلبات.
  - إنشاء الطلب: للمستخدم المصادق عليه فقط، لطلبه الخاص، بالحالة `PENDING`، مع المبالغ الصحيحة والمزودين المعتمدين، ودون ملاحظات مشرف.
  - تحديث الطلب: محصور حصراً بالمشرف صاحب صلاحية `admin: true`، ولا يُسمح إلا بنقل الحالة من `PENDING` إلى `APPROVED` أو `REJECTED` وتحديث `adminNote` و`reviewedAt` فقط دون المساس ببيانات المستخدم أو المبلغ.
  - الحذف: ممنوع نهائياً على الجميع.
- **`storage.rules`:**
  - المسار: `topUpReceipts/{uid}/{requestId}/{fileName}`.
  - القراءة: مقتصرة على صاحب الوصل نفسه أو المشرف الذي يحمل `admin == true`.
  - الرفع: مقتصر على صاحب الحساب بحد أقصى 10 ميغابايت للملفات من نوع صور فقط.
  - منع أي وصول عام (Public Access).

---

## 2. ما تبقى على صاحب المشروع (مهم جداً للتشغيل)

لكي تكتمل الدورة بنجاح في بيئة الإنتاج الخاصة بك على Firebase، يتبقى عليك تنفيذ الخطوات التالية:

### الخطوة 1: نشر قواعد الأمان (Firestore & Storage Rules)
قم بتشغيل الأوامر التالية من سطر الأوامر بواسطة Firebase CLI:
```bash
firebase deploy --only firestore:rules,storage
```
أو انسخ محتويات الملفين `firestore.rules` و `storage.rules` وضعهما في لوحة تحكم Firebase Console في تبويبي **Rules** لكل من Firestore و Storage.

---

### الخطوة 2: إنشاء حساب المشرف وتعيين صلاحية `admin: true`
1. أنشئ حساباً للمشرف في Firebase Authentication (Email/Password) عبر Firebase Console، على سبيل المثال: `admin@ocaventedz.dz`.
2. احصل على الـ UID الخاص بهذا الحساب من تبويب Authentication.
3. لتعيين الـ Custom Claim (`admin: true`)، استخدم سكريبت Node.js التالي عبر **Firebase Admin SDK** (تجنب وضع المفتاح السري داخل تطبيق الأندرويد):

#### سكريبت تعيين الصلاحية (`set-admin.js`):
```javascript
const admin = require('firebase-admin');
const serviceAccount = require('./serviceAccountKey.json'); // ملف المفتاح السري الذي تحمّله من إعدادات مشروع فايربيس

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

// ضع هنا UID الخاص بحساب المشرف
const adminUid = "ضع_هنا_UID_المسؤول";

admin.auth().setCustomUserClaims(adminUid, { admin: true })
  .then(() => {
    console.log(`✅ تم بنجاح منح صلاحية admin: true للحساب: ${adminUid}`);
    process.exit(0);
  })
  .catch((error) => {
    console.error('❌ خطأ أثناء تعيين الصلاحية:', error);
    process.exit(1);
  });
```

قم بتشغيل السكريبت مرة واحدة على جهازك:
```bash
node set-admin.js
```
بمجرد تنفيذ هذا السكريبت، سيتمكن حساب المشرف من تسجيل الدخول إلى تطبيق الإدارة والوصول الفوري إلى كافة الطلبات والوصولات.

---

### الخطوة 3: فهرس Firestore (Composite Index) - اختياري / تلقائي
في حال طلب Firestore إنشاء فهرس مركب للاستعلام:
`collection("topUpRequests").whereEqualTo("userId", uid).orderBy("createdAt", DESCENDING)`
ستظهر لك رسالة خطأ في Logcat تحتوي على رابط مباشر لإنشاء الفهرس بنقرة زر واحدة في Firebase Console.
