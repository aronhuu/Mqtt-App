package com.sn.miot.mqtt_app;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LegendEntry;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;
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
    private static final String SUBSCRIBE_TOPIC = "test/#";
    private static final String PUBLISH_TOPIC="test";
    private String serverUri = "tcp://192.168.123.1:1883";
    private String publishMessage="";
    private String status=""; //connection status
    private MqttAndroidClient mqttAndroidClient;
    private String moteID="end_node";

    //Layout
    private MenuItem infoTV;
    private ListView lv;
    private LineChart lChart;
    private CheckBox tempCheck, humidCheck, pressCheck, battCheck, accelCheck;
    Gson gson = new Gson();
    int YMAX = 100;
    int YMIN = 0;
    //Datasets
    private List<Entry> eTemperature= new ArrayList<>();
    private List<Entry> eHumidity = new ArrayList<>();
    private List<Entry> ePression = new ArrayList<>();
    private List<Entry> eBattery = new ArrayList<>();
    private List<Entry> eXData = new ArrayList<>();
    private List<Entry> eYData = new ArrayList<>();
    private List<Entry> eZData = new ArrayList<>();

    private List<Entry> gTemperature= new ArrayList<>();
    private List<Entry> gHumidity = new ArrayList<>();
    private List<Entry> gPression = new ArrayList<>();
    private List<Entry> gBattery = new ArrayList<>();
    private List<Entry> gXData = new ArrayList<>();
    private List<Entry> gYData = new ArrayList<>();
    private List<Entry> gZData = new ArrayList<>();
    private LineDataSet eTempDataSet, eHumDataSet, ePressDataSet, eBattDataSet, eAccXDataSet, eAccYDataSet, eAccZDataSet; //Datasets of end node
    private LineDataSet gTempDataSet, gHumDataSet, gPressDataSet, gBattDataSet, gAccXDataSet, gAccYDataSet, gAccZDataSet; //Datasets of gateway

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

        RadioGroup radioGroup = (RadioGroup) findViewById(R.id.radioGroup);
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                // checkedId is the RadioButton selected
                RadioButton rb=(RadioButton)findViewById(checkedId);
                moteID=""+rb.getText();
                System.out.println(moteID);
                checkVisible();
                lChart.getAxisLeft().setAxisMaximum(YMAX);
                lChart.getAxisLeft().setAxisMinimum(YMIN);
                lineData.notifyDataChanged();
                lChart.notifyDataSetChanged();
                lChart.invalidate();
            }
        });

        //setting chart
        lChart = findViewById(R.id.chart);
        lChart.setNoDataText("Waiting for messages...");
        lChart.setNoDataTextColor(Color.BLACK);
        lChart.getDescription().setEnabled(false);
//        lChart.setScaleX(1);
//        lChart.getAxisLeft().setEnabled(false);
//        lChart.getAxisLeft().setDrawGridLines(false);
        lChart.getAxisRight().setEnabled(false);
