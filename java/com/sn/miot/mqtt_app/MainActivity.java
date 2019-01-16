package com.sn.miot.mqtt_app;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.gson.Gson;

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

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    //Mqtt data
    private static final String SUBSCRIBE_TOPIC = "test";
    private static final String PUBLISH_TOPIC="test";
    final String serverUri = "tcp://192.168.123.1:1883";
    private String publishMessage="";
    private String status=""; //connection status
    private MqttAndroidClient mqttAndroidClient;

    //Layout
    private MenuItem infoTV;
    private ListView lv;
    private LineChart lChart;
    private CheckBox tempCheck, humidCheck, pressCheck, battCheck, accelCheck;
    Gson gson = new Gson();


    //Datasets
    private List<Entry> temperature= new ArrayList<>();
    private List<Entry> humidity = new ArrayList<>();
    private List<Entry> pression = new ArrayList<>();
    private List<Entry> battery = new ArrayList<>();
    private List<Entry> xData = new ArrayList<>();
    private List<Entry> yData = new ArrayList<>();
    private List<Entry> zData = new ArrayList<>();
    private LineDataSet tempDataSet, humDataSet, pressDataSet, battDataSet, accXDataSet, accYDataSet, accZDataSet;
    LineData lineData;

    //Messages for listview
    private ArrayList<String> messages=new ArrayList<String>();
    private ArrayAdapter adapter;
    MessageData messageData= new MessageData();
    int messageCount=0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //setting chart
        lChart = findViewById(R.id.chart);
        lChart.setNoDataText("Waiting for messages...");
        lChart.setNoDataTextColor(Color.BLACK);


        lChart.getDescription().setEnabled(false);
//        lChart.setScaleX(1);
//        lChart.getAxisLeft().setEnabled(false);
//        lChart.getAxisLeft().setDrawGridLines(false);
//        lChart.getAxisRight().setEnabled(false);
//        //lChart.setScaleEnabled(false);
//        lChart.setAutoScaleMinMaxEnabled(true);
//        lChart.getLegend().setWordWrapEnabled(true);
//        lChart.getLegend().setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
//        lChart.getLegend().setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        XAxis xAxis = lChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setAxisMaximum(50);
        xAxis.setAxisMinimum(0);
        YAxis yAxis = lChart.getAxisLeft();
        //lChart.setVisibleYRangeMaximum(150, YAxis.AxisDependency.LEFT);

        yAxis.setAxisMaximum(100);
        yAxis.setAxisMinimum(0);
        //lChart.setScaleMinima(200,200);
        //lChart.setVisibleYRangeMaximum(100,YAxis.AxisDependency.LEFT);
        //lChart.setVisibleYRangeMinimum(0,YAxis.AxisDependency.LEFT);

        temperature.add(new Entry(0,0));
        humidity.add(new Entry(0,0));
        pression.add(new Entry(0,0));
        battery.add(new Entry(0,0));
        xData.add(new Entry(0,0));
        yData.add(new Entry(0,0));
        zData.add(new Entry(0,0));


        //setting listview
        lv = findViewById(R.id.listView);
        adapter = new myAdapter(this,  messageData.getMessageList());
        lv.setAdapter(adapter);
        lv.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        lv.setDivider(new ColorDrawable(Color.parseColor("#00000000")));
        lv.setDividerHeight(20);

        //setting listener to checkboxes
        tempCheck = findViewById(R.id.tempCheck);
        tempCheck.setOnClickListener(this);
        humidCheck = findViewById(R.id.humidCheck);
        humidCheck.setOnClickListener(this);
        pressCheck = findViewById(R.id.pressCheck);
        pressCheck.setOnClickListener(this);
        battCheck = findViewById(R.id.battCheck);
        battCheck.setOnClickListener(this);
        accelCheck = findViewById(R.id.accelCheck);
        accelCheck.setOnClickListener(this);


