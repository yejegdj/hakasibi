package com.example.qwentouch;
import android.accessibilityservice.*;
import android.view.accessibility.AccessibilityEvent;
import android.graphics.*;
import android.hardware.HardwareBuffer;
import android.os.*;
import android.util.Base64;
import android.view.*;
import android.widget.*;
import android.content.Context;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
public class TouchService extends AccessibilityService {
  public static volatile TouchService instance;
  private volatile boolean stopped=true;
  private final Handler main=new Handler(Looper.getMainLooper());
  private View overlay;private WindowManager wm;private Thread worker;
  @Override protected void onServiceConnected(){super.onServiceConnected();instance=this;}
  @Override public void onAccessibilityEvent(AccessibilityEvent e){}
  @Override public void onInterrupt(){stop();}
  @Override public void onDestroy(){stop();instance=null;super.onDestroy();}
  public void stop(){stopped=true;if(worker!=null)worker.interrupt();main.post(this::hideOverlay);}
  void showOverlay(){
    if(overlay!=null)return;
    wm=(WindowManager)getSystemService(Context.WINDOW_SERVICE);
    Button stop=new Button(this);stop.setText("■ Qwen 중단");stop.setAllCaps(false);stop.setBackgroundColor(0xffef4758);stop.setTextColor(0xffffffff);
    stop.setOnClickListener(v->stop());
    WindowManager.LayoutParams p=new WindowManager.LayoutParams(-2,-2,WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,PixelFormat.TRANSLUCENT);
    p.gravity=Gravity.RIGHT|Gravity.TOP;p.y=85;wm.addView(stop,p);overlay=stop;
  }
  void hideOverlay(){if(overlay!=null){try{wm.removeView(overlay);}catch(Exception ignored){}overlay=null;}}
  public void start(String task,String key){
    if(!stopped)return;
    stopped=false;main.post(this::showOverlay);
    worker=new Thread(()->{
      String msg="작업 종료";
      try{
        Thread.sleep(1200);
        for(int step=1;step<=8&&!stopped;step++){
          JSONObject action=QwenClient.next(key,task,screenshot(),step);
          if(stopped)break;
          String type=action.optString("action");
          if(type.equals("done")){msg="AI가 작업 완료를 보고했어";break;}
          if(type.equals("wait")){Thread.sleep(1100);continue;}
          if(type.equals("back")){performGlobalAction(GLOBAL_ACTION_BACK);}
          else if(type.equals("home")){performGlobalAction(GLOBAL_ACTION_HOME);}
          else if(type.equals("tap"))gesture(coord(action,"x"),coord(action,"y"),coord(action,"x"),coord(action,"y"),85);
          else if(type.equals("swipe"))gesture(coord(action,"x1"),coord(action,"y1"),coord(action,"x2"),coord(action,"y2"),400);
          else {throw new IllegalArgumentException("지원되지 않는 명령: "+type);}
          Thread.sleep(1200);
        }
      }catch(InterruptedException e){msg="작업 중단";Thread.currentThread().interrupt();}
       catch(Exception e){msg="오류: "+e.getMessage();}
      finally{stopped=true;String result=msg;main.post(()->{hideOverlay();Toast.makeText(this,result,Toast.LENGTH_LONG).show();});}
    },"QwenAgent");
    worker.start();
  }
  int coord(JSONObject a,String k)throws Exception{int n=a.getInt(k);if(n<0||n>1000)throw new Exception("좌표 범위 오류");return n;}
  void gesture(int x1,int y1,int x2,int y2,long ms)throws Exception{
    int w=getResources().getDisplayMetrics().widthPixels,h=getResources().getDisplayMetrics().heightPixels;
    Path path=new Path();path.moveTo(x1*w/1000f,y1*h/1000f);if(x1!=x2||y1!=y2)path.lineTo(x2*w/1000f,y2*h/1000f);
    GestureDescription g=new GestureDescription.Builder().addStroke(new GestureDescription.StrokeDescription(path,0,ms)).build();
    CountDownLatch latch=new CountDownLatch(1);
    if(!dispatchGesture(g,new GestureResultCallback(){@Override public void onCompleted(GestureDescription d){latch.countDown();}@Override public void onCancelled(GestureDescription d){latch.countDown();}},main))throw new Exception("터치 거절");
    if(!latch.await(8,TimeUnit.SECONDS))throw new Exception("터치 시간 초과");
  }
  String screenshot()throws Exception{
    CountDownLatch latch=new CountDownLatch(1);AtomicReference<Bitmap> img=new AtomicReference<>();
    takeScreenshot(Display.DEFAULT_DISPLAY,getMainExecutor(),new TakeScreenshotCallback(){
      @Override public void onSuccess(ScreenshotResult r){
        HardwareBuffer buf=r.getHardwareBuffer();
        try{Bitmap src=Bitmap.wrapHardwareBuffer(buf,r.getColorSpace());if(src!=null)img.set(src.copy(Bitmap.Config.ARGB_8888,false));}finally{buf.close();latch.countDown();}
      }
      @Override public void onFailure(int c){latch.countDown();}
    });
    if(!latch.await(12,TimeUnit.SECONDS)||img.get()==null)throw new Exception("화면 캡처 실패");
    Bitmap b=img.get();float f=Math.min(1f,1152f/Math.max(b.getWidth(),b.getHeight()));
    Bitmap small=f==1f?b:Bitmap.createScaledBitmap(b,Math.round(b.getWidth()*f),Math.round(b.getHeight()*f),true);
    ByteArrayOutputStream out=new ByteArrayOutputStream();small.compress(Bitmap.CompressFormat.JPEG,65,out);
    if(small!=b)small.recycle();b.recycle();return Base64.encodeToString(out.toByteArray(),Base64.NO_WRAP);
  }
}
