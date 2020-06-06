package app.photils.keywhat;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;


@Entity(tableName = "custom_tags")
public class CustomTag implements Parcelable {
    @PrimaryKey(autoGenerate = true)
    public int tid;

    @ColumnInfo(name = "name")
    public String name;

    @ColumnInfo(name = "group")
    public String group;

    @ColumnInfo(name = "is_default")
    public boolean isDefault;

    public CustomTag(String name, String group, boolean isDefault) {
        this.name = name;
        this.group = group;
        this.isDefault = isDefault;
    }


    protected CustomTag(Parcel in) {
        tid = in.readInt();
        name = in.readString();
        group = in.readString();
        isDefault = in.readByte() != 0;
    }


    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CustomTag tag = (CustomTag)o;
        return tag.tid == this.tid;
    }

    public static final Creator<CustomTag> CREATOR = new Creator<CustomTag>() {
        @Override
        public CustomTag createFromParcel(Parcel in) {
            return new CustomTag(in);
        }

        @Override
        public CustomTag[] newArray(int size) {
            return new CustomTag[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(tid);
        dest.writeString(name);
        dest.writeString(group);
        dest.writeByte((byte) (isDefault ? 1 : 0));
    }

    @Override
    public int hashCode() {
        return tid;
    }
}

