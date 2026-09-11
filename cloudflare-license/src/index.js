const json = (data, status = 200) => new Response(JSON.stringify(data), {
  status,
  headers: {
    "content-type": "application/json; charset=utf-8",
    "cache-control": "no-store",
    "access-control-allow-origin": "*",
    "access-control-allow-headers": "authorization, content-type",
    "access-control-allow-methods": "GET, POST, OPTIONS"
  }
});

const now = () => Date.now();
const normalize = (v) => String(v || "").trim().toUpperCase();
const GB = 1024 * 1024 * 1024;

function admin(request, env) {
  const auth = request.headers.get("authorization") || "";
  return auth === `Bearer ${env.ADMIN_TOKEN}`;
}

/** محاسبه فعال بودن بر اساس زمان و حجم */
function computeActive(row, t = now()) {
  if (!row || !row.active) return false;
  const type = row.license_type || "time";
  const expiresOk = type === "volume" ? true : Number(row.expires_at) > t;
  const volumeGb = Number(row.volume_gb || 0);
  const usedBytes = Number(row.used_bytes || 0);
  const volumeOk = type === "time" ? true : (volumeGb <= 0 || usedBytes < volumeGb * GB);
  return expiresOk && volumeOk;
}

function statusPayload(row, t = now()) {
  const volumeGb = Number(row?.volume_gb || 0);
  const usedBytes = Number(row?.used_bytes || 0);
  const remainingBytes = Math.max(0, volumeGb * GB - usedBytes);
  const active = computeActive(row, t);
  return {
    ok: true,
    active,
    expiresAt: Number(row?.expires_at || 0),
    serverTime: t,
    licenseType: row?.license_type || "time",
    volumeGb,
    usedBytes,
    remainingBytes,
    remainingGb: volumeGb > 0 ? remainingBytes / GB : null
  };
}

