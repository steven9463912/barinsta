package awais.instagrabber.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.common.collect.ImmutableList;

import java.util.List;

import awais.instagrabber.R;
import awais.instagrabber.adapters.viewholder.directmessages.DirectUserViewHolder;
import awais.instagrabber.databinding.ItemFavSectionHeaderBinding;
import awais.instagrabber.databinding.LayoutDmUserItemBinding;
import awais.instagrabber.repositories.responses.User;

public final class DirectUsersAdapter extends ListAdapter<DirectUsersAdapter.DirectUserOrHeader, RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_USER = 1;
    private static final DiffUtil.ItemCallback<DirectUserOrHeader> DIFF_CALLBACK = new DiffUtil.ItemCallback<DirectUserOrHeader>() {
        @Override
        public boolean areItemsTheSame(@NonNull DirectUserOrHeader oldItem, @NonNull DirectUserOrHeader newItem) {
            boolean bothHeaders = oldItem.isHeader() && newItem.isHeader();
            boolean bothItems = !oldItem.isHeader() && !newItem.isHeader();
            final boolean areSameType = bothHeaders || bothItems;
            if (!areSameType) return false;
            if (bothHeaders) {
                return oldItem.headerTitle == newItem.headerTitle;
            }
            if (oldItem.user != null && newItem.user != null) {
                return oldItem.user.getPk() == newItem.user.getPk();
            }
            return false;
        }

        @Override
        public boolean areContentsTheSame(@NonNull DirectUserOrHeader oldItem, @NonNull DirectUserOrHeader newItem) {
            boolean bothHeaders = oldItem.isHeader() && newItem.isHeader();
            boolean bothItems = !oldItem.isHeader() && !newItem.isHeader();
            final boolean areSameType = bothHeaders || bothItems;
            if (!areSameType) return false;
            if (bothHeaders) {
                return oldItem.headerTitle == newItem.headerTitle;
            }
            if (oldItem.user != null && newItem.user != null) {
                return oldItem.user.getUsername().equals(newItem.user.getUsername()) &&
                        oldItem.user.getFullName().equals(newItem.user.getFullName());
            }
            return false;
        }
    };

    private final long inviterId;
    private final OnDirectUserClickListener onClickListener;
    private final OnDirectUserLongClickListener onLongClickListener;
    private List<Long> adminUserIds;

    public DirectUsersAdapter(long inviterId,
                              OnDirectUserClickListener onClickListener,
                              OnDirectUserLongClickListener onLongClickListener) {
        super(DirectUsersAdapter.DIFF_CALLBACK);
        this.inviterId = inviterId;
        this.onClickListener = onClickListener;
        this.onLongClickListener = onLongClickListener;
        this.setHasStableIds(true);
    }

    public void submitUsers(List<User> users, List<User> leftUsers) {
        if (users == null && leftUsers == null) return;
        List<DirectUserOrHeader> userOrHeaders = this.combineLists(users, leftUsers);
        this.submitList(userOrHeaders);
    }

    private List<DirectUserOrHeader> combineLists(List<User> users, List<User> leftUsers) {
        ImmutableList.Builder<DirectUserOrHeader> listBuilder = ImmutableList.builder();
        if (users != null && !users.isEmpty()) {
            listBuilder.add(new DirectUserOrHeader(R.string.members));
            users.stream()
                 .map(DirectUserOrHeader::new)
                 .forEach(listBuilder::add);
        }
        if (leftUsers != null && !leftUsers.isEmpty()) {
            listBuilder.add(new DirectUserOrHeader(R.string.dms_left_users));
            leftUsers.stream()
                     .map(DirectUserOrHeader::new)
                     .forEach(listBuilder::add);
        }
        return listBuilder.build();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        switch (viewType) {
            case DirectUsersAdapter.VIEW_TYPE_USER:
                LayoutDmUserItemBinding binding = LayoutDmUserItemBinding.inflate(layoutInflater, parent, false);
                return new DirectUserViewHolder(binding, this.onClickListener, this.onLongClickListener);
            case DirectUsersAdapter.VIEW_TYPE_HEADER:
            default:
                ItemFavSectionHeaderBinding headerBinding = ItemFavSectionHeaderBinding.inflate(layoutInflater, parent, false);
                return new HeaderViewHolder(headerBinding);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).bind(this.getItem(position).headerTitle);
            return;
        }
        if (holder instanceof DirectUserViewHolder) {
            User user = this.getItem(position).user;
            ((DirectUserViewHolder) holder).bind(position,
                                                 user,
                                                 user != null && this.adminUserIds != null && this.adminUserIds.contains(user.getPk()),
                                                 user != null && user.getPk() == this.inviterId,
                                                 false,
                                                 false);
        }
    }

    @Override
    public int getItemViewType(int position) {
        DirectUserOrHeader item = this.getItem(position);
        return item.isHeader() ? DirectUsersAdapter.VIEW_TYPE_HEADER : DirectUsersAdapter.VIEW_TYPE_USER;
    }

    @Override
    public long getItemId(int position) {
        DirectUserOrHeader item = this.getItem(position);
        return item.isHeader() ? item.headerTitle : item.user.getPk();
    }

    public void setAdminUserIds(List<Long> adminUserIds) {
        this.adminUserIds = adminUserIds;
        this.notifyDataSetChanged();
    }

    public static class DirectUserOrHeader {
        int headerTitle;
        User user;

        public DirectUserOrHeader(int headerTitle) {
            this.headerTitle = headerTitle;
        }

        public DirectUserOrHeader(User user) {
            this.user = user;
        }

        boolean isHeader() {
            return this.headerTitle > 0;
        }
    }

    public static class HeaderViewHolder extends RecyclerView.ViewHolder {
        private final ItemFavSectionHeaderBinding binding;

        public HeaderViewHolder(@NonNull ItemFavSectionHeaderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(@StringRes int headerTitle) {
            this.binding.getRoot().setText(headerTitle);
        }
    }

    public interface OnDirectUserClickListener {
        void onClick(int position, User user, boolean selected);
    }

    public interface OnDirectUserLongClickListener {
        boolean onLongClick(int position, User user);
    }
}