package awais.instagrabber.animations;

import android.graphics.PointF;
import android.view.animation.Interpolator;

public class CubicBezierInterpolator implements Interpolator {

    public static final CubicBezierInterpolator DEFAULT = new CubicBezierInterpolator(0.25, 0.1, 0.25, 1);
    public static final CubicBezierInterpolator EASE_OUT = new CubicBezierInterpolator(0, 0, .58, 1);
    public static final CubicBezierInterpolator EASE_OUT_QUINT = new CubicBezierInterpolator(.23, 1, .32, 1);
    public static final CubicBezierInterpolator EASE_IN = new CubicBezierInterpolator(.42, 0, 1, 1);
    public static final CubicBezierInterpolator EASE_BOTH = new CubicBezierInterpolator(.42, 0, .58, 1);

    protected PointF start;
    protected PointF end;
    protected PointF a = new PointF();
    protected PointF b = new PointF();
    protected PointF c = new PointF();

    public CubicBezierInterpolator(final PointF start, final PointF end) throws IllegalArgumentException {
        if (start.x < 0 || start.x > 1) {
            throw new IllegalArgumentException("startX value must be in the range [0, 1]");
        }
        if (end.x < 0 || end.x > 1) {
            throw new IllegalArgumentException("endX value must be in the range [0, 1]");
        }
        this.start = start;
        this.end = end;
    }

    public CubicBezierInterpolator(final float startX, final float startY, final float endX, final float endY) {
        this(new PointF(startX, startY), new PointF(endX, endY));
    }

    public CubicBezierInterpolator(final double startX, final double startY, final double endX, final double endY) {
        this((float) startX, (float) startY, (float) endX, (float) endY);
    }

    @Override
    public float getInterpolation(final float time) {
        return this.getBezierCoordinateY(this.getXForTime(time));
    }

    protected float getBezierCoordinateY(final float time) {
        this.c.y = 3 * this.start.y;
        this.b.y = 3 * (this.end.y - this.start.y) - this.c.y;
        this.a.y = 1 - this.c.y - this.b.y;
        return time * (this.c.y + time * (this.b.y + time * this.a.y));
    }

    protected float getXForTime(final float time) {
        float x = time;
        float z;
        for (int i = 1; i < 14; i++) {
            z = this.getBezierCoordinateX(x) - time;
            if (Math.abs(z) < 1e-3) {
                break;
            }
            x -= z / this.getXDerivate(x);
        }
        return x;
    }

    private float getXDerivate(final float t) {
        return this.c.x + t * (2 * this.b.x + 3 * this.a.x * t);
    }

    private float getBezierCoordinateX(final float time) {
        this.c.x = 3 * this.start.x;
        this.b.x = 3 * (this.end.x - this.start.x) - this.c.x;
        this.a.x = 1 - this.c.x - this.b.x;
        return time * (this.c.x + time * (this.b.x + time * this.a.x));
    }
}
