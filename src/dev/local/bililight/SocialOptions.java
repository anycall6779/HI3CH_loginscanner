package dev.local.bililight;

import org.json.*;

/** Only provider types traced in the installed game SDK; never invent OAuth clients. */
final class SocialOptions {
    final JSONObject config,third;
    SocialOptions(JSONObject config,JSONObject third){this.config=config;this.third=third;}
    boolean has(String type){
        Object value=config.opt("login_option");
        try{
            JSONArray a=value instanceof JSONArray?(JSONArray)value:new JSONArray(String.valueOf(value));
            for(int i=0;i<a.length();i++)if(type.equals(a.getJSONObject(i).optString("loginType")))return true;
        }catch(JSONException ignored){}
        return false;
    }
    boolean appleEnabled(){return config.optInt("config_apple_login_switch",0)==1&&has("APPLE")&&!third.optString("apple_web_client_id").isEmpty()&&AppleAuthActivity.allowedRedirect(third.optString("redirect_url"));}
    String appleStatus(){
        if(config.optInt("config_apple_login_switch",0)!=1||!has("APPLE"))return "현재 B服 서버 설정에서 Apple 로그인을 제공하지 않습니다.";
        if(third.optString("apple_web_client_id").isEmpty()||third.optString("redirect_url").isEmpty())return "서버의 Apple 연동 ID 또는 콜백 주소가 비어 있습니다.";
        if(!AppleAuthActivity.allowedRedirect(third.optString("redirect_url")))return "서버 콜백 주소를 안전하게 확인할 수 없어 중단했습니다.";
        return "서버 제공 · 로그인 성공은 아직 별도 검증 필요";
    }
    String summary(){
        return "계정·비밀번호: "+(has("ACCT")?"서버 목록에 있음":"SDK 경로 · 서버 목록에는 없음")+
            "\nSMS: 실제 문자 발송 확인 · 방식별 인증 성공은 추가 검증 필요\nBilibili SSO: 설치 앱으로 인증 · 연동 오류 해결 미확인\nApple: "+appleStatus()+
            "\nQQ·위챗·Google 직접 연동: 현재 게임 SDK에서 경로/등록값 미확인\n게스트: 기존 B服 계정용이 아니므로 제공하지 않음";
    }
}
