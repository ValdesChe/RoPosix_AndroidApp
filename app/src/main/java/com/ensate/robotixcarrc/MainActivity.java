package com.ensate.robotixcarrc;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.annotation.RequiresApi;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getName();

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    private static final int UPDATE_SETTING = 3;



    /**
     * String buffer for outgoing messages
     */
    private StringBuffer mOutStringBuffer;

    /**
     * My options menu
     */
    private MenuItem mConnectScanMenuItem;


    /**
     * control buttons
     */
    private ImageButton mMoveRightButton;
    private ImageButton mMoveLeftButton;
    private ImageButton mMoveUpButton;
    private ImageButton mMoveDownButton;
    private ImageButton mControlFrontLightButton;

    /*
        Seekbar Control for robot speed
     */
    private SeekBar mSpeedSeekBar;

    /*
        Switch controls for mode and power
     */
    private Switch mModeSwitch;
    private Switch mStateSwitch;

    /*

     */
    private View mDecorView;

    /*
        Preference object
     */
    private SharedPreferences mPrefs;

    /*
        Varaibales for checking the robot state
     */
    private boolean isModeAutoEnabled = false;
    private boolean isRobotEnabled = false;
    private boolean isFrontLightOn = false;


    private boolean doubleBackToExitPressedOnce = false;

    private boolean isVertical = false;
    private char isVerticalVal = ' ';
    private boolean isHorizontal = false;
    private char isHorizontalVal = ' ';


    /*
        Instance of MQTT Client Android
     */
    private MqttAndroidClient mqttAndroidClient;
    String clientId = "AndroidRoPosix";

    private View.OnTouchListener mControlsButtonOnTouchListener = new View.OnTouchListener() {

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            ImageButton imageButton = (ImageButton) v;
            Resources res = getResources();
            if (mqttAndroidClient != null && event.getAction() == MotionEvent.ACTION_DOWN  ) {
                ConstraintLayout constraintLayout = findViewById(R.id.statusBoard);
                switch (v.getId()) {

                      /*
                        Front Lights
                     */
                    case R.id.btn1s:
                        System.out.println("BackLight");
                        if (isFrontLightOn) {
                            imageButton.setImageDrawable(getDrawable(R.drawable.btn_2servo_off));
                            publishMessage(Constants.CAR_LIGHT_TOPIC, "OFF" );
                            isFrontLightOn = false;
                        }else{
                            imageButton.setImageDrawable(getDrawable(R.drawable.btn_2servo_on));
                            publishMessage(Constants.CAR_LIGHT_TOPIC, "ON" );
                            isFrontLightOn = true;
                        }

                        break;




                        /*
                            Direction/Move controls actions
                         */
                    case R.id.upButton:
                        isVertical = true;
                        imageButton.setImageDrawable(getDrawable(R.drawable.btn_top_hover));
                        if (isHorizontal && isHorizontalVal == 'L') {
                            System.out.println("TOP - LEFT");
                            publishMessage(Constants.CAR_MOVE_TOPIC, mPrefs.getString(res.getString(R.string.key_pref_pos_up_left),
                                    res.getString(R.string.pref_default_pos_up_left)));
                            constraintLayout.setBackgroundColor(Color.rgb(55, 32, 5));
                        } else if (isHorizontal && isHorizontalVal == 'R') {
                            System.out.println("TOP - RIGHT");
                            constraintLayout.setBackgroundColor(Color.rgb(155, 132, 45));
                            publishMessage(Constants.CAR_MOVE_TOPIC, mPrefs.getString(res.getString(R.string.key_pref_pos_up_right),
                                    res.getString(R.string.pref_default_pos_up_right)));
                            constraintLayout.setBackgroundColor(Color.rgb(55, 32, 5));
                        } else {
                            System.out.println("TOP");
                            isVerticalVal = 'T';
                            publishMessage(Constants.CAR_MOVE_TOPIC, mPrefs.getString(res.getString(R.string.key_pref_pos_up),
                                    res.getString(R.string.pref_default_pos_up)));
                            constraintLayout.setBackgroundColor(Color.rgb(5, 2, 0));
                        }
                        break;
                    case R.id.downButton:
                        isVertical = true;

                        imageButton.setImageDrawable(getDrawable(R.drawable.btn_bottom_hover));
                        if (isHorizontal && isHorizontalVal == 'L') {
                            System.out.println("DOWN - LEFT");
                            publishMessage(Constants.CAR_MOVE_TOPIC, mPrefs.getString(res.getString(R.string.key_pref_pos_down_left),
                                    res.getString(R.string.pref_default_pos_down_left)));
                            constraintLayout.setBackgroundColor(Color.rgb(205, 3, 53));
                        } else if (isHorizontal && isHorizontalVal == 'R') {
                            System.out.println("DOWN - RIGHT");
                            publishMessage(Constants.CAR_MOVE_TOPIC, mPrefs.getString(res.getString(R.string.key_pref_pos_down_right),
                                    res.getString(R.string.pref_default_pos_down_right)));
                            constraintLayout.setBackgroundColor(Color.rgb(155, 132, 45));
                        } else {
                            System.out.println("DOWN");
                            isVerticalVal = 'D';
                            publishMessage(Constants.CAR_MOVE_TOPIC, mPrefs.getString(res.getString(R.string.key_pref_pos_down),
                                    res.getString(R.string.pref_default_pos_down)));
                            constraintLayout.setBackgroundColor(Color.rgb(105, 52, 205));
                        }
                        break;
                    case R.id.leftButton:
                        isHorizontal = true;
                        imageButton.setImageDrawable(getDrawable(R.drawable.btn_left_hover));
                        if (isVertical && isVerticalVal == 'T') {
                            System.out.println("TOP - LEFT");
                            publishMessage(Constants.CAR_MOVE_TOPIC, mPrefs.getString(res.getString(R.string.key_pref_pos_up_left),
                                    res.getString(R.string.pref_default_pos_up_left)));
                            constraintLayout.setBackgroundColor(Color.rgb(145, 3, 3));
                        } else if (isVertical && isVerticalVal == 'D') {
                            System.out.println("DOWN - LEFT");
                            publishMessage(Constants.CAR_MOVE_TOPIC, mPrefs.getString(res.getString(R.string.pref_default_pos_down_left),
                                    res.getString(R.string.pref_default_pos_down_left)));
                            constraintLayout.setBackgroundColor(Color.rgb(15, 132, 245));
                        } else {
                            System.out.println("LEFT");
                            publishMessage(Constants.CAR_MOVE_TOPIC, mPrefs.getString(res.getString(R.string.key_pref_pos_left),
                                    res.getString(R.string.pref_default_pos_left)));
                            isHorizontalVal = 'L';
                            constraintLayout.setBackgroundColor(Color.rgb(105, 92, 25));
                        }
                        break;
                    case R.id.rightButton:
                        isHorizontal = true;
                        imageButton.setImageDrawable(getDrawable(R.drawable.btn_right_hover));
                        if (isVertical && isVerticalVal == 'T') {
                            System.out.println("TOP - RIGHT");
                            publishMessage(Constants.CAR_MOVE_TOPIC, mPrefs.getString(res.getString(R.string.pref_default_pos_up_right),
                                    res.getString(R.string.pref_default_pos_up_right)));
                            constraintLayout.setBackgroundColor(Color.rgb(15, 30, 123));
                        } else if (isVertical && isVerticalVal == 'D') {
                            System.out.println("DOWN - RIGHT");
                            publishMessage(Constants.CAR_MOVE_TOPIC, mPrefs.getString(res.getString(R.string.key_pref_pos_down_right),
                                    res.getString(R.string.pref_default_pos_down_right)));
                            constraintLayout.setBackgroundColor(Color.rgb(105, 32, 5));
                        } else {
                            System.out.println("RIGHT");
                            isHorizontalVal = 'R';
                            publishMessage(Constants.CAR_MOVE_TOPIC, mPrefs.getString(res.getString(R.string.key_pref_pos_right),
                                    res.getString(R.string.pref_default_pos_right)));
                            constraintLayout.setBackgroundColor(Color.rgb(205, 9, 125));
                        }
                        break;
                }
                // imageButton.setBackgroundColor(getResources().getColor(R.color.btn_bg_pressed));
                if (mPrefs.getBoolean(getResources().getString(R.string.key_pref_vibrate_switch), true)) {
                    Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                    vibrator.vibrate(50);
                }
                // publishMessage(Constants.CAR_MOVE_TOPIC, button);
                return true;
            } else if (mqttAndroidClient != null && event.getAction() == MotionEvent.ACTION_UP) {
                switch (v.getId()) {
                    case R.id.upButton:
                        isVertical = false;
                        isVerticalVal = ' ';
                        imageButton.setImageDrawable(null);
                        imageButton.setImageDrawable(getDrawable(R.drawable.btn_top));
                        break;
                    case R.id.downButton:
                        isVertical = false;
                        isVerticalVal = ' ';
                        imageButton.setImageDrawable(null);
                        imageButton.setImageDrawable(getDrawable(R.drawable.btn_bottom));
                        break;
                    case R.id.leftButton:
                        isHorizontal = false;
                        isHorizontalVal = ' ';
                        imageButton.setImageDrawable(null);
                        imageButton.setImageDrawable(getDrawable(R.drawable.btn_left));
                        break;
                    case R.id.rightButton:
                        isHorizontal = false;
                        isHorizontalVal = ' ';
                        imageButton.setImageDrawable(null);
                        imageButton.setImageDrawable(getDrawable(R.drawable.btn_right));
                        break;
                }
                imageButton.setBackgroundColor(getResources().getColor(R.color.btn_bg_default));
                return true;
            }
            return false;
        }
    };

    SeekBar.OnSeekBarChangeListener mSpeedSeekBarListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (mqttAndroidClient == null || false) {
                mSpeedSeekBar.setProgress(0);
                return;
            }
            // Sending data
            publishMessage(Constants.CAR_SPEED_TOPIC, Integer.toString(progress));
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // set view
        mDecorView = getWindow().getDecorView();
        // hide system ui
        hideSystemUI();

        //initMembers and setup control pad
        setupControlPadView();
        //load user prefs
        loadPreference();

        connectToMQTTBroker();
    }

    public void connectToMQTTBroker() {
        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setAutomaticReconnect(true);
        mqttConnectOptions.setCleanSession(false);
        mqttConnectOptions.setUserName(Constants.MQTT_USERNAME);
        mqttConnectOptions.setPassword(Constants.MQTT_PASSWORD.toCharArray());

        // Here we generate a client ID with current milliseconds value
        clientId = clientId + System.currentTimeMillis();
        mqttAndroidClient = new MqttAndroidClient(getApplicationContext(),
                Constants.MQTT_SERVER_NAME+":"+ Constants.MQTT_PORT, clientId);
        mqttAndroidClient.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {

                if (reconnect) {
                    TextView textView = findViewById(R.id.textView);
                    textView.setText("Reconnected to : " + serverURI);
                    // Because Clean Session is true, we need to re-subscribe
                } else {
                    TextView textView = findViewById(R.id.textView);
                    textView.setText("Connected to: " + serverURI);
                }
            }

            @Override
            public void connectionLost(Throwable cause) {
                TextView textView = findViewById(R.id.textView);
                textView.setText("The Connection was lost.");
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                TextView textView = findViewById(R.id.textView);
                textView.setText("Incoming message: " + new String(message.getPayload()));
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });




        try {
            TextView textView = findViewById(R.id.textView);
            textView.setText("Connecting to " + Constants.MQTT_SERVER_NAME);
            mqttAndroidClient.connect(mqttConnectOptions, getApplicationContext(), new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
                    disconnectedBufferOptions.setBufferEnabled(true);
                    disconnectedBufferOptions.setBufferSize(100);
                    disconnectedBufferOptions.setPersistBuffer(false);
                    disconnectedBufferOptions.setDeleteOldestMessages(false);
                    mqttAndroidClient.setBufferOpts(disconnectedBufferOptions);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    TextView textView = findViewById(R.id.textView);
                    textView.setText("Failed to connect to MQTT Server");
                }
            });

        } catch (MqttException ex) {
            ex.printStackTrace();
        }

    }


    public void publishMessage(String topicToPublish , String msg) {

        try {

            MqttMessage message = new MqttMessage();
            message.setPayload(String.valueOf(msg).getBytes());
            mqttAndroidClient.publish(topicToPublish, message);

             TextView textView = findViewById(R.id.textView);
                    textView.setText("Message Published to "+ topicToPublish + " : {"+msg+"}");

            if (!mqttAndroidClient.isConnected()) {
                    textView.setText("messages in buffer");
            }

        } catch (MqttException e) {
             TextView textView = findViewById(R.id.textView);
             textView.setText("Error Publishing");
            System.err.println("Error Publishing: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        mConnectScanMenuItem = menu.getItem(0);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.full_screen_menu_item:
            case R.id.action_full_screen: {
                hideSystemUI();
                return true;
            }
            case R.id.action_settings: {
                Intent i = new Intent(getApplicationContext(), SettingsActivity.class);
                startActivityForResult(i, UPDATE_SETTING);
                return true;
            }
            case R.id.action_help: {
                openHelpActivity();
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        loadPreference();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.

        if (mqttAndroidClient != null) {

            loadPreference();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if (hasFocus)
            hideSystemUI();
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mqttAndroidClient != null) {
            mqttAndroidClient.close();
        }
    }

    /**
     * Menu methods
     */
    private void openHelpActivity() {
        Intent helpIntent = new Intent(MainActivity.this, HelpActivity.class);
        startActivity(helpIntent);
    }


    /******========Private methods===========******/
    /**
     * hide SystemUI to make App full screen
     */

    private void hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            mDecorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                            | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                            | View.SYSTEM_UI_FLAG_IMMERSIVE
            );
        }
    }

    /**
     * showSystemUI
     */
    private void showSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            mDecorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }
    }


    /**
     * setup Control pad View
     */
    @SuppressLint("ClickableViewAccessibility")
    private void setupControlPadView() {

        mMoveUpButton = findViewById(R.id.upButton);
        mMoveDownButton = findViewById(R.id.downButton);
        mMoveLeftButton = findViewById(R.id.leftButton);
        mMoveRightButton = findViewById(R.id.rightButton);

        mControlFrontLightButton = findViewById(R.id.btn1s);

        mSpeedSeekBar = findViewById(R.id.speedSeekBar);

        /*
            State switches init
         */

        mModeSwitch = findViewById(R.id.robot_switch_mode);
        mStateSwitch = findViewById(R.id.robot_state);

        /*
            Set listeners on padControls
         */

        mMoveUpButton.setOnTouchListener(mControlsButtonOnTouchListener);
        mMoveDownButton.setOnTouchListener(mControlsButtonOnTouchListener);
        mMoveLeftButton.setOnTouchListener(mControlsButtonOnTouchListener);
        mMoveRightButton.setOnTouchListener(mControlsButtonOnTouchListener);


        mModeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                System.out.println("SWITCH ROBOT_ MODE");
                publishMessage(Constants.CAR_MODE_TOPIC, isModeAutoEnabled ? "OFF": "ON");
                isModeAutoEnabled = !isModeAutoEnabled;
             }
        });

        mStateSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                System.out.println("SWITCH ROBOT_STATE");
                publishMessage(Constants.CAR_STATE_TOPIC, isRobotEnabled ? "MANUAL": "AUTO");
                isRobotEnabled = !isRobotEnabled;

            }
        });

        mControlFrontLightButton.setOnTouchListener(mControlsButtonOnTouchListener);


        mSpeedSeekBar.setOnSeekBarChangeListener(mSpeedSeekBarListener);


    }


    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    // connectDevice(data, true);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled
                    mConnectScanMenuItem.setIcon(R.drawable.ic_bluetooth_blue_24dp);
                } else {
                    // User did not enable Bluetooth or an error occurred
                    Toast.makeText(MainActivity.this, R.string.bt_not_enabled_leaving,
                            Toast.LENGTH_SHORT).show();
                    MainActivity.this.finish();
                }
        }
    }


    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
        }

        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, "Press BACK again to exit", Toast.LENGTH_SHORT).show();

        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                doubleBackToExitPressedOnce = false;
            }
        }, 2000);
    }


    public void loadPreference() {
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        Map<String, ?> keys = mPrefs.getAll();
        Log.d("LOG", "Keys = " + keys.size() + "");
        if (keys.size() >= 11) {
            for (Map.Entry<String, ?> entry : keys.entrySet()) {
                Log.d("LOG", entry.getKey() + ": " +
                        entry.getValue().toString());
            }
        }
    }

}
