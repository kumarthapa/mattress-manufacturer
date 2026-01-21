package com.treewalker.rfidapp.zpSDK;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

import java.util.List;


public class zpPrinter
{
  private Context context;
  static int w;
  static int h;
  String TAG = "zpSDK";

  _PrinterPageImpl impl=new _PrinterPageImpl();

/*************CPCL指令接口********************CPCL指令接口*************************CPCL指令接口*****************************/

public void drawDATAMATRIX(int x,int y,int h,String str)
{
   impl.drawDATAMATRIX(x,y,h,str);
}

  /**执行打印操作
   * @param skip           走纸方式
   * 0：不走纸；1：设置标签检测；2：设置左黑标检测指令；3：设置右黑标检测指令；
   * */
  public String print(int horizontal, int skip) {
    impl.print(skip);
     return impl.getPrintStr();
  }

  public byte[] printByte(int horizontal, int skip) {
    impl.print(skip);
     return impl.getPrintByte();
  }


  /**设置打印次数
   * @param   time         次数
   * 设置打印次数，默认为 1 次
   * */

  public void setPrintTime(int time){impl.setPrintTime(time);}

  public void noPrint(int horizontal, int skip) {
    impl.print(skip);
  }

  public void noPrint() {
    impl.print();
  }

  /**执行打印操作*/
  public String print() {
    impl.print();
    return impl.getPrintStr();
  }

  /**打印填入的CPCL指令*/
  public void printPrintStr()
  {
    Log.e(TAG, impl.getPrintStr() );
  }

  public String getprintPrintStr()
  {
    return  impl.getPrintStr();
  }

  /**设置页面宽高*/
  public void pageSetup(int pageWidth, int pageHeight) {
    w = pageWidth;
    h = pageHeight;
    impl.Create(pageWidth, pageHeight);
  }
  /**设置页面宽高*
   * @param pageWidth     打印纸宽度
   * @param pageHeight    打印纸高度
   * @param r             旋转180度打印 0：不旋转；1：旋转90；2：旋转180；3：旋转270；
   * @param gap           走纸方式
   * 0：不走纸；1：设置标签检测；2：设置左黑标检测指令；3：设置右黑标检测指令；
   */

  public void pageSetup(int pageWidth, int pageHeight,int r,int gap) {
    w = pageWidth;
    h = pageHeight;
    impl.Create(pageWidth, pageHeight,r,gap);
  }




  /**
   * 打印矩形框
   *
   * @param top_left_x     矩形框左上角 x 坐标
   * @param top_left_y     矩形框左上角 y 坐标
   * @param bottom_right_x 矩形框右下角 x 坐标
   * @param bottom_right_y 矩形框右下角 y 坐标
   * @param lineWidth      边框线条宽度
   */
  public void drawBox(int lineWidth, int top_left_x, int top_left_y, int bottom_right_x, int bottom_right_y) {
    impl.Drawbox(top_left_x, top_left_y, bottom_right_x, bottom_right_y,
            lineWidth);
  }

  /**
   * 打印实线
   *
   * @param start_x        线条起始点 x 坐标
   * @param start_y        线条起始点 y 坐标
   * @param end_x        线条结束点 x 坐标
   * @param end_y        线条结束点 y 坐标
   * @param lineWidth 线条宽度
   */
  public void drawLine(int lineWidth, int start_x, int start_y, int end_x, int end_y, boolean fullline) {
    impl.DrawLine(start_x, start_y, end_x, end_y, lineWidth);
  }

  /**
   * 打印文字
   *
   * @param text          要打印的文本内容
   * @param text_x        文字起始横坐标
   * @param text_y        文字起始纵坐标
   * @param fontSize      fontSize - 字体大小
   * @param rotate       旋转角度(逆时针) 支持 0; 90; 180 ;270;
   * @param bold         打印粗细
   * @param reverse      反向打印
   * @param underline    下划线
   */
  public void drawText(int text_x, int text_y, String text, int fontSize, int rotate, int bold, boolean reverse, boolean underline) {
    impl.DrawText(text_x, text_y, text, fontSize, rotate, bold, reverse, underline);
  }

  /**
   * 打印文字
   *
   * @param text          要打印的文本内容
   * @param text_x        文字起始横坐标
   * @param text_y        文字起始纵坐标
   * @param fontType      fontType - 字体类型（int）
   * @param fontSize      fontSize - 字体大小
   * @param rotate       旋转角度(逆时针) 支持 0; 90; 180 ;270;
   * @param bold         打印粗细
   * @param reverse      反向打印
   * @param underline    下划线
   */

