package dev.local.bililight;

import org.json.*;

/** All profiles, aliases and minimal history share the existing encrypted vault. */
final class Profiles {
    final Vault vault;JSONObject root;
    Profiles(Vault vault)throws JSONException{this.vault=vault;resetMemory();}
    void resetMemory()throws JSONException{root=new JSONObject().put("schema",2).put("profiles",new JSONArray()).put("history",new JSONArray()).put("active","");}
    void load()throws Exception{
        JSONObject saved=vault.load();if(saved==null)return;
        if(saved.has("uid")&&saved.has("access_key")){
            // In-memory migration; the original encrypted file stays intact until a user action saves it.
            root.getJSONArray("profiles").put(saved);root.put("active",saved.getString("uid"));
        }else{
            if(saved.optInt("schema")!=2||!(saved.opt("profiles") instanceof JSONArray)||!(saved.opt("history") instanceof JSONArray))throw new Exception("저장 계정 형식을 확인할 수 없습니다.");
            root=saved;
        }
    }
    JSONArray list()throws JSONException{return root.getJSONArray("profiles");}
    JSONObject active()throws JSONException{
        JSONArray a=list();String id=root.optString("active");for(int i=0;i<a.length();i++)if(id.equals(a.getJSONObject(i).optString("uid")))return a.getJSONObject(i);return null;
    }
    void upsert(JSONObject value)throws Exception{
        JSONArray a=list();int found=-1;for(int i=0;i<a.length();i++)if(value.getString("uid").equals(a.getJSONObject(i).optString("uid")))found=i;
        if(found>=0){JSONObject old=a.getJSONObject(found);if(old.has("alias"))value.put("alias",old.getString("alias"));a.put(found,value);}
        else{if(a.length()>=20)throw new Exception("최대 20개 계정을 보관할 수 있습니다. 사용하지 않는 계정을 먼저 삭제하세요.");a.put(value);}
        root.put("active",value.getString("uid"));vault.save(root);
    }
    void select(int index)throws Exception{root.put("active",list().getJSONObject(index).getString("uid"));vault.save(root);}
    void alias(int index,String alias)throws Exception{
        alias=alias.trim();if(alias.length()>24||alias.matches("(?s).*\\p{Cntrl}.*"))throw new Exception("별칭은 제어 문자 없이 24자 이내로 입력하세요.");
        list().getJSONObject(index).put("alias",alias);vault.save(root);
    }
    void remove(int index)throws Exception{
        JSONArray a=list();String id=a.getJSONObject(index).getString("uid");a.remove(index);if(id.equals(root.optString("active")))root.put("active",a.length()==0?"":a.getJSONObject(0).getString("uid"));vault.save(root);
    }
    void record(boolean ok)throws Exception{
        JSONArray a=root.getJSONArray("history");a.put(new JSONObject().put("at",System.currentTimeMillis()).put("ok",ok));while(a.length()>50)a.remove(0);vault.save(root);
    }
    JSONArray history()throws JSONException{return root.getJSONArray("history");}
    void clearHistory()throws Exception{root.put("history",new JSONArray());vault.save(root);}
    void clear()throws JSONException{vault.clear();resetMemory();}
}
