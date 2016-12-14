package com.example.collector;

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

/*
 * 
 * @author   liaohongfei
 * 
 * @Date  2015 2015-1-26  下午6:26:07
 * 
 */
public class ClientAdapter extends BaseAdapter {

	private ArrayList<ChatMessage> list;
	private LayoutInflater mInflater;
	
	public ClientAdapter(Context context, ArrayList<ChatMessage> messages) {
		// TODO Auto-generated constructor stub
		this.list = messages;
		this.mInflater = LayoutInflater.from(context);
	}
	
	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return list.size();
	}

	@Override
	public Object getItem(int position) {
		// TODO Auto-generated method stub
		return list.get(position);
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return position;
	}
	
	@Override
	public int getItemViewType(int position) {
		// TODO Auto-generated method stub
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// TODO Auto-generated method stub
		ViewHolder viewHolder = null;
		ChatMessage message = list.get(position);
		if (convertView == null) {
			convertView = mInflater.inflate(R.layout.list_item, null);
			viewHolder = new ViewHolder((View)convertView.findViewById(R.id.list_child)
					, (TextView)convertView.findViewById(R.id.chat_msg));
			convertView.setTag(viewHolder);
		} else {
			viewHolder = (ViewHolder) convertView.getTag();
		}
		if (message.isSiri()) {
			viewHolder.child.setBackgroundResource(R.drawable.msgbox_rec);
		} else {
			viewHolder.child.setBackgroundResource(R.drawable.msgbox_send);
		}
		viewHolder.msg.setText(message.getMessage());
		
		return convertView;
	}

	class ViewHolder {
  	    protected View child;
        protected TextView msg;

        public ViewHolder(View child, TextView msg){
            this.child = child;
            this.msg = msg;
            
        }
    }

}