package com.partheniadisk.bliner;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.support.wearable.view.WearableListView;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.LinearLayout;
import android.widget.TextView;




public class WearableListItemLayout extends LinearLayout
        implements WearableListView.OnCenterProximityListener {

    private TextView mName;


    private final float mFadedTextAlpha;
    private Typeface anwb_font = Typeface.createFromAsset(this.getContext().getApplicationContext().getAssets() ,  "fonts/anwb.ttf");

    public WearableListItemLayout(Context context) {
        this(context, null);
    }

    public WearableListItemLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WearableListItemLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        mFadedTextAlpha = getResources().getInteger(R.integer.action_text_faded_alpha) / 100f;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mName = (TextView) findViewById(R.id.name);

    }

    @Override
    public void onCenterPosition(boolean animate) {
        mName.setTypeface(anwb_font);
        mName.setAlpha(1f);
        mName.setTextColor(Color.BLACK);
        mName.setBackgroundColor(Color.TRANSPARENT);
        mName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 38);


    }

    @Override
    public void onNonCenterPosition(boolean animate) {
        mName.setTypeface(anwb_font);
        mName.setAlpha(mFadedTextAlpha);
        mName.setTextColor(Color.BLACK);
        mName.setBackgroundColor(Color.TRANSPARENT);
        mName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 7);
    }


}