//        lChart.setData(lineData);

        startMeasures();

        //connecting to mqtt broker
        String clientId = MqttClient.generateClientId();
        mqttAndroidClient = new MqttAndroidClient(getApplicationContext(), serverUri, clientId);
        mqttAndroidClient.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {

                if (reconnect) {
                    updateMenu("reconnected");
                    addToList("Reconnected to : " + serverURI);
                    subscribeToTopic();
                } else {
                    updateMenu("connected");
                    addToList("Connected to: " + serverURI);
                    subscribeToTopic();
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
            addToList("Connecting to " + serverUri);
            updateMenu("connecting...");

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

    //---------- Adding all the datasets to chart ---------
    private void startMeasures(){

        tempDataSet = getDataSet(temperature,"Temperature",Color.GREEN);
        humDataSet = getDataSet(humidity,"Humidity",Color.BLUE);
        pressDataSet = getDataSet(pression,"Pression",Color.RED);
        battDataSet = getDataSet(battery,"Battery",Color.YELLOW);
        accXDataSet = getDataSet(xData,"X Axis", Color.MAGENTA);
        accYDataSet = getDataSet(yData,"Y Axis", Color.CYAN);
        accZDataSet = getDataSet(zData,"Z Axis", Color.BLACK);
        lineData=new LineData(tempDataSet,humDataSet,pressDataSet,battDataSet,accXDataSet,accYDataSet,accZDataSet);
        lChart.setData(lineData);

        if(tempCheck.isChecked())
            tempDataSet.setVisible(true);
        else
            tempDataSet.setVisible(false);

        if(humidCheck.isChecked())
            humDataSet.setVisible(true);
        else
            humDataSet.setVisible(false);

        if(pressCheck.isChecked())
            pressDataSet.setVisible(true);
        else
            pressDataSet.setVisible(false);

        if(battCheck.isChecked())
            battDataSet.setVisible(true);
        else
            battDataSet.setVisible(false);

        if(accelCheck.isChecked()) {
            accXDataSet.setVisible(true);
            accYDataSet.setVisible(true);
            accZDataSet.setVisible(true);
        }
        else{
            accXDataSet.setVisible(false);
            accYDataSet.setVisible(false);
            accZDataSet.setVisible(false);
        }
    }

    //----------- Adding 1 dataset to chart -------------
    private void addDataSet(List<Entry> value, LineDataSet lineDataSet, int color) {
        LineData lineData = lChart.getData();

        if(lineData != null && !value.isEmpty()) {

            lineDataSet.setColor(color);
            lineDataSet.setCircleColor(color);
            lineDataSet.setLineWidth(1f);
            lineDataSet.setFillAlpha(65);

            lineData.addDataSet(lineDataSet);

            lChart.notifyDataSetChanged();
            lChart.invalidate();
            lChart.animateX(500);

        }
    }

    public void showData( LineDataSet lineDataSet){
        lineDataSet.setVisible(true);
        lChart.notifyDataSetChanged();
        lChart.invalidate();
        //lChart.animateX(500);
    }

    //------------ Removing dataset from chart ----------
    protected void removeDataSet( LineDataSet lineDataSet) {
        LineData lineData = lChart.getData();

        if (lineData != null) {
            lineDataSet.setVisible(false);
//            lineData.removeDataSet(lineDataSet);
//
            lChart.notifyDataSetChanged();
            lChart.invalidate();
            //lChart.animateX(500);
        }

    }

    //-------- Configuring dataset properties --------
    public LineDataSet getDataSet(List<Entry> data, String label, int color){
        LineDataSet dataSet;

        dataSet = new LineDataSet(data,label);
        dataSet.setDrawValues(false);
        //dataSet.setValueTextSize(10);
        dataSet.setDrawCircles(true);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setHighlightEnabled(false);

        dataSet.setColor(color);
        dataSet.setCircleColor(color);
        dataSet.setLineWidth(1f);
        dataSet.setFillAlpha(65);

        return dataSet;
    }

    //---------------- Menu bar function ---------------
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_search, menu);
        infoTV = menu.findItem(R.id.info);
        infoTV.setTitle(this.status);

        return super.onCreateOptionsMenu(menu);
    }

    //---------- updating text in menu ------
    private void updateMenu(String text){
        status=text;
        invalidateOptionsMenu();
    }

    //---------- Adding message to listview -----
    private void addToList(String mainText){
        messages.add(mainText);
        messageData.addData(mainText);
        adapter.notifyDataSetChanged();
        //lv.invalidateViews();

        System.out.println("LOG: " + mainText);
    }

    //---------- Subscribe to topic ----------
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
                    String msg =new String(message.getPayload());
                    System.out.println("***********************************");
                    System.out.println("Message: " + topic + " : " + msg);
                    String s1="{\"t\":25.6,\"h\":60.01,\"p\":97789.90,\"bL\":60,\"accX\":108,\"accY\":-267,\"accZ\":900}";
