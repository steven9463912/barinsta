package awais.instagrabber.customviews.helpers;

import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class VerticalSpaceItemDecoration extends RecyclerView.ItemDecoration {
    private final int verticalSpaceHeight;

    public VerticalSpaceItemDecoration(final int verticalSpaceHeight) {
        this.verticalSpaceHeight = verticalSpaceHeight;
    }

    @Override
    public void getItemOffsets(final Rect outRect, @NonNull final View view, @NonNull final RecyclerView parent, @NonNull final RecyclerView.State state) {
        outRect.bottom = this.verticalSpaceHeight;
    }
}
