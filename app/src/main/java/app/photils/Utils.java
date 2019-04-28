package app.photils;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.net.Uri;
import android.os.AsyncTask;

import org.rajawali3d.math.MathUtil;
import org.rajawali3d.math.vector.Vector3;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Utils {
    public static ByteBuffer convertBitmapToByteBuffer(Bitmap bitmap, int inputSize) {
        bitmap = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize,true);
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * inputSize * inputSize * 3);

        byteBuffer.order(ByteOrder.nativeOrder());
        int[] intValues = new int[inputSize * inputSize];
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        int pixel = 0;
        for (int i = 0; i < inputSize; ++i) {
            for (int j = 0; j < inputSize; ++j) {
                final int val = intValues[pixel++];
                byteBuffer.putFloat(((val) & 0xFF) - 103.939f);
                byteBuffer.putFloat(((val >> 8) & 0xFF) - 116.779f);
                byteBuffer.putFloat(((val >> 16) & 0xFF) - 123.68f);
            }
        }
        return byteBuffer;
    }

    public static Bitmap rotateBitmap(Bitmap bitmap, int degrees) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    public static void shareImageIntent(Context ctx, Uri uri, String conent){
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("image/*");
        share.putExtra(Intent.EXTRA_TEXT,conent);
        share.putExtra(Intent.EXTRA_STREAM, uri);

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

    public static Vector3 latLonToXYZ(double lat, double lon){
        final double r = 6371; // km
        lat = MathUtil.degreesToRadians(lat);
        lon = MathUtil.degreesToRadians(lon);

        final double x = r * Math.cos(lat) * Math.cos(lon);
        final double y = r * Math.cos(lat) * Math.sin(lon);
        final double z = r * Math.sin(lat);

        return new Vector3(x,z,y);
    }

    public static double angleBetweenLocations(double lat1, double lon1, double lat2, double lon2) {
        final double dLon = (lon2 - lon1);
        final double y = Math.sin(dLon) * Math.cos(lat2);
        final double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1)
                * Math.cos(lat2) * Math.cos(dLon);
        double bearing = Math.atan2(y, x);
        bearing = MathUtil.radiansToDegrees(bearing);
        bearing = (bearing + 360) % 360;
        bearing = 360 - bearing;
        return bearing;
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


