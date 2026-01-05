package com.galla.rfidapp.zpSDK;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.UUID;


public class BluetoothSocket {
    public static String ErrorMessage="No Error";
    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static BluetoothAdapter myBluetoothAdapter;
    private static BluetoothDevice myDevice;
    private static android.bluetooth.BluetoothSocket mySocket = null;
    private static OutputStream myOutStream = null;
    private static InputStream myInStream = null;
    private static BluetoothSocket mBluetoothSocket = null;

    public static BluetoothSocket getInstance()
    {
        if (mBluetoothSocket==null)
            mBluetoothSocket=new BluetoothSocket();
        return mBluetoothSocket;
    }

    public  boolean ConnectPrinter(String BDAddr)
    {
        if(BDAddr.equals("")||BDAddr==null)
        {
            ErrorMessage="Address not found";
            return false;
        }
        myBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(myBluetoothAdapter==null)
        {
            ErrorMessage="Bluetooth not found";
            return false;
        }
        myDevice = myBluetoothAdapter.getRemoteDevice(BDAddr);
        if(myDevice==null)
        {
            ErrorMessage="Unable to connect to Bluetooth";
            return false;
        }
        if(!SPPOpen(myBluetoothAdapter, myDevice))
        {
            return false;
        }
        return true;
    }
    public  boolean SPPOpen(BluetoothAdapter bluetoothAdapter, BluetoothDevice btDevice)
    {
        boolean error=false;
        myBluetoothAdapter = bluetoothAdapter;
        myDevice = btDevice;

        if(!myBluetoothAdapter.isEnabled())
        {
            ErrorMessage = "Bluetooth is not enabled";
            return false;
        }
        myBluetoothAdapter.cancelDiscovery();

        try
        {
            //mySocket = myDevice.createRfcommSocketToServiceRecord(SPP_UUID);
            Method m = myDevice.getClass().getMethod("createRfcommSocket", new Class[] {int.class});
            mySocket = (android.bluetooth.BluetoothSocket) m.invoke(myDevice, 1);
        }
        catch (SecurityException e){
            mySocket = null;
            ErrorMessage = "Failed to print";
            return false;
        }
        catch (NoSuchMethodException e) {
            mySocket = null;
            ErrorMessage = "Failed to print";
            return false;
        } catch (IllegalArgumentException e) {
            mySocket = null;
            ErrorMessage = "Failed to print";
            return false;
        } catch (IllegalAccessException e) {
            mySocket = null;
            ErrorMessage = "Failed to print";
            return false;
        } catch (InvocationTargetException e) {
            mySocket = null;
            ErrorMessage = "Failed to print";
            return false;
        }

        try
        {
            mySocket.connect();
        }
        catch (IOException e2)
        {
            ErrorMessage = e2.getLocalizedMessage();//"无法连接蓝牙打印机";
            mySocket = null;
            return false;
        }

        try
        {
            myOutStream = mySocket.getOutputStream();
        }
        catch (IOException e3)
        {
            myOutStream = null;
            error = true;
            return false;
        }

        try
        {
            myInStream = mySocket.getInputStream();
        }
        catch (IOException e3)
        {
            myInStream = null;
            error = true;
            return false;
        }

        if(error)
        {
            //SPPClose();
            return false;
        }

        return true;
    }

    public boolean disconnect()
    {
        if (!SPPClose())
          return false;
        return true;
    }
    private  boolean SPPClose()
    {
        try {Thread.sleep(1000);} catch (InterruptedException e) {}
        if(myOutStream!=null)
        {
            try{myOutStream.flush();}catch (IOException e1){}
            try{myOutStream.close();}catch (IOException e){}
            myOutStream=null;
        }
        if(myInStream!=null)
        {
            try{myInStream.close();}catch(IOException e){}
            myInStream=null;
        }
        if(mySocket!=null)
        {
            try{mySocket.close();}catch (IOException e){}
            mySocket=null;
        }
        try {Thread.sleep(200);} catch (InterruptedException e) {}
        return true;
    }

    public  boolean Write(byte[] Data)
    {
        try
        {
            myOutStream.write(Data);
        }
        catch (IOException e)
        {
            ErrorMessage = "发送蓝牙数据失败";
            return false;
        }
        return true;
    }
    public  boolean Write(byte[] Data,int DataLen)
    {
        try
        {
            myOutStream.write(Data,0,DataLen);
        }
        catch (IOException e)
        {
            ErrorMessage = "发送蓝牙数据失败";
            return false;
        }
        return true;
    }
    public  void SPPFlush()
    {
        int i=0,DataLen=0;
        try
        {
            DataLen = myInStream.available();
        }
        catch (IOException e1)
        {
        }
        for(i=0;i<DataLen;i++)
        {
            try
            {
                myInStream.read();
            }
            catch (IOException e)
            {

            }
        }
    }
    public  byte[] Read(int Timeout) {
        byte[] data = null;
        float rest = (float)Timeout;
        int len = 0;
        try {
            len = myInStream.available();
            if (len>0)
            {
                myInStream.skip(myInStream.available());
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            while(rest > 0.0F) {

                if (myInStream != null) {
                    int available = myInStream.available();
                    if (available > 0) {
                        data = new byte[available];
                        myInStream.read(data);
                        return data;
                    }

                    Thread.sleep(100L);
                    rest -= 0.1F;
                }
            }

            return null;
        } catch (Exception var6) {
            return null;
        }

    }

}
