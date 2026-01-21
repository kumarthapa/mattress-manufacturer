package com.treewalker.rfidapp.zpSDK;


import android.graphics.Bitmap;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.nio.IntBuffer;
import java.util.List;


public class _PrinterPageImpl
{
  private String printStr = "";  //add printer cmd string
  private byte[] printByte = new byte[1024*1024*2];  //add printer cmd byte[]
  private int printByteLen = 0;
  private String LastprintStr = "";  //last cmd string 
  private byte[] LastprintByte = new byte[1024*1024*2];
  private int LastprintByteLen = 0;
  private int PrintTime = 1;
  private int mHeight ;
  private int mWidth;
  public void Create(int width, int height) {
    printStr = "";printByteLen = 0;
    mHeight = height;
    mWidth = width;
    
  }

  public void setPrintTime(int time){
    PrintTime=time;
  }

  void add(String str)
  {
    printStr+=str;
  }

  void addByte(byte[] buffer)
  {    
    System.arraycopy(buffer, 0, printByte, printByteLen,  buffer.length);
    printByteLen += buffer.length;
  }

  public void Create(int width, int height,int r,int gap) {

    printStr = "";printByteLen = 0;
    mHeight = height;
    mWidth= width;
    switch (r){
      case 0:
        add("ZPROTATE\r\n");
        break;
      case 1:
          add("ZPROTATE90\r\n");
        break;
      case 2:
          add("ZPROTATE180\r\n");
        break;
      case 3:
          add("ZPROTATE270\r\n");
        break;
      default:
        break;
    }
    if (gap==1)
        add("GAP-SENSE\r\n");
    else if (gap== 2)
        add("BAR-SENSE LEFT\r\n");
    else if (gap== 3)
        add("BAR-SENSE\r\n");

  }

  public String getPrintStr()
  {
    return LastprintStr;
  }

  public byte[] getPrintByte()
  {
    byte[] buffer = new byte[LastprintByteLen];
    System.arraycopy(LastprintByte, 0, buffer, 0, LastprintByteLen);
    return buffer;
  }

  public void prefeed(int len)
  {
    add(String.format("PREFEED %d\r\n", new Object[] {Integer.valueOf(len)}));
  }
  public void postfeed(int len)
  {
    add(String.format("POSTFEED %d\r\n", new Object[] {Integer.valueOf(len)})) ;
  }


  public void INVERSE(int x0, int y0, int x1, int y1, int width) {

    String str = String.format("INVERSE-LINE %d %d %d %d %d\r\n", x0, y0, x1, y1, width);
    add(str);

  }

  public void DrawBitmap(Bitmap bmp, int x, int y, boolean rotate) {

    int w = bmp.getWidth();
    int h = bmp.getHeight();
    int byteCountW = (w + 7) / 8;
    int[] bmpData = new int[w * h];
    byte[] outData = new byte[byteCountW * h];
    bmp.copyPixelsToBuffer(IntBuffer.wrap(bmpData));

    int i1;
    for(int yy = 0; yy < h; ++yy) {
      for(int xx = 0; xx < w; ++xx) {
        int c = bmpData[yy * w + xx];
        i1 = c >> 16 & 255;
        int g = c >> 8 & 255;
        int b = c & 255;
        int gray = (i1 * 30 + g * 59 + b * 11 + 50) / 100;
        if(gray < 128) {
          outData[byteCountW * yy + xx / 8] = (byte)(outData[byteCountW * yy + xx / 8] | 128 >> xx % 8);
        }
      }
    }
    String cmd = "EG";
    if(rotate) {
      cmd = "VEG";
    }

    String strCmdHeader = String.format("%s %d %d %d %d ", new Object[]{cmd, Integer.valueOf(byteCountW), Integer.valueOf(h), Integer.valueOf(x), Integer.valueOf(y)});
    String strData = "";

    for( i1 = 0; i1 < outData.length; ++i1) {
      strData = strData + this.ByteToString(outData[i1]);
    }

    add(strCmdHeader + strData + "000\r\n");
  }



