package dev.local.bililight;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.net.Uri;
import android.text.InputType;
import android.view.*;
import android.webkit.*;
import android.widget.*;
import org.json.*;
import java.util.*;
import java.util.concurrent.*;

public final class MainActivity extends Activity {
    final ExecutorService worker=Executors.newSingleThreadExecutor();
    final ArrayList<Button> buttons=new ArrayList<>();
    SocialOptions social;
    Profiles profiles;
    Api api; Vault vault; JSONObject session,game; TextView state,account,detail;
    EditText username,password,country,phone,otp,qr; LinearLayout body;
    String smsKey,smsPhone,smsCountry; long smsAt; boolean busy;
    volatile CountDownLatch challengeWait; volatile JSONObject challengeAnswer;
    AlertDialog captchaDialog; WebView captchaView;
    interface Task { String run()throws Exception; }
    LinearLayout content; ScrollView scroller; Button[] nav=new Button[4];
    boolean preview; int page=0; boolean passwordMode;
    @Override public void onCreate(Bundle saved){super.onCreate(saved);
        preview=getIntent().getBooleanExtra("ui_preview",false);
        if(!preview)getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        getWindow().setStatusBarColor(Ui.BG);getWindow().setNavigationBarColor(Ui.SURFACE);
        api=new Api(this);vault=new Vault(this);api.challenge=this::challenge;
        try{profiles=new Profiles(vault);if(!preview){profiles.load();session=profiles.active();}}catch(Exception ignored){}
        LinearLayout root=Ui.column(this);root.setBackgroundColor(Ui.BG);setContentView(root);
        LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);header.setPadding(dp(22),dp(16),dp(22),dp(10));
        LinearLayout brand=Ui.column(this);brand.addView(Ui.label(this,"HONKAI QR",23,Ui.TEXT,true));brand.addView(Ui.label(this,"CHANNEL LOGIN COMPANION",9,Ui.MUTED,true));header.addView(brand,new LinearLayout.LayoutParams(0,-2,1));header.addView(Ui.badge(this,preview?"UI PREVIEW":"BETA 0.7",Ui.ACCENT));root.addView(header);
        state=Ui.label(this,preview?"미리보기 · 계정 데이터 및 네트워크 사용 안 함":"기기 안에서 안전하게 · Bilibili 채널",11,Ui.MUTED,false);state.setPadding(dp(22),dp(3),dp(22),dp(8));state.setMaxLines(2);root.addView(state);
        scroller=new ScrollView(this);scroller.setFillViewport(true);scroller.setClipToPadding(false);content=Ui.column(this);content.setPadding(dp(20),dp(10),dp(20),dp(18));scroller.addView(content);root.addView(scroller,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout bottom=new LinearLayout(this);bottom.setPadding(dp(8),dp(6),dp(8),dp(8));bottom.setBackgroundColor(Ui.SURFACE);
        String[] titles={"홈","스캔","기록","설정"};for(int i=0;i<4;i++){final int target=i;nav[i]=Ui.button(this,titles[i],false);nav[i].setTextSize(13);nav[i].setMinHeight(dp(48));nav[i].setMinimumHeight(dp(48));nav[i].setBackgroundColor(Color.TRANSPARENT);nav[i].setOnClickListener(v->{if(!busy)render(target);});bottom.addView(nav[i],new LinearLayout.LayoutParams(0,dp(52),1));}root.addView(bottom);
        content.setFocusableInTouchMode(true);content.requestFocus();render(0);if(!preview&&getIntent().getBooleanExtra("local_tests",false))run(()->LocalTests.run(this));else if(!preview&&getIntent().getBooleanExtra("diagnose",false))diagnose();else if(!preview&&!DispatchProvider.allowed(this))providerConsent();
    }
    void render(int target){page=target;content.removeAllViews();buttons.clear();body=content;account=null;detail=null;
        for(int i=0;i<nav.length;i++)nav[i].setTextColor(i==target?Ui.ACCENT:Ui.MUTED);
        if(target==0)home();else if(target==1)scanPage();else if(target==2)historyPage();else settings();content.requestFocus();scroller.post(()->scroller.scrollTo(0,0));
    }
    void heading(String eyebrow,String title,String subtitle){text(eyebrow,11).setTextColor(Ui.ACCENT);text(title,27);text(subtitle,13).setTextColor(Ui.MUTED);Ui.gap(body,14);}
    LinearLayout card(){LinearLayout c=Ui.card(this);content.addView(c);body=c;return c;}
    void chip(String value,int color){body.addView(Ui.badge(this,value,color),new LinearLayout.LayoutParams(-2,-2));Ui.gap(body,7);}
    void home(){heading("CONNECT, WITHOUT THE WAIT","게임 말고, 로그인만.","모바일 게임을 실행하지 않고 PC 연결을 준비하세요.");
        card();chip("현재 채널  ·  BILIBILI",Ui.ACCENT);
        account=text(preview?"데모 계정":session==null?"계정을 연결해 주세요":maskedAccount(),21);
        text(preview?"개인정보 없는 디자인 미리보기":session==null?"SMS 또는 계정 인증으로 시작합니다.":"암호화 저장됨 · "+session.optString("login_method","기존 인증"),12).setTextColor(Ui.MUTED);
        button(session==null?"계정 연결하기":"계정 전환 / 관리",()->{if(session==null)accountPage(false);else accounts();});
        card();Ui.Mark mark=new Ui.Mark(this,0,Ui.ACCENT);body.addView(mark,new LinearLayout.LayoutParams(dp(48),dp(48)));Ui.gap(body,12);text("PC에 바로 연결",23);text("내 PC의 QR을 읽고, 확인 후 승인하세요.",13).setTextColor(Ui.MUTED);button("PC QR 스캔",this::startScan);secondary("QR 주소 직접 입력",()->render(1));
        card();chip("연결 흐름",Ui.MUTED);text("01  계정 인증   →   02  QR 스캔   →   03  승인",13);text("원본 설정 서비스 실시간 조회 · 새 경로 PC 접속 검증 중",11).setTextColor(Ui.AMBER);
        body=content;text("비밀번호 저장 없음  ·  카메라 이미지 업로드 없음",11).setTextColor(Ui.MUTED);
    }
    String maskedAccount(){return displayName(session);}
    String displayName(JSONObject p){if(p==null)return "계정 없음";String alias=p.optString("alias");if(!alias.isEmpty())return alias;String name=p.optString("username","B服 계정");return name.length()>2?name.substring(0,2)+"•••":name+"•••";}
    void startScan(){if(preview){message("미리보기에서는 카메라와 계정 인증을 사용하지 않습니다.");return;}if(session==null){accountPage(false);return;}startActivityForResult(new Intent(this,ScanActivity.class),20);}
    void scanPage(){heading("SCAN & APPROVE","내 PC 연결","중국 붕괴3 공식 QR만 허용합니다.");
        if(session==null&&!preview){card();text("먼저 계정을 연결하세요",21);text("다른 서버의 계정으로 바뀌지 않도록 B服 인증이 먼저 필요합니다.",13).setTextColor(Ui.MUTED);button("계정 연결",()->accountPage(false));return;}
        card();chip("BILIBILI  ·  CHANNEL 14",Ui.ACCENT);button("카메라로 QR 읽기",this::startScan);secondary("갤러리 이미지에서 읽기",this::pickImage);Ui.gap(body,12);text("QR 주소로 연결",16);qr=edit("PC QR 주소를 붙여넣으세요",InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_URI);qr.setMaxLines(3);button("로그인 요청 확인",this::confirmQr);
        card();chip("승인 전 확인",Ui.AMBER);text("본인 PC에 표시된 QR인지 확인하세요. 스캔만으로 자동 승인하지 않습니다.",13);text("승인 API 수락과 게임의 실제 접속 성공은 별개입니다.",12).setTextColor(Ui.MUTED);
    }
    void accountPage(boolean usePassword){passwordMode=usePassword;content.removeAllViews();buttons.clear();body=content;heading("YOUR ACCOUNT","B服 계정 연결","기존 게임의 계정 파일은 읽지 않습니다.");
        card();LinearLayout tabs=new LinearLayout(this);String[] names={"SMS 인증","계정·비밀번호"};for(int i=0;i<2;i++){final boolean pw=i==1;Button t=Ui.button(this,names[i],passwordMode==pw);t.setTextSize(13);t.setOnClickListener(v->{if(!busy)accountPage(pw);});tabs.addView(t,new LinearLayout.LayoutParams(0,dp(48),1));buttons.add(t);}body.addView(tabs);Ui.gap(body,8);
        if(usePassword){username=edit("비리비리 계정",InputType.TYPE_CLASS_TEXT);password=edit("비밀번호",InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);button("계정 인증",()->{String u=value(username),p=password.getText().toString();password.setText("");if(u.isEmpty()||p.isEmpty()){message("계정과 비밀번호를 입력하세요.");return;}run(()->{accept(api.password(u,p),"계정·비밀번호");return "계정 인증 성공. 이제 PC QR을 스캔할 수 있습니다.";},()->render(0));});}
        else{country=edit("국가 코드",InputType.TYPE_CLASS_PHONE);country.setText("86");phone=edit("휴대전화 번호",InputType.TYPE_CLASS_PHONE);secondary("인증번호 받기",this::requestSms);otp=edit("SMS 인증번호",InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_VARIATION_PASSWORD);button("인증하고 연결",()->{final String code=value(otp);otp.setText("");if(smsKey==null||!value(phone).equals(smsPhone)||!value(country).equals(smsCountry)){message("현재 번호로 인증번호를 먼저 요청하세요.");return;}if(!code.matches("[0-9]{4,8}")){message("인증번호를 확인하세요.");return;}run(()->{accept(api.sms(smsCountry,smsPhone,smsKey,code),"SMS");smsKey=null;return "SMS 계정 인증 성공";},()->render(0));});text("국제 SMS 수신 여부와 요금은 통신사 조건에 따릅니다.",11).setTextColor(Ui.MUTED);}
        card();text("다른 인증 방법",18);secondary("기타 / 소셜 로그인",()->run(()->{JSONObject configured=api.configure();JSONObject third;try{third=api.thirdConfig();}catch(Exception e){third=new JSONObject();}social=new SocialOptions(configured,third);return "설정 확인 완료";},()->socialMenu()));text("현재 제공되는 인증만 활성화합니다. 소셜 연동은 추가 검증 중입니다.",12).setTextColor(Ui.MUTED);body=content;secondary("홈으로 돌아가기",()->render(0));content.requestFocus();scroller.post(()->scroller.scrollTo(0,0));
    }
    void requestSms(){if(preview){message("미리보기에서는 SMS를 보내지 않습니다.");return;}final String c=value(country),p=value(phone);if(!c.matches("[0-9]{1,4}")||!p.matches("[0-9]{5,20}")){message("국가 코드와 번호를 확인하세요.");return;}if(smsAt!=0&&SystemClock.elapsedRealtime()-smsAt<60000){message("재전송은 60초 후 가능합니다.");return;}
        new AlertDialog.Builder(this).setTitle("인증번호 전송").setMessage("+"+c+" "+p+" 번호로 SMS를 요청합니다.").setNegativeButton("취소",null).setPositiveButton("전송",(d,w)->run(()->{smsAt=SystemClock.elapsedRealtime();JSONObject r=api.sendSms(c,p);smsKey=r.getString("captcha_key");smsPhone=p;smsCountry=c;return "인증번호를 보냈습니다. 받은 코드를 입력하세요.";})).show();
    }
    void channels(){heading("CHANNEL DIRECTORY","지원 채널","원본 목록과 현재 앱의 구현 상태를 구분합니다.");
        for(ChannelCatalog.Entry e:ChannelCatalog.ALL){card();LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);TextView icon=Ui.label(this,e.shortName,15,e.color,true);icon.setGravity(Gravity.CENTER);icon.setBackground(Ui.shape(this,(e.color&0xffffff)|0x22000000,12));row.addView(icon,new LinearLayout.LayoutParams(dp(44),dp(44)));TextView label=Ui.label(this,e.name,17,Ui.TEXT,true);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);lp.leftMargin=dp(13);row.addView(label,lp);body.addView(row);Ui.gap(body,13);chip(e.status,e.available?Ui.ACCENT:Ui.MUTED);text(e.notes,12).setTextColor(Ui.MUTED);secondary(e.available?"B服 계정 관리":"연동 상태 보기",()->{if(e.available)accountPage(false);else new AlertDialog.Builder(this).setTitle(e.name+" · 아직 사용 불가").setMessage(e.notes+"\n\n이 항목은 지원 목록이며 동작하는 로그인 버튼이 아닙니다.").setPositiveButton("확인",null).show();});}
    }
    void settings(){heading("PRIVATE BY DESIGN","설정과 진단","계정은 기기에, 상태는 명확하게.");
        card();chip("개인정보",Ui.ACCENT);text("기기 안에 암호화 보관",20);text("Android Keystore · AES-GCM\n계정 자동 백업 비활성화\n비밀번호·SMS 코드 저장 안 함\nQR 이미지는 로컬에서만 처리",13).setTextColor(Ui.MUTED);
        card();text("게임 설정 서비스",20);text(DispatchProvider.DISCLOSURE,12).setTextColor(Ui.MUTED);text("게임 버전: "+DispatchProvider.version(this)+" · 동의: "+(DispatchProvider.allowed(this)?"켜짐":"꺼짐"),13);secondary("설정 서비스 사용 동의 / 철회",this::providerConsent);secondary("게임 버전 변경",this::providerVersion);secondary("실시간 설정 조회 검사",()->run(()->{String value=DispatchProvider.fetch(this);return "설정 서비스 응답 정상 · 암호문 "+value.length()+"자. PC 접속 성공은 별도 확인이 필요합니다.";}));
        card();text("연결 진단",20);secondary("초기 설정 · RSA · QR 검사",this::diagnose);secondary("로컬 호환 기능 검사",()->run(()->LocalTests.run(this)));secondary("B服 게임 토큰 검사",()->run(()->{requireSession();game=api.gameLogin(session);return "B服 게임 토큰 발급 성공";}));detail=text("진단은 PC 접속 성공을 자동으로 보증하지 않습니다.",12);detail.setTextColor(Ui.MUTED);
        card();text("호환성과 채널",20);secondary("원본 채널 호환표",()->{content.removeAllViews();buttons.clear();body=content;channels();content.requestFocus();scroller.post(()->scroller.scrollTo(0,0));});
        card();text("개발자",20);text("HONKAI QR",17);secondary("개발자 블로그",this::developerBlog);
        card();chip("공개 배포 준비 중",Ui.AMBER);text("아직 배포용 완성판은 아닙니다",18);text("로그 기반 임시 설정을 제거했습니다. 외부 설정 서비스에 의존하며, 새 경로의 실제 PC 로그인은 검증 중입니다. 다른 채널 및 일부 소셜 인증은 미검증입니다.",13).setTextColor(Ui.MUTED);secondary("라이선스 고지",this::licenses);
        card();text("저장 계정",18);secondary("이 앱의 저장 계정 삭제",()->{if(preview)return;new AlertDialog.Builder(this).setTitle("저장 계정 삭제").setMessage("이 앱에 저장한 세션만 삭제합니다. 원래 게임의 로그인에는 영향을 주지 않습니다.").setNegativeButton("취소",null).setPositiveButton("삭제",(d,w)->{try{profiles.clear();}catch(Exception ignored){}session=null;game=null;smsKey=null;render(0);message("저장 계정을 삭제했습니다.");}).show();});body=content;text("HONKAI QR 0.7.0-beta · 비공식 시험판",11).setTextColor(Ui.MUTED);
    }
    void pickImage(){if(preview){message("미리보기에서는 개인 이미지를 열지 않습니다.");return;}Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("image/*");i.addCategory(Intent.CATEGORY_OPENABLE);i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);try{startActivityForResult(i,40);}catch(ActivityNotFoundException e){message("시스템 사진 선택기를 열 수 없습니다.");}}
    void accounts(){
        if(preview){message("미리보기에는 실제 계정이 없습니다.");return;}if(profiles==null)return;
        try{JSONArray a=profiles.list();String[] labels=new String[a.length()+1];for(int i=0;i<a.length();i++){JSONObject p=a.getJSONObject(i);labels[i]=(p.optString("uid").equals(session==null?"":session.optString("uid"))?"현재 · ":"")+displayName(p)+"  /  "+p.optString("login_method","기존 인증");}labels[a.length()]="＋ 새 계정 추가";
            new AlertDialog.Builder(this).setTitle("암호화 보관 계정").setItems(labels,(d,index)->{if(index==a.length())accountPage(false);else profileActions(index);}).setNegativeButton("닫기",null).show();
        }catch(Exception e){message("보관 계정 목록을 열지 못했습니다.");}
    }
    void profileActions(int index){try{JSONObject p=profiles.list().getJSONObject(index);new AlertDialog.Builder(this).setTitle(displayName(p)).setItems(new String[]{"이 계정으로 전환","별칭 편집","이 계정만 삭제"},(d,which)->{
        if(which==0){try{profiles.select(index);session=profiles.active();game=null;smsKey=null;render(0);message("계정을 전환했습니다. 다음 승인 때 해당 계정의 게임 토큰을 새로 확인합니다.");}catch(Exception e){message("계정 전환 저장에 실패했습니다.");}}
        else if(which==1){EditText input=Ui.input(this,"별칭 · 최대 24자",InputType.TYPE_CLASS_TEXT);input.setText(p.optString("alias"));new AlertDialog.Builder(this).setTitle("기기 안에서만 사용할 별칭").setView(input).setNegativeButton("취소",null).setPositiveButton("저장",(x,w)->{try{profiles.alias(index,input.getText().toString());session=profiles.active();render(0);}catch(Exception e){message(e.getMessage());}}).show();}
        else new AlertDialog.Builder(this).setTitle("이 계정의 저장 정보 삭제").setMessage("이 앱에 보관한 해당 계정의 토큰과 별칭만 삭제합니다. 실제 게임 계정은 삭제되지 않습니다.").setNegativeButton("취소",null).setPositiveButton("삭제",(x,w)->{try{profiles.remove(index);session=profiles.active();game=null;render(0);}catch(Exception e){message("저장 계정 삭제에 실패했습니다.");}}).show();
    }).setNegativeButton("취소",null).show();}catch(Exception e){message("계정 정보를 열지 못했습니다.");}}
    void historyPage(){heading("RECENT ACTIVITY","연결 기록","승인 API 결과만 기기에 보관합니다.");
        JSONArray a=new JSONArray();if(!preview&&profiles!=null)try{a=profiles.history();}catch(Exception ignored){}
        if(a.length()==0){card();text("아직 기록이 없습니다",21);text("QR 승인 요청이 끝나면 수락·실패 여부를 표시합니다. 계정 ID, 토큰, QR 원문은 기록하지 않습니다.",13).setTextColor(Ui.MUTED);secondary("PC 연결하기",()->render(1));}
        else{java.text.DateFormat fmt=new java.text.SimpleDateFormat("MM.dd  HH:mm",java.util.Locale.getDefault());for(int i=a.length()-1;i>=0;i--)try{JSONObject e=a.getJSONObject(i);card();chip(e.optBoolean("ok")?"요청 수락":"요청 실패",e.optBoolean("ok")?Ui.ACCENT:Ui.AMBER);text(fmt.format(new java.util.Date(e.getLong("at"))),17);text("Bilibili · 승인 API 결과 / 실제 PC 진입 여부와 별개",11).setTextColor(Ui.MUTED);}catch(Exception ignored){}
            body=content;secondary("기록 지우기",()->new AlertDialog.Builder(this).setTitle("이 앱의 연결 기록 삭제").setMessage("계정과 로그인 상태는 유지합니다.").setNegativeButton("취소",null).setPositiveButton("삭제",(d,w)->{try{profiles.clearHistory();render(2);}catch(Exception e){message("기록 삭제에 실패했습니다.");}}).show());}
    }
    void developerBlog(){try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://blog.naver.com/rkdrlgh22")));}catch(ActivityNotFoundException e){message("블로그를 열 수 있는 브라우저가 없습니다.");}}
    void licenses(){
        StringBuilder notice=new StringBuilder("비공식 개발 프리뷰\nHonkaiScanner 참고 구현 · ZXing core 3.5.3\n\n");
        for(String name:Arrays.asList("HonkaiScanner-MIT.txt","ZXing-Apache-2.0.txt"))try(java.io.InputStream in=getAssets().open("licenses/"+name)){
            java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();byte[] buffer=new byte[4096];int n;while((n=in.read(buffer))!=-1){out.write(buffer,0,n);if(out.size()>100000)throw new java.io.IOException("notice size");}notice.append(name).append("\n").append(out.toString("UTF-8")).append("\n\n");
        }catch(Exception e){notice.append(name).append(": 라이선스 고지를 열 수 없습니다.\n");}
        ScrollView scroll=new ScrollView(this);TextView label=Ui.label(this,notice.toString(),12,Ui.TEXT,false);label.setPadding(dp(18),dp(10),dp(18),dp(16));label.setTextIsSelectable(true);scroll.addView(label);new AlertDialog.Builder(this).setTitle("라이선스 고지").setView(scroll).setPositiveButton("확인",null).show();
    }
    void secondary(String label,Runnable action){Button b=Ui.button(this,label,false);b.setOnClickListener(v->action.run());body.addView(b);buttons.add(b);}
    void requireSession()throws Exception{if(session==null)throw new Exception("먼저 계정 인증이 필요합니다.");}
    void accept(JSONObject result,String method)throws Exception{
        if(result.optString("uid").isEmpty()||result.optString("access_key").isEmpty())throw new Exception("로그인 응답에 게임 계정 uid/access_key가 없습니다.");
        JSONObject next=new JSONObject().put("uid",result.getString("uid")).put("access_key",result.getString("access_key")).put("username",result.optString("uname",result.optString("username","B服 계정"))).put("saved_at",System.currentTimeMillis()).put("login_method",method);
        profiles.upsert(next);session=profiles.active();game=null;
    }
    void refresh(){if(account!=null)account.setText(preview?"데모 계정":session==null?"계정을 연결해 주세요":maskedAccount());}
    void diagnose(){run(()->{
        JSONObject c=api.configure();JSONObject r=Api.check(api.sdk("issue/cipher/v3",Api.fields("cipher_type","bili_login_rsa")));
        if(Api.rsa("test",r.getString("cipher_key")).isEmpty())throw new Exception("RSA 테스트 실패");
        String sign=Api.sdkSign(Api.fields("b","2","a","1"));if(!sign.matches("[0-9a-f]{32}"))throw new Exception("서명 형식 오류");
        for(String bad:Arrays.asList("https://evil.example/?biz_key=bh3_cn&app_id=1&ticket=abcdefgh","https://webstatic.mihoyo.com/?biz_key=hk4e_cn&app_id=4&ticket=abcdefgh","https://webstatic.mihoyo.com/?biz_key=bh3_cn&app_id=1&ticket=abcdefgh&ticket=ijklmnop")){
            boolean rejected=false;try{Api.ticket(bad);}catch(Exception e){rejected=true;}if(!rejected)throw new Exception("QR 검증 테스트 실패");
        }
        final String options=c.optString("login_option","미제공");runOnUiThread(()->{if(detail!=null)detail.setText("서버 초기 설정: "+options+"\nSMS·SSO는 SDK에 존재하지만 이 진단에서 실제 계정으로 검증하지 않았습니다.");});
        android.util.Log.i("BiliLightTest","CONFIG_RSA_QR_TEST_OK");return "진단 성공: 설정 code=0 · RSA 키 code=0 · RSA 암호화 · QR 거부 테스트. 실제 로그인은 미검증.";
    });}
    void providerConsent(){if(preview)return;new AlertDialog.Builder(this).setTitle("외부 설정 서비스 사용").setMessage(DispatchProvider.DISCLOSURE).setNegativeButton("동의 철회",(d,w)->{DispatchProvider.prefs(this).edit().putBoolean("upstream_consent_v1",false).apply();render(3);}).setNeutralButton("닫기",null).setPositiveButton("동의하고 사용",(d,w)->{DispatchProvider.prefs(this).edit().putBoolean("upstream_consent_v1",true).apply();render(3);}).show();}
    void providerVersion(){if(preview)return;EditText input=new EditText(this);input.setSingleLine(true);input.setText(DispatchProvider.version(this));new AlertDialog.Builder(this).setTitle("설치된 중국 B服 게임 버전").setMessage("게임 업데이트 후 버전을 맞춰 주세요. 설정 조회가 성공해도 해당 버전 PC 접속을 보증하지 않습니다.").setView(input).setNegativeButton("취소",null).setPositiveButton("저장",(d,w)->{try{String version=DispatchProvider.validateVersion(input.getText().toString().trim());DispatchProvider.prefs(this).edit().putString("game_version",version).apply();render(3);}catch(Exception e){message(e.getMessage());}}).show();}
    void confirmQr(){if(!DispatchProvider.allowed(this)){providerConsent();return;}final String t;try{t=Api.ticket(value(qr));if(session==null)throw new Exception("먼저 계정 인증이 필요합니다.");}catch(Exception e){message(e.getMessage());return;}
        new AlertDialog.Builder(this).setTitle("중국 B服 PC 로그인 승인").setMessage("현재 저장된 계정으로 방금 스캔한 PC에 로그인을 승인합니다. 본인 PC의 QR인지 확인하세요.\n\n원본 HonkaiScanner 설정 서비스를 사용합니다. 게임 버전 "+DispatchProvider.version(this)+" · 계정/토큰은 설정 서비스로 보내지 않습니다.").setNegativeButton("취소",null).setPositiveButton("승인",(d,w)->{qr.setText("");run(()->{requireSession();game=api.gameLogin(session);try{api.approve(game,t);}catch(Exception e){try{profiles.record(false);}catch(Exception ignored){}throw e;}try{profiles.record(true);}catch(Exception ignored){}return "승인 API가 요청을 수락했습니다. PC에서 B服 캐릭터와 접속 성공 여부를 확인하세요.";});}).show();
    }
    void run(Task t){run(t,null);}
    void run(Task t,Runnable after){if(preview){message("디자인 미리보기에서는 네트워크 인증을 실행하지 않습니다.");return;}if(busy)return;busy=true;for(Button b:buttons)b.setEnabled(false);for(Button b:nav)b.setEnabled(false);state.setText("처리 중…");worker.submit(()->{
        String s;boolean ok=false;try{s=t.run();ok=true;}catch(Exception e){s="실패: "+Api.safe(e.getMessage()==null?e.getClass().getSimpleName():e.getMessage());}
        final String result=s;final boolean success=ok;runOnUiThread(()->{if(isFinishing()||isDestroyed())return;busy=false;for(Button b:buttons)b.setEnabled(true);for(Button b:nav)b.setEnabled(true);refresh();state.setText(result);
            if(success&&after!=null)after.run();else new AlertDialog.Builder(this).setTitle("처리 결과").setMessage(result).setPositiveButton("확인",null).show();
        });
    });}
    Intent biliIntent(boolean legacy){
        for(String pkg:Arrays.asList("tv.danmaku.bili","tv.danmaku.bilibilihd")){
            Intent i;
            if(legacy){i=new Intent("tv.danmaku.bili.action.AUTHORIZE").setComponent(new ComponentName(pkg,pkg+".activities.login.SSOActivity"));i.putExtra("package_name",getPackageName());}
            else{i=new Intent("tv.danmaku.bili.action.sso.authorize").setPackage(pkg);i.putExtra("target_subid","180");i.putExtra("target_appkey","d4761645d1632e8c");}
            if(getPackageManager().resolveActivity(i,0)!=null)return i;
        }return null;
    }
    void socialMenu(){
        if(social==null||isFinishing())return;
        LinearLayout panel=new LinearLayout(this);panel.setOrientation(1);panel.setPadding(dp(18),dp(12),dp(18),dp(12));
        TextView info=new TextView(this);info.setText(social.summary());panel.addView(info);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("기타 / 소셜 로그인").setView(panel).setNegativeButton("닫기",null).create();
        Intent modern=biliIntent(false),legacy=biliIntent(true);
        Button bili=new Button(this);bili.setText(modern==null?"Bilibili 앱 SSO · 지원 앱 없음":"Bilibili 앱 SSO");bili.setEnabled(modern!=null);bili.setOnClickListener(v->{dialog.dismiss();startBili(modern);});panel.addView(bili);
        Button old=new Button(this);old.setText(legacy==null?"구형 Bilibili 인증 · 설치 앱에서 미지원":"구형 Bilibili 인증 (호환 모드)");old.setEnabled(legacy!=null);old.setOnClickListener(v->{dialog.dismiss();startBili(legacy);});panel.addView(old);
        Button apple=new Button(this);apple.setText(social.appleEnabled()?"Apple로 로그인 · 추가 검증 필요":"Apple로 로그인 · 현재 서버 미제공");apple.setEnabled(social.appleEnabled());apple.setOnClickListener(v->{dialog.dismiss();startActivityForResult(new Intent(this,AppleAuthActivity.class).putExtra("client",social.third.optString("apple_web_client_id")).putExtra("redirect",social.third.optString("redirect_url")),30);});panel.addView(apple);
        dialog.show();
    }
    void startBili(Intent i){
        if(i==null||busy||preview)return;
        try{startActivityForResult(i,10);}catch(ActivityNotFoundException|SecurityException e){message("Bilibili 앱이 이 인증 요청을 처리하지 못했습니다. 앱 버전과 연동 등록 상태를 확인해야 합니다.");}
    }
    JSONObject challenge(JSONObject model)throws Exception{
        challengeWait=new CountDownLatch(1);challengeAnswer=null;runOnUiThread(()->openCaptcha(model));
        try{if(!challengeWait.await(180,TimeUnit.SECONDS))throw new Exception("캡차 대기 시간이 초과됐습니다.");return challengeAnswer;}
        finally{runOnUiThread(()->{if(captchaDialog!=null)captchaDialog.dismiss();if(captchaView!=null){captchaView.removeJavascriptInterface("BiliJsObject");captchaView.destroy();captchaView=null;}});}
    }
    void openCaptcha(JSONObject model){
        if(isFinishing()||isDestroyed()){challengeWait.countDown();return;}
        String type=model.optString("captcha_type");
        if(!Arrays.asList("1","2","geetest").contains(type)){message("지원되지 않는 추가 인증 유형: "+type);challengeWait.countDown();return;}
        String page=model.optString("_captcha_page").equals("geetest_v2")?"geetest_v2":"geetest";
        Uri.Builder u=Uri.parse("https://game.bilibili.com/sdk/"+page+"/").buildUpon().appendQueryParameter("captcha_type",type);
        for(String k:Arrays.asList("challenge","gt","gs"))if(model.has(k))u.appendQueryParameter(k,model.optString(k));
        if(type.equals("1"))u.appendQueryParameter("userid",model.optString("gt_user_id"));
        if(type.equals("2"))u.appendQueryParameter("url",model.optString("url")).appendQueryParameter("token",model.optString("image_token"));
        captchaView=new WebView(this);WebSettings settings=captchaView.getSettings();settings.setJavaScriptEnabled(true);settings.setDomStorageEnabled(true);settings.setAllowFileAccess(false);settings.setAllowContentAccess(false);settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        captchaView.addJavascriptInterface(new Object(){@JavascriptInterface public void finishWithResult(String s){try{if(s!=null&&s.length()<32768&&!s.equals("{}"))challengeAnswer=new JSONObject(s);}catch(Exception ignored){}finally{challengeWait.countDown();}}},"BiliJsObject");
        captchaView.setWebViewClient(new WebViewClient(){@Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){Uri x=r.getUrl();return !"https".equals(x.getScheme())||!"game.bilibili.com".equals(x.getHost())||!x.getPath().startsWith("/sdk/");}});
        captchaDialog=new AlertDialog.Builder(this).setTitle("Bilibili 추가 인증").setView(captchaView).setNegativeButton("취소",(d,w)->challengeWait.countDown()).setOnCancelListener(d->challengeWait.countDown()).create();captchaDialog.show();captchaView.loadUrl(u.build().toString());
    }
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(result==RESULT_CANCELED||data==null){if(request==30)message(data==null?"Apple 인증 취소":Api.safe(data.getStringExtra("error")==null?"Apple 인증 취소":data.getStringExtra("error")));if(request==10)message("Bilibili SSO에서 인증 코드가 반환되지 않았습니다. 취소했거나 Bilibili 앱에서 인증을 거부했을 수 있습니다.");return;}
        if(request==40){Uri selected=data.getData();final String[] decoded=new String[1];run(()->{decoded[0]=ImageQr.read(getContentResolver(),selected);return "이미지 QR 인식 완료";},()->{render(1);qr.setText(decoded[0]);message("QR을 읽었습니다. 본인 PC인지 확인한 뒤 승인하세요.");});}
        if(request==30){String code=data.getStringExtra("code");run(()->{accept(api.appleLogin(code),"Apple");return "Apple 계정 인증 성공. B服 게임 토큰을 확인하세요.";});}
        if(request==20){render(1);qr.setText(data.getStringExtra("qr"));message("QR을 인식했습니다. 주소를 확인한 뒤 승인하세요.");}
        if(request==10){String code=data.getStringExtra("code"),access=data.getStringExtra("access_key");if(code!=null&&!code.isEmpty())run(()->{accept(api.exchange(code,true),"SSO");return "Bilibili SSO 계정 인증 성공";});else if(access!=null&&!access.isEmpty())run(()->{accept(api.exchange(access,false),"Bilibili 연동");return "Bilibili 연동 계정 인증 성공";});else message("Bilibili 앱이 지원되는 인증 결과를 반환하지 않았습니다.");}
    }
    @Override protected void onDestroy(){if(challengeWait!=null)challengeWait.countDown();worker.shutdownNow();super.onDestroy();}
    int dp(int n){return (int)(n*getResources().getDisplayMetrics().density);}
    TextView text(String s,int size){TextView v=Ui.label(this,s,size,Ui.TEXT,size>=17);v.setPadding(0,dp(4),0,dp(5));body.addView(v);return v;}
    void section(String s){text(s,19);}
    EditText edit(String hint,int type){EditText e=Ui.input(this,hint,type);body.addView(e);return e;}
    void button(String s,Runnable action){Button b=Ui.button(this,s,true);b.setOnClickListener(v->action.run());body.addView(b);buttons.add(b);}
    String value(EditText e){return e.getText().toString().trim();}
    void message(String s){state.setText(s==null?"오류":s);Toast.makeText(this,s==null?"오류":s,Toast.LENGTH_LONG).show();}
}
