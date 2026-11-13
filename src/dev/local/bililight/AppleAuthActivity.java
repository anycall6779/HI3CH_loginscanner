package dev.local.bililight;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.webkit.*;
import android.view.WindowManager;
import android.widget.*;
import java.security.SecureRandom;
import android.util.Base64;

/** SDK Apple web flow, with HTTPS/origin/state checks and no JavaScript credential bridge. */
public final class AppleAuthActivity extends Activity {
    WebView web;Uri redirect;String state;boolean consumed;
    static boolean allowedRedirect(String url){
        try{
            Uri u=Uri.parse(url);String h=u.getHost();
            return "https".equals(u.getScheme())&&h!=null&&u.getUserInfo()==null&&u.getPort()==-1&&(h.equals("bilibili.com")||h.endsWith(".bilibili.com")||h.equals("biligame.com")||h.endsWith(".biligame.com")||h.equals("biligame.net")||h.endsWith(".biligame.net"))&&u.getFragment()==null&&u.getQuery()==null;
        }catch(Exception e){return false;}
    }
    static boolean sameEndpoint(Uri a,Uri b){return a!=null&&b!=null&&a.getScheme().equals(b.getScheme())&&a.getHost().equals(b.getHost())&&a.getPort()==b.getPort()&&java.util.Objects.equals(a.getEncodedPath(),b.getEncodedPath());}
    @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        String client=getIntent().getStringExtra("client"),callback=getIntent().getStringExtra("redirect");
        if(client==null||client.isEmpty()||client.length()>256||!allowedRedirect(callback)){fail("유효한 Apple 연동 설정이 없습니다.");return;}
        redirect=Uri.parse(callback);byte[] nonce=new byte[32];new SecureRandom().nextBytes(nonce);state=Base64.encodeToString(nonce,Base64.URL_SAFE|Base64.NO_WRAP|Base64.NO_PADDING);
        LinearLayout root=new LinearLayout(this);root.setOrientation(1);TextView notice=new TextView(this);notice.setText("Apple 공식 페이지에서 직접 로그인하세요. 이 앱은 비밀번호를 읽거나 저장하지 않습니다.");notice.setPadding(20,30,20,20);root.addView(notice);
        web=new WebView(this);root.addView(web,new LinearLayout.LayoutParams(-1,0,1));Button cancel=new Button(this);cancel.setText("인증 취소");cancel.setOnClickListener(v->finish());root.addView(cancel);setContentView(root);
        WebSettings s=web.getSettings();s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);s.setAllowFileAccess(false);s.setAllowContentAccess(false);s.setSaveFormData(false);s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        web.setWebViewClient(new WebViewClient(){
            @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest request){return navigate(request.getUrl());}
            @Override public boolean shouldOverrideUrlLoading(WebView v,String url){return navigate(Uri.parse(url));}
            @Override public void onPageStarted(WebView v,String url,android.graphics.Bitmap icon){navigate(Uri.parse(url));}
        });
        String url=Uri.parse("https://appleid.apple.com/auth/authorize").buildUpon().appendQueryParameter("client_id",client).appendQueryParameter("redirect_uri",callback).appendQueryParameter("response_type","code").appendQueryParameter("response_mode","fragment").appendQueryParameter("state",state).build().toString();web.loadUrl(url);
    }
    boolean navigate(Uri u){
        if(consumed)return true;
        if(sameEndpoint(redirect,u)){
            consumed=true;if(web!=null)web.stopLoading();
            try{
                Uri fragment=Uri.parse("https://callback.invalid/?"+(u.getEncodedFragment()==null?"":u.getEncodedFragment()));
                if(fragment.getQueryParameters("state").size()!=1||!state.equals(fragment.getQueryParameter("state")))throw new IllegalArgumentException("Apple 콜백 state 검증 실패");
                String error=fragment.getQueryParameter("error");if(error!=null)throw new IllegalArgumentException("Apple 인증이 취소되었거나 거부되었습니다.");
                String code=fragment.getQueryParameter("code");if(fragment.getQueryParameters("code").size()!=1||code==null||code.isEmpty()||code.length()>16384)throw new IllegalArgumentException("Apple 인증 코드 누락");
                setResult(RESULT_OK,new Intent().putExtra("code",code));finish();
            }catch(Exception e){fail(e.getMessage());}return true;
        }
        String h=u.getHost();boolean allowed="https".equals(u.getScheme())&&u.getUserInfo()==null&&u.getPort()==-1&&h!=null&&(h.equals("apple.com")||h.endsWith(".apple.com"));
        if(!allowed){if(web!=null)web.stopLoading();fail("예상하지 않은 인증 페이지 이동을 차단했습니다.");return true;}return false;
    }
    void fail(String reason){setResult(RESULT_CANCELED,new Intent().putExtra("error",reason==null?"Apple 인증 실패":reason));finish();}
    @Override protected void onDestroy(){if(web!=null){web.stopLoading();web.destroy();web=null;}super.onDestroy();}
}
