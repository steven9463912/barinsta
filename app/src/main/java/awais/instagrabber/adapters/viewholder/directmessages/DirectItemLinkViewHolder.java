package awais.instagrabber.adapters.viewholder.directmessages;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.google.common.collect.ImmutableList;

import java.util.List;

import awais.instagrabber.R;
import awais.instagrabber.adapters.DirectItemsAdapter;
import awais.instagrabber.customviews.DirectItemContextMenu;
import awais.instagrabber.databinding.LayoutDmBaseBinding;
import awais.instagrabber.databinding.LayoutDmLinkBinding;
import awais.instagrabber.repositories.responses.User;
import awais.instagrabber.repositories.responses.directmessages.DirectItem;
import awais.instagrabber.repositories.responses.directmessages.DirectItemLink;
import awais.instagrabber.repositories.responses.directmessages.DirectItemLinkContext;
import awais.instagrabber.repositories.responses.directmessages.DirectThread;
import awais.instagrabber.utils.TextUtils;
import awais.instagrabber.utils.Utils;

public class DirectItemLinkViewHolder extends DirectItemViewHolder {

    private final LayoutDmLinkBinding binding;

    public DirectItemLinkViewHolder(@NonNull LayoutDmBaseBinding baseBinding,
                                    LayoutDmLinkBinding binding,
                                    User currentUser,
                                    DirectThread thread,
                                    DirectItemsAdapter.DirectItemCallback callback) {
        super(baseBinding, currentUser, thread, callback);
        this.binding = binding;
        int width = this.windowWidth - this.margin - this.dmRadiusSmall;
        ViewGroup.LayoutParams layoutParams = binding.preview.getLayoutParams();
        layoutParams.width = width;
        binding.preview.requestLayout();
        this.setItemView(binding.getRoot());
    }

    @Override
    public void bindItem(DirectItem item, MessageDirection messageDirection) {
        DirectItemLink link = item.getLink();
        if (link == null) return;
        DirectItemLinkContext linkContext = link.getLinkContext();
        if (linkContext == null) return;
        String linkImageUrl = linkContext.getLinkImageUrl();
        if (TextUtils.isEmpty(linkImageUrl)) {
            this.binding.preview.setVisibility(View.GONE);
        } else {
            this.binding.preview.setVisibility(View.VISIBLE);
            this.binding.preview.setImageURI(linkImageUrl);
        }
        if (TextUtils.isEmpty(linkContext.getLinkTitle())) {
            this.binding.title.setVisibility(View.GONE);
        } else {
            this.binding.title.setVisibility(View.VISIBLE);
            this.binding.title.setText(linkContext.getLinkTitle());
        }
        if (TextUtils.isEmpty(linkContext.getLinkSummary())) {
            this.binding.summary.setVisibility(View.GONE);
        } else {
            this.binding.summary.setVisibility(View.VISIBLE);
            this.binding.summary.setText(linkContext.getLinkSummary());
        }
        if (TextUtils.isEmpty(linkContext.getLinkUrl())) {
            this.binding.url.setVisibility(View.GONE);
        } else {
            this.binding.url.setVisibility(View.VISIBLE);
            this.binding.url.setText(linkContext.getLinkUrl());
        }
        this.binding.text.setText(link.getText());
        this.setupListeners(linkContext);
    }

    private void setupListeners(DirectItemLinkContext linkContext) {
        this.setupRamboTextListeners(this.binding.text);
        View.OnClickListener onClickListener = v -> this.openURL(linkContext.getLinkUrl());
        this.binding.preview.setOnClickListener(onClickListener);
        // binding.preview.setOnLongClickListener(v -> itemView.performLongClick());
        this.binding.title.setOnClickListener(onClickListener);
        this.binding.summary.setOnClickListener(onClickListener);
        this.binding.url.setOnClickListener(onClickListener);
    }

    @Override
    protected boolean showBackground() {
        return true;
    }

    @Override
    protected List<DirectItemContextMenu.MenuItem> getLongClickOptions() {
        return ImmutableList.of(
                new DirectItemContextMenu.MenuItem(R.id.copy, R.string.copy, item -> {
                    DirectItemLink link = item.getLink();
                    if (link == null || TextUtils.isEmpty(link.getText())) return null;
                    Utils.copyText(this.itemView.getContext(), link.getText());
                    return null;
                })
        );
    }
}