  public void DrawBitmapCG(Bitmap bmp, int x, int y, boolean rotate) {

    int w = bmp.getWidth();
    int h = bmp.getHeight();
    int byteCountW = (w + 7) / 8;
    int[] bmpData = new int[w * h];
    byte[] outData = new byte[byteCountW * h];
    bmp.copyPixelsToBuffer(IntBuffer.wrap(bmpData));

    int i1;
    for(int yy = 0; yy < h; ++yy) {
      for(int xx = 0; xx < w; ++xx) {
        int c = bmpData[yy * w + xx];
        i1 = c >> 16 & 255;
        int g = c >> 8 & 255;
        int b = c & 255;
        int gray = (i1 * 30 + g * 59 + b * 11 + 50) / 100;
        if(gray < 128) {
          outData[byteCountW * yy + xx / 8] = (byte)(outData[byteCountW * yy + xx / 8] | 128 >> xx % 8);
        }
      }
    }
    String cmd = "CG";
    if(rotate) {
      cmd = "VCG";
    }
    String strCmdHeader = String.format("%s %d %d %d %d ", new Object[]{cmd, Integer.valueOf(byteCountW), Integer.valueOf(h), Integer.valueOf(x), Integer.valueOf(y)});    
    addByte(strCmdHeader.getBytes());
    addByte(outData);
  }

  public void barcodeText(int font, int size, int offset, int rotate, int width, int ratio, int height, int x, int y, String data) {

    String cm = String.format("BARCODE 128 %d %d %d %d %d %s\r\n", width, ratio, height,x,y,data);
    if (rotate==1)
      cm = String.format("VBARCODE 128 %d %d %d %d %d %s\r\n", width, ratio, height,x,y,data);

    String str = String.format("BARCODE-TEXT %d %d %d\r\n", font, size, offset);
    add(str);
    add(cm);
    add("BARCODE-TEXT OFF\r\n");

  }



