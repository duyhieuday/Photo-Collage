// Detect pure-white (r,g,b > 240) connected components in a template.
// Output rects scaled to logic space (width=1125, height proportional).
// Matches TemplateEditorView.createMaskFromWhite threshold exactly.
// Compile: csc WhiteRegions.cs /r:PresentationCore.dll /r:WindowsBase.dll /r:System.Xaml.dll
using System;
using System.Collections.Generic;
using System.Windows.Media;
using System.Windows.Media.Imaging;

class WhiteRegions
{
    static void Main(string[] args)
    {
        if (args.Length < 1) { Console.WriteLine("usage: WhiteRegions <image> [minAreaLogic]"); return; }
        string path = args[0];
        int minAreaLogic = args.Length > 1 ? int.Parse(args[1]) : 16000;

        var bi = new BitmapImage();
        bi.BeginInit();
        bi.UriSource = new Uri(path);
        bi.CacheOption = BitmapCacheOption.OnLoad;
        bi.EndInit();
        bi.Freeze();

        var conv = new FormatConvertedBitmap(bi, PixelFormats.Bgra32, null, 0);
        conv.Freeze();
        int W = conv.PixelWidth, H = conv.PixelHeight;
        int stride = W * 4;
        byte[] px = new byte[stride * H];
        conv.CopyPixels(px, stride, 0);

        // logic-space scale factor (uniform): 1125 / W
        double scale = 1125.0 / W;
        int logicH = (int)Math.Round(H * scale);

        // white mask
        bool[] white = new bool[W * H];
        for (int y = 0; y < H; y++)
        {
            int row = y * stride;
            int wr = y * W;
            for (int x = 0; x < W; x++)
            {
                int i = row + x * 4;
                if (px[i] > 240 && px[i + 1] > 240 && px[i + 2] > 240) white[wr + x] = true;
            }
        }

        // min area in native pixels
        long minAreaNative = (long)(minAreaLogic / (scale * scale));

        bool[] visited = new bool[W * H];
        var stackX = new Stack<int>();
        var stackY = new Stack<int>();
        var results = new List<int[]>(); // x1,y1,x2,y2,px (native)

        for (int y = 0; y < H; y++)
        {
            for (int x = 0; x < W; x++)
            {
                int k = y * W + x;
                if (!white[k] || visited[k]) continue;
                int minX = x, maxX = x, minY = y, maxY = y, cnt = 0;
                stackX.Push(x); stackY.Push(y); visited[k] = true;
                while (stackX.Count > 0)
                {
                    int cx = stackX.Pop(); int cy = stackY.Pop(); cnt++;
                    if (cx < minX) minX = cx; if (cx > maxX) maxX = cx;
                    if (cy < minY) minY = cy; if (cy > maxY) maxY = cy;
                    if (cx > 0) { int nk = k0(cx - 1, cy, W); if (white[nk] && !visited[nk]) { visited[nk] = true; stackX.Push(cx - 1); stackY.Push(cy); } }
                    if (cx < W - 1) { int nk = k0(cx + 1, cy, W); if (white[nk] && !visited[nk]) { visited[nk] = true; stackX.Push(cx + 1); stackY.Push(cy); } }
                    if (cy > 0) { int nk = k0(cx, cy - 1, W); if (white[nk] && !visited[nk]) { visited[nk] = true; stackX.Push(cx); stackY.Push(cy - 1); } }
                    if (cy < H - 1) { int nk = k0(cx, cy + 1, W); if (white[nk] && !visited[nk]) { visited[nk] = true; stackX.Push(cx); stackY.Push(cy + 1); } }
                }
                long area = (long)(maxX - minX + 1) * (maxY - minY + 1);
                if (cnt >= minAreaNative)
                    results.Add(new int[] { minX, minY, maxX, maxY, cnt });
            }
        }

        // sort top-to-bottom then left-to-right (row tolerance ~ 80 logic px)
        results.Sort((a, b) =>
        {
            int ay = (int)(a[1] * scale), by = (int)(b[1] * scale);
            if (Math.Abs(ay - by) > 80) return ay.CompareTo(by);
            return ((int)(a[0] * scale)).CompareTo((int)(b[0] * scale));
        });

        Console.WriteLine("# logicH=" + logicH + " count=" + results.Count + " (native " + W + "x" + H + ")");
        foreach (var r in results)
        {
            int x1 = (int)Math.Round(r[0] * scale);
            int y1 = (int)Math.Round(r[1] * scale);
            int x2 = (int)Math.Round((r[2] + 1) * scale);
            int y2 = (int)Math.Round((r[3] + 1) * scale);
            long bbox = (long)(r[2] - r[0] + 1) * (r[3] - r[1] + 1);
            double fill = (double)r[4] / bbox;
            Console.WriteLine(string.Format("RectF({0}f, {1}f, {2}f, {3}f),   fill={4:0.00}", x1, y1, x2, y2, fill));
        }
    }

    static int k0(int x, int y, int W) { return y * W + x; }
}
