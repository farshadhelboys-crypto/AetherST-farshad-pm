const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Methods": "GET, POST, PUT, DELETE, OPTIONS",
  "Access-Control-Allow-Headers": "Content-Type, Authorization, Accept"
};

const json = (data, status = 200) => new Response(JSON.stringify(data), {
  status,
  headers: { ...corsHeaders, "content-type": "application/json; charset=utf-8", "cache-control": "no-store" }
});

const now = () => Date.now();
const normalize = (v) => String(v || "").trim().toUpperCase();
const GB = 1024 * 1024 * 1024;

function admin(request, env) {
  const auth = request.headers.get("authorization") || "";
  return auth === `Bearer ${env.ADMIN_TOKEN}`;
}

function computeActive(row, t = now()) {
  if (!row || !(row.active === 1 || row.active === true)) return false;
  const type = row.license_type || "time";
  const expiresOk = type === "volume" ? true : Number(row.expires_at || 0) > t;
  const volumeGb = Number(row.volume_gb || 0);
  const usedBytes = Number(row.used_bytes || 0);
  const volumeOk = type === "time" ? true : (volumeGb <= 0 || usedBytes < volumeGb * GB);
  return expiresOk && volumeOk;
}

function statusPayload(row, t = now()) {
  const volumeGb = Number(row?.volume_gb || 0);
  const usedBytes = Number(row?.used_bytes || 0);
  const remainingBytes = Math.max(0, volumeGb * GB - usedBytes);
  return {
    ok: true,
    active: computeActive(row, t),
    expiresAt: Number(row?.expires_at || 0),
    serverTime: t,
    licenseType: row?.license_type || "time",
    volumeGb,
    usedBytes,
    remainingBytes,
    remainingGb: volumeGb > 0 ? remainingBytes / GB : null
  };
}

