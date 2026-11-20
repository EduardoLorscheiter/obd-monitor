/*
 **=================================================================================================
 **  @file    Device.java
 **  @author  Eduardo Lorscheiter
 **  @class   Device
 **  @date    21/07/2026
 **  @version 1.0
 **  @brief   Representa um dispositivo Bluetooth da aplicação, armazenando informações como nome,
 **           endereço MAC e estado da conexão, além de permitir sua transferência entre componentes
 **           Android por meio da interface Parcelable.
 **=================================================================================================
 */

package com.eduardo.obdmonitor.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.eduardo.obdmonitor.R;

public class Device implements Parcelable {
    private static final int DEVICE_STATUS_DISCONNECTED = R.string.disconnected;
    private static final int DEVICE_STATUS_CONNECTING = R.string.connecting;
    private static final int DEVICE_STATUS_CONNECTED = R.string.connected;

    private String name;
    private String macAddress;
    private int status;

    @SuppressWarnings("unused")
    public Device() {
    }

    public Device(String name, String macAddress) {
        this.name = name;
        this.macAddress = macAddress;
        this.status = DEVICE_STATUS_DISCONNECTED;
    }

    protected Device(Parcel in) {
        name = in.readString();
        status = DEVICE_STATUS_DISCONNECTED;
    }

    public static final Creator<Device> CREATOR = new Creator<>() {
        @Override
        public Device createFromParcel(Parcel in) {
            return new Device(in);
        }

        @Override
        public Device[] newArray(int size) {
            return new Device[size];
        }
    };

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMacAddress() {
        return macAddress;
    }

    @SuppressWarnings("unused")
    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public int getStatus() {
        return status;
    }

    public void setStatusDisconnected() {
        status = DEVICE_STATUS_DISCONNECTED;
    }

    public void setStatusConnecting() {
        status = DEVICE_STATUS_CONNECTING;
    }

    public void setStatusConnected() {
        status = DEVICE_STATUS_CONNECTED;
    }

    @NonNull
    @Override
    public String toString() {
        return "Device{" +
                "name='" + name + '\'' +
                ", macAddress='" + macAddress + '\'' +
                ", status='" + status + '\'' +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(macAddress);
        dest.writeInt(status);
    }
}
