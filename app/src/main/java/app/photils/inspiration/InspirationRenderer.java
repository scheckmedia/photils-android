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
import org.rajawali3d.materials.textures.TextureManager;
import org.rajawali3d.math.Quaternion;
import org.rajawali3d.math.vector.Vector2;
import org.rajawali3d.math.vector.Vector3;
import org.rajawali3d.primitives.Plane;
import org.rajawali3d.primitives.PointSprite;
import org.rajawali3d.primitives.ScreenQuad;
import org.rajawali3d.primitives.Sphere;
import org.rajawali3d.renderer.Renderer;
import org.rajawali3d.scene.Scene;
import org.rajawali3d.util.OnObjectPickedListener;
import org.rajawali3d.util.RayPicker;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.stream.IntStream;

import javax.microedition.khronos.opengles.GL10;

import app.photils.BuildConfig;
import app.photils.R;
import app.photils.api.FlickrImage;

public class InspirationRenderer extends Renderer
        implements StreamingTexture.ISurfaceListener,
        OnObjectPickedListener
{
    private Context mCtx;
    private Quaternion mCameraOrientation = new Quaternion();
    private final Object mCameraOrientationLock = new Object();
    private CameraManager mCameraManager;
    private CameraDevice mCameraDevice;
    private StreamingTexture mCameraTexture;
    private Handler mHandler;

    private Camera mCamera3D;
    private Material mVideoMaterial;
    private PointSprite mSprite;
    private Size mPrefSize;

    private Scene mWorldScene;

    private ScreenQuad mVideoPlane;
    private Vector2 mCenter = new Vector2();
    private final float mGazeTime = 1000.0f;
    private RayPicker mPicker;
    private Object3D mFocusingObject;
    private InspirationListener mListener;
    private long mFocusStartTime;
    private boolean mActiveGaze = false;


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

        }

        @Override
        public void onError(CameraDevice camera, int error) {

        }
    };

    public InspirationRenderer(Context context) {
        super(context);
        mCtx = context;
        mHandler = new Handler(Looper.getMainLooper());
        mCameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        mCamera3D = getCurrentCamera();
        mCamera3D.setNearPlane(0.1);
        mPicker = new RayPicker(this);
        mPicker.setOnObjectPickedListener(this);
        setFrameRate(60);

        mWorldScene = getCurrentScene();
    }

    private void initCamera() {
        try {
            for (String cid : mCameraManager.getCameraIdList()) {
                CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(cid);
                if (characteristics.get(CameraCharacteristics.LENS_FACING) ==
                        CameraCharacteristics.LENS_FACING_BACK) {

                    StreamConfigurationMap streamConfigurationMap =
                            characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

                    mPrefSize = streamConfigurationMap.getOutputSizes(SurfaceTexture.class)[0];
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

        CustomSpriteSheetMaterialPlugin sprite = new CustomSpriteSheetMaterialPlugin(
                5,5,25,25, false
        );
        Material mat = new Material();
        mat.addPlugin(sprite);
        mat.setColorInfluence(0);
        try {
            mat.addTexture(new Texture("aperture_sprite", R.drawable.aperture_sprite));
        } catch (ATexture.TextureException e) {
            e.printStackTrace();
        }

        mSprite = new PointSprite(1,1);
        mSprite.setPosition(0, 0, -10);
        mSprite.setScale(5.0);
        mSprite.setLookAt(mCamera3D.getPosition());
        mSprite.setMaterial(mat);
        mSprite.setBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        mSprite.setTransparent(true);

        mVideoMaterial = new Material();
        mVideoMaterial.setColor(Color.GREEN);
        mVideoMaterial.setColorInfluence(0);
        mVideoPlane.setMaterial(mVideoMaterial);
        mVideoPlane.setRotX(90);

        getCurrentScene().setBackgroundColor(Color.BLACK);
        getCurrentScene().addChildAt(mVideoPlane, 0);
        getCurrentScene().addChild(mSprite);

        initCamera();
    }

    public void setInspirationListener(InspirationListener listener) {
        mListener = listener;
    }

    @Override
    protected void onRender(long ellapsedRealtime, double deltaTime) {
        super.onRender(ellapsedRealtime, deltaTime);

        mCamera3D.setOrientation(mCameraOrientation);

        mPicker.getObjectAt((float)mCenter.getX(), (float)mCenter.getY());
        if(mCameraTexture != null)
            mCameraTexture.update();
    }


    @Override
    public void onOffsetsChanged(float xOffset, float yOffset, float xOffsetStep, float yOffsetStep, int xPixelOffset, int yPixelOffset) {

    }

    @Override
    public void onTouchEvent(MotionEvent event) {

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

                                Log.e(mCtx.getPackageName(), "something cool!");

                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                                Log.e(mCtx.getPackageName(), "still something wrong!");
                            }
                        }

                        @Override
                        public void onConfigureFailed(CameraCaptureSession session) {
                            Log.e(mCtx.getPackageName(), "something wrong!");
                        }
                    }, mHandler);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onObjectPicked(@NonNull Object3D object) {
        if(object instanceof ScreenQuad || object.getName() != "aperture")
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

            if (delta  >= mGazeTime) {
                onGazeSelect();
            }

        }
    }

    @Override
    public void onNoObjectPicked() {
        if(mFocusingObject != null) {
            mFocusingObject = null;

            if(mListener != null) {
                mListener.onGazeProgress(0);
                onGazeUnselect();
            }
        }
    }

    public void setWorldPosition(Vector3 pos) {
        mCamera3D.setPosition(pos);
    }

    public void addImage(FlickrImage image) {
        image.setLookAt(mCamera3D.getPosition());
        image.loadImage();
        image.setY(mCamera3D.getY());
        mWorldScene.addChild(image);
    }

    public void addImages(ArrayList<FlickrImage> images) {
        for(FlickrImage image : images)
            addImage(image);
    }

    private void onGazeSelect() {
        if(mListener != null) {
            mListener.onGazeSelected();
        }

        mActiveGaze = true;

        Material mat = mFocusingObject.getMaterial();
        CustomSpriteSheetMaterialPlugin sprite = ((CustomSpriteSheetMaterialPlugin)
                mat.getPlugin(CustomSpriteSheetMaterialPlugin.class));
        sprite.play();
        Log.v(BuildConfig.APPLICATION_ID, "play!");

        mCamera3D.disableLookAt();

        Vector3 targetPos = mSprite.getPosition();
        Animation3D anim = new TranslateAnimation3D(getCurrentCamera().getPosition(),targetPos);
        anim.setInterpolator(new LinearInterpolator());
        anim.setDurationMilliseconds(1000);
        anim.setTransformable3D(getCurrentCamera());
        anim.setRepeatCount(0);
        anim.registerListener(new IAnimationListener() {
            @Override
            public void onAnimationEnd(Animation animation) {
                mVideoPlane.setVisible(false);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }

            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationUpdate(Animation animation, double interpolatedTime) {
                Log.v(BuildConfig.APPLICATION_ID, "time " + interpolatedTime);
            }
        });
        getCurrentScene().registerAnimation(anim);
        anim.play();
    }

    private void onGazeUnselect() {
        mActiveGaze = false;

        if(mListener != null) {
            mListener.onGazeUnselect();
        }
    }
}