function getAdminHTML() {
  return `<!DOCTYPE html>
<html lang="fa" dir="rtl">
<head>
<meta charset="UTF-8" />
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0" />
<title>AetherST - پنل لایسنس</title>
<style>
*{margin:0;padding:0;box-sizing:border-box}
body{font-family:system-ui,-apple-system,sans-serif;background:#0b0d12;color:#e8edf5;min-height:100vh;padding:12px}
.container{max-width:960px;margin:0 auto}
.login-box{background:rgba(255,255,255,.05);border:1px solid rgba(255,255,255,.08);border-radius:20px;padding:28px 20px;max-width:400px;margin:60px auto}
.login-box h1{text-align:center;font-size:26px;background:linear-gradient(135deg,#a78bfa,#6c5ce7);-webkit-background-clip:text;-webkit-text-fill-color:transparent;margin-bottom:6px}
.login-box p{text-align:center;color:#8892a8;font-size:13px;margin-bottom:20px}
.login-box input{width:100%;padding:14px;border-radius:12px;border:1px solid rgba(255,255,255,.08);background:rgba(255,255,255,.05);color:#fff;font-size:16px;outline:none;margin-bottom:12px;direction:ltr;text-align:left}
.login-box input:focus{border-color:#6c5ce7}
.login-box button{width:100%;padding:14px;border:none;border-radius:12px;background:linear-gradient(135deg,#6c5ce7,#a78bfa);color:#fff;font-size:16px;font-weight:700}
.login-box .error{color:#ef4444;font-size:13px;text-align:center;margin-top:10px;min-height:22px}
.login-box .hint{text-align:center;color:#5a647a;font-size:12px;margin-top:14px;background:rgba(255,255,255,.03);padding:10px;border-radius:8px}
.app{display:none}.app.show{display:block}
.header{display:flex;justify-content:space-between;align-items:center;padding:14px 16px;background:rgba(255,255,255,.04);border-radius:14px;border:1px solid rgba(255,255,255,.06);margin-bottom:16px;flex-wrap:wrap;gap:8px}
.header h1{font-size:18px;font-weight:800;background:linear-gradient(135deg,#a78bfa,#6c5ce7);-webkit-background-clip:text;-webkit-text-fill-color:transparent}
.badge{background:#22c55e;padding:4px 12px;border-radius:100px;font-size:11px;font-weight:600;color:#fff}
.logout-btn{background:rgba(239,68,68,.15);border:1px solid rgba(239,68,68,.25);color:#ef4444;padding:6px 14px;border-radius:8px;font-size:12px;font-weight:600}
.stats{display:grid;grid-template-columns:1fr 1fr 1fr 1fr;gap:10px;margin-bottom:16px}
.stat-card{background:rgba(255,255,255,.03);border:1px solid rgba(255,255,255,.06);border-radius:12px;padding:12px;text-align:center}
.stat-card .num{font-size:22px;font-weight:700;color:#a78bfa}
.stat-card .label{font-size:10px;color:#8892a8}
.card{background:rgba(255,255,255,.03);border:1px solid rgba(255,255,255,.06);border-radius:14px;padding:16px;margin-bottom:14px}
.card h2{font-size:16px;font-weight:600;margin-bottom:12px}
.card label{display:block;font-size:12px;font-weight:500;color:#b0b8cc;margin-bottom:4px}
.card input,.card select{width:100%;padding:12px;border-radius:10px;border:1px solid rgba(255,255,255,.08);background:rgba(255,255,255,.05);color:#fff;font-size:14px;outline:none;margin-bottom:10px}
.card input:focus,.card select:focus{border-color:#6c5ce7}
.row{display:grid;grid-template-columns:1fr 1fr;gap:10px}
.row3{display:grid;grid-template-columns:1fr 1fr 1fr;gap:10px}
@media(max-width:500px){.row,.row3{grid-template-columns:1fr}.stats{grid-template-columns:1fr 1fr}}
.btn{padding:12px 18px;border:none;border-radius:10px;font-weight:600;font-size:14px}
.btn-primary{background:linear-gradient(135deg,#6c5ce7,#a78bfa);color:#fff;width:100%}
.btn-outline{background:transparent;border:1px solid rgba(255,255,255,.12);color:#b0b8cc;padding:8px 12px;font-size:12px;border-radius:8px}
.btn-sm{padding:6px 10px;font-size:11px;border-radius:6px}
.btn-danger{background:rgba(239,68,68,.15);color:#ef4444;border:1px solid rgba(239,68,68,.25)}
.btn-success{background:rgba(34,197,94,.15);color:#22c55e;border:1px solid rgba(34,197,94,.25)}
.btn-warn{background:rgba(234,179,8,.15);color:#eab308;border:1px solid rgba(234,179,8,.25)}
.flex{display:flex;align-items:center;gap:8px;flex-wrap:wrap;margin-top:10px}
.code-box{background:rgba(0,0,0,.3);padding:8px 12px;border-radius:8px;font-family:monospace;font-size:13px;color:#a78bfa;word-break:break-all;border:1px dashed rgba(108,92,231,.3);flex:1}
.text-muted{color:#5a647a;font-size:12px}
.hidden{display:none!important}
.mt-8{margin-top:8px}
.table-wrap{overflow-x:auto;-webkit-overflow-scrolling:touch}
table{width:100%;border-collapse:collapse;font-size:12px}
table th{text-align:right;padding:8px 10px;color:#8892a8;font-weight:500;font-size:11px;border-bottom:1px solid rgba(255,255,255,.06)}
table td{padding:10px;border-bottom:1px solid rgba(255,255,255,.04);color:#d0d8e8;word-break:break-all;vertical-align:middle}
.badge-status{display:inline-block;padding:2px 8px;border-radius:100px;font-size:10px;font-weight:600}
.badge-status.active{background:rgba(34,197,94,.2);color:#22c55e}
.badge-status.inactive{background:rgba(239,68,68,.2);color:#ef4444}
.badge-status.expired{background:rgba(251,191,36,.2);color:#fbbf24}
.type-badge{display:inline-block;padding:2px 6px;border-radius:6px;font-size:10px;background:rgba(108,92,231,.2);color:#a78bfa}
.empty{text-align:center;padding:24px;color:#5a647a;font-size:13px}
.actions{display:flex;gap:5px;flex-wrap:wrap}
.modal-overlay{position:fixed;inset:0;background:rgba(0,0,0,.75);display:flex;align-items:flex-end;justify-content:center;z-index:1000;padding:12px;opacity:0;pointer-events:none;transition:.2s}
.modal-overlay.show{opacity:1;pointer-events:auto}
.modal{background:#14171f;border:1px solid rgba(255,255,255,.1);border-radius:20px 20px 12px 12px;padding:20px;width:100%;max-width:420px;max-height:85vh;overflow-y:auto}
.modal h3{font-size:17px;margin-bottom:14px;color:#a78bfa}
.modal-actions{display:flex;gap:10px;margin-top:14px}
.modal-actions .btn{flex:1}
.toast{position:fixed;bottom:20px;left:50%;transform:translateX(-50%);background:#1a1d27;border:1px solid rgba(255,255,255,.1);padding:12px 18px;border-radius:12px;color:#e8edf5;font-size:13px;font-weight:500;z-index:9999;opacity:0;transition:.3s;pointer-events:none;max-width:92%;text-align:center;line-height:1.5}
.toast.show{opacity:1}
.toast.success{border-color:#22c55e}
.toast.error{border-color:#ef4444}
.bulk-actions{display:flex;gap:10px;margin:10px 0;flex-wrap:wrap}
.bulk-actions .btn{width:auto;padding:8px 16px;font-size:12px}
.hint-box{color:#5a647a;font-size:11px;margin-top:8px;line-height:1.6;background:rgba(255,255,255,.03);padding:10px;border-radius:8px;border:1px dashed rgba(255,255,255,.06)}
</style>
</head>
<body>

<div class="container" id="loginPage">
  <div class="login-box">
    <h1>⚡ AetherST</h1>
    <p>پنل مدیریت لایسنس (زمانی + حجمی)</p>
    <input type="password" id="tokenInput" placeholder="توکن ادمین..." />
    <button onclick="handleLogin()">🚀 ورود</button>
    <div class="error" id="loginError"></div>
    <div class="hint">💡 توکن ادمین را وارد کنید</div>
  </div>
</div>

<div class="container app" id="app">
  <div class="header">
    <h1>⚡ AetherST</h1>
    <div style="display:flex;align-items:center;gap:8px;flex-wrap:wrap">
      <span class="badge">● Online</span>
      <button class="logout-btn" onclick="logout()">خروج</button>
    </div>
  </div>

  <div class="stats">
    <div class="stat-card"><div class="num" id="totalLicenses">-</div><div class="label">مجموع</div></div>
    <div class="stat-card"><div class="num" id="activeLicenses">-</div><div class="label">فعال</div></div>
    <div class="stat-card"><div class="num" id="volumeLicenses">-</div><div class="label">حجمی</div></div>
    <div class="stat-card"><div class="num" id="boundLicenses">-</div><div class="label">دستگاه</div></div>
  </div>

  <div class="card">
    <h2>➕ ساخت لایسنس</h2>
    <div class="row">
      <div>
        <label>کد (خالی = خودکار)</label>
        <input type="text" id="licenseKey" placeholder="خالی بگذار برای تولید خودکار" />
      </div>
      <div>
        <label>نوع لایسنس</label>
        <select id="licenseType" onchange="toggleTypeFields()">
          <option value="time">فقط زمانی</option>
          <option value="volume">فقط حجمی (دانلود)</option>
          <option value="both">زمانی + حجمی</option>
        </select>
      </div>
    </div>
    <div class="row">
      <div id="fieldDays">
        <label>مدت (روز)</label>
        <input type="number" id="durationDays" value="30" min="1" />
      </div>
      <div id="fieldVolume" class="hidden">
        <label>حجم (گیگابایت) — فقط دانلود</label>
        <input type="number" id="volumeGb" value="5" min="0.1" step="0.1" />
      </div>
    </div>
    <div>
      <label>Device ID (اختیاری)</label>
      <input type="text" id="createDeviceId" placeholder="اختیاری" style="direction:ltr;text-align:left" />
    </div>
    <button class="btn btn-primary" id="btnCreate" onclick="createLicense()">✨ ساخت لایسنس</button>
    <div class="hint-box">
      • <b>فقط زمانی</b>: فقط تاریخ انقضا<br>
      • <b>فقط حجمی</b>: فقط حجم دانلود (مثلاً ۵ گیگ)<br>
      • <b>ترکیبی</b>: هر کدام زودتر تمام شود → غیرفعال
    </div>
    <div id="createdCode" class="hidden mt-8">
      <div class="flex">
        <span class="text-muted">✅</span>
        <div class="code-box" id="newCodeDisplay"></div>
        <button class="btn-outline" onclick="copyCode()" style="width:auto">کپی</button>
      </div>
    </div>
  </div>

  <div class="card">
    <h2>📋 لیست <span id="licenseCount"></span></h2>
    <div class="bulk-actions">
      <button class="btn btn-outline btn-sm" onclick="selectAll()">✅ انتخاب همه</button>
      <button class="btn btn-outline btn-sm" onclick="deselectAll()">❌ لغو انتخاب</button>
      <button class="btn btn-sm btn-danger" onclick="bulkDelete()">🗑️ حذف انتخاب‌شده‌ها</button>
      <span style="color:#8892a8;font-size:12px;margin-right:auto" id="selectedCount">۰ انتخاب</span>
    </div>
    <div class="table-wrap">
      <table>
        <thead><tr><th style="width:30px">#</th><th>کد</th><th>نوع</th><th>حجم</th><th>دستگاه</th><th>وضعیت</th><th>عملیات</th></tr></thead>
        <tbody id="licenseTableBody"><tr><td colspan="7" class="empty">...</td></tr></tbody>
      </table>
    </div>
    <button class="btn-outline mt-8" onclick="fetchLicenses()" style="width:100%;margin-top:10px">🔄 بروزرسانی</button>
  </div>
</div>

<div class="modal-overlay" id="editModal">
  <div class="modal">
    <h3>✏️ ویرایش</h3>
    <input type="hidden" id="editCode" />
    <div><label>کد</label><input type="text" id="editCodeDisplay" disabled style="opacity:.6" /></div>
    <div><label>Device ID</label><input type="text" id="editDeviceId" placeholder="خالی = بدون دستگاه" style="direction:ltr;text-align:left" /></div>
    <div>
      <label>وضعیت</label>
      <select id="editActive">
        <option value="1">فعال</option>
        <option value="0">غیرفعال</option>
      </select>
    </div>
    <div><label>تمدید زمان (روز) - خالی=بدون تغییر</label><input type="number" id="editDuration" min="1" placeholder="مثلا ۳۰" /></div>
    <div><label>افزودن حجم (GB) - خالی=بدون تغییر</label><input type="number" id="editAddVolume" min="0.1" step="0.1" placeholder="مثلا ۵" /></div>
    <div class="modal-actions">
      <button class="btn btn-outline" onclick="closeEditModal()">انصراف</button>
      <button class="btn btn-primary" onclick="saveLicense()">ذخیره</button>
    </div>
    <div style="margin-top:12px;display:flex;gap:8px;flex-direction:column">
      <button class="btn btn-sm btn-warn" style="width:100%" onclick="resetDeviceFromModal()">🔄 ریست Device ID</button>
      <button class="btn btn-sm btn-outline" style="width:100%" onclick="resetUsageFromModal()">📉 ریست مصرف حجم</button>
    </div>
  </div>
</div>

<div class="toast" id="toast"></div>

<script>
const API_BASE = '';
const STORAGE_KEY = 'aetherst_token';
let allLicenses = [];
let currentToken = '';
let selectedLicenses = new Set();

const mem = Object.create(null);
let useLocal = false;
try {
  const t = '__t__';
  window.localStorage.setItem(t, '1');
  window.localStorage.removeItem(t);
  useLocal = true;
} catch (e) { useLocal = false; }

function safeGet(k) {
  if (useLocal) { try { return window.localStorage.getItem(k); } catch (e) { useLocal = false; } }
  return mem[k] != null ? mem[k] : null;
}
function safeSet(k, v) {
  if (useLocal) { try { window.localStorage.setItem(k, v); return; } catch (e) { useLocal = false; } }
  mem[k] = String(v);
}
function safeRemove(k) {
  if (useLocal) { try { window.localStorage.removeItem(k); return; } catch (e) { useLocal = false; } }
  delete mem[k];
}

function getToken() { return currentToken || safeGet(STORAGE_KEY) || ''; }

function showToast(msg, type) {
  const t = document.getElementById('toast');
  t.textContent = msg;
  t.className = 'toast ' + (type || 'success') + ' show';
  clearTimeout(t._h);
  t._h = setTimeout(() => t.classList.remove('show'), 4500);
}

function toggleTypeFields() {
  const type = document.getElementById('licenseType').value;
  document.getElementById('fieldDays').classList.toggle('hidden', type === 'volume');
  document.getElementById('fieldVolume').classList.toggle('hidden', type === 'time');
}

async function handleLogin() {
  const token = document.getElementById('tokenInput').value.trim();
  const err = document.getElementById('loginError');
  if (!token) { err.textContent = 'توکن را وارد کنید'; return; }
  err.textContent = 'در حال بررسی...';
  currentToken = token;
  safeSet(STORAGE_KEY, token);
  try {
    const res = await fetch(API_BASE + '/v1/admin/verify', {
      headers: { 'Authorization': 'Bearer ' + token }
    });
    const data = await res.json();
    if (data.ok) {
      document.getElementById('loginPage').style.display = 'none';
      document.getElementById('app').classList.add('show');
      err.textContent = '';
      fetchLicenses();
    } else {
      err.textContent = 'توکن نامعتبر';
      safeRemove(STORAGE_KEY);
      currentToken = '';
    }
  } catch (e) {
    document.getElementById('loginPage').style.display = 'none';
    document.getElementById('app').classList.add('show');
    err.textContent = '';
    fetchLicenses();
  }
}

function logout() {
  safeRemove(STORAGE_KEY);
  currentToken = '';
  selectedLicenses.clear();
  document.getElementById('app').classList.remove('show');
  document.getElementById('loginPage').style.display = 'block';
  document.getElementById('tokenInput').value = '';
}

(function () {
  try {
    const s = safeGet(STORAGE_KEY);
    if (s) {
      currentToken = s;
      document.getElementById('loginPage').style.display = 'none';
      document.getElementById('app').classList.add('show');
      fetchLicenses();
    }
  } catch (e) {}
})();

function makeCode() {
  const c = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
  const t = Date.now().toString(36).toUpperCase().slice(-5);
  let r = '';
  for (let i = 0; i < 7; i++) r += c[Math.floor(Math.random() * c.length)];
  return 'AETHER-' + t + r;
}

function formatBytes(b) {
  b = Number(b) || 0;
  if (b >= 1024*1024*1024) return (b/(1024**3)).toFixed(2) + ' GB';
  if (b >= 1024*1024) return (b/(1024**2)).toFixed(1) + ' MB';
  if (b >= 1024) return (b/1024).toFixed(0) + ' KB';
  return b + ' B';
}

function typeLabel(t) {
  if (t === 'volume') return 'حجمی';
  if (t === 'both') return 'ترکیبی';
  return 'زمانی';
}

async function fetchLicenses() {
  const token = getToken();
  if (!token) return;
  try {
    const res = await fetch(API_BASE + '/v1/admin/licenses', {
      headers: { 'Authorization': 'Bearer ' + token }
    });
    const data = await res.json();
    if (!data.ok) {
      if (data.error === 'unauthorized') { showToast('توکن نامعتبر', 'error'); logout(); return; }
      showToast(data.error || 'خطا در لیست', 'error');
      return;
    }
    allLicenses = data.licenses || [];
    const validCodes = new Set(allLicenses.map(l => l.code));
    for (const code of selectedLicenses) {
      if (!validCodes.has(code)) selectedLicenses.delete(code);
    }
    renderLicenses(allLicenses);
    updateStats(allLicenses);
  } catch (e) {
    showToast('خطا: ' + e.message, 'error');
  }
}

function esc(s) {
  if (!s) return '';
  return String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;').replace(/'/g,'&#39;');
}

function toggleSelect(code) {
  if (selectedLicenses.has(code)) selectedLicenses.delete(code);
  else selectedLicenses.add(code);
  updateSelectedCount();
  const checkbox = document.getElementById('cb_' + code);
  if (checkbox) checkbox.checked = selectedLicenses.has(code);
}

function selectAll() {
  allLicenses.forEach(l => selectedLicenses.add(l.code));
  renderLicenses(allLicenses);
  updateSelectedCount();
  showToast('همه انتخاب شدند', 'success');
}

function deselectAll() {
  selectedLicenses.clear();
  renderLicenses(allLicenses);
  updateSelectedCount();
}

function updateSelectedCount() {
  document.getElementById('selectedCount').textContent = selectedLicenses.size + ' انتخاب';
}

function renderLicenses(list) {
  const tb = document.getElementById('licenseTableBody');
  const cnt = document.getElementById('licenseCount');
  if (!list || !list.length) {
    tb.innerHTML = '<tr><td colspan="7" class="empty">لایسنسی نیست</td></tr>';
    cnt.textContent = '(۰)';
    updateSelectedCount();
    return;
  }
  cnt.textContent = '(' + list.length + ')';
  tb.innerHTML = list.map((l) => {
    const isActive = l.isCurrentlyActive != null ? l.isCurrentlyActive : (l.active === 1 || l.active === true);
    const device = l.device_id || l.deviceId || '';
    const code = esc(l.code);
    const checked = selectedLicenses.has(l.code) ? 'checked' : '';
    const lt = l.license_type || 'time';
    const volGb = Number(l.volume_gb || 0);
    const used = Number(l.used_bytes || 0);
    let volCell = '—';
    if (lt === 'volume' || lt === 'both') {
      volCell = formatBytes(used) + ' / ' + volGb + ' GB';
    }
    let statusClass = 'inactive';
    let statusText = 'غیرفعال';
    if (isActive) { statusClass = 'active'; statusText = 'فعال'; }
    else if ((l.active === 1 || l.active === true) && volGb > 0 && used >= volGb * 1024 * 1024 * 1024) {
      statusClass = 'expired'; statusText = 'حجم تمام';
    }
    return '<tr>' +
      '<td><input type="checkbox" id="cb_' + code + '" ' + checked + ' onchange="toggleSelect(\\'' + code + '\\')" style="accent-color:#6c5ce7;width:16px;height:16px;cursor:pointer" /></td>' +
      '<td><strong style="color:#a78bfa">' + code + '</strong></td>' +
      '<td><span class="type-badge">' + typeLabel(lt) + '</span></td>' +
      '<td style="font-size:11px">' + volCell + '</td>' +
      '<td style="font-size:11px;color:#8892a8;direction:ltr;text-align:right">' + (device ? esc(device) : '—') + '</td>' +
      '<td><span class="badge-status ' + statusClass + '">' + statusText + '</span></td>' +
      '<td><div class="actions">' +
        '<button class="btn-outline btn-sm" onclick="openEditModal(\\'' + code + '\\')">✏️</button>' +
        '<button class="btn-outline btn-sm ' + (isActive ? 'btn-danger' : 'btn-success') + '" onclick="toggleActive(\\'' + code + '\\',' + ((l.active===1||l.active===true)?0:1) + ')">' + ((l.active===1||l.active===true) ? '🔇' : '🔊') + '</button>' +
        '<button class="btn-outline btn-sm btn-warn" onclick="resetDevice(\\'' + code + '\\')">🔄</button>' +
        '<button class="btn-outline btn-sm btn-danger" onclick="deleteSingle(\\'' + code + '\\')">🗑️</button>' +
      '</div></td></tr>';
  }).join('');
  updateSelectedCount();
}

function updateStats(list) {
  document.getElementById('totalLicenses').textContent = list.length;
  document.getElementById('activeLicenses').textContent = list.filter(l => l.isCurrentlyActive || l.active === 1 || l.active === true).length;
  document.getElementById('volumeLicenses').textContent = list.filter(l => l.license_type === 'volume' || l.license_type === 'both').length;
  document.getElementById('boundLicenses').textContent = list.filter(l => l.device_id || l.deviceId).length;
}

async function deleteSingle(code) {
  if (!confirm('آیا از حذف لایسنس "' + code + '" مطمئن هستید؟')) return;
  await deleteLicenses([code]);
}

async function bulkDelete() {
  const codes = Array.from(selectedLicenses);
  if (!codes.length) {
    showToast('هیچ لایسنسی انتخاب نشده', 'error');
    return;
  }
  if (!confirm('آیا از حذف ' + codes.length + ' لایسنس انتخاب‌شده مطمئن هستید؟')) return;
  await deleteLicenses(codes);
}

async function deleteLicenses(codes) {
  const token = getToken();
  if (!token) { showToast('وارد شوید', 'error'); return; }

  let successCount = 0;
  let failCount = 0;

  for (const code of codes) {
    try {
      const res = await fetch(API_BASE + '/v1/admin/delete', {
        method: 'POST',
        headers: {
          'Authorization': 'Bearer ' + token,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ code })
      });
      const data = await res.json();
      if (data.ok) {
        successCount++;
        selectedLicenses.delete(code);
      } else {
        failCount++;
      }
    } catch (e) {
      failCount++;
    }
  }

  if (successCount > 0) {
    showToast(successCount + ' لایسنس حذف شد' + (failCount > 0 ? '، ' + failCount + ' خطا' : ''), 'success');
  } else {
    showToast('هیچ لایسنسی حذف نشد', 'error');
  }
  fetchLicenses();
}

async function createLicense() {
  const token = getToken();
  if (!token) { showToast('وارد شوید', 'error'); return; }

  const manual = document.getElementById('licenseKey').value.trim();
  const licenseType = document.getElementById('licenseType').value;
  const days = parseInt(document.getElementById('durationDays').value) || 30;
  const volumeGb = parseFloat(document.getElementById('volumeGb').value) || 0;
  const deviceId = document.getElementById('createDeviceId').value.trim() || null;
  const btn = document.getElementById('btnCreate');

  if (licenseType !== 'time' && volumeGb <= 0) {
    showToast('حجم باید بیشتر از صفر باشد', 'error');
    return;
  }

  btn.disabled = true;
  btn.textContent = 'در حال ساخت...';

  let lastErr = '';
  const tries = manual ? 1 : 6;

  for (let i = 0; i < tries; i++) {
    const code = manual || makeCode();
    const body = {
      code: code,
      licenseType: licenseType,
      durationDays: days,
      volumeGb: volumeGb
    };
    if (deviceId) {
      body.device_id = deviceId;
      body.deviceId = deviceId;
    }

    try {
      const res = await fetch(API_BASE + '/v1/admin/create', {
        method: 'POST',
        headers: {
          'Authorization': 'Bearer ' + token,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(body)
      });
      const data = await res.json();

      if (data.ok) {
        const finalCode = data.code || code;
        showToast('ساخته شد: ' + finalCode, 'success');
        document.getElementById('createdCode').classList.remove('hidden');
        document.getElementById('newCodeDisplay').textContent = finalCode;
        document.getElementById('licenseKey').value = '';
        document.getElementById('createDeviceId').value = '';
        btn.disabled = false;
        btn.textContent = '✨ ساخت لایسنس';
        fetchLicenses();
        return;
      }

      lastErr = data.error || data.message || ('status ' + res.status);
      const e = String(lastErr).toLowerCase();
      const isDup = e.includes('exist') || e.includes('already') || e.includes('duplicate') || e.includes('موجود') || e.includes('تکرار');

      if (isDup && !manual) continue;
      if (isDup && manual) {
        showToast('این کد قبلاً وجود دارد', 'error');
        btn.disabled = false;
        btn.textContent = '✨ ساخت لایسنس';
        return;
      }
      showToast(lastErr, 'error');
      btn.disabled = false;
      btn.textContent = '✨ ساخت لایسنس';
      return;
    } catch (err) {
      lastErr = err.message;
    }
  }

  showToast('ناموفق: ' + (lastErr || 'خطای ناشناخته'), 'error');
  btn.disabled = false;
  btn.textContent = '✨ ساخت لایسنس';
}

function openEditModal(code) {
  const lic = allLicenses.find(l => l.code === code);
  if (!lic) return;
  document.getElementById('editCode').value = code;
  document.getElementById('editCodeDisplay').value = code;
  document.getElementById('editDeviceId').value = lic.device_id || lic.deviceId || '';
  document.getElementById('editActive').value = (lic.active === 1 || lic.active === true) ? '1' : '0';
  document.getElementById('editDuration').value = '';
  document.getElementById('editAddVolume').value = '';
  document.getElementById('editModal').classList.add('show');
}
function closeEditModal() {
  document.getElementById('editModal').classList.remove('show');
}

async function saveLicense() {
  const token = getToken();
  const code = document.getElementById('editCode').value;
  const deviceId = document.getElementById('editDeviceId').value.trim();
  const active = parseInt(document.getElementById('editActive').value);
  const duration = document.getElementById('editDuration').value.trim();
  const addVol = document.getElementById('editAddVolume').value.trim();

  const body = { code, active, device_id: deviceId || null, deviceId: deviceId || null };
  if (duration) body.durationDays = parseInt(duration);
  if (addVol) body.addVolumeGb = parseFloat(addVol);

  try {
    const res = await fetch(API_BASE + '/v1/admin/update', {
      method: 'POST',
      headers: { 'Authorization': 'Bearer ' + token, 'Content-Type': 'application/json' },
      body: JSON.stringify(body)
    });
    const data = await res.json();
    if (!data.ok) { showToast(data.error || 'خطا', 'error'); return; }
    showToast('ذخیره شد', 'success');
    closeEditModal();
    fetchLicenses();
  } catch (e) { showToast(e.message, 'error'); }
}

async function toggleActive(code, newActive) {
  const token = getToken();
  try {
    const res = await fetch(API_BASE + '/v1/admin/update', {
      method: 'POST',
      headers: { 'Authorization': 'Bearer ' + token, 'Content-Type': 'application/json' },
      body: JSON.stringify({ code, active: newActive })
    });
    const data = await res.json();
    if (!data.ok) { showToast(data.error || 'خطا', 'error'); return; }
    showToast(newActive ? 'فعال شد' : 'غیرفعال شد', 'success');
    fetchLicenses();
  } catch (e) { showToast(e.message, 'error'); }
}

async function resetDevice(code) {
  if (!confirm('Device ID پاک شود؟\\n' + code)) return;
  const token = getToken();
  try {
    let res = await fetch(API_BASE + '/v1/admin/reset-device', {
      method: 'POST',
      headers: { 'Authorization': 'Bearer ' + token, 'Content-Type': 'application/json' },
      body: JSON.stringify({ code })
    });
    let data = await res.json();
    if (!data.ok) {
      res = await fetch(API_BASE + '/v1/admin/update', {
        method: 'POST',
        headers: { 'Authorization': 'Bearer ' + token, 'Content-Type': 'application/json' },
        body: JSON.stringify({ code, device_id: null, deviceId: null })
      });
      data = await res.json();
    }
    if (!data.ok) { showToast(data.error || 'خطا', 'error'); return; }
    showToast('ریست شد', 'success');
    fetchLicenses();
  } catch (e) { showToast(e.message, 'error'); }
}

function resetDeviceFromModal() {
  const code = document.getElementById('editCode').value;
  closeEditModal();
  resetDevice(code);
}

async function resetUsageFromModal() {
  const code = document.getElementById('editCode').value;
  if (!confirm('مصرف حجم ریست شود؟\\n' + code)) return;
  const token = getToken();
  try {
    const res = await fetch(API_BASE + '/v1/admin/reset-usage', {
      method: 'POST',
      headers: { 'Authorization': 'Bearer ' + token, 'Content-Type': 'application/json' },
      body: JSON.stringify({ code })
    });
    const data = await res.json();
    if (!data.ok) { showToast(data.error || 'خطا', 'error'); return; }
    showToast('مصرف ریست شد', 'success');
    closeEditModal();
    fetchLicenses();
  } catch (e) { showToast(e.message, 'error'); }
}

function copyCode() {
  const text = document.getElementById('newCodeDisplay').textContent;
  if (!text) return;
  if (navigator.clipboard) {
    navigator.clipboard.writeText(text).then(() => showToast('کپی شد', 'success'));
  } else {
    const ta = document.createElement('textarea');
    ta.value = text;
    document.body.appendChild(ta);
    ta.select();
    document.execCommand('copy');
    document.body.removeChild(ta);
    showToast('کپی شد', 'success');
  }
}

document.getElementById('editModal').addEventListener('click', function (e) {
  if (e.target === this) closeEditModal();
});
</script>
</body>
</html>`;
}

