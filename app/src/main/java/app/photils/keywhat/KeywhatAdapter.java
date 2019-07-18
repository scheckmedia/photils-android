package app.photils.keywhat;

import android.app.Application;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.adroitandroid.chipcloud.ChipCloud;
import com.adroitandroid.chipcloud.ChipListener;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import app.photils.BuildConfig;
import app.photils.Keywhat;
import app.photils.R;

public class KeywhatAdapter extends BaseAdapter{
    private Keywhat mKeywhat;
    private boolean mAliasEnabled = false;
    private ArrayList<String> mGroups = new ArrayList<>();
    private HashMap<Integer, ArrayList<KeywhatTag>> mTags = new HashMap<>();
    private KeywhatAdapterListener mListener;

    public interface KeywhatAdapterListener {
        void onTagSelected(int tagid);
        void onTagDeselected(int tagid);
    }

    class ViewHolder {
        TextView group;
        ChipCloud cloud;
        ChipListener listener;

        public ViewHolder(@NotNull View v) {
            group = v.findViewById(R.id.tag_list_item_title);
            cloud = v.findViewById(R.id.tag_list_item_tag_cloud);
        }
    }

    public void registerListener(KeywhatAdapterListener listener) {
        mListener = listener;
    }

    public KeywhatAdapter(Keywhat ctx) {
        mKeywhat = ctx;
    }

    public void setAliasEnabled(boolean enabled) {
        this.mAliasEnabled = enabled;
    }

    @Override
    public int getCount() {
        return mGroups.size();
    }

    @Override
    public Object getItem(int position) {
        return mGroups.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        String groupItem = (String) getItem(position);
        int groupid = position * CustomTagModel.TAG_PER_GROUP_LIMIT;

        final ViewHolder holder;
        if (convertView == null) {
            LayoutInflater layoutInflater = (LayoutInflater) mKeywhat.getContext().getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE
            );
            convertView = layoutInflater.inflate(R.layout.keywhat_tag_list_item, null);

            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder)convertView.getTag();
        }

        holder.group.setText(groupItem);
        if(holder.listener == null) {
            holder.listener = new ChipListener() {
                @Override
                public void chipSelected(int i) {
                    if(mListener != null)
                        mListener.onTagSelected(position * CustomTagModel.TAG_PER_GROUP_LIMIT + i);
                }

                @Override
                public void chipDeselected(int i) {
                    if(mListener != null)
                        mListener.onTagDeselected(position * CustomTagModel.TAG_PER_GROUP_LIMIT + i);
                }
            };
        }

        holder.cloud.removeAllViews();
        holder.cloud.setChipListener(null);
        if(mTags.containsKey(groupid) && mTags.get(groupid).size() > 0 ) {
            for(KeywhatTag tag : mTags.get(groupid)) {
                holder.cloud.addChip((mAliasEnabled ? "#" : "") + tag.getName());

                int idx = tag.getTid() % CustomTagModel.TAG_PER_GROUP_LIMIT;
                if(tag.isSelected() && !holder.cloud.isSelected(idx)) {
                    holder.cloud.setSelectedChip(idx);
                }
            }
            holder.group.setVisibility(View.VISIBLE);
        } else {
            holder.group.setVisibility(View.GONE);
        }

        holder.cloud.setChipListener(holder.listener);


        Log.v(BuildConfig.APPLICATION_ID, "Tags: " + mTags.size());

        return convertView;
    }

    public void setData(ArrayList<String> groups, HashMap<Integer, ArrayList<KeywhatTag>> tags) {
        mGroups = groups;
        mTags = tags;
        notifyDataSetChanged();
    }
}
