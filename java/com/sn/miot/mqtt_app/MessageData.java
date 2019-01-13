package com.sn.miot.mqtt_app;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

public class MessageData {

    private ArrayList<Message> mList;

    public MessageData() {
        mList = new ArrayList<Message>();
        }

    public ArrayList<Message> getMessageList() {
        return mList;
    }
    public void addData(String msg){
        mList.add(new Message(new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime()), msg));
    }
}
