/*
T�RKAY B�L�YOR turkaybiliyor@hotmail.com
 */

package com.example.elmmaster;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.Manifest;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This Activity appears as a dialog. It lists any paired devices and
 * devices detected in the area after discovery. When a device is chosen
 * by the user, the MAC address of the device is sent back to the parent
 * Activity in the result Intent.
 */
public class DeviceListActivity extends Activity {
    public static final UUID BluetoothSerialUuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    // Debugging
    private static final String TAG = "DeviceListActivity";
    private static final boolean D = true;
    private static final int MY_PERMISSIONS_REQUEST_BLUETOOTH = 123;
    // Return Intent extra
    private static final int REQUEST_CONNECT_DEVICE = 2;
    public static String EXTRA_DEVICE_ADDRESS = "device_address";
    private TextView newDevicesTitle;
    // Member fields
    private BluetoothAdapter mBtAdapter;
    private ArrayAdapter<String> mPairedDevicesArrayAdapter;
    private ArrayAdapter<String> mNewDevicesArrayAdapter;
    private static final int REQUEST_ENABLE_BT = 3;
    private Calendar _discoveryStartTime;
    private SimpleDateFormat _logDateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS");
    // The on-click listener for all devices in the ListViews
    private OnItemClickListener mDeviceClickListener = new OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
            // Cancel discovery because it's costly and we're about to connect


            mBtAdapter.cancelDiscovery();
            TextView textView = v.findViewById(R.id.item_text);
            // Get the device MAC address, which is the last 17 chars in the View
            String info = ((TextView) textView).getText().toString();
            String address = info.substring(info.length() - 17);
            Log.d("address", address);

            // Create the result Intent and include the MAC address
            Intent intent = new Intent();
            intent.putExtra(EXTRA_DEVICE_ADDRESS, address);

            // Set result and finish this Activity
            setResult(Activity.RESULT_OK, intent);
            finish();
        }
    };

    // The BroadcastReceiver that listens for discovered devices and
    // changes the title when discovery is finished
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // If it's already paired, skip it, because it's been listed already
                if (ContextCompat.checkSelfPermission(DeviceListActivity.this, Manifest.permission.BLUETOOTH_CONNECT)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(DeviceListActivity.this,
                            new String[]{Manifest.permission.BLUETOOTH_CONNECT}, MY_PERMISSIONS_REQUEST_BLUETOOTH);
                }
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    mNewDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                }
                // When discovery is finished, change the Activity title
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                setProgressBarIndeterminateVisibility(false);
                setTitle(R.string.select_device);

                /*for (BluetoothDevice device : mBtAdapter.getBondedDevices())
                {
                    mNewDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                }*/

                // Trawl through the logs to find any devices that were skipped >:(
                try {
                    Process process = Runtime.getRuntime().exec("logcat -d -v time *:E");
                    BufferedReader bufferedReader = new BufferedReader(
                            new InputStreamReader(process.getInputStream()));

                    String line;
                    Pattern pattern = Pattern.compile("(.{18}).*\\[(.+)\\] class is 0x00 - skip it.");
                    while ((line = bufferedReader.readLine()) != null) {
                        Matcher matcher = pattern.matcher(line);
                        if (matcher.find()) {
                            // Found a blocked device, check if it was newly discovered.
                            // Android log timestamps don't contain the year!?
                            String logTimeStamp = Integer.toString(_discoveryStartTime.get(Calendar.YEAR)) + "-" + matcher.group(1);
                            Date logTime = null;
                            try {
                                logTime = _logDateFormat.parse(logTimeStamp);
                            } catch (ParseException e) {
                            }

                            if (logTime != null) {
                                if (logTime.after(_discoveryStartTime.getTime())) {
                                    // Device was discovered during this scan,
                                    // now we want to get the name of the device.
                                    String deviceAddress = matcher.group(2);
                                    BluetoothDevice device = mBtAdapter.getRemoteDevice(deviceAddress);

                                    // In order to get the name, we must attempt to connect to the device.
                                    // This will attempt to pair with the device, and will ask the user
                                    // for a PIN code if one is required.
                                    try {
                                        BluetoothSocket socket = device.createRfcommSocketToServiceRecord(BluetoothSerialUuid);
                                        socket.connect();
                                        socket.close();
                                        mNewDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                                    } catch (IOException e) {
                                    }
                                }
                            }
                        }
                    }
                } catch (IOException e) {
                }


