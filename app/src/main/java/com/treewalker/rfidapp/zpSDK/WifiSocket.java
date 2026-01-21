package com.treewalker.rfidapp.zpSDK;
import android.os.Message;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Timer;
import java.util.TimerTask;

public class WifiSocket {

    public static String ErrorMessage = "No Error";
    private static Socket wifiSocket = null;
    private static OutputStream myOutStream = null;
    private static InputStream myInStream = null;
    private static WifiSocket mWifiSocket = null;
    wifiListener mwifiListener=null;
    boolean isWrite = true;
    boolean isRead = true;

    private WifiSocket(wifiListener listener) {
        mwifiListener = listener;
    }

    public static WifiSocket getInstance(wifiListener listener){
        if (mWifiSocket==null)
            mWifiSocket = new WifiSocket(listener);
        return mWifiSocket;
    }


    public  void ConnectPrinter(String ip) {
        InetAddress mInetaddr = null;
        int port = 9100;
        if (ip == null||ip.equals("")) {
        } else {
            try {
                mInetaddr = InetAddress.getByName(ip);
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
            InetAddress finalMInetaddr = mInetaddr;
            Timer timer = new Timer();
                timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        wifiSocket = new Socket(finalMInetaddr, port);

                        myOutStream = wifiSocket.getOutputStream();
                        myInStream = wifiSocket.getInputStream();
                        mwifiListener.receivedmsg("finish");
                    } catch (UnknownHostException var4) {
                        mwifiListener.receivedmsg("timeout");
                    } catch (IOException var5) {
                        mwifiListener.receivedmsg("timeout");
                    }
                }
            }, 1500);

            mwifiListener.receivedmsg("");
        }
    }


        public  boolean disconncet() {
        try {
            Thread.sleep(1000L);
        } catch (InterruptedException var6) {
        }
            mwifiListener.receivedmsg("");
            mwifiListener.receivedstatus(null);
        if (myOutStream != null) {
            try {
                myOutStream.flush();
            } catch (IOException var5) {
            }

            try {
                myOutStream.close();
            } catch (IOException var4) {
            }

            myOutStream = null;
        }

        if (myInStream != null) {
            try {
                myInStream.close();
            } catch (IOException var3) {
            }

            myInStream = null;
        }

        if (wifiSocket != null) {
            try {
                wifiSocket.close();
            } catch (IOException var2) {
            }

            wifiSocket = null;
        }

        try {
            Thread.sleep(200L);
        } catch (InterruptedException var1) {
        }

        return true;
    }

    public  void Write(byte[] Data) {

        isWrite = true;
        if (isWrite)
        {
            isWrite = false;
            new Thread(){
                @Override
                public void run()
                {
                    try {
                        myOutStream.write(Data);
                        isWrite = true;
                    } catch (IOException var2) {
                        mwifiListener.receivedmsg("发送数据失败");
                    }
                }
            }.start();
        }

    }

    public  void Write(byte[] Data, int DataLen) {

        isWrite = true;
        if (isWrite)
        {
            isWrite = false;
            new Thread(){
                @Override
                public void run()
                {
                    try {
                        myOutStream.write(Data, 0, DataLen);
                        isWrite = true;
                    } catch (IOException var2) {
                        mwifiListener.receivedmsg("发送数据失败");
                    }
                }
            }.start();
        }
    }

    public static void Flush() {
        int DataLen = 0;

        try {
            DataLen = myInStream.available();
        } catch (IOException var4) {
        }

        for(int i = 0; i < DataLen; ++i) {
            try {
                myInStream.read();
            } catch (IOException var3) {
            }
        }
    }



    public  void Read(int Timeout) {
        new Thread(){
            @Override
            public void run() {
                byte[] buffer = new byte[1024];
                int bytes;
                int len = 0;

                try {
                    len = myInStream.available();
                } catch (IOException var4) {
                }

                if (len>0)
                {
                    try {
                        myInStream.skip(myInStream.available());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                while (true)
                {
                    try {
                        bytes = myInStream.read(buffer);
                        if (bytes>0)
                        {
                            final byte[] data = new byte[bytes];
                            System.arraycopy(buffer, 0, data, 0, bytes);
                            mwifiListener.receivedstatus(data);
                            return;
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();
    }
}
