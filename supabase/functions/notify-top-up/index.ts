import { SignJWT, importPKCS8 } from "npm:jose@5.10.0";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type, x-webhook-secret",
};

type WebhookPayload = {
  type?: string;
  table?: string;
  record?: {
    id?: string;
    amount_dzd?: number;
    provider?: string;
    reference?: string;
    status?: string;
  };
};

async function getGoogleAccessToken(serviceAccount: Record<string, string>) {
  const now = Math.floor(Date.now() / 1000);
  const privateKey = await importPKCS8(serviceAccount.private_key, "RS256");
  const assertion = await new SignJWT({
    scope: "https://www.googleapis.com/auth/firebase.messaging",
  })
    .setProtectedHeader({ alg: "RS256", typ: "JWT" })
    .setIssuer(serviceAccount.client_email)
    .setSubject(serviceAccount.client_email)
    .setAudience("https://oauth2.googleapis.com/token")
    .setIssuedAt(now)
    .setExpirationTime(now + 3600)
    .sign(privateKey);

  const response = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "content-type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion,
    }),
  });
  if (!response.ok) throw new Error(`Google OAuth failed: ${response.status}`);
  return (await response.json()).access_token as string;
}

Deno.serve(async (request) => {
  if (request.method === "OPTIONS") return new Response("ok", { headers: corsHeaders });
  try {
    const webhookSecret = Deno.env.get("NOTIFY_WEBHOOK_SECRET");
    if (!webhookSecret || request.headers.get("x-webhook-secret") !== webhookSecret) {
      return new Response(JSON.stringify({ error: "unauthorized" }), { status: 401, headers: { ...corsHeaders, "content-type": "application/json" } });
    }
    const payload = (await request.json()) as WebhookPayload;
    const record = payload.record;
    if (payload.type !== "INSERT" || payload.table !== "top_up_requests" || !record?.id) {
      return new Response(JSON.stringify({ ignored: true }), { headers: { ...corsHeaders, "content-type": "application/json" } });
    }

    const supabaseUrl = Deno.env.get("SUPABASE_URL")!;
    const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
    const serviceAccount = JSON.parse(Deno.env.get("FIREBASE_SERVICE_ACCOUNT_JSON")!);
    const tokenResponse = await fetch(`${supabaseUrl}/rest/v1/admin_push_tokens?enabled=eq.true&select=token`, {
      headers: { apikey: serviceRoleKey, Authorization: `Bearer ${serviceRoleKey}` },
    });
    if (!tokenResponse.ok) throw new Error(`Token lookup failed: ${tokenResponse.status}`);
    const tokens = (await tokenResponse.json()).map((row: { token: string }) => row.token).filter(Boolean);
    if (!tokens.length) return new Response(JSON.stringify({ sent: 0 }), { headers: { ...corsHeaders, "content-type": "application/json" } });

    const accessToken = await getGoogleAccessToken(serviceAccount);
    const sendResults = await Promise.all(tokens.map(async (token) => {
      const fcmResponse = await fetch(`https://fcm.googleapis.com/v1/projects/${serviceAccount.project_id}/messages:send`, {
        method: "POST",
        headers: { Authorization: `Bearer ${accessToken}`, "content-type": "application/json" },
        body: JSON.stringify({ message: {
          token,
          notification: {
            title: "طلب شحن رصيد جديد",
            body: `تم استلام وصل شحن بقيمة ${record.amount_dzd ?? 0} دج للمراجعة`,
          },
          data: {
            type: "TOP_UP_REQUEST",
            request_id: record.id,
            amount_dzd: String(record.amount_dzd ?? 0),
            provider: record.provider ?? "",
          },
          android: { priority: "high" },
        } }),
      });
      return fcmResponse.ok;
    }));
    const sent = sendResults.filter(Boolean).length;
    if (sent === 0) throw new Error("FCM did not accept any admin device token");
    return new Response(JSON.stringify({ sent, total: tokens.length, request_id: record.id }), { headers: { ...corsHeaders, "content-type": "application/json" } });
  } catch (error) {
    console.error(error);
    return new Response(JSON.stringify({ error: error instanceof Error ? error.message : "notification_failed" }), { status: 500, headers: { ...corsHeaders, "content-type": "application/json" } });
  }
});
