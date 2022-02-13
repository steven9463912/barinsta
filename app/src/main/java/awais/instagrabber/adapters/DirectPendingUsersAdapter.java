package awais.instagrabber.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import awais.instagrabber.adapters.viewholder.directmessages.DirectPendingUserViewHolder;
import awais.instagrabber.databinding.LayoutDmPendingUserItemBinding;
import awais.instagrabber.repositories.responses.User;
import awais.instagrabber.repositories.responses.directmessages.DirectThreadParticipantRequestsResponse;

public final class DirectPendingUsersAdapter extends ListAdapter<DirectPendingUsersAdapter.PendingUser, DirectPendingUserViewHolder> {

    private static final DiffUtil.ItemCallback<PendingUser> DIFF_CALLBACK = new DiffUtil.ItemCallback<PendingUser>() {
        @Override
        public boolean areItemsTheSame(@NonNull PendingUser oldItem, @NonNull PendingUser newItem) {
            return oldItem.user.getPk() == newItem.user.getPk();
        }

        @Override
        public boolean areContentsTheSame(@NonNull PendingUser oldItem, @NonNull PendingUser newItem) {
            return Objects.equals(oldItem.user.getUsername(), newItem.user.getUsername()) &&
                    Objects.equals(oldItem.user.getFullName(), newItem.user.getFullName()) &&
                    Objects.equals(oldItem.requester, newItem.requester);
        }
    };

    private final PendingUserCallback callback;

    public DirectPendingUsersAdapter(PendingUserCallback callback) {
        super(DirectPendingUsersAdapter.DIFF_CALLBACK);
        this.callback = callback;
        this.setHasStableIds(true);
    }

    public void submitPendingRequests(DirectThreadParticipantRequestsResponse requests) {
        if (requests == null || requests.getUsers() == null) {
            this.submitList(Collections.emptyList());
            return;
        }
        this.submitList(this.parse(requests));
    }

    private List<PendingUser> parse(DirectThreadParticipantRequestsResponse requests) {
        List<User> users = requests.getUsers();
        Map<Long, String> requesterUsernames = requests.getRequesterUsernames();
        return users.stream()
                    .map(user -> new PendingUser(user, requesterUsernames.get(user.getPk())))
                    .collect(Collectors.toList());
    }

    @NonNull
    @Override
    public DirectPendingUserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        LayoutDmPendingUserItemBinding binding = LayoutDmPendingUserItemBinding.inflate(layoutInflater, parent, false);
        return new DirectPendingUserViewHolder(binding, this.callback);
    }

    @Override
    public void onBindViewHolder(@NonNull DirectPendingUserViewHolder holder, int position) {
        PendingUser pendingUser = this.getItem(position);
        holder.bind(position, pendingUser);
    }

    @Override
    public long getItemId(int position) {
        PendingUser item = this.getItem(position);
        return item.user.getPk();
    }

    public static class PendingUser {
        private final User user;
        private final String requester;

        private boolean inProgress;

        public PendingUser(User user, String requester) {
            this.user = user;
            this.requester = requester;
        }

        public User getUser() {
            return this.user;
        }

        public String getRequester() {
            return this.requester;
        }

        public boolean isInProgress() {
            return this.inProgress;
        }

        public PendingUser setInProgress(boolean inProgress) {
            this.inProgress = inProgress;
            return this;
        }
    }

    public interface PendingUserCallback {
        void onClick(int position, PendingUser pendingUser);

        void onApprove(int position, PendingUser pendingUser);

        void onDeny(int position, PendingUser pendingUser);
    }
}