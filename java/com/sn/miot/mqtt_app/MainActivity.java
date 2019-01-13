package com.sn.miot.mqtt_app;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String SUBSCRIBE_TOPIC = "test";
    private static final String PUBLISH_TOPIC="test";

    final String serverUri = "tcp://192.168.1.63:1883";
    TextView tV;
    MqttAndroidClient mqttAndroidClient;
    private Menu menu;
    private MenuItem infoTV;
    private String status="";
    String publishMessage="ds";
    ArrayAdapter adapter;
    ListView lv;

    //****Datasets
    List<Entry> temperature= new ArrayList<>();
    List<Entry> humidity = new ArrayList<>();
    List<Entry> pression = new ArrayList<>();
    List<Entry> battery = new ArrayList<>();
    List<Entry> xAxis = new ArrayList<>();
    List<Entry> yAxis = new ArrayList<>();
    List<Entry> zAxis = new ArrayList<>();
    int flagSensors=0x00;
    LineChart lChart;
    private LineDataSet tempDataSet, humDataSet, pressDataSet, battDataSet, accXDataSet, accYDataSet, accZDataSet;
    private LineData lineData;

    private ArrayList<String> messages=new ArrayList<String>();
    private CheckBox tempCheck, humidCheck, pressCheck, battCheck, accelCheck;
    MessageData messageData= new MessageData();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Elements from the layout
        lChart = findViewById(R.id.chart);
        lChart.setNoDataText("Waiting for messages");
        lChart.setNoDataTextColor(Color.BLACK);

        temperature.add(new Entry(0, (12)));
        temperature.add(new Entry(1, (15)));
        temperature.add(new Entry(2, (13)));
        humidity.add(new Entry(0, (6)));
        humidity.add(new Entry(1, (5)));
        humidity.add(new Entry(2, (7)));
        pression.add(new Entry(0, (26)));
        pression.add(new Entry(1, (53)));
        pression.add(new Entry(2, (23)));

        startMeasures();

        lv = findViewById(R.id.listView);
        adapter = new myAdapter(this,  messageData.getMessageList());
        lv.setAdapter(adapter);
        lv.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        lv.setDivider(new ColorDrawable(Color.parseColor("#00000000")));
        lv.setDivider(new ColorDrawable(Color.parseColor("#00000000")));
