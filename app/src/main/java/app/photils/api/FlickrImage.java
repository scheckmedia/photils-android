package app.photils.api;

import android.graphics.Bitmap;
import android.widget.ImageView;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.ImageRequest;

import org.json.JSONException;
import org.json.JSONObject;
import org.rajawali3d.materials.Material;
import org.rajawali3d.materials.textures.ATexture;
import org.rajawali3d.materials.textures.Texture;
import org.rajawali3d.math.vector.Vector3;
import org.rajawali3d.primitives.Plane;

import app.photils.Utils;

public class FlickrImage extends Plane {
    private String mId;
    private String mTitle;
    private String mThumbnailUrl;
    private String mImageUrl;
    private String mDetailsUrl;
    private double mLat;
    private double mLon;
    private RequestQueue mRequestQueue;
    private Material mMaterial;

    public FlickrImage(JSONObject image, RequestQueue queue) throws JSONException {
        super(1,1,1,1);
        mRequestQueue = queue;
        mId = image.getString("id");
        mTitle = image.getString("title");
        mThumbnailUrl = image.getString("url_t");
        mImageUrl = image.getString("url_z");
        setLocation(image.getDouble("latitude"), image.getDouble("longitude"));

        mMaterial = new Material();
        mMaterial.setColorInfluence(0);
        setMaterial(mMaterial);

    }

    public void setLocation(double lat, double lon) {
        mLat = lat;
        mLon = lon;
        setPosition(Utils.latLonToXYZ(lat, lon));
    }

    public String getFlickrId() {
        return mId;
    }

    public String getTitle() {
        return mTitle;
    }

    public String getThumbnailUrl() {
        return mThumbnailUrl;
    }

    public String getImageUrl() {
        return mImageUrl;
    }

    public String getDetailsUrl() {
        return mDetailsUrl;
    }

    public double getLat() {
        return mLat;
    }

    public double getLon() {
        return mLon;
    }

    public Vector3 getWorldPosition() {
        return mPosition;
    }

    public void loadImage() {
        ImageRequest request = new ImageRequest(mImageUrl, bitmap -> {


            float width = (float)bitmap.getWidth();
            float heigth = (float)bitmap.getHeight();

            if(width > heigth) {
                width = width / heigth;
                heigth = 1;
            } else {
                heigth = heigth / width;
                width = 1;
            }


            setScale(new Vector3(width, heigth, 1));

            try {
                String id = getFlickrId().substring(0, 8);
                String name = "";
                for(int i = 0; i < id.length(); i++)
                    name += String.valueOf((char)(97 + Integer.valueOf( String.valueOf(id.charAt(i)))));

                Texture tex  = new Texture(name, bitmap);
                tex.setMipmap(false);
                mMaterial.addTexture(tex);

            } catch (ATexture.TextureException e) { }
        }, 0, 0, ImageView.ScaleType.CENTER, Bitmap.Config.RGB_565, error -> {

        });

        mRequestQueue.add(request);
    }

    public interface ImageSuccessListener {
        void onSuccess(Bitmap image);
    }
}
