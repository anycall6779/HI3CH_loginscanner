package dev.local.bililight;

import android.content.ContentResolver;
import android.net.Uri;
import android.graphics.*;
import com.google.zxing.*;
import com.google.zxing.common.HybridBinarizer;
import java.io.*;
import java.util.*;

/** User-selected content URI only. No gallery permissions, cache, uploads or saved images. */
final class ImageQr {
    static String read(ContentResolver resolver,Uri uri)throws Exception{
        if(uri==null||!"content".equals(uri.getScheme()))throw new IOException("사진 선택기에서 고른 이미지만 허용합니다.");
        BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;
        try(InputStream in=resolver.openInputStream(uri)){if(in==null)throw new IOException("이미지를 열 수 없습니다.");BitmapFactory.decodeStream(in,null,o);}
        if(o.outWidth<=0||o.outHeight<=0||o.outWidth>50000||o.outHeight>50000)throw new IOException("지원하지 않는 이미지 크기 또는 형식입니다.");
        o.inSampleSize=1;while(o.outWidth/o.inSampleSize>2048||o.outHeight/o.inSampleSize>2048)o.inSampleSize*=2;
        o.inJustDecodeBounds=false;o.inPreferredConfig=Bitmap.Config.ARGB_8888;Bitmap bitmap;
        try(InputStream in=resolver.openInputStream(uri)){bitmap=BitmapFactory.decodeStream(in,null,o);}
        if(bitmap==null)throw new IOException("이미지를 읽지 못했습니다.");
        return decode(bitmap);
    }
    static String decode(Bitmap bitmap)throws Exception{
        try{
            int width=bitmap.getWidth(),height=bitmap.getHeight();int[] pixels=new int[width*height];bitmap.getPixels(pixels,0,width,0,0,width,height);
            LuminanceSource source=new RGBLuminanceSource(width,height,pixels);
            try{return QrDecoder.decode(source.getMatrix(),width,height);}catch(NotFoundException e){throw new IOException("유효한 중국 붕괴3 QR을 찾지 못했습니다. QR이 크게 보이는 이미지를 선택하세요.");}
        }finally{bitmap.recycle();}
    }
}
