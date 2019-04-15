package app.photils;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class KeywhatState implements Parcelable {
    private ArrayList<String> mTags = new ArrayList<>();
    private HashSet<String> mSelectedTags = new HashSet<>();
    private Uri mActiveUri;
    private boolean mAliasEnabled = false;

    public KeywhatState() { }

    public ArrayList<String> getmTags() {
        return mTags;
    }


    public HashSet<String> getSelectedTags() {
        return mSelectedTags;
    }


    public Uri getActiveUri() {
        return mActiveUri;
    }

    public void setActiveUri(Uri mActiveUri) {
        this.mActiveUri = mActiveUri;
    }

    public boolean isAliasEnabled() {
        return mAliasEnabled;
    }

    public void setAliasEnabled(boolean mAliasEnabled) {
        this.mAliasEnabled = mAliasEnabled;
    }

    protected KeywhatState(Parcel in) {
        mTags = in.createStringArrayList();
        mAliasEnabled = in.readByte() != 0;

        List<String> tmp = new ArrayList<>();
        in.readStringList(tmp);
        for(String v : tmp)
            mSelectedTags.add(v);

        mActiveUri = in.readParcelable(Uri.class.getClassLoader());
    }

    public void addSelectedTag(String tag) {
        this.mSelectedTags.add(tag);
    }

    public static final Parcelable.Creator<KeywhatState> CREATOR =
            new Parcelable.Creator<KeywhatState>() {
        @Override
        public KeywhatState createFromParcel(Parcel in) {
            return new KeywhatState(in);
        }

        @Override
        public KeywhatState[] newArray(int size) {
            return new KeywhatState[size];
        }
    };


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeStringList(mTags);
        dest.writeByte((byte)(mAliasEnabled ? 1 : 0) );
        dest.writeList(new ArrayList(mSelectedTags));

        if(mActiveUri != null)
            mActiveUri.writeToParcel(dest, 0);
    }
}
