package com.greatest.walt.arduinoscan;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.greatest.walt.arduinoscan.MyData.LocationResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.util.Date;
import java.util.UUID;


/**
 * Created by Walt on 2/15/2017.
 */

public class MainActivity extends Activity {

    //private MyData gpsdata;
    Button btnOn;
    TextView txtString;
    TextView txtStringLength;
    TextView sensorView;
    TextView sensorView2; //for pressure
    TextView sensorView3; //for altitude
    Handler bluetoothIn;

    private static double sensorT;
    private static double gpsValueX;
    private static double gpsValueY;
    private static String timeT;
    private static String unixT;


    final int handlerState = 0;
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private StringBuilder recDataString = new StringBuilder();

    private ConnectedThread mConnectedThread;

    // SPP UUID service - this should work for most devices
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // String for MAC address
    private static String address;
    private static final int LOCATION_REQUEST=1337;
    private static final String[] LOCATION_PERMS={
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };
    private boolean canAccessLocation() {
        return(hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) && hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION));
    }
    @TargetApi(Build.VERSION_CODES.M)
    private boolean hasPermission(String perm) {
        return(PackageManager.PERMISSION_GRANTED==checkSelfPermission(perm));
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        if (canAccessLocation()) {
            //
        }
        else {
            requestPermissions(LOCATION_PERMS, LOCATION_REQUEST);
        }
        setContentView(R.layout.activity_main);
        btnOn = (Button) findViewById(R.id.buttonOn);
        txtString = (TextView) findViewById(R.id.txtString);
        txtStringLength = (TextView) findViewById(R.id.testView1);
        sensorView = (TextView) findViewById(R.id.sensorView);


        bluetoothIn = new Handler() {
            public void handleMessage(Message msg) {
                if (msg.what == handlerState) {
                    String readMessage = (String) msg.obj;
                    recDataString.append(readMessage);
                    int temperatureEOL = recDataString.indexOf(":");

                    if (temperatureEOL > 0) {
                        String dataInPrint = recDataString.substring(0, 5);
                        //txtString.setText("Data Received = " + dataInPrint);
                        int tempLength = dataInPrint.length();
                        txtStringLength.setText("gpsx = " + gpsValueX + " " + "gpsy = " + gpsValueY);
                        //txtStringLength.setText("");

                        String sensor = recDataString.substring(0, 5);
                        sensorView.setText(" Sensor Temp = " + sensor + " F");
                        sensorT = Double.parseDouble(sensor);
                        recDataString.delete(0, recDataString.length());

                    }
                }
            }
        };

        btAdapter = BluetoothAdapter.getDefaultAdapter();
        checkBTState();

        // Set up onClick listener for buttons to turn on logging
        btnOn.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                //mConnectedThread.write("1");    // Send "1" via Bluetooth
                Toast.makeText(getBaseContext(), "Packaging and Sending to Server...", Toast.LENGTH_SHORT).show();
                //String s = sensorT;
                //String gps = gpsValue;
                //gps setup contains pointer to gps location
        LocationResult locationResult = new LocationResult(){
            @Override
            public void gotLocation(Location location){
                //Got the location!
                gpsValueX = location.getLatitude();
                gpsValueY = location.getLongitude();
            }
        };

        MyData myLocation = new MyData();
        myLocation.getLocation(MainActivity.this, locationResult);
        String currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date());
        Long tsLong = System.currentTimeMillis()/1000;
        String ts = tsLong.toString();
        // textView is the TextView view that should display it
        //txtStringLength.setText(currentDateTimeString);
        timeT = currentDateTimeString;
        unixT = ts;
        String time = timeT;
        String unix = unixT;
        double gpsX = gpsValueX;
        double gpsY = gpsValueY;
        double temp = sensorT;

        //double gpsX = 36.2157698;
        //double gpsY = -81.669296;
        //double temp = 75;


        JSONArray list = new JSONArray();
        JSONObject obj1 = new JSONObject();

        try{
            obj1.put("coordX", gpsX);
            obj1.put("coordY", gpsY);
            obj1.put("temp", temp);
            obj1.put("time", time);
            obj1.put("unix", unix);
            list.put(0, obj1);
        } catch (JSONException e) {
            e.printStackTrace();
        }


        String urlString = "34.223.239.158/addTest.php"; // URL to call
        HttpURLConnection urlConnection;
        String result = null;
        try {
            //Connect
            urlConnection = (HttpURLConnection) ((new URL("http://34.223.239.158/addTest.php").openConnection()));
            urlConnection.setDoOutput(true);
            urlConnection.setRequestProperty("Content-Type", "application/json");
            urlConnection.setRequestMethod("POST");
            urlConnection.connect();

            //Write
            OutputStream outputStream = urlConnection.getOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream, "UTF-8"));
            Log.e("BLah", list.toString());
            writer.write(list.toString());
            writer.close();
            outputStream.close();

            //Read
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream(), "UTF-8"));

            String line = null;
            StringBuilder sb = new StringBuilder();

            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line);
            }

            bufferedReader.close();
            result = sb.toString();

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
});}

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {

        return  device.createRfcommSocketToServiceRecord(BTMODULEUUID);
    }

    @Override
    public void onResume() {
        super.onResume();

        //Get MAC address from DeviceActivity via intent
        Intent intent = getIntent();

        //Get the MAC address from the DeviceListActivty via EXTRA
        address = intent.getStringExtra(DeviceActivity.EXTRA_DEVICE_ADDRESS);

        //create device and set the MAC address
        BluetoothDevice device = btAdapter.getRemoteDevice(address);

        try {
            btSocket = createBluetoothSocket(device);
        } catch (IOException e) {
            Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_LONG).show();
        }
        // Establish the Bluetooth socket connection
        try
        {
            btSocket.connect();
        } catch (IOException e) {
            try
            {
                btSocket.close();
            } catch (IOException e2)
            {
                //insert code to deal with this

            }
        }
        mConnectedThread = new ConnectedThread(btSocket);
        mConnectedThread.start();

        //sends a character over bluetooth and triggers an exception if the connection isn't active
        mConnectedThread.write("x");
    }


    //Checks that Bluetooth functionality is enabled on the device; prompts user to turn it on if not
    private void checkBTState() {

        if(btAdapter==null) {
            Toast.makeText(getBaseContext(), "Device does not support bluetooth", Toast.LENGTH_LONG).show();
        }
        else
        {
            if (btAdapter.isEnabled()) {
            } else {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
            }
        }
    }

    //create new class for connect thread
    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        //creation of the connect thread
        public ConnectedThread(BluetoothSocket socket) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                //Create I/O streams for connection
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[256];
            int bytes;

            // Keep listening until exception occurs or a socket is returned.
            while (true) {
                try {
                    bytes = mmInStream.read(buffer);//read bytes from input buffer
                    String readMessage = new String(buffer, 0, bytes);
                    // Send the obtained bytes to the UI Activity via handler
                    bluetoothIn.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }
        }

        public void onPause()
        {
            super.onPause();
            try
            {
                //Shuts off active sockets when phone sleeps or puts app in bg
                btSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //write method
        public void write(String input) {
            byte[] msgBuffer = input.getBytes();//converts entered String into bytes
            try {
                mmOutStream.write(msgBuffer);                //write bytes over BT connection via outstream
            } catch (IOException e) {
                //if you cannot write, close the application
                Toast.makeText(getBaseContext(), "Connection Failure", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }
}