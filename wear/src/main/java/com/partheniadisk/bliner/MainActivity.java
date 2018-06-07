/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.partheniadisk.bliner;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.BoxInsetLayout;
import android.support.wearable.view.DismissOverlayView;
import android.support.wearable.view.WatchViewStub;
import android.support.wearable.view.WearableListView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;

import org.w3c.dom.Text;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import static com.partheniadisk.bliner.DataLayerListenerService.LOGD;

public class MainActivity extends WearableActivity implements
        WearableListView.ClickListener,
        SensorEventListener,
        ConnectionCallbacks,
        OnConnectionFailedListener,
        DataApi.DataListener,
        MessageApi.MessageListener,
        CapabilityApi.CapabilityListener {

    private WearableListView listView;

    private static final String TAG = "MainActivity";
    private GoogleApiClient mGoogleApiClient;
    private static final SimpleDateFormat AMBIENT_DATE_FORMAT =
            new SimpleDateFormat("HH:mm", Locale.US);
    private BoxInsetLayout mContainerView;

    /**
     * Overlay that shows a short help text when first launched. It also provides an option to
     * exit the app.
     */
    private DismissOverlayView mDismissOverlay;

    private Vibrator v;
    private int wearStage = 1;
    long[] patternScanning = {0, 100, 50, 100, 700}; //tat-tat
    long[] warningpattern = {0, 500, 100, 200, 500}; //taaat-tat
    long[] onetap = { 0, 100};
    long[] twotap = { 0, 100, 300, 100};
    long[] longtap = { 0, 500};
    public static ArrayList<String> listItems;
    static {
        listItems = new ArrayList<String>();
        listItems.add(0,"N");
        listItems.add(1,"");
        listItems.add(2,"Y");

    }
    //AWESOME real time UI updates!!
    private BroadcastReceiver _broadcastReceiver;
    static final long ONE_MINUTE_IN_MILLIS=60000;//millisecs
    Calendar date = Calendar.getInstance();
    private long t= date.getTimeInMillis();
    private MyAdapter mAdapter;
    private Typeface anwb_font;
    private TextView view;
    private TextView messageText;
    private TextView etaTimeAdded;
    private TextView detailsText;
    private TextView mClockView;
    private TextView etaText;
    private int delay = 30; //delays
    private int trip = 25; //trip without delays
    //function with total time to arrive
    private ImageView vehicleIconLow;
    private ImageView vehicleIconHigh;
    private TextView descriptionText;
    private ImageView circleNo;
    private ImageView circleYes;
    private ImageView checkIcon;
    private ImageView closeIcon;
    private ProgressBar progressBar;
    private TextView urgentMessage;
    private Date etaTimeExactly;
    CountDownTimer mCountDownTimer;
    private int counter=0;



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
        setContentView(R.layout.activity_main);
        anwb_font = Typeface.createFromAsset(getAssets(),  "fonts/anwb.ttf");

        mClockView = findViewById(R.id.clock);
        etaTimeAdded = findViewById(R.id.etaTimeAdded);
        mClockView.setText(AMBIENT_DATE_FORMAT.format(new Date()));
        etaTimeExactly = new Date(t + (durationOfDrive() * ONE_MINUTE_IN_MILLIS));
        etaTimeAdded.setText(AMBIENT_DATE_FORMAT.format(etaTimeExactly));
        messageText = findViewById(R.id.mainText);
        etaText = findViewById(R.id.etaText);
        vehicleIconLow = findViewById(R.id.vehicleIconLow);
        descriptionText = findViewById(R.id.descriptionText);
        mClockView.setTypeface(anwb_font);
        messageText.setTypeface(anwb_font);
        etaText.setTypeface(anwb_font);
        etaTimeAdded.setTypeface(anwb_font);
//        vehicleIconHigh = (ImageView) findViewById(R.id.vehicleIconHigh);
        circleNo = findViewById(R.id.circleNo);
        circleYes = findViewById(R.id.circleYes);
        closeIcon = findViewById(R.id.closeIcon);
        checkIcon = findViewById(R.id.checkIcon);
        progressBar = findViewById(R.id.progressBar);
        progressBar.setProgress(counter);
        progressBar.setMax(100);
        urgentMessage = findViewById(R.id.urgentMesage);

        final WatchViewStub stub = findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(final WatchViewStub stub) {
                listView = findViewById(R.id.listView1);
                ///*TODO: Uncomment when ready*/ listView.setEnabled(false);
                mAdapter=new MyAdapter(MainActivity.this);
                listView.setAdapter(mAdapter);
                checkStage();
                listView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
//                        Toast.makeText(getApplicationContext(),"Clicked list!", Toast.LENGTH_SHORT).show();
                    }
                });
                listView.addOnScrollListener(new WearableListView.OnScrollListener() {
                    @Override
                    public void onScroll(int scroll) {

                    }

                    @Override
                    public void onAbsoluteScrollChange(int scroll) {

                    }

                    @Override
                    public void onScrollStateChanged(int scrollState) {

                    }

                    @Override
                    public void onCentralPositionChanged(final int centralPosition) {
                        //DEFINE FLICK INTERACTIONS HERE PER STAGE
                        if(wearStage==0) {
                            //Choosing vehicles... do nothing
                            //if(centralPosition==1) stub.setBackground(getResources().getDrawable(R.drawable.commuterecognized));
                        }
                        if(wearStage==1){ //Commuting Driver mode recognized! (autodismissable)
                            if(centralPosition==0) {
                                wearStage=2;
                                //TODO: add auto    timer
                                checkStage();
                            }
                            if(centralPosition==2) finish(); //android.os.Process.killProcess(android.os.Process.myPid()); FOR FORCE STOP

                        }
                        if(wearStage==2){ //glancable
                            checkStage();

                        }if(wearStage==3){ //Commuting Driver mode recognized! (autodismissable)
                            checkStage();
                        }if(wearStage==4){ //Commuting Driver mode recognized! (autodismissable)
                            checkStage();
                        }if(wearStage==5){ //Commuting Driver mode recognized! (autodismissable)
                            checkStage();
                        }
                    }
                });
            }
        });
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
//        setAmbientEnabled(); // AMBIENT OFF FOR NOW AS IT MAY INTERFERE WITH CONNECTION

