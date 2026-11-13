package dev.local.bililight;

import android.content.Context;
import android.util.Base64;
import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** Upstream protocol adapter. Never accepts an account, token, QR or device identity. */
final class DispatchProvider {
    static final String HOST="api-v2.scanner.hellocraft.xyz";
    static final String DEFAULT_VERSION="9.1.0";
    static final String DISCLOSURE="원본 HonkaiScanner 설정 서비스("+HOST+")에서 게임 설정을 받습니다. 공식 서버 직접 조회가 아닙니다. 게임 버전·프로토콜용 고정값만 보내며 계정, 토큰, QR, 실제 기기 식별자는 보내지 않습니다. 서비스 운영자는 연결 IP와 요청 시각을 볼 수 있습니다. 서비스 중단·응답 변경 시 PC 승인이 중단됩니다. 설정 암호문은 구조만 검사하며 내부 내용을 독립적으로 검증하지 못합니다.";
    static android.content.SharedPreferences prefs(Context c){return c.getSharedPreferences("dispatch_provider",0);}
    static boolean allowed(Context c){return prefs(c).getBoolean("upstream_consent_v1",false);}
    static String version(Context c){return prefs(c).getString("game_version",DEFAULT_VERSION);}
    static String validateVersion(String s)throws IOException{
        if(s==null||!s.matches("[0-9]{1,2}\\.[0-9]{1,2}\\.[0-9]{1,2}"))throw new IOException("게임 버전은 9.1.0 형식으로 입력하세요.");return s;
    }
    static String hmac(Map<String,String> p,String key)throws Exception{
        StringBuilder s=new StringBuilder();for(Map.Entry<String,String> e:new TreeMap<>(p).entrySet()){
            if(e.getKey().equals("x-req-sign"))continue;if(s.length()>0)s.append('&');s.append(e.getKey()).append('=').append(e.getValue());
        }
        Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8),"HmacSHA256"));return Api.hex(mac.doFinal(s.toString().getBytes(StandardCharsets.UTF_8)));
    }
    static String validate(JSONObject r)throws Exception{
        if(!r.has("retcode")||r.optInt("retcode",-1)!=0)throw new IOException("설정 서비스 오류 "+r.optString("retcode","unknown"));
        Object data=r.opt("data");if(!(data instanceof String))throw new IOException("설정 응답 형식 오류");String value=(String)data;
        if(value.length()<256||value.length()>65536||value.length()%4!=0||!value.matches("[A-Za-z0-9+/]+={0,2}"))throw new IOException("설정 암호문 형식 오류");
        byte[] bytes=Base64.decode(value,Base64.NO_WRAP);
        if(bytes.length%16!=0||!Base64.encodeToString(bytes,Base64.NO_WRAP).equals(value))throw new IOException("설정 암호문 블록 오류");
        return value;
    }
    static String fetch(Context context)throws Exception{
        if(!allowed(context))throw new IOException("설정에서 원본 설정 서비스 사용에 동의하세요.");
        String version=validateVersion(version(context))+"_gf_android_bilibili";long now=System.currentTimeMillis();
        // The name/code identify the upstream wire protocol, not this app's version.
        TreeMap<String,String> p=Api.fields("x-req-openid",version+"_null","x-req-fp","deviceFp","x-req-name","1.9.8:135","x-req-code","135","x-req-version",version,"x-req-ts",Long.toString(now/2000)+"t");
        String reversed=new StringBuilder(hmac(p,Api.GAME_KEY)).reverse().toString();StringBuilder split=new StringBuilder(),salt=new StringBuilder();int count=0,offset=0;SecureRandom random=new SecureRandom();
        while(split.length()<64){int start=random.nextInt(63),end=Math.min(start+random.nextInt(4)+4,63);split.append(reversed.substring(start,end));salt.append(start).append('|').append(end).append('|');offset+=start+end;count++;}
        String key=split.substring(0,64);p.put("x-req-sign",key);String signature=key.substring(0,16)+hmac(p,key).substring(16);
        if(offset%2==0)offset=-offset;
        URL url=new URL("https://"+HOST+"/v4/query_dispatch/?version="+version+"&t="+(now+offset));
        HttpURLConnection c=(HttpURLConnection)url.openConnection();c.setConnectTimeout(10000);c.setReadTimeout(15000);c.setInstanceFollowRedirects(false);c.setUseCaches(false);
        for(Map.Entry<String,String> e:p.entrySet())if(!e.getKey().equals("x-req-ts"))c.setRequestProperty(e.getKey(),e.getValue());
        c.setRequestProperty("x-req-sign",signature);c.setRequestProperty("x-req-salt",count+"|"+salt);c.setRequestProperty("Accept","application/json");
        try{
            int status=c.getResponseCode();if(status!=200)throw new IOException("설정 서비스 HTTP "+status);
            ByteArrayOutputStream out=new ByteArrayOutputStream();try(InputStream in=c.getInputStream()){byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1){if(out.size()+n>100000)throw new IOException("설정 응답 크기 초과");out.write(b,0,n);}}
            String result=validate(new JSONObject(out.toString("UTF-8")));android.util.Log.i("BiliLightStage","dispatch_provider retcode=0 chars="+result.length());return result;
        }finally{c.disconnect();}
    }
}
