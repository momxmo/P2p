package com.amaze.filemanager.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.amaze.filemanager.R;
import com.amaze.filemanager.provider.Record;
import com.amaze.filemanager.ui.views.NumberProgressBar;
import com.amaze.filemanager.utils.DataTypeUtils;
import com.amaze.filemanager.utils.FileResLoaderUtils;
import com.amaze.filemanager.utils.FileTypeUtils;

import java.io.File;
import java.util.ArrayList;

/**
 * the adapter of the expandableListView
 */
public class MyExpandableListViewAdapter extends BaseExpandableListAdapter{
    private ArrayList<ExpandableListViewGroup> groupList;
    private Context context;

    public MyExpandableListViewAdapter(Context context, ArrayList<Integer> groupIds, ArrayList<String> groupNames, ArrayList<ArrayList<Record>> records) {
        this.context = context;
        this.groupList = new ArrayList<>();
        int size = groupNames.size();
        size = size < records.size() ? size : records.size();
        for(int i=0; i<size; i++) {
            ExpandableListViewGroup group = new ExpandableListViewGroup(groupIds.get(i), groupNames.get(i), records.get(i));
            this.groupList.add(group);
        }
    }

    public class ExpandableListViewGroup {
        private int id;
        private String name;
        private ArrayList<Record> recordList;
        public ExpandableListViewGroup(int id, String name, ArrayList<Record> recordList) {
            this.id = id;
            this.name = name;
            this.recordList = recordList;
        }
        public String getName() {
            return name;
        }
        public void setName(String name) {
            this.name = name;
        }
        public ArrayList<Record> getRecordList() {
            return recordList;
        }
        public void setRecordList(ArrayList<Record> recordList) {
            this.recordList = recordList;
        }
    }

    public ArrayList<ExpandableListViewGroup> getGroupList() {
        return groupList;
    }

    @Override
    public int getGroupCount() {
        return groupList.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        ExpandableListViewGroup group = groupList.get(groupPosition);
        return group.recordList.size();
    }

    public ExpandableListViewGroup getGroupById(int id) {
        for(int i=0; i<groupList.size(); i++) {
            ExpandableListViewGroup group = groupList.get(i);
            if(group.id == id) {
                return group;
            }
        }
        return null;
    }

    @Override
    public Object getGroup(int groupPosition) {
        return groupList.get(groupPosition);
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return ((ExpandableListViewGroup)getGroup(groupPosition)).recordList.get(childPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupList.get(groupPosition).id;
    }
    public long getGroupId(ExpandableListViewGroup group) {
        for(int i=0; i<groupList.size(); i++) {
            if(group == groupList.get(i)) {
                return getGroupId(i);
            }
        }
        return -1;
    }

    public int getGroupPostion(ExpandableListViewGroup group) {
        return groupList.indexOf(group);
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return groupPosition*1000+childPosition;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    class GroupViewHolder {
        TextView txt_title;
        ImageView img_expand;
    }



    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        if(convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.expandable_listview_groupview_layout, parent, false);
            GroupViewHolder groupViewHolder = new GroupViewHolder();
            groupViewHolder.txt_title = (TextView) convertView.findViewById(R.id.txt_title);
            convertView.setTag(groupViewHolder);
        }
        GroupViewHolder holder = (GroupViewHolder) convertView.getTag();
        ExpandableListViewGroup group = (ExpandableListViewGroup) getGroup(groupPosition);
        holder.txt_title.setText(group.name + "(" + group.recordList.size() + ")");
        return convertView;
    }

    class ChildViewHolder {
        View view_send_recevice_flag;
        ImageView img_icon;
        ImageView iv_delete;
        TextView txt_name;
        NumberProgressBar progressBar;
//        ProgressBar progressBar;
        TextView txt_send_speed;
        TextView txt_send_size;
        TextView txt_state;
    }

