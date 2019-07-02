package app.photils.keywhat;

import android.os.Bundle;
import androidx.fragment.app.DialogFragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import java.util.ArrayList;

import app.photils.R;

public class CustomTagEditDialog extends DialogFragment {
    private static final String SELECTED_TAG = "CUSTOM_TAGS_DIALOG_TAG";
    private CustomTag mSelectedTag;
    private OnTagSave mListener;
    private CustomTagModel mModel;

    public CustomTagEditDialog() {

    }

    public void setListener(OnTagSave listener) {
        mListener = listener;
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param tag Parameter 1.
     * @return A new instance of fragment Info.
     */
    // TODO: Rename and change types and number of parameters
    public static CustomTagEditDialog newInstance(CustomTag tag) {
        CustomTagEditDialog fragment = new CustomTagEditDialog();
        Bundle args = new Bundle();
        args.putParcelable(SELECTED_TAG, tag);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mSelectedTag = getArguments().getParcelable(SELECTED_TAG);
        }

        mModel = new CustomTagModel(getActivity().getApplication());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_custom_tags_edit, container, false);

        AutoCompleteTextView tvGroup = v.findViewById(R.id.tb_custom_tags_group);
        TextView tvTag = v.findViewById(R.id.tb_custom_tags_tag);
        Button btnSave = v.findViewById(R.id.custom_tags_btn_save);
        Button btnCancel = v.findViewById(R.id.custom_tags_btn_dismiss);
        CheckBox cb = v.findViewById(R.id.cb_custom_tags_default);

        if(mSelectedTag != null) {
            tvGroup.setText(mSelectedTag.group);
            tvTag.setText(mSelectedTag.name);
            cb.setChecked(mSelectedTag.isDefault);
        }

        ArrayList<String> groups = new ArrayList<>();
        for(CustomTagGroup group : mModel.getTagGroups())
            groups.add(group.group);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                getContext(),
                android.R.layout.simple_dropdown_item_1line,
                groups
        );

        tvGroup.setAdapter(adapter);


        btnSave.setOnClickListener(v1 -> {
            dismiss();
            if(mListener == null)
                return;

            CustomTag tag = mSelectedTag;
            if(tag == null) {
                tag = new CustomTag(tvTag.getText().toString(),tvGroup.getText().toString(), cb.isChecked());
            } else {
                tag.isDefault = cb.isChecked();
                tag.name = tvTag.getText().toString();
                tag.group = tvGroup.getText().toString();
            }

            mListener.onFragmentInteraction(tag, mSelectedTag != null);

        });

        btnCancel.setOnClickListener(v1 -> dismiss());

        return v;
    }

    public interface OnTagSave {
        // TODO: Update argument type and name
        void onFragmentInteraction(CustomTag tag, boolean isUpdate);
    }
}
