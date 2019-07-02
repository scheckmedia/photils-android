package app.photils.keywhat;

import android.app.AlertDialog;
import android.os.Bundle;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.fragment.app.DialogFragment;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.CheckBox;
import android.widget.ExpandableListView;

import app.photils.R;

public class KeywhatCustomTag extends AppCompatActivity implements CustomTagEditDialog.OnTagSave,
        CustomTagListAdapter.OnItemSelectionChange {

    CustomTagModel mModel;
    ExpandableListView mListView;
    CustomTagListAdapter mListAdapter;
    Menu mMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_keywhat_custom_tags);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        mModel = new CustomTagModel(getApplication());
        mListAdapter = new CustomTagListAdapter(mModel, this);
        mListAdapter.setListener(this);

        mListView = findViewById(R.id.custom_tags);
        mListView.setAdapter(mListAdapter);

        mListView.setEmptyView(findViewById(R.id.custom_tags_item_items));

        mListView.setOnChildClickListener((parent, v, groupPosition, childPosition, id) -> {
            CustomTag tag = (CustomTag)mListAdapter.getChild(groupPosition, childPosition);
            CustomTagEditDialog f = CustomTagEditDialog.newInstance(tag);
            f.setListener(this);
            f.setStyle(DialogFragment.STYLE_NO_TITLE, R.style.InfoDialog);
            f.show(getSupportFragmentManager(), "CustomTagDialog");


            return true;
        });

        mListView.setOnItemLongClickListener((parent, view, position, id) -> {
            mListAdapter.isSelectionMode(true);
            CheckBox cb = view.findViewById(R.id.custom_tags_cb);
            if(cb != null) cb.setChecked(true);
            return true;
        });

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> {
            CustomTagEditDialog f = CustomTagEditDialog.newInstance(null);
            f.setListener(this);
            f.setStyle(DialogFragment.STYLE_NO_TITLE, R.style.InfoDialog);
            f.show(getSupportFragmentManager(), "CustomTagDialog");
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }

    @Override
    public void onFragmentInteraction(CustomTag tag, boolean isUpdate) {
        CustomTag dbTag = mModel.getTag(tag.name);

        if(dbTag == null && tag.tid == 0) {
            mModel.add(tag);
        } else if(dbTag == null || tag.tid == dbTag.tid) {
            mModel.update(tag);
        } else {
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.custom_tags_dialog_add_error_title))
                    .setMessage(getString(R.string.custom_tags_dialog_add_error_message, dbTag.name, dbTag.group))
                    .setPositiveButton(R.string.ok, null)
                    .show();

        }
        mListAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.custom_tags_actions, menu);
        mMenu = menu;
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onItemSelectionChange(CustomTag tag, boolean selected) {
        if(mMenu != null) {
            mMenu.getItem(0).setVisible(mListAdapter.getSelectedItemsCount() > 0);
        }

        if(mListAdapter.getSelectedItemsCount() == 0) mListAdapter.isSelectionMode(false);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.custom_tags_action_delete) {
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.custom_tags_delete_title))
                    .setMessage(getString(R.string.custom_tags_delete_message, mListAdapter.getSelectedItemsCount()))
                    .setNeutralButton(R.string.cancel, null)
                    .setPositiveButton(R.string.ok, (dialog, which) -> delete())
                    .show();
        }

        return super.onOptionsItemSelected(item);
    }

    private void delete() {
        mModel.removeAll(mListAdapter.getSelectedIds());
        mListAdapter.notifyDataSetChanged();
        mListAdapter.isSelectionMode(false);
    }
}
