package app.photils;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import android.util.Size;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Utils {
    public static ByteBuffer convertBitmapToByteBuffer(Bitmap bitmap, int inputSize) {

        double scale = inputSize / (double)(Math.min(bitmap.getWidth(), bitmap.getHeight()));
        double nw = Math.floor(scale * bitmap.getWidth());
        double nh = Math.floor(scale * bitmap.getHeight());
        double half = inputSize / 2.0;
        double cx = nw / 2.0;
        double cy = nh / 2.0;
        int startx = (int)(cx - half);
        int starty = (int)(cy - half);

        int[] intValues = new int[inputSize * inputSize];
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * inputSize * inputSize * 3);
        byteBuffer.order(ByteOrder.nativeOrder());

        bitmap = Bitmap.createScaledBitmap(bitmap, (int)nw, (int)nh,true);
        bitmap.getPixels(intValues, 0, inputSize, startx, starty, inputSize, inputSize);

        // Bitmap test = Bitmap.createBitmap(intValues,0,inputSize, inputSize, inputSize, Bitmap.Config.ARGB_8888);
        for(int i = 0; i < intValues.length; i++) {
            final int val = intValues[i];
            byteBuffer.putFloat(((val >> 16) & 0xFF) / 127.5f - 1.0f);
            byteBuffer.putFloat(((val >> 8) & 0xFF) / 127.5f - 1.0f);
            byteBuffer.putFloat(((val) & 0xFF) / 127.5f - 1.0f);
        }

        return byteBuffer;
    }

    public static float[][] l2_normalize(float[][] data) {
        float[][] out = new float[data.length][data[0].length];
        double epsilon = 1e-12;
        for(int i = 0; i < data.length; i++) {
            double sum = 0.0f;
            for(int j = 0; j < data[i].length; j++) {
                sum += data[i][j] * data[i][j];
            }

            float length = (float)Math.sqrt(Math.max(sum, epsilon));
            for(int j = 0; j < data[i].length; j++) {
                sum += data[i][j] * data[i][j];
                out[i][j] = data[i][j] / length;
            }

        }

        return out;
    }

    public static Bitmap rotateBitmap(Bitmap bitmap, int degrees) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    public static void shareImageIntent(Context ctx, Uri uri, String content){
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("image/*");
        share.putExtra(Intent.EXTRA_TEXT,content);
        share.putExtra(Intent.EXTRA_STREAM, uri);

        ctx.startActivity(Intent.createChooser(share, "Share to"));
    }

    public static void shareContent(Context ctx, String title, String content) {
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_TEXT,content);
        share.putExtra(Intent.EXTRA_SUBJECT, title);

        ctx.startActivity(Intent.createChooser(share, "Share to"));
    }

    // https://stackoverflow.com/questions/15440647/scaled-bitmap-maintaining-aspect-ratio
    public static Bitmap scaleBitmapAndKeepRation(Bitmap targetBmp,int reqHeightInPixels,int reqWidthInPixels)
    {
        Matrix matrix = new Matrix();
        matrix .setRectToRect(new RectF(0, 0, targetBmp.getWidth(), targetBmp.getHeight()), new RectF(0, 0, reqWidthInPixels, reqHeightInPixels), Matrix.ScaleToFit.CENTER);
        Bitmap scaledBitmap = Bitmap.createBitmap(targetBmp, 0, 0, targetBmp.getWidth(), targetBmp.getHeight(), matrix, true);
        return scaledBitmap;
    }


    /**
     * https://github.com/googlesamples/android-Camera2Basic/blob/master/Application/src/main/java/com/example/android/camera2basic/Camera2BasicFragment.java
     * Given {@code choices} of {@code Size}s supported by a camera, choose the smallest one that
     * is at least as large as the respective texture view size, and that is at most as large as the
     * respective max size, and whose aspect ratio matches with the specified value. If such size
     * doesn't exist, choose the largest one that is at most as large as the respective max size,
     * and whose aspect ratio matches with the specified value.
     *
     * @param choices           The list of sizes that the camera supports for the intended output
     *                          class
     * @param textureViewWidth  The width of the texture view relative to sensor coordinate
     * @param textureViewHeight The height of the texture view relative to sensor coordinate
     * @param maxWidth          The maximum width that can be chosen
     * @param maxHeight         The maximum height that can be chosen
     * @param aspectRatio       The aspect ratio
     * @return The optimal {@code Size}, or an arbitrary one if none were big enough
     */
    public static Size chooseOptimalSize(Size[] choices, int textureViewWidth,
                                          int textureViewHeight, int maxWidth,
                                          int maxHeight, Size aspectRatio) {

        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();
        // Collect the supported resolutions that are smaller than the preview Surface
        List<Size> notBigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getWidth() <= maxWidth && option.getHeight() <= maxHeight &&
                    option.getHeight() == option.getWidth() * h / w) {
                if (option.getWidth() >= textureViewWidth &&
                        option.getHeight() >= textureViewHeight) {
                    bigEnough.add(option);
                } else {
                    notBigEnough.add(option);
                }
            }
        }

        // Pick the smallest of those big enough. If there is no one big enough, pick the
        // largest of those not big enough.
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else if (notBigEnough.size() > 0) {
            return Collections.max(notBigEnough, new CompareSizesByArea());
        } else {
            Log.e(BuildConfig.APPLICATION_ID, "Couldn't find any suitable preview size");
            return choices[0];
        }
    }

    /**
     * Compares two {@code Size}s based on their areas.
     */
    static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }

    }

}

// https://stackoverflow.com/questions/1560788/how-to-check-internet-access-on-android-inetaddress-never-times-out?answertab=votes#tab-top
class InternetCheck extends AsyncTask<Void,Void,Boolean> {

    private Consumer mConsumer;
    public interface Consumer { void accept(Boolean internet); }

    public  InternetCheck(Consumer consumer) { mConsumer = consumer; execute(); }

    @Override protected Boolean doInBackground(Void... voids) { try {
        Socket sock = new Socket();
        sock.connect(new InetSocketAddress("8.8.8.8", 53), 1500);
        sock.close();
        return true;
    } catch (IOException e) { return false; } }

    @Override protected void onPostExecute(Boolean internet) { mConsumer.accept(internet); }
}