//                    System.out.println(s1);
                    MessageContentClass messageContent = gson.fromJson(msg, MessageContentClass.class);

//                    String s = "http://api.thingspeak.com/update?apikey=57DMCLT0NQXN1UVY&t=" + messageContent.getTemp() + //o Float.toString(messageContent.getTemp());
//                            "&h=" + messageContent.getHumd() + //o Float.toString(messageContent.getHumd());
//                            "&p=" + messageContent.getPres() + //o Float.toString(messageContent.getPres());
//                            "&bL=" + messageContent.getBatteryLevel() + //o Float.toString(messageContent.getBatteryLevel());
//                            "&accX=" + messageContent.getAccX() + //o Float.toString(messageContent.getAccX());
//                            "&accY=" + messageContent.getAccY() + //o Float.toString(messageContent.getAccY());
//                            "&accZ=" + messageContent.getAccZ(); //o Float.toString(messageContent.getAccZ());
//                    System.out.println(messageContent);
                    System.out.println(""+ messageContent.t+","+messageContent.h+","+messageContent.p+","+messageContent.bL+","+messageContent.accX+","+messageContent.accY+","+messageContent.accZ);
                    messageCount++;
                    System.out.print(messageCount);
                    System.out.println("***********************************");
                    addToList("Message: " + topic + " : " + msg );
                    addEntry2Dataset((float)messageCount,messageContent);


                }
            });

        } catch (MqttException ex){
            System.err.println("Exception whilst subscribing");
            ex.printStackTrace();
        }
    }

    private void addEntry2Dataset(float index, MessageContentClass msgContent){
        //Data test
        temperature.add(new Entry(index,msgContent.t));
        humidity.add(new Entry(index,msgContent.h));
        pression.add(new Entry(index,msgContent.p));
        battery.add(new Entry(index,msgContent.bL));
        xData.add(new Entry(index,msgContent.accX));
        yData.add(new Entry(index,msgContent.accY));
        zData.add(new Entry(index,msgContent.accZ));
//        startMeasures();

        lineData.notifyDataChanged();
        lChart.notifyDataSetChanged();
        lChart.invalidate();
        lv.invalidateViews();


        //lChart.animateX(500);
        //lChart.fitScreen();
        //lChart.setAutoScaleMinMaxEnabled(true);


    }
    //-------- publishing message to topic ------------
//    public void publishMessage(){
//
//        try {
//            MqttMessage message = new MqttMessage();
//            message.setPayload(publishMessage.getBytes());
//            mqttAndroidClient.publish(PUBLISH_TOPIC, message);
//            addToList("Message Published");
//            if(!mqttAndroidClient.isConnected()){
//                addToList(mqttAndroidClient.getBufferedMessageCount() + " messages in buffer.");
//            }
//        } catch (MqttException e) {
//            System.err.println("Error Publishing: " + e.getMessage());
//            e.printStackTrace();
//        }
//    }

    //--------- Listener for checkboxes--------
    @Override
    public void onClick(View v) {

        if(v==tempCheck) {
            if(tempCheck.isChecked()){
                showData(tempDataSet);
            }else{
                removeDataSet(tempDataSet);
            }
        } else if(v==humidCheck) {
            if(humidCheck.isChecked()){
                showData(humDataSet);
            }else{
                removeDataSet(humDataSet);
            }
        } else if (v==pressCheck) {
            if(pressCheck.isChecked()){
                showData(pressDataSet);
            }else{
                removeDataSet(pressDataSet);
            }
        } else if (v==battCheck) {
            if(battCheck.isChecked()){
                showData(battDataSet);
            }else{
                removeDataSet(battDataSet);
            }
        } else if (v==accelCheck) {
            if(accelCheck.isChecked()){
                showData(accXDataSet);
                showData(accYDataSet);
                showData(accZDataSet);
            }else{
                removeDataSet(accXDataSet);
                removeDataSet(accYDataSet);
                removeDataSet(accZDataSet);
            }
        }
    }
}




