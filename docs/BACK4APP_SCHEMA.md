# مخطط ترحيل OcaVente DZ من Back4App إلى Back4App

## نطاق المرحلة الأولى

هذا المستند هو **مخطط الترحيل** وليس تنفيذ الترحيل نفسه. تمت قراءة:

- `Back4AppService.kt` و`Back4AppModels.kt`.
- كيانات Room وواجهاتها DAO.
- `MarketplaceRepository` وعميل Back4App REST الحالي.
- `Back4App.rules` و`storage.rules`.
- جداول الإدارة المحفوظة مسبقاً في Back4App.

ملف المخطط الآلي المقابل هو [`docs/schema.json`](schema.json).

## ملاحظات أساسية

1. مجموعات Back4App الحالية هي: `users`, `listings`, `payments`, `topUpRequests`, `wallets`, `settings`, و`orders`.
2. لا توجد مجموعات فرعية Back4App مستخدمة في الكود الحالي؛ كل البيانات على مستوى مجموعات مستقلة.
3. توجد كيانات Room محلية لا تملك مجموعة Back4App صريحة حالياً: المحادثات، التقييمات، البلاغات، المفضلة، معاملات المحفظة. صُممت لها جداول Parse مستقلة حتى لا تضيع عند إزالة Room كمصدر سحابي.
4. `Room` يبقى كاشاً محلياً. كل `applicationId` يملك قاعدة محلية منفصلة.
5. سيتم تحويل هوية المستخدم إلى Parse `_User` في مراحل المصادقة والبيانات. عميل Back4App REST الحالي يستخدم `AppUser` بشكل انتقالي؛ يجب عدم إنشاء `AppUser` جديد في الترحيل النهائي.
6. الجداول `B4aSetting`, `B4aMenuItem`, و`B4aCustomField` محفوظة كما هي ولا تدخل في عملية الحذف أو إعادة التسمية.

## مخطط الجداول

| المصدر | جدول Parse المقترح | العلاقة الأساسية | الملاحظات |
|---|---|---|---|
| `users` | `_User` | هوية Parse الأساسية | الحقول الإدارية لا يغيرها العميل |
| `listings` | `Listing` | `owner -> _User` | الإعلان مرتبط بصاحبه |
| `payments` | `PaymentOrder` | `user -> _User`, `listing -> Listing` | سجل تدقيق؛ لا يعدّل العميل بعد الإنشاء |
| `topUpRequests` | `TopUpRequest` | `user -> _User`, `reviewedBy -> _User` | إيصال ParseFile، والاعتماد عبر Cloud Code |
| `wallets` | `Wallet` | `user -> _User` | رصيد حساس |
| Room `wallet_transactions` | `WalletTransaction` | `user -> _User` | سجل غير قابل للتلاعب من العميل |
| Room `chat_messages` | `ChatMessage` | `listing`, `sender`, `receiver` | يستخدم Parse Live Query فقط في المرحلة السابعة/الخامسة |
| Room `reviews` | `Review` | `seller`, `buyer`, `listing` | منع التقييم المكرر عبر Cloud Code/unique logic |
| Room `reports` | `Report` | `reporter`, `reportedUser`, `reportedListing` | تغيير الحالة للإدارة فقط |
| Room `favorites` | `Favorite` | `user`, `listing` | فهرس مركب منطقي `user + listing` |
| `settings/global` | `PlatformSettings` | سجل global واحد | الكتابة للإدارة فقط |
| `orders` | `Order` | `buyer`, `seller`, `listing` | حالات الطلب الحساسة تمر عبر Cloud Code |
| FCM المستقبلي | `AppNotification` | `recipient -> _User` | سجل اختياري للإشعارات داخل التطبيق |

## استراتيجية المعرفات والـ Pointers

Parse ينشئ `objectId` خاصاً به. للحفاظ على التوافق مع Room والبيانات القديمة:

- يضاف `legacyId` أو `legacyUserId` إلى السجل الجديد.
- يبقى المعرف القديم قابلاً للبحث أثناء الترحيل فقط.
- بعد إنشاء جميع السجلات، يبني سكربت النقل الـ Pointers من جدول المعرفات القديمة إلى `objectId` الجديد.
- لا يتم استخدام رقم الهاتف أو الاسم كمفتاح ربط.
- تواريخ Back4App Timestamp وmilliseconds تتحول إلى Parse Date.
- روابط الصور القديمة تبقى مؤقتاً في حقول `*Legacy` حتى تنجح عملية نقلها إلى ParseFile.

