package app.photils.keywhat;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.CheckBox;
import android.widget.ExpandableListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import app.photils.R;

public class CustomTagListAdapter extends BaseExpandableListAdapter {
    public interface OnItemSelectionChange {
        void onItemSelectionChange(CustomTag tag, boolean selected);
    }
    private CustomTagModel mModel;
    private Context mCtx;
    private boolean isSelectionMode;
    private OnItemSelectionChange mListener;

    private HashSet<CustomTag> mSelectedTags = new HashSet<>();

    public CustomTagListAdapter(CustomTagModel model, Context ctx) {
        mModel = model;
        mCtx = ctx;
    }

    public void setListener(OnItemSelectionChange mListener) {
        this.mListener = mListener;
    }

    @Override
    public int getGroupCount() {
        if(mModel.getTagGroups() == null)
            return 0;

        return mModel.getTagGroups().size();
    }

    public void isSelectionMode(boolean selectionMode) {
        mSelectedTags.clear();
        isSelectionMode = selectionMode;
        notifyDataSetChanged();
    }

    public int getSelectedItemsCount() {
        return mSelectedTags.size();
    }

    public List<Integer> getSelectedIds() {
        ArrayList<Integer> ids = new ArrayList<>();
        for(CustomTag tag : mSelectedTags)
            ids.add(tag.tid);

        return ids;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        if(mModel.getTagGroups() == null)
            return 0;

        String group = mModel.getTagGroups().get(groupPosition).group;
        List<CustomTag> tags = mModel.getTagByGroup(group);
        if(tags == null)
            return 0;

        return tags.size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        List<CustomTagGroup> groups = mModel.getTagGroups();
        if(groups == null)
            return null;

        return groups.get(groupPosition);
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        List<CustomTagGroup> groups = mModel.getTagGroups();
        if(groups == null)
            return null;

        String group = groups.get(groupPosition).group;
        List<CustomTag> tags = mModel.getTagByGroup(group);
        if(tags == null)
            return null;

        return tags.get(childPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        String group = mModel.getTagGroups().get(groupPosition).group;
        CustomTag tag = mModel.getTagByGroup(group).get(childPosition);
        return tag.hashCode();
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        final CustomTagGroup group = (CustomTagGroup) getGroup(groupPosition);
        if (convertView == null) {
            LayoutInflater layoutInflater = (LayoutInflater) this.mCtx.getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE
            );
            convertView = layoutInflater.inflate(R.layout.custom_tags_group_item, null);
        }
        TextView listTitleTextView = convertView.findViewById(R.id.custom_tags_group_title);
        listTitleTextView.setTypeface(null, Typeface.BOLD);
        listTitleTextView.setText(group.group);

        ((ExpandableListView) parent).expandGroup(groupPosition);
        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        final CustomTag tag = (CustomTag) getChild(groupPosition, childPosition);

        if (convertView == null) {
            LayoutInflater layoutInflater = (LayoutInflater) this.mCtx.getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE
            );
            convertView = layoutInflater.inflate(R.layout.custom_tags_tag_item, null);
        }

        TextView expandedListTextView = convertView.findViewById(R.id.custom_tags_item_title);
        expandedListTextView.setText(tag.name);

        View fav = convertView.findViewById(R.id.custom_tags_item_default);
        fav.setVisibility(tag.isDefault ? View.VISIBLE : View.INVISIBLE);

        CheckBox cb = convertView.findViewById(R.id.custom_tags_cb);
        cb.setVisibility(isSelectionMode ? View.VISIBLE : View.INVISIBLE);
        cb.setOnCheckedChangeListener(null);
        cb.setChecked(mSelectedTags.contains(tag));
        cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if(isChecked) {
                mSelectedTags.add(tag);
            } else {
                mSelectedTags.remove(tag);
            }

            if(mListener != null) mListener.onItemSelectionChange(tag, isChecked);
        });


        //System.out.println("blaa" + mSelectedTags.contains(tag));


        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
    }
}
