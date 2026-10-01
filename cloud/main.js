/*
 * Cloud Code الخاص بـ OcaVente DZ.
 * كل العمليات الحساسة تستخدم master key داخل الخادم فقط.
 * لا تضع مفاتيح Back4App أو أسراراً في هذا الملف.
 */

const ADMIN_ROLE_NAME = "Admin";
const SENSITIVE_USER_FIELDS = ["isVerified", "verificationRequested", "isBanned", "profileRole"];
const SENSITIVE_LISTING_FIELDS = ["status", "isPaid", "isFeatured", "isUrgent", "rejectionReason"];
const SENSITIVE_TOPUP_FIELDS = ["status", "adminNote", "reviewedAt", "amountDzd", "user"];
const SENSITIVE_WALLET_FIELDS = ["balanceDzd", "pendingBalanceDzd", "isActive"];
const SENSITIVE_ORDER_FIELDS = ["status", "isPaid", "trackingNumber", "statusNote", "deliveredAt", "cancelledAt"];

function requireUser(request) {
  if (!request.user) {
    throw new Error("يجب تسجيل الدخول لتنفيذ هذه العملية");
  }
  return request.user;
}

async function isAdmin(user) {
  if (!user) return false;
  const roleQuery = new Parse.Query(Parse.Role);
  roleQuery.equalTo("name", ADMIN_ROLE_NAME);
  const role = await roleQuery.first({useMasterKey: true});
  if (!role) return false;
  const users = role.relation("users");
  const userQuery = users.query();
  userQuery.equalTo("objectId", user.id);
  return (await userQuery.count({useMasterKey: true})) > 0;
}

async function requireAdmin(request) {
  const user = requireUser(request);
  if (!(request.master || await isAdmin(user))) {
    throw new Error("هذه العملية متاحة للمشرفين فقط");
  }
  return user;
}

function setAcl(acl, user, options) {
  const config = options || {};
  if (user && config.ownerRead !== false) acl.setReadAccess(user, true);
  if (user && config.ownerWrite === true) acl.setWriteAccess(user, true);
  acl.setRoleReadAccess(ADMIN_ROLE_NAME, true);
  acl.setRoleWriteAccess(ADMIN_ROLE_NAME, true);
  if (config.publicRead === true) acl.setPublicReadAccess(true);
  return acl;
}

function isSensitiveChange(request, fields) {
  return fields.some((field) => request.object.dirty(field));
}

async function assertSensitiveChangeAllowed(request, fields) {
  if (!isSensitiveChange(request, fields)) return;
  if (request.master || await isAdmin(request.user)) return;
  throw new Error("لا يمكن تعديل الحقول الحساسة من التطبيق مباشرة");
}

async function getUserById(userId) {
  if (!userId) throw new Error("معرف المستخدم مطلوب");
  const query = new Parse.Query(Parse.User);
  const user = await query.get(userId, {useMasterKey: true});
  return user;
}

async function getAdminRole() {
  const query = new Parse.Query(Parse.Role);
  query.equalTo("name", ADMIN_ROLE_NAME);
  let role = await query.first({useMasterKey: true});
  if (!role) {
    role = new Parse.Role(ADMIN_ROLE_NAME, new Parse.ACL());
    role.getACL().setPublicReadAccess(false);
    role.getACL().setPublicWriteAccess(false);
    await role.save(null, {useMasterKey: true});
  }
  return role;
}

async function applyDefaultAcl(request, options) {
  if (request.master || !request.object.isNew()) return;
  const owner = request.user;
  const acl = new Parse.ACL();
  setAcl(acl, owner, options);
  request.object.setACL(acl);
}

/* تهيئة ACL الافتراضي لكل جدول. */
Parse.Cloud.beforeSave("_User", async (request) => {
  await assertSensitiveChangeAllowed(request, SENSITIVE_USER_FIELDS);
  if (request.object.isNew() && !request.master) {
    request.object.set("profileRole", "USER");
    const acl = new Parse.ACL(request.object);
    acl.setReadAccess(request.object, true);
    acl.setWriteAccess(request.object, true);
    acl.setRoleReadAccess(ADMIN_ROLE_NAME, true);
    acl.setRoleWriteAccess(ADMIN_ROLE_NAME, true);
    request.object.setACL(acl);
  }
});

Parse.Cloud.beforeSave("Listing", async (request) => {
  await assertSensitiveChangeAllowed(request, SENSITIVE_LISTING_FIELDS);
  const status = request.object.get("status");
  await applyDefaultAcl(request, {ownerRead: true, ownerWrite: true, publicRead: status === "PUBLISHED"});
  if (status === "PUBLISHED") {
    const acl = request.object.getACL() || new Parse.ACL();
    acl.setPublicReadAccess(true);
    request.object.setACL(acl);
  }
});