  public void DrawText(int text_x, int text_y, String text, int fontSize, int rotate, int bold, boolean reverse, boolean underline) {

    if (bold!=0)
    {
      setBold(bold);
    }
    if (underline)
    {
      underLine(true);
    }

    String cmd = "T";
    if (rotate == 90) cmd = "VT";
    if (rotate == 180) cmd = "T180";
    if (rotate == 270) cmd = "T270";

    int f_size = 24;
    int f_height = 24;

    if (fontSize == 1) {
      f_size = 55;
      f_height = 16;
    }

    if (fontSize == 2) {
      f_size = 24;
      f_height = 24;
    }

    if (fontSize == 3) {
      f_size = 56;
      f_height = 32;
    }

    if (fontSize == 4) {
      f_size = 24;
      setMag(2,2);
      f_height = 48;
    }

    if (fontSize == 5) {
      f_size = 56;
      setMag(2,2);
      f_height = 64;
    }

    if (fontSize == 6) {
      f_size = 24;
      setMag(3,3);
      f_height = 72;
    }

    if (fontSize == 7) {
      f_size = 32;
      setMag(4,4);
      f_height = 96;
    }
    if (fontSize == 8) {
      f_size = 24;
      setMag(5,5);
      f_height = 120;
    }
    if (fontSize == 9) {
      f_size = 32;
      setMag(6,6);
      f_height = 192;
    }
    if (fontSize == 10) {
      f_size = 24;
      setMag(7,7);
      f_height = 168;
    }
    if (fontSize == 11) {
      f_size = 32;
      setMag(8,8);
      f_height = 256;
    }
    if (fontSize == 12) {
      f_size = 24;
      setMag(9,9);
      f_height = 216;
    }


    String temp = String.format("%s %d %d %d %d %s\r\n", new Object[]{cmd, Integer.valueOf(f_size), Integer.valueOf(0), Integer.valueOf(text_x), Integer.valueOf(text_y), text});
    add(temp);
    if (reverse) {

      byte[] bytetext = (byte[]) null;
      try {
        bytetext = text.getBytes("gbk");
      } catch (UnsupportedEncodingException e) {
        return;
      }
      if (bytetext == null)
        return;
      int block_h = f_height;
      int block_w = f_height / 2 * bytetext.length;

      INVERSE(text_x, text_y, text_x + block_w, text_y, f_height);

    }
    if (underline)
    {
      underLine(false);
    }
    setMag(0,0);
    if (bold!=0)
    {
      setBold(0);
    }
//    int f_name = 1;
//    int f_size = 0;
//    int f_height = 1;
//    if (mBold!=bold)
//    {
//      mBold=bold;
//      setBold(mBold);
//    }
//    if (mUnderLine!=underline)
//    {
//      mUnderLine=underline;
//      underLine(mUnderLine);
//    }
//
//    String textScale = "";
//    boolean f_width;
//    if (fontSize == 1) {
//      f_name = 55;
//      f_width = true;
//      f_height = 16;
//      f_size = 0;
//    } else if (fontSize == 2) {
//      f_name = 20;
//      f_width = true;
//      f_height = 20;
//      f_size = 0;
//    } else if (fontSize == 3) {
//      f_name = 24;
//      f_width = true;
//      f_height = 24;
//      f_size = 0;
//    } else if (fontSize == 4) {
//      f_name = 28;
//      f_width = true;
//      f_height = 28;
//      f_size = 0;
//    } else if (fontSize == 5) {
//      f_name = 56;
//      f_width = true;
//      f_height = 32;
//      f_size = 0;
//    } else if (fontSize == 6) {
//      f_name = 24;
//      f_width = true;
//      f_height = 48;
//      f_size = 0;
//      setMag(2,2);
//    } else if (fontSize == 7) {
//      f_name = 56;
//      f_width = true;
//      f_height = 64;
//      f_size = 0;
//      setMag(2,2);
//    } else if (fontSize == 8) {
//      f_name = 56;
//      f_width = true;
//      f_height = 96;
//      f_size = 0;
//      setMag(3,3);
//    } else {
//      f_name = 24;
//      f_width = true;
//      f_height = 24;
//      f_size = 0;
//      setMag(0,0);
//    }
//
//    String cmd = "T";
//    if (rotate != 0) {
//      if (rotate == 1) {
//        cmd = "VT";
//      } else if (rotate == 2) {
//        cmd = "T180";
//      } else if (rotate == 3) {
//        cmd = "T270";
//      }
//    }
//
//
//    String str = String.format("%s %s %s %d %d %s\r\n", cmd, Integer.valueOf(f_name), Integer.valueOf(f_size), text_x, text_y, text);
//    printStr+=str;
//
//
//    if (reverse) {
//      Object var16 = null;
//
//      byte[] bytetext;
//      try {
//        bytetext = text.getBytes("gbk");
//      } catch (UnsupportedEncodingException var19) {
//        return;
//      }
//
//      if (bytetext == null) {
//        return;
//      }
//
//      int block_w = f_height / 2 * bytetext.length;
//      this.INVERSE(text_x, text_y, text_x + block_w, text_y, f_height);
//    }
//
//   if (underline)
//      underLine(false);
//
//    if (bold!=0) {
//      setBold(0);
//    }
//
//
//      setMag(0,0);





  }


  public void SetPace()
  {
    add("PACE\r\n");
  }


  public void DrawText(int text_x, int text_y, String text, int fontType,int fontSize, int rotate, int bold, boolean reverse, boolean underline) {

    if (underline)
    {
      underLine(true);
    }
    if (bold!=0)
    {
      setBold(bold);
    }

    String cmd = "T";
    if (rotate == 90) cmd = "VT";
    if (rotate == 180) cmd = "T180";
    if (rotate == 270) cmd = "T270";


    int f_height = 24*fontSize;
    if (f_height==0)
      f_height=24;


    String temp = String.format("%s %d %d %d %d %s\r\n", new Object[]{cmd, Integer.valueOf(fontType), Integer.valueOf(fontSize), Integer.valueOf(text_x), Integer.valueOf(text_y), text});
    add(temp);
    if (reverse) {

      byte[] bytetext = (byte[]) null;
      try {
        bytetext = text.getBytes("gbk");
      } catch (UnsupportedEncodingException e) {
        return;
      }
      if (bytetext == null)
        return;
      int block_h = f_height;
      int block_w = f_height / 2 * bytetext.length;

      INVERSE(text_x, text_y, text_x + block_w, text_y, f_height);
    }
  }

