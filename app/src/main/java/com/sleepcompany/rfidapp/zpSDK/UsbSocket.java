package com.sleepcompany.rfidapp.zpSDK;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

public class UsbSocket {

    Context context;
    String TAG="zpSDK";
    static UsbSocket mUsbSocket=null;
    private int sysVersion;
    public static UsbSocket getInstance(Context context)
    {
        if (mUsbSocket==null)
            mUsbSocket=new UsbSocket(context);
        return mUsbSocket;
    }
    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    UsbManager mUsbManager;

    UsbSocket(Context context)
    {
        this.context=context;
        sysVersion = Integer.parseInt(Build.VERSION.SDK);
//        if(sysVersion<19)
//            context.onNewIntent(context.getIntent());
        context.registerReceiver(mUsbReceiver, new IntentFilter(UsbManager.ACTION_USB_DEVICE_ATTACHED));
        context.registerReceiver(mUsbReceiver, new IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED));
    }

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver()
    {
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();
            if(UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action))
            {
                UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if(device.getVendorId()==1155&&device.getProductId()==22339)
                {
                    Log.e(TAG, "USB打印机已接入");
                    Toast.makeText(context,"USB打印机已接入", Toast.LENGTH_LONG).show();
                }
            }
            if(UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action))
            {
                UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if(device.getVendorId()==1155&&device.getProductId()==22339)
                {
                    Log.e(TAG, "USB打印机已断开");
                    Toast.makeText(context,"USB打印机已断开", Toast.LENGTH_LONG).show();
                }
            }
        }
    };

    public void ConnectPrinter()
    {
        context.registerReceiver(mUsbReceiver, new IntentFilter(UsbManager.ACTION_USB_DEVICE_ATTACHED));
        context.registerReceiver(mUsbReceiver, new IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED));
    }

   public String Write(byte[] prnData)
    {
        UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
        while(deviceIterator.hasNext())
        {
            //Log.d("zpSDK","USB deviceIterator");
            UsbDevice usbDevice = deviceIterator.next();
            //Log.d("zpSDK",String.valueOf(usbDevice.getVendorId())+","+String.valueOf(usbDevice.getProductId()));
            if(usbDevice.getVendorId()!=1155||usbDevice.getProductId()!=22339)continue;
            if(writePort(usbManager,usbDevice,prnData))
            {
                Log.e(TAG, "USB打印成功");

                //Looper.prepare();
                Toast.makeText(context,"USB打印成功", Toast.LENGTH_LONG).show();
                //Looper.loop();
                return "USB打印成功";
            }
            else
            {
                Log.e(TAG, "USB打印失败");
               // Looper.prepare();
                Toast.makeText(context,"USB打印失败", Toast.LENGTH_LONG).show();
               // Looper.loop();
                return "USB打印失败";
            }
        }
        Log.e(TAG, "无法连接USB打印机");
       // Looper.prepare();
        Toast.makeText(context,"无法连接USB打印机", Toast.LENGTH_LONG).show();
       // Looper.loop();
        return "无法连接USB打印机";
    }

    boolean writePort(UsbManager manager,UsbDevice usbDev, byte[] data)
    {
        boolean success=false;
        UsbEndpoint end_in = null;
        UsbEndpoint end_out = null;
        byte[] buffer = new byte[64];
        try {
            UsbInterface interf = usbDev.getInterface(0);
            UsbDeviceConnection connection = manager.openDevice(usbDev);//连接usb设备
            if (connection == null) {
                Log.d(TAG, "mUsbDeviceConnection can't be null");
                return false;
            }
            if (connection.claimInterface(interf, true)) {
                //Log.d(TAG,"claimInterface success");
                int endpointCount = interf.getEndpointCount();
                for (int i = 0; i < endpointCount; i++) {
                    UsbEndpoint endpoint = interf.getEndpoint(i);
                    if (endpoint.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                        if (UsbConstants.USB_DIR_IN == endpoint.getDirection()) {
                            end_in = endpoint;//获取读数据通道
                        } else if(UsbConstants.USB_DIR_OUT == endpoint.getDirection()){
                            end_out = endpoint;//获取写数据通道
                        }
                    }
                }
            } else {
                connection.close();
                Log.d(TAG,"claimInterface fail");
                return  false;
            }

            if (end_out != null)
            {
                // bulkTransfer通过给定的endpoint来进行大量的数据传输，传输的方向取决于该节点的方向，
                // 传输成果返回传输字节数组的长度，失败返回负数
                int send_ret = connection.bulkTransfer(end_out, buffer,buffer.length,0 );
                send_ret=0;
                int total_length=data.length;
                //API28之前的系统，bulkTransfer最大包不能超过16384，所以要拆包
                int pack = data.length / 16384;
                int j = 0;
                for (j = 0; j < pack; j++) {
                    byte[] newBuffer = Arrays.copyOfRange(data, j * 16384, 16384 + j * 16384);
                    // 参数依次为：下行端点，字节数组消息，消息长度，响应时间
                    send_ret += connection.bulkTransfer(end_out, newBuffer, newBuffer.length, 0);
                }
                if(send_ret<total_length){
                    byte[] newBuffer = Arrays.copyOfRange(data, j * 16384, total_length);
                    send_ret += connection.bulkTransfer(end_out, newBuffer, newBuffer.length, 0);
                }
                Log.d(TAG,"send Data:"+","+String.valueOf(send_ret));
                if (send_ret >= data.length)success=true;
                if (connection != null)
                {
                    connection.releaseInterface(interf);
                    connection.close();
                }
                interf = null;
                connection = null;
            }
        } catch (Exception e)
        {
            e.printStackTrace();
        }
        return success;
    }


 /*   private Thread threadWriteData = new Thread(new Runnable()
    {
        String message = "";
        @Override
        public void run()
        {
            while (connect) //USB处于连接状态就循环执行
            {
                String temMes = readData(); //获取到接收到的字符串
                if (temMes != null)
                {
                    //如果接收到的数据不为空，就一直拼接，因为这些可能属于同一组数据（除非USB设备发送频率小于我们设置的超时时间100毫秒）
                    message = message+temMes;
                    continue;
                }
                else
                {
                    //接收到的数据为空了，表示该组数据接收完整了，就可以发送给消息处理中心进行处理了
                    if (!message.equals("")) //接收到的数据要不为空，不然没意义
                    {
                        mes = new Message();
                        mes.obj = message;
                        mes.what = MyHandler.INPUT;
                        myHandler.sendMessage(mes);
                        message = "";
                    }
                }
            }
        }
    });

*/
    public  byte[] Read(int Timeout)
    {
//        UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
//        UsbDeviceConnection connection = manager.openDevice(usbDev);
        return null;



    }

}
