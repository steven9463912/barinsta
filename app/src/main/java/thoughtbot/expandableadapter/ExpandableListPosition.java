package thoughtbot.expandableadapter;

import androidx.annotation.NonNull;

public class ExpandableListPosition {
    private static final ExpandableListPosition LIST_POSITION = new ExpandableListPosition();
    public static final int CHILD = 1;
    public static final int GROUP = 2;
    private int flatListPos;
    public int groupPos;
    public int childPos;
    public int type;

    @NonNull
    public static ExpandableListPosition obtain(int type, int groupPos, int childPos, int flatListPos) {
        ExpandableListPosition.LIST_POSITION.type = type;
        ExpandableListPosition.LIST_POSITION.groupPos = groupPos;
        ExpandableListPosition.LIST_POSITION.childPos = childPos;
        ExpandableListPosition.LIST_POSITION.flatListPos = flatListPos;
        return ExpandableListPosition.LIST_POSITION;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        //if (o != null && getClass() == o.getClass()) {
        if (o instanceof ExpandableListPosition) {
            ExpandableListPosition that = (ExpandableListPosition) o;
            if (this.groupPos != that.groupPos) return false;
            if (this.childPos != that.childPos) return false;
            if (this.flatListPos != that.flatListPos) return false;
            return this.type == that.type;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 31 * (31 * (31 * this.groupPos + this.childPos) + this.flatListPos) + this.type;
    }
}