// FaceFirstAppLock PIN kurtarma servisi (Cloudflare Worker + KV). Gizli anahtarlar yalnızca Worker sırlarındadır:
//   MAIL_API_KEY, MAIL_FROM  (APK'da veya Git'te YOK)
const WINDOW_MS = 120_000;      // istek zaman damgası toleransı
const CODE_TTL_S = 300;         // kod 5 dakika geçerli
const MAX_ATTEMPTS = 5;         // kod başına hatalı deneme
const MAX_SENDS_PER_HOUR = 3;   // cihaz başına
const MAX_SENDS_PER_EMAIL_HOUR = 5;

const enc = new TextEncoder();
const b64 = {
  dec: (s) => Uint8Array.from(atob(s), (c) => c.charCodeAt(0)),
  enc: (u8) => btoa(String.fromCharCode(...new Uint8Array(u8))),
};
const json = (o, status = 200) =>
  new Response(JSON.stringify(o), { status, headers: { 'content-type': 'application/json' } });

// Android Keystore ECDSA imzası DER; WebCrypto ham r||s ister.
export function derToRaw(der) {
  let i = 2; if (der[1] & 0x80) i += der[1] & 0x7f;
  const read = () => { const len = der[i + 1]; let v = der.slice(i + 2, i + 2 + len); i += 2 + len;
    while (v.length > 32 && v[0] === 0) v = v.slice(1); const out = new Uint8Array(32); out.set(v, 32 - v.length); return out; };
  const r = read(), s = read(); const raw = new Uint8Array(64); raw.set(r, 0); raw.set(s, 32); return raw;
}

async function verifySig(pubB64, message, sigB64) {
  try {
    const key = await crypto.subtle.importKey('spki', b64.dec(pubB64), { name: 'ECDSA', namedCurve: 'P-256' }, false, ['verify']);
    const sig = b64.dec(sigB64);
    const raw = sig.length === 64 ? sig : derToRaw(sig);
    return await crypto.subtle.verify({ name: 'ECDSA', hash: 'SHA-256' }, key, raw, enc.encode(message));
  } catch { return false; }
}

async function sha256Hex(s) {
  const d = await crypto.subtle.digest('SHA-256', enc.encode(s));
  return [...new Uint8Array(d)].map((b) => b.toString(16).padStart(2, '0')).join('');
}

export function maskEmail(e) {
  const [u, d] = e.split('@'); return `${u[0]}***@${d}`;
}

function newCode() {
  const n = crypto.getRandomValues(new Uint32Array(1))[0] % 1_000_000;
  return String(n).padStart(6, '0');
}

async function sendMail(env, to, subject, text) {
  if (env.sendMail) return env.sendMail(to, subject, text); // test enjeksiyonu
  const r = await fetch('https://api.brevo.com/v3/smtp/email', {
    method: 'POST',
    headers: { 'api-key': env.MAIL_API_KEY, 'content-type': 'application/json' },
    body: JSON.stringify({ sender: { email: env.MAIL_FROM, name: 'FaceFirst Kilit' }, to: [{ email: to }], subject, textContent: text }),
  });
  if (!r.ok) throw new Error('mail_failed');
}

async function bump(env, key, limit, ttl = 3600) {
  const n = parseInt((await env.KV.get(key)) || '0', 10);
  if (n >= limit) return false;
  await env.KV.put(key, String(n + 1), { expirationTtl: ttl });
  return true;
}

const validEmail = (e) => typeof e === 'string' && e.length <= 254 && /^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(e);
const validId = (s) => typeof s === 'string' && /^[A-Za-z0-9_-]{16,64}$/.test(s);

