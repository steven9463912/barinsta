package thoughtbot.expandableadapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;

public final class ExpandableList {
    private final int groupsSize;
    public final ArrayList<ExpandableGroup> groups;
    public final boolean[] expandedGroupIndexes;

    public ExpandableList(@NonNull ArrayList<ExpandableGroup> groups) {
        this.groups = groups;
        groupsSize = groups.size();
        expandedGroupIndexes = new boolean[this.groupsSize];
    }

    public ExpandableList(@NonNull ArrayList<ExpandableGroup> groups,
                          @Nullable boolean[] expandedGroupIndexes) {
        this.groups = groups;
        groupsSize = groups.size();
        this.expandedGroupIndexes = expandedGroupIndexes;
    }

    public int getVisibleItemCount() {
        int count = 0;
        for (int i = 0; i < this.groupsSize; i++) count = count + this.numberOfVisibleItemsInGroup(i);
        return count;
    }

    @NonNull
    public ExpandableListPosition getUnflattenedPosition(int flPos) {
        int adapted = flPos;
        for (int i = 0; i < this.groupsSize; i++) {
            int groupItemCount = this.numberOfVisibleItemsInGroup(i);
            if (adapted == 0)
                return ExpandableListPosition.obtain(ExpandableListPosition.GROUP, i, -1, flPos);
            else if (adapted < groupItemCount)
                return ExpandableListPosition.obtain(ExpandableListPosition.CHILD, i, adapted - 1, flPos);
            adapted = adapted - groupItemCount;
        }
        throw new RuntimeException("Unknown state");
    }

    private int numberOfVisibleItemsInGroup(int group) {
        return this.expandedGroupIndexes[group] ? this.groups.get(group).getItemCount() + 1 : 1;
    }

    public int getFlattenedGroupIndex(@NonNull ExpandableListPosition listPosition) {
        int runningTotal = 0;
        for (int i = 0; i < listPosition.groupPos; i++) runningTotal = runningTotal + this.numberOfVisibleItemsInGroup(i);
        return runningTotal;
    }

    public ExpandableGroup getExpandableGroup(@NonNull final ExpandableListPosition listPosition) {
        return this.groups.get(listPosition.groupPos);
    }
}