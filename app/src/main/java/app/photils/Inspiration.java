package app.photils;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.annotation.Nullable;
import android.support.v4.text.HtmlCompat;
import android.text.Layout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.rajawali3d.materials.Material;
import org.rajawali3d.math.Matrix4;
import org.rajawali3d.math.Quaternion;
import org.rajawali3d.math.vector.Vector3;
import org.rajawali3d.primitives.Sphere;
import org.rajawali3d.view.SurfaceView;

import java.util.ArrayList;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link OnInspirationListener} interface
 * to handle interaction events.
 * Use the {@link Inspiration#newInstance} factory method to
 * create an instance of this fragment.
 */
public class Inspiration extends Fragment implements SensorEventListener {
    private View mOverlay;
    private static int PERMISSION_CODE = 0;
    private static float LOWPASS_ALPHA = 0.1f;
    private float[] mRotationVector;
    private OnInspirationListener mListener;
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Renderer mRenderer;

    public Inspiration() {

    }

    @Override
    public void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    public void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     *
     * @return A new instance of fragment Inspiration.
     */
    // TODO: Rename and change types and number of parameters
    public static Inspiration newInstance() {
        Inspiration fragment = new Inspiration();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {

        }
    }

    @Override
    public void onStart() {
        super.onStart();

        checkPermission();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        super.onCreateView(inflater, container, savedInstanceState);
        FrameLayout layout = (FrameLayout)inflater.inflate(R.layout.fragment_inspiration, container, false);

        final SurfaceView surface = new SurfaceView(getContext());
        surface.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        surface.setFrameRate(60.0);
        ((FrameLayout)layout.findViewById(R.id.inspiration_main)).addView(surface);

        mRenderer = new Renderer(getContext());
        surface.setSurfaceRenderer(mRenderer);

        mSensorManager = (SensorManager)getActivity().getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

        mOverlay = layout.findViewById(R.id.inspiration_permission_overlay);
        TextView tv = mOverlay.findViewById(R.id.inspiration_tv_permission);
        tv.setText(HtmlCompat.fromHtml(
                getString(R.string.inspiration_permission_explanation),
                HtmlCompat.FROM_HTML_MODE_LEGACY
        ));

        mOverlay.findViewById(R.id.inspiration_btn_permission_settings).setOnClickListener( event -> {
            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package", getActivity().getPackageName(), null);
            intent.setData(uri);
            startActivity(intent);
        });

        return layout;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    private void checkPermission() {
        String[] requiredPermission = new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_FINE_LOCATION
        };

        ArrayList<String> requestPermission = new ArrayList<>();

        for(String permission : requiredPermission) {
            if (getContext().checkSelfPermission(permission) !=
                    PackageManager.PERMISSION_GRANTED)
                requestPermission.add(permission);
        }

        if(requestPermission.size() > 0) {
            mOverlay.setVisibility(View.VISIBLE);
            requestPermissions(requestPermission.toArray(new String[0]), PERMISSION_CODE);

        } else {
            mOverlay.setVisibility(View.GONE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == PERMISSION_CODE && grantResults.length == 2) {
            int state = grantResults[0] == PackageManager.PERMISSION_GRANTED ? View.GONE : View.VISIBLE;
            mOverlay.setVisibility(state);
        }
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnInspirationListener) {
            mListener = (OnInspirationListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnInspirationListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if(event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            mRotationVector = lowPass(event.values.clone(), mRotationVector);
            float[] rm = new float[16];
            SensorManager.getRotationMatrixFromVector(rm, mRotationVector);
            Matrix.invertM(rm, 0, rm, 0);

            Quaternion q = new Quaternion().fromMatrix(new Matrix4(rm));
            int rotation = getActivity().getWindowManager().getDefaultDisplay().getRotation();
            switch (rotation) {
                case Surface.ROTATION_0:
                    break;
                case Surface.ROTATION_90:
                    q.multiplyLeft(new Quaternion().fromAngleAxis(Vector3.Axis.Z, 90));
                    break;
                case Surface.ROTATION_180:
                    q.multiplyLeft(new Quaternion().fromAngleAxis(Vector3.Axis.Z, 180));
                    break;
                case Surface.ROTATION_270:
                    q.multiplyLeft(new Quaternion().fromAngleAxis(Vector3.Axis.Z, 270));
                    break;
            }

            mRenderer.setSensorOrientation(q);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.v(getTag(), String.format("Accuracy %d", accuracy));
    }

    /*
    borrowed from
    https://github.com/raweng/augmented-reality-view/blob/master/ARView/src/com/raw/arview/ARView.java#L262
     */
    protected float[] lowPass( float[] input, float[] output ) {
        if ( output == null ) return input;
        for ( int i=0; i<input.length; i++ ) {
            output[i] = output[i] + LOWPASS_ALPHA * (input[i] - output[i]);
        }
        return output;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnInspirationListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }

    private class Renderer extends org.rajawali3d.renderer.Renderer {
        private Context mCtx;
        private Quaternion mCameraOrientation = new Quaternion();
        private final Object mCameraOrientationLock = new Object();

        public Renderer(Context context) {
            super(context);
            mCtx = context;
            setFrameRate(60);
        }

        @Override
        protected void initScene() {
            Sphere s = new Sphere(1,24,24);
            s.setY(10);
            s.setZ(0.5);
            s.setMaterial(new Material());
            getCurrentScene().addChild(s);
            getCurrentCamera().setZ(0);

        }

        @Override
        protected void onRender(long ellapsedRealtime, double deltaTime) {
            super.onRender(ellapsedRealtime, deltaTime);

            getCurrentCamera().setRotation(mCameraOrientation);
        }

        @Override
        public void onOffsetsChanged(float xOffset, float yOffset, float xOffsetStep, float yOffsetStep, int xPixelOffset, int yPixelOffset) {

        }

        @Override
        public void onTouchEvent(MotionEvent event) {

        }

        public void setSensorOrientation(Quaternion q)
        {
            synchronized (mCameraOrientationLock) {
                mCameraOrientation.setAll(q);
            }
        }
    }
}