//
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

//
        // initialize touch listeners TODO: intitialize motion sensors to sense vehicle shits.
        initListeners();

//        mDismissOverlay = (DismissOverlayView) findViewById(R.id.dismiss_overlay);
//        mDismissOverlay.setIntroText(R.string.intro_text);
//        mDismissOverlay.showIntroIfNecessary();


    }

    private int durationOfDrive() {
        int duration = 0;
        duration = trip + delay;
        return duration;
    }

    @Override
    public void onStart() {
        super.onStart();
        _broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context ctx, Intent intent) {

                if (intent.getAction().compareTo(Intent.ACTION_TIME_TICK) == 0)
                    mClockView.setText(AMBIENT_DATE_FORMAT.format(new Date()));

                etaTimeExactly = new Date(t + (durationOfDrive() * ONE_MINUTE_IN_MILLIS));
                etaTimeAdded.setText(AMBIENT_DATE_FORMAT.format(etaTimeExactly));
            }
        };

        registerReceiver(_broadcastReceiver, new IntentFilter(Intent.ACTION_TIME_TICK));
    }

    @Override
    public void onStop() {
        super.onStop();
        if (_broadcastReceiver != null)
            unregisterReceiver(_broadcastReceiver);
    }


    public void initListeners() {

//        mContainerView.setOnLongClickListener(new View.OnLongClickListener() {
//            @Override
//            public boolean onLongClick(View view) {
//                // Display the dismiss overlay with a button to exit this activity.
//                mDismissOverlay.show();
//                v.vibrate(longtap,-1);
//
////                v.cancel();
//                return false;
//            }
//        });
//        mContainerView.setOnClickListener(new DoubleClickListener() {
//
//            @Override
//            public void onSingleClick(View view) {
////                Toast.makeText(getApplicationContext(),"Single tap!", Toast.LENGTH_SHORT).show();
//                v.vibrate(onetap, -1);
//
//            }
//
//            @Override
//            public void onDoubleClick(View veiw) {
////                Toast.makeText(getApplicationContext(),"Double tap!", Toast.LENGTH_SHORT).show();
//                v.vibrate(onetap, -1);
//
//
//            }
//        });
    }



    @Override
    public void onClick(WearableListView.ViewHolder viewHolder) {

    }

    @Override
    public void onTopEmptyRegionClick() {

    }

    private class MyAdapter extends WearableListView.Adapter  {

        private final LayoutInflater inflater;

        private MyAdapter(Context c) {
            inflater = LayoutInflater.from(c);
            listView.scrollToPosition(1);
        }

        @Override
        public WearableListView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            return new WearableListView.ViewHolder(inflater.inflate(R.layout.row_simple_item_layout, null));
        }

        @Override
        public void onBindViewHolder(WearableListView.ViewHolder viewHolder, int i) {
            view = viewHolder.itemView.findViewById(R.id.textView);
            view.setTypeface(anwb_font);
            view.setText(listItems.get(i).toString());
            viewHolder.itemView.setTag(i);
        }

        @Override
        public int getItemCount() {
            return listItems.size();
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onPause() {
        if ((mGoogleApiClient != null) && mGoogleApiClient.isConnected()) {
            Wearable.DataApi.removeListener(mGoogleApiClient, this);
            Wearable.MessageApi.removeListener(mGoogleApiClient, this);
            Wearable.CapabilityApi.removeListener(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }

        super.onPause();
        android.os.Process.killProcess(android.os.Process.myPid());
    }



    @Override
    public void onConnected(Bundle connectionHint) {
        LOGD(TAG, "onConnected(): Successfully connected to Google API client");
        Wearable.DataApi.addListener(mGoogleApiClient, this);
        Wearable.MessageApi.addListener(mGoogleApiClient, this);
        Wearable.CapabilityApi.addListener(
                mGoogleApiClient, this, Uri.parse("wear://"), CapabilityApi.FILTER_REACHABLE);
    }

    @Override
    public void onConnectionSuspended(int cause) {
        LOGD(TAG, "onConnectionSuspended(): Connection to Google API client was suspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.e(TAG, "onConnectionFailed(): Failed to connect, with result: " + result);
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {

//        TODO: Screens watch:
        //
        // STAGE 0: BEFORE just see the carousel images. nothing special.
        // STAGE 1: WHILE check touches and mtue them, receive data changes from phone.
        // STAGE 2: AFTER show map of vehicle and user real position. show time parked and cost.

//        Watch 1: STAGE 0: before commute, swipe between car and scooter (simple carousel with images. CAN NOT TAP)
//        Watch 2: STAGE 1: commuting driver mode recognised-with car (pop up, onRemoteClick by phone. Show current Time, Delays, ETA)
//        Watch 3: STAGE 3: notification speedlight (pop up onRemoteClick, auto dismiss after 10s, goes back to STAGE 1) (? flick up for details, flick down to dismiss quickly?)
//        Watch 4: STAGE 4: notification get alternative route, save 10min (pop up, onRemoteClick, auto dismiss after 20s, goes back to STAGE 1) (?flick up for yes, flick down for no(NO back to stage 1)Yes, open maps.)
//        Watch 5: no alternative route (user says no, flicks down or waits for the auto dismiss, returns back to STAGE 1)
//        Watch 6: STAGE 2: location saved screen car (if noMotionDetectedforTheLast3Seconds==true, user can LongClick to PARK and confirms with single tap. User can not LongClick while driving as long sensors detect accelaration/motion and gps movement together)
      //          (STAGE 2:) show lcation on map (show a picture_1 lol)
//        Watch 7: STAGE 1: commuting driver mode recognised bike (pop up, onRemoteClick by phone. Show current time, delays, ETA)
//        Watch 8: user takes a glance on bike, no delay, ETA
//        Watch 9: STAGE 2: location saved screen bike if noMotion...)
//        Watch 10: STAGE 2: show location car and current position (show a picture_2 lol)


        //Q: Swipe toe xit app or Hold and confirm the app.
        //Q: Onboarding and training with the wrist.
        //Q: While commuting.
        //Q FLICK ONLY while on route and when at destination (when car is still)
        //Q: If suer exists the app, it reopens when motion detected.


        //Carousel with eddys screens.
        //if click car start stage(1,0)
        //if click bike start stage (1,1)
        //(only if stage 1)if motion detected dont let LongClick (give audio feedback or visual)
        //(only if stage 1) if no motion let LongClick, appear blue"P" for parking. click and show a toast.
        //when dataChanged and stage

        //data changed from mobile and have been received in watch.
        LOGD(TAG, "onDataChanged(): " + dataEvents);
        for (DataEvent event : dataEvents) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                String path = event.getDataItem().getUri().getPath();
                if (DataLayerListenerService.STAGE_PATH.equals(path)) {

                    DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
                    int stage = dataMapItem.getDataMap()
                            .getInt(DataLayerListenerService.STAGE_KEY);
                    //stage is 1-7 (i can have 7 different interfaces depending on the stage)
                    wearStage=stage;
                    //itsFirstTime = true;
//                    tempStage = stage;
                    checkStage();

                }
            }

        }
    }

//
    /*
    * Screens watch:
Watch 1: before commute, swipe between car and scooter

Watch 2: commuting driver mode recognised car

Watch 3: notification speedlight
Watch 4: notification get alternative route
Watch 5: no alternative route
Watch 6: location saved screen car
Watch 7: commuting driver mode recognised bike
Watch 8: glance bike, no delay, ETA
Watch 9: location saved screen bike
Watch 10: show location car and current position
Watch 11: show location bike and current position
    * */
    private void checkStage() {

            if (wearStage == 0) {
                v.vibrate(onetap, -1);
                //Choosing Vehicles.....
            }
            else if(wearStage ==1){
                //Route recognized! Wrong?
                v.vibrate(onetap, -1);
                listItems.clear();
                listItems.add(0," ");
                listItems.add(1," ");
                listItems.add(2," ");
                mAdapter.notifyDataSetChanged();
                listView.scrollToPosition(1);
                //DO:
                messageText.setText("To Work");
                messageText.setTextColor(getResources().getColor(R.color.colorPrimary));
                vehicleIconLow.setImageDrawable(getDrawable(R.drawable.car));
                //UPDATE:
                //updateElementsVisibility(int clock,
                // int  message,
                // int  description,
                // int  urgent,
                // int vehicleLow,
                // int mEtaText,
                // int mEtaTimeAdded,
                // int noBubble,
                // int yesBubble,
                // int progressbar)
                updateElementsVisibility(View.INVISIBLE,
                        View.VISIBLE,
                        View.INVISIBLE,
                        View.INVISIBLE,
                        View.VISIBLE,
                        View.INVISIBLE,
                        View.INVISIBLE,
                        View.VISIBLE,
                        View.INVISIBLE,
                        View.VISIBLE);
                mCountDownTimer=new CountDownTimer(5000,1000) {

                    @Override
                    public void onTick(long millisUntilFinished) {
                        Log.v("Log_tag", "Tick of Progress"+ counter+ millisUntilFinished);
                        counter++;
                        v.vibrate(100);
                        progressBar.setProgress((5- counter)*100/(5000/1000));
                    }

                    @Override
                    public void onFinish() {
                        //Do what you want
                        wearStage=2;
                        counter++;
                        progressBar.setProgress(100);
                        v.cancel();
                        checkStage();
                        counter=0;
                    }
                };
                mCountDownTimer.start();
                //what if i am going to work but with bicycle and the sensors got it wrong? Does it bother me again? All combinations? Work, Car? Work, Bike? Grandma, Car? Grandma, Bike? Only confirms when sensors got it correct also validated by Google API.
            }
            else if (wearStage == 2) {
                //Glancable
                listItems.clear();
                listItems.add(0,"");
                mAdapter.notifyDataSetChanged();
                listView.scrollToPosition(0);
                //DO:
                messageText.setText("+" + String.valueOf(delay) +" min");
                int color = getResources().getColor(R.color.yesGreen);
                if(durationOfDrive()>40) {
                    color = getResources().getColor(R.color.brightRed);
                    v.vibrate(onetap, -1);
                }
                else  color = getResources().getColor(R.color.yesGreen);
                messageText.setTextColor(color);
                etaText.setTextColor(color);
                etaTimeAdded.setTextColor(color);
                //UPDATE:
                //updateElementsVisibility(int clock,
                // int  message,
                // int  description,
                // int  urgent,
                // int vehicleLow,
                // int mEtaText,
                // int mEtaTimeAdded,
                // int noBubble,
                // int yesBubble,
                // int progressbar)
                updateElementsVisibility(View.VISIBLE,
                        View.VISIBLE,
                        View.VISIBLE,
                        View.INVISIBLE,
                        View.INVISIBLE,
                        View.VISIBLE,
                        View.VISIBLE,
                        View.INVISIBLE,
                        View.INVISIBLE,
                        View.INVISIBLE);
            }
            else if (wearStage == 3){
                //notification speedlight , ADD SWAP LAYOUT soon
                v.vibrate(warningpattern, 3);
                listItems.clear();
                listItems.add("");
                mAdapter.notifyDataSetChanged();
                listView.scrollToPosition(0);
                //DO:
                messageText.setText("Speedlight!");
                descriptionText.setText("Amstel");
                urgentMessage.setText("80 km/h");

                //UPDATE:
                //updateElementsVisibility(int clock,
                // int  message,
                // int  description,
                // int urgent,
                // int vehicleLow,
                // int mEtaText,
                // int mEtaTimeAdded,
                // int noBubble,
                // int yesBubble,
                // int progressbar)
                updateElementsVisibility(View.VISIBLE,
                        View.VISIBLE,
                        View.VISIBLE,
                        View.VISIBLE,
                        View.INVISIBLE,
                        View.INVISIBLE,
                        View.INVISIBLE,
                        View.INVISIBLE,
                        View.INVISIBLE,
                        View.INVISIBLE);

                //TODO: add timer to autodismiss the speedlimit
            }
            else if (wearStage == 4){

                v.vibrate(warningpattern, 3);

            }
            else if (wearStage == 5){
                //ALT ROUTE FOUND!
                //TODO: do some other vibration?
                //TODO: play some sound alert?
                    v.vibrate(patternScanning, 3);
//                textView.setText("Alternative route found. Save 12minutes!");
                //TODO: start autodismiss for NO on bottom. Show YES also on top.
            }

    }

    private void updateElementsVisibility(int clock,int  message,int  description,int urgent, int vehicleLow, int mEtaText, int mEtaTimeAdded, int noBubble, int yesBubble, int progressbar) {

        mClockView.setVisibility(clock);
        messageText.setVisibility(message);
        descriptionText.setVisibility(description);
        urgentMessage.setVisibility(urgent);
        vehicleIconLow.setVisibility(vehicleLow);
        etaText.setVisibility(mEtaText);
        etaTimeAdded.setVisibility(mEtaTimeAdded);
        circleNo.setVisibility(noBubble);
        closeIcon.setVisibility(noBubble);
        circleYes.setVisibility(yesBubble);
        checkIcon.setVisibility(yesBubble);
        progressBar.setVisibility(progressbar);

    }


    /**
     * Find the connected nodes that provide at least one of the given capabilities
     */

    @Override
    public void onMessageReceived(MessageEvent event) {
        LOGD(TAG, "onMessageReceived: " + event);
//        mDataFragment.appendItem("Message", event.toString());
    }

    @Override
    public void onCapabilityChanged(CapabilityInfo capabilityInfo) {
        LOGD(TAG, "onCapabilityChanged: " + capabilityInfo);
//        mDataFragment.appendItem("onCapabilityChanged", capabilityInfo.toString());
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        //TODO: with this, demonstrate that people cant touch the interface as long as vehicle is moving.

    }

    /**
     * Switches to the page {@code index}. The first page has index 0.
     */
/*    private void moveToPage(int index) {
        mPager.setCurrentItem(0, index, true);
    }*/


    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

}