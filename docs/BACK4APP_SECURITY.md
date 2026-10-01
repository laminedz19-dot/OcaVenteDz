# أمان Back4App وParse في OcaVente DZ

## مبدأ الأمان

تنتقل صلاحيات Firebase Rules وCustom Claims إلى ثلاث طبقات:

1. **Class Level Permissions (CLP):** الوضع الافتراضي مغلق للعامة، ولا يفتح إلا ما يحتاجه التطبيق.
2. **ACL:** كل سجل جديد يحدد المالك ودور `Admin` صراحة.
3. **Cloud Code:** كل عملية تغير الرصيد أو التحقق أو الحظر أو اعتماد الشحن تنفذ على الخادم باستخدام `useMasterKey`.

الملف التنفيذي هو `cloud/main.js`، ولا يحتوي على Application ID أو Client Key.

## دور Admin

- اسم الدور الثابت: `Admin`.
- عضوية الدور هي البديل عن Firebase Custom Claims مثل `{ "admin": true }`.
- لا يكفي وجود حقل `profileRole = ADMIN` في `_User` وحده لمنح الصلاحية.
- التحقق الفعلي يتم باستعلام علاقة المستخدمين في `Parse.Role`.
- إنشاء أول مدير وإضافته إلى الدور خطوة يدوية من لوحة Back4App أو باستخدام Master Key آمن خارج تطبيق الهاتف.

## مصفوفة CLP المقترحة

| الجدول | القراءة العامة | كتابة عامة | قراءة مستخدم مسجل | كتابة مستخدم | Admin |
|---|---:|---:|---:|---:|---:|
| `_User` | لا | لا | ACL فقط | ACL للملف الشخصي فقط | كل شيء عبر Cloud Code |
| `Listing` | لا | لا | ACL/الإعلانات المنشورة حسب سياسة التطبيق | المالك للحقول العادية | كل شيء |
| `PaymentOrder` | لا | لا | صاحب السجل فقط | إنشاء أولي فقط | كل شيء |
| `TopUpRequest` | لا | لا | صاحب الطلب فقط | إنشاء طلب PENDING فقط | قراءة وكتابة |
| `Wallet` | لا | لا | صاحب المحفظة قراءة فقط | لا | كل شيء |
| `WalletTransaction` | لا | لا | صاحب المحفظة قراءة فقط | لا | إنشاء من الخادم |
| `ChatMessage` | لا | لا | المرسل والمستقبل | المرسل عند الإنشاء | كل شيء |
| `Review` | لا | لا | حسب سياسة الإعلان/الأطراف | المشتري عند الإنشاء | كل شيء |
| `Report` | لا | لا | المبلّغ | إنشاء بلاغ فقط | كل شيء |
| `Favorite` | لا | لا | المالك | المالك | كل شيء |
| `PlatformSettings` | لا | لا، أو قراءة مصادق عليها | قراءة للمستخدم المسجل | لا | كل شيء |
| `Order` | لا | لا | المشتري والبائع | إنشاء أولي | الحالة عبر Cloud Code |
| `AppNotification` | لا | لا | المستلم فقط | لا | إنشاء وإرسال |

> عند الحاجة إلى تصفح الإعلانات قبل الدخول، يمكن فتح Public Read لـ`Listing` المنشور فقط عبر ACL عند نشر الإعلان، دون فتح الكتابة العامة.

## ACL الافتراضي

ينشئ `cloud/main.js` ACL حسب نوع السجل:

- السجل الشخصي: المستخدم يقرأ سجله، ودور `Admin` يقرأ ويكتب.
- المحفظة: المستخدم يقرأ فقط، ودور `Admin` يقرأ ويكتب.
- المحادثة: المرسل والمستقبل يقرآن، والمرسل ينشئ، والإدارة تملك وصولاً كاملاً.
- الإعلان: المالك يقرأ ويعدل، وعند `PUBLISHED` يضاف Public Read، والإدارة تملك وصولاً كاملاً.
- الإشعارات: المستلم يقرأ، والخادم/الإدارة ينشئان.

## العمليات الحساسة في Cloud Code

| الدالة | الغرض |
|---|---|
| `approveTopUpRequest` | التحقق من الطلب، زيادة الرصيد، إنشاء معاملة، ثم اعتماد الطلب |
| `rejectTopUpRequest` | رفض الطلب مع حفظ المدير والملاحظة والتاريخ |
| `banUser` | تعديل `isBanned` لمدير فقط |
| `verifyUser` | تعديل `isVerified` و`verificationRequested` لمدير فقط |
| `sendNotification` | إنشاء إشعار وإرساله إلى ParseInstallation من الخادم |
| `setAdminRole` | إضافة مستخدم إلى Role `Admin` بعد تحقق مدير موجود |

## الحقول المحمية

لا يسمح `beforeSave` بتعديل الحقول التالية من الهاتف مباشرة:

- `_User`: `isVerified`, `verificationRequested`, `isBanned`, `profileRole`.
- `Listing`: `status`, `isPaid`, `isFeatured`, `isUrgent`, `rejectionReason`.
- `TopUpRequest`: `status`, `adminNote`, `reviewedAt`, `amountDzd`, `user` بعد الإنشاء.
- `Wallet`: `balanceDzd`, `pendingBalanceDzd`, `isActive`.
- `Order`: `status`, `isPaid`, `trackingNumber`, `statusNote`, `deliveredAt`, `cancelledAt`.

## إعدادات لوحة Back4App المطلوبة

1. إنشاء/مراجعة CLP لكل جدول بحسب المصفوفة أعلاه، مع إغلاق Public Find/Get/Create/Update/Delete افتراضياً.
2. إنشاء Role باسم `Admin` مع ACL مغلق للعامة.
3. إضافة أول حساب إداري إلى Role `Admin` يدوياً.
4. نشر `cloud/main.js` في Cloud Code.
5. تفعيل Live Query لاحقاً لجدول `ChatMessage` فقط.
6. ضبط Push/Installations بعد إكمال المرحلة السابعة.
7. عدم منح Client Key صلاحيات Master؛ Master Key لا يوضع في التطبيق أو Git.
8. مراجعة صلاحيات الملفات، خصوصاً `receiptFile`، قبل نقلها إلى ParseFile.

## حدود التنفيذ في هذه المرحلة

- لم يتم بعد استبدال مستودعات Firebase في Kotlin؛ ذلك يبدأ في مراحل SDK والمصادقة وطبقة البيانات.
- Cloud Code يعتمد على مخطط المرحلة الأولى، لذلك يجب نشر الجداول والحقول أو تفعيل إنشاء الجداول عند أول حفظ قبل اختبار الدوال.
- `Parse.Push.send` يتطلب أن تكون `ParseInstallation` مرتبطة بحساب `_User` في مرحلة الإشعارات.
- صلاحيات لوحة Back4App والنشر الفعلي لـCloud Code خطوات خارجية لا يمكن تنفيذها من مستودع GitHub وحده دون بيانات دخول Back4App.
