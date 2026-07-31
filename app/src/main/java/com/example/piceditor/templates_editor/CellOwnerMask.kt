package com.example.piceditor.templates_editor

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.RectF
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Chia "quyen so huu" tung vung trong suot cua mask cho dung o anh.
 *
 * VAN DE: mask theo mau la mask TOAN CUC — no choc thung MOI pixel dung dai mau nam trong
 * cellRect, khong quan tam pixel do thuoc khung nao. Voi cac o co rect CHONG NHAU (khung tron
 * bd11, blob xep doc sm14, tem nghieng sm02...) thi o ve SAU dam anh cua no vao phan slot cua o
 * ve truoc. Thay ro tren may: sm14 blob tren bi xen ngang mot duong THANG dung o dinh rect cua
 * blob duoi, phan duoi hien anh CUA O KIA.
 *
 * CACH GIAI: voi moi o, dung them mot mask rieng chi cho phep anh hien o nhung pixel THUOC SLOT
 * CUA CHINH O DO. Quyen so huu tinh bang loang (BFS) TRONG vung trong suot:
 *  - pixel chi nam trong DUNG MOT rect  -> chac chan thuoc o do, lam nguon loang;
 *  - pixel nam trong NHIEU rect         -> thuoc o nao loang toi truoc (di trong vung trong suot,
 *                                          nen khong the nhay qua khung khac);
 *  - pixel khong nam trong rect nao     -> khong o nao ve, bo qua.
 * Loang theo vung lien thong nen dung ca khi hai slot LONG VAO NHAU theo truc y (sm14: mui blob
 * tren thop xuong giua hai tai blob duoi) — cat theo toa do khong the tach duoc.
 *
 * Chi tao mask cho o NAO THUC SU bi tranh chap; o khong tranh chap tra ve null va giu nguyen
 * duong ve cu. Tinh o do phan giai 1/2 cho nhe, mask duoc ve co gian lai nen khong lech.
 */
object CellOwnerMask {

    /** Buoc luoi phan tich: 2 = mot nua do phan giai template. */
    private const val STEP = 2

    /** Co lai bay nhieu pixel truoc khi chia quyen so huu — bang dung muc mask da no rong. */
    private const val ERODE_PX = 2

    /** Ket qua cho mot o: bitmap alpha + o vuong (toa do logic) de ve. */
    class Result(val mask: Bitmap, val rect: RectF)

    /**
     * true neu co it nhat 2 o co hop chu nhat (da tinh goc xoay) giao nhau HOAC ke sat nhau.
     *
     * Phai tinh ca "ke sat": luoi manh ghep (sm07) co cac rect dung canh vao nhau dung khit, ma
     * rang cua manh ghep thi THO qua ranh sang o ben - dung ngay do anh o nay dam sang o kia.
     */
    fun anyOverlap(rects: List<RectF>, angles: List<Float>): Boolean {
        val pad = 8f
        for (i in rects.indices) {
            val a = bounds(rects[i], angles.getOrElse(i) { 0f })
            a.inset(-pad, -pad)
            for (j in i + 1 until rects.size) {
                val b = bounds(rects[j], angles.getOrElse(j) { 0f })
                if (RectF.intersects(a, b)) return true
            }
        }
        return false
    }

