// Compile with: csc DetectCells.cs (uses System.Drawing).
// Detects rectangular photo-cells in a template image.
// Strategy: downsample to TARGET (1125x2000), build a binary mask of pixels
// that are "cell pixels" (light-gray placeholder OR pixels surrounded by background),
// run connected-component labeling, keep large rectangle-shaped components,
// emit bounding boxes scaled to 1125x2000 coordinates.

using System;
using System.Collections.Generic;
using System.Drawing;
using System.Drawing.Imaging;
using System.IO;

class DetectCells
{
    const int TW = 1125;
    const int TH = 2000;

    static int closeRadius = 2;

    static void Main(string[] args)
    {
        if (args.Length < 1) { Console.WriteLine("usage: DetectCells <imagePath> [mode] [closeRadius]"); return; }
        var path = args[0];
        var mode = args.Length > 1 ? args[1] : "auto"; // auto / gray / photo / white / diff / light
        if (args.Length > 2) int.TryParse(args[2], out closeRadius);

        using (var src = Image.FromFile(path))
        using (var bmp = new Bitmap(TW, TH))
        {
            using (var g = Graphics.FromImage(bmp))
            {
                g.InterpolationMode = System.Drawing.Drawing2D.InterpolationMode.HighQualityBicubic;
                g.DrawImage(src, 0, 0, TW, TH);
            }
            var mask = BuildMask(bmp, mode);
            var rects = FindRects(mask, TW, TH);
            // Expand each rect by 8 px on each side, clamp to [0, TW]/[0,TH]
            foreach (var r in rects)
            {
                int x1 = Math.Max(0, r.Left - 8);
                int y1 = Math.Max(0, r.Top - 8);
                int x2 = Math.Min(TW, r.Right + 8);
                int y2 = Math.Min(TH, r.Bottom + 8);
                Console.WriteLine(string.Format("RectF({0}f, {1}f, {2}f, {3}f),", x1, y1, x2, y2));
            }
        }
    }

    static bool[,] BuildMask(Bitmap bmp, string mode)
    {
        var data = bmp.LockBits(new Rectangle(0, 0, TW, TH), ImageLockMode.ReadOnly, PixelFormat.Format32bppArgb);
        int stride = data.Stride;
        var bytes = new byte[stride * TH];
        System.Runtime.InteropServices.Marshal.Copy(data.Scan0, bytes, 0, bytes.Length);
        bmp.UnlockBits(data);

        var mask = new bool[TW, TH];

        // Sample background color from corners (top-left/top-right/bottom-left/bottom-right 30x30 patches).
        int bgR, bgG, bgB;
        SampleBackground(bytes, stride, out bgR, out bgG, out bgB);

        // Modes:
        //  gray  - light-gray placeholders on white bg (Temp_11)
        //  photo - any non-white-bg content (Temp_18/19/20)
        //  white - white photo slots on colored bg (Temp_12)
        //  diff  - anything visually different from sampled background color (catch-all)
        bool wantGray = (mode == "gray" || mode == "auto");
        bool wantPhoto = (mode == "photo" || mode == "auto");
        bool wantWhite = (mode == "white");
        bool wantDiff = (mode == "diff");
        bool wantLight = (mode == "light"); // light = white OR light-gray placeholder

        for (int y = 0; y < TH; y++)
        {
            int row = y * stride;
            for (int x = 0; x < TW; x++)
            {
                int idx = row + x * 4;
                byte b = bytes[idx];
                byte gr = bytes[idx + 1];
                byte r = bytes[idx + 2];
                byte a = bytes[idx + 3];

                int min = Math.Min(r, Math.Min(gr, b));
                int max = Math.Max(r, Math.Max(gr, b));
                int sat = max - min;
                int avg = (r + gr + b) / 3;

                bool isPureWhite = (r >= 248 && gr >= 248 && b >= 248);
                bool isNearWhite = (r >= 240 && gr >= 240 && b >= 240);

                bool isGrayCell = wantGray && (avg >= 215 && avg <= 248 && sat <= 12 && !isPureWhite);
                bool isPhotoPx = wantPhoto && !isPureWhite && a > 8;
                bool isWhiteCell = wantWhite && isNearWhite;

                int dr = r - bgR, dg = gr - bgG, db = b - bgB;
                int distSq = dr * dr + dg * dg + db * db;
                bool isDiff = wantDiff && (distSq > 60 * 60);

                bool isLightCell = wantLight && (avg >= 215 && sat <= 14);

                if (isGrayCell || isPhotoPx || isWhiteCell || isDiff || isLightCell) mask[x, y] = true;
            }
        }

        if (closeRadius > 0)
        {
            mask = Dilate(mask, TW, TH, closeRadius);
            mask = Erode(mask, TW, TH, closeRadius);
        }

        return mask;
    }

