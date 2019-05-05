package app.photils.inspiration;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.opengl.GLES20;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.util.Size;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.animation.LinearInterpolator;

import org.rajawali3d.Object3D;
import org.rajawali3d.animation.Animation;
import org.rajawali3d.animation.Animation3D;
import org.rajawali3d.animation.IAnimationListener;
import org.rajawali3d.animation.TranslateAnimation3D;
import org.rajawali3d.cameras.Camera;
import org.rajawali3d.materials.Material;
import org.rajawali3d.materials.textures.ATexture;
import org.rajawali3d.materials.textures.StreamingTexture;
import org.rajawali3d.materials.textures.Texture;
import org.rajawali3d.math.MathUtil;
import org.rajawali3d.math.Quaternion;
import org.rajawali3d.math.vector.Vector2;
import org.rajawali3d.math.vector.Vector3;
import org.rajawali3d.primitives.PointSprite;
import org.rajawali3d.primitives.ScreenQuad;
import org.rajawali3d.renderer.Renderer;
import org.rajawali3d.scene.Scene;
import org.rajawali3d.util.OnObjectPickedListener;
import org.rajawali3d.util.RayPicker;

import java.util.ArrayList;
import java.util.Arrays;

import javax.microedition.khronos.opengles.GL10;

import app.photils.BuildConfig;
import app.photils.R;
import app.photils.Utils;
import app.photils.api.FlickrImage;

