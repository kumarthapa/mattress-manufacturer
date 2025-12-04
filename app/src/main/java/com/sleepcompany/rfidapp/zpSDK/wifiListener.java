package com.sleepcompany.rfidapp.zpSDK;
public interface wifiListener {

     void receivedmsg(String msg);
     void receivedstatus(byte[] statue);
}
