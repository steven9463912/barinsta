package awais.instagrabber.repositories.responses;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

public class Usertags implements Serializable {
    private final List<UsertagIn> in;

    public Usertags(List<UsertagIn> in) {
        this.in = in;
    }

    public List<UsertagIn> getIn() {
        return this.in;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || this.getClass() != o.getClass()) return false;
        Usertags usertags = (Usertags) o;
        return Objects.equals(this.in, usertags.in);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.in);
    }
}
