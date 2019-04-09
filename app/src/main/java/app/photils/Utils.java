package app.photils;

import android.graphics.Bitmap;

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
}
