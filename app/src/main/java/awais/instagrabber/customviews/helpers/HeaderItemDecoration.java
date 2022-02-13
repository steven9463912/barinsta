package awais.instagrabber.customviews.helpers;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Java implementation of <a href="https://gist.github.com/filipkowicz/1a769001fae407b8813ab4387c42fcbd/3cda7542b12100b01da449e8648368b8f1369c70">this gist</a> by filipkowicz
 */
public class HeaderItemDecoration extends RecyclerView.ItemDecoration {
    private static final String TAG = HeaderItemDecoration.class.getSimpleName();

    private final HeaderItemDecorationCallback callback;

    private boolean layoutReversed;
    private Pair<Integer, RecyclerView.ViewHolder> currentHeader;

    public HeaderItemDecoration(@NonNull final RecyclerView parent,
                                @NonNull final HeaderItemDecorationCallback callback) {
        this.callback = callback;
        RecyclerView.LayoutManager layoutManager = parent.getLayoutManager();
        if (layoutManager instanceof LinearLayoutManager) {
            this.layoutReversed = ((LinearLayoutManager) layoutManager).getReverseLayout();
        }
        //noinspection rawtypes
        RecyclerView.Adapter adapter = parent.getAdapter();
        if (adapter == null) return;
        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                // clear saved header as it can be outdated now
                Log.d(HeaderItemDecoration.TAG, "registerAdapterDataObserver");
                HeaderItemDecoration.this.currentHeader = null;
            }
        });
        parent.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            // clear saved layout as it may need layout update
            Log.d(HeaderItemDecoration.TAG, "addOnLayoutChangeListener");
            this.currentHeader = null;
        });
        parent.addOnItemTouchListener(new RecyclerView.SimpleOnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
                if (e.getAction() == MotionEvent.ACTION_DOWN && HeaderItemDecoration.this.currentHeader != null) {
                    RecyclerView.ViewHolder viewHolder = HeaderItemDecoration.this.currentHeader.second;
                    if (viewHolder != null && viewHolder.itemView != null) {
                        int bottom = viewHolder.itemView.getBottom();
                        return e.getY() <= bottom;
                    }
                }
                return super.onInterceptTouchEvent(rv, e);
            }
        });
    }

    @Override
    public void onDrawOver(@NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        super.onDrawOver(c, parent, state);
        View topChild = parent.findChildViewUnder(
                parent.getPaddingLeft(),
                parent.getPaddingTop()
        );
        if (topChild == null) {
            return;
        }
        int topChildPosition = parent.getChildAdapterPosition(topChild);
        if (topChildPosition == RecyclerView.NO_POSITION) {
            return;
        }
        View headerView = this.getHeaderViewForItem(topChildPosition, parent);
        if (headerView == null) {
            return;
        }
        int contactPoint = headerView.getBottom() + parent.getPaddingTop();
        View childInContact = this.getChildInContact(parent, contactPoint);
        if (childInContact != null && this.callback.isHeader(parent.getChildAdapterPosition(childInContact))) {
            this.moveHeader(c, headerView, childInContact, parent.getPaddingTop());
            return;
        }
        this.drawHeader(c, headerView, parent.getPaddingTop());
    }

    private void drawHeader(@NonNull Canvas c, @NonNull View header, int paddingTop) {
        c.save();
        c.translate(0f, paddingTop);
        header.draw(c);
        c.restore();
    }

    private void moveHeader(@NonNull Canvas c, @NonNull View currentHeader, @NonNull View nextHeader, int paddingTop) {
        c.save();
        c.translate(0f, nextHeader.getTop() - currentHeader.getHeight() /*+ paddingTop*/);
        currentHeader.draw(c);
        c.restore();
    }

    @Nullable
    private View getChildInContact(@NonNull RecyclerView parent, int contactPoint) {
        View childInContact = null;
        int childCount = parent.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = parent.getChildAt(i);
            Rect mBounds = new Rect();
            parent.getDecoratedBoundsWithMargins(child, mBounds);
            if (mBounds.bottom > contactPoint) {
                if (mBounds.top <= contactPoint) {
                    // This child overlaps the contactPoint
                    childInContact = child;
                    break;
                }
            }
        }
        return childInContact;
    }

    @Nullable
    private View getHeaderViewForItem(int itemPosition, @NonNull RecyclerView parent) {
        if (parent.getAdapter() == null) {
            return null;
        }
        int headerPosition = this.getHeaderPositionForItem(itemPosition, parent.getAdapter());
        if (headerPosition == RecyclerView.NO_POSITION) return null;
        int headerType = parent.getAdapter().getItemViewType(headerPosition);
        // if match reuse viewHolder
        if (this.currentHeader != null
                && this.currentHeader.first == headerPosition
                && this.currentHeader.second.getItemViewType() == headerType) {
            return this.currentHeader.second.itemView;
        }
        RecyclerView.ViewHolder headerHolder = parent.getAdapter().createViewHolder(parent, headerType);
        if (headerHolder != null) {
            //noinspection unchecked
            parent.getAdapter().onBindViewHolder(headerHolder, headerPosition);
            this.fixLayoutSize(parent, headerHolder.itemView);
            // save for next draw
            this.currentHeader = new Pair<>(headerPosition, headerHolder);
            return headerHolder.itemView;
        }
        return null;
    }

    @SuppressWarnings("rawtypes")
    private int getHeaderPositionForItem(int itemPosition, RecyclerView.Adapter adapter) {
        int headerPosition = RecyclerView.NO_POSITION;
        int currentPosition = itemPosition;
        do {
            if (this.callback.isHeader(currentPosition)) {
                headerPosition = currentPosition;
                break;
            }
            currentPosition += this.layoutReversed ? 1 : -1;
        } while (this.layoutReversed ? currentPosition < adapter.getItemCount() : currentPosition >= 0);
        return headerPosition;
    }

    /**
     * Properly measures and layouts the top sticky header.
     *
     * @param parent ViewGroup: RecyclerView in this case.
     */
    private void fixLayoutSize(@NonNull ViewGroup parent, @NonNull View view) {

        // Specs for parent (RecyclerView)
        int widthSpec = View.MeasureSpec.makeMeasureSpec(parent.getWidth(), View.MeasureSpec.EXACTLY);
        int heightSpec = View.MeasureSpec.makeMeasureSpec(parent.getHeight(), View.MeasureSpec.UNSPECIFIED);

        // Specs for children (headers)
        int childWidthSpec = ViewGroup.getChildMeasureSpec(
                widthSpec,
                parent.getPaddingLeft() + parent.getPaddingRight(),
                view.getLayoutParams().width
        );
        int childHeightSpec = ViewGroup.getChildMeasureSpec(
                heightSpec,
                parent.getPaddingTop() + parent.getPaddingBottom(),
                view.getLayoutParams().height
        );

        view.measure(childWidthSpec, childHeightSpec);
        view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
    }

    public View getCurrentHeader() {
        return this.currentHeader == null ? null : this.currentHeader.second.itemView;
    }

    public interface HeaderItemDecorationCallback {
        boolean isHeader(int itemPosition);
    }
}
