package com.example.playo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/** Image decoding, orientation, scaling, and JPEG compression helpers. */
public final class ImageUtils {

    private static final int MAX_COMPRESSED_BYTES = 400 * 1024;

    private ImageUtils() {
    }

    /**
     * Compresses an image. Callers must invoke this method off the main thread.
     */
    public static byte[] compress(Context ctx, Uri uri, int maxSide, int startQuality)
            throws IOException {
        return compress(ctx, uri, maxSide, startQuality, MAX_COMPRESSED_BYTES);
    }

    public static byte[] compress(Context ctx, Uri uri, int maxSide, int startQuality,
                                  int maxBytes) throws IOException {
        Bitmap bitmap = null;
        try {
            byte[] imageBytes = readBytes(ctx, uri);
            bitmap = decodeSampledBitmap(ctx, uri, imageBytes, maxSide);
            if (bitmap == null) {
                throw new IOException("Unable to decode image");
            }

            Bitmap oriented = applyExifRotation(imageBytes, bitmap, uri, ctx);
            if (oriented != bitmap) {
                bitmap.recycle();
                bitmap = oriented;
            }

            Bitmap scaled = scaleDown(bitmap, maxSide);
            if (scaled != bitmap) {
                bitmap.recycle();
                bitmap = scaled;
            }

            int quality = Math.max(40, Math.min(100, startQuality));
            byte[] compressed = compressJpeg(bitmap, quality);
            while (compressed.length > maxBytes && quality > 40) {
                quality = Math.max(40, quality - 10);
                compressed = compressJpeg(bitmap, quality);
            }
            return compressed;
        } catch (IOException e) {
            logFailure(ctx, uri, e);
            throw e;
        } catch (Exception e) {
            logFailure(ctx, uri, e);
            throw new IOException("Unable to process image", e);
        } finally {
            if (bitmap != null && !bitmap.isRecycled()) {
                bitmap.recycle();
            }
        }
    }

    private static byte[] readBytes(Context ctx, Uri uri) throws IOException {
        try (InputStream input = ctx.getContentResolver().openInputStream(uri)) {
            if (input == null) {
                throw new IOException("Unable to open selected image");
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
            byte[] bytes = output.toByteArray();
            if (bytes.length == 0) {
                throw new IOException("Selected image is empty");
            }
            return bytes;
        }
    }

    private static Bitmap decodeSampledBitmap(Context ctx, Uri uri, byte[] imageBytes, int maxSide)
            throws IOException {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length, bounds);
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            return decodeWithImageDecoder(ctx, uri, maxSide);
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = calculateInSampleSize(bounds, maxSide);
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length, options);
    }

    private static Bitmap decodeWithImageDecoder(Context ctx, Uri uri, int maxSide)
            throws IOException {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            throw new IOException("Unsupported image format");
        }
        ImageDecoder.Source source = ImageDecoder.createSource(ctx.getContentResolver(), uri);
        return ImageDecoder.decodeBitmap(source, (decoder, info, unused) -> {
            int width = info.getSize().getWidth();
            int height = info.getSize().getHeight();
            int longestSide = Math.max(width, height);
            if (longestSide > maxSide) {
                float scale = maxSide / (float) longestSide;
                decoder.setTargetSize(Math.max(1, Math.round(width * scale)),
                        Math.max(1, Math.round(height * scale)));
            }
            decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
        });
    }

    private static int calculateInSampleSize(BitmapFactory.Options options, int maxSide) {
        int height = options.outHeight;
        int width = options.outWidth;
        int sample = 1;
        int target = Math.max(1, maxSide * 2);
        while (height / sample > target || width / sample > target) {
            sample *= 2;
        }
        return sample;
    }

    private static Bitmap applyExifRotation(byte[] imageBytes, Bitmap bitmap, Uri uri, Context ctx) {
        int orientation;
        try (InputStream input = new ByteArrayInputStream(imageBytes)) {
            orientation = new ExifInterface(input).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        } catch (Exception e) {
            Log.e("ImageUtils", "Unable to read EXIF uri=" + uri + " mime="
                    + ctx.getContentResolver().getType(uri), e);
            return bitmap;
        }

        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.postRotate(90);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.postRotate(180);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.postRotate(270);
                break;
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                matrix.setScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                matrix.setScale(1, -1);
                break;
            default:
                return bitmap;
        }
        try {
            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        } catch (Exception e) {
            Log.e("ImageUtils", "Unable to apply EXIF rotation uri=" + uri + " mime="
                    + ctx.getContentResolver().getType(uri), e);
            return bitmap;
        }
    }

    private static void logFailure(Context ctx, Uri uri, Exception exception) {
        String mime = ctx.getContentResolver().getType(uri);
        Log.e("ImageUtils", "Failed to process image uri=" + uri + " mime=" + mime, exception);
    }

    private static Bitmap scaleDown(Bitmap bitmap, int maxSide) {
        int longestSide = Math.max(bitmap.getWidth(), bitmap.getHeight());
        if (longestSide <= maxSide) {
            return bitmap;
        }
        float scale = maxSide / (float) longestSide;
        int width = Math.max(1, Math.round(bitmap.getWidth() * scale));
        int height = Math.max(1, Math.round(bitmap.getHeight() * scale));
        return Bitmap.createScaledBitmap(bitmap, width, height, true);
    }

    private static byte[] compressJpeg(Bitmap bitmap, int quality) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output);
        return output.toByteArray();
    }
}
