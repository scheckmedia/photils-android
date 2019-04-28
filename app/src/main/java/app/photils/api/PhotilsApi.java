package app.photils.api;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.util.Base64;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

import app.photils.R;

public class PhotilsApi {

    protected Interpreter mInterpreter;

    final static String URL = "https://api.photils.app/";

    private static PhotilsApi mInstance;
    private RequestQueue mRequestQueue;
    private Context mContext;

    private PhotilsApi(Context ctx) {
        this.mContext = ctx;
        mRequestQueue = Volley.newRequestQueue(ctx);
        mRequestQueue.start();

        try {
            mInterpreter = new Interpreter(loadModel());
        } catch (Exception ex) {}
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


        float[][] result = new float[1][256];
        mInterpreter.run(img ,result);

        ByteBuffer buffer = ByteBuffer.allocate(4 * 256);
        buffer.order(ByteOrder.nativeOrder());
        buffer.asFloatBuffer().put(result[0]);


        String content = Base64.encodeToString(buffer.array(), Base64.DEFAULT);
        JSONObject data;

        try {
            data = new JSONObject();
            data.put("feature", content);
        } catch (JSONException e) {
            callback.onFail(new ApiException(e.getMessage(), e.getCause()));
            return;
        }


        JsonObjectRequest req = new JsonObjectRequest(Request.Method.POST, URL + "tags", data,
                response -> {
                    try {
                        if (response.getString("success") == "true"
                                && response.has("tags")) {
                            List tagList = new ArrayList();
                            JSONArray tags = response.getJSONArray("tags");
                            for(int i = 0; i < tags.length(); i++) {
                                tagList.add(tags.getString(i));
                            }

                            callback.onSuccess(tagList);
                        }
                    } catch (JSONException e) {
                        callback.onFail(new ApiException(e.getMessage(), e.getCause()));
                    }

                }, error -> {
            callback.onFail(new ApiException(error.getMessage(), error.getCause()));
        });

        mRequestQueue.add(req);
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
        //AssetManager mgr = mContext.getApplicationContext().getAssets();
        //AssetFileDescriptor fd = mgr.openFd(MODEL_PATH);
        AssetFileDescriptor fd = mContext.getResources().openRawResourceFd(R.raw.model);
        FileInputStream fis = new FileInputStream(fd.getFileDescriptor());
        FileChannel channel = fis.getChannel();

        return channel.map(FileChannel.MapMode.READ_ONLY,
                fd.getStartOffset(), fd.getDeclaredLength());
    }

    public interface OnTagsReceived {
        void onSuccess(List<String> tags);
        void onFail(ApiException ex);
    }
}