    @Override
    public View getChildView(final int groupPosition, final int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        if(convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.expandalbe_listview_childview_layout, parent, false);
            ChildViewHolder childViewHolder = new ChildViewHolder();
            childViewHolder.view_send_recevice_flag = convertView.findViewById(R.id.view_send_recevice_flag);
            childViewHolder.img_icon = (ImageView) convertView.findViewById(R.id.img_icon);
            childViewHolder.iv_delete = (ImageView) convertView.findViewById(R.id.iv_delete);
            childViewHolder.txt_name = (TextView) convertView.findViewById(R.id.txt_name);
            childViewHolder.progressBar = (NumberProgressBar) convertView.findViewById(R.id.progressbar);
            childViewHolder.txt_send_speed = (TextView) convertView.findViewById(R.id.txt_send_speed);
            childViewHolder.txt_send_size = (TextView) convertView.findViewById(R.id.txt_send_size);
            childViewHolder.txt_state = (TextView) convertView.findViewById(R.id.txt_state);
            convertView.setTag(childViewHolder);
        }
        ChildViewHolder holder = (ChildViewHolder) convertView.getTag();
        final Record record = (Record) getChild(groupPosition, childPosition);

        //set translate speed
        holder.txt_send_speed.setText(record.getSpeed()+"M/S");
        //set file send/total size
        StringBuffer sb = new StringBuffer();
        sb.append(DataTypeUtils.format(record.getTransported_len() / 1024 / 1024.0f)).
                append("/").
                append(DataTypeUtils.format(record.getLength() / 1024 / 1024.0f)).
                append("M");
        holder.txt_send_size.setText(sb.toString());
        //set the progress bar
        int progress = 0;
        if(record.getLength() != 0) {
            progress = (int)(record.getTransported_len() / (double)record.getLength() * holder.progressBar.getMax());
        }
        holder.progressBar.setProgress(progress);
        //set send or receive flag
        if(record.isSend()) { //发送
            holder.view_send_recevice_flag.setBackgroundColor(Color.GREEN);
        }else {
            holder.view_send_recevice_flag.setBackgroundColor(Color.RED);
        }
        //set image icon
        Object obj = FileResLoaderUtils.getPic(record.getPath());
        if(obj != null) {
            if(obj instanceof Drawable) {
                holder.img_icon.setImageDrawable((Drawable) obj);
            }else if(obj instanceof Bitmap) {
                holder.img_icon.setImageBitmap((Bitmap) obj);
            }else if(obj instanceof Integer) {
                holder.img_icon.setImageResource((Integer) obj);
            }else {
                holder.img_icon.setImageResource(FileTypeUtils.getDefaultFileIcon(record.getPath()));
            }
        }else { //set default icon
            holder.img_icon.setImageResource(FileTypeUtils.getDefaultFileIcon(record.getPath()));
        }

        final int gp = groupPosition;
        holder.iv_delete.setOnClickListener(new View.OnClickListener() {  //点击删除当前item任务
            @Override
            public void onClick(View v) {
                ExpandableListViewGroup group = (ExpandableListViewGroup) getGroup(gp);
                ArrayList<Record> recordList = group.getRecordList();
                if (recordList.contains(record)) {
                    if (record.getState() == Record.STATE_TRANSPORTING) { //正在传输
                        record.setState(Record.STATE_PAUSED);
                        recordList.remove(record);
                    } else {
                        recordList.remove(record);
                    }

                }
            }
        });
        //set name:
        if(record.getName() == null) {
            File f = new File(record.getPath());
            holder.txt_name.setText(f.getName());
        }else {
            holder.txt_name.setText(record.getName());
        }
        //set state:
        int resId=0;
        switch (record.getState()) {
            case Record.STATE_TRANSPORTING:
                resId = R.string.state_transporting;
                break;
            case Record.STATE_WAIT_FOR_TRANSPORT:
                resId = R.string.state_waiting;
                break;
            case Record.STATE_FAILED:
                resId = R.string.state_failed;
                break;
            case Record.STATE_FINISHED:
                resId = R.string.state_finished;
                break;
            case Record.STATE_PAUSED:
                resId = R.string.state_paused;
                break;
        }
        holder.txt_state.setText(resId);
        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return false;
    }
}
