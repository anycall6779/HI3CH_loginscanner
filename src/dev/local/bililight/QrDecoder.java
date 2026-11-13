package dev.local.bililight;

import com.google.zxing.*;
import com.google.zxing.common.*;
import java.util.*;
import java.io.IOException;

/** Full field first, then overlapping ROIs. Original pixels are retained, not downscaled. */
final class QrDecoder {
    static String decode(byte[] luma,int width,int height)throws Exception{
        if(width<32||height<32||(long)width*height>luma.length)throw new IOException("잘못된 카메라 프레임");
        long start=android.os.SystemClock.elapsedRealtime();
        MultiFormatReader reader=new MultiFormatReader();Map<DecodeHintType,Object> hints=new EnumMap<>(DecodeHintType.class);
        hints.put(DecodeHintType.POSSIBLE_FORMATS,Collections.singletonList(BarcodeFormat.QR_CODE));hints.put(DecodeHintType.TRY_HARDER,true);reader.setHints(hints);
        // Crop coordinates are in the sensor frame, including off-center areas.
        float[][] regions={{0,0,1,1},{.2f,.2f,.6f,.6f},{.35f,.35f,.3f,.3f},{0,0,.6f,.6f},{.4f,0,.6f,.6f},{0,.4f,.6f,.6f},{.4f,.4f,.6f,.6f}};
        boolean invalid=false;
        for(int index=0;index<regions.length;index++){
            if(index>1&&android.os.SystemClock.elapsedRealtime()-start>1000)break;
            float[] region=regions[index];int x=(int)(region[0]*width),y=(int)(region[1]*height),w=Math.min(width-x,(int)(region[2]*width)),h=Math.min(height-y,(int)(region[3]*height));
            LuminanceSource crop=new PlanarYUVLuminanceSource(luma,width,height,x,y,w,h,false);
            for(int variant=0;variant<4;variant++){
                LuminanceSource source=variant==1?contrast(crop):variant==3?crop.invert():crop;
                try{String text=reader.decodeWithState(new BinaryBitmap(variant==2?new GlobalHistogramBinarizer(source):new HybridBinarizer(source))).getText();Api.ticket(text);return text;}
                catch(NotFoundException|ChecksumException|FormatException ignored){}
                catch(IOException e){invalid=true;}
                finally{reader.reset();}
            }
            // Enlarging a central ROI changes adaptive threshold granularity; it cannot restore lost detail.
            if(index==2&&Math.max(w,h)<1200){
                LuminanceSource enlarged=scale2(contrast(crop));
                try{String text=reader.decodeWithState(new BinaryBitmap(new HybridBinarizer(enlarged))).getText();Api.ticket(text);return text;}
                catch(NotFoundException|ChecksumException|FormatException ignored){}
                catch(IOException e){invalid=true;}
                finally{reader.reset();}
            }
        }
        if(invalid)throw new IOException("중국 붕괴3 공식 게임 QR만 지원합니다.");
        throw NotFoundException.getNotFoundInstance();
    }
    private static LuminanceSource contrast(LuminanceSource source){
        byte[] pixels=source.getMatrix();int min=255,max=0;
        for(byte pixel:pixels){int v=pixel&255;if(v<min)min=v;if(v>max)max=v;}
        if(max-min<4||max-min>=192)return source;
        byte[] adjusted=new byte[pixels.length];int range=max-min;for(int i=0;i<pixels.length;i++)adjusted[i]=(byte)(((pixels[i]&255)-min)*255/range);
        return new PlanarYUVLuminanceSource(adjusted,source.getWidth(),source.getHeight(),0,0,source.getWidth(),source.getHeight(),false);
    }
    private static LuminanceSource scale2(LuminanceSource source){
        int w=source.getWidth(),h=source.getHeight();byte[] old=source.getMatrix(),scaled=new byte[w*h*4];
        for(int y=0;y<h;y++)for(int x=0;x<w;x++){byte v=old[y*w+x];int i=y*2*w*2+x*2;scaled[i]=v;scaled[i+1]=v;scaled[i+w*2]=v;scaled[i+w*2+1]=v;}
        return new PlanarYUVLuminanceSource(scaled,w*2,h*2,0,0,w*2,h*2,false);
    }
}
