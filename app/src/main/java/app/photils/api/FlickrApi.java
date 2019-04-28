package app.photils.api;

import android.content.Context;
import android.os.Debug;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;

import app.photils.BuildConfig;

public class FlickrApi {
    Context mCtx;
    private RequestQueue mRequestQueue;
    private static FlickrApi mInstance;
    private final String mBaseUrl = "https://api.flickr.com/services/rest/?method=";
    private int mImagesPerPage = 500;
    private String mApiKey = "686d968ee5fac542bb420630b04d9b87";

    private FlickrApi(Context mCtx) {
        this.mCtx = mCtx;

        mRequestQueue = Volley.newRequestQueue(mCtx);
        mRequestQueue.start();
    }

    public static synchronized FlickrApi getInstance(Context ctx) {
        if(FlickrApi.mInstance == null)
            FlickrApi.mInstance = new FlickrApi(ctx);

        return FlickrApi.mInstance;
    }

    protected String buildUri(String method, String params) {
        String url = mBaseUrl + method +
                "&nojsoncallback=1" +
                "&per_page=" + mImagesPerPage +
                "&api_key="+ mApiKey +
                "&format=json&" + params;

        return url;
    }

    public FlickrImages getImagesAtLocation(double lat, double lon, double radius) {
        return new FlickrImages(lat, lon, radius);
    }

    public static class ApiException extends RuntimeException {
        public ApiException(String message) {
            super(message);
        }
        public ApiException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public interface OnImageReceived {
        void onSuccess(ArrayList<FlickrImage> images);
        void onFail(ApiException ex);
    }

    public class FlickrImages {
        private int mCurrentPage = 1;
        private float mNumPages = 0;
        private double mLat;
        private double mLon;
        private double mRadius;


        public FlickrImages(double lat, double lon, double radius) {
            mLat = lat;
            mLon = lon;
            mRadius = radius;
        }

        public int getCurrentPage() {
            return mCurrentPage;
        }


        public void getNextImages(OnImageReceived callback) {
            String params = "lat="+mLat+"&lon="+mLon+"&radius="+mRadius +
                    "&page=" + mCurrentPage + "&extras=geo,url_t,url_z,views,path_alias";
            String uri = buildUri("flickr.photos.search", params);
            Log.v(BuildConfig.APPLICATION_ID, uri);

            JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, uri, null, response -> {
                try {
                    if(!response.getString("stat").equals("ok")) {
                        callback.onFail(new ApiException(response.getString("message")));
                    }

                    JSONObject page = response.getJSONObject("photos");
                    JSONArray images = page.getJSONArray("photo");

                    ArrayList<FlickrImage> flickrImages = new ArrayList<>();
                    for(int i = 0; i < images.length(); i++) {
                        flickrImages.add(new FlickrImage(images.getJSONObject(i), mRequestQueue));
                    }

                    mCurrentPage++;
                    callback.onSuccess(flickrImages);

                } catch (JSONException e) {
                    callback.onFail(new ApiException(e.getMessage(), e.getCause()));
                }
            }, error -> {
                callback.onFail(new ApiException(error.getMessage(), error.getCause()));
            });
            mRequestQueue.add(request);
        }


    }
}
