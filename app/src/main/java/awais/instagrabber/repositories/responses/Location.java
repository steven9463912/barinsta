package awais.instagrabber.repositories.responses;

import java.io.Serializable;
import java.util.Objects;

public class Location implements Serializable {
    private final long pk;
    private final String shortName;
    private final String name;
    private final String address;
    private final String city;
    private final double lng;
    private final double lat;

    public Location(long pk,
                    String shortName,
                    String name,
                    String address,
                    String city,
                    double lng,
                    double lat) {
        this.pk = pk;
        this.shortName = shortName;
        this.name = name;
        this.address = address;
        this.city = city;
        this.lng = lng;
        this.lat = lat;
    }

    public long getPk() {
        return this.pk;
    }

    public String getShortName() {
        return this.shortName;
    }

    public String getName() {
        return this.name;
    }

    public String getAddress() {
        return this.address;
    }

    public String getCity() {
        return this.city;
    }

    public double getLng() {
        return this.lng;
    }

    public double getLat() {
        return this.lat;
    }

    public String getGeo() { return "geo:" + this.lat + "," + this.lng + "?z=17&q=" + this.lat + "," + this.lng + "(" + this.name + ")"; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || this.getClass() != o.getClass()) return false;
        Location location = (Location) o;
        return this.pk == location.pk &&
                Double.compare(location.lng, this.lng) == 0 &&
                Double.compare(location.lat, this.lat) == 0 &&
                Objects.equals(this.shortName, location.shortName) &&
                Objects.equals(this.name, location.name) &&
                Objects.equals(this.address, location.address) &&
                Objects.equals(this.city, location.city);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.pk, this.shortName, this.name, this.address, this.city, this.lng, this.lat);
    }
}