//        //lChart.setScaleEnabled(false);
//        lChart.setAutoScaleMinMaxEnabled(true);
        Legend legend=lChart.getLegend();
        legend.setWordWrapEnabled(true);
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        legend.setForm(Legend.LegendForm.CIRCLE);

        LegendEntry l1=new LegendEntry("Temperature",Legend.LegendForm.CIRCLE,10f,2f,null,Color.GREEN);
        LegendEntry l2=new LegendEntry("Humidity",Legend.LegendForm.CIRCLE,10f,2f,null,Color.BLUE);
        LegendEntry l3=new LegendEntry("Pression",Legend.LegendForm.CIRCLE,10f,2f,null,Color.RED);
        LegendEntry l4=new LegendEntry("Battery",Legend.LegendForm.CIRCLE,10f,2f,null,Color.YELLOW);
        LegendEntry l5=new LegendEntry("X Axis",Legend.LegendForm.CIRCLE,10f,2f,null,Color.MAGENTA);
        LegendEntry l6=new LegendEntry("Y Axis",Legend.LegendForm.CIRCLE,10f,2f,null,Color.CYAN);
        LegendEntry l7=new LegendEntry("Z Axis",Legend.LegendForm.CIRCLE,10f,2f,null,Color.BLACK);
        legend.setCustom(new LegendEntry[] {l1,l2,l3,l4,l5,l6,l7});

        XAxis xAxis = lChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setAxisMaximum(50);
        xAxis.setAxisMinimum(0);
        YAxis yAxis = lChart.getAxisLeft();
        yAxis.setAxisMaximum(YMAX);
        yAxis.setAxisMinimum(YMIN);
        //lChart.setScaleMinima(200,200);
        //lChart.setVisibleYRangeMaximum(100,YAxis.AxisDependency.LEFT);
        //lChart.setVisibleYRangeMinimum(0,YAxis.AxisDependency.LEFT);

        eTemperature.add(new Entry(0,0));
        eHumidity.add(new Entry(0,0));
        ePression.add(new Entry(0,0));
        eBattery.add(new Entry(0,0));
        eXData.add(new Entry(0,0));
        eYData.add(new Entry(0,0));
        eZData.add(new Entry(0,0));

        gTemperature.add(new Entry(0,0));
        gHumidity.add(new Entry(0,0));
        gPression.add(new Entry(0,0));
        gBattery.add(new Entry(0,0));
        gXData.add(new Entry(0,0));
        gYData.add(new Entry(0,0));
        gZData.add(new Entry(0,0));


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

        startMeasures();

        //connecting to mqtt broker
        String clientId = MqttClient.generateClientId();
        mqttAndroidClient = new MqttAndroidClient(getApplicationContext(), serverUri, clientId);
        mqttAndroidClient.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {

                if (reconnect) {
                    updateMenu("reconnected");
                    //addToList("Reconnected to : " + serverURI);
                } else {
                    updateMenu("connected");
                    //addToList("Connected to: " + serverURI);
                }
                subscribeToTopic();
            }

            @Override
            public void connectionLost(Throwable cause) {
                updateMenu("Lost connec.");
                //addToList("The connection was lost.");
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                //addToList("Incoming message: " + new String(message.getPayload()));
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
                    //addToList("Failed to connect to: " + serverUri);
                }
            });


        } catch (MqttException ex){
            ex.printStackTrace();
        }
    }

    //---------- Adding all the datasets to chart ---------
    private void startMeasures() {

        eTempDataSet = getDataSet(eTemperature, "Temperature", Color.GREEN);
        eHumDataSet = getDataSet(eHumidity, "eHumidity", Color.BLUE);
        ePressDataSet = getDataSet(ePression, "ePression", Color.RED);
        eBattDataSet = getDataSet(eBattery, "eBattery", Color.YELLOW);
        eAccXDataSet = getDataSet(eXData, "eX Axis", Color.MAGENTA);
        eAccYDataSet = getDataSet(eYData, "eY Axis", Color.CYAN);
        eAccZDataSet = getDataSet(eZData, "eZ Axis", Color.BLACK);
        gTempDataSet = getDataSet(gTemperature, "Temperature", Color.GREEN);
        gHumDataSet = getDataSet(gHumidity, "gHumidity", Color.BLUE);
        gPressDataSet = getDataSet(gPression, "gPression", Color.RED);
        gBattDataSet = getDataSet(gBattery, "gBattery", Color.YELLOW);
        gAccXDataSet = getDataSet(gXData, "gX Axis", Color.MAGENTA);
        gAccYDataSet = getDataSet(gYData, "gY Axis", Color.CYAN);
        gAccZDataSet = getDataSet(gZData, "gZ Axis", Color.BLACK);
        lineData = new LineData(eTempDataSet, eHumDataSet, ePressDataSet, eBattDataSet, eAccXDataSet, eAccYDataSet, eAccZDataSet, gTempDataSet, gHumDataSet, gPressDataSet, gBattDataSet, gAccXDataSet, gAccYDataSet, gAccZDataSet);
        lChart.setData(lineData);

        checkVisible();
    }

    private void checkVisible() {
        if (moteID.equals("end_node")){
            if (tempCheck.isChecked())
                eTempDataSet.setVisible(true);
            else
                eTempDataSet.setVisible(false);

            if (humidCheck.isChecked())
                eHumDataSet.setVisible(true);
            else
                eHumDataSet.setVisible(false);

            if (pressCheck.isChecked())
                ePressDataSet.setVisible(true);
            else
                ePressDataSet.setVisible(false);

            if (battCheck.isChecked())
                eBattDataSet.setVisible(true);
            else
                eBattDataSet.setVisible(false);

            if (accelCheck.isChecked()) {
                eAccXDataSet.setVisible(true);
                eAccYDataSet.setVisible(true);
                eAccZDataSet.setVisible(true);
            } else {
                eAccXDataSet.setVisible(false);
                eAccYDataSet.setVisible(false);
                eAccZDataSet.setVisible(false);
            }
            gTempDataSet.setVisible(false);
            gHumDataSet.setVisible(false);
            gPressDataSet.setVisible(false);
            gBattDataSet.setVisible(false);
            gAccXDataSet.setVisible(false);
            gAccYDataSet.setVisible(false);
            gAccZDataSet.setVisible(false);
        }else if(moteID.equals("gateway")){
            if (tempCheck.isChecked())
                gTempDataSet.setVisible(true);
            else
                gTempDataSet.setVisible(false);

            if (humidCheck.isChecked())
                gHumDataSet.setVisible(true);
            else
                gHumDataSet.setVisible(false);

            if (pressCheck.isChecked())
                gPressDataSet.setVisible(true);
            else
                gPressDataSet.setVisible(false);

            if (battCheck.isChecked())
                gBattDataSet.setVisible(true);
            else
                gBattDataSet.setVisible(false);

            if (accelCheck.isChecked()) {
                gAccXDataSet.setVisible(true);
                gAccYDataSet.setVisible(true);
                gAccZDataSet.setVisible(true);
            } else {
                gAccXDataSet.setVisible(false);
                gAccYDataSet.setVisible(false);
                gAccZDataSet.setVisible(false);
            }
            eTempDataSet.setVisible(false);
            eHumDataSet.setVisible(false);
            ePressDataSet.setVisible(false);
            eBattDataSet.setVisible(false);
            eAccXDataSet.setVisible(false);
            eAccYDataSet.setVisible(false);
            eAccZDataSet.setVisible(false);
        }
    }

