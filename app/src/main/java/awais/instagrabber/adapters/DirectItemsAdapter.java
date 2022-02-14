package awais.instagrabber.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.AdapterListUpdateCallback;
import androidx.recyclerview.widget.AsyncDifferConfig;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import awais.instagrabber.adapters.viewholder.directmessages.DirectItemActionLogViewHolder;
import awais.instagrabber.adapters.viewholder.directmessages.DirectItemAnimatedMediaViewHolder;
import awais.instagrabber.adapters.viewholder.directmessages.DirectItemDefaultViewHolder;
import awais.instagrabber.adapters.viewholder.directmessages.DirectItemLikeViewHolder;
import awais.instagrabber.adapters.viewholder.directmessages.DirectItemLinkViewHolder;
import awais.instagrabber.adapters.viewholder.directmessages.DirectItemMediaShareViewHolder;
import awais.instagrabber.adapters.viewholder.directmessages.DirectItemMediaViewHolder;
import awais.instagrabber.adapters.viewholder.directmessages.DirectItemPlaceholderViewHolder;
import awais.instagrabber.adapters.viewholder.directmessages.DirectItemProfileViewHolder;
import awais.instagrabber.adapters.viewholder.directmessages.DirectItemRavenMediaViewHolder;
import awais.instagrabber.adapters.viewholder.directmessages.DirectItemReelShareViewHolder;
import awais.instagrabber.adapters.viewholder.directmessages.DirectItemStoryShareViewHolder;
import awais.instagrabber.adapters.viewholder.directmessages.DirectItemTextViewHolder;
import awais.instagrabber.adapters.viewholder.directmessages.DirectItemVideoCallEventViewHolder;
import awais.instagrabber.adapters.viewholder.directmessages.DirectItemViewHolder;
import awais.instagrabber.adapters.viewholder.directmessages.DirectItemVoiceMediaViewHolder;
import awais.instagrabber.adapters.viewholder.directmessages.DirectItemXmaViewHolder;
import awais.instagrabber.customviews.emoji.Emoji;
import awais.instagrabber.databinding.LayoutDmActionLogBinding;
import awais.instagrabber.databinding.LayoutDmAnimatedMediaBinding;
import awais.instagrabber.databinding.LayoutDmBaseBinding;
import awais.instagrabber.databinding.LayoutDmHeaderBinding;
import awais.instagrabber.databinding.LayoutDmLikeBinding;
import awais.instagrabber.databinding.LayoutDmLinkBinding;
import awais.instagrabber.databinding.LayoutDmMediaBinding;
import awais.instagrabber.databinding.LayoutDmMediaShareBinding;
import awais.instagrabber.databinding.LayoutDmProfileBinding;
import awais.instagrabber.databinding.LayoutDmRavenMediaBinding;
import awais.instagrabber.databinding.LayoutDmReelShareBinding;
import awais.instagrabber.databinding.LayoutDmStoryShareBinding;
import awais.instagrabber.databinding.LayoutDmTextBinding;
import awais.instagrabber.databinding.LayoutDmVoiceMediaBinding;
import awais.instagrabber.models.enums.DirectItemType;
import awais.instagrabber.repositories.responses.Media;
import awais.instagrabber.repositories.responses.User;
import awais.instagrabber.repositories.responses.directmessages.DirectItem;
import awais.instagrabber.repositories.responses.directmessages.DirectItemStoryShare;
import awais.instagrabber.repositories.responses.directmessages.DirectThread;

