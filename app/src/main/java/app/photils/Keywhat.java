package app.photils;

import android.Manifest;
import android.app.Activity;
import android.arch.core.util.Function;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.adroitandroid.chipcloud.ChipCloud;

import org.tensorflow.lite.Interpreter;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link Keywhat.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link Keywhat#newInstance} factory method to
 * create an instance of this fragment.
 */
public class Keywhat extends Fragment {
    private static final String MODEL_PATH = "model.tflite";
    // TODO: Rename and change types of parameters

    private int IMAGE_SELECTION_RESULT = 1;
    static final int PERMISSION_READ_CODE = 1;
    static final int PERMISSION_INTERNET_CODE = 2;
    static final int REQUEST_ID_MULTIPLE_PERMISSIONS = 101;
    protected Interpreter tflite;

    final static int inputSize = 256;
    final ArrayList<String> tags = new ArrayList<>();

    public Keywhat() {
        // Required empty public constructor
    }


    public static Keywhat newInstance(Uri imageUri) {
        Keywhat f = new Keywhat();

        Bundle args = new Bundle();
        args.putParcelable("queuedImage", imageUri);
        f.setArguments(args);
        return f;
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == IMAGE_SELECTION_RESULT && resultCode == Activity.RESULT_OK) {
            Uri selectedImage = data.getData();

            String[] filePathColumn = { MediaStore.Images.Media.DATA };

            Cursor cursor = getActivity().getContentResolver().query(selectedImage,
                    filePathColumn, null, null, null);
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String picturePath = cursor.getString(columnIndex);
            cursor.close();

            Bitmap bm = BitmapFactory.decodeFile(picturePath);
            predictImage(bm);
        }
    }

    public void predictImage(Bitmap bm) {
        ImageView imageView = getView().findViewById(R.id.keywhat_image_view);
        imageView.setImageDrawable(null);
        imageView.setImageTintList(null);

        imageView.setImageBitmap(bm);

        predict(bm);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        try {
            tflite = new Interpreter(loadModel());

            if(getArguments() != null)
            {
                Uri imageUri = getArguments().getParcelable("queuedImage");
                InputStream is = getActivity().getContentResolver().openInputStream(imageUri);
                Bitmap bm = BitmapFactory.decodeStream(is);

                if(bm != null)
                    predictImage(bm);
            }


        } catch (IOException ex)
        {
            Log.e(getTag(), ex.getMessage());
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View  v = inflater.inflate(R.layout.fragment_keywhat, container, false);
        ImageView iv = v.findViewById(R.id.keywhat_image_view);
        CheckBox cb = v.findViewById(R.id.keywhat_cb_hashtag);

        checkPermission();

        iv.setOnClickListener(v1 -> {
            Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(i, IMAGE_SELECTION_RESULT);
        });

        cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateTagCloud();
        });

        return v;
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
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }

    private void checkPermission(){
        boolean canRead = ContextCompat.checkSelfPermission(getActivity(),
                Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;

        boolean hasInternet = ContextCompat.checkSelfPermission(getActivity(),
                Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED;

        List<String> reqPermissionList = new ArrayList<>();

        if(!canRead) {
            reqPermissionList.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }

        if(!hasInternet) {
            reqPermissionList.add(Manifest.permission.INTERNET);
        }

        if(!reqPermissionList.isEmpty()) {
            ActivityCompat.requestPermissions(getActivity(), reqPermissionList
                            .toArray(new String[reqPermissionList.size()]),
                    REQUEST_ID_MULTIPLE_PERMISSIONS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_READ_CODE: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {

                }
                return;
            }

            case PERMISSION_INTERNET_CODE: {
                if (grantResults.length > 0
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED) {

                } else {

                }
                return;
            }
        }
    }

    private MappedByteBuffer loadModel() throws IOException {
        AssetManager mgr = getActivity().getApplicationContext().getAssets();
        AssetFileDescriptor fd = mgr.openFd(MODEL_PATH);
        FileInputStream fis = new FileInputStream(fd.getFileDescriptor());
        FileChannel channel = fis.getChannel();

        return channel.map(FileChannel.MapMode.READ_ONLY,
                fd.getStartOffset(), fd.getDeclaredLength());
    }

    private ByteBuffer convertBitmapToByteBuffer(Bitmap bitmap) {
        bitmap = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize,false);
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * inputSize * inputSize * 3);

        byteBuffer.order(ByteOrder.nativeOrder());
        int[] intValues = new int[inputSize * inputSize];
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        int pixel = 0;
        for (int i = 0; i < inputSize; ++i) {
            for (int j = 0; j < inputSize; ++j) {
                final int val = intValues[pixel++];
                byteBuffer.putFloat(((val >> 16) & 0xFF) - 103.939f);
                byteBuffer.putFloat(((val >> 8) & 0xFF) - 116.779f);
                byteBuffer.putFloat(((val) & 0xFF) - 123.68f);
            }
        }
        return byteBuffer;
    }

    private void predict(Bitmap bm) {
        if(tflite == null)
            return;

        ProgressBar pb = getView().findViewById(R.id.keywhat_progress);
        ViewGroup.LayoutParams params = pb.getLayoutParams();
        params.height = 20;
        pb.setLayoutParams(params);
        pb.setVisibility(View.VISIBLE);

        new Thread(() -> {
            ByteBuffer img = convertBitmapToByteBuffer(bm);
            float[][] result = new float[1][256];
            tflite.run(img ,result);

            ByteBuffer buffer = ByteBuffer.allocate(4 * 256);
            buffer.order(ByteOrder.nativeOrder());
            buffer.asFloatBuffer().put(result[0]);


            Function<List<String>, Void> callback = (List<String> tags) -> {
                this.tags.clear();
                this.tags.addAll(tags);
                this.tags.add("photils");

                updateTagCloud();
                pb.setVisibility(View.INVISIBLE);

                params.height = 0;
                pb.setLayoutParams(params);

                return null;
            };

            Api.getInstance(getContext()).getTags(buffer, callback);
        }).start();

    }

    private void updateTagCloud() {
        CheckBox cb = getView().findViewById(R.id.keywhat_cb_hashtag);
        String[] tags = this.tags.toArray(new String[0]);
        if(cb.isChecked()) {
            for(int i = 0; i < tags.length; i++) {
                 tags[i] = "#" + tags[i];
            }
        }

        ChipCloud cloud = getView().findViewById(R.id.keywhat_tag_cloud);
        cloud.removeAllViews();
        cloud.addChips(tags);
    }
}
