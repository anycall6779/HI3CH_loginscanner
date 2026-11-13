package dev.local.bililight;

import android.content.Context;
import android.net.Uri;
import android.util.Base64;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;
import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;

/** Protocol observed in the installed B服 9.1.0 / SDK 5.9.0. No telemetry. */
public final class Api {
    static final String SDK_KEY="dbf8f1b4496f430b8a3c0f436a35b931";
    static final String GAME_KEY="0ebc517adb1b62c6b408df153331f9aa";
    static final String FEIGN_KEY="MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCb0lFxD1OJX+sDvG1nn+QGi95wIRP71Y0r2js47NgTmYnMKKtwVbRdcpvc/MYeOQP1OUCUcQCBJaKWpTtsuIeFPgXYlIq7looKqxL5xzB2quEAinRVRTWaPpD5v0Re4n4b6SZmEr4BjVg2QhVrVE8gHWkFQlRZwf5f6TrrkdP+BQIDAQAB";
    static final String HOST="line3-sdk-center-login-sh.biligame.net";
    static final String PREFIX="/api/external/";
    final String device;
    final Context context;
    JSONObject config;
    public interface Challenge { JSONObject solve(JSONObject model) throws Exception; }
    Challenge challenge;
    Api(Context context) {
        this.context=context.getApplicationContext();
        android.content.SharedPreferences p=context.getSharedPreferences("identity",0);
        String id=p.getString("device",null);
        if(id==null){id=UUID.randomUUID().toString();p.edit().putString("device",id).apply();}
        device=id;
    }
    static TreeMap<String,String> fields(String... kv) {
        TreeMap<String,String> r=new TreeMap<>();
        if(kv.length%2!=0)throw new IllegalArgumentException("key/value pairs required");
        for(int i=0;i<kv.length;i+=2)r.put(kv[i],kv[i+1]);return r;
    }
    static String hex(byte[] b){StringBuilder s=new StringBuilder();for(byte v:b)s.append(String.format(Locale.ROOT,"%02x",v&255));return s.toString();}
    static String sdkSign(Map<String,String> p)throws Exception{
        StringBuilder s=new StringBuilder();
        for(Map.Entry<String,String> e:new TreeMap<>(p).entrySet())
            if(!Arrays.asList("item_name","item_desc","feign_sign","token","sign").contains(e.getKey().toLowerCase(Locale.ROOT)))s.append(e.getValue());
        return hex(MessageDigest.getInstance("MD5").digest((s+SDK_KEY).getBytes(StandardCharsets.UTF_8)));
    }
    static String rsa(String message,String key)throws Exception{
        key=key.replace("-----BEGIN PUBLIC KEY-----","").replace("-----END PUBLIC KEY-----","").replaceAll("\\s","");
        Cipher c=Cipher.getInstance("RSA/ECB/PKCS1Padding");
        c.init(Cipher.ENCRYPT_MODE,KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(Base64.decode(key,Base64.DEFAULT))));
        return Base64.encodeToString(c.doFinal(message.getBytes(StandardCharsets.UTF_8)),Base64.NO_WRAP);
    }
    static String enc(String s)throws Exception{return URLEncoder.encode(s,"UTF-8");}
    static String form(Map<String,String> p)throws Exception{
        StringBuilder s=new StringBuilder();for(Map.Entry<String,String> e:p.entrySet()){
            if(e.getValue()==null||e.getValue().isEmpty())continue;
            if(s.length()>0)s.append('&');s.append(enc(e.getKey())).append('=').append(enc(e.getValue()));
        }return s.toString();
    }
    static JSONObject http(String target,String body,boolean json)throws Exception{
        URL url=new URL(target);
        if(!"https".equals(url.getProtocol()))throw new IOException("HTTPS required");
        HttpURLConnection c=(HttpURLConnection)url.openConnection();
        c.setConnectTimeout(15000);c.setReadTimeout(20000);c.setInstanceFollowRedirects(false);
        c.setRequestMethod("POST");c.setRequestProperty("User-Agent","Mozilla/5.0 BSGameSDK");
        c.setRequestProperty("Content-Type",json?"application/json; charset=utf-8":"application/x-www-form-urlencoded; charset=utf-8");
        c.setDoOutput(true);
        try{
            try(OutputStream out=c.getOutputStream()){out.write(body.getBytes(StandardCharsets.UTF_8));}
            int code=c.getResponseCode();
            if(code<200||code>=300)throw new IOException("HTTP "+code+" ("+url.getHost()+")");
            ByteArrayOutputStream out=new ByteArrayOutputStream();
            try(InputStream in=c.getInputStream()){byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1){out.write(b,0,n);if(out.size()>2_000_000)throw new IOException("Response too large");}}
            try{return new JSONObject(out.toString("UTF-8"));}catch(JSONException e){throw new IOException("서버가 JSON이 아닌 응답을 반환했습니다.");}
        }finally{c.disconnect();}
    }
    JSONObject sdk(String path,Map<String,String> input)throws Exception{
        TreeMap<String,String> params=new TreeMap<>(input);
        for(int attempt=0;attempt<2;attempt++){
            TreeMap<String,String> p=fields("merchant_id","777","app_id","180","game_id","180","server_id","378",
                "sdk_ver","5.9.0","sdk_type","1","sdk_log_type","1","platform","3","platform_type","3",
                "channel_id","1","current_env","0","version","3","domain",HOST,"timestamp",Long.toString(System.currentTimeMillis()));
            p.putAll(params);p.put("sign",sdkSign(p));
            if(path.equals("login/otp/v3"))p.put("feign_sign",rsa(p.get("sign"),FEIGN_KEY));
            JSONObject result=http("https://"+HOST+PREFIX+path,form(p),false);
            String code=result.optString("code","");
            if(!Arrays.asList("200000","200001","200005","200006","200007").contains(code))return result;
            if(attempt>0||challenge==null)throw new IOException("캡차 인증이 필요합니다 ("+code+").");
            JSONObject model=code.equals("200006")?sdkNoChallenge("start_captcha/v3",fields("captcha_category","acctAntiFraud","version","3")):
                httpSdkLegacyCaptcha();
            check(model);
            model.put("_captcha_page",code.equals("200006")?"geetest_v2":"geetest");
            JSONObject answer=challenge.solve(model);
            if(answer==null)throw new IOException("캡차 인증이 취소되었습니다.");
            String type=model.optString("captcha_type");
            params.put("captcha_type",type);
            params.put("ctoken",type.equals("1")?model.optString("gt_user_id"):type.equals("2")?model.optString("image_token"):answer.optString("ctoken"));
            for(String k:Arrays.asList("challenge","validate","seccode"))params.put(k,answer.optString(k));
            params.put("img_code",type.equals("2")?answer.optString("captcha"):answer.optString("code"));
            params.put("gt_user_id",answer.optString("userid"));params.put("captcha_code",answer.optString("captcha"));params.put("image_token",answer.optString("token"));
        }throw new IOException("인증 재시도 제한");
    }
    private JSONObject sdkNoChallenge(String path,Map<String,String> p)throws Exception{
        // These non-login requests must never recursively open another challenge.
        return basicSdk(PREFIX+path,p);
    }
    private JSONObject httpSdkLegacyCaptcha()throws Exception{return basicSdk("/api/client/start_captcha",fields("version","1"));}
    private JSONObject basicSdk(String path,Map<String,String> extra)throws Exception{
        TreeMap<String,String> p=fields("merchant_id","777","app_id","180","game_id","180","server_id","378","sdk_ver","5.9.0","sdk_type","1","sdk_log_type","1","platform","3","platform_type","3","channel_id","1","current_env","0","version","3","domain",HOST,"timestamp",Long.toString(System.currentTimeMillis()));
        p.putAll(extra);p.put("sign",sdkSign(p));return http("https://"+HOST+path,form(p),false);
    }
    static JSONObject check(JSONObject r)throws Exception{
        if(!r.has("code")||!r.optString("code").equals("0"))throw new IOException("인증 서버 오류 "+r.optString("code","unknown")+": "+safe(r.optString("message",r.optString("server_message","추가 인증/설정 확인 필요"))));return r;
    }
    static String safe(String s){s=s.replaceAll("(?i)(access[_ ]?(key|token)|combo_token|refresh_token|password|pwd)[\\s\\\"=:]+[^ ,;\\\"}]+","[redacted]");return s.length()>240?s.substring(0,240):s;}
    JSONObject configure()throws Exception{config=check(sdk("config/v3",fields()));return config;}
    JSONObject password(String account,String password)throws Exception{
        JSONObject key=check(sdk("issue/cipher/v3",fields("cipher_type","bili_login_rsa")));
        String encrypted=rsa(key.getString("hash")+password,key.getString("cipher_key"));
        return check(sdk("login/v3",fields("user_id",account,"pwd",enc(encrypted))));
    }
    JSONObject sendSms(String country,String phone)throws Exception{return check(sdk("otp/send/v3",fields("otp_type","login","otp_channel_no",phone,"country_code",country,"otp_channel_category","tel")));}
    JSONObject sms(String country,String phone,String key,String code)throws Exception{return check(sdk("login/otp/v3",fields("country_code",country,"mobile",phone,"captcha_key",key,"otp",code)));}
    JSONObject exchange(String token,boolean sso)throws Exception{return check(sdk("token.exchange/v3",fields("access_key",token,"key_type",sso?"2":"0","acct_exchange_type",sso?"11":"10")));}
    JSONObject thirdConfig()throws Exception{return check(sdk("third.config/v3",fields()));}
    JSONObject appleLogin(String code)throws Exception{
        if(code==null||code.isEmpty()||code.length()>16384)throw new IOException("Apple 인증 코드 누락/형식 오류");
        return check(sdk("third/login/v3",fields("apple_type","2","id_token",code)));
    }
    JSONObject gameLogin(JSONObject session)throws Exception{
        // Current forBzhan.PluginImpl sends a JSON Long, not a quoted UID string.
        JSONObject account=new JSONObject().put("uid",Long.parseLong(session.getString("uid"))).put("access_key",session.getString("access_key"));
        JSONObject request=new JSONObject().put("app_id",1).put("channel_id",14).put("device",device).put("data",account.toString());
        signGame(request);JSONObject r=http("https://api-sdk.mihoyo.com/bh3_cn/combo/granter/login/v2/login",request.toString(),true);
        event("game_token",r.optString("retcode","missing"));checkGame(r);JSONObject d=r.getJSONObject("data");
        if(d.has("channel_id")&&!d.optString("channel_id").equals("14"))throw new IOException("B服가 아닌 응답을 거부했습니다.");
        if(d.optString("combo_token").isEmpty()||d.optString("open_id").isEmpty())throw new IOException("게임 토큰 응답 누락");
        return d;
    }
    static void signGame(JSONObject p)throws Exception{
        TreeMap<String,String> values=new TreeMap<>();for(Iterator<String> i=p.keys();i.hasNext();){String k=i.next();if(!k.equals("sign"))values.put(k,p.get(k).toString());}
        StringBuilder message=new StringBuilder();for(Map.Entry<String,String> e:values.entrySet()){if(message.length()>0)message.append('&');message.append(e.getKey()).append('=').append(e.getValue());}
        Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(GAME_KEY.getBytes(StandardCharsets.UTF_8),"HmacSHA256"));p.put("sign",hex(mac.doFinal(message.toString().getBytes(StandardCharsets.UTF_8))));
    }
    static void checkGame(JSONObject r)throws Exception{if(!r.has("retcode")||r.optInt("retcode",-1)!=0)throw new IOException("게임 인증 오류 "+r.optString("retcode","unknown")+": "+safe(r.optString("message")));}
    static void event(String stage,String code){android.util.Log.i("BiliLightStage",stage+" retcode="+code.replaceAll("[^0-9-]","?"));}
    static String ticket(String url)throws Exception{
        if(url==null||url.length()>8192)throw new IOException("잘못된 QR");Uri u=Uri.parse(url);String h=u.getHost();
        if(!"https".equals(u.getScheme())||h==null||!(h.equals("mihoyo.com")||h.endsWith(".mihoyo.com"))||u.getUserInfo()!=null||u.getPort()!=-1)throw new IOException("미호요 공식 HTTPS 게임 QR만 허용합니다.");
        if(!"bh3_cn".equals(u.getQueryParameter("biz_key"))||!"1".equals(u.getQueryParameter("app_id")))throw new IOException("중국 붕괴3 QR이 아닙니다. 다른 게임/국제서버 QR은 거부합니다.");
        String t=u.getQueryParameter("ticket");if(t==null||!t.matches("[A-Za-z0-9_-]{8,256}"))throw new IOException("QR ticket 누락/형식 오류");
        if(u.getQueryParameters("ticket").size()!=1||u.getQueryParameters("biz_key").size()!=1||u.getQueryParameters("app_id").size()!=1)throw new IOException("중복 QR 파라미터");
        return t;
    }
    void approve(JSONObject game,String t)throws Exception{
        String dispatch=DispatchProvider.fetch(context); // Fetch before scan; no fixture or stale fallback.
        JSONObject scan=new JSONObject().put("device",device).put("app_id",1).put("ts",System.currentTimeMillis()).put("ticket",t);signGame(scan);
        JSONObject scanned=http("https://api-sdk.mihoyo.com/bh3_cn/combo/panda/qrcode/scan",scan.toString(),true);event("qr_scan",scanned.optString("retcode","missing"));checkGame(scanned);
        JSONObject raw=new JSONObject().put("heartbeat",false).put("open_id",game.getString("open_id")).put("combo_id",game.getString("combo_id")).put("combo_token",game.getString("combo_token")).put("device_id",device).put("app_id","1").put("channel_id","14").put("account_type",game.optString("account_type","2")).put("asterisk_name","HONKAI QR");
        JSONObject extData=new JSONObject().put("accountType",2).put("accountID",game.getString("open_id")).put("accountToken",game.getString("combo_token")).put("dispatch",dispatch);
        JSONObject ext=new JSONObject().put("data",extData);
        JSONObject payload=new JSONObject().put("proto","Combo").put("raw",raw.toString()).put("ext",ext.toString());
        JSONObject confirm=new JSONObject().put("device",device).put("app_id",1).put("ts",System.currentTimeMillis()).put("ticket",t).put("payload",payload);signGame(confirm);
        JSONObject confirmed=http("https://api-sdk.mihoyo.com/bh3_cn/combo/panda/qrcode/confirm",confirm.toString(),true);event("qr_confirm",confirmed.optString("retcode","missing"));checkGame(confirmed);
        // A zero retcode means only that the approval API accepted it, NOT that the PC entered the game.
    }
}
