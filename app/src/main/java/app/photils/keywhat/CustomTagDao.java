package app.photils.keywhat;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface CustomTagDao {
    @Query("SELECT * FROM custom_tags ORDER BY `name` ASC")
    List<CustomTag> getAll();

    @Query("SELECT * FROM custom_tags WHERE `group` = :group ORDER BY `name` ASC")
    List<CustomTag> getTagsByGroup(String group);

    @Query("SELECT * FROM custom_tags WHERE `name` = :name")
    CustomTag getTag(String name);

    @Query("SELECT DISTINCT `group` from custom_tags ORDER BY `group` ASC")
    List<CustomTagGroup> getGroups();

    @Query("SELECT 1 FROM custom_tags where `name` = :name")
    int isUnique(String name);

    @Update
    void updateTags(CustomTag tag);

    @Insert
    void insertAll(CustomTag... tags);

    @Delete
    void delete(CustomTag user);

    @Query("delete from custom_tags where tid in (:ids)")
    void deleteAll(List<Integer> ids);

}
