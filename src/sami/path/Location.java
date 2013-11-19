package sami.path;

import java.io.Serializable;

public class Location implements Serializable {

    private UTMCoordinate coordinate;
    private double altitude;

    public Location() {
    }

    public Location(UTMCoordinate u, double altitude) {
        this.coordinate = u;
        this.altitude = altitude;
    }

    public Location(double latDeg, double lonDeg, double altitude) {
        this(new UTMCoordinate(latDeg, lonDeg), altitude);
    }

    public UTMCoordinate getCoordinate() {
        return coordinate;
    }

    public void setCoordinate(UTMCoordinate coordinate) {
        this.coordinate = coordinate;
    }

    public double getAltitude() {
        return altitude;
    }

    public void setAltitude(double altitude) {
        this.altitude = altitude;
    }

    public Location clone() {
        Location location = new Location();
        location.setAltitude(this.getAltitude());
        location.setCoordinate(this.coordinate);
        return location;
    }

    public String toString() {
        return "Location: [" + coordinate + ", " + altitude + "]";
    }
}
