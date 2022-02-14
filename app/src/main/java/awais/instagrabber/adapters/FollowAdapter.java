package awais.instagrabber.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import awais.instagrabber.R;
import awais.instagrabber.adapters.viewholder.FollowsViewHolder;
import awais.instagrabber.databinding.ItemFollowBinding;
import awais.instagrabber.interfaces.OnGroupClickListener;
import awais.instagrabber.repositories.responses.User;
import awais.instagrabber.utils.TextUtils;
import thoughtbot.expandableadapter.ExpandableGroup;
import thoughtbot.expandableadapter.ExpandableList;
import thoughtbot.expandableadapter.ExpandableListPosition;
import thoughtbot.expandableadapter.GroupViewHolder;

// thanks to ThoughtBot's ExpandableRecyclerViewAdapter
//   https://github.com/thoughtbot/expandable-recycler-view
public final class FollowAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements OnGroupClickListener, Filterable {
    private final View.OnClickListener onClickListener;
    private final ExpandableList expandableListOriginal;
    private final boolean hasManyGroups;
    private ExpandableList expandableList;

    private final Filter filter = new Filter() {
        @Nullable
        @Override
        protected FilterResults performFiltering(CharSequence filter) {
            List<User> filteredItems = new ArrayList<>();
            if (FollowAdapter.this.expandableListOriginal.groups == null || TextUtils.isEmpty(filter)) return null;
            String query = filter.toString().toLowerCase();
            ArrayList<ExpandableGroup> groups = new ArrayList<>();
            for (int x = 0; x < FollowAdapter.this.expandableListOriginal.groups.size(); ++x) {
                ExpandableGroup expandableGroup = FollowAdapter.this.expandableListOriginal.groups.get(x);
                String title = expandableGroup.getTitle();
                List<User> items = expandableGroup.getItems();
                if (items != null) {
                    List<User> toReturn = items.stream()
                            .filter(u -> this.hasKey(query, u.getUsername(), u.getFullName()))
                            .collect(Collectors.toList());
                    groups.add(new ExpandableGroup(title, toReturn));
                }
            }
            FilterResults filterResults = new FilterResults();
            filterResults.values = new ExpandableList(groups, FollowAdapter.this.expandableList.expandedGroupIndexes);
            return filterResults;
        }

        private boolean hasKey(String key, String username, String name) {
            if (TextUtils.isEmpty(key)) return true;
            boolean hasUserName = username != null && username.toLowerCase().contains(key);
            if (!hasUserName && name != null) return name.toLowerCase().contains(key);
            return true;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            if (results == null) {
                FollowAdapter.this.expandableList = FollowAdapter.this.expandableListOriginal;
            }
            else {
                FollowAdapter.this.expandableList = (ExpandableList) results.values;
            }
            FollowAdapter.this.notifyDataSetChanged();
        }
    };

    public FollowAdapter(View.OnClickListener onClickListener, @NonNull ArrayList<ExpandableGroup> groups) {
        expandableListOriginal = new ExpandableList(groups);
        this.expandableList = expandableListOriginal;
        this.onClickListener = onClickListener;
        hasManyGroups = groups.size() > 1;
    }

    @Override
    public Filter getFilter() {
        return this.filter;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        boolean isGroup = this.hasManyGroups && viewType == ExpandableListPosition.GROUP;
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View view;
        if (isGroup) {
            view = layoutInflater.inflate(R.layout.header_follow, parent, false);
            return new GroupViewHolder(view, this);
        } else {
            ItemFollowBinding binding = ItemFollowBinding.inflate(layoutInflater, parent, false);
            return new FollowsViewHolder(binding);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ExpandableListPosition listPos = this.expandableList.getUnflattenedPosition(position);
        ExpandableGroup group = this.expandableList.getExpandableGroup(listPos);

        if (this.hasManyGroups && listPos.type == ExpandableListPosition.GROUP) {
            GroupViewHolder gvh = (GroupViewHolder) holder;
            gvh.setTitle(group.getTitle());
            gvh.toggle(this.isGroupExpanded(group));
            return;
        }
        User model = group.getItems().get(this.hasManyGroups ? listPos.childPos : position);
        ((FollowsViewHolder) holder).bind(model, this.onClickListener);
    }

    @Override
    public int getItemCount() {
        return this.expandableList.getVisibleItemCount() - (this.hasManyGroups ? 0 : 1);
    }

    @Override
    public int getItemViewType(int position) {
        return !this.hasManyGroups ? 0 : this.expandableList.getUnflattenedPosition(position).type;
    }

    @Override
    public void toggleGroup(int flatPos) {
        ExpandableListPosition listPosition = this.expandableList.getUnflattenedPosition(flatPos);

        int groupPos = listPosition.groupPos;
        int positionStart = this.expandableList.getFlattenedGroupIndex(listPosition) + 1;
        int positionEnd = this.expandableList.groups.get(groupPos).getItemCount();

        boolean isExpanded = this.expandableList.expandedGroupIndexes[groupPos];
        this.expandableList.expandedGroupIndexes[groupPos] = !isExpanded;
        this.notifyItemChanged(positionStart - 1);
        if (positionEnd > 0) {
            if (isExpanded) this.notifyItemRangeRemoved(positionStart, positionEnd);
            else this.notifyItemRangeInserted(positionStart, positionEnd);
        }
    }

    public boolean isGroupExpanded(ExpandableGroup group) {
        return this.expandableList.expandedGroupIndexes[this.expandableList.groups.indexOf(group)];
    }
}