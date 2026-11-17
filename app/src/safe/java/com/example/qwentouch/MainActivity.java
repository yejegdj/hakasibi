package com.example.qwentouch;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.content.ContentResolver;
import android.net.Uri;
import android.graphics.*;
import android.text.InputType;
import android.util.Base64;
import android.widget.*;
import android.view.View;
import java.io.*;

public class MainActivity extends Activity {
  private static final int PICK_IMAGE=15;
  private Uri imageUri;
  private EditText apiKey, task;
  private TextView status, result;
  private Button analyze;
  private LinearLayout root;
  private TextView label(String text,int size) {
    TextView v=new TextView(this);v.setText(text);v.setTextSize(size);
    v.setTextColor(0xfff1f4ff);v.setPadding(12,14,12,14);return v;
  }
  private Button button(String text) {Button b=new Button(this);b.setAllCaps(false);b.setText(text);return b;}
  @Override public void onCreate(Bundle saved) {
    super.onCreate(saved);
    getWindow().setStatusBarColor(0xff11182d);
    ScrollView scroll=new ScrollView(this);
    root=new LinearLayout(this);root.setOrientation(1);root.setPadding(26,36,26,36);root.setBackgroundColor(0xff11182d);
    scroll.addView(root);setContentView(scroll);
    root.addView(label("✦ Qwen Touch AI · Safe",26));
    root.addView(label("안전 모드: 화면 자동 캡처 및 자동 터치를 사용하지 않아. 사용자가 선택한 스크린샷을 AI가 분석해서 다음 터치 위치를 안내해.",15));
    root.addView(label("① Qwen API 키 (해당 서버에서 과금될 수 있음)",16));
    apiKey=new EditText(this);apiKey.setSingleLine(true);
    apiKey.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
    apiKey.setTextColor(-1);apiKey.setHintTextColor(0xffb7bdd2);apiKey.setHint("sk-...");root.addView(apiKey);
    root.addView(label("② 할 일",16));
    task=new EditText(this);task.setTextColor(-1);task.setHintTextColor(0xffb7bdd2);task.setHint("예: 유튜브에서 고양이 영상 찾기");task.setMinLines(2);
    root.addView(task);
    Button pick=button("③ 스크린샷 선택 (수동)");root.addView(pick);
    status=label("아직 이미지를 선택하지 않았어.",14);root.addView(status);
    pick.setOnClickListener(v->{
      Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT);intent.setType("image/*");
      intent.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(intent,PICK_IMAGE);
    });
    analyze=button("④ Qwen에게 한 단계 제안받기");root.addView(analyze);
    result=label("",17);root.addView(result);
    root.addView(label("⚠️ 선택한 이미지가 Qwen 서버로 전송돼. 계정·결제·인증 정보가 포함된 스크린샷을 선택하지 마. AI가 판단한 터치는 자동 실행되지 않아.",13));
    analyze.setOnClickListener(v->{
      String k=apiKey.getText().toString().trim(),t=task.getText().toString().trim();
      if(imageUri==null||k.length()<5||t.length()<2){
        Toast.makeText(this,"API 키, 할 일, 스크린샷을 모두 준비해 줘.",Toast.LENGTH_LONG).show();return;
      }
      new android.app.AlertDialog.Builder(this).setTitle("화면 정보 전송")
       .setMessage("방금 고른 스크린샷을 Qwen 클라우드 API에 전송할까? 개인 정보가 들어 있지 않은지 확인해 줘.")
       .setNegativeButton("취소",(d,w)->{})
       .setPositiveButton("전송",(d,w)->analyze(k,t,imageUri)).show();
    });
  }
  @Override protected void onActivityResult(int request,int code,Intent data){
    super.onActivityResult(request,code,data);
    if(request==PICK_IMAGE&&code==RESULT_OK&&data!=null){
      imageUri=data.getData();status.setText("✅ 이미지 선택됨: "+(imageUri==null?"없음":"분석 가능"));
    }
  }
  private void analyze(String key,String prompt,Uri uri){
    analyze.setEnabled(false);result.setText("Qwen이 화면을 분석하는 중...");
    new Thread(()->{
      String text;
      try{
        String image=encodeImage(uri);
        org.json.JSONObject response=QwenClient.next(key,prompt,image,1);
        text="AI의 추천: "+QwenClient.describe(response)+"\n\n실제 터치는 직접 수행해. AI 제안은 틀릴 수 있어.";
      }catch(Exception e){text="분석 실패: "+e.getMessage();}
      final String message=text;runOnUiThread(()->{result.setText(message);analyze.setEnabled(true);});
    },"QwenSafe").start();
  }
  private String encodeImage(Uri uri)throws IOException{
    ContentResolver resolver=getContentResolver();
    BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;
    try(InputStream stream=resolver.openInputStream(uri)){if(stream==null)throw new IOException("이미지 열기 실패");BitmapFactory.decodeStream(stream,null,o);}
    if(o.outWidth<=0||o.outHeight<=0)throw new IOException("지원되지 않는 이미지");
    o.inSampleSize=1;
    while(Math.max(o.outWidth,o.outHeight)/o.inSampleSize>1600)o.inSampleSize*=2;
    o.inJustDecodeBounds=false;
    Bitmap b;
    try(InputStream stream=resolver.openInputStream(uri)){b=BitmapFactory.decodeStream(stream,null,o);}
    if(b==null)throw new IOException("이미지 디코딩 실패");
    float scale=Math.min(1f,1152f/Math.max(b.getWidth(),b.getHeight()));
    Bitmap target=scale<1f?Bitmap.createScaledBitmap(b,Math.max(1,Math.round(b.getWidth()*scale)),Math.max(1,Math.round(b.getHeight()*scale)),true):b;
    ByteArrayOutputStream out=new ByteArrayOutputStream();
    target.compress(Bitmap.CompressFormat.JPEG,65,out);
    if(target!=b)target.recycle();b.recycle();
    return Base64.encodeToString(out.toByteArray(),Base64.NO_WRAP);
  }
}