Parse.Cloud.beforeSave("TopUpRequest", async (request) => {
  await assertSensitiveChangeAllowed(request, SENSITIVE_TOPUP_FIELDS);
  if (request.object.isNew() && !request.master) {
    const user = requireUser(request);
    const linkedUser = request.object.get("user");
    if (!linkedUser || linkedUser.id !== user.id) {
      throw new Error("طلب الشحن يجب أن يخص المستخدم الحالي");
    }
    if (request.object.get("status") !== "PENDING") {
      throw new Error("طلب الشحن الجديد يجب أن يكون في حالة انتظار");
    }
    await applyDefaultAcl(request, {ownerRead: true, ownerWrite: false});
  }
});

Parse.Cloud.beforeSave("Wallet", async (request) => {
  await assertSensitiveChangeAllowed(request, SENSITIVE_WALLET_FIELDS);
  await applyDefaultAcl(request, {ownerRead: true, ownerWrite: false});
});

Parse.Cloud.beforeSave("PaymentOrder", async (request) => {
  if (request.object.isNew()) await applyDefaultAcl(request, {ownerRead: true, ownerWrite: false});
  if (!request.object.isNew() && !request.master && !(await isAdmin(request.user))) {
    throw new Error("سجلات الدفع غير قابلة للتعديل");
  }
});

Parse.Cloud.beforeSave("Order", async (request) => {
  await assertSensitiveChangeAllowed(request, SENSITIVE_ORDER_FIELDS);
  await applyDefaultAcl(request, {ownerRead: true, ownerWrite: false});
});

Parse.Cloud.beforeSave("ChatMessage", async (request) => {
  if (!request.object.isNew()) return;
  const user = requireUser(request);
  const sender = request.object.get("sender");
  if (!sender || sender.id !== user.id) throw new Error("مرسل الرسالة غير مطابق للمستخدم الحالي");
  const acl = new Parse.ACL();
  acl.setReadAccess(user, true);
  acl.setWriteAccess(user, true);
  const receiver = request.object.get("receiver");
  if (receiver) acl.setReadAccess(receiver, true);
  acl.setRoleReadAccess(ADMIN_ROLE_NAME, true);
  acl.setRoleWriteAccess(ADMIN_ROLE_NAME, true);
  request.object.setACL(acl);
});

Parse.Cloud.beforeSave("Report", async (request) => {
  if (!request.object.isNew()) return;
  const user = requireUser(request);
  const reporter = request.object.get("reporter");
  if (!reporter || reporter.id !== user.id) throw new Error("مبلّغ البلاغ غير مطابق للمستخدم الحالي");
  await applyDefaultAcl(request, {ownerRead: true, ownerWrite: false});
});

Parse.Cloud.beforeSave("Favorite", async (request) => {
  if (!request.object.isNew()) return;
  const user = requireUser(request);
  const owner = request.object.get("user");
  if (!owner || owner.id !== user.id) throw new Error("المفضلة يجب أن تخص المستخدم الحالي");
  await applyDefaultAcl(request, {ownerRead: true, ownerWrite: true});
});

Parse.Cloud.beforeSave("WalletTransaction", async (request) => {
  if (!request.master && !(await isAdmin(request.user))) {
    throw new Error("معاملات المحفظة ينشئها الخادم فقط");
  }
  await applyDefaultAcl(request, {ownerRead: true, ownerWrite: false});
});

Parse.Cloud.beforeSave("AppNotification", async (request) => {
  if (!request.master && !(await isAdmin(request.user))) {
    throw new Error("الإشعارات ينشئها الخادم فقط");
  }
  await applyDefaultAcl(request, {ownerRead: true, ownerWrite: false});
});

Parse.Cloud.beforeSave("PlatformSettings", async (request) => {
  if (!request.master && !(await isAdmin(request.user))) {
    throw new Error("إعدادات المنصة متاحة للقراءة فقط للمستخدمين");
  }
  if (request.object.isNew()) {
    const acl = new Parse.ACL();
    acl.setRoleReadAccess(ADMIN_ROLE_NAME, true);
    acl.setRoleWriteAccess(ADMIN_ROLE_NAME, true);
    request.object.setACL(acl);
  }
});

