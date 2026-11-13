package dev.local.bililight;

import com.google.zxing.*;
import com.google.zxing.common.*;
import com.google.zxing.qrcode.QRCodeWriter;
import java.util.*;

/** Synthetic camera frames, not a claim about physical distance or camera optics. */
final class SmallQrTests {
    static String run()throws Exception{
        String url="https://webstatic.mihoyo.com/qr_code_in_game.html?biz_key=bh3_cn&app_id=1&ticket=LOCAL_TEST_1234";
        int w=1920,h=1080,side=144;BitMatrix qr=new QRCodeWriter().encode(url,BarcodeFormat.QR_CODE,side,side);
        byte[] frame=new byte[w*h];Arrays.fill(frame,(byte)150);
        for(int y=0;y<side;y++)for(int x=0;x<side;x++)frame[((h-side)/2+y)*w+(w-side)/2+x]=(byte)(qr.get(x,y)?130:150);
        boolean baseline=false;
        try{Map<DecodeHintType,Object> hints=new EnumMap<>(DecodeHintType.class);hints.put(DecodeHintType.POSSIBLE_FORMATS,Collections.singletonList(BarcodeFormat.QR_CODE));hints.put(DecodeHintType.TRY_HARDER,true);baseline=url.equals(new MultiFormatReader().decode(new BinaryBitmap(new HybridBinarizer(new PlanarYUVLuminanceSource(frame,w,h,0,0,w,h,false))),hints).getText());}catch(NotFoundException ignored){}
        long time=android.os.SystemClock.elapsedRealtime();LocalTests.require(url.equals(QrDecoder.decode(frame,w,h)),"small low-contrast camera QR");long elapsed=android.os.SystemClock.elapsedRealtime()-time;
        Arrays.fill(frame,(byte)240);int left=150,top=100;for(int y=0;y<side;y++)for(int x=0;x<side;x++)frame[(top+y)*w+left+x]=(byte)(qr.get(x,y)?20:240);
        LocalTests.require(url.equals(QrDecoder.decode(frame,w,h)),"off-center camera QR");
        String result="SMALL_QR_TEST_OK frame=1920x1080 box=144x144 low_contrast=130:150 baseline="+baseline+" enhanced_ms="+elapsed+" off_center=true";
        android.util.Log.i("BiliLightTest",result);return result;
    }
}
