
package com.galla.rfidapp.zpSDK;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.pdf.PdfRenderer;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class printPDF {

    Context mCotext;
    ArrayList<Bitmap> mbitmaps=null;

    public printPDF(Context con)
    {
        mCotext = con;
        mbitmaps = new ArrayList<Bitmap>();
    }


    /** 打开PDF文件
     * @param pdfPath  pdf路径
     * @param maxWeight  最大宽度
     * */

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void openPDF(String pdfPath, int maxWeight)
    {
        mbitmaps = pdfToBitmap(pdfPath,maxWeight);
    }




    /** PDF成功转化为Bitmap,并返回list
     * */
    public ArrayList<Bitmap> getMbitmaps()
    {return mbitmaps;}



  /** 把PDF以PNG的格式保存到本地文件夹
    * @param savePath  保存路径
    * */
    void savePDFToImage(String savePath)
    {
        if (mbitmaps==null)
            return;
        for (int i = 0; i < mbitmaps.size(); i++) {
                try {
                File file = new File(savePath);
                if (!file.exists()) {
                    file.mkdirs();
                }
                String localFile = savePath + "/" + i + ".png";
                Log.i("test_sign", "图片全路径localFile = " + localFile);
                File f = new File(localFile);
                FileOutputStream fos = new FileOutputStream(f);
                    mbitmaps.get(i).compress(Bitmap.CompressFormat.PNG, 100, fos);
                fos.flush();
                fos.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }


    private Bitmap zoomImage(Bitmap bgimage, int newWidth) {
// 获取这个图片的宽和高
        float width = bgimage.getWidth();
        float height = bgimage.getHeight();
// 创建操作图片用的matrix对象
        Matrix matrix = new Matrix();
// 计算宽高缩放率
        double  scale = (float)newWidth / width;
// 缩放图片动作
        matrix.postScale((float)scale,(float)scale);
        Bitmap bitmap = Bitmap.createBitmap(bgimage, 0, 0, (int) width,
                (int) height, matrix, true);
        return bitmap;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private ArrayList<Bitmap> pdfToBitmap(String pdfPath, int maxWeight) {
        File pdfFile = new File(pdfPath);
        ArrayList<Bitmap> bitmaps = new ArrayList<>();
        PdfRenderer renderer = null;
        try {

             renderer = new PdfRenderer(ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY));

        } catch (Exception ex) {
            ex.printStackTrace();
        }
        if (renderer==null)
            return null;
        Bitmap bitmap;
        final int pageCount = renderer.getPageCount();
        for (int i = 0; i < pageCount; i++) {
            PdfRenderer.Page page = renderer.openPage(i);
            int width = mCotext.getResources().getDisplayMetrics().densityDpi / 72 * page.getWidth();
            int height = mCotext.getResources().getDisplayMetrics().densityDpi / 72 * page.getHeight();
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            //todo 以下三行处理图片存储到本地出现黑屏的问题，这个涉及到背景问题
            Canvas canvas = new Canvas(bitmap);
            canvas.drawColor(Color.WHITE);
            canvas.drawBitmap(bitmap, 0, 0, null);
            Rect r = new Rect(0, 0, width, height);
            page.render(bitmap, r, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
            Bitmap b=zoomImage(bitmap,maxWeight);
            bitmaps.add(b);
            // close the page
            page.close();
        }
        // close the renderer
        renderer.close();
        return bitmaps;
    }
}