public final class DirectItemsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final String TAG = DirectItemsAdapter.class.getSimpleName();

    private List<DirectItem> items;
    private DirectThread thread;
    private DirectItemViewHolder selectedViewHolder;

    private final User currentUser;
    private final DirectItemCallback callback;
    private final AsyncListDiffer<DirectItemOrHeader> differ;
    private final DirectItemInternalLongClickListener longClickListener;

    private static final DiffUtil.ItemCallback<DirectItemOrHeader> diffCallback = new DiffUtil.ItemCallback<DirectItemOrHeader>() {
        @Override
        public boolean areItemsTheSame(@NonNull DirectItemOrHeader oldItem, @NonNull DirectItemOrHeader newItem) {
            boolean bothHeaders = oldItem.isHeader() && newItem.isHeader();
            boolean bothItems = !oldItem.isHeader() && !newItem.isHeader();
            final boolean areSameType = bothHeaders || bothItems;
            if (!areSameType) return false;
            if (bothHeaders) {
                return oldItem.date.equals(newItem.date);
            }
            if (oldItem.item != null && newItem.item != null) {
                String oldClientContext = oldItem.item.getClientContext();
                if (oldClientContext == null) {
                    oldClientContext = oldItem.item.getItemId();
                }
                String newClientContext = newItem.item.getClientContext();
                if (newClientContext == null) {
                    newClientContext = newItem.item.getItemId();
                }
                return oldClientContext.equals(newClientContext);
            }
            return false;
        }

        @Override
        public boolean areContentsTheSame(@NonNull DirectItemOrHeader oldItem, @NonNull DirectItemOrHeader newItem) {
            boolean bothHeaders = oldItem.isHeader() && newItem.isHeader();
            boolean bothItems = !oldItem.isHeader() && !newItem.isHeader();
            final boolean areSameType = bothHeaders || bothItems;
            if (!areSameType) return false;
            if (bothHeaders) {
                return oldItem.date.equals(newItem.date);
            }
            boolean timestampEqual = oldItem.item.getTimestamp() == newItem.item.getTimestamp();
            boolean bothPending = oldItem.item.isPending() == newItem.item.isPending();
            boolean reactionSame = Objects.equals(oldItem.item.getReactions(), newItem.item.getReactions());
            return timestampEqual && bothPending && reactionSame;
        }
    };

    public DirectItemsAdapter(@NonNull User currentUser,
                              @NonNull DirectThread thread,
                              @NonNull DirectItemCallback callback,
                              @NonNull DirectItemLongClickListener itemLongClickListener) {
        this.currentUser = currentUser;
        this.thread = thread;
        this.callback = callback;
        this.differ = new AsyncListDiffer<>(new AdapterListUpdateCallback(this),
                                       new AsyncDifferConfig.Builder<>(DirectItemsAdapter.diffCallback).build());
        this.longClickListener = (position, viewHolder) -> {
            if (this.selectedViewHolder != null) {
                this.selectedViewHolder.setSelected(false);
            }
            this.selectedViewHolder = viewHolder;
            viewHolder.setSelected(true);
            itemLongClickListener.onLongClick(position);
        };
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        if (type == -1) {
            // header
            return new HeaderViewHolder(LayoutDmHeaderBinding.inflate(layoutInflater, parent, false));
        }
        LayoutDmBaseBinding baseBinding = LayoutDmBaseBinding.inflate(layoutInflater, parent, false);
        DirectItemType directItemType = DirectItemType.Companion.getTypeFromId(type);
        DirectItemViewHolder itemViewHolder = this.getItemViewHolder(layoutInflater, baseBinding, directItemType);
        itemViewHolder.setLongClickListener(this.longClickListener);
        return itemViewHolder;
    }

    @NonNull
    private DirectItemViewHolder getItemViewHolder(LayoutInflater layoutInflater,
                                                   LayoutDmBaseBinding baseBinding,
                                                   @NonNull DirectItemType directItemType) {
        switch (directItemType) {
            case TEXT: {
                LayoutDmTextBinding binding = LayoutDmTextBinding.inflate(layoutInflater, baseBinding.message, false);
                return new DirectItemTextViewHolder(baseBinding, binding, this.currentUser, this.thread, this.callback);
            }
            case LIKE: {
                LayoutDmLikeBinding binding = LayoutDmLikeBinding.inflate(layoutInflater, baseBinding.message, false);
                return new DirectItemLikeViewHolder(baseBinding, binding, this.currentUser, this.thread, this.callback);
            }
            case LINK: {
                LayoutDmLinkBinding binding = LayoutDmLinkBinding.inflate(layoutInflater, baseBinding.message, false);
                return new DirectItemLinkViewHolder(baseBinding, binding, this.currentUser, this.thread, this.callback);
            }
            case ACTION_LOG: {
                LayoutDmActionLogBinding binding = LayoutDmActionLogBinding.inflate(layoutInflater, baseBinding.message, false);
                return new DirectItemActionLogViewHolder(baseBinding, binding, this.currentUser, this.thread, this.callback);
            }
            case VIDEO_CALL_EVENT: {
                LayoutDmActionLogBinding binding = LayoutDmActionLogBinding.inflate(layoutInflater, baseBinding.message, false);
                return new DirectItemVideoCallEventViewHolder(baseBinding, binding, this.currentUser, this.thread, this.callback);
            }
            case PLACEHOLDER: {
                LayoutDmStoryShareBinding binding = LayoutDmStoryShareBinding.inflate(layoutInflater, baseBinding.message, false);
                return new DirectItemPlaceholderViewHolder(baseBinding, binding, this.currentUser, this.thread, this.callback);
            }
            case ANIMATED_MEDIA: {
                LayoutDmAnimatedMediaBinding binding = LayoutDmAnimatedMediaBinding.inflate(layoutInflater, baseBinding.message, false);
                return new DirectItemAnimatedMediaViewHolder(baseBinding, binding, this.currentUser, this.thread, this.callback);
            }
            case VOICE_MEDIA: {
                LayoutDmVoiceMediaBinding binding = LayoutDmVoiceMediaBinding.inflate(layoutInflater, baseBinding.message, false);
                return new DirectItemVoiceMediaViewHolder(baseBinding, binding, this.currentUser, this.thread, this.callback);
            }
            case LOCATION:
            case PROFILE: {
                LayoutDmProfileBinding binding = LayoutDmProfileBinding.inflate(layoutInflater, baseBinding.message, false);
                return new DirectItemProfileViewHolder(baseBinding, binding, this.currentUser, this.thread, this.callback);
            }
            case MEDIA: {
                LayoutDmMediaBinding binding = LayoutDmMediaBinding.inflate(layoutInflater, baseBinding.message, false);
                return new DirectItemMediaViewHolder(baseBinding, binding, this.currentUser, this.thread, this.callback);
            }
            case CLIP:
            case FELIX_SHARE:
            case MEDIA_SHARE: {
                LayoutDmMediaShareBinding binding = LayoutDmMediaShareBinding.inflate(layoutInflater, baseBinding.message, false);
                return new DirectItemMediaShareViewHolder(baseBinding, binding, this.currentUser, this.thread, this.callback);
            }
            case STORY_SHARE: {
                LayoutDmStoryShareBinding binding = LayoutDmStoryShareBinding.inflate(layoutInflater, baseBinding.message, false);
                return new DirectItemStoryShareViewHolder(baseBinding, binding, this.currentUser, this.thread, this.callback);
            }
            case REEL_SHARE: {
                LayoutDmReelShareBinding binding = LayoutDmReelShareBinding.inflate(layoutInflater, baseBinding.message, false);
                return new DirectItemReelShareViewHolder(baseBinding, binding, this.currentUser, this.thread, this.callback);
            }
            case RAVEN_MEDIA: {
                LayoutDmRavenMediaBinding binding = LayoutDmRavenMediaBinding.inflate(layoutInflater, baseBinding.message, false);
                return new DirectItemRavenMediaViewHolder(baseBinding, binding, this.currentUser, this.thread, this.callback);
            }
            case XMA: {
                LayoutDmAnimatedMediaBinding binding = LayoutDmAnimatedMediaBinding.inflate(layoutInflater, baseBinding.message, false);
                return new DirectItemXmaViewHolder(baseBinding, binding, this.currentUser, this.thread, this.callback);
            }
            case UNKNOWN:
            default: {
                LayoutDmTextBinding binding = LayoutDmTextBinding.inflate(layoutInflater, baseBinding.message, false);
                return new DirectItemDefaultViewHolder(baseBinding, binding, this.currentUser, this.thread, this.callback);
            }
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        DirectItemOrHeader itemOrHeader = this.getItem(position);
        if (itemOrHeader.isHeader()) {
            ((HeaderViewHolder) holder).bind(itemOrHeader.date);
            return;
        }
        if (this.thread == null) return;
        ((DirectItemViewHolder) holder).bind(position, itemOrHeader.item);
    }

    protected DirectItemOrHeader getItem(final int position) {
        return this.differ.getCurrentList().get(position);
    }

    @Override
    public int getItemCount() {
        return this.differ.getCurrentList().size();
    }

    @Override
    public int getItemViewType(int position) {
        DirectItemOrHeader itemOrHeader = this.getItem(position);
        if (itemOrHeader.isHeader()) {
            return -1;
        }
        DirectItemType itemType = itemOrHeader.item.getItemType();
        if (itemType == null) {
            return 0;
        }
        return itemType.getId();
    }

    @Override
    public long getItemId(int position) {
        DirectItemOrHeader itemOrHeader = this.getItem(position);
        if (itemOrHeader.isHeader()) {
            return itemOrHeader.date.hashCode();
        }
        if (itemOrHeader.item.getClientContext() == null) {
            return itemOrHeader.item.getItemId().hashCode();
        }
        return itemOrHeader.item.getClientContext().hashCode();
    }

    public void setThread(DirectThread thread) {
        if (thread == null) return;
        this.thread = thread;
        // notifyDataSetChanged();
    }

    public void submitList(@Nullable List<DirectItem> list) {
        if (list == null) {
            this.differ.submitList(null);
            return;
        }
        this.differ.submitList(this.sectionAndSort(list));
        items = list;
    }

    public void submitList(@Nullable List<DirectItem> list, @Nullable Runnable commitCallback) {
        if (list == null) {
            this.differ.submitList(null, commitCallback);
            return;
        }
        this.differ.submitList(this.sectionAndSort(list), commitCallback);
        items = list;
    }

    private List<DirectItemOrHeader> sectionAndSort(List<DirectItem> list) {
        List<DirectItemOrHeader> itemOrHeaders = new ArrayList<>();
        LocalDate prevSectionDate = null;
        for (int i = 0; i < list.size(); i++) {
            DirectItem item = list.get(i);
            if (item == null || item.getDate() == null) continue;
            DirectItemOrHeader prev = itemOrHeaders.isEmpty() ? null : itemOrHeaders.get(itemOrHeaders.size() - 1);
            if (prev != null
                    && prev.item != null
                    && prev.item.getDate() != null
                    && prev.item.getDate().toLocalDate().isEqual(item.getDate().toLocalDate())) {
                // just add item
                DirectItemOrHeader itemOrHeader = new DirectItemOrHeader();
                itemOrHeader.item = item;
                itemOrHeaders.add(itemOrHeader);
                if (i == list.size() - 1) {
                    // add header
                    DirectItemOrHeader itemOrHeader2 = new DirectItemOrHeader();
                    itemOrHeader2.date = prevSectionDate;
                    itemOrHeaders.add(itemOrHeader2);
                }
                continue;
            }
            if (prevSectionDate != null) {
                // add header
                DirectItemOrHeader itemOrHeader = new DirectItemOrHeader();
                itemOrHeader.date = prevSectionDate;
                itemOrHeaders.add(itemOrHeader);
            }
            // Add item
            DirectItemOrHeader itemOrHeader = new DirectItemOrHeader();
            itemOrHeader.item = item;
            itemOrHeaders.add(itemOrHeader);
            prevSectionDate = item.getDate().toLocalDate();
        }
        return itemOrHeaders;
    }

    public List<DirectItemOrHeader> getList() {
        return this.differ.getCurrentList();
    }

    public List<DirectItem> getItems() {
        return this.items;
    }

    @Override
    public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
        if (holder instanceof DirectItemViewHolder) {
            ((DirectItemViewHolder) holder).cleanup();
        }
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull RecyclerView.ViewHolder holder) {
        if (holder instanceof DirectItemViewHolder) {
            ((DirectItemViewHolder) holder).cleanup();
        }
    }

    public DirectThread getThread() {
        return this.thread;
    }

    public static class DirectItemOrHeader {
        LocalDate date;
        public DirectItem item;

        public boolean isHeader() {
            return this.date != null;
        }

        @NonNull
        @Override
        public String toString() {
            return "DirectItemOrHeader{" +
                    "date=" + this.date +
                    ", item=" + (this.item != null ? this.item.getItemType() : null) +
                    '}';
        }
    }

    public static class HeaderViewHolder extends RecyclerView.ViewHolder {
        private final LayoutDmHeaderBinding binding;

        public HeaderViewHolder(@NonNull LayoutDmHeaderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(LocalDate date) {
            if (date == null) {
                this.binding.header.setText("");
                return;
            }
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT);
            this.binding.header.setText(dateFormatter.format(date));
        }
    }

    public interface DirectItemCallback {
        void onHashtagClick(String hashtag);

        void onMentionClick(String mention);

        void onLocationClick(long locationId);

        void onURLClick(String url);

        void onEmailClick(String email);

        void onMediaClick(Media media, int index);

        void onStoryClick(DirectItemStoryShare storyShare);

        void onReaction(DirectItem item, Emoji emoji);

        void onReactionClick(DirectItem item, int position);

        void onOptionSelect(DirectItem item, @IdRes int itemId, Function<DirectItem, Void> callback);

        void onAddReactionListener(DirectItem item);
    }

    public interface DirectItemInternalLongClickListener {
        void onLongClick(int position, DirectItemViewHolder viewHolder);
    }

    public interface DirectItemLongClickListener {
        void onLongClick(int position);
    }
}