public class InspirationRenderer extends Renderer
        implements StreamingTexture.ISurfaceListener,
        OnObjectPickedListener
{
    private static final int MAX_PREVIEW_WIDTH = 1920;
    private static final int MAX_PREVIEW_HEIGHT = 1080;
    private Context mCtx;
    private Quaternion mCameraOrientation = new Quaternion();
    private final Object mCameraOrientationLock = new Object();
    private final float mAngleSteps = 45; // divide circle into 8 pieces
    private int mSensorOrientation = 0;
    private CameraManager mCameraManager;
    private CameraDevice mCameraDevice;
    private StreamingTexture mCameraTexture;
    private Handler mHandler;
    private boolean mVideoPlaneDirty = false;

    private Camera mWorldCamera;
    private Camera mGalleryCamera;
    private Material mVideoMaterial;
    private Size mPrefSize;

    private Scene mWorldScene;
    private Scene mGalleryScene;
    private Texture mApertureTexture;

    private ScreenQuad mVideoPlane;
    private Vector2 mCenter = new Vector2();
    private final float mGazeTime = 1000.0f;
    private RayPicker mPicker;
    private Object3D mFocusingObject;
    private InspirationListener mListener;
    private long mFocusStartTime;
    private boolean mActiveGaze = false;
    private boolean mIsWorldScene = true;
    private Vector2 mCamLatLon = new Vector2();
    private Object3D mGalleryImages = new Object3D("images");
    private ArrayList<ArrayList<FlickrImage>> mGroupedImages = new ArrayList<>();

    private GestureDetector mDetector;

    SwipeUpListener mSwipeUpListener = new SwipeUpListener() {
        @Override
        public void onSwipeUp() {
            Log.v(BuildConfig.APPLICATION_ID, "Swipe Up!");
            onBackButton();
        }
    };

    CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            mCameraDevice = camera;
            mCameraTexture = new StreamingTexture(
                    "videoTexture", InspirationRenderer.this::setSurface);
            try {
                mVideoMaterial.addTexture(mCameraTexture);
            } catch (ATexture.TextureException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            camera.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            camera.close();
            mCameraDevice = null;
        }
    };

    public InspirationRenderer(Context context) {
        super(context);
        mCtx = context;


        SwipeUpGesture gesture = new SwipeUpGesture(mSwipeUpListener);
        mDetector = new GestureDetector(mCtx, gesture);

        mHandler = new Handler(Looper.getMainLooper());
        mCameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        mWorldCamera = getCurrentCamera();
        mWorldCamera.setNearPlane(0.1);
        mPicker = new RayPicker(this);
        mPicker.setOnObjectPickedListener(this);
        setFrameRate(60);

        mWorldScene = getCurrentScene();
        mGalleryScene = new Scene(this);
        mGalleryCamera = mWorldCamera.clone();
        mGalleryCamera.disableLookAt();
        mGalleryCamera.setPosition(Vector3.ZERO);
        mGalleryScene.addAndSwitchCamera(mGalleryCamera);

        addScene(mGalleryScene);

        mApertureTexture = new Texture("aperture_sprite", R.drawable.aperture_sprite);

        for(int i = 0; i < 360; i += mAngleSteps)
            mGroupedImages.add(new ArrayList<>());
    }

    private void initCamera() {
        try {
            for (String cid : mCameraManager.getCameraIdList()) {
                CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(cid);
                mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                if (characteristics.get(CameraCharacteristics.LENS_FACING) ==
                        CameraCharacteristics.LENS_FACING_BACK) {

                    StreamConfigurationMap streamConfigurationMap =
                            characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

                    int rotatedPreviewWidth = getViewportWidth();
                    int rotatedPreviewHeight = getViewportHeight();
                    int maxPreviewWidth = MAX_PREVIEW_WIDTH;
                    int maxPreviewHeight = MAX_PREVIEW_HEIGHT;


                    mPrefSize = Utils.chooseOptimalSize(
                            streamConfigurationMap.getOutputSizes(SurfaceTexture.class),
                            rotatedPreviewWidth, rotatedPreviewHeight,maxPreviewWidth, maxPreviewHeight,
                            new Size(MAX_PREVIEW_WIDTH, MAX_PREVIEW_HEIGHT)
                    );

                    if (ActivityCompat.checkSelfPermission(mCtx, Manifest.permission.CAMERA)
                            != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    mCameraManager.openCamera(cid, mStateCallback, mHandler);
                    break;
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    @Override
    protected void initScene() {
        mVideoPlane = new ScreenQuad();
        mVideoMaterial = new Material();
        mVideoMaterial.setColorInfluence(0);
        mVideoPlane.setMaterial(mVideoMaterial);
        mVideoPlane.setRotX(90);

        mWorldScene.setBackgroundColor(Color.BLACK);
        mWorldScene.addChildAt(mVideoPlane, 0);

        try {
            mGalleryScene.setSkybox(R.drawable.cubemap);
        } catch (ATexture.TextureException e) {
            e.printStackTrace();
        }

        //Sphere background = new Sphere(100, 24, 24);
        //background.setDoubleSided(true);
        //Material mat = new Material(new VertexShader(), new FragmentShader(R.raw.starrrs));
        //mat.enableTime(true);
        //background.setMaterial(mat);
        //mGalleryScene.addChild(background);

        initCamera();
    }



    public void setInspirationListener(InspirationListener listener) {
        mListener = listener;
    }

    @Override
    protected void onRender(long ellapsedRealtime, double deltaTime) {
        super.onRender(ellapsedRealtime, deltaTime);

        if(mVideoPlaneDirty) {
            cropVideo();
            mVideoPlaneDirty = false;
        }

        mWorldCamera.setOrientation(mCameraOrientation);
        mGalleryCamera.setOrientation(mCameraOrientation);

        //Log.v(BuildConfig.APPLICATION_ID, "rotation" + MathUtil.radiansToDegrees(mCameraOrientation.getRotationY()));

        if (!mIsWorldScene)
            return;

        mPicker.getObjectAt((float)mCenter.getX(), (float)mCenter.getY());
        if(mCameraTexture != null)
            mCameraTexture.update();

    }

    @Override
    public void onOffsetsChanged(float xOffset, float yOffset, float xOffsetStep, float yOffsetStep, int xPixelOffset, int yPixelOffset) {

    }

    @Override
    public void onTouchEvent(MotionEvent event) {
        mDetector.onTouchEvent(event);
    }

    public void setSensorOrientation(Quaternion q) {
        synchronized (mCameraOrientationLock) {
            mCameraOrientation.setAll(q);
        }
    }

    @Override
    public void onRenderSurfaceSizeChanged(GL10 gl, int width, int height) {
        super.onRenderSurfaceSizeChanged(gl, width, height);

        mCenter.setAll(width / 2.0, height / 2.0);
    }

    @Override
    public void setSurface(Surface surface) {
        try {
            mCameraTexture.getSurfaceTexture().setDefaultBufferSize(
                    mPrefSize.getWidth(), mPrefSize.getHeight()
            );
            CaptureRequest.Builder previewRequestBuilder =
                    mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            previewRequestBuilder.addTarget(surface);

            previewRequestBuilder.set(
                    CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
            );


            mCameraDevice.createCaptureSession(Arrays.asList(surface),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(CameraCaptureSession session) {
                            try {
                                session.setRepeatingRequest(
                                        previewRequestBuilder.build(),
                                        null, mHandler);

                                mVideoPlaneDirty = true;
                                Log.e(mCtx.getPackageName(), "something cool!");


                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                                session.close();
                                Log.e(mCtx.getPackageName(), "still something wrong!");
                            }
                        }

                        @Override
                        public void onConfigureFailed(CameraCaptureSession session) {
                            Log.e(mCtx.getPackageName(), "something wrong!");
                            session.close();
                        }
                    }, mHandler);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onObjectPicked(@NonNull Object3D object) {
        if(object instanceof ScreenQuad || !object.getName().contains("aperture"))
            return;

        if(mFocusingObject == null) {
            mFocusingObject = object;
            mFocusStartTime = System.currentTimeMillis();
        }


        if (object == mFocusingObject && mActiveGaze == false) {
            long delta = System.currentTimeMillis() - mFocusStartTime;

            if(mListener != null) {
                int p = (int)(delta / mGazeTime * 100);
                p = Math.max(0, Math.min(100,p));
                mListener.onGazeProgress(p);
            }

            if (delta >= mGazeTime) {
                onGazeSelect();
            }

        }
    }

    @Override
    public void onNoObjectPicked() {
        if(mFocusingObject != null) {
            CustomSpriteSheetMaterialPlugin plg = (CustomSpriteSheetMaterialPlugin)mFocusingObject.getMaterial()
                    .getPlugin(CustomSpriteSheetMaterialPlugin.class);

            plg.reset();

            mFocusingObject = null;
            mActiveGaze = false;

            if(mListener != null) {
                mListener.onGazeProgress(0);
                onGazeUnselect();
            }
        }
    }

    public void setWorldPosition(double lat ,double lon) {
        Vector3 pos = Utils.latLonToXYZ(lat,lon);
        pos.y = 0;

        mWorldCamera.setPosition(pos);
        mCamLatLon.setX(lat);
        mCamLatLon.setY(lon);
    }

    public void clearGroups() {
        for(ArrayList arr : mGroupedImages)
            arr.clear();
    }

    public void addImageToGroup(FlickrImage image) {
        Vector2 l = image.getLatLon();
        double angle = Utils.angleBetweenLocations(l.getX(), l.getY(),
                mCamLatLon.getX(), mCamLatLon.getY());

        mGroupedImages.get((int)(angle / mAngleSteps)).add(image);
    }

    public void addImagesToGroup(ArrayList<FlickrImage> images) {
        for(FlickrImage image : images)
            addImageToGroup(image);
    }

    public void addImageGroupToScene() {
        double halfAngleStep = mAngleSteps / 2.0;

        for(int i = 0; i < mGroupedImages.size(); i++) {
            ArrayList<FlickrImage> images = mGroupedImages.get(i);

            if(images.size() == 0)
                continue;

            CustomSpriteSheetMaterialPlugin spriteMat = new CustomSpriteSheetMaterialPlugin(
                    5,5,25,25, false
            );

            Vector3 v = new Vector3(0, 0, 20);
            v.rotateY(MathUtil.degreesToRadians(i * mAngleSteps + halfAngleStep));
            v.add(mWorldCamera.getPosition());
            Log.v(BuildConfig.APPLICATION_ID, "angle. " + (i * mAngleSteps + halfAngleStep));

            Material mat = new Material();
            mat.addPlugin(spriteMat);
            mat.setColorInfluence(0);
            try {
                mat.addTexture(mApertureTexture);
                PointSprite sprite = new PointSprite(1,1);
                sprite.setScale(5.0);
                sprite.setPosition(v);
                sprite.setLookAt(mWorldCamera.getPosition());
                sprite.setMaterial(mat);
                sprite.setBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
                sprite.setTransparent(true);
                sprite.setName("aperture-" + i);
                mWorldScene.addChild(sprite);
            } catch (ATexture.TextureException e) {
                e.printStackTrace();
            }
        }
    }

    private void onGazeSelect() {
        if(mActiveGaze || mFocusingObject == null)
            return;

        if(mListener != null) {
            mListener.onGazeSelected();
        }

        mActiveGaze = true;

        Material mat = mFocusingObject.getMaterial();
        CustomSpriteSheetMaterialPlugin sprite = ((CustomSpriteSheetMaterialPlugin)
                mat.getPlugin(CustomSpriteSheetMaterialPlugin.class));
        sprite.play();
        Log.v(BuildConfig.APPLICATION_ID, "play!");


        Vector3 target = mFocusingObject.getPosition();
        Camera clone = getCurrentCamera().clone();
        mWorldScene.addAndSwitchCamera(clone);

        Animation3D anim = new TranslateAnimation3D(getCurrentCamera().getPosition(), target);
        anim.setInterpolator(new LinearInterpolator());
        anim.setDurationMilliseconds(1000);
        anim.setTransformable3D(clone);
        anim.setRepeatCount(0);
        anim.registerListener(new IAnimationListener() {
            @Override
            public void onAnimationEnd(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }

            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationUpdate(Animation animation, double interpolatedTime) {
                if(interpolatedTime > 0.9 || mFocusingObject != null) {
                    mIsWorldScene = false;
                    switchScene(mGalleryScene);

                    int group = Integer.valueOf(
                            mFocusingObject.getName().replace("aperture-","")
                    );

                    placeImages(group);
                    resetGaze();

                    mWorldScene.unregisterAnimation(anim);
                    mWorldScene.switchCamera(mWorldCamera);
                }
            }
        });

        Log.v(BuildConfig.APPLICATION_ID, "select item " + mFocusingObject.getName().replace("aperture-",""));

        mWorldScene.registerAnimation(anim);
        anim.play();
    }

    private void onGazeUnselect() {
        mActiveGaze = false;

        if(mListener != null) {
            mListener.onGazeUnselect();
        }
    }

    private void resetGaze() {
        onNoObjectPicked();
        mActiveGaze = false;
        mFocusingObject = null;
    }

    private void placeImages(int groupIndex) {
        for(int i = 0; i < mGalleryImages.getNumChildren(); i++) {
            Object3D child = mGalleryImages.getChildAt(i);
            mGalleryImages.removeChild(child);
            mGalleryScene.removeChild(child);
        }


        ArrayList<FlickrImage> imageGroup = mGroupedImages.get(groupIndex);

        //Log.v(BuildConfig.APPLICATION_ID, "target: " + target);
        double offset = (MathUtil.radiansToDegrees(mCameraOrientation.getRotationY()) + 360) % 360;
        mGalleryScene.addChild(mGalleryImages);

        int cols = (int)Math.floor(Math.sqrt(imageGroup.size()));
        int rows = imageGroup.size() / cols;
        double curveDeg = 80.0;
        double curve = MathUtil.degreesToRadians(curveDeg);
        double curveHalf = curve * 0.5;
        double sectors = curve / cols;
        double stacks = curve / rows;

        double r = 10.0;

        Log.v(BuildConfig.APPLICATION_ID, "items: " + imageGroup.size() + " scene: " + mGalleryScene.getNumChildren());

        for(int i = 0; i < imageGroup.size(); i++) {
            FlickrImage image = imageGroup.get(i);
            image.loadThumbnail();

            double x = i % cols;
            double y = i / cols;

            double theta = curveHalf - x * sectors;
            double phi = curveHalf - y * stacks;
            //Log.v(BuildConfig.APPLICATION_ID, "i: " + i + " theta " + MathUtil.radiansToDegrees(theta) +
            //        " phi " + MathUtil.radiansToDegrees(phi));

            double xy = r * Math.cos(phi);
            double z = r * Math.sin(phi);

            x =  xy * Math.cos(theta);
            y = xy * Math.sin(theta);

            // z and y are swapped
            Vector3 v = new Vector3(x, y, -z);
            image.enableLookAt();
            image.setLookAt(mGalleryCamera.getPosition());
            image.setPosition(v);

            //Log.v(BuildConfig.APPLICATION_ID, "v " + v);


            mGalleryImages.addChild(image);
        }

        mGalleryImages.setRotY(-(offset - curveDeg / 2.0));

        Log.v(BuildConfig.APPLICATION_ID, "rot" + offset + "count: " + mGalleryImages.getNumChildren());
    }

    private void cropVideo() {
        float va = getViewportHeight() / (float)getViewportWidth();
        float a = mPrefSize.getWidth() / (float)mPrefSize.getHeight();
        Utils.fitTextureToAspect(mVideoPlane, (a - va));
    }

    public void onBackButton() {
        if(!mIsWorldScene) {
            resetGaze();
            mIsWorldScene = true;
            switchScene(mWorldScene);
        }
    }

    public boolean isIsWorldScene() {
        return mIsWorldScene;
    }

    public interface SwipeUpListener {
        void onSwipeUp();
    }

    class SwipeUpGesture extends GestureDetector.SimpleOnGestureListener {
        private static final int SWIPE_MIN_DISTANCE = 120;
        private static final int SWIPE_MAX_OFF_PATH = 250;
        private static final int SWIPE_THRESHOLD_VELOCITY = 200;
        private SwipeUpListener mListener;

        public SwipeUpGesture(SwipeUpListener listener) {
            mListener = listener;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            try {
                if (Math.abs(e1.getX() - e2.getX()) > SWIPE_MAX_OFF_PATH){
                    return false;
                }
                // right to left swipe
                if (e1.getY() - e2.getY() > SWIPE_MIN_DISTANCE
                        && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                    mListener.onSwipeUp();
                }
            } catch (Exception e) {

            }
            return false;
        }
    }
}