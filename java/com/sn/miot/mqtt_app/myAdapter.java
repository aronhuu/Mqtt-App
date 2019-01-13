package com.sn.miot.mqtt_app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;


public class myAdapter extends ArrayAdapter<Message> {

    private ArrayList<Message> messages;
    private LayoutInflater inflater;

    public myAdapter(Context context, ArrayList<Message> messages) {
        super(context, 0, messages);
        this.messages=messages;
        inflater  = LayoutInflater.from(context);

    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View newView = convertView;

        if ( newView == null ) {
            newView = inflater.inflate(R.layout.listview, parent, false);
        }

        TextView time    = (TextView) newView.findViewById(R.id.time);
        TextView msg = (TextView) newView.findViewById(R.id.msg);

        Message message = messages.get(position);

        time.setText(message.getTime());
        msg.setText(message.getMessage());

        return newView;
    }

    @Override
    public Message getItem(int position) {
        return super.getItem(super.getCount() - position - 1);
    }
}

class Message{
    private String time, msg;

    public Message(String time, String msg){
        this.time=time;
        this.msg = msg;
    }

    public String getMessage(){
        return msg;
    }

    public String getTime(){
        return time;
    }

}


