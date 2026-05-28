using System;
using System.Windows.Media.Imaging;

class SampleWebp
{
    static void Main(string[] args)
    {
        var path = args[0];
        int sx = int.Parse(args[1]);
        int sy = int.Parse(args[2]);
        int w = args.Length > 3 ? int.Parse(args[3]) : 8;
        int h = args.Length > 4 ? int.Parse(args[4]) : 8;

        var uri = new Uri(path);
        var bi = new BitmapImage();
        bi.BeginInit();
        bi.UriSource = uri;
        bi.CacheOption = BitmapCacheOption.OnLoad;
        bi.EndInit();
        bi.Freeze();

        int bytesPerPixel = (bi.Format.BitsPerPixel + 7) / 8;
        int stride = bi.PixelWidth * bytesPerPixel;
        byte[] pixels = new byte[bi.PixelHeight * stride];
        bi.CopyPixels(pixels, stride, 0);

        Console.WriteLine(string.Format("Format: {0}, PixelWidth={1}, PixelHeight={2}", bi.Format, bi.PixelWidth, bi.PixelHeight));

        // Map a 1125-space coordinate to actual pixels
        int realX = (int)(sx * (double)bi.PixelWidth / 1125);
        int realY = (int)(sy * (double)bi.PixelHeight / 2000);

        for (int dy = 0; dy < h; dy++)
        {
            for (int dx = 0; dx < w; dx++)
            {
                int px = realX + dx * (int)(50 * (double)bi.PixelWidth / 1125);
                int py = realY + dy * (int)(50 * (double)bi.PixelHeight / 2000);
                if (px >= bi.PixelWidth || py >= bi.PixelHeight) continue;
                int idx = py * stride + px * bytesPerPixel;
                // BGRA32
                byte b = pixels[idx];
                byte g = pixels[idx + 1];
                byte r = pixels[idx + 2];
                Console.WriteLine(string.Format("({0,4},{1,4})  R={2,3} G={3,3} B={4,3}", px, py, r, g, b));
            }
        }
    }
}
