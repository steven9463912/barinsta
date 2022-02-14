package thoughtbot.expandableadapter;

import java.util.List;

import awais.instagrabber.repositories.responses.User;

public class ExpandableGroup {
    private final String title;
    private final List<User> items;

    public ExpandableGroup(String title, List<User> items) {
        this.title = title;
        this.items = items;
    }

    public String getTitle() {
        return this.title;
    }

    public List<User> getItems() {
        return this.items;
    }

    public int getItemCount() {
        if (this.items != null) {
            return this.items.size();
        }
        return 0;
    }
}