/* اعتماد طلب شحن وزيادة الرصيد ومعاملة المحفظة في عملية خادمية واحدة. */
Parse.Cloud.define("approveTopUpRequest", async (request) => {
  await requireAdmin(request);
  const requestId = request.params.requestId;
  const note = String(request.params.adminNote || "");
  const topUpQuery = new Parse.Query("TopUpRequest");
  const topUp = await topUpQuery.get(requestId, {useMasterKey: true});
  if (topUp.get("status") !== "PENDING") throw new Error("طلب الشحن تمت معالجته مسبقاً");
  const user = topUp.get("user");
  if (!user) throw new Error("طلب الشحن لا يحتوي على مستخدم صالح");
  await user.fetch({useMasterKey: true});
  const amount = Number(topUp.get("amountDzd") || 0);
  if (!Number.isFinite(amount) || amount < 200) throw new Error("قيمة الشحن غير صالحة");

  const walletQuery = new Parse.Query("Wallet");
  walletQuery.equalTo("user", user);
  let wallet = await walletQuery.first({useMasterKey: true});
  if (!wallet) {
    wallet = new Parse.Object("Wallet");
    wallet.set("user", user);
    wallet.set("balanceDzd", 0);
    wallet.set("pendingBalanceDzd", 0);
    wallet.set("currency", "DZD");
    wallet.set("isActive", true);
  }
  wallet.increment("balanceDzd", amount);
  await wallet.save(null, {useMasterKey: true});

  const transaction = new Parse.Object("WalletTransaction");
  transaction.set("user", user);
  transaction.set("type", "TOPUP");
  transaction.set("amount", amount);
  transaction.set("description", "اعتماد طلب شحن المحفظة");
  transaction.set("referenceId", topUp.id);
  transaction.set("timestamp", new Date());
  await transaction.save(null, {useMasterKey: true});

  topUp.set("status", "APPROVED");
  topUp.set("adminNote", note);
  topUp.set("reviewedBy", request.user);
  topUp.set("reviewedAt", new Date());
  await topUp.save(null, {useMasterKey: true});
  return {success: true, requestId: topUp.id, newBalanceDzd: wallet.get("balanceDzd")};
});

Parse.Cloud.define("rejectTopUpRequest", async (request) => {
  await requireAdmin(request);
  const topUpQuery = new Parse.Query("TopUpRequest");
  const topUp = await topUpQuery.get(request.params.requestId, {useMasterKey: true});
  if (topUp.get("status") !== "PENDING") throw new Error("طلب الشحن تمت معالجته مسبقاً");
  topUp.set("status", "REJECTED");
  topUp.set("adminNote", String(request.params.adminNote || ""));
  topUp.set("reviewedBy", request.user);
  topUp.set("reviewedAt", new Date());
  await topUp.save(null, {useMasterKey: true});
  return {success: true, requestId: topUp.id};
});

Parse.Cloud.define("banUser", async (request) => {
  await requireAdmin(request);
  const target = await getUserById(request.params.userId);
  target.set("isBanned", Boolean(request.params.banned));
  await target.save(null, {useMasterKey: true});
  return {success: true, userId: target.id, banned: target.get("isBanned")};
});

Parse.Cloud.define("verifyUser", async (request) => {
  await requireAdmin(request);
  const target = await getUserById(request.params.userId);
  target.set("isVerified", Boolean(request.params.verified));
  target.set("verificationRequested", false);
  await target.save(null, {useMasterKey: true});
  return {success: true, userId: target.id, verified: target.get("isVerified")};
});

Parse.Cloud.define("sendNotification", async (request) => {
  await requireAdmin(request);
  const recipient = await getUserById(request.params.userId);
  const title = String(request.params.title || "OcaVente DZ");
  const body = String(request.params.body || "");
  const data = request.params.data || {};

  const notification = new Parse.Object("AppNotification");
  notification.set("recipient", recipient);
  notification.set("type", String(request.params.type || "GENERAL"));
  notification.set("title", title);
  notification.set("body", body);
  notification.set("data", data);
  notification.set("isRead", false);
  notification.set("sentAt", new Date());
  await notification.save(null, {useMasterKey: true});

  const installationQuery = new Parse.Query(Parse.Installation);
  installationQuery.equalTo("user", recipient);
  await Parse.Push.send({
    where: installationQuery,
    data: {alert: body, title: title, sound: "default", payload: data}
  }, {useMasterKey: true});
  return {success: true, notificationId: notification.id};
});

Parse.Cloud.define("setAdminRole", async (request) => {
  await requireAdmin(request);
  const target = await getUserById(request.params.userId);
  const role = await getAdminRole();
  role.relation("users").add(target);
  await role.save(null, {useMasterKey: true});
  target.set("profileRole", "ADMIN");
  await target.save(null, {useMasterKey: true});
  return {success: true, userId: target.id, role: ADMIN_ROLE_NAME};
});
