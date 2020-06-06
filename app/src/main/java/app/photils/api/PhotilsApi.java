package app.photils.api;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;

import org.json.JSONArray;
import org.json.JSONException;
import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import app.photils.R;

public class PhotilsApi {

    protected Interpreter mInterpreter;
    private static PhotilsApi mInstance;
    private Context mContext;
    private JSONArray mLabels;

    private PhotilsApi(Context ctx) {
        this.mContext = ctx;

        try {
            mInterpreter = new Interpreter(loadModel());
            loadLabels();
        } catch (Exception ex) {

        }
    }

    private void loadLabels() throws IOException {
        try(InputStream is = mContext.getResources().openRawResource(R.raw.labels)) {
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            mLabels = new JSONArray( new String(buffer, "UTF-8"));
        } catch(IOException | JSONException | Resources.NotFoundException ex) {

        }


    }

    public static synchronized PhotilsApi getInstance(Context ctx) {
        if(PhotilsApi.mInstance == null)
            PhotilsApi.mInstance = new PhotilsApi(ctx);

        return PhotilsApi.mInstance;
    }

    public void getTags(ByteBuffer img, OnTagsReceived callback) {
        if(mInterpreter == null) {
            callback.onFail(new ApiException(mContext.getString(R.string.api_tflite_error)));
            return;
        }

        float[][] result = new float[1][mLabels.length()];
        mInterpreter.run(img ,result);

        ArrayList<Prediction> predictions = new ArrayList<>();

        try {
            for(int i = 0; i < result[0].length; i++) {
                predictions.add(new Prediction(result[0][i], mLabels.getString(i), i));
            }
        } catch (Exception e) {
            callback.onFail(new ApiException(e.getMessage(), e.getCause()));
        }

        Collections.sort(predictions, Collections.reverseOrder());
        callback.onSuccess(predictions);
    }


    public static class ApiException extends RuntimeException {
        public ApiException(String message) {
            super(message);
        }

        public ApiException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private MappedByteBuffer loadModel() throws IOException {
        AssetFileDescriptor fd = mContext.getResources().openRawResourceFd(R.raw.model);
        FileInputStream fis = new FileInputStream(fd.getFileDescriptor());
        FileChannel channel = fis.getChannel();

        return channel.map(FileChannel.MapMode.READ_ONLY,
                fd.getStartOffset(), fd.getDeclaredLength());
    }

    public interface OnTagsReceived {
        void onSuccess(List<Prediction> tags);
        void onFail(ApiException ex);
    }

    public class Prediction implements Comparable<Prediction> {
        float mConfidence;
        String mLabel;
        int mLabelId;

        public Prediction(float confidence, String label, int tid) {
            this.mConfidence = confidence;
            this.mLabel = label;
            this.mLabelId = tid;
        }

        public float getConfidence() {
            return mConfidence;
        }

        public String getLabel() {
            return mLabel;
        }

        public int getLabelId() {
            return this.mLabelId;
        }


        @Override
        public int compareTo(Prediction o) {
            if(mConfidence == o.getConfidence())
                return 0;
            return mConfidence < o.getConfidence() ? -1 : 1;
        }
    }
}