  public void DrawText(int text_x, int text_y, String text, String fontType,int fontSize, int rotate, int bold, boolean reverse, boolean underline) {

    if (underline)
    {
      underLine(true);
    }
    if (bold!=0)
    {
      setBold(bold);
    }

    String cmd = "T";
    if (rotate == 90) cmd = "VT";
    if (rotate == 180) cmd = "T180";
    if (rotate == 270) cmd = "T270";


    int f_height = 24*fontSize;
    if (f_height==0)
      f_height=24;

   // setMag(fontSize,fontSize);

    String temp = String.format("%s %s %d %d %d %s\r\n", new Object[]{cmd, fontType, Integer.valueOf(fontSize), Integer.valueOf(text_x), Integer.valueOf(text_y), text});
    add(temp);
    if (reverse) {

      byte[] bytetext = (byte[]) null;
      try {
        bytetext = text.getBytes("gbk");
      } catch (UnsupportedEncodingException e) {
        return;
      }
      if (bytetext == null)
        return;
      int block_h = f_height;
      int block_w = f_height / 2 * bytetext.length;

      INVERSE(text_x, text_y, text_x + block_w, text_y, f_height);


    }
    if (underline)
    {
      underLine(false);
    }
    setMag(0,0);
    if (bold!=0)
    {
      setBold(0);
    }
  }

  public void count(int mun) {
    String str = String.format("COUNT %d\r\n", new Object[] {Integer.valueOf(mun)});
  add(str);
  }

  public void setMag(int w, int h) {
    String str = String.format("SETMAG %d %d\r\n",new Object[] {Integer.valueOf(w), Integer.valueOf(h)});
    add(str);
  }

  public void Drawbox(int x0, int y0, int x1, int y1, int width) {

    String str = String.format("BOX %d %d %d %d %d\r\n", new Object[] {Integer.valueOf(x0), Integer.valueOf(y0), Integer.valueOf(x1), Integer.valueOf(y1), Integer.valueOf(width)});
    add(str);
  }

  public void DrawLine(int x0, int y0, int x1, int y1, int width) {
    String str = String.format("LINE %d %d %d %d %d\r\n", new Object[] {Integer.valueOf(x0), Integer.valueOf(y0), Integer.valueOf(x1), Integer.valueOf(y1), Integer.valueOf(width)});
    add(str);
  }


  public void DrawBarcode1D(String type, int x, int y, String text, int width, int height, int rotate) {

    String cmd = "BARCODE";
    if (rotate==1)
      cmd = "VBARCODE";
    String str = String.format("%s %s %d 1 %d %d %d %s\r\n", new Object[] { cmd, type, Integer.valueOf(width - 1), Integer.valueOf(height), Integer.valueOf(x), Integer.valueOf(y), text });

    add(str);
  }
  public void DrawBarcodeQRcode(int x, int y, String text, int size, String errLevel, boolean rotate,int len) {

    String cmd = "BARCODE";
    if (rotate)
      cmd = "VBARCODE";
    String str = String.format("%s QR %d %d M %d U %d\r\n", new Object[] { cmd, Integer.valueOf(x), Integer.valueOf(y), Integer.valueOf(len),Integer.valueOf(size) });
    add(str);
    String tm = String.format("%sA,", errLevel);
    add(tm);
    add(text);
    add("\r\nENDQR\r\n");
  }



  public void alignLeft() {
    add("LEFT\r\n");
  }
  public void alignRight() {
    add("RIGHT\r\n");
  }
  public void alignCenter() {
      add("CENTER\r\n");
  }
  public void end() {
      add("END\r\n");
  }
  public void about() {
      add("ABOUT\r\n");
  }

  public  class CONCAT
  {
    String str;
    public CONCAT(int font, int size, int offset, String str)
    {

      this.str = String.format("%d %d %d %s\r\n", new Object[] { Integer.valueOf(font), Integer.valueOf(size), Integer.valueOf(offset), str });
    }
    public String getStr()
    {
      return this.str;
    }
  }