//                if (mNewDevicesArrayAdapter.getCount() != 0) {
//                    newDevicesTitle.setText(R.string.title_other_devices);
//                } else {
//                    newDevicesTitle.setText(R.string.none_found);
//                }
            }
        }
    };
    ImageView connectimg;
    ImageView notconnectimg;
    TextView available, notavailable;
    Button scanButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Setup the window
        //requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.device_list);
        connectimg = findViewById(R.id.bt_connect_img);
        notconnectimg = findViewById(R.id.bt_nconnect_img);
        available = findViewById(R.id.avbl_txt);
        notavailable = findViewById(R.id.notavbl);
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();      //  newDevicesTitle = (TextView) findViewById(R.id.title_new_devices);

        // Initialize the button to perform device discovery
        scanButton = (Button) findViewById(R.id.button_scan);
        // Set result CANCELED in case the user backs out
        setResult(Activity.RESULT_CANCELED);
        if (!mBtAdapter.isEnabled()) {
            available.setVisibility(View.GONE);
            notavailable.setVisibility(View.VISIBLE);
            connectimg.setVisibility(View.GONE);
            notconnectimg.setVisibility(View.VISIBLE);
            scanButton.setText("Enable Bluetooth");
        } else {
            available.setVisibility(View.VISIBLE);
            notavailable.setVisibility(View.GONE);
            connectimg.setVisibility(View.VISIBLE);
            notconnectimg.setVisibility(View.GONE);
        }
        scanButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(DeviceListActivity.this, android.Manifest.permission.BLUETOOTH)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(DeviceListActivity.this,
                            new String[]{android.Manifest.permission.BLUETOOTH}, MY_PERMISSIONS_REQUEST_BLUETOOTH);
                }
                if (!mBtAdapter.isEnabled()) {
                    Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
                }
                doDiscovery();
                Mylogger.logAnalysePIDS( "this is my log statement");
            }
        });

        // Initialize array adapters. One for already paired devices and
        // one for newly discovered devices
        mPairedDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.list_item_layout, R.id.item_text);
        mNewDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);

        // Find and set up the ListView for paired devices
        ListView pairedListView = (ListView) findViewById(R.id.paired_devices);
        pairedListView.setAdapter(mPairedDevicesArrayAdapter);
        pairedListView.setOnItemClickListener(mDeviceClickListener);

        // Find and set up the ListView for newly discovered devices
        ListView newDevicesListView = (ListView) findViewById(R.id.new_devices);
        newDevicesListView.setAdapter(mNewDevicesArrayAdapter);
        newDevicesListView.setOnItemClickListener(mDeviceClickListener);

        // Register for broadcasts when a device is discovered
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mReceiver, filter);

        // Register for broadcasts when discovery has finished
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mReceiver, filter);

        // Get the local Bluetooth adapter
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.BLUETOOTH_CONNECT}, MY_PERMISSIONS_REQUEST_BLUETOOTH);
        }
        // Get a set of currently paired devices
        Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();

        // If there are paired devices, add each one to the ArrayAdapter
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                mPairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        }
        //   else {
//            String noDevices = getResources().getText(R.string.none_paired).toString();
//            mPairedDevicesArrayAdapter.add(noDevices);
        //  }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.BLUETOOTH_SCAN}, MY_PERMISSIONS_REQUEST_BLUETOOTH);
        }
        // Make sure we're not doing discovery anymore
        if (mBtAdapter != null) {
            mBtAdapter.cancelDiscovery();
        }

        // Unregister broadcast listeners
        this.unregisterReceiver(mReceiver);
    }

    /**
     * Start device discover with the BluetoothAdapter
     */
    private void doDiscovery() {
        if (D) Log.d(TAG, "doDiscovery()");

        // Indicate scanning in the title
        setProgressBarIndeterminateVisibility(true);
        setTitle(R.string.scanning);

        // Turn on sub-title for new devices
//        newDevicesTitle.setVisibility(View.VISIBLE);
//        newDevicesTitle.setText(R.string.scanning);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.BLUETOOTH_SCAN}, MY_PERMISSIONS_REQUEST_BLUETOOTH);
        }
        // If we're already discovering, stop it
        if (mBtAdapter.isDiscovering()) {
            mBtAdapter.cancelDiscovery();
        }

        // Request discover from BluetoothAdapter
        mBtAdapter.startDiscovery();
    }
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        // When DeviceListActivity returns with a device to connect
        if (resultCode == RESULT_OK) {
            notavailable.setVisibility(View.GONE);
            available.setVisibility(View.VISIBLE);
//                  Intent  serverIntent = new Intent(this, DeviceListActivity.class);
//                    startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
            mPairedDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.list_item_layout, R.id.item_text);
            mNewDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);

            // Find and set up the ListView for paired devices
            ListView pairedListView = (ListView) findViewById(R.id.paired_devices);
            pairedListView.setAdapter(mPairedDevicesArrayAdapter);
            pairedListView.setOnItemClickListener(mDeviceClickListener);

            // Find and set up the ListView for newly discovered devices
            ListView newDevicesListView = (ListView) findViewById(R.id.new_devices);
            newDevicesListView.setAdapter(mNewDevicesArrayAdapter);
            newDevicesListView.setOnItemClickListener(mDeviceClickListener);

            // Register for broadcasts when a device is discovered
            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            this.registerReceiver(mReceiver, filter);

            // Register for broadcasts when discovery has finished
            filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            this.registerReceiver(mReceiver, filter);

            // Get the local Bluetooth adapter
            mBtAdapter = BluetoothAdapter.getDefaultAdapter();
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.BLUETOOTH_CONNECT}, MY_PERMISSIONS_REQUEST_BLUETOOTH);
            }
            // Get a set of currently paired devices
            Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();

            // If there are paired devices, add each one to the ArrayAdapter
            if (pairedDevices.size() > 0) {
                for (BluetoothDevice device : pairedDevices) {
                    mPairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                }
            }
            scanButton.setVisibility(View.VISIBLE);
            scanButton.setText("Refresh List");
            notconnectimg.setVisibility(View.GONE);
            connectimg.setVisibility(View.VISIBLE);

        }

    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        super.onKeyDown(keyCode, event);
        if (keyCode == KeyEvent.KEYCODE_BACK) {


            Intent intent=new Intent(DeviceListActivity.this,MainActivity.class);
            startActivity(intent);


            return false;
        }

        return super.onKeyDown(keyCode, event);
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_BLUETOOTH: {
                // Check if the permission is granted
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted, perform Bluetooth operations
                    // For example, call startDiscovery() or connectToDevice() here
                } else {
                    // Permission denied, handle accordingly
                    //Toast.makeText(this, "Bluetooth permission denied", Toast.LENGTH_SHORT).show();
                }
                return;
            }
            // Add additional cases if you have more permissions to handle
        }
    }

}