export default {
  async fetch(request, env) {
    if (request.method !== 'POST') return json({ error: 'method' }, 405);
    const path = new URL(request.url).pathname;
    const raw = await request.text();
    let body; try { body = JSON.parse(raw); } catch { return json({ error: 'bad_json' }, 400); }
    const now = env.now ? env.now() : Date.now();
    if (!validId(body.deviceId) || typeof body.ts !== 'number' || Math.abs(now - body.ts) > WINDOW_MS) return json({ error: 'bad_request' }, 400);
    const sig = request.headers.get('x-sig') || '';
    const dev = JSON.parse((await env.KV.get(`dev:${body.deviceId}`)) || 'null');
    let pubKey = dev?.pub ?? body.publicKey;       // ilk kayıtta gövdedeki anahtar (TOFU)
    if (!pubKey && path === '/v1/email/verify') {  // bekleyen kayıt: anahtar, kod kaydında saklı
      pubKey = JSON.parse((await env.KV.get(`otp:email:${body.deviceId}`)) || 'null')?.pub;
    }
    if (!pubKey || !(await verifySig(pubKey, `${path}\n${raw}`, sig))) return json({ error: 'bad_signature' }, 401);

    const otpKey = (kind) => `otp:${kind}:${body.deviceId}`;

    async function issueCode(kind, to, extra = {}) {
      if (!(await bump(env, `rl:dev:${body.deviceId}`, MAX_SENDS_PER_HOUR)) ||
          !(await bump(env, `rl:mail:${await sha256Hex(to)}`, MAX_SENDS_PER_EMAIL_HOUR))) return json({ error: 'rate_limited' }, 429);
      const code = newCode();
      const salt = crypto.randomUUID();
      await env.KV.put(otpKey(kind), JSON.stringify({ salt, h: await sha256Hex(code + salt), attempts: 0, ...extra }), { expirationTtl: CODE_TTL_S });
      try { await sendMail(env, to, 'FaceFirst Kilit doğrulama kodu', `Doğrulama kodun: ${code}\nKod 5 dakika geçerlidir. Bu isteği sen yapmadıysan görmezden gel.`); }
      catch { await env.KV.delete(otpKey(kind)); return json({ error: 'mail_failed' }, 502); }
      return json({ ok: true, masked: maskEmail(to) });
    }

    async function checkCode(kind) {
      const rec = JSON.parse((await env.KV.get(otpKey(kind))) || 'null');
      if (!rec) return { err: json({ error: 'no_code' }, 400) };
      if (rec.attempts >= MAX_ATTEMPTS) { await env.KV.delete(otpKey(kind)); return { err: json({ error: 'too_many_attempts' }, 429) }; }
      if ((await sha256Hex(String(body.code) + rec.salt)) !== rec.h) {
        rec.attempts += 1;
        await env.KV.put(otpKey(kind), JSON.stringify(rec), { expirationTtl: CODE_TTL_S });
        return { err: json({ error: 'wrong_code' }, 400) };
      }
      await env.KV.delete(otpKey(kind)); // tek kullanımlık
      return { rec };
    }

    if (path === '/v1/email/start') {
      if (!validEmail(body.email)) return json({ error: 'bad_email' }, 400);
      if (dev && dev.pub !== pubKey) return json({ error: 'conflict' }, 409);
      return issueCode('email', body.email, { email: body.email, pub: pubKey });
    }
    if (path === '/v1/email/verify') {
      const { rec, err } = await checkCode('email'); if (err) return err;
      await env.KV.put(`dev:${body.deviceId}`, JSON.stringify({ pub: rec.pub, email: rec.email }));
      return json({ ok: true, masked: maskEmail(rec.email) });
    }
    if (path === '/v1/reset/start') {
      if (!dev?.email) return json({ error: 'no_recovery_email' }, 400);
      return issueCode('reset', dev.email);
    }
    if (path === '/v1/reset/verify') {
      if (!dev?.email) return json({ error: 'no_recovery_email' }, 400);
      const { err } = await checkCode('reset'); if (err) return err;
      await env.KV.put(`resetok:${body.deviceId}`, '1', { expirationTtl: 600 });
      return json({ ok: true });
    }
    if (path === '/v1/reset/done') { // uygulama yeni PIN'i kaydettikten sonra: bildirim e-postası
      if (!dev?.email || !(await env.KV.get(`resetok:${body.deviceId}`))) return json({ error: 'not_verified' }, 403);
      await env.KV.delete(`resetok:${body.deviceId}`);
      try { await sendMail(env, dev.email, 'FaceFirst Kilit: PIN sıfırlandı', 'PIN\'in sıfırlandı. Bunu sen yapmadıysan uygulamayı hemen kontrol et.'); } catch {}
      return json({ ok: true });
    }
    return json({ error: 'not_found' }, 404);
  },
};