  public void textConcatenation(int x, int y, List<CONCAT> concat, int rotate) {
    String temp = String.format("CONCAT %d %d\r\n", new Object[] {Integer.valueOf(x), Integer.valueOf(y)});
    if (rotate==1) {
      temp = String.format("VCONCAT %d %d\r\n", new Object[] {Integer.valueOf(x), Integer.valueOf(y)});
    }
   add(temp);
    for (int i=0;i<concat.size();i++)
    {
      add(concat.get(i).getStr());
    }

    add("ENDCONCAT\r\n");
  }

  public void multLine(int height, int fontTpye,int fontSize,int x, int y, int rotate, String...strs) {
    String direction = "T";
   add(String.format("ML %d\r\n", new Object[] {Integer.valueOf(height)}));
    switch(rotate)
    {
      case 0:
        direction = "T";
        break;
      case 90:
        direction = "T90";
        break;
      case 180:
        direction = "T180";
        break;
      case 270:
        direction = "T270";
        break;
      default:
        direction="T";
        break;
    }


    String temp = String.format("%s %d %d %d %d\r\n", new Object[] { direction, Integer.valueOf(fontTpye), Integer.valueOf(fontSize), Integer.valueOf(x), Integer.valueOf(y)});
   add(temp);
    for (String s:strs)
    {
      add(String.format("%s\r\n", new Object[] {s}));
    }
   add("ENDML\r\n");
  }

  public void contRast(int level) {
   add(String.format("CONTRAST %d\r\n", new Object[] {Integer.valueOf(level)}));
  }

  public void speed(int level) {
    add(String.format("SPEED %d\r\n", new Object[] {Integer.valueOf(level)}));
  }

  public void setBold(int level) {
    add(String.format("SETBOLD %d\r\n", new Object[] {Integer.valueOf(level)})) ;
  }

  public void setSP(int spacing){add(String.format("SETSP %d\r\n", new Object[] {Integer.valueOf(spacing)}));}

  public void underLine(boolean mode) {
    if (mode)
     add(String.format("UNDERLINE ON\r\n")) ;
    else
      add(String.format("UNDERLINE OFF\r\n"));
  }
  public void pace() {
    add("PACE\r\n");
  }

  public void printWait(int time) {
    add(String.format("WAIT %d\r\n", new Object[] {Integer.valueOf(time)}));
  }

  public void backGround(int level) {
   add(String.format("f %d\r\n", new Object[] {Integer.valueOf(level)}));
  }

  public void bkText(int font, int size, int x, int y, int lev,String str, int rotate) {
    String direction = "BKT";
    switch(rotate)
    {
      case 0:
        direction = "BKT";
        break;
      case 90:
        direction = "BKT90";
        break;
      case 180:
        direction = "BKT180";
        break;
      case 270:
        direction = "BKT270";
        break;
      default:
        direction="BKT";
        break;
    }

    String temp = String.format("BACKGROUND %d\r\n",new Object[]{Integer.valueOf(lev)})
            +String.format(
            "%s %d %d %d %d %s\r\n", new Object[] {direction, Integer.valueOf(font), Integer.valueOf(size), Integer.valueOf(x), Integer.valueOf(y),str}
           ) +"BACKGROUND 0\r\n";
    add(temp);

  }