    /**
     * @param maskBmp mask da dung (pixel trong suot = cho anh hien), kich thuoc = khong gian logic
     * @return list cung do dai voi [rects]; null = o do khong tranh chap, ve nhu cu
     */
    fun build(maskBmp: Bitmap, rects: List<RectF>, angles: List<Float>): List<Result?> {
        val n = rects.size
        if (n < 2) return List(n) { null }

        val bw = maskBmp.width
        val bh = maskBmp.height
        val gw = bw / STEP
        val gh = bh / STEP
        if (gw < 4 || gh < 4) return List(n) { null }

        // --- vung trong suot cua mask, CO LAI (erode) truoc khi chia quyen so huu ---
        //
        // Mask da duoc no rong ERODE_PX pixel de an vanh anti-alias (xem dilateTransparent).
        // Nhung chinh cai no rong do lam hai slot ke nhau DINH LIEN qua vach trang mong -> thanh
        // mot vung lien thong -> loang chia quyen so huu bi lan, anh o nay thoc sang o kia (da
        // thay o sm14: mang lime thoc xuong blob teal).
        // Nen o day co lai dung bay nhieu: o luoi chi tinh la trong suot khi CA cua so
        // (STEP + 2*ERODE_PX)^2 pixel goc deu trong suot. Chi dung cho viec chia quyen so huu -
        // luc VE van dung mask da no rong, nen khong sinh lo o mep.
        //
        // Doc alpha vao mot mang byte (1 byte/pixel) roi moi quet: doc thang tung dong bang
        // getPixels thi phai dem theo nhieu dong mot luc, con tao THEM mot Bitmap mask nua thi
        // HET BO NHO (da thu, app chet ngay khi mo template).
        val alpha = ByteArray(bw * bh)
        run {
            val row = IntArray(bw)
            for (y in 0 until bh) {
                maskBmp.getPixels(row, 0, bw, 0, y, bw, 1)
                val base = y * bw
                for (x in 0 until bw) alpha[base + x] = if (Color.alpha(row[x]) == 0) 1 else 0
            }
        }
        val open = BooleanArray(gw * gh)
        for (gy in 0 until gh) {
            val y0 = max(0, gy * STEP - ERODE_PX)
            val y1 = min(bh - 1, gy * STEP + STEP - 1 + ERODE_PX)
            val base = gy * gw
            for (gx in 0 until gw) {
                val x0 = max(0, gx * STEP - ERODE_PX)
                val x1 = min(bw - 1, gx * STEP + STEP - 1 + ERODE_PX)
                var ok = true
                var y = y0
                loop@ while (y <= y1) {
                    val rowBase = y * bw
                    var x = x0
                    while (x <= x1) {
                        if (alpha[rowBase + x].toInt() == 0) { ok = false; break@loop }
                        x++
                    }
                    y++
                }
                open[base + gx] = ok
            }
        }

        // --- chu so huu ban dau: -1 khong o nao phu, -2 nhieu o (cho loang quyet) ---
        val owner = IntArray(gw * gh) { -1 }
        val queue = IntArray(gw * gh)
        var qHead = 0
        var qTail = 0

        val cosA = FloatArray(n)
        val sinA = FloatArray(n)
        for (i in 0 until n) {
            val rad = Math.toRadians(-angles.getOrElse(i) { 0f }.toDouble())
            cosA[i] = cos(rad).toFloat()
            sinA[i] = sin(rad).toFloat()
        }

        for (gy in 0 until gh) {
            val ly = (gy * STEP + 0.5f)
            val base = gy * gw
            for (gx in 0 until gw) {
                val idx = base + gx
                if (!open[idx]) continue
                val lx = (gx * STEP + 0.5f)
                var hit = -1
                var count = 0
                for (i in 0 until n) {
                    if (contains(rects[i], cosA[i], sinA[i], lx, ly)) {
                        hit = i; count++
                        if (count > 1) break
                    }
                }
                when (count) {
                    0 -> owner[idx] = -1
                    1 -> { owner[idx] = hit; queue[qTail++] = idx }
                    else -> owner[idx] = -2
                }
            }
        }

        // --- loang trong vung trong suot de chia not cac pixel bi nhieu o phu ---
        while (qHead < qTail) {
            val cur = queue[qHead++]
            val own = owner[cur]
            val cy = cur / gw
            val cx = cur - cy * gw
            for (dy in -1..1) {
                val ny = cy + dy
                if (ny < 0 || ny >= gh) continue
                for (dx in -1..1) {
                    if (dx == 0 && dy == 0) continue
                    val nx = cx + dx
                    if (nx < 0 || nx >= gw) continue
                    val ni = ny * gw + nx
                    if (open[ni] && owner[ni] == -2) {
                        owner[ni] = own
                        queue[qTail++] = ni
                    }
                }
            }
        }

        // --- vung NEN (khong phai slot) -> cat khoi MOI o ---
        // Template co nen cung mau voi o (giay nhan bd11 trang, net xoan trang sp12) thi ca nen
        // dinh thanh mot vung trong suot khong lo. Pixel nen nao roi vao rect cua mot o se an
        // mau cua o do -> lot vet mau ra ngoai khung. Vung nao khong o nao phu duoc >= 50% thi
        // khong phai slot: cat het.
        run {
            val comp = IntArray(gw * gh) { -1 }
            val q = IntArray(gw * gh)
            var nComp = 0
            val compSize = ArrayList<Int>()
            for (seed in 0 until gw * gh) {
                if (!open[seed] || comp[seed] >= 0) continue
                var head = 0; var tail = 0
                q[tail++] = seed; comp[seed] = nComp
                var size = 0
                while (head < tail) {
                    val cur = q[head++]; size++
                    val cy = cur / gw; val cx = cur - cy * gw
                    for (dy in -1..1) {
                        val ny = cy + dy
                        if (ny < 0 || ny >= gh) continue
                        for (dx in -1..1) {
                            val nx = cx + dx
                            if (nx < 0 || nx >= gw) continue
                            val ni = ny * gw + nx
                            if (open[ni] && comp[ni] < 0) { comp[ni] = nComp; q[tail++] = ni }
                        }
                    }
                }
                compSize.add(size); nComp++
            }
            if (nComp > 0) {
                val cov = Array(nComp) { IntArray(n) }
                for (idx in 0 until gw * gh) {
                    if (!open[idx]) continue
                    val gy = idx / gw; val gx = idx - gy * gw
                    val lx = (gx * STEP + 0.5f); val ly = (gy * STEP + 0.5f)
                    val cc = comp[idx]
                    for (i in 0 until n) if (contains(rects[i], cosA[i], sinA[i], lx, ly)) cov[cc][i]++
                }
                val isBg = BooleanArray(nComp)
                for (c in 0 until nComp) {
                    var best = 0
                    for (i in 0 until n) if (cov[c][i] > best) best = cov[c][i]
                    if (best.toFloat() / compSize[c] < 0.5f) isBg[c] = true
                }
                for (idx in 0 until gw * gh) {
                    if (open[idx] && isBg[comp[idx]]) owner[idx] = -3
                }
            }
        }

        // --- slot nam GON trong vung chong: khong co nguon de loang -> giao cho o co tam gan nhat ---
        for (gy in 0 until gh) {
            val base = gy * gw
            for (gx in 0 until gw) {
                val idx = base + gx
                if (owner[idx] != -2) continue
                val lx = (gx * STEP + 0.5f); val ly = (gy * STEP + 0.5f)
                var bestI = -1
                var bestD = Float.MAX_VALUE
                for (i in 0 until n) {
                    if (!contains(rects[i], cosA[i], sinA[i], lx, ly)) continue
                    val d = hypot(lx - rects[i].centerX(), ly - rects[i].centerY())
                    if (d < bestD) { bestD = d; bestI = i }
                }
                owner[idx] = bestI
            }
        }

        // --- dung mask cho tung o (chi khi o do that su bi o khac dam vao / dam sang o khac) ---
        val out = ArrayList<Result?>(n)
        for (i in 0 until n) {
            val bb = bounds(rects[i], angles.getOrElse(i) { 0f })
            val x0 = max(0, (bb.left / STEP).toInt())
            val y0 = max(0, (bb.top / STEP).toInt())
            val x1 = min(gw - 1, (bb.right / STEP).toInt() + 1)
            val y1 = min(gh - 1, (bb.bottom / STEP).toInt() + 1)
            if (x1 <= x0 || y1 <= y0) { out.add(null); continue }

            var conflict = false
            for (gy in y0..y1) {
                val base = gy * gw
                for (gx in x0..x1) {
                    val idx = base + gx
                    if (!open[idx]) continue
                    val own = owner[idx]
                    // -3 = vung nen: cung phai cat, nen tinh la tranh chap.
                    if ((own == -3 || (own >= 0 && own != i)) &&
                        contains(rects[i], cosA[i], sinA[i], (gx * STEP + 0.5f), (gy * STEP + 0.5f))
                    ) { conflict = true; break }
                }
                if (conflict) break
            }
            if (!conflict) { out.add(null); continue }

            val mw = x1 - x0 + 1
            val mh = y1 - y0 + 1
            val px = IntArray(mw * mh)
            for (gy in y0..y1) {
                val src = gy * gw
                val dst = (gy - y0) * mw
                for (gx in x0..x1) {
                    val idx = src + gx
                    // Giu (mo) o cho khong phai vung trong suot — vi o do mask tong da che roi.
                    // owner == -3 la vung NEN: cat khoi moi o.
                    val keep = !open[idx] || owner[idx] == i || owner[idx] == -1
                    px[dst + (gx - x0)] = if (keep) Color.BLACK else Color.TRANSPARENT
                }
            }
            val bmp = Bitmap.createBitmap(px, mw, mh, Bitmap.Config.ARGB_8888)
            val rect = RectF(
                (x0 * STEP).toFloat(), (y0 * STEP).toFloat(),
                ((x1 + 1) * STEP).toFloat(), ((y1 + 1) * STEP).toFloat()
            )
            out.add(Result(bmp, rect))
        }
        return out
    }

    /** Hop chu nhat cua rect sau khi xoay quanh tam. */
    private fun bounds(r: RectF, deg: Float): RectF {
        if (deg == 0f) return RectF(r)
        val rad = Math.toRadians(deg.toDouble())
        val c = cos(rad).toFloat(); val s = sin(rad).toFloat()
        val cx = r.centerX(); val cy = r.centerY()
        val hw = r.width() / 2f; val hh = r.height() / 2f
        val ex = Math.abs(hw * c) + Math.abs(hh * s)
        val ey = Math.abs(hw * s) + Math.abs(hh * c)
        return RectF(cx - ex, cy - ey, cx + ex, cy + ey)
    }

    /** Hit-test rect co xoay — dung y het TemplateEditorView.cellContains. */
    private fun contains(r: RectF, cosA: Float, sinA: Float, lx: Float, ly: Float): Boolean {
        if (cosA == 1f && sinA == 0f) return r.contains(lx, ly)
        val cx = r.centerX(); val cy = r.centerY()
        val ox = lx - cx; val oy = ly - cy
        val rx = cx + (ox * cosA - oy * sinA)
        val ry = cy + (ox * sinA + oy * cosA)
        return r.contains(rx, ry)
    }
}
