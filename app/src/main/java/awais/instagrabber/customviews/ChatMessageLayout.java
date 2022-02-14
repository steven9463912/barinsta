package awais.instagrabber.customviews;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import awais.instagrabber.R;

public class ChatMessageLayout extends FrameLayout {

    private FrameLayout viewPartMain;
    private View viewPartInfo;
    private TypedArray a;

    private int viewPartInfoWidth;
    private int viewPartInfoHeight;

    // private boolean withGroupHeader = false;

    public ChatMessageLayout(@NonNull Context context) {
        super(context);
    }

    public ChatMessageLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.a = context.obtainStyledAttributes(attrs, R.styleable.ChatMessageLayout, 0, 0);
    }

    public ChatMessageLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.a = context.obtainStyledAttributes(attrs, R.styleable.ChatMessageLayout, defStyleAttr, 0);
    }

    public ChatMessageLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.a = context.obtainStyledAttributes(attrs, R.styleable.ChatMessageLayout, defStyleAttr, defStyleRes);
    }

    // public void setWithGroupHeader(boolean withGroupHeader) {
    //     this.withGroupHeader = withGroupHeader;
    // }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        try {
            this.viewPartMain = this.findViewById(this.a.getResourceId(R.styleable.ChatMessageLayout_viewPartMain, -1));
            this.viewPartInfo = this.findViewById(this.a.getResourceId(R.styleable.ChatMessageLayout_viewPartInfo, -1));
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize;
        // heightSize = MeasureSpec.getSize(heightMeasureSpec);

        if (this.viewPartMain == null || this.viewPartInfo == null || widthSize <= 0) {
            return;
        }

        View firstChild = this.viewPartMain.getChildAt(0);
        if (firstChild == null) return;

        int firstChildId = firstChild.getId();

        final int availableWidth = widthSize - this.getPaddingLeft() - this.getPaddingRight();
        // int availableHeight = heightSize - getPaddingTop() - getPaddingBottom();

        LayoutParams viewPartMainLayoutParams = (LayoutParams) this.viewPartMain.getLayoutParams();
        int viewPartMainWidth = this.viewPartMain.getMeasuredWidth() + viewPartMainLayoutParams.leftMargin + viewPartMainLayoutParams.rightMargin;
        int viewPartMainHeight = this.viewPartMain.getMeasuredHeight() + viewPartMainLayoutParams.topMargin + viewPartMainLayoutParams.bottomMargin;

        LayoutParams viewPartInfoLayoutParams = (LayoutParams) this.viewPartInfo.getLayoutParams();
        this.viewPartInfoWidth = this.viewPartInfo.getMeasuredWidth() + viewPartInfoLayoutParams.leftMargin + viewPartInfoLayoutParams.rightMargin;
        this.viewPartInfoHeight = this.viewPartInfo.getMeasuredHeight() + viewPartInfoLayoutParams.topMargin + viewPartInfoLayoutParams.bottomMargin;

        widthSize = this.getPaddingLeft() + this.getPaddingRight();
        heightSize = this.getPaddingTop() + this.getPaddingBottom();
        if (firstChildId == R.id.media_container) {
            widthSize += viewPartMainWidth;
            heightSize += viewPartMainHeight;
        } else if (firstChildId == R.id.raven_media_container || firstChildId == R.id.profile_container || firstChildId == R.id.voice_media
                || firstChildId == R.id.story_container || firstChildId == R.id.media_share_container || firstChildId == R.id.link_container
                || firstChildId == R.id.ivAnimatedMessage || firstChildId == R.id.reel_share_container) {
            widthSize += viewPartMainWidth;
            heightSize += viewPartMainHeight + this.viewPartInfoHeight;
        } else {
            int viewPartMainLineCount = 1;
            float viewPartMainLastLineWidth = 0;
            TextView textMessage;
            if (firstChild instanceof TextView) {
                textMessage = (TextView) firstChild;
            }
            else textMessage = null;
            if (textMessage != null) {
                viewPartMainLineCount = textMessage.getLineCount();
                viewPartMainLastLineWidth = viewPartMainLineCount > 0
                                            ? textMessage.getLayout().getLineWidth(viewPartMainLineCount - 1)
                                            : 0;
                // also include start left padding
                viewPartMainLastLineWidth += textMessage.getPaddingLeft();
            }

            float lastLineWithInfoWidth = viewPartMainLastLineWidth + this.viewPartInfoWidth;
            if (viewPartMainLineCount > 1 && lastLineWithInfoWidth <= this.viewPartMain.getMeasuredWidth()) {
                widthSize += viewPartMainWidth;
                heightSize += viewPartMainHeight;
            } else if (viewPartMainLineCount > 1 && (lastLineWithInfoWidth > availableWidth)) {
                widthSize += viewPartMainWidth;
                heightSize += viewPartMainHeight + this.viewPartInfoHeight;
            } else if (viewPartMainLineCount == 1 && (viewPartMainWidth + this.viewPartInfoWidth > availableWidth)) {
                widthSize += this.viewPartMain.getMeasuredWidth();
                heightSize += viewPartMainHeight + this.viewPartInfoHeight;
            } else {
                heightSize += viewPartMainHeight;
                widthSize += viewPartMainWidth + this.viewPartInfoWidth;
            }

            // if (isInEditMode()) {
            //     TextView wDebugView = (TextView) ((ViewGroup) this.getParent()).findViewWithTag("debug");
            //     wDebugView.setText(lastLineWithInfoWidth
            //                                + "\n" + availableWidth
            //                                + "\n" + viewPartMain.getMeasuredWidth()
            //                                + "\n" + (lastLineWithInfoWidth <= viewPartMain.getMeasuredWidth())
            //                                + "\n" + (lastLineWithInfoWidth > availableWidth)
            //                                + "\n" + (viewPartMainWidth + viewPartInfoWidth > availableWidth));
            // }
        }
        this.setMeasuredDimension(widthSize, heightSize);
        super.onMeasure(MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(heightSize, MeasureSpec.EXACTLY));

    }

    @Override
    protected void onLayout(final boolean changed, final int left, final int top, final int right, final int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        if (this.viewPartMain == null || this.viewPartInfo == null) {
            return;
        }
        // if (withGroupHeader) {
        //     viewPartMain.layout(
        //             getPaddingLeft(),
        //             getPaddingTop() - Utils.convertDpToPx(4),
        //             viewPartMain.getWidth() + getPaddingLeft(),
        //             viewPartMain.getHeight() + getPaddingTop());
        //
        // } else {
        this.viewPartMain.layout(
                this.getPaddingLeft(),
                this.getPaddingTop(),
                this.viewPartMain.getWidth() + this.getPaddingLeft(),
                this.viewPartMain.getHeight() + this.getPaddingTop());

        // }
        this.viewPartInfo.layout(
                right - left - this.viewPartInfoWidth - this.getPaddingRight(),
                bottom - top - this.getPaddingBottom() - this.viewPartInfoHeight,
                right - left - this.getPaddingRight(),
                bottom - top - this.getPaddingBottom());
    }
}