  public void drawDATAMATRIX(int x,int y,int h,String str)
  {
    String begin = String.format("B DATAMATRIX %d %d H %d\r\n",new Object[]{Integer.valueOf(x),Integer.valueOf(y),Integer.valueOf(h)});

    String end="\r\nENDDATAMATRIX\r\n";

   add((begin+str+end));


  }
  public void print(int skip) {

    if(printByteLen != 0)
    {
      String str = String.format("! 0 200 200 %d %d\r\n"+
                    "PAGE-WIDTH %d\r\n",
            new Object[] { Integer.valueOf(mHeight), Integer.valueOf(PrintTime),Integer.valueOf(mWidth)});
      StringBuilder st = new StringBuilder(printStr);
      st.insert(0,str);
      LastprintStr = st.toString();

      LastprintByteLen = 0;
      byte[] tmp = LastprintStr.getBytes();
      System.arraycopy(tmp, 0, LastprintByte, 0, tmp.length);
      LastprintByteLen += tmp.length;
      System.arraycopy(printByte, 0, LastprintByte, LastprintByteLen, printByteLen);
      LastprintByteLen += printByteLen;
      String strTmp="";
      if(skip == 1)  strTmp = "GAP-SENSE\r\nFORM\r\nPRINT\r\n";
      else if(skip == 2)  strTmp = "BAR-SENSE-LEFT\r\nFORM\r\nPRINT\r\n";
      else if(skip == 3)  strTmp = "BAR-SENSE\r\nFORM\r\nPRINT\r\n";
      else  strTmp = "PRINT\r\n";
      LastprintStr += strTmp;
      
      byte[] tmplast = strTmp.getBytes();
      System.arraycopy(tmplast, 0, LastprintByte, LastprintByteLen, tmplast.length);
      LastprintByteLen += tmplast.length;
    }
    else
    {
      switch (skip)
      {
        case 0:
          break;
        case 1:
          add("GAP-SENSE\r\n");
          break;
        case 2:
        add("BAR-SENSE-LEFT\r\n");
          break;
        case 3:
          add("BAR-SENSE\r\n");
          break;
        default:
          break;
      }
      add("FORM\r\n");
      add("PRINT\r\n");
      String str = String.format("! 0 200 200 %d %d\r\n"+
                    "PAGE-WIDTH %d\r\n",
            new Object[] { Integer.valueOf(mHeight), Integer.valueOf(PrintTime),Integer.valueOf(mWidth)});
      StringBuilder st = new StringBuilder(printStr);
      st.insert(0,str);
      LastprintStr = st.toString();
    }
  }

  public void print() {
    if(printByteLen != 0)
    {
      String str = String.format("! 0 200 200 %d %d\r\n"+
                      "PAGE-WIDTH %d\r\n",
              new Object[] { Integer.valueOf(mHeight), Integer.valueOf(PrintTime),Integer.valueOf(mWidth)});
      StringBuilder st = new StringBuilder(printStr);
      st.insert(0,str);
      LastprintStr = st.toString();
      LastprintByteLen = 0;
      byte[] tmp = LastprintStr.getBytes();
      System.arraycopy(tmp, 0, LastprintByte, 0, tmp.length);
      LastprintByteLen += tmp.length;
      System.arraycopy(printByte, 0, LastprintByte, LastprintByteLen, printByteLen);
      LastprintByteLen += printByteLen;

      String strTmp="PRINT\r\n";
      LastprintStr += strTmp;      
      byte[] tmplast = strTmp.getBytes();
      System.arraycopy(tmplast, 0, LastprintByte, LastprintByteLen, tmplast.length);
      LastprintByteLen += tmplast.length;
    }
    else{
      add("FORM\r\n");
      add("PRINT\r\n");
      String str = String.format("! 0 200 200 %d %d\r\n"+
                      "PAGE-WIDTH %d\r\n",
              new Object[] { Integer.valueOf(mHeight), Integer.valueOf(PrintTime),Integer.valueOf(mWidth)});
      StringBuilder st = new StringBuilder(printStr);
      st.insert(0,str);
      LastprintStr = st.toString();
    }
  }

  private String IntToHex(byte data) {
    char ch;
    String r = "";
    switch (data)
    { case 0:
      case 1:
      case 2:
      case 3:
      case 4:
      case 5:
      case 6:
      case 7:
      case 8:
      case 9:
        ch = (char)(data + 48);
        r = Character.toString(ch);
        return r;case 10: return "A";case 11: return "B";case 12: return "C";case 13: return "D";case 14: return "E";case 15: return "F"; }  Log.d("long", "ch is error "); return r;
  }


  private String ByteToString(byte data) {
    String str = "";
    byte d1 = (byte)(data >> 4 & 0xF);
    byte d2 = (byte)(data & 0xF);
    str = String.valueOf(IntToHex(d1)) + IntToHex(d2);
    return str;
  }
  public String version(){
    return "V1.5";
  }

}



