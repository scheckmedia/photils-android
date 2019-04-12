package app.photils;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.adroitandroid.chipcloud.ChipCloud;
import com.adroitandroid.chipcloud.ChipListener;

import java.io.IOException;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link Keywhat.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link Keywhat#newInstance} factory method to
 * create an instance of this fragment.
 */
public class Keywhat extends Fragment implements ChipListener {

    // TODO: Rename and change types of parameters

    private int IMAGE_SELECTION_RESULT = 1;
    static final int PERMISSION_READ_CODE = 1;
    static final int PERMISSION_INTERNET_CODE = 2;
    static final int REQUEST_ID_MULTIPLE_PERMISSIONS = 101;


    final static int inputSize = 256;
    final ArrayList<String> tags = new ArrayList<>();

    private HashSet<String> selectedTags = new HashSet<>();
    private ChipCloud cloud;
    private Uri currentMediaPath = null;

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
            showTags(selectedImage);
        }
    }

    public void showTags(Uri uri) {
        currentMediaPath = uri;
        String[] filePathColumn = { MediaStore.Images.Media.DATA };

        Cursor cursor = getActivity().getContentResolver().query(uri,
                filePathColumn, null, null, null);
        cursor.moveToFirst();

        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
        String picturePath = cursor.getString(columnIndex);
        cursor.close();

        Bitmap bm = BitmapFactory.decodeFile(picturePath);
        ExifInterface exif = null;

        try {
            exif = new ExifInterface(picturePath);
        } catch (IOException e) { }

        displayImageAndTags(bm, exif);
    }

    private void displayImageAndTags(Bitmap bm, ExifInterface exif) {
        int orientation = ExifInterface.ORIENTATION_NORMAL;

        if (exif != null)
            orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                bm = Utils.rotateBitmap(bm, 90);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                bm = Utils.rotateBitmap(bm, 180);
                break;

            case ExifInterface.ORIENTATION_ROTATE_270:
                bm = Utils.rotateBitmap(bm, 270);
                break;
        }

        ImageView imageView = getView().findViewById(R.id.keywhat_image_view);
        imageView.setImageDrawable(null);
        imageView.setImageTintList(null);

        imageView.setImageBitmap(bm);

        requestTags(bm);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        cloud = getView().findViewById(R.id.keywhat_tag_cloud);
        cloud.setChipListener(this);

        if(getArguments() != null)
        {
            Uri imageUri = getArguments().getParcelable("queuedImage");
            showTags(imageUri);
        }


    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.keyhwat_actions, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        setHasOptionsMenu(true);
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if(id == R.id.keywhat_action_copy) {
            handleCopy(false);
        } else if(id == R.id.keywhat_action_share) {
            handleShare();
        }


        return super.onOptionsItemSelected(item);
    }

    @Override
    public void chipSelected(int i) {
        selectedTags.add(this.tags.get(i));
        toggleMenuItems(selectedTags.size() > 0);
    }

    @Override
    public void chipDeselected(int i) {
        selectedTags.remove(this.tags.get(i));
        toggleMenuItems(selectedTags.size() > 0);
    }

    void toggleMenuItems(boolean visible) {
        int[] menuItems = new int[] {
          R.id.keywhat_action_share,
          R.id.keywhat_action_copy
        };

        Toolbar toolbar = ((MainActivity)getActivity()).getToolbar();

        for(int i = 0; i < menuItems.length; i++) {
            MenuItem item = toolbar.getMenu().getItem(i);
            if(item != null)
                item.setVisible(visible);
        }

        getView().findViewById(R.id.keywhat_cb_hashtag)
                .setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
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

    // TODO: remove from here and put it else
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


    private void requestTags(Bitmap bm) {
        toggleProgress(true);

        Api.OnTagsReceived callback = new Api.OnTagsReceived() {
            @Override
            public void onSuccess(List<String> tagList) {
                tags.clear();
                tags.addAll(tagList);
                selectedTags.clear();

                updateTagCloud();
                toggleProgress(false);
            }

            @Override
            public void onFail(Api.ApiException ex) {
                toggleProgress(false);

                new AlertDialog.Builder(getContext())
                        .setTitle(getString(R.string.keywhat_error_title))
                        .setMessage(getString(R.string.keywhat_error_message, ex.getMessage()))
                        .setPositiveButton(R.string.keywhat_error_btn_ok, null)
                        .show();
            }
        };

        new Thread(() -> {
            ByteBuffer img = Utils.convertBitmapToByteBuffer(bm, inputSize);
            Api.getInstance(getActivity().getApplicationContext()).getTags(img, callback);
        }).start();

    }

    private void toggleProgress(boolean visible) {
        ProgressBar pb = getView().findViewById(R.id.keywhat_progress);
        ViewGroup.LayoutParams params = pb.getLayoutParams();

        if(visible) {
            params.height = 20;
            pb.setLayoutParams(params);
            pb.setVisibility(View.VISIBLE);

        } else {
            pb.setVisibility(View.INVISIBLE);
            params.height = 0;
            pb.setLayoutParams(params);
        }
    }

    private void updateTagCloud() {
        CheckBox cb = getView().findViewById(R.id.keywhat_cb_hashtag);
        cloud.removeAllViews();

        for(int i = 0; i < this.tags.size(); i++) {
            String prefix = cb.isChecked() ? "#" : "";
            String tag = prefix + this.tags.get(i);
            cloud.addChip(tag);

            if(selectedTags.contains(this.tags.get(i)))
                cloud.setSelectedChip(i);
        }
    }

    private String handleCopy(boolean suppressToast) {
        if(selectedTags.size() == 0)
            return "";

        String prefix = ((CheckBox)getView().findViewById(R.id.keywhat_cb_hashtag))
                .isChecked() ? "#" : "";

        String output = "";
        for(String tag : selectedTags) {
            output += prefix + tag + " ";
        }

        output += prefix + "photils";

        ClipData data = ClipData.newPlainText("photils tags", output);
        ((ClipboardManager)getActivity().getSystemService(Context.CLIPBOARD_SERVICE))
                .setPrimaryClip(data);


        if(!suppressToast) {
            Toast.makeText(getActivity(), getActivity()
                    .getString(R.string.keywhat_copy_success), Toast.LENGTH_LONG).show();
        }

        return output;

    }

    private void handleShare() {
        String content = handleCopy(true);
        Utils.shareImageIntent(getContext(), currentMediaPath, content);
    }


}
