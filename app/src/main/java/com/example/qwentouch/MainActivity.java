package com.example.qwentouch;
import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.provider.Settings;
import android.graphics.Color;
import android.view.View;
import android.widget.*;
public class MainActivity extends Activity {
  static final int DARK=0xff151b31;
  EditText key,task; TextView status;
  LinearLayout root;
  TextView label(String s,int size){ TextView v=new TextView(this); v.setText(s);v.setTextSize(size);v.setTextColor(Color.WHITE);v.setPadding(12,14,12,14);return v;}
  Button button(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);return b;}
  @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(DARK);
    ScrollView sc=new ScrollView(this);root=new LinearLayout(this);root.setOrientation(1);root.setPadding(28,35,28,40);root.setBackgroundColor(DARK);sc.addView(root);setContentView(sc);
    root.addView(label("✦ Qwen Touch AI",29));
    root.addView(label("화면을 인식하고 직접 터치하는 AI · 초기 테스트 버전",14));
    status=label("접근성 서비스를 먼저 활성화해 줘.",14);root.addView(status);
    Button access=button("① 접근성 권한 설정 열기");root.addView(access);access.setOnClickListener(v->startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
    root.addView(label("② Qwen API 키 (DashScope 국제 리전)",16));
    key=new EditText(this);key.setSingleLine(true);key.setTextColor(Color.WHITE);key.setHintTextColor(0xffaaaaaa);key.setHint("sk-...");root.addView(key);
    root.addView(label("③ AI에게 시킬 작업",16));
    task=new EditText(this);task.setSingleLine(false);task.setMinLines(2);task.setTextColor(Color.WHITE);task.setHintTextColor(0xffaaaaaa);task.setHint("예: 브라우저를 열어서 고양이 검색해 줘");root.addView(task);
    root.addView(label("주의: 현재 화면이 Qwen 클라우드 서버로 전송돼. 민감한 화면이나 계정에서는 사용하지 마.",13));
    Button start=button("▶ AI 작업 시작 (8단계 한정)");root.addView(start);start.setOnClickListener(v->{
      if(TouchService.instance==null){Toast.makeText(this,"접근성 권한을 켜 줘.",Toast.LENGTH_LONG).show();return;}
      String k=key.getText().toString().trim(),t=task.getText().toString().trim();
      if(k.length()<5||t.length()<3){Toast.makeText(this,"API 키와 작업 내용을 입력해 줘.",Toast.LENGTH_LONG).show();return;}
      new android.app.AlertDialog.Builder(this).setTitle("화면 전송 및 터치 권한")
        .setMessage("현재 화면이 Qwen API에 전송되고 AI가 화면을 터치해. 결제·개인정보·메시지 전송은 허용하지 않아. 작업을 시작할까?")
        .setNegativeButton("취소",(d,w)->{}).setPositiveButton("시작",(d,w)->{TouchService.instance.start(t,k);moveTaskToBack(true);}).show();
    });
    Button stop=button("■ 긴급 중단");root.addView(stop);stop.setOnClickListener(v->{if(TouchService.instance!=null)TouchService.instance.stop();});
  }
  @Override protected void onResume(){super.onResume();if(status!=null) status.setText(TouchService.instance==null?"🔒 접근성 서비스 꺼짐":"✅ 접근성 서비스 준비됨");}
}