export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    const path = url.pathname;

    if (request.method === "OPTIONS") {
      return new Response(null, { status: 204, headers: corsHeaders });
    }

    try {
      // صفحه ادمین
      if ((path === "/" || path === "/admin" || path === "/admin.html") && request.method === "GET") {
        const accept = request.headers.get("accept") || "";
        if (path === "/" && accept.includes("application/json")) {
          return json({
            ok: true,
            service: "AetherST License API",
            status: "online",
            version: "3.0.0-volume",
            timestamp: new Date().toISOString()
          });
        }
        return new Response(getAdminHTML(), {
          headers: { ...corsHeaders, "content-type": "text/html; charset=utf-8" }
        });
      }

      // تایید توکن
      if (path === "/v1/admin/verify" && request.method === "GET") {
        if (!admin(request, env)) return json({ ok: false, error: "unauthorized" }, 401);
        return json({ ok: true, valid: true });
      }

      // ۱. وضعیت لایسنس (کلاینت)
      if (path === "/v1/status" && request.method === "GET") {
        const deviceId = String(url.searchParams.get("deviceId") || "").trim();
        if (!deviceId) return json({ ok: false, error: "device_id_required" }, 400);

        const row = await env.DB.prepare(
          `SELECT code, device_id, license_type, duration_days, volume_gb, used_bytes,
                  expires_at, active FROM licenses
           WHERE device_id = ? ORDER BY activated_at DESC, created_at DESC LIMIT 1`
        ).bind(deviceId).first().catch(async () => {
          // fallback اگر ستون‌های جدید هنوز اضافه نشده
          return env.DB.prepare(
            "SELECT code, device_id, expires_at, active FROM licenses WHERE device_id = ? ORDER BY expires_at DESC LIMIT 1"
          ).bind(deviceId).first();
        });

        if (!row) {
          return json({
            ok: true, active: false, expiresAt: 0, serverTime: now(),
            licenseType: "none", volumeGb: 0, usedBytes: 0, remainingBytes: 0, remainingGb: 0
          });
        }
        // اگر ستون‌های حجمی نبود، رفتار قدیمی
        if (row.volume_gb === undefined && row.license_type === undefined) {
          const active = row.active && Number(row.expires_at) > now();
          return json({
            ok: true, active: !!active, expiresAt: Number(row.expires_at || 0), serverTime: now(),
            licenseType: "time", volumeGb: 0, usedBytes: 0, remainingBytes: 0, remainingGb: null
          });
        }
        return json(statusPayload(row));
      }

      // ۲. فعال‌سازی
      if (path === "/v1/activate" && request.method === "POST") {
        const body = await request.json().catch(() => ({}));
        const code = normalize(body.code);
        const deviceId = String(body.deviceId || body.device_id || "").trim();
        if (!code || !deviceId) return json({ ok: false, error: "code_and_device_required" }, 400);

        const row = await env.DB.prepare("SELECT * FROM licenses WHERE code = ?").bind(code).first();
        if (!row) return json({ ok: false, error: "code_not_found" }, 404);
        if (row.device_id && row.device_id !== deviceId) return json({ ok: false, error: "code_used_by_other_device" }, 409);
        if (!row.active) return json({ ok: false, error: "license_revoked" }, 403);

        const t = now();
        let expires = Number(row.expires_at || 0);
        const type = row.license_type || "time";
        const days = Number(row.duration_days || 30);

        if (type === "time" || type === "both" || !row.license_type) {
          const base = Math.max(expires, t);
          if (!row.device_id) expires = base + days * 86400000;
          else if (expires <= t) expires = t + days * 86400000;
        } else {
          // volume-only
          if (!row.device_id || expires <= 0) expires = t + 3650 * 86400000;
        }

        await env.DB.prepare(
          "UPDATE licenses SET device_id=?, expires_at=?, activated_at=COALESCE(activated_at,?), updated_at=? WHERE code=?"
        ).bind(deviceId, expires, t, t, code).run();

        const updated = await env.DB.prepare("SELECT * FROM licenses WHERE code = ?").bind(code).first();
        if (updated && updated.volume_gb !== undefined) {
          return json(statusPayload(updated, t));
        }
        return json({ ok: true, active: true, expiresAt: expires, serverTime: t });
      }

      // ۳. گزارش مصرف حجم (فقط دانلود)
      if (path === "/v1/report-usage" && request.method === "POST") {
        const body = await request.json().catch(() => ({}));
        const deviceId = String(body.deviceId || body.device_id || "").trim();
        const deltaBytes = Math.max(0, Math.floor(Number(body.deltaBytes || body.downloadedBytes || 0)));
        if (!deviceId) return json({ ok: false, error: "device_id_required" }, 400);

        const row = await env.DB.prepare(
          `SELECT * FROM licenses WHERE device_id = ? ORDER BY activated_at DESC, created_at DESC LIMIT 1`
        ).bind(deviceId).first().catch(() => null);

        if (!row) return json({ ok: false, error: "no_license" }, 404);
        if (!row.active) return json({ ok: false, error: "license_revoked" }, 403);

        const type = row.license_type || "time";
        if (type === "time" || deltaBytes <= 0) {
          return json(statusPayload(row));
        }

        const newUsed = Number(row.used_bytes || 0) + deltaBytes;
        await env.DB.prepare(
          "UPDATE licenses SET used_bytes=?, updated_at=? WHERE code=?"
        ).bind(newUsed, now(), row.code).run();

        return json(statusPayload({ ...row, used_bytes: newUsed }));
      }

      // ۴. ساخت لایسنس
      if (path === "/v1/admin/create" && request.method === "POST") {
        if (!admin(request, env)) return json({ ok: false, error: "unauthorized" }, 401);

        const body = await request.json().catch(() => ({}));
        let code = normalize(body.code || body.key || body.licenseKey);
        const days = Math.max(0, Number(body.durationDays || body.duration || 30));
        const deviceId = body.device_id || body.deviceId || null;
        let licenseType = String(body.licenseType || body.type || "time").toLowerCase();
        if (!["time", "volume", "both"].includes(licenseType)) licenseType = "time";
        const volumeGb = Math.max(0, Number(body.volumeGb || body.volume_gb || 0));

        if (licenseType === "volume" && volumeGb <= 0) {
          return json({ ok: false, error: "volume_gb_required" }, 400);
        }
        if (licenseType === "both" && (days < 1 || volumeGb <= 0)) {
          return json({ ok: false, error: "both_duration_and_volume_required" }, 400);
        }
        if (licenseType === "time" && days < 1) {
          return json({ ok: false, error: "duration_days_required" }, 400);
        }

        if (!code) {
          const chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
          for (let attempt = 0; attempt < 8; attempt++) {
            const t = Date.now().toString(36).toUpperCase().slice(-5);
            let r = "";
            for (let i = 0; i < 7; i++) r += chars[Math.floor(Math.random() * chars.length)];
            code = "AETHER-" + t + r;
            const exists = await env.DB.prepare("SELECT code FROM licenses WHERE code = ?").bind(code).first();
            if (!exists) break;
          }
        }

        const existing = await env.DB.prepare("SELECT code FROM licenses WHERE code = ?").bind(code).first();
        if (existing) return json({ ok: false, error: "code_already_exists" }, 400);

        const t = now();
        try {
          await env.DB.prepare(
            `INSERT INTO licenses(code, device_id, license_type, duration_days, volume_gb, used_bytes, expires_at, active, created_at, updated_at)
             VALUES(?,?,?,?,?,0,0,1,?,?)`
          ).bind(code, deviceId, licenseType, days || 30, volumeGb, t, t).run();
        } catch (e) {
          const msg = String(e.message || e);
          // اگر کاربر حجمی خواسته ولی ستون‌ها نیست → خطا (نه ساخت زمانی مخفی)
          if (licenseType === "volume" || licenseType === "both" || volumeGb > 0) {
            return json({
              ok: false,
              error: "volume_columns_missing",
              message: "ستون‌های حجمی در دیتابیس نیست. این SQL را در D1 اجرا کنید: ALTER TABLE licenses ADD COLUMN license_type TEXT NOT NULL DEFAULT 'time'; ALTER TABLE licenses ADD COLUMN volume_gb REAL NOT NULL DEFAULT 0; ALTER TABLE licenses ADD COLUMN used_bytes INTEGER NOT NULL DEFAULT 0;",
              detail: msg
            }, 500);
          }
          // فقط برای لایسنس زمانی، fallback
          await env.DB.prepare(
            "INSERT INTO licenses(code, device_id, duration_days, expires_at, active, created_at, updated_at) VALUES(?,?,?,?,1,?,?)"
          ).bind(code, deviceId, days || 30, 0, t, t).run();
          return json({ ok: true, code, durationDays: days || 30, volumeGb: 0, licenseType: "time", device_id: deviceId });
        }

        return json({ ok: true, code, durationDays: days || 30, volumeGb, licenseType, device_id: deviceId });
      }

      // ۵. بروزرسانی
      if (path === "/v1/admin/update" && request.method === "POST") {
        if (!admin(request, env)) return json({ ok: false, error: "unauthorized" }, 401);

        const body = await request.json().catch(() => ({}));
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
        if (body.deviceId !== undefined || body.device_id !== undefined) {
          fields.push("device_id=?");
          const v = body.deviceId !== undefined ? body.deviceId : body.device_id;
          args.push(v ? String(v) : null);
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
        if (body.durationDays !== undefined && Number(body.durationDays) > 0) {
          const row = await env.DB.prepare(
            "SELECT expires_at, duration_days FROM licenses WHERE code = ?"
          ).bind(code).first();
          if (row) {
            const newExpires = Math.max(Number(row.expires_at || 0), now()) + (Number(body.durationDays) * 86400000);
            fields.push("expires_at=?");
            args.push(newExpires);
            fields.push("duration_days=?");
            args.push(Number(row.duration_days || 0) + Number(body.durationDays));
          }
        }
        if (body.addVolumeGb !== undefined && Number(body.addVolumeGb) > 0) {
          const row = await env.DB.prepare("SELECT volume_gb FROM licenses WHERE code = ?").bind(code).first();
          if (row) {
            fields.push("volume_gb=?");
            args.push(Number(row.volume_gb || 0) + Number(body.addVolumeGb));
          }
        }

        if (!fields.length) return json({ ok: false, error: "nothing_to_update" }, 400);

        fields.push("updated_at=?");
        args.push(now());
        args.push(code);

        const result = await env.DB.prepare(
          `UPDATE licenses SET ${fields.join(",")} WHERE code=?`
        ).bind(...args).run();

        if (result.meta?.changes === 0) return json({ ok: false, error: "license_not_found" }, 404);
        return json({ ok: true });
      }

      // ۶. حذف لایسنس
      if (path === "/v1/admin/delete" && request.method === "POST") {
        if (!admin(request, env)) return json({ ok: false, error: "unauthorized" }, 401);

        const body = await request.json().catch(() => ({}));
        const code = normalize(body.code || body.licenseKey);
        if (!code) return json({ ok: false, error: "code_required" }, 400);

        const existing = await env.DB.prepare("SELECT code FROM licenses WHERE code = ?").bind(code).first();
        if (!existing) return json({ ok: false, error: "license_not_found" }, 404);

        const result = await env.DB.prepare("DELETE FROM licenses WHERE code = ?").bind(code).run();
        if (result.meta?.changes > 0) return json({ ok: true, message: "license_deleted" });
        return json({ ok: false, error: "delete_failed" }, 500);
      }

      // ۷. حذف با DELETE
      if (path.startsWith("/v1/admin/licenses/") && request.method === "DELETE") {
        if (!admin(request, env)) return json({ ok: false, error: "unauthorized" }, 401);

        const code = normalize(path.split("/").pop());
        if (!code) return json({ ok: false, error: "code_required" }, 400);

        const existing = await env.DB.prepare("SELECT code FROM licenses WHERE code = ?").bind(code).first();
        if (!existing) return json({ ok: false, error: "license_not_found" }, 404);

        const result = await env.DB.prepare("DELETE FROM licenses WHERE code = ?").bind(code).run();
        if (result.meta?.changes > 0) return json({ ok: true, message: "license_deleted" });
        return json({ ok: false, error: "delete_failed" }, 500);
      }

      // ۸. لیست لایسنس‌ها
      if (path === "/v1/admin/licenses" && request.method === "GET") {
        if (!admin(request, env)) return json({ ok: false, error: "unauthorized" }, 401);

        let result;
        try {
          result = await env.DB.prepare(`
            SELECT code, device_id, license_type, duration_days, volume_gb, used_bytes,
                   expires_at, active, activated_at, created_at
            FROM licenses
            ORDER BY created_at DESC
          `).all();
        } catch (e) {
          result = await env.DB.prepare(`
            SELECT code, device_id, duration_days, expires_at, active, activated_at, created_at
            FROM licenses
            ORDER BY created_at DESC
          `).all();
        }

        const licenses = (result.results || []).map((r) => {
          const t = now();
          const volumeGb = Number(r.volume_gb || 0);
          const usedBytes = Number(r.used_bytes || 0);
          return {
            ...r,
            isCurrentlyActive: computeActive(
              { ...r, license_type: r.license_type || "time", volume_gb: volumeGb, used_bytes: usedBytes },
              t
            ),
            remainingBytes: Math.max(0, volumeGb * GB - usedBytes),
            remainingGb: volumeGb > 0 ? Math.max(0, volumeGb - usedBytes / GB) : null
          };
        });

        return json({ ok: true, licenses });
      }

      // ۹. ریست Device ID
      if (path === "/v1/admin/reset-device" && request.method === "POST") {
        if (!admin(request, env)) return json({ ok: false, error: "unauthorized" }, 401);

        const body = await request.json().catch(() => ({}));
        const code = normalize(body.code);
        if (!code) return json({ ok: false, error: "code_required" }, 400);

        const result = await env.DB.prepare(
          "UPDATE licenses SET device_id = NULL, updated_at = ? WHERE code = ?"
        ).bind(now(), code).run();

        if (result.meta?.changes > 0) return json({ ok: true, message: "device_reset" });
        return json({ ok: false, error: "license_not_found" }, 404);
      }

      // ۱۰. ریست مصرف حجم
      if (path === "/v1/admin/reset-usage" && request.method === "POST") {
        if (!admin(request, env)) return json({ ok: false, error: "unauthorized" }, 401);

        const body = await request.json().catch(() => ({}));
        const code = normalize(body.code);
        if (!code) return json({ ok: false, error: "code_required" }, 400);

        try {
          const result = await env.DB.prepare(
            "UPDATE licenses SET used_bytes = 0, updated_at = ? WHERE code = ?"
          ).bind(now(), code).run();
          if (result.meta?.changes > 0) return json({ ok: true, message: "usage_reset" });
          return json({ ok: false, error: "license_not_found" }, 404);
        } catch (e) {
          return json({ ok: false, error: "volume_columns_missing_run_migration" }, 500);
        }
      }


      // ============================================
      // پیام همگانی (اعلان برای همه کاربران اپ)
      // ============================================
      if (path === "/v1/announcements" && request.method === "GET") {
        // کلاینت: آخرین پیام‌های فعال
        const limit = Math.min(20, Math.max(1, Number(url.searchParams.get("limit") || 5)));
        try {
          const result = await env.DB.prepare(
            `SELECT id, title, body, created_at, active
             FROM announcements
             WHERE active = 1
             ORDER BY created_at DESC
             LIMIT ?`
          ).bind(limit).all();
          return json({ ok: true, announcements: result.results || [], serverTime: now() });
        } catch (e) {
          return json({
            ok: false,
            error: "announcements_table_missing",
            message: "جدول announcements وجود ندارد. SQL: CREATE TABLE IF NOT EXISTS announcements (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL, body TEXT NOT NULL, active INTEGER NOT NULL DEFAULT 1, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL);"
          }, 500);
        }
      }

      if (path === "/v1/admin/announcements" && request.method === "GET") {
        if (!admin(request, env)) return json({ ok: false, error: "unauthorized" }, 401);
        try {
          const result = await env.DB.prepare(
            `SELECT id, title, body, active, created_at, updated_at
             FROM announcements ORDER BY created_at DESC LIMIT 50`
          ).all();
          return json({ ok: true, announcements: result.results || [] });
        } catch (e) {
          return json({ ok: false, error: "announcements_table_missing", detail: String(e.message || e) }, 500);
        }
      }

      if (path === "/v1/admin/announcements" && request.method === "POST") {
        if (!admin(request, env)) return json({ ok: false, error: "unauthorized" }, 401);
        const body = await request.json().catch(() => ({}));
        const title = String(body.title || "").trim();
        const text = String(body.body || body.message || "").trim();
        if (!title || !text) return json({ ok: false, error: "title_and_body_required" }, 400);
        const t = now();
        try {
          const result = await env.DB.prepare(
            `INSERT INTO announcements(title, body, active, created_at, updated_at) VALUES(?,?,1,?,?)`
          ).bind(title, text, t, t).run();
          return json({ ok: true, id: result.meta?.last_row_id, title, body: text, created_at: t });
        } catch (e) {
          return json({
            ok: false,
            error: "announcements_table_missing",
            message: "CREATE TABLE IF NOT EXISTS announcements (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL, body TEXT NOT NULL, active INTEGER NOT NULL DEFAULT 1, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL);",
            detail: String(e.message || e)
          }, 500);
        }
      }

      if (path === "/v1/admin/announcements/toggle" && request.method === "POST") {
        if (!admin(request, env)) return json({ ok: false, error: "unauthorized" }, 401);
        const body = await request.json().catch(() => ({}));
        const id = Number(body.id);
        if (!id) return json({ ok: false, error: "id_required" }, 400);
        const active = body.active ? 1 : 0;
        const result = await env.DB.prepare(
          "UPDATE announcements SET active=?, updated_at=? WHERE id=?"
        ).bind(active, now(), id).run();
        if (result.meta?.changes > 0) return json({ ok: true });
        return json({ ok: false, error: "not_found" }, 404);
      }

      if (path === "/v1/admin/announcements/delete" && request.method === "POST") {
        if (!admin(request, env)) return json({ ok: false, error: "unauthorized" }, 401);
        const body = await request.json().catch(() => ({}));
        const id = Number(body.id);
        if (!id) return json({ ok: false, error: "id_required" }, 400);
        const result = await env.DB.prepare("DELETE FROM announcements WHERE id=?").bind(id).run();
        if (result.meta?.changes > 0) return json({ ok: true });
        return json({ ok: false, error: "not_found" }, 404);
      }


      return json({ ok: false, error: "not_found" }, 404);

    } catch (e) {
      console.error("Error:", e);
      return json({ ok: false, error: String(e.message || e) }, 500);
    }
  }
};
