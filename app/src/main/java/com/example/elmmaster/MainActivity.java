package com.example.elmmaster;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.button.MaterialButton;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int MY_PERMISSIONS_REQUEST_BLUETOOTH = 123;

    public static final int MESSAGE_STATE_CHANGE = 1;

    /*0	Automatic protocol detection
   1	SAE J1850 PWM (41.6 kbaud)
   2	SAE J1850 VPW (10.4 kbaud)
   3	ISO 9141-2 (5 baud init, 10.4 kbaud)
   4	ISO 14230-4 KWP (5 baud init, 10.4 kbaud)
   5	ISO 14230-4 KWP (fast init, 10.4 kbaud)
   6	ISO 15765-4 CAN (11 bit ID, 500 kbaud)
   7	ISO 15765-4 CAN (29 bit ID, 500 kbaud)
   8	ISO 15765-4 CAN (11 bit ID, 250 kbaud) - used mainly on utility vehicles and Volvo
   9	ISO 15765-4 CAN (29 bit ID, 250 kbaud) - used mainly on utility vehicles and Volvo


    01 04 - ENGINE_LOAD
    01 05 - ENGINE_COOLANT_TEMPERATURE
    01 0C - ENGINE_RPM
    01 0D - VEHICLE_SPEED
    01 0F - INTAKE_AIR_TEMPERATURE
    01 10 - MASS_AIR_FLOW
    01 11 - THROTTLE_POSITION_PERCENTAGE
    01 1F - ENGINE_RUN_TIME
    01 2F - FUEL_LEVEL
    01 46 - AMBIENT_AIR_TEMPERATURE
    01 51 - FUEL_TYPE
    01 5E - FUEL_CONSUMPTION_1
    01 5F - FUEL_CONSUMPTION_2

   */

    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    protected final static char[] dtcLetters = {'P', 'C', 'B', 'U'};
    protected final static char[] hexArray = "0123456789ABCDEF".toCharArray();

    private static final String[] PIDS = {
            "01", "02", "03", "04", "05", "06", "07", "08",
            "09", "0A", "0B", "0C", "0D", "0E", "0F", "10",
            "11", "12", "13", "14", "15", "16", "17", "18",
            "19", "1A", "1B", "1C", "1D", "1E", "1F", "20"};

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 2;
    private static final int REQUEST_ENABLE_BT = 3;
    private static final float APPBAR_ELEVATION = 14f;
    private static boolean actionbar = true;
    final List<String> commandslist = new ArrayList<String>();
    ;
    final List<Double> avgconsumption = new ArrayList<Double>();
    final List<String> troubleCodesArray = new ArrayList<String>();
    MenuItem itemtemp;
    GaugeRpm rpm;
    BluetoothDevice currentdevice;
    boolean commandmode = false, initialized = false, m_getPids = false, tryconnect = false, defaultStart = false;
    String devicename = null, deviceprotocol = null;

    String[] initializeCommands;
    Intent serverIntent = null;
    String VOLTAGE = "ATRV",
            PROTOCOL = "ATDP",
            RESET = "ATZ",
            PIDS_SUPPORTED20 = "0100",
            ENGINE_COOLANT_TEMP = "0105",  //A-40
            ENGINE_RPM = "010C",  //((A*256)+B)/4
            ENGINE_LOAD = "0104",  // A*100/255
            VEHICLE_SPEED = "010D",  //A
            INTAKE_AIR_TEMP = "010F",  //A-40
            MAF_AIR_FLOW = "0110", //MAF air flow rate 0 - 655.35	grams/sec ((256*A)+B) / 100  [g/s]
            ENGINE_OIL_TEMP = "015C",  //A-40
            FUEL_RAIL_PRESSURE = "0122", // ((A*256)+B)*0.079
            INTAKE_MAN_PRESSURE = "010B", //Intake manifold absolute pressure 0 - 255 kPa
            CONT_MODULE_VOLT = "0142",  //((A*256)+B)/1000
            AMBIENT_AIR_TEMP = "0146",  //A-40
            CATALYST_TEMP_B1S1 = "013C",  //(((A*256)+B)/10)-40
            STATUS_DTC = "0101", //Status since DTC Cleared
            THROTTLE_POSITION = "0111", //Throttle position 0 -100 % A*100/255
            OBD_STANDARDS = "011C", //OBD standards this vehicle
            PIDS_SUPPORTED = "0120"; //PIDs supported
    Toolbar toolbar;
    AppBarLayout appbar;
    String trysend = null;
    private PowerManager.WakeLock wl;
    private Menu menu;
    private EditText mOutEditText;
    private Button btn_connect;
    private TextView engineLoad, voltage, coolantTemperature, Status, Loadtext, Volttext, Temptext, Centertext, Info, Air_temp, Air_txt ;
    private String mConnectedDeviceName = "Ecu";
    private int rpmval = 0, intakeairtemp = 0, ambientairtemp = 0, coolantTemp = 0, mMaf = 0,
            engineoiltemp = 0, b1s1temp = 0, Enginetype = 0, FaceColor = 0,
            whichCommand = 0, m_dedectPids = 0, connectcount = 0, trycount = 0;
    private int mEnginedisplacement = 1500;
    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the chat services
    private BluetoothService mBtService = null;


    StringBuilder inStream = new StringBuilder();

    // The Handler that gets information back from the BluetoothChatService
    // Array adapter for the conversation thread
    private ArrayAdapter<String> mConversationArrayAdapter;
    private final Handler mBtHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:

                    switch (msg.arg1) {
                        case BluetoothService.STATE_CONNECTED:

                            Status.setText(getString(R.string.title_connected_to, mConnectedDeviceName));
                             Info.setText(R.string.title_connected);
//                            try {
//                              //  itemtemp = findViewById(R.id.btn_connect);
//                                itemtemp.setTitle(R.string.disconnectbt);
//                                Info.setText(R.string.title_connected);
//                            } catch (Exception e) {
//                            }

                            tryconnect = false;
                            resetvalues();
                            sendEcuMessage(RESET);

                            break;
                        case BluetoothService.STATE_CONNECTING:
                            Status.setText(R.string.title_connecting);
                             Info.setText(R.string.tryconnectbt);
                            break;
                        case BluetoothService.STATE_LISTEN:

                        case BluetoothService.STATE_NONE:

                            Status.setText(R.string.title_not_connected);
//                            itemtemp =findViewById(R.id.btn_connect);
                            // itemtemp.setTitle(R.string.connectbt);
                            if (tryconnect) {
                                mBtService.connect(currentdevice);
                                connectcount++;
                                if (connectcount >= 2) {
                                    tryconnect = false;
                                }
                            }
                            resetvalues();

                            break;
                    }
                    break;
                case MESSAGE_WRITE:

                    byte[] writeBuf = (byte[]) msg.obj;
                    String writeMessage = new String(writeBuf);

                    if (commandmode || !initialized) {
                        mConversationArrayAdapter.add("Command:  " + writeMessage);
                    }

                    break;
                case MESSAGE_READ:

                    String tmpmsg = clearMsg(msg);

                     Info.setText(tmpmsg);

                    /*if (tmpmsg.contains(RSP_ID.NODATA.response) || tmpmsg.contains(RSP_ID.ERROR.response)) {

                        try{
                            String command = tmpmsg.substring(0,4);

                            if(isHexadecimal(command))
                            {
                                removePID(command);
                            }
                        }catch(Exception e)
                        {
                            Toast.makeText(getApplicationContext(), e.getMessage(),
                                Toast.LENGTH_LONG).show();
                        }
                    }*/

                    if (commandmode || !initialized) {
                        mConversationArrayAdapter.add(mConnectedDeviceName + ":  " + tmpmsg);
                    }
                    analysMsg(msg);
                    break;
                case MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                            Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    private void removePID(String pid)
    {
        int index = commandslist.indexOf(pid);

        if (index != -1)
        {
            commandslist.remove(index);
              Info.setText("Removed pid: " + pid);
        }
    }

    // The action listener for the EditText widget, to listen for the return key
    private TextView.OnEditorActionListener mWriteListener =
            new TextView.OnEditorActionListener() {
                public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
                    // If the action is a key-up event on the return key, send the message
                    if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
                        String message = view.getText().toString();
                        sendEcuMessage(message);
                    }
                    return true;
                }
            };

    public static boolean isHexadecimal(String text) {
        text = text.trim();

        char[] hexDigits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                'a', 'b', 'c', 'd', 'e', 'f', 'A', 'B', 'C', 'D', 'E', 'F'};

        int hexDigitsCount = 0;

        for (char symbol : text.toCharArray()) {
            for (char hexDigit : hexDigits) {
                if (symbol == hexDigit) {
                    hexDigitsCount++;
                    break;
                }
            }
        }

        return true ? hexDigitsCount == text.length() : false;
    }

    @SuppressLint("InvalidWakeLockTag")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gauges);

        // appbar = (AppBarLayout) findViewById(R.id.appbar);
        btn_connect=findViewById(R.id.btn_connect);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wl = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "My Tag");
        wl.acquire(10*60*1000L /*10 minutes*/);

        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
        );

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        Status = (TextView) findViewById(R.id.Status);
        engineLoad = (TextView) findViewById(R.id.Load);
        coolantTemperature = (TextView) findViewById(R.id.Temp);
        voltage = (TextView) findViewById(R.id.Volt);
        Loadtext = (TextView) findViewById(R.id.Load_text);
        Temptext = (TextView) findViewById(R.id.Temp_text);
        Volttext = (TextView) findViewById(R.id.Volt_text);
        // Centertext = (TextView) findViewById(R.id.Center_text);
        Info = (TextView) findViewById(R.id.info);
        rpm = (GaugeRpm) findViewById(R.id.GaugeRpm);
        Air_txt =  findViewById(R.id.air_txt);
        Air_temp = findViewById(R.id.air_temp);

        //ATZ reset all
        //ATDP Describe the current Protocol
        //ATAT0-1-2 Adaptive Timing Off - daptive Timing Auto1 - daptive Timing Auto2
        //ATE0-1 Echo Off - Echo On
        //ATSP0 Set Protocol to Auto and save it
        //ATMA Monitor All
        //ATL1-0 Linefeeds On - Linefeeds Off
        //ATH1-0 Headers On - Headers Off
        //ATS1-0 printing of Spaces On - printing of Spaces Off
        //ATAL Allow Long (>7 byte) messages
        //ATRD Read the stored data
        //ATSTFF Set time out to maximum
        //ATSTHH Set timeout to 4ms

        initializeCommands = new String[]{"ATL0", "ATE1", "ATH1", "ATAT1", "ATSTFF", "ATI", "ATDP", "ATSP0", "0100"};

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "Bluetooth is not available", Toast.LENGTH_LONG).show();
        }
        else
        {
            if (mBtService != null) {
                if (mBtService.getState() == BluetoothService.STATE_NONE) {
                    mBtService.start();
                }
            }
        }

        // Initialize the array adapter for the conversation thread
        mConversationArrayAdapter = new ArrayAdapter<String>(this, R.layout.message) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                // Get the Item from ListView
                View view = super.getView(position, convertView, parent);

                // Initialize a TextView for ListView each Item
                TextView tv = (TextView) view.findViewById(R.id.listText);

                // Set the text color of TextView (ListView Item)
                tv.setTextColor(Color.parseColor("#3ADF00"));
                tv.setTextSize(10);

                // Generate ListView Item using TextView
                return view;
            }
        };
        btn_connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {



                if (!mBluetoothAdapter.isEnabled()) {
                    Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    if (ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.BLUETOOTH_CONNECT)
                            != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{android.Manifest.permission.BLUETOOTH_CONNECT}, MY_PERMISSIONS_REQUEST_BLUETOOTH);
                    }
                    startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
                }else{Intent intent = new Intent(MainActivity.this, DeviceListActivity.class);
                    startActivityForResult(intent, REQUEST_CONNECT_DEVICE);
                    finish();
                }



                if (mBtService == null) setupChat();

