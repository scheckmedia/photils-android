package app.photils;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Utils {

    public static ByteBuffer convertBitmapToByteBuffer(Bitmap bitmap, int inputSize) {
        bitmap = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize,false);
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * inputSize * inputSize * 3);

        byteBuffer.order(ByteOrder.nativeOrder());
        int[] intValues = new int[inputSize * inputSize];
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        int pixel = 0;
        for (int i = 0; i < inputSize; ++i) {
            for (int j = 0; j < inputSize; ++j) {
                final int val = intValues[pixel++];
                byteBuffer.putFloat(((val >> 16) & 0xFF) - 103.939f);
                byteBuffer.putFloat(((val >> 8) & 0xFF) - 116.779f);
                byteBuffer.putFloat(((val) & 0xFF) - 123.68f);
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
}

