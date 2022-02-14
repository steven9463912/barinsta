package awais.instagrabber.adapters.viewholder;

import android.annotation.SuppressLint;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.color.MaterialColors;

import awais.instagrabber.R;
import awais.instagrabber.adapters.TabsAdapter;
import awais.instagrabber.databinding.ItemTabOrderPrefBinding;
import awais.instagrabber.models.Tab;

public class TabViewHolder extends RecyclerView.ViewHolder {
    private final ItemTabOrderPrefBinding binding;
    private final TabsAdapter.TabAdapterCallback tabAdapterCallback;
    private final int highlightColor;
    private final Drawable originalBgColor;

    private boolean draggable = true;

    @SuppressLint("ClickableViewAccessibility")
    public TabViewHolder(@NonNull ItemTabOrderPrefBinding binding,
                         @NonNull TabsAdapter.TabAdapterCallback tabAdapterCallback) {
        super(binding.getRoot());
        this.binding = binding;
        this.tabAdapterCallback = tabAdapterCallback;
        this.highlightColor = MaterialColors.getColor(this.itemView.getContext(), R.attr.colorControlHighlight, 0);
        this.originalBgColor = this.itemView.getBackground();
        binding.handle.setOnTouchListener((v, event) -> {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                tabAdapterCallback.onStartDrag(this);
            }
            return true;
        });
    }

    public void bind(@NonNull Tab tab,
                     boolean isInOthers,
                     boolean isCurrentFull) {
        this.draggable = !isInOthers;
        this.binding.icon.setImageResource(tab.getIconResId());
        this.binding.title.setText(tab.getTitle());
        this.binding.handle.setVisibility(isInOthers ? View.GONE : View.VISIBLE);
        this.binding.addRemove.setImageResource(isInOthers ? R.drawable.ic_round_add_circle_24
                                                      : R.drawable.ic_round_remove_circle_24);
        ColorStateList tintList = ColorStateList.valueOf(ContextCompat.getColor(
                this.itemView.getContext(),
                isInOthers ? R.color.green_500
                           : R.color.red_500));
        ImageViewCompat.setImageTintList(this.binding.addRemove, tintList);
        this.binding.addRemove.setOnClickListener(v -> {
            if (isInOthers) {
                this.tabAdapterCallback.onAdd(tab);
                return;
            }
            this.tabAdapterCallback.onRemove(tab);
        });
        boolean enabled = tab.isRemovable()
                && !(isInOthers && isCurrentFull); // All slots are full in current
        this.binding.addRemove.setEnabled(enabled);
        this.binding.addRemove.setAlpha(enabled ? 1 : 0.5F);
    }

    public boolean isDraggable() {
        return this.draggable;
    }

    public void setDragging(boolean isDragging) {
        if (isDragging) {
            if (this.highlightColor != 0) {
                this.itemView.setBackgroundColor(this.highlightColor);
            } else {
                this.itemView.setAlpha(0.5F);
            }
            return;
        }
        this.itemView.setAlpha(1);
        this.itemView.setBackground(this.originalBgColor);
    }
}
