package awais.instagrabber.customviews;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.style.ImageSpan;

import androidx.annotation.NonNull;

public class VerticalImageSpan extends ImageSpan {

    public VerticalImageSpan(Drawable drawable) {
        super(drawable);
    }

    /**
     * update the text line height
     */
    @Override
    public int getSize(@NonNull final Paint paint,
                       final CharSequence text,
                       final int start,
                       final int end,
                       final Paint.FontMetricsInt fontMetricsInt) {
        final Drawable drawable = this.getDrawable();
        final Rect rect = drawable.getBounds();
        if (fontMetricsInt != null) {
            final Paint.FontMetricsInt fmPaint = paint.getFontMetricsInt();
            final int fontHeight = fmPaint.descent - fmPaint.ascent;
            final int drHeight = rect.bottom - rect.top;
            final int centerY = fmPaint.ascent + fontHeight / 2;

            fontMetricsInt.ascent = centerY - drHeight / 2;
            fontMetricsInt.top = fontMetricsInt.ascent;
            fontMetricsInt.bottom = centerY + drHeight / 2;
            fontMetricsInt.descent = fontMetricsInt.bottom;
        }
        return rect.right;
    }

    /**
     * see detail message in android.text.TextLine
     *
     * @param canvas the canvas, can be null if not rendering
     * @param text   the text to be draw
     * @param start  the text start position
     * @param end    the text end position
     * @param x      the edge of the replacement closest to the leading margin
     * @param top    the top of the line
     * @param y      the baseline
     * @param bottom the bottom of the line
     * @param paint  the work paint
     */
    @Override
    public void draw(final Canvas canvas,
                     final CharSequence text,
                     final int start,
                     final int end,
                     final float x,
                     final int top,
                     final int y,
                     final int bottom,
                     final Paint paint) {
        final Drawable drawable = this.getDrawable();
        canvas.save();
        final Paint.FontMetricsInt fmPaint = paint.getFontMetricsInt();
        final int fontHeight = fmPaint.descent - fmPaint.ascent;
        final int centerY = y + fmPaint.descent - fontHeight / 2;
        final int transY = centerY - (drawable.getBounds().bottom - drawable.getBounds().top) / 2;
        canvas.translate(x, transY);
        drawable.draw(canvas);
        canvas.restore();
    }

}
