using System;
using System.Drawing;
using System.Drawing.Imaging;

class SamplePixels
{
    static void Main(string[] args)
    {
        var path = args[0];
        using (var src = Image.FromFile(path))
        using (var bmp = new Bitmap(1125, 2000))
        {
            using (var g = Graphics.FromImage(bmp))
            {
                g.InterpolationMode = System.Drawing.Drawing2D.InterpolationMode.HighQualityBicubic;
                g.DrawImage(src, 0, 0, 1125, 2000);
            }
            // Sample a grid of points
            for (int y = 100; y < 2000; y += 200)
            {
                for (int x = 100; x < 1125; x += 200)
                {
                    var p = bmp.GetPixel(x, y);
                    Console.WriteLine(string.Format("({0,4},{1,4}): R={2,3} G={3,3} B={4,3}", x, y, p.R, p.G, p.B));
                }
            }
        }
    }
}
