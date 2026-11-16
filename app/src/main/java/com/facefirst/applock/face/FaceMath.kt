package com.facefirst.applock.face

import kotlin.math.exp
import kotlin.math.sqrt

/** Saf matematik (Android'e bağımlı değil, birim testli). */
object FaceMath {
    /** 112x112 kırpım için 5 referans nokta (x,y): sol göz, sağ göz, burun, sol ağız, sağ ağız (görüntüye göre). */
    val REF = floatArrayOf(38.2946f, 51.6963f, 73.5318f, 51.5014f, 56.0252f, 71.7366f, 41.5493f, 92.3655f, 70.7299f, 92.2041f)

    fun l2(v: FloatArray): FloatArray {
        var s = 0f
        for (x in v) s += x * x
        val n = sqrt(s).coerceAtLeast(1e-9f)
        return FloatArray(v.size) { v[it] / n }
    }

    fun cosine(a: FloatArray, b: FloatArray): Float {
        var d = 0f; var na = 0f; var nb = 0f
        for (i in a.indices) { d += a[i] * b[i]; na += a[i] * a[i]; nb += b[i] * b[i] }
        return d / (sqrt(na) * sqrt(nb)).coerceAtLeast(1e-9f)
    }

    fun bestSimilarity(e: FloatArray, templates: List<FloatArray>): Float = templates.maxOfOrNull { cosine(e, it) } ?: -1f

    /**
     * En küçük kareler benzerlik dönüşümü (ölçek+dönme+öteleme, yansıma yok).
     * Dönen [a, b, tx, ty]: x' = a*x - b*y + tx, y' = b*x + a*y + ty. src/dst: (x,y) çiftleri.
     */
    fun similarityTransform(src: FloatArray, dst: FloatArray): FloatArray {
        val n = src.size / 2
        var msx = 0f; var msy = 0f; var mdx = 0f; var mdy = 0f
        for (i in 0 until n) { msx += src[2 * i]; msy += src[2 * i + 1]; mdx += dst[2 * i]; mdy += dst[2 * i + 1] }
        msx /= n; msy /= n; mdx /= n; mdy /= n
        var num1 = 0f; var num2 = 0f; var den = 0f
        for (i in 0 until n) {
            val xs = src[2 * i] - msx; val ys = src[2 * i + 1] - msy
            val xd = dst[2 * i] - mdx; val yd = dst[2 * i + 1] - mdy
            num1 += xs * xd + ys * yd; num2 += xs * yd - ys * xd; den += xs * xs + ys * ys
        }
        den = den.coerceAtLeast(1e-9f)
        val a = num1 / den; val b = num2 / den
        return floatArrayOf(a, b, mdx - (a * msx - b * msy), mdy - (b * msx + a * msy))
    }

    /** İki göz ve iki ağız köşesini görüntüdeki x'e göre sıralayıp REF sırasına koyar. */
    fun orderLandmarks(eyeA: FloatArray, eyeB: FloatArray, nose: FloatArray, mouthA: FloatArray, mouthB: FloatArray): FloatArray {
        val (le, re) = if (eyeA[0] <= eyeB[0]) eyeA to eyeB else eyeB to eyeA
        val (lm, rm) = if (mouthA[0] <= mouthB[0]) mouthA to mouthB else mouthB to mouthA
        return floatArrayOf(le[0], le[1], re[0], re[1], nose[0], nose[1], lm[0], lm[1], rm[0], rm[1])
    }

    fun softmax(x: FloatArray): FloatArray {
        val m = x.max()
        val e = FloatArray(x.size) { exp(x[it] - m) }
        val s = e.sum()
        return FloatArray(x.size) { e[it] / s }
    }

    /** Canlılık modeli için yüz kutusunu `scale` kat büyütüp kare içinde tutar. Dönen: [left, top, right, bottom]. */
    fun scaledBox(srcW: Int, srcH: Int, left: Int, top: Int, w: Int, h: Int, scale: Float): IntArray {
        val s = minOf((srcH - 1f) / h, (srcW - 1f) / w, scale)
        val nw = w * s; val nh = h * s
        val cx = w / 2f + left; val cy = h / 2f + top
        var l = cx - nw / 2; var t = cy - nh / 2; var r = cx + nw / 2; var b = cy + nh / 2
        if (l < 0) { r -= l; l = 0f }
        if (t < 0) { b -= t; t = 0f }
        if (r > srcW - 1) { l -= r - (srcW - 1); r = (srcW - 1).toFloat() }
        if (b > srcH - 1) { t -= b - (srcH - 1); b = (srcH - 1).toFloat() }
        return intArrayOf(l.toInt().coerceAtLeast(0), t.toInt().coerceAtLeast(0), r.toInt(), b.toInt())
    }

    /** Şablonları bayta çevir / geri al: [adet][boyut][float...] (şifreleme ayrı katmanda). */
    fun encodeTemplates(list: List<FloatArray>): ByteArray {
        val dim = list.firstOrNull()?.size ?: 0
        val bb = java.nio.ByteBuffer.allocate(8 + list.size * dim * 4)
        bb.putInt(list.size); bb.putInt(dim)
        for (t in list) for (x in t) bb.putFloat(x)
        return bb.array()
    }

    fun decodeTemplates(bytes: ByteArray): List<FloatArray> {
        val bb = java.nio.ByteBuffer.wrap(bytes)
        val n = bb.int; val dim = bb.int
        require(n in 0..64 && dim in 1..2048 && bytes.size == 8 + n * dim * 4) { "bozuk şablon" }
        return List(n) { FloatArray(dim) { bb.float } }
    }
}
