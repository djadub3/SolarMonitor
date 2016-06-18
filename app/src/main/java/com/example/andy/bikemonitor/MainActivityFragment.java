package com.example.andy.bikemonitor;

import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;


/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment {
    private static final String TAG = "BluetoothFragment";

    private int MESSAGE_READ;

    // text display views
    private TextView voltageView;
    private TextView batVoltageView;
    private TextView currentView;
    private TextView powerView;

    // buttons
    private Button connectButton;
    private Button dayGraphButton;

    // bluetooth thread
    private ConnectedThread thread;

    // variables
    private double voltage;
    private double batVoltage;
    private double current;
    private double power;


    // bluetooth variables
    private BluetoothDevice mBlueToothDevice;
    private BluetoothSocket mBlueToothSocket;
    private BluetoothAdapter mBluetoothAdapter;

    // graphing variables
    private double graphLastXValue = 0d;
    private LineGraphSeries<DataPoint> series;
    private double graph2LastXValue = 0d;
    private double graph3LastXValue = 0d;
    private LineGraphSeries<DataPoint> series2;
    private LineGraphSeries<DataPoint> series3;

    // handler for bluetooth messages
    protected Handler mHandler;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();            //obtain local bluetooth adapter

        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {                         // bluetooth message handler
                JSONObject inputJson = null;
                try {
                    inputJson = new JSONObject((String) msg.obj);           //extract JSON string from msg

                    voltage = Double.parseDouble(inputJson.getString("voltage"));
                    voltageView.setText(String.format("%.2f",voltage)+"V");    // update text views

                    batVoltage = Double.parseDouble(inputJson.getString("batVoltage"));
                    batVoltageView.setText(String.format("%.2f",batVoltage)+"V");

                    current = Math.abs(Double.parseDouble(inputJson.getString("current")));
                    currentView.setText((String.format("%.2f", current) + "A"));

                    series.appendData(new DataPoint(graphLastXValue, voltage), true, 40);
                    graphLastXValue+=1d;

                    series2.appendData(new DataPoint(graph2LastXValue,current), true, 40);
                    graph2LastXValue+=1d;

                    power = current*voltage;
                    series3.appendData(new DataPoint(graph3LastXValue,power), true, 40);
                    graph3LastXValue+=1d;

                    powerView.setText((String.format("%.2f",power) + "W"));

                } catch (JSONException e) {                                 // catch errors with JSON extraction
                e.printStackTrace();
                }
            }
        };
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_main, container, false);

        voltageView = (TextView) root.findViewById(R.id.voltage_text);
        voltageView.setText(voltage + "");

        batVoltageView = (TextView) root.findViewById(R.id.bat_voltage_text);
        batVoltageView.setText(batVoltage + "");

        currentView = (TextView) root.findViewById(R.id.current_text);
        currentView.setText(current + "");

        powerView = (TextView) root.findViewById(R.id.power_text);
        powerView.setText(power + "");

        connectButton = (Button) root.findViewById(R.id.connect_button);
        connectButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.v("Fragment", "connect button clicked");
                connectToBluetooth();
            }
        });

        dayGraphButton = (Button) root.findViewById(R.id.day_graph);
        String string ="1";
        final byte[] b = string.getBytes(StandardCharsets.UTF_8);
        dayGraphButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.v("Fragment", "light button clicked");

                thread.write(b);
            }
        });



        GraphView graph = (GraphView) root.findViewById(R.id.graph);
        series = new LineGraphSeries<DataPoint>();
        graph.addSeries(series);
        graph.getGridLabelRenderer().setGridColor(Color.GRAY);
        series2 = new LineGraphSeries<DataPoint>();
        graph.addSeries(series2);
        series3 = new LineGraphSeries<DataPoint>();
        graph.getSecondScale().addSeries(series3);

        graph.getGridLabelRenderer().setVerticalLabelsSecondScaleColor(Color.GRAY);
        graph.getGridLabelRenderer().setVerticalLabelsColor(Color.GRAY);

        graph.getSecondScale().setMinY(0);
        graph.getSecondScale().setMaxY(30);

        series2.setColor(Color.YELLOW);
        series.setColor(Color.MAGENTA);
        series3.setColor(Color.CYAN);

        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setMinX(0);
        graph.getViewport().setMaxX(10);
        graph.getViewport().setMinY(0);
        graph.getViewport().setMaxY(15);
        graph.getGridLabelRenderer().setPadding(30);
        graph.getGridLabelRenderer().setHorizontalLabelsVisible(false);
        graph.getGridLabelRenderer().setNumVerticalLabels(16);
        graph.getGridLabelRenderer().setVerticalAxisTitle("Volts/Amps");
        //graph.getGridLabelRenderer().set
        graph.getGridLabelRenderer().setVerticalAxisTitleColor(Color.GRAY);
        graph.getGridLabelRenderer().setVerticalLabelsSecondScaleColor(Color.GRAY);
        graph.getGridLabelRenderer().reloadStyles();

        return root;
    }

    public void connectToBluetooth() {

        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mBluetoothAdapter == null) Log.v("connect to bluetooth", "mBluetoothAdapter is null");
        else {
            Log.v("connect to bluetooth", "mBluetoothAdapter is  not null");
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            if (!mBluetoothAdapter.isEnabled()) startActivityForResult(enableBtIntent, 1);

            try {
                mBlueToothDevice = mBluetoothAdapter.getRemoteDevice("20:13:02:19:14:54");
            } catch (IllegalArgumentException exception) {
                Log.v("connect to bluetooth", "address invalid");
            }

            try {
                mBlueToothSocket = mBlueToothDevice.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
            } catch (IOException exception) {
                Log.v("connect to BT", "IOException");
            }

            try {
                mBlueToothSocket.connect();
            } catch (IOException e) {
                e.printStackTrace();
            }
            manageBlueToothConnection();
        }
    }

    private void manageBlueToothConnection() {
        thread = new ConnectedThread(mBlueToothSocket);
        thread.start();
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        char inchar;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;


            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            int sentByte;
            String JsonString = "";
            while (true) try {
                sentByte = mmInStream.read();
                if (sentByte == 10) {
                    sentByte = mmInStream.read();
                    while (sentByte != 13) {
                        inchar = (char) sentByte;
                        JsonString += inchar;
                        sentByte = mmInStream.read();
                    }
                    Log.v(" Input string",JsonString );
                    mHandler.obtainMessage(1, JsonString).sendToTarget();
                    JsonString ="";
                    /*try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }*/
                }


            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
                Log.v("bytes sent", "write");
                thread.sleep(10);
            } catch (IOException e) {
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
            }
        }
    }

}

