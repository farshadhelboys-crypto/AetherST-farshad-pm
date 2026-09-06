const json = (data, status = 200) => new Response(JSON.stringify(data), {
  status,
  headers: { "content-type": "application/json; charset=utf-8", "cache-control": "no-store" }
});

const now = () => Date.now();
const normalize = (v) => String(v || "").trim().toUpperCase();

function admin(request, env) {
  return request.headers.get("authorization") === `Bearer ${env.ADMIN_TOKEN}`;
}

export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    if (request.method === "OPTIONS") return new Response(null, { status: 204 });

    try {
      if (url.pathname === "/v1/status" && request.method === "GET") {
        const deviceId = String(url.searchParams.get("deviceId") || "").trim();
        if (!deviceId) return json({ ok:false, error:"device_id_required" }, 400);
        const row = await env.DB.prepare(
          "SELECT code, device_id, expires_at, active FROM licenses WHERE device_id = ? ORDER BY expires_at DESC LIMIT 1"
        ).bind(deviceId).first();
        if (!row || !row.active || Number(row.expires_at) <= now()) {
          return json({ ok:true, active:false, expiresAt:0, serverTime:now() });
        }
        return json({ ok:true, active:true, expiresAt:Number(row.expires_at), serverTime:now() });
      }

      if (url.pathname === "/v1/activate" && request.method === "POST") {
        const body = await request.json();
        const code = normalize(body.code);
        const deviceId = String(body.deviceId || "").trim();
        if (!code || !deviceId) return json({ ok:false, error:"code_and_device_required" }, 400);

        const row = await env.DB.prepare("SELECT * FROM licenses WHERE code = ?").bind(code).first();
        if (!row) return json({ ok:false, error:"code_not_found" }, 404);
        if (row.device_id && row.device_id !== deviceId) return json({ ok:false, error:"code_used_by_other_device" }, 409);
        if (!row.active) return json({ ok:false, error:"license_revoked" }, 403);

        const t = now();
        let expires = Number(row.expires_at || 0);
        const base = Math.max(expires, t);
        if (!row.device_id) expires = base + Number(row.duration_days) * 86400000;
        else if (expires <= t) expires = t + Number(row.duration_days) * 86400000;

        await env.DB.prepare(
          "UPDATE licenses SET device_id=?, expires_at=?, activated_at=COALESCE(activated_at,?), updated_at=? WHERE code=?"
        ).bind(deviceId, expires, t, t, code).run();
        return json({ ok:true, active:true, expiresAt:expires, serverTime:t });
      }

      if (url.pathname === "/v1/admin/create" && request.method === "POST") {
        if (!admin(request, env)) return json({ ok:false, error:"unauthorized" }, 401);
        const body = await request.json();
        const code = normalize(body.code);
        const days = Math.max(1, Number(body.durationDays || 30));
        if (!code) return json({ ok:false, error:"code_required" }, 400);
        const t = now();
        await env.DB.prepare(
          "INSERT INTO licenses(code,duration_days,expires_at,active,created_at,updated_at) VALUES(?,?,0,1,?,?)"
        ).bind(code, days, t, t).run();
        return json({ ok:true, code, durationDays:days });
      }

      if (url.pathname === "/v1/admin/update" && request.method === "POST") {
        if (!admin(request, env)) return json({ ok:false, error:"unauthorized" }, 401);
        const body = await request.json();
        const code = normalize(body.code);
        const fields = [];
        const args = [];
        if (body.active !== undefined) { fields.push("active=?"); args.push(body.active ? 1 : 0); }
        if (body.expiresAt !== undefined) { fields.push("expires_at=?"); args.push(Number(body.expiresAt)); }
        if (body.deviceId !== undefined) { fields.push("device_id=?"); args.push(body.deviceId ? String(body.deviceId) : null); }
        if (!fields.length || !code) return json({ ok:false, error:"nothing_to_update" }, 400);
        fields.push("updated_at=?"); args.push(now()); args.push(code);
        await env.DB.prepare(`UPDATE licenses SET ${fields.join(",")} WHERE code=?`).bind(...args).run();
        return json({ ok:true });
      }

      return json({ ok:false, error:"not_found" }, 404);
    } catch (e) {
      return json({ ok:false, error:String(e.message || e) }, 500);
    }
  }
};