//    //----------- Adding 1 dataset to chart -------------
//    private void addDataSet(List<Entry> value, LineDataSet lineDataSet, int color) {
//        LineData lineData = lChart.getData();
//
//        if(lineData != null && !value.isEmpty()) {
//
//            lineDataSet.setColor(color);
//            lineDataSet.setCircleColor(color);
//            lineDataSet.setLineWidth(1f);
//            lineDataSet.setFillAlpha(65);
//
//            lineData.addDataSet(lineDataSet);
//
//            lChart.notifyDataSetChanged();
//            lChart.invalidate();
//            lChart.animateX(500);
//
//        }
//    }

    public void showData( LineDataSet lineDataSet){
        lineDataSet.setVisible(true);
        lChart.getAxisLeft().setAxisMaximum(YMAX);
        lChart.getAxisLeft().setAxisMinimum(YMIN);
        lChart.notifyDataSetChanged();
        lChart.invalidate();
        //lChart.animateX(500);
    }

    //------------ Removing dataset from chart ----------
    protected void hideDataSet( LineDataSet lineDataSet) {
        LineData lineData = lChart.getData();

        if (lineData != null) {
            lineDataSet.setVisible(false);
//            lineData.hideDataSet(lineDataSet);
//
            lChart.getAxisLeft().setAxisMaximum(YMAX);
            lChart.getAxisLeft().setAxisMinimum(YMIN);
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

        System.out.println("LOG: " + mainText);
    }

    //---------- Subscribe to topic ----------
    public void subscribeToTopic(){
        try {
            mqttAndroidClient.subscribe(SUBSCRIBE_TOPIC, 0, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    //addToList("Subscribed to topic: "+SUBSCRIBE_TOPIC);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    //addToList("Failed to subscribe to topic: "+SUBSCRIBE_TOPIC);
                }
            });

            mqttAndroidClient.subscribe(SUBSCRIBE_TOPIC, 0, new IMqttMessageListener() {
                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    // message Arrived!
                    if(topic.equals("test/data")) {
                        String msg = new String(message.getPayload());
                        System.out.println("***********************************");
                        System.out.println("Message: " + topic + " : " + msg);

                        MessageContentClass messageContent = gson.fromJson(msg, MessageContentClass.class);

                        System.out.println(messageContent.moteID + "," + messageContent.t + "," + messageContent.h + "," + messageContent.p + "," + messageContent.bL + "," + messageContent.accX + "," + messageContent.accY + "," + messageContent.accZ);
                        messageCount++;
                        System.out.print(messageCount);
                        System.out.println("***********************************");
                        addEntry2Dataset((float) messageCount, messageContent);
                        addToList("Message: " + topic + " : " + msg);
                        lChart.invalidate();
                        lv.invalidate();
                    }
                    else if(topic.equals("test/warning")){
                        String msg = new String(message.getPayload());
                        System.out.println("***********************************");
                        System.out.println("Message: " + topic + " : " + msg);
                        System.out.println("***********************************");

                        Warning warning = gson.fromJson(msg, Warning.class);

                        System.out.println(warning);

                        addToList("Message: " + topic + " : " + msg);
                        lv.invalidate();
                    }
                }
            });

        } catch (MqttException ex){
            System.err.println("Exception whilst subscribing");
            ex.printStackTrace();
        }
    }

    private void addEntry2Dataset(float index, MessageContentClass msgContent){
        //Data test
        if(msgContent.moteID.equals("end_node")){
            eTemperature.add(new Entry(index, msgContent.t));
            eHumidity.add(new Entry(index, msgContent.h));
            ePression.add(new Entry(index, msgContent.p));
            eBattery.add(new Entry(index, msgContent.bL));
            eXData.add(new Entry(index, msgContent.accX));
            eYData.add(new Entry(index, msgContent.accY));
            eZData.add(new Entry(index, msgContent.accZ));
        }else if (msgContent.moteID.equals("gateway")){
            gTemperature.add(new Entry(index, msgContent.t));
            gHumidity.add(new Entry(index, msgContent.h));
            gPression.add(new Entry(index, msgContent.p));
            gBattery.add(new Entry(index, msgContent.bL));
            gXData.add(new Entry(index, msgContent.accX));
            gYData.add(new Entry(index, msgContent.accY));
            gZData.add(new Entry(index, msgContent.accZ));
        }
//        startMeasures();
        lChart.getAxisLeft().setAxisMaximum(YMAX);
        lChart.getAxisLeft().setAxisMinimum(YMIN);

        lineData.notifyDataChanged();
        lChart.notifyDataSetChanged();
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
        if (moteID.equals("end_node")){
            if (v == tempCheck) {
                if (tempCheck.isChecked()) {
                    showData(eTempDataSet);
                } else {
                    hideDataSet(eTempDataSet);
                }
            } else if (v == humidCheck) {
                if (humidCheck.isChecked()) {
                    showData(eHumDataSet);
                } else {
                    hideDataSet(eHumDataSet);
                }
            } else if (v == pressCheck) {
                if (pressCheck.isChecked()) {
                    showData(ePressDataSet);
                } else {
                    hideDataSet(ePressDataSet);
                }
            } else if (v == battCheck) {
                if (battCheck.isChecked()) {
                    showData(eBattDataSet);
                } else {
                    hideDataSet(eBattDataSet);
                }
            } else if (v == accelCheck) {
                if (accelCheck.isChecked()) {
                    showData(eAccXDataSet);
                    showData(eAccYDataSet);
                    showData(eAccZDataSet);
                } else {
                    hideDataSet(eAccXDataSet);
                    hideDataSet(eAccYDataSet);
                    hideDataSet(eAccZDataSet);
                }
            }
        }
        else if(moteID.equals("gateway")){
            if (v == tempCheck) {
                if (tempCheck.isChecked()) {
                    showData(gTempDataSet);
                } else {
                    hideDataSet(gTempDataSet);
                }
            } else if (v == humidCheck) {
                if (humidCheck.isChecked()) {
                    showData(gHumDataSet);
                } else {
                    hideDataSet(gHumDataSet);
                }
            } else if (v == pressCheck) {
                if (pressCheck.isChecked()) {
                    showData(gPressDataSet);
                } else {
                    hideDataSet(gPressDataSet);
                }
            } else if (v == battCheck) {
                if (battCheck.isChecked()) {
                    showData(gBattDataSet);
                } else {
                    hideDataSet(gBattDataSet);
                }
            } else if (v == accelCheck) {
                if (accelCheck.isChecked()) {
                    showData(gAccXDataSet);
                    showData(gAccYDataSet);
                    showData(gAccZDataSet);
                } else {
                    hideDataSet(gAccXDataSet);
                    hideDataSet(gAccYDataSet);
                    hideDataSet(gAccZDataSet);
                }
            }
        }

        if(pressCheck.isChecked()){
            YMAX=100000;
            YMIN=0;
        }else if (accelCheck.isChecked()){
            YMAX=1000;
            YMIN=-1000;
        }else if(battCheck.isChecked() || tempCheck.isChecked() || humidCheck.isChecked()){
            YMAX=100;
            YMIN=0;
        }
        lChart.getAxisLeft().setAxisMaximum(YMAX);
        lChart.getAxisLeft().setAxisMinimum(YMIN);
        lChart.notifyDataSetChanged();
        lChart.invalidate();

    }
}




