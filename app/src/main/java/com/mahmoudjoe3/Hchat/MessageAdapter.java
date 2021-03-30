package com.mahmoudjoe3.Hchat;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.List;

public class MessageAdapter extends ArrayAdapter<FriendlyMessage> {
    public MessageAdapter(Context context, int resource, List<FriendlyMessage> objects) {
        super(context, resource, objects);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        /*
        * [1] inflate the item_message layout
        * [2] findViewById()
        * [3] get the item object that we inflate by the position
        * [4] set the item_message views with its crossponding values in the item object
        * */

        if (convertView == null) {
            convertView = ((Activity) getContext()).getLayoutInflater().inflate(R.layout.item_message, parent, false);
        }

        ImageView photoImageView = (ImageView) convertView.findViewById(R.id.photoImageView);
        TextView messageTextView = (TextView) convertView.findViewById(R.id.messageTextView);
        TextView authorTextView = (TextView) convertView.findViewById(R.id.nameTextView);

        FriendlyMessage message = getItem(position);

        assert message != null;
        boolean isPhoto = message.getPhotoUrl() != null;
        if (isPhoto) {
            photoImageView.setVisibility(View.VISIBLE);
            Glide.with(photoImageView.getContext())
                    .load(message.getPhotoUrl())
                    .centerCrop()
                    .into(photoImageView);
            photoImageView.setOnClickListener(v -> {
                if(onImageClickListener!=null){
                    onImageClickListener.onClick(message.getPhotoUrl());
                }
            });
            if(message.getText()!=null&&!message.getText().equals("")){
                messageTextView.setVisibility(View.VISIBLE);
                messageTextView.setText(message.getText());
            }else messageTextView.setVisibility(View.GONE);

        } else {
            messageTextView.setVisibility(View.VISIBLE);
            photoImageView.setVisibility(View.GONE);
            messageTextView.setText(message.getText());
        }
        authorTextView.setText(message.getName());

        return convertView;
    }

    onImageClickListener onImageClickListener;

    public void setOnImageClickListener(MessageAdapter.onImageClickListener onImageClickListener) {
        this.onImageClickListener = onImageClickListener;
    }

    interface onImageClickListener{
        void onClick(String uri);
    }

}