//        lv.setBackgroundDrawable(this.getResources().getDrawable(R.drawable.item_green));
        lv.setDividerHeight(20);




        tempCheck = (CheckBox)findViewById(R.id.tempCheck);
        tempCheck.setOnClickListener(this);
        humidCheck = (CheckBox)findViewById(R.id.humidCheck);
        humidCheck.setOnClickListener(this);
        pressCheck = (CheckBox)findViewById(R.id.pressCheck);
        pressCheck.setOnClickListener(this);
        battCheck = (CheckBox)findViewById(R.id.battCheck);
        battCheck.setOnClickListener(this);
        accelCheck = (CheckBox)findViewById(R.id.accelCheck);
        accelCheck.setOnClickListener(this);

        String clientId = MqttClient.generateClientId();
        mqttAndroidClient = new MqttAndroidClient(getApplicationContext(), serverUri, clientId);
        mqttAndroidClient.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {

                if (reconnect) {
                    updateMenu("reconnected");
                    addToList("Reconnected to : " + serverURI);
                    // Because Clean Session is true, we need to re-subscribe
                    subscribeToTopic();
                } else {
                    updateMenu("connected");
                    addToList("Connected to: " + serverURI);
                }
            }

            @Override
            public void connectionLost(Throwable cause) {
                updateMenu("Lost connec.");
                addToList("The connection was lost.");
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                addToList("Incoming message: " + new String(message.getPayload()));
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });

        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setAutomaticReconnect(true);
        mqttConnectOptions.setCleanSession(false);

        try {
            //addToList("Connecting to " + serverUri);
            updateMenu("connecting...");
            addToList("Connecting...");

            mqttAndroidClient.connect(mqttConnectOptions, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
                    disconnectedBufferOptions.setBufferEnabled(true);
                    disconnectedBufferOptions.setBufferSize(100);
                    disconnectedBufferOptions.setPersistBuffer(false);
                    disconnectedBufferOptions.setDeleteOldestMessages(false);
                    mqttAndroidClient.setBufferOpts(disconnectedBufferOptions);
                    subscribeToTopic();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    updateMenu("Failed");
                    addToList("Failed to connect to: " + serverUri);
                }
            });


        } catch (MqttException ex){
            ex.printStackTrace();
        }
    }


    private void startMeasures(){

        lChart.setData(new LineData());

        tempDataSet = getDataSet(temperature,"Temperature");
        humDataSet = getDataSet(humidity,"Humidity");
        pressDataSet = getDataSet(pression,"Pression");
        battDataSet = getDataSet(battery,"Battery");
        accXDataSet = getDataSet(xAxis,"X Axis");
        accYDataSet = getDataSet(yAxis,"Y Axis");
        accZDataSet = getDataSet(zAxis,"Z Axis");

        addDataSet(temperature,tempDataSet,Color.GREEN);
        addDataSet(humidity,humDataSet,Color.BLUE);
        addDataSet(pression,pressDataSet,Color.RED);
        addDataSet(battery,battDataSet,Color.YELLOW);
        addDataSet(xAxis,accXDataSet,Color.MAGENTA);
        addDataSet(yAxis,accYDataSet,Color.CYAN);
        addDataSet(zAxis,accZDataSet,Color.BLACK);
    }

    private void addDataSet(List<Entry> value, LineDataSet lineDataSet, int color) {
        LineData lineData = lChart.getData();

        if(lineData != null && !value.isEmpty()) {

            lineDataSet.setColor(color);
            lineDataSet.setCircleColor(color);
            lineDataSet.setLineWidth(1f);
            //lineDataSet.setCircleSize(4f);
            lineDataSet.setFillAlpha(65);

            lineData.addDataSet(lineDataSet);

            lChart.notifyDataSetChanged();
            lChart.invalidate();
            lChart.animateX(500);

        }
    }

    protected void removeDataSet( LineDataSet lineDataSet) {
        LineData lineData = lChart.getData();

        if (lineData != null) {

            lineData.removeDataSet(lineDataSet);

            lChart.notifyDataSetChanged();
            lChart.invalidate();
            lChart.animateX(500);
        }

    }

    //----------------------Menu bar functions--------------------------
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu=menu;
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_search, menu);
        infoTV = menu.findItem(R.id.info);
        infoTV.setTitle(this.status);

        return super.onCreateOptionsMenu(menu);
    }

    public LineDataSet getDataSet(List<Entry> data, String label){
        LineDataSet dataSet1;

        dataSet1 = new LineDataSet(data,label);
        dataSet1.setDrawValues(true);
        dataSet1.setValueTextSize(10);
        dataSet1.setDrawCircles(true);
        dataSet1.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet1.setHighlightEnabled(false);

        return dataSet1;
    }


    private void updateMenu(String text){
        status=text;
        invalidateOptionsMenu();
    }
    
    private void addToList(String mainText){
        messages.add(mainText);
        messageData.addData(mainText);
        adapter.notifyDataSetChanged();
        lv.invalidateViews();


        System.out.println("LOG: " + mainText);
    }

    public void subscribeToTopic(){
        try {
            mqttAndroidClient.subscribe(SUBSCRIBE_TOPIC, 0, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    addToList("Subscribed to topic: "+SUBSCRIBE_TOPIC);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    addToList("Failed to subscribe to topic: "+SUBSCRIBE_TOPIC);
                }
            });

            // THIS DOES NOT WORK!
            mqttAndroidClient.subscribe(SUBSCRIBE_TOPIC, 0, new IMqttMessageListener() {
                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    // message Arrived!
                    System.out.println("Message: " + topic + " : " + new String(message.getPayload()));
                    addToList("Message: " + topic + " : " + new String(message.getPayload()));
                }
            });

        } catch (MqttException ex){
            System.err.println("Exception whilst subscribing");
            ex.printStackTrace();
        }
    }

    public void publishMessage(){

        try {
            MqttMessage message = new MqttMessage();
            message.setPayload(publishMessage.getBytes());
            mqttAndroidClient.publish(PUBLISH_TOPIC, message);
            addToList("Message Published");
            if(!mqttAndroidClient.isConnected()){
                addToList(mqttAndroidClient.getBufferedMessageCount() + " messages in buffer.");
            }
        } catch (MqttException e) {
            System.err.println("Error Publishing: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View v) {

        if(v==tempCheck) {
            if(tempCheck.isChecked()){
                addDataSet(temperature,tempDataSet,Color.GREEN);
            }else{
                removeDataSet(tempDataSet);
            }
        } else if(v==humidCheck) {
            if(humidCheck.isChecked()){
                addDataSet(humidity,humDataSet,Color.BLUE);
            }else{
                removeDataSet(humDataSet);
            }
        } else if (v==pressCheck) {
            if(pressCheck.isChecked()){
                addDataSet(pression,pressDataSet,Color.RED);
            }else{
                removeDataSet(pressDataSet);
            }
        } else if (v==battCheck) {
            if(battCheck.isChecked()){
                addDataSet(battery,battDataSet,Color.YELLOW);
            }else{
                removeDataSet(battDataSet);
            }
        } else if (v==accelCheck) {
            if(accelCheck.isChecked()){
                addDataSet(xAxis,accXDataSet,Color.MAGENTA);
                addDataSet(yAxis,accYDataSet,Color.CYAN);
                addDataSet(zAxis,accZDataSet,Color.BLACK);
            }else{
                removeDataSet(accXDataSet);
                removeDataSet(accYDataSet);
                removeDataSet(accZDataSet);
            }
        }
    }

}




