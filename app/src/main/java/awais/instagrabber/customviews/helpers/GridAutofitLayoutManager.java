package awais.instagrabber.customviews.helpers;

import android.content.Context;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import awais.instagrabber.utils.Utils;

public class GridAutofitLayoutManager extends GridLayoutManager {
    private int mColumnWidth;
    private boolean mColumnWidthChanged = true;

    public GridAutofitLayoutManager(final Context context, int columnWidth) {
        super(context, 1);
        if (columnWidth <= 0) columnWidth = (int) (48 * Utils.displayMetrics.density);
        if (columnWidth > 0 && columnWidth != this.mColumnWidth) {
            this.mColumnWidth = columnWidth;
            this.mColumnWidthChanged = true;
        }
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        int width = this.getWidth();
        int height = this.getHeight();
        if (this.mColumnWidthChanged && this.mColumnWidth > 0 && width > 0 && height > 0) {
            int totalSpace = this.getOrientation() == LinearLayoutManager.VERTICAL ? width - this.getPaddingRight() - this.getPaddingLeft()
                    : height - this.getPaddingTop() - this.getPaddingBottom();

            this.setSpanCount(Math.max(1, Math.min(totalSpace / this.mColumnWidth, 3)));

            this.mColumnWidthChanged = false;
        }
        super.onLayoutChildren(recycler, state);
    }
}