    static void SampleBackground(byte[] bytes, int stride, out int r, out int g, out int b)
    {
        long sumR = 0, sumG = 0, sumB = 0;
        int n = 0;
        int[][] patches = new int[][] {
            new int[]{0, 0, 30, 30},
            new int[]{TW - 30, 0, TW, 30},
            new int[]{0, TH - 30, 30, TH},
            new int[]{TW - 30, TH - 30, TW, TH}
        };
        foreach (var p in patches)
        {
            for (int y = p[1]; y < p[3]; y++)
            {
                int row = y * stride;
                for (int x = p[0]; x < p[2]; x++)
                {
                    int idx = row + x * 4;
                    sumB += bytes[idx];
                    sumG += bytes[idx + 1];
                    sumR += bytes[idx + 2];
                    n++;
                }
            }
        }
        r = (int)(sumR / n);
        g = (int)(sumG / n);
        b = (int)(sumB / n);
    }

    static bool[,] Dilate(bool[,] m, int w, int h, int r)
    {
        var o = new bool[w, h];
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
            {
                bool any = false;
                for (int dy = -r; dy <= r && !any; dy++)
                    for (int dx = -r; dx <= r && !any; dx++)
                    {
                        int nx = x + dx, ny = y + dy;
                        if (nx >= 0 && ny >= 0 && nx < w && ny < h && m[nx, ny]) any = true;
                    }
                o[x, y] = any;
            }
        return o;
    }
    static bool[,] Erode(bool[,] m, int w, int h, int r)
    {
        var o = new bool[w, h];
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
            {
                bool all = true;
                for (int dy = -r; dy <= r && all; dy++)
                    for (int dx = -r; dx <= r && all; dx++)
                    {
                        int nx = x + dx, ny = y + dy;
                        if (nx < 0 || ny < 0 || nx >= w || ny >= h || !m[nx, ny]) all = false;
                    }
                o[x, y] = all;
            }
        return o;
    }

    static List<Rectangle> FindRects(bool[,] mask, int w, int h)
    {
        var visited = new bool[w, h];
        var results = new List<Rectangle>();

        // Iterative flood fill (BFS) to find connected components.
        for (int y = 0; y < h; y++)
        {
            for (int x = 0; x < w; x++)
            {
                if (!mask[x, y] || visited[x, y]) continue;
                int minX = x, maxX = x, minY = y, maxY = y;
                int count = 0;
                var stackX = new Stack<int>();
                var stackY = new Stack<int>();
                stackX.Push(x); stackY.Push(y);
                visited[x, y] = true;
                while (stackX.Count > 0)
                {
                    int cx = stackX.Pop(); int cy = stackY.Pop();
                    count++;
                    if (cx < minX) minX = cx;
                    if (cx > maxX) maxX = cx;
                    if (cy < minY) minY = cy;
                    if (cy > maxY) maxY = cy;
                    int[] dx = { -1, 1, 0, 0 };
                    int[] dy = { 0, 0, -1, 1 };
                    for (int i = 0; i < 4; i++)
                    {
                        int nx = cx + dx[i]; int ny = cy + dy[i];
                        if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue;
                        if (!mask[nx, ny] || visited[nx, ny]) continue;
                        visited[nx, ny] = true;
                        stackX.Push(nx); stackY.Push(ny);
                    }
                }
                int rw = maxX - minX + 1;
                int rh = maxY - minY + 1;
                if (rw < 180 || rh < 180) continue;          // skip small noise
                if (count < rw * rh * 0.45) continue;        // skip non-rectangular
                results.Add(new Rectangle(minX, minY, rw, rh));
            }
        }
        // Sort top-to-bottom, then left-to-right.
        results.Sort((a, b) => {
            // Group into rows of ~80 px tolerance
            int yDiff = Math.Abs(a.Top - b.Top);
            if (yDiff > 80) return a.Top.CompareTo(b.Top);
            return a.Left.CompareTo(b.Left);
        });
        return results;
    }
}
