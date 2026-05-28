// Pre-process template PNGs for the collage editor.
//   prep temp  in.png out.png  - convert light-gray placeholders to pure white, save as PNG (downscaled to 1620x2880)
//   prep thumb in.png out.jpg  - save thumbnail as JPEG quality 80 at 562x1000 (matches existing thumbs)
using System;
using System.Drawing;
using System.Drawing.Drawing2D;
using System.Drawing.Imaging;
using System.IO;

class PrepTemplate
{
    static void Main(string[] args)
    {
        if (args.Length < 3) { Console.WriteLine("usage: PrepTemplate <temp|thumb> <inPath> <outPath>"); return; }
        var mode = args[0];
        var inPath = args[1];
        var outPath = args[2];

        if (mode == "temp") DoTemp(inPath, outPath, false);
        else if (mode == "blacktemp") DoTemp(inPath, outPath, true);
        else if (mode == "thumb") DoThumb(inPath, outPath);
        else Console.WriteLine("unknown mode");
    }

    // blackMode: convert gray cells -> pure black (for MaskMode.BLACK). White borders stay opaque.
    // !blackMode: convert gray cells -> pure white (for MaskMode.WHITE).
    static void DoTemp(string inPath, string outPath, bool blackMode)
    {
        // Auto-select size & format: bigger templates with photographic content use jpg.
        bool asJpeg = outPath.EndsWith(".jpg", StringComparison.OrdinalIgnoreCase);
        int tw = asJpeg ? 1125 : 1620;
        int th = asJpeg ? 2000 : 2880;
        using (var src = Image.FromFile(inPath))
        using (var bmp = new Bitmap(tw, th, PixelFormat.Format32bppArgb))
        {
            using (var g = Graphics.FromImage(bmp))
            {
                g.InterpolationMode = InterpolationMode.HighQualityBicubic;
                g.DrawImage(src, 0, 0, tw, th);
            }

            var rect = new Rectangle(0, 0, tw, th);
            var data = bmp.LockBits(rect, ImageLockMode.ReadWrite, PixelFormat.Format32bppArgb);
            int stride = data.Stride;
            var bytes = new byte[stride * th];
            System.Runtime.InteropServices.Marshal.Copy(data.Scan0, bytes, 0, bytes.Length);

            for (int y = 0; y < th; y++)
            {
                int row = y * stride;
                for (int x = 0; x < tw; x++)
                {
                    int i = row + x * 4;
                    byte b = bytes[i], g2 = bytes[i + 1], r = bytes[i + 2];
                    int min = Math.Min(r, Math.Min(g2, b));
                    int max = Math.Max(r, Math.Max(g2, b));
                    int sat = max - min;
                    int avg = (r + g2 + b) / 3;
                    if (blackMode)
                    {
                        // Convert mid-gray placeholders (~232) to pure black; leave near-white pixels (>=245) alone.
                        if (avg >= 200 && avg < 245 && sat <= 16)
                        {
                            bytes[i] = 0; bytes[i + 1] = 0; bytes[i + 2] = 0;
                        }
                    }
                    else
                    {
                        if (avg >= 200 && sat <= 16)
                        {
                            bytes[i] = 255; bytes[i + 1] = 255; bytes[i + 2] = 255;
                        }
                    }
                }
            }

            System.Runtime.InteropServices.Marshal.Copy(bytes, 0, data.Scan0, bytes.Length);
            bmp.UnlockBits(data);

            if (asJpeg)
            {
                var encoder = GetEncoder(ImageFormat.Jpeg);
                var ep = new EncoderParameters(1);
                ep.Param[0] = new EncoderParameter(Encoder.Quality, 85L);
                using (var rgb = new Bitmap(tw, th, PixelFormat.Format24bppRgb))
                {
                    using (var g2 = Graphics.FromImage(rgb))
                    {
                        g2.Clear(Color.White);
                        g2.DrawImage(bmp, 0, 0);
                    }
                    rgb.Save(outPath, encoder, ep);
                }
            }
            else
            {
                bmp.Save(outPath, ImageFormat.Png);
            }
        }
    }

    static void DoThumb(string inPath, string outPath)
    {
        int tw = 562, th = 1000;
        using (var src = Image.FromFile(inPath))
        using (var bmp = new Bitmap(tw, th, PixelFormat.Format24bppRgb))
        {
            using (var g = Graphics.FromImage(bmp))
            {
                g.InterpolationMode = InterpolationMode.HighQualityBicubic;
                g.PixelOffsetMode = PixelOffsetMode.HighQuality;
                g.DrawImage(src, 0, 0, tw, th);
            }
            var encoder = GetEncoder(ImageFormat.Jpeg);
            var ep = new EncoderParameters(1);
            ep.Param[0] = new EncoderParameter(Encoder.Quality, 80L);
            bmp.Save(outPath, encoder, ep);
        }
    }

    static ImageCodecInfo GetEncoder(ImageFormat fmt)
    {
        foreach (var c in ImageCodecInfo.GetImageEncoders())
            if (c.FormatID == fmt.Guid) return c;
        return null;
    }
}
