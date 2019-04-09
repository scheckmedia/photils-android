package app.photils;

import android.arch.core.util.Function;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.provider.CalendarContract;
import android.util.Base64;
import android.util.Log;

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

public class Api {

    protected Interpreter tflite;

    final static String URL = "https://api.photils.app/";

    private static Api instance;
    private RequestQueue requestQueue;
    private Context ctx;

    private Api(Context ctx) throws IOException {
        this.ctx = ctx;
        requestQueue = Volley.newRequestQueue(ctx);
        requestQueue.start();

        tflite = new Interpreter(loadModel());
    }

    public static synchronized Api getInstance(Context ctx) throws IOException {
        if(Api.instance == null)
            Api.instance = new Api(ctx);

        return Api.instance;
    }

    public void getTags(ByteBuffer img, OnTagsReceived callback) {

        float[][] result = new float[1][256];
        tflite.run(img ,result);

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

        requestQueue.add(req);
    }


    public static class ApiException extends RuntimeException {
        public ApiException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private MappedByteBuffer loadModel() throws IOException {
        //AssetManager mgr = ctx.getApplicationContext().getAssets();
        //AssetFileDescriptor fd = mgr.openFd(MODEL_PATH);
        AssetFileDescriptor fd = ctx.getResources().openRawResourceFd(R.raw.model);
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
