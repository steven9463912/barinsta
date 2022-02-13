package awais.instagrabber.customviews;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.google.android.material.button.MaterialButton;

import awais.instagrabber.animations.ScaleAnimation;

/**
 * Created by Devlomi on 13/12/2017.
 */

public class RecordButton extends MaterialButton implements View.OnTouchListener, View.OnClickListener, View.OnLongClickListener {

    private ScaleAnimation scaleAnimation;
    private RecordView recordView;
    private boolean listenForRecord = true;
    private OnRecordClickListener onRecordClickListener;
    private OnRecordLongClickListener onRecordLongClickListener;

    public RecordButton(final Context context) {
        super(context);
        this.init(context, null);
    }

    public RecordButton(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        this.init(context, attrs);
    }

    public RecordButton(final Context context, final AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.init(context, attrs);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void init(final Context context, final AttributeSet attrs) {
        this.scaleAnimation = new ScaleAnimation(this);
        setOnTouchListener(this);
        setOnClickListener(this);
        setOnLongClickListener(this);
    }

    public void setRecordView(final RecordView recordView) {
        this.recordView = recordView;
    }

    @Override
    public boolean onTouch(final View v, final MotionEvent event) {
        if (this.isListenForRecord()) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    this.recordView.onActionDown((RecordButton) v, event);
                    break;
                case MotionEvent.ACTION_MOVE:
                    this.recordView.onActionMove((RecordButton) v, event, false);
                    break;
                case MotionEvent.ACTION_UP:
                    this.recordView.onActionUp((RecordButton) v);
                    break;
            }
        }
        return this.isListenForRecord();
    }

    protected void startScale() {
        this.scaleAnimation.start();
    }

    public void stopScale() {
        this.scaleAnimation.stop();
    }

    public void setListenForRecord(final boolean listenForRecord) {
        this.listenForRecord = listenForRecord;
    }

    public boolean isListenForRecord() {
        return this.listenForRecord;
    }

    public void setOnRecordClickListener(final OnRecordClickListener onRecordClickListener) {
        this.onRecordClickListener = onRecordClickListener;
    }

    public void setOnRecordLongClickListener(final OnRecordLongClickListener onRecordLongClickListener) {
        this.onRecordLongClickListener = onRecordLongClickListener;
    }

    @Override
    public void onClick(final View v) {
        if (this.onRecordClickListener != null) {
            this.onRecordClickListener.onClick(v);
        }
    }

    @Override
    public boolean onLongClick(View v) {
        if (this.onRecordLongClickListener != null) {
            return this.onRecordLongClickListener.onLongClick(v);
        }
        return false;
    }

    public interface OnRecordClickListener {
        void onClick(View v);
    }

    public interface OnRecordLongClickListener {
        boolean onLongClick(View v);
    }
}
