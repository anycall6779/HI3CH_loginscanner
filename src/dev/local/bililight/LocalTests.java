package dev.local.bililight;

import android.content.Context;
import android.graphics.Bitmap;
import org.json.JSONObject;
import com.google.zxing.*;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.common.BitMatrix;

/** Tests only synthetic identities in a separately named vault; never loads the user's session. */
final class LocalTests {
    static void require(boolean ok,String name)throws Exception{if(!ok)throw new Exception("로컬 검사 실패: "+name);}
    static String run(Context context)throws Exception{
        Vault v=new Vault(context,"local-tests.aesgcm");
        try{
            v.save(new JSONObject().put("uid","1").put("access_key","LOCAL_TEST_ONLY").put("username","Demo One"));
            Profiles p=new Profiles(v);p.load();require(p.list().length()==1&&p.active().getString("uid").equals("1"),"legacy migration");
            p.upsert(new JSONObject().put("uid","2").put("access_key","LOCAL_TEST_ONLY_2").put("username","Demo Two"));require(p.list().length()==2&&p.active().getString("uid").equals("2"),"second profile");
            p.select(0);p.alias(0,"테스트 별칭");Profiles restored=new Profiles(v);restored.load();require(restored.active().getString("alias").equals("테스트 별칭"),"encrypted roundtrip");
            restored.record(true);require(restored.history().length()==1&&restored.history().getJSONObject(0).length()==2,"minimal history");restored.remove(0);require(restored.active().getString("uid").equals("2"),"profile removal");
            String url="https://webstatic.mihoyo.com/qr_code_in_game.html?biz_key=bh3_cn&app_id=1&ticket=LOCAL_TEST_1234";
            BitMatrix matrix=new QRCodeWriter().encode(url,BarcodeFormat.QR_CODE,512,512);int[] pixels=new int[512*512];for(int y=0;y<512;y++)for(int x=0;x<512;x++)pixels[y*512+x]=matrix.get(x,y)?0xff000000:0xffffffff;
            Bitmap bitmap=Bitmap.createBitmap(pixels,512,512,Bitmap.Config.ARGB_8888);require(url.equals(ImageQr.decode(bitmap)),"gallery QR decoder");
            require(Api.sdkSign(Api.fields("b","2","a","1")).equals("fa6aab4fb56c851d908fb5c6231a7f90"),"SDK signature vector");
            JSONObject signed=new JSONObject().put("app_id",1).put("device","test");Api.signGame(signed);require(signed.getString("sign").equals("ac2e1c72463f226ef93bc2d83a79d48bee298ad62eae683941d79eb2b9a31ed5"),"game signature vector");
            SmallQrTests.run();android.util.Log.i("BiliLightTest","LOCAL_PROFILE_IMAGE_SIGNATURE_TESTS_OK");return "로컬 검사 통과: 기존 세션 변환 · 다중 계정 · 암호화 · 이미지 QR · 작은 저명암 QR · 화면 가장자리 QR · 서명 벡터";
        }finally{v.clear();}
    }
}
