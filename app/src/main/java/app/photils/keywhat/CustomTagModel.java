package app.photils.keywhat;

import android.app.Application;
import android.os.AsyncTask;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.concurrent.ExecutionException;

public class CustomTagModel {
    public static int TAG_PER_GROUP_LIMIT = 1000;
    private CustomTagDatabase mDB;
    private CustomTagDao mDao;

    public CustomTagModel(@NonNull Application application) {
        mDB = CustomTagDatabase.getDatabase(application);
        mDao = mDB.customTagDao();
    }


    public List<CustomTagGroup> getTagGroups() {
        try {
            return new AsyncTask<Void, Void, List<CustomTagGroup>>() {
                @Override
                protected List<CustomTagGroup> doInBackground(Void... voids) {
                    return mDao.getGroups();
                }
            }.execute().get();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return null;
    }

    public List<CustomTag> getTagByGroup(String group) {
        try {
            return new AsyncTask<Void, Void, List<CustomTag>>() {
                @Override
                protected List<CustomTag> doInBackground(Void... voids) {
                    return mDao.getTagsByGroup(group);
                }
            }.execute().get();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return null;
    }

    public CustomTag getTag(String name) {
        try {
            return new AsyncTask<Void, Void, CustomTag>() {
                @Override
                protected CustomTag doInBackground(Void... voids) {
                    return mDao.getTag(name);
                }
            }.execute().get();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return null;
    }

    public void remove(CustomTag tag) {
        new removeAsyncTask(mDao).execute(tag);
    }

    public void removeAll(List<Integer> ids) {
        new removeAllAsyncTask(mDao).execute(ids);
    }

    public void add(CustomTag tag) {
        new insertAsyncTask(mDao).execute(tag);
    }

    public void update(CustomTag tag) {
        new updateAsyncTask(mDao).execute(tag);
    }

    public Boolean isUnique(CustomTag tag) {
        try {
            return new checkUniqueAsyncTask(mDao).execute(tag).get();
        } catch (ExecutionException e) {
            return false;
        } catch (InterruptedException e) {
            return false;
        }
    }


    private static class insertAsyncTask extends AsyncTask<CustomTag, Void, Void> {
        CustomTagDao mDao;
        public insertAsyncTask(CustomTagDao dao) {
            mDao = dao;
        }

        @Override
        protected Void doInBackground(CustomTag... tags) {
            mDao.insertAll(tags);
            return null;
        }
    }

    private static class removeAllAsyncTask extends AsyncTask<List<Integer>, Void, Void> {
        CustomTagDao mDao;
        public removeAllAsyncTask(CustomTagDao dao) {
            mDao = dao;
        }

        @Override
        protected Void doInBackground(List<Integer>... ids) {
            mDao.deleteAll(ids[0]);
            return null;
        }
    }

    private static class removeAsyncTask extends AsyncTask<CustomTag, Void, Void> {
        CustomTagDao mDao;
        public removeAsyncTask(CustomTagDao dao) {
            mDao = dao;
        }

        @Override
        protected Void doInBackground(CustomTag... tags) {
            mDao.delete(tags[0]);
            return null;
        }
    }

    private static class updateAsyncTask extends AsyncTask<CustomTag, Void, Void> {
        CustomTagDao mDao;
        public updateAsyncTask(CustomTagDao dao) {
            mDao = dao;
        }

        @Override
        protected Void doInBackground(CustomTag... tags) {
            mDao.updateTags(tags[0]);
            return null;
        }
    }

    private static class checkUniqueAsyncTask extends AsyncTask<CustomTag, Void, Boolean> {
        CustomTagDao mDao;
        public checkUniqueAsyncTask(CustomTagDao dao) {
            mDao = dao;
        }

        @Override
        protected Boolean doInBackground(CustomTag... tags) {
            return new Boolean(mDao.isUnique(tags[0].name) < 1);
        }
    }

}