  public void drawText(int text_x, int text_y, String text, int fontType,int fontSize, int rotate, int bold, boolean reverse, boolean underline){
    impl.DrawText(text_x, text_y, text, fontType,fontSize, rotate, bold, reverse, underline);
}

  /**
   * 打印文字
   *
   * @param text          要打印的文本内容
   * @param text_x        文字起始横坐标
   * @param text_y        文字起始纵坐标
   * @param fontType      fontType - 字体类型(string)
   * @param fontSize      fontSize - 字体大小
   * @param rotate       旋转角度(逆时针) 支持 0; 90; 180 ;270;
   * @param bold         打印粗细
   * @param reverse      反向打印
   * @param underline    下划线
   */

  public void drawText(int text_x, int text_y, String text, String fontType,int fontSize, int rotate, int bold, boolean reverse, boolean underline){
    impl.DrawText(text_x, text_y, text, fontType,fontSize, rotate, bold, reverse, underline);
  }

  /**
   * 打印条形码
   *
   * @param text      条形码的内容
   * @param start_x   条形码起始横坐标
   * @param start_y   条形码起始纵坐标
   * @param height    条形码高度
   * @param linewidth 条形码线宽度
   * @param type      条形码类型  0:CODE39; 1:CODE128;2:CODE93; 3:CODEBAR; 4:EAN8; 5:EAN13; 6:UPCA; 7:UPC-E; 8:I2OF5
   * @param rotate  旋转角度 支持 0:水平条码；1:竖直条码
   */
  public void drawBarCode(int start_x, int start_y, String text, int type, int rotate, int linewidth, int height) {
    String type_ = "128";
    if (type == 0)
      type_ = "39";
    if (type == 1)
      type_ = "128";
    if (type == 2)
      type_ = "93";
    if (type == 3)
      type_ = "CODABAR";
    if (type == 4)
      type_ = "EAN8";
    if (type == 5)
      type_ = "EAN13";
    if (type == 6)
      type_ = "UPCA";
    if (type == 7)
      type_ = "UPCE";
    if (type == 8) {
      type_ = "I2OF5";
    }

    impl.DrawBarcode1D(type_,start_x, start_y, text, linewidth, height, rotate);

  }

  /**
   * 打印二维码
   *
   * @param text     二维码内容
   * @param start_x  二维码起始横坐标
   * @param start_y  二维码起始纵坐标
   * @param ver      二维码放大倍数，值的范围是1-32，默认填6
   * @param lel     二维码版本型号，值的范围是1-40，越大标识尺寸越大，尺寸17+n*4，0表示自动
   * @param rotate  旋转角度
   */
  public void drawQrCode(int start_x, int start_y, String text, int rotate, int ver, int lel) { impl.DrawBarcodeQRcode(start_x, start_y, text, ver,"M",false,lel); }


  /**
   * 打印图片
   *
   * @param start_x   位图开始的X坐标
   * @param start_y  位图开始的Y坐标
   * @param bmp_size_x  位图高
   * @param bmp_size_y 位图宽
   * @param bmp       位图
   */
  //public void drawGraphic(int start_x, int start_y, int bmp_size_x, int bmp_size_y, Bitmap bmp) { impl.DrawBitmap(bmp,start_x, start_y,false); }
  public void drawGraphic(int start_x, int start_y, int bmp_size_x, int bmp_size_y, Bitmap bmp) { impl.DrawBitmapCG(bmp,start_x, start_y,false); }

  public void drawImageFormFile(int start_x, int start_y, String filepath) {

    Bitmap bmp=null;
    FileInputStream fis = null;
    try {
      fis = new FileInputStream(filepath);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      return;
    }
  
    bmp  = BitmapFactory.decodeStream(fis);
    impl.DrawBitmap(bmp,start_x, start_y,false);
  }

