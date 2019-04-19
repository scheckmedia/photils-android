package app.photils;


import android.os.Bundle;
import android.app.Fragment;
import android.support.v4.app.DialogFragment;
import android.support.v4.text.HtmlCompat;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link Info#newInstance} factory method to
 * create an instance of this fragment.
 */
public class Info extends DialogFragment {
    private static final String FRAGMENT_ID = "FRAGMENT_ID";
    private int mFragmentId;


    public Info() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param id Parameter 1.
     * @return A new instance of fragment Info.
     */
    // TODO: Rename and change types and number of parameters
    public static Info newInstance(int id) {
        Info fragment = new Info();
        Bundle args = new Bundle();
        args.putInt(FRAGMENT_ID, id);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mFragmentId = getArguments().getInt(FRAGMENT_ID);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_info, container, false);

        Button btn = v.findViewById(R.id.info_btn_dissmiss);
        btn.setOnClickListener(v1 -> {
            dismiss();
        });

        TextView tv = v.findViewById(R.id.info_content);
        if(mFragmentId == R.id.nav_keywhat) {
            tv.setText(
                    HtmlCompat.fromHtml(
                            getString(R.string.info_keywhat),
                            HtmlCompat.FROM_HTML_MODE_LEGACY
                    ));
        }

        return v;
    }

}
