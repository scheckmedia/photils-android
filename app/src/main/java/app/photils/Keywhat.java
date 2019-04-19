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
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.text.HtmlCompat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.adroitandroid.chipcloud.ChipCloud;
import com.adroitandroid.chipcloud.ChipListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
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

    final static int PERMISSION_READ = 0;
    final static int mInputSize = 256;

    private int IMAGE_SELECTION_RESULT = 1;
    private View mOverlay;
    private ProgressBar mProgressBar;
    private ChipCloud mCloud;
    private KeywhatState mKeywhatState = new KeywhatState();
    private ImageView mImageView;
    private CheckBox mAlias;
    private Api mApi;

    private OnKeywhatListener mKeyhwatListener;

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
        args.putParcelable("mKeywhatState", state);
        f.setArguments(args);
        return f;
    }

    public KeywhatState getState() {
        return mKeywhatState;
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
            mKeywhatState.setActiveUri(uri);

        if(uri == null)
            return;

        String[] filePathColumn = { MediaStore.Images.Media.DATA };

        Cursor cursor = getActivity().getContentResolver().query(uri,
                filePathColumn, null, null, null);
        cursor.moveToFirst();

        String picturePath = null;

        if (cursor.getColumnCount() > 0) {
            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            picturePath = cursor.getString(columnIndex);
        }

        cursor.close();

        ExifInterface exif = null;
        Bitmap bm = null;
        try {
            if(picturePath == null)
                picturePath = getUriFromTemporary(uri);

            bm = BitmapFactory.decodeFile(picturePath);
            exif = new ExifInterface(picturePath);
        } catch (IOException e) { }

        if(bm != null) {
            displayImageAndTags(bm, exif);
            if(!cached)
                requestTags(bm);
        }
    }

    private String getUriFromTemporary(Uri uri) throws IOException {
        File imageCache = new File(getActivity().getCacheDir() + "/image/");

        if(!imageCache.exists())
            imageCache.mkdir();

        InputStream is = getActivity().getContentResolver().openInputStream(uri);
        Bitmap bm = BitmapFactory.decodeStream(is);
        File tmp = new File(imageCache, "cached.jpg");
        FileOutputStream fos = new FileOutputStream(tmp);
        bm.compress(Bitmap.CompressFormat.JPEG, 80, fos);
        fos.flush();
        fos.close();

        Uri tmpUri = FileProvider.getUriForFile(getContext(), BuildConfig.APPLICATION_ID + ".provider", tmp);
        getContext().grantUriPermission(BuildConfig.APPLICATION_ID, tmpUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        mKeywhatState.setActiveUri(tmpUri);
        return tmp.toString();
    }

    private void restoreFromState(KeywhatState state) {
        this.mKeywhatState = state;
        mAlias.setChecked(state.isAliasEnabled());

        showTags(this.mKeywhatState.getActiveUri(), true);
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

        mImageView.setImageDrawable(null);
        mImageView.setImageTintList(null);

        mImageView.setImageBitmap(bm);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mApi = Api.getInstance(getActivity().getApplicationContext());
        if(getArguments() != null)
        {
            Uri imageUri = getArguments().getParcelable("queuedImage");
            KeywhatState s = getArguments().getParcelable("mKeywhatState");
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
        for(int i = 0; i < size; i++) {
            MenuItem item = menu.getItem(i);

            if(item.getOrder() > 100)
                item.setVisible(this.mKeywhatState.getSelectedTags().size() > 0);
        }

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        // Inflate the layout for this fragment
        View  v = inflater.inflate(R.layout.fragment_keywhat, container, false);
        mProgressBar = v.findViewById(R.id.keywhat_progress);
        mImageView = v.findViewById(R.id.keywhat_image_view);
        mOverlay = v.findViewById(R.id.keywhat_permission_overlay);
        mAlias = v.findViewById(R.id.keywhat_cb_hashtag);
        mCloud = v.findViewById(R.id.keywhat_tag_cloud);
        mCloud.setChipListener(this);

        mImageView.setOnClickListener(v1 -> {
            Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(i, IMAGE_SELECTION_RESULT);
        });

        mAlias.setOnCheckedChangeListener((buttonView, isChecked) -> {
            this.mKeywhatState.setAliasEnabled(isChecked);
            updateTagCloud();
        });

        TextView tv = mOverlay.findViewById(R.id.keywhat_tv_permission);
        tv.setText(HtmlCompat.fromHtml(
                getString(R.string.keywhat_permission_explanation),
                HtmlCompat.FROM_HTML_MODE_LEGACY
        ));

        mOverlay.findViewById(R.id.keywhat_btn_permission_settings).setOnClickListener( event -> {
            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package", getActivity().getPackageName(), null);
            intent.setData(uri);
            startActivity(intent);
        });

        return v;
    }

    @Override
    public void onStart() {
        super.onStart();

        checkPermission();
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
        this.mKeywhatState.getSelectedTags().add(this.mKeywhatState.getmTags().get(i));
        toggleMenuItems();
    }

    @Override
    public void chipDeselected(int i) {
        this.mKeywhatState.getSelectedTags().remove(this.mKeywhatState.getmTags().get(i));
        toggleMenuItems();
    }

    void toggleMenuItems() {
        int size = this.mKeywhatState.getSelectedTags().size();
        mAlias.setVisibility(size > 0 ? View.VISIBLE : View.INVISIBLE);

        if(mKeyhwatListener != null)
            mKeyhwatListener.onTagSelectedSize(size);
    }

    private void checkPermission() {
        String permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        if(getContext().checkSelfPermission(permission)
                != PackageManager.PERMISSION_GRANTED) {
            mOverlay.setVisibility(View.VISIBLE);
            requestPermissions(     new String[]{permission},
                    PERMISSION_READ
            );

        } else {
            mOverlay.setVisibility(View.GONE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == PERMISSION_READ && grantResults.length > 0) {
            int state = grantResults[0] == PackageManager.PERMISSION_GRANTED ? View.GONE : View.VISIBLE;
            mOverlay.setVisibility(state);
        }
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
        void onRequestTags();
        void onTagsAvailable();
    }

    public void setListener(OnKeywhatListener listener) {
        this.mKeyhwatListener = listener;
    }


    private void requestTags(Bitmap bm) {
        if(this.mKeyhwatListener != null)
            this.mKeyhwatListener.onRequestTags();

        toggleProgress(true);
        Api.OnTagsReceived callback = new Api.OnTagsReceived() {
            @Override
            public void onSuccess(List<String> tagList) {
                mKeywhatState.getmTags().clear();
                mKeywhatState.getmTags().addAll(tagList);
                mKeywhatState.getSelectedTags().clear();

                updateTagCloud();
                toggleProgress(false);

                if(mKeyhwatListener != null)
                    mKeyhwatListener.onTagsAvailable();
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
            ByteBuffer img = Utils.convertBitmapToByteBuffer(bm, mInputSize);
            mApi.getTags(img, callback);
        }).start();

    }

    private void toggleProgress(boolean visible) {
        ViewGroup.LayoutParams params = mProgressBar.getLayoutParams();

        if(visible) {
            params.height = 20;
            mProgressBar.setLayoutParams(params);
            mProgressBar.setVisibility(View.VISIBLE);

        } else {
            mProgressBar.setVisibility(View.INVISIBLE);
            params.height = 0;
            mProgressBar.setLayoutParams(params);
        }
    }

    private void updateTagCloud() {
        mCloud.removeAllViews();

        for(int i = 0; i < this.mKeywhatState.getmTags().size(); i++) {
            String prefix = mAlias.isChecked() ? "#" : "";
            String tag = prefix + this.mKeywhatState.getmTags().get(i);
            mCloud.addChip(tag);

            if(this.mKeywhatState.getSelectedTags().contains(this.mKeywhatState.getmTags().get(i)))
                mCloud.setSelectedChip(i);
        }

        if(this.mKeyhwatListener != null) {
            mKeyhwatListener.onTagSelectedSize(this.mKeywhatState.getSelectedTags().size());
        }
    }

    private String handleCopy(boolean suppressToast) {
        if(this.mKeywhatState.getSelectedTags().size() == 0)
            return "";

        String prefix = mAlias.isChecked() ? "#" : "";

        String output = "";
        for(String tag : this.mKeywhatState.getSelectedTags()) {
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
        Utils.shareImageIntent(getContext(), this.mKeywhatState.getActiveUri(), content);
    }
}

