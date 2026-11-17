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
  static volatile TouchService instance;
  private final Handler ui=new Handler(Looper.getMainLooper());
  private volatile boolean running=false,working=false;
  private volatile String lastApp="";
  private String key,task;
  private int step=1;
  private JSONObject proposal;
  private WindowManager manager;
  private LinearLayout overlay;
  private TextView info;
  private Button scan,approve,stopButton;
  @Override protected void onServiceConnected(){super.onServiceConnected();instance=this;}
  @Override public void onAccessibilityEvent(AccessibilityEvent event){
    if(event.getPackageName()!=null){
      String app=event.getPackageName().toString();
      if(!app.startsWith("com.example.qwentouch")&&!app.equals("com.android.systemui"))lastApp=app;
    }
  }
  @Override public void onInterrupt(){stop();}
  @Override public void onDestroy(){stop();instance=null;super.onDestroy();}
  private boolean allowedApp(){
    String p=lastApp;
    return p.equals("com.android.chrome")||p.equals("com.google.android.youtube")||
      p.equals("com.sec.android.app.sbrowser")||p.equals("com.sec.android.app.launcher")||
      p.equals("com.google.android.apps.nexuslauncher")||p.equals("org.mozilla.firefox")||
      p.equals("com.brave.browser")||p.equals("com.microsoft.emmx");
  }
  public void start(String userTask,String apiKey){
    if(running)return;
    task=userTask;key=apiKey;step=1;proposal=null;running=true;working=false;
    ui.post(this::showOverlay);
  }
  public void stop(){
    running=false;working=false;proposal=null;key=null;task=null;
    ui.post(()->{
      if(overlay!=null){try{manager.removeView(overlay);}catch(Exception ignored){} overlay=null;}
    });
  }
  private void showOverlay(){
    if(!running||overlay!=null)return;
    manager=(WindowManager)getSystemService(Context.WINDOW_SERVICE);
    overlay=new LinearLayout(this);overlay.setOrientation(1);overlay.setPadding(12,8,12,8);overlay.setBackgroundColor(0xf5222a42);
    info=new TextView(this);info.setTextColor(-1);info.setTextSize(14);info.setText("Qwen: 각 단계를 직접 승인해 줘.");overlay.addView(info);
    LinearLayout row=new LinearLayout(this);row.setOrientation(0);overlay.addView(row);
    scan=new Button(this);scan.setText("화면 전송");scan.setAllCaps(false);row.addView(scan,new LinearLayout.LayoutParams(0,-2,1));
    approve=new Button(this);approve.setText("터치 승인");approve.setAllCaps(false);approve.setEnabled(false);row.addView(approve,new LinearLayout.LayoutParams(0,-2,1));
    stopButton=new Button(this);stopButton.setText("■ 중단");stopButton.setAllCaps(false);row.addView(stopButton,new LinearLayout.LayoutParams(0,-2,1));
    scan.setOnClickListener(v->requestSuggestion());approve.setOnClickListener(v->performApproved());stopButton.setOnClickListener(v->stop());
    WindowManager.LayoutParams params=new WindowManager.LayoutParams(-1,-2,WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
      WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,PixelFormat.TRANSLUCENT);
    params.gravity=Gravity.TOP;
    manager.addView(overlay,params);
  }
  private void say(String msg){
    ui.post(()->{if(running&&overlay!=null){overlay.setVisibility(View.VISIBLE);info.setText(msg);scan.setEnabled(!working&&proposal==null);approve.setEnabled(!working&&proposal!=null);}});
  }
  private void requestSuggestion(){
    if(!running||working||proposal!=null)return;
    if(!allowedApp()){say("안전상 지원 앱만 분석 가능해. 브라우저 또는 유튜브를 열어 줘.");return;}
    working=true;scan.setEnabled(false);approve.setEnabled(false);
    info.setText("현재 화면을 Qwen에 전송하기 위해 캡처 중...");
    String currentTask=task, currentKey=key;
    String app=lastApp;
    overlay.setVisibility(View.GONE);
    ui.postDelayed(()->new Thread(()->{
      try{
        String screenshot=screenshot();
        ui.post(()->{if(overlay!=null)overlay.setVisibility(View.VISIBLE);});
        if(!running)return;
        JSONObject result=QwenClient.next(currentKey,currentTask,screenshot,step);
        if(!running)return;
        if(!app.equals(lastApp)){throw new Exception("작업 중 앱이 변경되었어. 다시 분석해 줘.");}
        if(result.optString("action").equals("done")){ui.post(()->{Toast.makeText(this,"Qwen이 완료를 보고했어.",Toast.LENGTH_LONG).show();stop();});return;}
        proposal=result;working=false;
        say("제안 ("+step+"/8): "+QwenClient.describe(result)+" — 실행 전 위치를 확인해.");
      }catch(Exception e){
        working=false;proposal=null;say("분석 실패: "+e.getMessage());
      }finally{ui.post(()->{if(overlay!=null)overlay.setVisibility(View.VISIBLE);});}
    },"QwenScreenshot").start(),220);
  }
  private void performApproved(){
    if(!running||working||proposal==null)return;
    if(!allowedApp()){proposal=null;say("안전상 지원되는 앱에서만 터치할 수 있어.");return;}
    JSONObject action=proposal;proposal=null;working=true;
    say("승인된 동작 실행 중...");
    new Thread(()->{
      try{
        String kind=action.getString("action");
        if(kind.equals("tap"))gesture(coord(action,"x"),coord(action,"y"),coord(action,"x"),coord(action,"y"),95);
        else if(kind.equals("swipe"))gesture(coord(action,"x1"),coord(action,"y1"),coord(action,"x2"),coord(action,"y2"),400);
        else if(kind.equals("back"))performGlobalAction(GLOBAL_ACTION_BACK);
        else if(kind.equals("home"))performGlobalAction(GLOBAL_ACTION_HOME);
        else if(kind.equals("wait"))Thread.sleep(950);
        else throw new Exception("지원하지 않는 동작");
        if(!running)return;
        step++;working=false;
        if(step>8){ui.post(()->{Toast.makeText(this,"8단계 제한으로 종료했어.",Toast.LENGTH_LONG).show();stop();});return;}
        say("동작 실행됨. 다음 화면을 직접 확인하고 '화면 전송'을 눌러 줘.");
      }catch(Exception e){working=false;say("동작 오류: "+e.getMessage());}
    },"QwenGesture").start();
  }
  private int coord(JSONObject o,String k)throws Exception{int v=o.getInt(k);if(v<0||v>1000)throw new Exception("AI 좌표 범위 오류");return v;}
  private void gesture(int x1,int y1,int x2,int y2,long duration)throws Exception{
    int width=getResources().getDisplayMetrics().widthPixels,height=getResources().getDisplayMetrics().heightPixels;
    Path path=new Path();path.moveTo(x1*width/1000f,y1*height/1000f);
    if(x1!=x2||y1!=y2)path.lineTo(x2*width/1000f,y2*height/1000f);
    GestureDescription gesture=new GestureDescription.Builder()
      .addStroke(new GestureDescription.StrokeDescription(path,0,duration)).build();
    CountDownLatch done=new CountDownLatch(1);
    if(!dispatchGesture(gesture,new GestureResultCallback(){
      @Override public void onCompleted(GestureDescription d){done.countDown();}
      @Override public void onCancelled(GestureDescription d){done.countDown();}
    },ui))throw new Exception("터치가 거부되었어");
    if(!done.await(8,TimeUnit.SECONDS))throw new Exception("터치 시간 초과");
  }
  private String screenshot()throws Exception{
    CountDownLatch done=new CountDownLatch(1);AtomicReference<Bitmap> ref=new AtomicReference<>();
    takeScreenshot(Display.DEFAULT_DISPLAY,getMainExecutor(),new TakeScreenshotCallback(){
      @Override public void onSuccess(ScreenshotResult shot){
        HardwareBuffer b=shot.getHardwareBuffer();
        try{Bitmap src=Bitmap.wrapHardwareBuffer(b,shot.getColorSpace());if(src!=null)ref.set(src.copy(Bitmap.Config.ARGB_8888,false));}
        finally{b.close();done.countDown();}
      }
      @Override public void onFailure(int error){done.countDown();}
    });
    if(!done.await(12,TimeUnit.SECONDS)||ref.get()==null)throw new Exception("화면 캡처 실패");
    Bitmap original=ref.get();
    float f=Math.min(1f,1152f/Math.max(original.getWidth(),original.getHeight()));
    Bitmap scaled=f<1?Bitmap.createScaledBitmap(original,Math.round(original.getWidth()*f),Math.round(original.getHeight()*f),true):original;
    ByteArrayOutputStream bytes=new ByteArrayOutputStream();scaled.compress(Bitmap.CompressFormat.JPEG,65,bytes);
    if(scaled!=original)scaled.recycle();original.recycle();
    return Base64.encodeToString(bytes.toByteArray(),Base64.NO_WRAP);
  }
}
