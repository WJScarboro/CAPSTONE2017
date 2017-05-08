package com.greatest.walt.arduinoscan;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.Set;

//import android.support.v7.app.AppCompatActivity;


public class DeviceActivity extends Activity {

    // textview for connection status
    TextView textView;
    ListView pairedListView;
    //Button logbutton;
    private static final String TAG = "DeviceActivity";
    private static final boolean D = true;

    // Member fields
    private BluetoothAdapter mBtAdapter;
    private ArrayAdapter<String> mPairedDevicesArrayAdapter;
    public static String EXTRA_DEVICE_ADDRESS = "device_address";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_list);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        checkBTState();
        textView = (TextView) findViewById(R.id.connecting);
        textView.setTextSize(40);

        mPairedDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);
        //clear device array and textView
        mPairedDevicesArrayAdapter.clear();
        textView.setText(" ");

        // Makes the listView on main screen that displays connected devices w/mac address
        pairedListView = (ListView) findViewById(R.id.paired_devices);
        pairedListView.setAdapter(mPairedDevicesArrayAdapter);
        pairedListView.setOnItemClickListener(mDeviceClickListener);

        // Get the phones default bluetooth adapter
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();

        // Makes a set out of currently paired Bluetooth devices
        Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();

        // Add set of paired devices to the array
        if (pairedDevices.size() > 0)
        {
            // There are paired devices. Get the name and address of each paired device.
            findViewById(R.id.title_paired_devices).setVisibility(View.VISIBLE);
            for (BluetoothDevice device : pairedDevices)
            {
                mPairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());//MAC address
            }
        }
        else
        {
            mPairedDevicesArrayAdapter.add("No device detected");
        }
    }

    private OnItemClickListener mDeviceClickListener = new OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {

            textView.setText("Connecting...");
            // Get the device MAC address, which is the last 17 chars in the View
            String info = ((TextView) v).getText().toString();
            String address = info.substring(info.length() - 17);

            // Make an intent to start next activity while taking an extra which is the MAC address.
            Intent i = new Intent(DeviceActivity.this, MainActivity.class);
            i.putExtra(EXTRA_DEVICE_ADDRESS, address);
            startActivity(i);
        }
    };


    private void checkBTState()
    {
        mBtAdapter=BluetoothAdapter.getDefaultAdapter();
        if(mBtAdapter==null)
        {
            Toast.makeText(getBaseContext(), "Device does not support Bluetooth", Toast.LENGTH_SHORT).show();
        }
        else
        {
            if (mBtAdapter.isEnabled())
            {
                Log.d(TAG, "...Bluetooth ON...");
            } else
            {
                //Makes a toast asking to enable Bluetooth
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
            }
        }
    }


}
