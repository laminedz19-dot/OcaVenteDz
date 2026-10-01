/**
 * سكريبت تعيين صلاحية المسؤول (admin: true) عبر Firebase Admin SDK
 *
 * طريقة الاستخدام:
 * 1. حمّل ملف serviceAccountKey.json من إعدادات مشروع فايربيس (Project Settings -> Service accounts -> Generate new private key).
 * 2. ضع الملف في نفس المجلد بجوار هذا السكريبت باسم serviceAccountKey.json.
 * 3. ضع الـ UID الخاص بالمشرف في المتغير adminUid بالأسفل.
 * 4. شغّل: npm install firebase-admin && node set-admin-claim.js
 */

const admin = require('firebase-admin');
const path = require('path');
const fs = require('fs');

const serviceAccountPath = path.join(__dirname, 'serviceAccountKey.json');

if (!fs.existsSync(serviceAccountPath)) {
  console.error("❌ ملف serviceAccountKey.json غير موجود في هذا المسار. يرجى تحميله من Firebase Console وضعه في نفس المجلد.");
  process.exit(1);
}

const serviceAccount = require(serviceAccountPath);

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

// أدخل الـ UID الخاص بحساب المشرف المسجل في Firebase Authentication
const adminUid = process.argv[2] || "ضع_هنا_UID_المسؤول";

if (!adminUid || adminUid === "ضع_هنا_UID_المسؤول") {
  console.error("❌ يرجى تمرير UID الخاص بالمشرف كمعامل للأمر، مثال: node set-admin-claim.js <ADMIN_UID>");
  process.exit(1);
}

admin.auth().setCustomUserClaims(adminUid, { admin: true })
  .then(() => {
    console.log(`✅ تم بنجاح تعيين صلاحية 'admin: true' للمستخدم ذو المعرف: ${adminUid}`);
    console.log("يمكنك الآن تسجيل الدخول من تطبيق الإدارة بهذا الحساب.");
    process.exit(0);
  })
  .catch((error) => {
    console.error("❌ حدث خطأ أثناء تعيين الصلاحية:", error);
    process.exit(1);
  });
