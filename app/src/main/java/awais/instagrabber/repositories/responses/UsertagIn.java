package awais.instagrabber.repositories.responses;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

public class UsertagIn implements Serializable {
    private final User user;
    private final List<String> position;

    public UsertagIn(User user, List<String> position) {
        this.user = user;
        this.position = position;
    }

    public User getUser() {
        return this.user;
    }

    public List<String> getPosition() {
        return this.position;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || this.getClass() != o.getClass()) return false;
        UsertagIn usertagIn = (UsertagIn) o;
        return Objects.equals(this.user, usertagIn.user) &&
                Objects.equals(this.position, usertagIn.position);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.user, this.position);
    }
}
