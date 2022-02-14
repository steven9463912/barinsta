package awais.instagrabber.customviews.masoudss_waveform;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import awais.instagrabber.R;
import awais.instagrabber.utils.CubicInterpolation;
import awais.instagrabber.utils.Utils;

public final class WaveformSeekBar extends View {
    private final int mScaledTouchSlop = ViewConfiguration.get(this.getContext()).getScaledTouchSlop();
    private final Paint mWavePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF mWaveRect = new RectF();
    private final Canvas mProgressCanvas = new Canvas();
    private final WaveGravity waveGravity = WaveGravity.CENTER;
    private final int waveBackgroundColor;
    private final int waveProgressColor;
    private final float waveWidth = Utils.convertDpToPx(3);
    private final float waveMinHeight = Utils.convertDpToPx(4);
    private final float waveCornerRadius = Utils.convertDpToPx(2);
    private final float waveGap = Utils.convertDpToPx(1);
    // private int mCanvasWidth = 0;
    // private int mCanvasHeight = 0;
    private float mTouchDownX;
    private float[] sample;
    private int progress;
    private WaveFormProgressChangeListener progressChangeListener;
    private int wavesCount;
    private CubicInterpolation interpolation;

    public WaveformSeekBar(Context context) {
        this(context, null);
    }

    public WaveformSeekBar(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WaveformSeekBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.WaveformSeekBar,
                0,
                0);
        int backgroundColor;
        int progressColor;
        try {
            backgroundColor = a.getResourceId(R.styleable.WaveformSeekBar_waveformBackgroundColor, R.color.white);
            progressColor = a.getResourceId(R.styleable.WaveformSeekBar_waveformProgressColor, R.color.blue_800);
        } finally {
            a.recycle();
        }
        waveBackgroundColor = context.getResources().getColor(backgroundColor);
        waveProgressColor = context.getResources().getColor(progressColor);
    }

    private float getSampleMax() {
        float max = -1f;
        if (this.sample != null) {
            for (float v : this.sample) {
                if (v > max) max = v;
            }
        }
        return max;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (this.sample == null || this.sample.length == 0) return;
        int availableWidth = this.getAvailableWidth();
        int availableHeight = this.getAvailableHeight();

        // final float step = availableWidth / (waveGap + waveWidth) / sample.size();

        int i = 0;
        float lastWaveRight = (float) this.getPaddingLeft();

        float sampleMax = this.getSampleMax();
        while (i < this.wavesCount) {
            float t = lastWaveRight / availableWidth * this.sample.length;
            float waveHeight = availableHeight * (this.interpolation.interpolate(t) / sampleMax);

            if (waveHeight < this.waveMinHeight)
                waveHeight = this.waveMinHeight;

            float top;
            if (this.waveGravity == WaveGravity.TOP) {
                top = (float) this.getPaddingTop();
            } else if (this.waveGravity == WaveGravity.CENTER) {
                top = (float) this.getPaddingTop() + availableHeight / 2F - waveHeight / 2F;
            } else if (this.waveGravity == WaveGravity.BOTTOM) {
                top = this.getMeasuredHeight() - (float) this.getPaddingBottom() - waveHeight;
            } else {
                top = 0;
            }

            this.mWaveRect.set(lastWaveRight, top, lastWaveRight + this.waveWidth, top + waveHeight);

            if (this.mWaveRect.contains(availableWidth * this.progress / 100F, this.mWaveRect.centerY())) {
                int bitHeight = (int) this.mWaveRect.height();
                if (bitHeight <= 0) bitHeight = (int) this.waveWidth;

                Bitmap bitmap = Bitmap.createBitmap(availableWidth, bitHeight, Bitmap.Config.ARGB_8888);
                this.mProgressCanvas.setBitmap(bitmap);

                final float fillWidth = availableWidth * this.progress / 100F;

                this.mWavePaint.setColor(this.waveProgressColor);
                this.mProgressCanvas.drawRect(0F, 0F, fillWidth, this.mWaveRect.bottom, this.mWavePaint);

                this.mWavePaint.setColor(this.waveBackgroundColor);
                this.mProgressCanvas.drawRect(fillWidth, 0F, (float) availableWidth, this.mWaveRect.bottom, this.mWavePaint);

                this.mWavePaint.setShader(new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
            } else {
                this.mWavePaint.setColor(this.mWaveRect.right <= availableWidth * this.progress / 100F ? this.waveProgressColor : this.waveBackgroundColor);
                this.mWavePaint.setShader(null);
            }

            canvas.drawRoundRect(this.mWaveRect, this.waveCornerRadius, this.waveCornerRadius, this.mWavePaint);

            lastWaveRight = this.mWaveRect.right + this.waveGap;

            if (lastWaveRight + this.waveWidth > availableWidth + this.getPaddingLeft()) {
                break;
            }
            i++;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!this.isEnabled()) return false;

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (this.isParentScrolling()) this.mTouchDownX = event.getX();
                else this.updateProgress(event);
                break;

            case MotionEvent.ACTION_MOVE:
                this.updateProgress(event);
                break;

            case MotionEvent.ACTION_UP:
                if (Math.abs(event.getX() - this.mTouchDownX) > this.mScaledTouchSlop)
                    this.updateProgress(event);

                this.performClick();
                break;
        }

        return true;
    }

    @Override
    protected void onLayout(final boolean changed, final int left, final int top, final int right, final int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed) {
            this.calculateWaveDimensions();
        }
    }

    private void calculateWaveDimensions() {
        if (this.sample == null || this.sample.length == 0) return;
        int availableWidth = this.getAvailableWidth();
        this.wavesCount = (int) (availableWidth / (this.waveGap + this.waveWidth));
        this.interpolation = new CubicInterpolation(this.sample);
    }

    // @Override
    // protected void onSizeChanged(final int w, final int h, final int oldw, final int oldh) {
    //     super.onSizeChanged(w, h, oldw, oldh);
    //     mCanvasWidth = w;
    //     mCanvasHeight = h;
    // }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    private boolean isParentScrolling() {
        View parent = (View) this.getParent();
        View root = this.getRootView();

        while (true) {
            if (parent.canScrollHorizontally(1) || parent.canScrollHorizontally(-1) ||
                    parent.canScrollVertically(1) || parent.canScrollVertically(-1))
                return true;

            if (parent == root) return false;

            parent = (View) parent.getParent();
        }
    }

    private void updateProgress(@NonNull MotionEvent event) {
        this.progress = (int) (100 * event.getX() / this.getAvailableWidth());
        this.invalidate();

        if (this.progressChangeListener != null)
            this.progressChangeListener.onProgressChanged(this, Math.min(Math.max(0, this.progress), 100), true);
    }

    private int getAvailableWidth() {
        return this.getMeasuredWidth() - this.getPaddingLeft() - this.getPaddingRight();
    }

    private int getAvailableHeight() {
        return this.getMeasuredHeight() - this.getPaddingTop() - this.getPaddingBottom();
    }

    public void setProgress(int progress) {
        this.progress = progress;
        this.invalidate();
    }

    public void setProgressChangeListener(WaveFormProgressChangeListener progressChangeListener) {
        this.progressChangeListener = progressChangeListener;
    }

    public void setSample(float[] sample) {
        if (sample == this.sample) return;
        this.sample = sample;
        this.calculateWaveDimensions();
        this.invalidate();
    }
}