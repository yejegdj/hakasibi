package com.example.qwentouch;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.provider.Settings;
import android.text.InputType;
import android.widget.*;

public class MainActivity extends Activity {
  private EditText key,task;
  private TextView status;
  private LinearLayout root;
  private TextView label(String s,int sz){TextView v=new TextView(this);v.setText(s);v.setTextSize(sz);v.setTextColor(-1);v.setPadding(12,14,12,14);return v;}
  private Button button(String s){Button b=new Button(this);b.setAllCaps(false);b.setText(s);return b;}
  @Override public void onCreate(Bundle b){
    super.onCreate(b);
    getWindow().setStatusBarColor(0xff11182d);
    ScrollView scroll=new ScrollView(this);
    root=new LinearLayout(this);root.setOrientation(1);root.setPadding(24,35,24,35);root.setBackgroundColor(0xff11182d);
    scroll.addView(root);setContentView(scroll);
    root.addView(label("Qwen Touch AI · Experimental",26));
    root.addView(label("실험적 터치 모드: 화면을 Qwen에 보낼 때마다 승인, 실제 터치할 때마다 승인이 필요해. 지원되는 앱은 브라우저·유튜브·기본 홈 화면으로 제한돼.",15));
    status=label("접근성 서비스 연결 대기 중",14);root.addView(status);
    Button grant=button("① 접근성 서비스 설정 열기");root.addView(grant);
    grant.setOnClickListener(v->startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
    root.addView(label("② Qwen API 키 (앱 종료 시 저장 안 됨)",16));
    key=new EditText(this);key.setSingleLine(true);
    key.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
    key.setTextColor(-1);key.setHintTextColor(0xffb7bdd2);key.setHint("sk-...");root.addView(key);
    root.addView(label("③ 할 일",16));
    task=new EditText(this);task.setTextColor(-1);task.setHintTextColor(0xffb7bdd2);
    task.setHint("예: 브라우저에서 고양이 검색");task.setMinLines(2);root.addView(task);
    root.addView(label("⚠️ 화면이 외부 Qwen 서버로 전송돼. 비밀번호, 대화, 금융 정보, 로그인 화면에서 사용하지 마. Play 프로텍트가 설치를 계속 차단할 수도 있어.",13));
    Button start=button("▶ 단계별 승인 모드 시작");root.addView(start);
    start.setOnClickListener(v->{
      if(TouchService.instance==null){Toast.makeText(this,"접근성 서비스를 활성화해 줘.",Toast.LENGTH_LONG).show();return;}
      String k=key.getText().toString().trim(),t=task.getText().toString().trim();
      if(k.length()<5||t.length()<2){Toast.makeText(this,"API 키와 할 일을 적어 줘.",Toast.LENGTH_LONG).show();return;}
      new android.app.AlertDialog.Builder(this).setTitle("화면 인식 및 터치")
        .setMessage("다음 화면 분석/터치 각각을 명시적으로 승인해야 해. 외부 서버에 화면이 전송돼. 진행할까?")
        .setNegativeButton("취소",(d,w)->{}).setPositiveButton("시작",(d,w)->{
          if(TouchService.instance!=null){TouchService.instance.start(t,k);moveTaskToBack(true);}
        }).show();
    });
    Button stop=button("■ 동작 중단");root.addView(stop);
    stop.setOnClickListener(v->{if(TouchService.instance!=null)TouchService.instance.stop();});
  }
  @Override protected void onResume(){
    super.onResume();if(status!=null)status.setText(TouchService.instance==null?"🔒 접근성 서비스 꺼짐":"✅ 접근성 서비스 연결됨");
  }
}
