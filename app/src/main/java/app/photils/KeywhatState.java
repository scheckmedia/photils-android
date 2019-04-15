package app.photils;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class KeywhatState implements Parcelable {
    private ArrayList<String> tags = new ArrayList<>();
    private HashSet<String> selectedTags = new HashSet<>();
    private Uri activeUri;
    private boolean aliasEnabled = false;

    public KeywhatState() { }

    public ArrayList<String> getTags() {
        return tags;
    }


    public HashSet<String> getSelectedTags() {
        return selectedTags;
    }


    public Uri getActiveUri() {
        return activeUri;
    }

    public void setActiveUri(Uri activeUri) {
        this.activeUri = activeUri;
    }

    public boolean isAliasEnabled() {
        return aliasEnabled;
    }

    public void setAliasEnabled(boolean aliasEnabled) {
        this.aliasEnabled = aliasEnabled;
    }

    protected KeywhatState(Parcel in) {
        tags = in.createStringArrayList();
        aliasEnabled = in.readByte() != 0;

        List<String> tmp = new ArrayList<>();
        in.readStringList(tmp);
        for(String v : tmp)
            selectedTags.add(v);

        activeUri = in.readParcelable(Uri.class.getClassLoader());
    }

    public void addSelectedTag(String tag) {
        this.selectedTags.add(tag);
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
        dest.writeStringList(tags);
        dest.writeByte((byte)(aliasEnabled ? 1 : 0) );
        dest.writeList(new ArrayList(selectedTags));

        if(activeUri != null)
            activeUri.writeToParcel(dest, 0);
    }
}
