package app.photils;

import android.arch.core.util.Function;
import android.content.Context;
import android.util.Base64;
import android.util.Log;

import com.android.volley.Cache;
import com.android.volley.Network;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class Api {

    final static String URL = "https://api.photils.app/";

    private static Api instance;
    private RequestQueue requestQueue;
    private Context ctx;

    private Api(Context ctx) {
        this.ctx = ctx;
        requestQueue = Volley.newRequestQueue(ctx);
        requestQueue.start();
    }

    public static synchronized Api getInstance(Context ctx) {
        if(Api.instance == null)
            Api.instance = new Api(ctx);

        return Api.instance;
    }



    public void getTags(ByteBuffer buffer, final Function<List<String>, Void> callback) throws ApiException {
        String content = Base64.encodeToString(buffer.array(), Base64.DEFAULT);
        JSONObject data;


        Log.i(ctx.getPackageCodePath(), content);
        try {
            data = new JSONObject();
            data.put("feature", content);
        } catch (JSONException e) {
            throw new ApiException(e.getMessage(), e.getCause());
        }


        JsonObjectRequest req = new JsonObjectRequest(Request.Method.POST, URL + "tags",
                data,
                response -> {
                    try {
                        if (response.getString("success") == "true" && response.has("tags")) {
                            List tagList = new ArrayList();
                            JSONArray tags = response.getJSONArray("tags");
                            for(int i = 0; i < tags.length(); i++) {
                                tagList.add(tags.getString(i));
                            }

                            callback.apply(tagList);
                        }
                    } catch (JSONException e) {
                        throw new ApiException(e.getMessage(), e.getCause());
                    }

                }, error -> {
                throw new ApiException(error.getMessage(), error.getCause());
            });

        requestQueue.add(req);
    }

    public static class ApiException extends RuntimeException {
        public ApiException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
