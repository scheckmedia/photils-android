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

import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link Keywhat.OnKeywhatListener} interface
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
    private ProgressBar progressBar;
    private ChipCloud cloud;
    private KeywhatState state = new KeywhatState();
    private ImageView imageView;
    private CheckBox cbAlias;
    private Api api;

    private OnKeywhatListener listener;

    public Keywhat() { }


    public static Keywhat newInstance(Uri imageUri) {
        Keywhat f = new Keywhat();
        Bundle args = new Bundle();
        args.putParcelable("queuedImage", imageUri);
        f.setArguments(args);
        return f;
    }

    public static Keywhat newInstance(KeywhatState state) {
        Keywhat f = new Keywhat();
        Bundle args = new Bundle();
        args.putParcelable("state", state);
        f.setArguments(args);
        return f;
    }

    public KeywhatState getState() {
        return state;
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == IMAGE_SELECTION_RESULT && resultCode == Activity.RESULT_OK) {
            Uri selectedImage = data.getData();
            showTags(selectedImage, false);
        }
    }

    public void showTags(Uri uri, boolean cached) {
        if(!cached)
            state.setActiveUri(uri);

        if(uri == null)
            return;

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

        if(!cached)
            requestTags(bm);
    }

    private void restoreFromState(KeywhatState state) {
        this.state = state;
        cbAlias.setChecked(state.isAliasEnabled());

        showTags(this.state.getActiveUri(), true);
        updateTagCloud();
        toggleMenuItems();
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

        bm = Utils.scaleBitmapAndKeepRation(bm, 512, 512);

        imageView.setImageDrawable(null);
        imageView.setImageTintList(null);

        imageView.setImageBitmap(bm);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        api = Api.getInstance(getActivity().getApplicationContext());
        if(getArguments() != null)
        {
            Uri imageUri = getArguments().getParcelable("queuedImage");
            KeywhatState s = getArguments().getParcelable("state");
            if (imageUri   != null) {
                showTags(imageUri, false);
            } else if(s !=  null) {
                restoreFromState(s);
            }

        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.keyhwat_actions, menu);

        int size = menu.size();
        for(int i = 0; i < size; i++)
            menu.getItem(i).setVisible(this.state.getSelectedTags().size() > 0);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        // Inflate the layout for this fragment
        View  v = inflater.inflate(R.layout.fragment_keywhat, container, false);
        progressBar = v.findViewById(R.id.keywhat_progress);
        imageView = v.findViewById(R.id.keywhat_image_view);
        cbAlias = v.findViewById(R.id.keywhat_cb_hashtag);
        cloud = v.findViewById(R.id.keywhat_tag_cloud);
        cloud.setChipListener(this);

        checkPermission();

        imageView.setOnClickListener(v1 -> {
            Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(i, IMAGE_SELECTION_RESULT);
        });

        cbAlias.setOnCheckedChangeListener((buttonView, isChecked) -> {
            this.state.setAliasEnabled(isChecked);
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
        this.state.getSelectedTags().add(this.state.getTags().get(i));
        toggleMenuItems();
    }

    @Override
    public void chipDeselected(int i) {
        this.state.getSelectedTags().remove(this.state.getTags().get(i));
        toggleMenuItems();
    }

    void toggleMenuItems() {
        int size = this.state.getSelectedTags().size();
        cbAlias.setVisibility(size > 0 ? View.VISIBLE : View.INVISIBLE);

        if(listener != null)
            listener.onTagSelectedSize(size);
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
    public interface OnKeywhatListener {
        // TODO: Update argument type and name
        void onTagSelectedSize(int size);
    }

    public void setListener(OnKeywhatListener listener) {
        this.listener = listener;
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
                state.getTags().clear();
                state.getTags().addAll(tagList);
                state.getSelectedTags().clear();

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
            api.getTags(img, callback);
        }).start();

    }

    private void toggleProgress(boolean visible) {
        ViewGroup.LayoutParams params = progressBar.getLayoutParams();

        if(visible) {
            params.height = 20;
            progressBar.setLayoutParams(params);
            progressBar.setVisibility(View.VISIBLE);

        } else {
            progressBar.setVisibility(View.INVISIBLE);
            params.height = 0;
            progressBar.setLayoutParams(params);
        }
    }

    private void updateTagCloud() {
        cloud.removeAllViews();

        for(int i = 0; i < this.state.getTags().size(); i++) {
            String prefix = cbAlias.isChecked() ? "#" : "";
            String tag = prefix + this.state.getTags().get(i);
            cloud.addChip(tag);

            if(this.state.getSelectedTags().contains(this.state.getTags().get(i)))
                cloud.setSelectedChip(i);
        }

        if(this.listener != null) {
            listener.onTagSelectedSize(this.state.getSelectedTags().size());
        }
    }

    private String handleCopy(boolean suppressToast) {
        if(this.state.getSelectedTags().size() == 0)
            return "";

        String prefix = cbAlias.isChecked() ? "#" : "";

        String output = "";
        for(String tag : this.state.getSelectedTags()) {
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
        Utils.shareImageIntent(getContext(), this.state.getActiveUri(), content);
    }
}

