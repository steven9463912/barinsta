package awais.instagrabber.customviews.helpers;

import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class GridSpacingItemDecoration extends RecyclerView.ItemDecoration {
    private final int halfSpace;

    private boolean hasHeader;

    public GridSpacingItemDecoration(final int spacing) {
        this.halfSpace = spacing / 2;
    }

    @Override
    public void getItemOffsets(@NonNull final Rect outRect, @NonNull final View view, @NonNull final RecyclerView parent, @NonNull final RecyclerView.State state) {
        if (this.hasHeader && parent.getChildAdapterPosition(view) == 0) {
            outRect.bottom = this.halfSpace;
            outRect.left = -this.halfSpace;
            outRect.right = -this.halfSpace;
            return;
        }
        if (parent.getPaddingLeft() != this.halfSpace) {
            parent.setPadding(this.halfSpace, this.hasHeader ? 0 : this.halfSpace, this.halfSpace, this.halfSpace);
            parent.setClipToPadding(false);
        }
        outRect.top = this.halfSpace;
        outRect.bottom = this.halfSpace;
        outRect.left = this.halfSpace;
        outRect.right = this.halfSpace;
    }

    public void setHasHeader(boolean hasHeader) {
        this.hasHeader = hasHeader;
    }
}