## مطابقة الحقول الرئيسية

### users إلى `_User`

| Back4App/Room | Parse |
|---|---|
| `id` | `legacyUserId` |
| `phone` | `phone` |
| `email` | `emailAddress`، بينما بريد Parse الأساسي يبقى في `email` عند تطبيق المرحلة الرابعة |
| `name` | `name` |
| `avatarUrl` | `avatarUrl` ثم `avatarFile` في المرحلة السادسة |
| `wilaya`, `commune`, `bio` | نفس الأسماء |
| `sellerRating`, `reviewsCount`, `adsCount` | نفس الأسماء |
| `isVerified`, `verificationRequested`, `isBanned` | نفس الأسماء، محمية من العميل |
| `role` | `profileRole`، والصلاحية الفعلية تعتمد على Parse Role باسم `Admin` |
| `createdAt` | Parse built-in `createdAt` |

### listings إلى `Listing`

| Back4App/Room | Parse |
|---|---|
| `id` | `legacyId` |
| `userId` | `owner` Pointer و`legacyUserId` أثناء الترحيل |
| الحقول النصية والوصفية | نفس الاسم والنوع بعد تحويل `Long/Int` إلى Number |
| `images` أو `imagesJson` | `images` Array<ParseFile>، مع `imageUrlsLegacy` مؤقتاً |
| `createdAt` | Parse built-in `createdAt` |
| `expiresAt` | Date |
| `status` | String مع قائمة حالات محكومة في Cloud Code |

### المالية والطلبات

- `wallets/{userId}` تصبح `Wallet` مع Pointer إلى `_User`، ولا يسمح ACL للعميل بتعديل `balanceDzd`.
- `topUpRequests` تصبح `TopUpRequest`، والإنشاء يكون `PENDING` فقط، بينما الاعتماد والرفض عبر Cloud Code.
- `payments` تصبح `PaymentOrder`، ويحتفظ بها كسجل تدقيق.
- `orders` تصبح `Order` مع Pointers للعميل والبائع والإعلان.

## الفهارس المطلوبة

يجب إنشاء الفهارس التالية بعد إنشاء الجداول:

- `Listing`: `status + createdAt`, `owner + createdAt`, `wilayaCode + status + createdAt`, `categoryId + status + createdAt`.
- `TopUpRequest`: `user + status + createdAt`, `status + createdAt`.
- `ChatMessage`: `listing + sentAt`, `sender + receiver + sentAt`.
- `Order`: `buyer + createdAt`, `seller + createdAt`, `status + createdAt`.
- `Favorite`: `user + listing`.
- `WalletTransaction`: `user + timestamp`.

## ما لم يمكن مطابقته حرفياً

- Back4App Rules وCustom Claims ليست كياناً قابلاً للنسخ إلى Parse؛ ستتحول إلى CLP وACL وParse Role وCloud Code في المرحلة الثانية.
- Back4App Storage Rules لا تنتقل إلى ParseFile تلقائياً؛ ستعاد صياغتها في beforeSave/afterDelete وCloud Code في المرحلة السادسة.
- Snapshot listeners الخاصة بالرسائل لا تعادل Back4App حرفياً؛ سيستخدم Parse Live Query للمحادثات فقط، بينما بقية البيانات باستعلامات عادية مع Cache Room.
- `B4aSetting`, `B4aMenuItem`, و`B4aCustomField` جداول إدارة موجودة مسبقاً، ولذلك لا يغيرها هذا المخطط.

## الحالة

- [x] استخراج مجموعات Back4App.
- [x] استخراج كيانات Room ذات الصلة.
- [x] تحديد العلاقات والـ Pointers.
- [x] تحديد الفهارس المقترحة.
- [x] إنشاء `docs/schema.json`.
- [ ] تطبيق المخطط في لوحة Back4App؛ هذه خطوة يدوية أو ستنفذها أدوات المرحلة الثالثة/الثامنة.
- [ ] نقل البيانات؛ المرحلة الثامنة.
