import test from 'node:test';
import assert from 'node:assert/strict';
import { webcrypto as crypto } from 'node:crypto';
import worker, { derToRaw, maskEmail } from '../worker.js';

globalThis.crypto ??= crypto;
const b64 = (u8) => Buffer.from(u8).toString('base64');

class FakeKV { m = new Map();
  async get(k) { return this.m.get(k) ?? null; }
  async put(k, v) { this.m.set(k, v); }
  async delete(k) { this.m.delete(k); } }

async function setup() {
  const kp = await crypto.subtle.generateKey({ name: 'ECDSA', namedCurve: 'P-256' }, true, ['sign', 'verify']);
  const pub = b64(await crypto.subtle.exportKey('spki', kp.publicKey));
  const mails = []; let t = 1_700_000_000_000;
  const env = { KV: new FakeKV(), now: () => t, sendMail: async (to, s, text) => mails.push({ to, s, text }) };
  const deviceId = 'device-1234567890abcdef';
  const call = async (path, body, { key = kp.privateKey, badSig = false } = {}) => {
    const raw = JSON.stringify({ deviceId, ts: t, ...body });
    const sig = badSig ? 'AAAA' : b64(await crypto.subtle.sign({ name: 'ECDSA', hash: 'SHA-256' }, key, new TextEncoder().encode(`${path}\n${raw}`)));
    const r = await worker.fetch(new Request('https://x' + path, { method: 'POST', body: raw, headers: { 'x-sig': sig } }), env);
    return { status: r.status, body: await r.json() };
  };
  const lastCode = () => mails.at(-1).text.match(/\d{6}/)[0];
  return { env, mails, pub, call, lastCode, tick: (ms) => (t += ms), kp };
}

async function registered(s, email = 'ridvan@example.com') {
  assert.equal((await s.call('/v1/email/start', { email, publicKey: s.pub })).status, 200);
  assert.equal((await s.call('/v1/email/verify', { code: s.lastCode() })).status, 200);
}

test('e-posta kaydı ve doğrulama', async () => {
  const s = await setup(); await registered(s);
  assert.equal(JSON.parse(s.env.KV.m.get('dev:device-1234567890abcdef')).email, 'ridvan@example.com');
  assert.equal(s.mails.length, 1);
});
test('kötü imza reddedilir', async () => {
  const s = await setup();
  assert.equal((await s.call('/v1/email/start', { email: 'a@b.co', publicKey: s.pub }, { badSig: true })).status, 401);
});
test('eski zaman damgası reddedilir', async () => {
  const s = await setup();
  const r = await s.call('/v1/email/start', { email: 'a@b.co', publicKey: s.pub, ts: 1 });
  assert.equal(r.status, 400);
});
test('yanlış kod 5 denemeden sonra kilitlenir, doğru kod da geçmez', async () => {
  const s = await setup();
  await s.call('/v1/email/start', { email: 'a@b.co', publicKey: s.pub });
  const good = s.lastCode();
  for (let i = 0; i < 5; i++) assert.equal((await s.call('/v1/email/verify', { code: '000000' })).body.error, i === 0 || true ? 'wrong_code' : '');
  assert.equal((await s.call('/v1/email/verify', { code: good })).status, 429);
});
test('kod tek kullanımlık', async () => {
  const s = await setup(); await registered(s);
  await s.call('/v1/reset/start', {}); const c = s.lastCode();
  assert.equal((await s.call('/v1/reset/verify', { code: c })).status, 200);
  assert.equal((await s.call('/v1/reset/verify', { code: c })).status, 400);
});
test('sıfırlama akışı: kod, doğrulama, bildirim e-postası', async () => {
  const s = await setup(); await registered(s);
  const st = await s.call('/v1/reset/start', {}); assert.equal(st.body.masked, 'r***@example.com');
  assert.equal((await s.call('/v1/reset/verify', { code: s.lastCode() })).status, 200);
  assert.equal((await s.call('/v1/reset/done', {})).status, 200);
  assert.match(s.mails.at(-1).s, /PIN sıfırlandı/);
  assert.equal((await s.call('/v1/reset/done', {})).status, 403); // tekrar yok
});
test('doğrulama olmadan reset/done reddedilir', async () => {
  const s = await setup(); await registered(s);
  assert.equal((await s.call('/v1/reset/done', {})).status, 403);
});
test('başka cihaz anahtarı kayıtlı cihaza istek yapamaz', async () => {
  const s = await setup(); await registered(s);
  const other = await crypto.subtle.generateKey({ name: 'ECDSA', namedCurve: 'P-256' }, true, ['sign', 'verify']);
  assert.equal((await s.call('/v1/reset/start', {}, { key: other.privateKey })).status, 401);
});
test('gönderim hız sınırı (cihaz başına saatte 3)', async () => {
  const s = await setup(); await registered(s); // 1
  assert.equal((await s.call('/v1/reset/start', {})).status, 200); // 2
  assert.equal((await s.call('/v1/reset/start', {})).status, 200); // 3
  assert.equal((await s.call('/v1/reset/start', {})).status, 429);
});
test('kayıtlı e-posta olmadan reset istenemez', async () => {
  const s = await setup();
  assert.equal((await s.call('/v1/reset/start', { publicKey: s.pub })).status, 400);
});
test('DER imza ham r||s biçimine çevrilir', () => {
  const der = Uint8Array.from([0x30, 0x08, 0x02, 0x02, 0x00, 0x80, 0x02, 0x02, 0x01, 0x02]);
  const raw = derToRaw(der);
  assert.equal(raw.length, 64); assert.equal(raw[31], 0x80); assert.equal(raw[62], 0x01); assert.equal(raw[63], 0x02);
});
test('e-posta maskeleme', () => assert.equal(maskEmail('ridvan@gmail.com'), 'r***@gmail.com'));