//                if (item.getTitle().equals("ConnectBT")) {
//                    // Launch the DeviceListActivity to see devices and do scan
//                    serverIntent = new Intent(this, DeviceListActivity.class);
//                    startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
//                } else {
//                    if (mBtService != null)
//                    {
//                        mBtService.stop();
//                        item.setTitle(R.string.connectbt);
//                    }
//                }

            }
        });
        RelativeLayout rlayout = (RelativeLayout) findViewById(R.id.mainscreen);
        getPreferences();

        resetgauges();
    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu; this adds items to the action bar if it is present.
//
//        this.menu = menu;
//
//        getMenuInflater().inflate(R.menu.menu_main, menu);
//        return true;
//    }



    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == MainActivity.RESULT_OK) {
                    connectDevice(data);
                }
                break;

            case REQUEST_ENABLE_BT:

                if (mBtService == null) setupChat();

                if (resultCode == MainActivity.RESULT_OK) {
                    serverIntent = new Intent(this, DeviceListActivity.class);
                    startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
                } else {
                    Toast.makeText(getApplicationContext(), "BT device not enabled", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }



    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        setDefaultOrientation();
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        getPreferences();
    }

    @Override
    public synchronized void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mBtService != null) mBtService.stop();

        wl.release();
    }

    @Override
    public void onStart() {
        super.onStart();
        getPreferences();
        setDefaultOrientation();
        resetvalues();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        super.onKeyDown(keyCode, event);
        if (keyCode == KeyEvent.KEYCODE_BACK) {


            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            alertDialogBuilder.setMessage("Are you sure you want exit?");
            alertDialogBuilder.setPositiveButton("Ok",
                    new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface arg0, int arg1) {
                            exit();
                        }
                    });

            alertDialogBuilder.setNegativeButton("cancel",
                    new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface arg0, int arg1) {

                        }
                    });

            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();


            return false;
        }

        return super.onKeyDown(keyCode, event);
    }

    private void exit() {
        if (mBtService != null) mBtService.stop();
        wl.release();
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    private void getPreferences() {

        SharedPreferences preferences = PreferenceManager
                .getDefaultSharedPreferences(getBaseContext());

        FaceColor = Integer.parseInt(preferences.getString("FaceColor", "0"));

        rpm.setFace(FaceColor);

        mEnginedisplacement = Integer.parseInt(preferences.getString("Enginedisplacement", "1500"));

        m_dedectPids = Integer.parseInt(preferences.getString("DedectPids", "0"));

        if (m_dedectPids == 0) {

            commandslist.clear();

            int i = 0;

            commandslist.add(i, VOLTAGE);

            if (preferences.getBoolean("checkboxENGINE_RPM", true)) {
                commandslist.add(i, ENGINE_RPM);
                i++;
            }

            if (preferences.getBoolean("checkboxVEHICLE_SPEED", true)) {
                commandslist.add(i, VEHICLE_SPEED);
                i++;
            }

            if (preferences.getBoolean("checkboxENGINE_LOAD", true)) {
                commandslist.add(i, ENGINE_LOAD);
                i++;
            }

            if (preferences.getBoolean("checkboxENGINE_COOLANT_TEMP", true)) {
                commandslist.add(i, ENGINE_COOLANT_TEMP);
                i++;
            }

            if (preferences.getBoolean("checkboxINTAKE_AIR_TEMP", true)) {
                commandslist.add(i, INTAKE_AIR_TEMP);
                i++;
            }

            if (preferences.getBoolean("checkboxMAF_AIR_FLOW", true)) {
                commandslist.add(i, MAF_AIR_FLOW);
            }

            whichCommand = 0;
        }
    }

    private void setDefaultOrientation() {

        try {

            settextsixe();
            setgaugesize();

        } catch (Exception e) {
        }
    }

    private void settextsixe() {
        int txtsize = 14;
        int sttxtsize = 12;

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindow().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        Status.setTextSize(sttxtsize);
        coolantTemperature.setTextSize(txtsize);
        engineLoad.setTextSize(txtsize);
        voltage.setTextSize(txtsize);
        Temptext.setTextSize(txtsize);
        Air_txt.setText(txtsize);
        Air_temp.setText(txtsize);
        Loadtext.setTextSize(txtsize);
        Volttext.setTextSize(txtsize);
        // Info.setTextSize(sttxtsize);
    }


    private void setgaugesize() {
        Display display = getWindow().getWindowManager().getDefaultDisplay();
        int width = 0;
        int height = 0;

        width = display.getWidth();
        height = display.getHeight();

        if (width > height) {
            RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(height, height);

            lp.addRule(RelativeLayout.BELOW, findViewById(R.id.Load).getId());
            lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            lp.setMargins(0, 0, 50, 0);
            rpm.setLayoutParams(lp);
            rpm.getLayoutParams().height = height;
            rpm.getLayoutParams().width = (int) (width - 100) / 2;

            lp = new RelativeLayout.LayoutParams(height, height);
            lp.addRule(RelativeLayout.BELOW, findViewById(R.id.Load).getId());
            lp.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            lp.setMargins(50, 0, 0, 0);

        } else if (width < height) {
            RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(width, width);

            lp.addRule(RelativeLayout.CENTER_HORIZONTAL);
            lp.setMargins(25, 5, 25, 5);
            rpm.setLayoutParams(lp);
            rpm.getLayoutParams().height = height/2;
            rpm.getLayoutParams().width = (int) (width);

            lp = new RelativeLayout.LayoutParams(width, width);
            lp.addRule(RelativeLayout.BELOW, findViewById(R.id.GaugeRpm).getId());
            //lp.addRule(RelativeLayout.ABOVE,findViewById(R.id.info).getId());
            lp.addRule(RelativeLayout.CENTER_HORIZONTAL);
            lp.setMargins(25, 5, 25, 5);
        }
    }

    public void resetgauges() {

        rpm.setTargetValue(80);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        rpm.setTargetValue(0);
                    }
                });
            }
        }).start();
    }

    public void resetvalues() {

        engineLoad.setText("0 %");
        voltage.setText("0 V");
        coolantTemperature.setText("0 C°");
         Info.setText("");
        Air_temp.setText("0 C°");
        m_getPids = false;
        whichCommand = 0;
        trycount = 0;
        initialized = false;
        defaultStart = false;
        avgconsumption.clear();
        mConversationArrayAdapter.clear();

        resetgauges();
    }

    private void connectDevice(Intent data) {
        tryconnect = true;
        // Get the device MAC address
        String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        try {
            // Attempt to connect to the device
            mBtService.connect(device);
            currentdevice = device;

        } catch (Exception e) {
        }
    }

    private void setupChat() {

        // Initialize the BluetoothChatService to perform bluetooth connections
        mBtService = new BluetoothService(this, mBtHandler);

    }

    private void sendEcuMessage(String message) {
        if (mBtService != null)
        {
            // Check that we're actually connected before trying anything
            if (mBtService.getState() != BluetoothService.STATE_CONNECTED) {
                //Toast.makeText(this, R.string.not_connected, Toast.LENGTH_LONG).show();
                return;
            }
            try {
                if (message.length() > 0) {

                    message = message + "\r";
                    // Get the message bytes and tell the BluetoothChatService to write
                    byte[] send = message.getBytes();
                    mBtService.write(send);
                }
            } catch (Exception e) {
            }
        }
    }

    private void sendInitCommands() {
        if (initializeCommands.length != 0) {

            if (whichCommand < 0) {
                whichCommand = 0;
            }

            String send = initializeCommands[whichCommand];
            sendEcuMessage(send);

            if (whichCommand == initializeCommands.length - 1) {
                initialized = true;
                whichCommand = 0;
                sendDefaultCommands();
            } else {
                whichCommand++;
            }
        }
    }

    private void sendDefaultCommands() {

        if (commandslist.size() != 0) {

            if (whichCommand < 0) {
                whichCommand = 0;
            }

            String send = commandslist.get(whichCommand);
            sendEcuMessage(send);

            if (whichCommand >= commandslist.size() - 1) {
                whichCommand = 0;
            } else {
                whichCommand++;
            }
        }
    }

    private String clearMsg(Message msg) {
        String tmpmsg = msg.obj.toString();

        tmpmsg = tmpmsg.replace("null", "");
        tmpmsg = tmpmsg.replaceAll("\\s", ""); //removes all [ \t\n\x0B\f\r]
        tmpmsg = tmpmsg.replaceAll(">", "");
        tmpmsg = tmpmsg.replaceAll("SEARCHING...", "");
        tmpmsg = tmpmsg.replaceAll("ATZ", "");
        tmpmsg = tmpmsg.replaceAll("ATI", "");
        tmpmsg = tmpmsg.replaceAll("atz", "");
        tmpmsg = tmpmsg.replaceAll("ati", "");
        tmpmsg = tmpmsg.replaceAll("ATDP", "");
        tmpmsg = tmpmsg.replaceAll("atdp", "");
        tmpmsg = tmpmsg.replaceAll("ATRV", "");
        tmpmsg = tmpmsg.replaceAll("atrv", "");

        return tmpmsg;
    }

    private void checkPids(String tmpmsg) {
        if (tmpmsg.indexOf("41") != -1) {
            int index = tmpmsg.indexOf("41");

            String pidmsg = tmpmsg.substring(index, tmpmsg.length());
            Log.d("pidmsg",pidmsg);
            if (pidmsg.contains("41000000000")) {

                setPidsSupported(pidmsg);
                return;
            }
        }
    }

    private void analysMsg(Message msg) {

        String tmpmsg = clearMsg(msg);

        generateVolt(tmpmsg);

        getElmInfo(tmpmsg);

        if (!initialized) {

            sendInitCommands();

        } else {

            checkPids(tmpmsg);

            if (!m_getPids && m_dedectPids == 1) {
                String sPIDs = "0100";
                sendEcuMessage(sPIDs);
                return;
            }


            try {
                analysPIDS(tmpmsg);
            } catch (Exception e) {
                  Info.setText("Error : " + e.getMessage());
            }

            sendDefaultCommands();
        }
    }




    private byte hexStringToByteArray(char s) {
        return (byte) ((Character.digit(s, 16) << 4));
    }

    private void getElmInfo(String tmpmsg) {

        if (tmpmsg.contains("ELM") || tmpmsg.contains("elm")) {
            devicename = tmpmsg;
        }

        if (tmpmsg.contains("SAE") || tmpmsg.contains("ISO")
                || tmpmsg.contains("sae") || tmpmsg.contains("iso") || tmpmsg.contains("AUTO")) {
            deviceprotocol = tmpmsg;
        }

        if (deviceprotocol != null && devicename != null) {
            devicename = devicename.replaceAll("STOPPED", "");
            deviceprotocol = deviceprotocol.replaceAll("STOPPED", "");
            Status.setText(devicename + " " + deviceprotocol);
        }
    }


    private void setPidsSupported(String buffer) {

          Info.setText("Trying to get available pids : " + String.valueOf(trycount));
        trycount++;

        StringBuilder flags = new StringBuilder();
        String buf = buffer.toString();
        buf = buf.trim();
        buf = buf.replace("\t", "");
        buf = buf.replace(" ", "");
        buf = buf.replace(">", "");

        if (buf.indexOf("4100") == 0 || buf.indexOf("4120") == 0) {

            for (int i = 0; i < 8; i++) {
                String tmp = buf.substring(i + 4, i + 5);
                int data = Integer.valueOf(tmp, 16).intValue();
//                String retStr = Integer.toBinaryString(data);
                if ((data & 0x08) == 0x08) {
                    flags.append("1");
                } else {
                    flags.append("0");
                }

                if ((data & 0x04) == 0x04) {
                    flags.append("1");
                } else {
                    flags.append("0");
                }

                if ((data & 0x02) == 0x02) {
                    flags.append("1");
                } else {
                    flags.append("0");
                }

                if ((data & 0x01) == 0x01) {
                    flags.append("1");
                } else {
                    flags.append("0");
                }
                if ((data & 0x09) == 0x09) {
                    flags.append("1");
                } else {
                    flags.append("0");
                }
                if ((data & 0x06) == 0x06) {
                    flags.append("1");
                } else {
                    flags.append("0");
                }
                if ((data & 0x07) == 0x07) {
                    flags.append("1");
                } else {
                    flags.append("0");
                }
                if ((data & 0x05) == 0x05) {
                    flags.append("1");
                } else {
                    flags.append("0");
                }
                if ((data & 0x00) == 0x00) {
                    flags.append("1");
                } else {
                    flags.append("0");
                }
                if ((data & 0x03) == 0x03) {
                    flags.append("1");
                } else {
                    flags.append("0");
                }
                if ((data & 0x0A) == 0x0A) {
                    flags.append("1");
                } else {
                    flags.append("0");
                }


            }

            commandslist.clear();
            commandslist.add(0, VOLTAGE);
            int pid = 1;

            StringBuilder supportedPID = new StringBuilder();
            supportedPID.append("Supported PIDS:\n");
            for (int j = 0; j < flags.length(); j++) {
                if (flags.charAt(j) == '1') {
                    supportedPID.append(" " + PIDS[j] + " ");
                    if (!PIDS[j].contains("11") && !PIDS[j].contains("01") && !PIDS[j].contains("20")) {
                        commandslist.add(pid, "01" + PIDS[j]);
                        pid++;
                    }
                }
            }
            m_getPids = true;
            mConversationArrayAdapter.add(mConnectedDeviceName + ": " + supportedPID.toString());
            whichCommand = 0;
            sendEcuMessage("ATRV");

        } else {

            return;
        }
    }

    private double calculateAverage(List<Double> listavg) {
        Double sum = 0.0;
        for (Double val : listavg) {
            sum += val;
        }
        return sum.doubleValue() / listavg.size();
    }
    private final Handler handler = new Handler();
    private void analysPIDS(String dataRecieved) {

        int A = 0;
        int B = 0;
        String PID1 = null;
        double val = 0;
        int intval = 0;
        int tempC = 0;

        if ((dataRecieved != null) && (dataRecieved.matches("^[0-9A-F]+$"))) {

            dataRecieved = dataRecieved.trim();
            String dt2=dataRecieved.substring(5);
            Log.d("mydata",dataRecieved);
            int index = dt2.indexOf("41");
           Log.d("index", String.valueOf(index));
            String tmpmsg = null;
            String tmpmsg1 = null;

            if (index != -1) {

                tmpmsg1 = dt2.substring(index, dt2.length());
                Log.d("tmpmsg1",tmpmsg1);
                if (tmpmsg1.substring(0, 2).equals("41")) {
                    PID1 = tmpmsg1.substring(2, 4);
                    String A1=tmpmsg1.substring(4,6);
                    A = Integer.parseInt(tmpmsg1.substring(4, 6), 16);
                    Log.d("A", String.valueOf(A));

                    // String B1=tmpmsg1.substring(6,8);
                    Log.d("valueB", String.valueOf(B));
                    Log.d("mypid", String.valueOf(PID1));


                    switch (PID1) {

                        case "04"://PID(04): Engine Load

                            // A*100/255
                            Log.d("loadA", String.valueOf(A));
                            val = A * 100 / 255;
                            Log.d("val", String.valueOf(val));
                            int calcLoad = (int) val;

                            engineLoad.setText(Integer.toString(calcLoad) + " %");
                            mConversationArrayAdapter.add("Engine Load: " + Integer.toString(calcLoad) + " %");
                            Log.d("load", String.valueOf(calcLoad));
                            double FuelFlowLH = (mMaf * calcLoad * mEnginedisplacement / 1000.0 / 714.0) + 0.8;

                            if (calcLoad == 0)
                                FuelFlowLH = 0;

                            avgconsumption.add(FuelFlowLH);
                            mConversationArrayAdapter.add("Fuel Consumption: " + String.format("%10.1f", calculateAverage(avgconsumption)).trim() + " l/h");
                            break;

                        case "05"://PID(05): Coolant Temperature
                            Log.d("coolantA", String.valueOf(A));
                            // A-40
                            tempC = A - 40;
                            coolantTemp = tempC;
                            coolantTemperature.setText(Integer.toString(coolantTemp) + " C°");
                            mConversationArrayAdapter.add("Enginetemp: " + Integer.toString(tempC) + " C°");
                            Log.d("coolantemp", String.valueOf(coolantTemp));
                            break;

                        case "0F"://PID(0F): Intake Temperature
                            tempC = A - 40;
                            intakeairtemp = tempC;
                            Air_temp.setText(Integer.toString(intakeairtemp) + " C°");
                            mConversationArrayAdapter.add("Intakeairtemp: " + Integer.toString(intakeairtemp) + " C°");

                            break;
                        case "0C": //PID(0C): RPM

                            //((A*256)+B)/4
                            B = Integer.parseInt(tmpmsg1.substring(6, 8), 16);
                            Log.d("rpmB", String.valueOf(B));
                            val = ((A * 256) + B) / 4;
                            intval = (int) val;
                            rpmval = intval;
                            Log.d("rpm", String.valueOf(intval));
                            rpm.setTargetValue(intval / 100);

                            break;
                    }

                }
            }else{
                tmpmsg1 = dataRecieved.trim();
                Mylogger.logAnalysePIDS( "my tmpmsg1: "+ tmpmsg1);
                if (tmpmsg1.substring(0, 2).equals("01")) {
                    PID1 = tmpmsg1.substring(2, 4);
                  //  String A1=tmpmsg1.substring(4,6);
                    A = Integer.parseInt(tmpmsg1.substring(4, 6), 16);
                    Log.d("A2", String.valueOf(A));

                    // String B1=tmpmsg1.substring(6,8);
                    Log.d("valueB2", String.valueOf(B));
                    Log.d("mypid2", String.valueOf(PID1));


                    switch (PID1) {

                        case "04"://PID(04): Engine Load

                            // A*100/255
                            Log.d("loadA", String.valueOf(A));
                            val = A * 100 / 255;
                            Log.d("val", String.valueOf(val));
                            int calcLoad = (int) val;

                            engineLoad.setText(Integer.toString(calcLoad) + " %");
                            mConversationArrayAdapter.add("Engine Load: " + Integer.toString(calcLoad) + " %");
                            Log.d("load2", String.valueOf(calcLoad));
                            double FuelFlowLH = (mMaf * calcLoad * mEnginedisplacement / 1000.0 / 714.0) + 0.8;

                            if (calcLoad == 0)
                                FuelFlowLH = 0;

                            avgconsumption.add(FuelFlowLH);
                            mConversationArrayAdapter.add("Fuel Consumption: " + String.format("%10.1f", calculateAverage(avgconsumption)).trim() + " l/h");
                            break;

                        case "05"://PID(05): Coolant Temperature
                            Log.d("coolantA", String.valueOf(A));
                            // A-40
                            tempC = A - 40;
                            coolantTemp = tempC;
                            coolantTemperature.setText(Integer.toString(coolantTemp) + " C°");
                            mConversationArrayAdapter.add("Enginetemp: " + Integer.toString(tempC) + " C°");
                            Log.d("coolant2", String.valueOf(coolantTemp));
                            break;

                        case "0F"://PID(0F): Intake Temperature
                            tempC = A - 40;
                            intakeairtemp = tempC;
                            Air_temp.setText(Integer.toString(intakeairtemp) + " C°");
                            mConversationArrayAdapter.add("Intakeairtemp: " + Integer.toString(intakeairtemp) + " C°");
                            Log.d("air2", String.valueOf(intakeairtemp));
                            break;
                        case "0C": //PID(0C): RPM

                            //((A*256)+B)/4
                            B = Integer.parseInt(tmpmsg1.substring(6, 8), 16);
                            Log.d("rpmB", String.valueOf(B));
                            val = ((A * 256) + B) / 4;
                            intval = (int) val;
                            rpmval = intval;
                            Log.d("rpm2", String.valueOf(intval));
                            rpm.setTargetValue(intval / 100);

                            break;
                    }

                }
            }

        }

    }
    private void generateVolt(String msg) {

        String VoltText = null;

        if ((msg != null) && (msg.matches("\\s*[0-9]{1,2}([.][0-9]{1,2})\\s*"))) {

            VoltText = msg + "V";

            mConversationArrayAdapter.add(mConnectedDeviceName + ": " + msg + "V");

        } else if ((msg != null) && (msg.matches("\\s*[0-9]{1,2}([.][0-9]{1,2})?V\\s*"))) {

            VoltText = msg;

            mConversationArrayAdapter.add(mConnectedDeviceName + ": " + msg);
        }

        if (VoltText != null) {
            voltage.setText(VoltText);
        }
    }

    private void calculateEcuValues(String PID, int A, int B) {

        double val = 0;
        int intval = 0;
        int tempC = 0;
        Log.d("pid123", String.valueOf(PID));
        switch (PID) {

            case "04"://PID(04): Engine Load

                // A*100/255
                Log.d("loadA", String.valueOf(A));
                val = A * 100 / 255;
                Log.d("val", String.valueOf(val));
                int calcLoad = (int) val;

                engineLoad.setText(Integer.toString(calcLoad) + " %");
                mConversationArrayAdapter.add("Engine Load: " + Integer.toString(calcLoad) + " %");
                Log.d("load", String.valueOf(calcLoad));
                double FuelFlowLH = (mMaf * calcLoad * mEnginedisplacement / 1000.0 / 714.0) + 0.8;

                if(calcLoad == 0)
                    FuelFlowLH = 0;

                avgconsumption.add(FuelFlowLH);
                mConversationArrayAdapter.add("Fuel Consumption: " + String.format("%10.1f", calculateAverage(avgconsumption)).trim() + " l/h");
                break;

            case "05"://PID(05): Coolant Temperature
                Log.d("coolantA", String.valueOf(A));
                // A-40
                tempC = A - 40;
                coolantTemp = tempC;
                coolantTemperature.setText(Integer.toString(coolantTemp) + " C°");
                mConversationArrayAdapter.add("Enginetemp: " + Integer.toString(tempC) + " C°");
                break;

            case "0B"://PID(0B)

                // A
                mConversationArrayAdapter.add("Intake Man Pressure: " + Integer.toString(A) + " kPa");

                break;

            case "0C": //PID(0C): RPM

                //((A*256)+B)/4
                val = ((A * 256) + B) / 4;
                intval = (int) val;
                rpmval = intval;
                rpm.setTargetValue(intval / 100);

                break;


            case "0D"://PID(0D): KM


                break;

            case "10"://PID(10): Maf
                break;

            case "11"://PID(11)

                //A*100/255
                val = A * 100 / 255;
                intval = (int) val;
                mConversationArrayAdapter.add(" Throttle position: " + Integer.toString(intval) + " %");

                break;

            case "23"://PID(23)

                // ((A*256)+B)*0.079
                val = ((A * 256) + B) * 0.079;
                intval = (int) val;
                mConversationArrayAdapter.add("Fuel Rail Pressure: " + Integer.toString(intval) + " kPa");

                break;

            case "31"://PID(31)

                //(256*A)+B km
                val = (A * 256) + B;
                intval = (int) val;
                mConversationArrayAdapter.add("Distance traveled: " + Integer.toString(intval) + " km");

                break;

            case "46" ://PID(46)

                // A-40 [DegC]
                tempC = A - 40;
                ambientairtemp = tempC;
                mConversationArrayAdapter.add("Ambientairtemp: " + Integer.toString(ambientairtemp) + " C°");

                break;

            case "5C" : //PID(5C)

                //A-40
                tempC = A - 40;
                engineoiltemp = tempC;
                mConversationArrayAdapter.add("Engineoiltemp: " + Integer.toString(engineoiltemp) + " C°");
                Log.d("oiltemp", String.valueOf(engineoiltemp));
                break;

            default:
        }
    }

    enum RSP_ID {
        PROMPT(">"),
        OK("OK"),
        MODEL("ELM"),
        NODATA("NODATA"),
        SEARCH("SEARCHING"),
        ERROR("ERROR"),
        NOCONN("UNABLE"),
        NOCONN_MSG("UNABLE TO CONNECT"),
        NOCONN2("NABLETO"),
        CANERROR("CANERROR"),
        CONNECTED("ECU CONNECTED"),
        BUSBUSY("BUSBUSY"),
        BUSY("BUSY"),
        BUSERROR("BUSERROR"),
        BUSINIERR("BUSINIT:ERR"),
        BUSINIERR2("BUSINIT:BUS"),
        BUSINIERR3("BUSINIT:...ERR"),
        BUS("BUS"),
        FBERROR("FBERROR"),
        DATAERROR("DATAERROR"),
        BUFFERFULL("BUFFERFULL"),
        STOPPED("STOPPED"),
        RXERROR("<"),
        QMARK("?"),
        UNKNOWN("");
        private String response;

        RSP_ID(String response) {
            this.response = response;
        }

        @Override
        public String toString() {
            return response;
        }
    }
}