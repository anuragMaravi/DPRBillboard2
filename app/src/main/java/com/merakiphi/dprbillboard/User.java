package com.merakiphi.dprbillboard;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class User {

    public String district;
    public String latitude;
    public String longitude;
    public String status;
    public String height;
    public String width;
    public String type;
    public String backlight;
    public String notes;
    public String timeStamp;
    public String booking_from;
    public String booking_till;
    public String images;

    // Default constructor required for calls to
    // DataSnapshot.getValue(User.class)
    public User() {
    }

    public User(String district, String latitude,String longitude,String status,String height,String width,String type,String backlight,String notes,String timeStamp,String booking_from,String booking_till,String images) {
        this.district = district;
        this.latitude = latitude;
        this.longitude = longitude;
        this.status = status;
        this.height = height;
        this.width = width;
        this.type = type;
        this.backlight = backlight;
        this.notes = notes;
        this.timeStamp = timeStamp;
        this.booking_from = booking_from;
        this.booking_till = booking_till;
        this.images = images;
    }
}
