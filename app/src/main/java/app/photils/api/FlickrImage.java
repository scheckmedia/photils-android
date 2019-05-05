package app.photils.api;

import android.graphics.Bitmap;
import android.util.Size;
import android.widget.ImageView;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.ImageRequest;

import org.json.JSONException;
import org.json.JSONObject;
import org.rajawali3d.cameras.Camera;
import org.rajawali3d.materials.Material;
import org.rajawali3d.materials.textures.ATexture;
import org.rajawali3d.materials.textures.Texture;
import org.rajawali3d.materials.textures.TextureManager;
import org.rajawali3d.math.Matrix4;
import org.rajawali3d.math.vector.Vector2;
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
    private float mAspectRation;
    private boolean mIsDirty;
    private boolean mThumbnaiLoaded;

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

    @Override
    public int hashCode() {
        return Integer.valueOf(mId);
    }

    public void setLocation(double lat, double lon) {
        mLat = lat;
        mLon = lon;
        Vector3 pos = Utils.latLonToXYZ(lat, lon);
        pos.y = 0;
        setPosition(pos);
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

    public Vector2 getLatLon() {
        return new Vector2(mLat, mLon);
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
            addTexture(bitmap);

        }, 0, 0, ImageView.ScaleType.CENTER, Bitmap.Config.RGB_565, error -> {

        });

        mRequestQueue.add(request);
    }

    public void loadThumbnail() {
        if(mThumbnaiLoaded)
            return;

        ImageRequest request = new ImageRequest(mThumbnailUrl, bitmap -> {
            addTexture(bitmap);
        }, 0, 0, ImageView.ScaleType.CENTER, Bitmap.Config.RGB_565, error -> {
        });

        mRequestQueue.add(request);
    }

    private void addTexture(Bitmap bitmap) {
        try {
            String id = getFlickrId().substring(0, 8);
            String name = "";
            for(int i = 0; i < id.length(); i++)
                name += String.valueOf((char)(97 + Integer.valueOf( String.valueOf(id.charAt(i)))));

            float width = bitmap.getWidth();
            float height = bitmap.getHeight();
            mAspectRation = Math.min(width, height) / Math.max(width, height);
            mIsDirty = true;

            Texture tex  = new Texture(name, bitmap);
            tex.setMipmap(false);
            mMaterial.addTexture(tex);
            mThumbnaiLoaded = true;

        } catch (ATexture.TextureException e) { }
    }

    @Override
    public void render(Camera camera, Matrix4 vpMatrix, Matrix4 projMatrix, Matrix4 vMatrix, Matrix4 parentMatrix, Material sceneMaterial) {
        if(isDestroyed())
            return;

        super.render(camera, vpMatrix, projMatrix, vMatrix, parentMatrix, sceneMaterial);

        if(mIsDirty) {
            Utils.fitTextureToAspect(this, mAspectRation);
            mIsDirty = false;
        }

    }

    public interface ImageSuccessListener {
        void onSuccess(Bitmap image);
    }


}
