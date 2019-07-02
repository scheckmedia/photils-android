package app.photils.keywhat;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {CustomTag.class}, version = 1)
public abstract class CustomTagDatabase extends RoomDatabase {
    public abstract CustomTagDao customTagDao();
    private static volatile CustomTagDatabase INSTANCE;

    static CustomTagDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (CustomTagDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            CustomTagDatabase.class, "custom_tags_database")
                            .build();
                }
            }
        }
        return INSTANCE;
    }

}
