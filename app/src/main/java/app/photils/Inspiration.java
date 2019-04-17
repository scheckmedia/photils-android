package app.photils;

import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.annotation.Nullable;
import android.text.Layout;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import org.rajawali3d.materials.Material;
import org.rajawali3d.primitives.Plane;
import org.rajawali3d.primitives.Sphere;
import org.rajawali3d.renderer.Renderer;
import org.rajawali3d.view.SurfaceView;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link OnInspirationListener} interface
 * to handle interaction events.
 * Use the {@link Inspiration#newInstance} factory method to
 * create an instance of this fragment.
 */
public class Inspiration extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnInspirationListener mListener;

    public Inspiration() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
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
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
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
        layout.addView(surface);

        Renderer renderer = new Renderer(getContext());
        surface.setSurfaceRenderer(renderer);

        return layout;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
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
        Context mCtx;

        public Renderer(Context context) {
            super(context);
            mCtx = context;
            setFrameRate(60);
        }

        @Override
        protected void initScene() {
            Sphere s = new Sphere(1,24,24);
            s.setMaterial(new Material());
            getCurrentScene().addChild(s);
            getCurrentCamera().setZ(4);
        }

        @Override
        public void onOffsetsChanged(float xOffset, float yOffset, float xOffsetStep, float yOffsetStep, int xPixelOffset, int yPixelOffset) {

        }

        @Override
        public void onTouchEvent(MotionEvent event) {

        }
    }
}
