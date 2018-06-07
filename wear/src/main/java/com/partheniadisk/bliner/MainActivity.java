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
    private int wearStage = 0;
    long[] patternScanning = {0, 100, 50, 100, 700}; //tat-tat
    long[] warningpattern = {0, 500, 100, 200, 500}; //taaat-tat
    long[] onetap = { 0, 100};
    long[] twotap = { 0, 100, 300, 100};
    long[] longtap = { 0, 500};
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
    private int delay = 10;
    private ImageView vehicleIconLow;
    private ImageView vehicleIconHigh;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
        setContentView(R.layout.activity_main);
        anwb_font = Typeface.createFromAsset(getAssets(),  "fonts/anwb.ttf");

        mClockView = findViewById(R.id.clock);
        etaTimeAdded = findViewById(R.id.etaTimeAdded);
        mClockView.setText(AMBIENT_DATE_FORMAT.format(new Date()));
        etaTimeAdded.setText(AMBIENT_DATE_FORMAT.format(new Date(t + (delay * ONE_MINUTE_IN_MILLIS))));
        messageText = findViewById(R.id.mainText);
        etaText = findViewById(R.id.etaText);
        vehicleIconLow = findViewById(R.id.vehicleIconLow);
        mClockView.setTypeface(anwb_font);
        messageText.setTypeface(anwb_font);
        etaText.setTypeface(anwb_font);
        etaTimeAdded.setTypeface(anwb_font);
//        vehicleIconHigh = (ImageView) findViewById(R.id.vehicleIconHigh);


        final WatchViewStub stub = findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(final WatchViewStub stub) {
                listView = findViewById(R.id.listView1);
                ///*TODO: Uncomment when ready*/ listView.setEnabled(false);
                mAdapter=new MyAdapter(MainActivity.this);
                listView.setAdapter(mAdapter);
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
                        if(wearStage==0) { //Choose vehicles

                        }
                        else if(wearStage==1){ //Commuting Driver mode recognized! (autodismissable)
//                            if(centralPosition==1) stub.setBackground(getResources().getDrawable(R.drawable.commuterecognized));
//                            else if (centralPosition==0) stub.setBackground(getResources().getDrawable(R.drawable.autodimissable));
//                            else if (centralPosition==2) stub.setBackground(getResources().getDrawable(R.drawable.flickable));
                        }
                        else if(wearStage==2){

                        }


//
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
    @Override
    public void onStart() {
        super.onStart();
        _broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context ctx, Intent intent) {

                if (intent.getAction().compareTo(Intent.ACTION_TIME_TICK) == 0)
                    mClockView.setText(AMBIENT_DATE_FORMAT.format(new Date()));
                    //TODO:
                etaTimeAdded.setText(AMBIENT_DATE_FORMAT.format(new Date(t + (10 * ONE_MINUTE_IN_MILLIS))));
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

    public static ArrayList<String> listItems;
    static {
        listItems = new ArrayList<String>();
        listItems.add("No, close app.");
        listItems.add("To Work\nby car?");
        listItems.add("Yes, continue");

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


    private void checkStage() {

            if (wearStage == 0) {
                    v.vibrate(onetap, -1);
                    listItems.clear();
                    listItems.add(0,"No, close app.");
                    listItems.add(1,"To Work\nby car?");
                    listItems.add(2,"Yes, continue");
                    mAdapter.notifyDataSetChanged();
                    listView.scrollToPosition(1);
        }
            if (wearStage == 1) { //Yes, check delays ETA
                    v.vibrate(onetap, -1);
                listItems.clear();
                listItems.add(0,"08:10");
                listItems.add(1,"0 min");
                listItems.add(2,"ETA\n" + "8:43");
                mAdapter.notifyDataSetChanged();
                listView.scrollToPosition(1);
                view.setTextColor(getResources().getColor(R.color.yesGreen)); //TODO: remove if i add swap layout
//                textView.setText("delays: 2 minutes, ETA: 9:30, 20 minutes to arrive to work in your car.");
            }
            if (wearStage == 2){ //notification speedlight , ADD SWAP LAYOUT soon
                v.vibrate(warningpattern, 3);
                listItems.clear();
                listItems.add("Speedlight\n");
                mAdapter.notifyDataSetChanged();
                listView.scrollToPosition(0);
                //TODO: add timer to autodismiss the speedlimit
            }
            if (wearStage == 3){

                v.vibrate(warningpattern, 3);

            }
            if (wearStage == 4){
                //ALT ROUTE FOUND!
                //TODO: do some other vibration?
                //TODO: play some sound alert?
                    v.vibrate(patternScanning, 3);
//                textView.setText("Alternative route found. Save 12minutes!");
                //TODO: start autodismiss for NO on bottom. Show YES also on top.
            }

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