  /**反白线段*/
  public void drawINVERSE(int x0, int y0, int x1, int y1, int width) { impl.INVERSE(x0, y0, x1, y1, width); }
  /**打印前走纸
   * {length}:	走纸距离点数。8点=1mm
   */
  public void prefeed(int len) { impl.prefeed(len); }
  /**打印后走纸
   * {length}:	走纸距离点数。8点=1mm
   */
  public void postfeed(int len) { impl.postfeed(len); }
  /**左对齐*/
  public void alignLeft() { impl.alignLeft(); }
  /**右对齐*/
  public void alignRight() { impl.alignRight(); }
  /**中心对齐*/
  public void alignCenter() { impl.alignCenter(); }
  /**结束指令*/
  public void end() { impl.end(); }
  /**忽略指令*/
  public void about() { impl.about(); }
  /**数字自动增减;
   * ±65535内的任意整数*/
  public void count(int mun) { impl.count(mun); }
  /**字符放大*/
  public void setMag(int w, int h) { impl.setMag(w,h); }
  /**字符间距
   * 字符与字符之间的间隔大小，spacing*0.125mm
   */
  public void setSP(int spacing){impl.setSP(spacing);}

  /**批量打印
   * 按下走纸键打印第二张，再按下打印第三张
   */
  public void setPace() {
    impl.SetPace();
  }

  /**设置对比度指令
   {level}: 对比度等级
   0 = 默认
   1 = 中等
   2 = 黑
   3 = 非常黑
   设置打印对比度。对比度越高，打印越黑，打印速度越慢
   */
  public void contRast(int level) { impl.contRast(level); }
  /**打印速度
   * {speed level}: 0-5的数值，0是最慢的速度
   */
  public void speed(int level) { impl.speed(level); }
  /**设置字体为粗体
   * {level} = 0	取消粗体模式
   * {level} > 1    设置打印字体为粗体
   */
  public void setBold(int level) { impl.setBold(level); }
  /**延时打印
   * 延时1/8的时间*/
  public void printWait(int time) { impl.printWait(time); }

  /**
   * 条码识别符
   *
   * @param font    识别符字体编号
   * @param size    识别符字号
   * @param offset  离条码远近的偏移点数	8点=1mm
   * @param rotate  旋转角度 支持 0:水平条码；1:竖直条码
   * @param width   窄条码的宽度点数,默认1
   * @param ratio   宽条码和窄条码的比率,默认1
   * @param height  条码高度点数（8点/mm）
   * @param x       条码开始的X轴坐标
   * @param y       条码开始的Y轴坐标
   * @param data    条码数据
   */
  public void barcodeText(int font, int size, int offset, int rotate, int width, int ratio, int height, int x, int y, String data) {
    impl.barcodeText(font, size, offset, rotate, width, ratio, height, x, y, data);
  }

  /**
   * 文字关联
   *
   * @param x        X轴 开始坐标
   * @param y        Y轴 开始坐标
   * @param concat  离条码远近的偏移点数	8点=1mm
   * @param rotate     旋转角度 支持 0:水平条码；1:竖直条码
  //   *    class CONCAT
  //   *   {
  //   *    @param str         要打印的文本内容
  //   *    @param font        字体的名字或者编号
  //   *    @param size        字体字号
  //   *    @param offset      开始坐标的偏移点数 8点=1mm
  //   *    public CONCAT(int font,int size,int offset,String str)
  //   *   }
   */
  public void textConcatenation(int x, int y, List<_PrinterPageImpl.CONCAT> concat, int rotate) {
    impl.textConcatenation(x, y, concat, rotate);
  }

  /**
   * 多行打印
   *
   * @param height    每行文字的高度
   * @param fontSize  字体标号
   * @param fontSize  字体大小
   * @param x         X轴开始坐标
   * @param y        Y轴开始坐标
   * @param rotate   旋转角度(逆时针) 支持 0 90 180 270
   * @param strs     需要打印的文字
   */
  public void multLine(int height, int fontType,int fontSize, int x, int y, int rotate, String... strs) {
    impl.multLine(height, fontType,fontSize,x, y, rotate, strs);
  }

  /**
   * 水印文字灰度级
   * @param level     设置水印文字的灰度0-255，值越大颜色越深
   */
  public void backGround(int level) { impl.backGround(level); }

  /**
   * 打印水印
   *
   * @param font     字体名字或者编号
   * @param size     字号
   * @param x        X轴开始坐标
   * @param y        Y轴开始坐标
   * @param rotate   旋转角度(逆时针) 支持 0 90 180 270
   * @param str      需要打印的文字
   * @param lev     水印深浅0-255
   */
  public void bkText(int font, int size, int x, int y,  int lev,String str, int rotate) {
    impl.bkText(font, size, x, y, lev,str, rotate);
  }

  /**版本信息*/
  public String version() { return impl.version();  }
}