export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    if (request.method === "OPTIONS") {
      return new Response(null, {
        status: 204,
        headers: {
          "access-control-allow-origin": "*",
          "access-control-allow-headers": "authorization, content-type",
          "access-control-allow-methods": "GET, POST, OPTIONS"
        }
      });
    }

    try {
      // ============================================
      // ۱. وضعیت لایسنس (کلاینت)
      // ============================================
      if (url.pathname === "/v1/status" && request.method === "GET") {
        const deviceId = String(url.searchParams.get("deviceId") || "").trim();
        if (!deviceId) return json({ ok: false, error: "device_id_required" }, 400);

        const row = await env.DB.prepare(
          `SELECT code, device_id, license_type, duration_days, volume_gb, used_bytes,
                  expires_at, active FROM licenses
           WHERE device_id = ? ORDER BY activated_at DESC, created_at DESC LIMIT 1`
        ).bind(deviceId).first();

        if (!row) {
          return json({
            ok: true, active: false, expiresAt: 0, serverTime: now(),
            licenseType: "none", volumeGb: 0, usedBytes: 0, remainingBytes: 0, remainingGb: 0
          });
        }
        return json(statusPayload(row));
      }

      // ============================================
      // ۲. فعال‌سازی لایسنس
      // ============================================
      if (url.pathname === "/v1/activate" && request.method === "POST") {
        const body = await request.json();
        const code = normalize(body.code);
        const deviceId = String(body.deviceId || "").trim();
        if (!code || !deviceId) return json({ ok: false, error: "code_and_device_required" }, 400);

        const row = await env.DB.prepare("SELECT * FROM licenses WHERE code = ?").bind(code).first();
        if (!row) return json({ ok: false, error: "code_not_found" }, 404);
        if (row.device_id && row.device_id !== deviceId) {
          return json({ ok: false, error: "code_used_by_other_device" }, 409);
        }
        if (!row.active) return json({ ok: false, error: "license_revoked" }, 403);

        const t = now();
        let expires = Number(row.expires_at || 0);
        const type = row.license_type || "time";
        const days = Number(row.duration_days || 0);

        // فقط برای لایسنس زمانی یا ترکیبی زمان را تنظیم کن
        if (type === "time" || type === "both") {
          if (!row.device_id) {
            expires = t + days * 86400000;
          } else if (expires <= t) {
            expires = t + days * 86400000;
          }
        } else {
          // volume-only: expires_at را خیلی دور بگذار تا فقط حجم مهم باشد
          if (!row.device_id || expires <= 0) expires = t + 3650 * 86400000; // ~10 سال
        }

        await env.DB.prepare(
          `UPDATE licenses SET device_id=?, expires_at=?,
           activated_at=COALESCE(activated_at,?), updated_at=? WHERE code=?`
        ).bind(deviceId, expires, t, t, code).run();

        const updated = await env.DB.prepare("SELECT * FROM licenses WHERE code = ?").bind(code).first();
        return json(statusPayload(updated, t));
      }

      // ============================================
      // ۳. گزارش مصرف حجم (فقط دانلود) — کلاینت
      // ============================================
      if (url.pathname === "/v1/report-usage" && request.method === "POST") {
        const body = await request.json();
        const deviceId = String(body.deviceId || "").trim();
        // deltaBytes = بایت‌های دانلود شده از آخرین گزارش
        const deltaBytes = Math.max(0, Math.floor(Number(body.deltaBytes || body.downloadedBytes || 0)));
        if (!deviceId) return json({ ok: false, error: "device_id_required" }, 400);
        if (deltaBytes <= 0) {
          // فقط وضعیت فعلی را برگردان
          const row = await env.DB.prepare(
            `SELECT * FROM licenses WHERE device_id = ? ORDER BY activated_at DESC, created_at DESC LIMIT 1`
          ).bind(deviceId).first();
          if (!row) return json({ ok: true, active: false, usedBytes: 0, remainingBytes: 0 });
          return json(statusPayload(row));
        }

        const row = await env.DB.prepare(
          `SELECT * FROM licenses WHERE device_id = ? ORDER BY activated_at DESC, created_at DESC LIMIT 1`
        ).bind(deviceId).first();
        if (!row) return json({ ok: false, error: "no_license" }, 404);
        if (!row.active) return json({ ok: false, error: "license_revoked" }, 403);

        const type = row.license_type || "time";
        // لایسنس فقط زمانی — مصرف حجم ثبت نمی‌شود
        if (type === "time") {
          return json(statusPayload(row));
        }

        const newUsed = Number(row.used_bytes || 0) + deltaBytes;
        await env.DB.prepare(
          "UPDATE licenses SET used_bytes=?, updated_at=? WHERE code=?"
        ).bind(newUsed, now(), row.code).run();

        const updated = { ...row, used_bytes: newUsed };
        const payload = statusPayload(updated);
        // اگر حجم تمام شد، active=false برمی‌گردد
        return json(payload);
      }

      // ============================================
      // ۴. ساخت لایسنس (ادمین)
      // ============================================
      if (
        (url.pathname === "/v1/admin/create" || url.pathname === "/v1/create-license") &&
        request.method === "POST"
      ) {
        // پشتیبانی از Bearer و همچنین adminSecret در body (سازگاری با پنل قدیمی)
        let isAdmin = admin(request, env);
        const body = await request.json();
        if (!isAdmin && body.adminSecret && body.adminSecret === env.ADMIN_TOKEN) {
          isAdmin = true;
        }
        if (!isAdmin) return json({ ok: false, error: "unauthorized" }, 401);

        const code = normalize(body.code || body.licenseKey);
        if (!code) return json({ ok: false, error: "code_required" }, 400);

        let licenseType = String(body.licenseType || body.type || "time").toLowerCase();
        if (!["time", "volume", "both"].includes(licenseType)) licenseType = "time";

        const days = Math.max(0, Number(body.durationDays || body.expiresInDays || 0));
        const volumeGb = Math.max(0, Number(body.volumeGb || body.volume_gb || 0));

        if (licenseType === "time" && days < 1) {
          return json({ ok: false, error: "duration_days_required" }, 400);
        }
        if (licenseType === "volume" && volumeGb <= 0) {
          return json({ ok: false, error: "volume_gb_required" }, 400);
        }
        if (licenseType === "both" && (days < 1 || volumeGb <= 0)) {
          return json({ ok: false, error: "both_duration_and_volume_required" }, 400);
        }

        const existing = await env.DB.prepare("SELECT code FROM licenses WHERE code = ?").bind(code).first();
        if (existing) return json({ ok: false, error: "code_already_exists" }, 400);

        const t = now();
        await env.DB.prepare(
          `INSERT INTO licenses(code, license_type, duration_days, volume_gb, used_bytes,
            expires_at, active, created_at, updated_at)
           VALUES(?,?,?,?,0,0,1,?,?)`
        ).bind(code, licenseType, days || 30, volumeGb, t, t).run();

        return json({
          ok: true,
          code,
          licenseType,
          durationDays: days || 30,
          volumeGb
        });
      }

      // ============================================
      // ۵. بروزرسانی لایسنس (ادمین)
      // ============================================
      if (url.pathname === "/v1/admin/update" && request.method === "POST") {
        if (!admin(request, env)) return json({ ok: false, error: "unauthorized" }, 401);
        const body = await request.json();
        const code = normalize(body.code);
        if (!code) return json({ ok: false, error: "code_required" }, 400);

        const fields = [];
        const args = [];

        if (body.active !== undefined) {
          fields.push("active=?");
          args.push(body.active ? 1 : 0);
        }
        if (body.expiresAt !== undefined) {
          fields.push("expires_at=?");
          args.push(Number(body.expiresAt));
        }
        if (body.deviceId !== undefined) {
          fields.push("device_id=?");
          args.push(body.deviceId ? String(body.deviceId) : null);
        }
        if (body.volumeGb !== undefined) {
          fields.push("volume_gb=?");
          args.push(Math.max(0, Number(body.volumeGb)));
        }
        if (body.usedBytes !== undefined) {
          fields.push("used_bytes=?");
          args.push(Math.max(0, Math.floor(Number(body.usedBytes))));
        }
        if (body.resetUsage === true) {
          fields.push("used_bytes=?");
          args.push(0);
        }
        if (body.licenseType !== undefined) {
          const lt = String(body.licenseType).toLowerCase();
          if (["time", "volume", "both"].includes(lt)) {
            fields.push("license_type=?");
            args.push(lt);
          }
        }
        if (body.durationDays !== undefined && body.durationDays > 0) {
          const row = await env.DB.prepare(
            "SELECT expires_at, duration_days FROM licenses WHERE code = ?"
          ).bind(code).first();
          if (row) {
            const newExpires =
              Math.max(Number(row.expires_at || 0), now()) +
              Number(body.durationDays) * 86400000;
            fields.push("expires_at=?");
            args.push(newExpires);
            fields.push("duration_days=?");
            args.push(Number(row.duration_days) + Number(body.durationDays));
          }
        }
        // افزودن حجم اضافی
        if (body.addVolumeGb !== undefined && Number(body.addVolumeGb) > 0) {
          const row = await env.DB.prepare(
            "SELECT volume_gb FROM licenses WHERE code = ?"
          ).bind(code).first();
          if (row) {
            fields.push("volume_gb=?");
            args.push(Number(row.volume_gb || 0) + Number(body.addVolumeGb));
          }
        }

        if (!fields.length) return json({ ok: false, error: "nothing_to_update" }, 400);
        fields.push("updated_at=?");
        args.push(now());
        args.push(code);
        await env.DB.prepare(
          `UPDATE licenses SET ${fields.join(",")} WHERE code=?`
        ).bind(...args).run();
        return json({ ok: true });
      }

      // ============================================
      // ۶. حذف لایسنس
      // ============================================
      if (
        (url.pathname === "/v1/admin/delete" || url.pathname.startsWith("/v1/admin/delete/")) &&
        (request.method === "POST" || request.method === "DELETE")
      ) {
        if (!admin(request, env)) return json({ ok: false, error: "unauthorized" }, 401);

        let code = "";
        if (request.method === "POST") {
          const body = await request.json().catch(() => ({}));
          code = normalize(body.code || body.licenseKey);
        }
        if (!code) code = normalize(url.pathname.split("/").pop());
        if (!code) return json({ ok: false, error: "code_required" }, 400);

        const result = await env.DB.prepare("DELETE FROM licenses WHERE code = ?").bind(code).run();
        if (result.meta.changes > 0) return json({ ok: true, message: "license_deleted" });
        return json({ ok: false, error: "license_not_found" }, 404);
      }

      // ============================================
      // ۷. لیست لایسنس‌ها
      // ============================================
      if (url.pathname === "/v1/admin/licenses" && request.method === "GET") {
        if (!admin(request, env)) return json({ ok: false, error: "unauthorized" }, 401);

        const result = await env.DB.prepare(`
          SELECT code, device_id, license_type, duration_days, volume_gb, used_bytes,
                 expires_at, active, activated_at, created_at
          FROM licenses
          ORDER BY created_at DESC
        `).all();

        const licenses = (result.results || []).map((r) => {
          const t = now();
          const volumeGb = Number(r.volume_gb || 0);
          const usedBytes = Number(r.used_bytes || 0);
          return {
            ...r,
            isCurrentlyActive: computeActive(r, t),
            remainingBytes: Math.max(0, volumeGb * GB - usedBytes),
            remainingGb: volumeGb > 0 ? Math.max(0, volumeGb - usedBytes / GB) : null
          };
        });

        return json({ ok: true, licenses });
      }

      // ============================================
      // ۸. ریست Device ID
      // ============================================
      if (url.pathname === "/v1/admin/reset-device" && request.method === "POST") {
        if (!admin(request, env)) return json({ ok: false, error: "unauthorized" }, 401);
        const body = await request.json();
        const code = normalize(body.code);
        if (!code) return json({ ok: false, error: "code_required" }, 400);

        const result = await env.DB.prepare(
          "UPDATE licenses SET device_id = NULL, updated_at = ? WHERE code = ?"
        ).bind(now(), code).run();

        if (result.meta.changes > 0) return json({ ok: true, message: "device_reset" });
        return json({ ok: false, error: "license_not_found" }, 404);
      }

      // ============================================
      // ۹. ریست مصرف حجم
      // ============================================
      if (url.pathname === "/v1/admin/reset-usage" && request.method === "POST") {
        if (!admin(request, env)) return json({ ok: false, error: "unauthorized" }, 401);
        const body = await request.json();
        const code = normalize(body.code);
        if (!code) return json({ ok: false, error: "code_required" }, 400);

        const result = await env.DB.prepare(
          "UPDATE licenses SET used_bytes = 0, updated_at = ? WHERE code = ?"
        ).bind(now(), code).run();

        if (result.meta.changes > 0) return json({ ok: true, message: "usage_reset" });
        return json({ ok: false, error: "license_not_found" }, 404);
      }

      return json({ ok: false, error: "not_found" }, 404);
    } catch (e) {
      console.error("Error:", e);
      return json({ ok: false, error: String(e.message || e) }, 500);
    }
  }
};
