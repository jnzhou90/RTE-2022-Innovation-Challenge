package com.qingkouwei.handyinstruction.common.db.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import java.io.Serializable;

@Entity(tableName = "app_key", primaryKeys = {"appKey"})
public class AppKeyEntity implements Serializable {

    @NonNull
    private String appKey;
    private double timestamp;

    public AppKeyEntity(@NonNull String appKey) {
        this.appKey = appKey;
        this.timestamp = System.currentTimeMillis();
    }

    public String getAppKey() {
        return appKey;
    }

    public void setAppKey(@NonNull String appKey) {
        this.appKey = appKey;
    }

    public double getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(double timestamp) {
        this.timestamp = timestamp;
    }
}
