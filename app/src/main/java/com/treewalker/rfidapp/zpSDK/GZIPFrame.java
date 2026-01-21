package com.treewalker.rfidapp.zpSDK;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.CRC32;
import java.util.zip.GZIPOutputStream;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;


public class GZIPFrame {
	private  static Bitmap myBitmap = null;
	private  static Canvas myCanvas = null;
	private  static Paint myPaint = null;
	private static int myBitmapHeight = 0;
	private static int myBitmapWidth = 0;
	//private static int PrinterDotWidth = 768;
	private static int PrinterDotWidth = 832;
	private static int PrinterDotPerMM = 8;

    public static byte[] codec(byte []data)
    {
        byte[] gzipData=null;
        ByteArrayOutputStream gzipStram = new ByteArrayOutputStream();
        try {
            GZIPOutputStream zos = new GZIPOutputStream(new BufferedOutputStream(gzipStram));
            zos.write(data);
            zos.close();
        }catch (IOException e) {e.printStackTrace();}
        gzipData=gzipStram.toByteArray();
        long length=gzipData.length;
        CRC32 crc32 = new CRC32();
        crc32.update(gzipData,8,(int)(length-8-4));
        long crc=crc32.getValue();

        gzipData[4]=(byte)((length>>0)&0xFF);
        gzipData[5]=(byte)((length>>8)&0xFF);
        gzipData[6]=(byte)((length>>16)&0xFF);
        gzipData[7]=(byte)((length>>24)&0xFF);
        gzipData[gzipData.length-4]=(byte)((crc>>0)&0xFF);
        gzipData[gzipData.length-3]=(byte)((crc>>8)&0xFF);
        gzipData[gzipData.length-2]=(byte)((crc>>16)&0xFF);
        gzipData[gzipData.length-1]=(byte)((crc>>24)&0xFF);

/*
        String outFilename= Environment.getExternalStorageDirectory().getPath()+"/1688/"+System.currentTimeMillis()+".gz";
        try
        {
            FileOutputStream out = new FileOutputStream(outFilename);
            out.write(gzipData);
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
*/
        return gzipData;
    }
    
    public static byte[] Draw_Page_Bitmap_(Bitmap bmp) {
		   //  matrix.postRotate(Rotate);  
		   int w=bmp.getWidth();
		   int h=bmp.getHeight();
		   myBitmapWidth=w;
		   myBitmapHeight=h;		   
		   myBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565);
		   myCanvas = new Canvas(myBitmap);
		   myPaint = new Paint();
		   myPaint.setColor(Color.BLACK);
		   myPaint.setTextSize(10);
		   myPaint.setTextAlign(Align.LEFT);
		   myPaint.setStrokeWidth(10);
		   myCanvas.drawColor(Color.WHITE);
		   /*  myCanvas.drawBitmap(dstbmp,xx,yy, myPaint);*/
		   myCanvas.drawBitmap(bmp,0,0, myPaint);
		   //-------------------------
		   int len;
		   if(myBitmapWidth>PrinterDotWidth)myBitmapWidth=PrinterDotWidth;
		   len = (myBitmapWidth+7) / 8;
			    byte[] bitmapdata = new byte[(len+4)*myBitmapHeight];
			int ndata=0;
			int i = 0;
			int j = 0;
			int[] RowData = new int[myBitmapWidth * myBitmapHeight];
			myBitmap.getPixels(RowData, 0, myBitmapWidth, 0, 0, myBitmapWidth, myBitmapHeight);
	
	    	for(i = 0; i < myBitmapHeight;i++)
			{
	    		bitmapdata[ndata+0] = 0x1F;
	    		bitmapdata[ndata+1] = 0x10;
	    		bitmapdata[ndata+2] = (byte)(len%256);
	    		bitmapdata[ndata+3] = (byte)(len/256);
				for(j = 0; j < len; j++)
				{
					bitmapdata[ndata+4 + j] = 0;
				}
				for(j = 0; j < myBitmapWidth; j++)
				{
					int color = RowData[i * myBitmapWidth + j];
					int b = (color>>0)&0xff;
					int g = (color>>8)&0xff;
					int r = (color>>16)&0xff;
					int grey = (r+g+b)/3;
					//if( grey <12 )
				     if( grey <153 )
				    	 bitmapdata[ndata+4 + j/8] |= (byte)(0x80 >> (j%8));
				}
				if(1==1)	// ������ÿ��ĩβ��0x00
				{
					int size;
					for(size=len-1;size>=0;size--)if(bitmapdata[ndata+4+size]!=0)break;
					size = size+1;
					size=len;
					bitmapdata[ndata+2] = (byte)(size%256);
					bitmapdata[ndata+3] = (byte)(size/256);
					ndata+=4+size;
				}
			
			}  	
	    	bitmapdata= codec(bitmapdata);
	    	
	    	
		 return bitmapdata;
	}
}
