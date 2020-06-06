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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.core.content.FileProvider;
import androidx.core.text.HtmlCompat;
import androidx.lifecycle.ViewModelProviders;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import app.photils.api.PhotilsApi;
import app.photils.keywhat.KeywhatAdapter;
import app.photils.keywhat.KeywhatTag;
import app.photils.keywhat.KeywhatViewModel;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link Keywhat.OnKeywhatListener} interface
 * to handle interaction events.
 * Use the {@link Keywhat#newInstance} factory method to
 * create an instance of this fragment.
 */
public class Keywhat extends Fragment implements KeywhatAdapter.KeywhatAdapterListener {
    final static int PERMISSION_READ = 0;
    final static int mInputSize = 224;

    private int IMAGE_SELECTION_RESULT = 1;
    private View mOverlay;
    private ProgressBar mProgressBar;
    //private ChipCloud mCloud;
    private KeywhatViewModel mKeywhatModel;
    private ImageView mImageView;
    private SeekBar mConfidenceThreshold;
    private TextView mCurrentConfidence;
    private TextView mFoundNumTags;
    private CheckBox mAlias;
    private PhotilsApi mApi;
    private ListView mTagList;
    private KeywhatAdapter mAdapter;
    private ConstraintLayout mKeywhatFilterRow;
    private TextView mTvSelectedTags;

    private OnKeywhatListener mKeyhwatListener;
    private List<PhotilsApi.Prediction> mCurrentPredictions;

    public Keywhat() { }


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
            showTags(selectedImage, false);
        }
    }

    public void showTags(Uri uri, boolean cached) {
        if(!cached)
            mKeywhatModel.setActiveUri(uri);

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
        mKeywhatModel.setActiveUri(tmpUri);
        return tmp.toString();
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
        mApi = PhotilsApi.getInstance(getActivity().getApplicationContext());
        if(getArguments() != null)
        {
            Uri imageUri = getArguments().getParcelable("queuedImage");
            if (imageUri   != null) {
                showTags(imageUri, false);
            }

        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.keyhwat_actions, menu);

        int size = menu.size();
        for(int i = 0; i < size; i++) {
            MenuItem item = menu.getItem(i);

            if(item.getOrder() > 100)
                item.setVisible(mKeywhatModel.getNumberOfSelectedTags() > 0);
        }

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        setHasOptionsMenu(true);

        mKeywhatModel = ViewModelProviders.of(this).get(KeywhatViewModel.class);

        mAdapter = new KeywhatAdapter(this);
        mAdapter.registerListener(this);

        // Inflate the layout for this fragment
        View  v = inflater.inflate(R.layout.fragment_keywhat, container, false);
        mProgressBar = v.findViewById(R.id.keywhat_progress);
        mImageView = v.findViewById(R.id.keywhat_image_view);
        mConfidenceThreshold = v.findViewById(R.id.keywhat_confidence_slider);
        mCurrentConfidence = v.findViewById(R.id.keywhat_tv_current_confidence);
        mFoundNumTags = v.findViewById(R.id.keywhat_tv_num_tags);
        mOverlay = v.findViewById(R.id.keywhat_permission_overlay);
        mAlias = v.findViewById(R.id.keywhat_cb_hashtag);
        mTagList = v.findViewById(R.id.keywhat_tag_list);
        mTagList.setAdapter(mAdapter);

        mTvSelectedTags = v.findViewById(R.id.keywhat_tv_selected_tags);
        mKeywhatFilterRow = v.findViewById(R.id.keywhat_filter);

        mImageView.setOnClickListener(v1 -> {
            Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(i, IMAGE_SELECTION_RESULT);
        });

        mAlias.setChecked(mKeywhatModel.isAliasEnabled());
        mAdapter.setAliasEnabled(mKeywhatModel.isAliasEnabled());
        mAlias.setOnCheckedChangeListener((buttonView, isChecked) -> {
            mKeywhatModel.setAliasEnabled(isChecked);
            mAdapter.setAliasEnabled(isChecked);
            mAdapter.notifyDataSetChanged();
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

        mKeywhatModel.getTags().observe(this, integerArrayListHashMap -> {
            ArrayList<String> groups = mKeywhatModel.getGroups().getValue();
            mAdapter.setData(groups, integerArrayListHashMap);
        });

        mConfidenceThreshold.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mCurrentConfidence.setText(getString(R.string.keywhat_current_confidence, progress));
                filterTags();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        mCurrentConfidence.setText(getString(R.string.keywhat_current_confidence, mConfidenceThreshold.getProgress()));

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

    void toggleMenuItems() {
        int num = mKeywhatModel.getNumberOfSelectedTags();
        mAlias.setVisibility(num > 0 ? View.VISIBLE : View.INVISIBLE);

        if(mKeyhwatListener != null)
            mKeyhwatListener.onTagSelectedSize(num);

        if (num == 0){
            mTvSelectedTags.setText("");
        } else {
            mTvSelectedTags.setText(getResources().getQuantityString(
                    R.plurals.keyhwat_selected_tags, num, num));
        }

        Log.v(BuildConfig.APPLICATION_ID, "Tag counter " + num);

        mAdapter.notifyDataSetChanged();
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

    @Override
    public void onTagSelected(int tagid) {
        mKeywhatModel.setTagSelected(tagid);
        toggleMenuItems();
    }

    @Override
    public void onTagDeselected(int tagid) {
        mKeywhatModel.setTagUnselected(tagid);
        toggleMenuItems();
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

    private void filterTags() {
        if(mCurrentPredictions == null || mCurrentPredictions.size() == 0)
            return;

        ArrayList<PhotilsApi.Prediction> tags = new ArrayList<>();
        float conf = mConfidenceThreshold.getProgress() / 100.0f;
        for(PhotilsApi.Prediction pred : mCurrentPredictions) {
            if(pred.getConfidence() < conf)
                continue;

            tags.add(pred);
        }

        mKeywhatModel.addSuggestionKeyword(tags);
        mFoundNumTags.setText(getResources().getQuantityString(
                R.plurals.keyhwat_tags_found, tags.size(), tags.size()));

        if(mKeyhwatListener != null)
            mKeyhwatListener.onTagsAvailable();
    }

    private void requestTags(Bitmap bm) {
        if(this.mKeyhwatListener != null) {
            this.mKeyhwatListener.onRequestTags();
        }

        toggleProgress(true);
        PhotilsApi.OnTagsReceived callback = new PhotilsApi.OnTagsReceived() {
            @Override
            public void onSuccess(List<PhotilsApi.Prediction> tagList) {
                mCurrentPredictions = tagList;
                getActivity().runOnUiThread(() -> {
                    mKeywhatModel.clearSelectedTags();
                    mKeywhatFilterRow.setVisibility(View.VISIBLE);
                    filterTags();
                    toggleProgress(false);
                    toggleMenuItems();
                });
            }

            @Override
            public void onFail(PhotilsApi.ApiException ex) {
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

    private String handleCopy(boolean suppressToast) {
        if(mKeywhatModel.getNumberOfSelectedTags() == 0)
            return "";

        String prefix = mAlias.isChecked() ? "#" : "";

        String output = "";
        for(ArrayList<KeywhatTag> tagSet : this.mKeywhatModel.getTags().getValue().values()) {
            for(KeywhatTag tag : tagSet) {
                if(!tag.isSelected())
                    continue;

                output += prefix + tag.getName() + " ";
            }
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
        Utils.shareImageIntent(getContext(), this.mKeywhatModel.getActiveUri(), content);
    }

    public void notifyChange() {
        mKeywhatModel.loadTags();
        toggleMenuItems();
    }
}

