package dev.local.bililight;

import android.Manifest;
import android.app.Activity;
import android.os.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.graphics.ImageFormat;
import android.view.*;
import android.widget.*;
import java.util.*;
import java.util.concurrent.*;
import com.google.zxing.NotFoundException;

/** High-resolution local QR scanning. No images are retained or sent to a server. */
@SuppressWarnings("deprecation")
public final class ScanActivity extends Activity implements SurfaceHolder.Callback {
    SurfaceView surface;Camera camera;TextView status,zoomText;Button autoButton,torchButton;SeekBar zoom;
    final Handler handler=new Handler(Looper.getMainLooper());
    volatile boolean decoding,closed,active;boolean autoZoom=true,torch;long last,lastZoom;int sweep,maxZoomIndex;
    final ExecutorService worker=Executors.newSingleThreadExecutor();
    final Runnable focusTick=new Runnable(){public void run(){if(active&&camera!=null){focus();handler.postDelayed(this,3500);}}};
    @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE|WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setStatusBarColor(Ui.BG);getWindow().setNavigationBarColor(Ui.BG);
        LinearLayout root=Ui.column(this);root.setBackgroundColor(Ui.BG);root.setPadding(Ui.dp(this,16),Ui.dp(this,12),Ui.dp(this,16),Ui.dp(this,12));
        root.addView(Ui.label(this,"PC QR 스캔",23,Ui.TEXT,true));status=Ui.label(this,"전체 화면 인식 · 멀리 있으면 QR을 중앙에 두세요.",13,Ui.MUTED,false);status.setPadding(0,Ui.dp(this,8),0,Ui.dp(this,12));root.addView(status);
        FrameLayout frame=new FrameLayout(this);surface=new SurfaceView(this);frame.addView(surface,new FrameLayout.LayoutParams(-1,-1));frame.addView(new Guide(this),new FrameLayout.LayoutParams(-1,-1));root.addView(frame,new LinearLayout.LayoutParams(-1,0,1));
        zoomText=Ui.label(this,"확대 1.0×",12,Ui.ACCENT,true);root.addView(zoomText);zoom=new SeekBar(this);zoom.setEnabled(false);zoom.setContentDescription("카메라 확대 조절");root.addView(zoom);
        zoom.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}public void onProgressChanged(SeekBar s,int progress,boolean user){if(user){autoZoom=false;autoButton.setText("자동 확대 꺼짐");setZoom(progress);}}});
        LinearLayout controls=new LinearLayout(this);autoButton=Ui.button(this,"자동 확대 켜짐",false);autoButton.setTextSize(12);autoButton.setOnClickListener(v->{autoZoom=!autoZoom;autoButton.setText(autoZoom?"자동 확대 켜짐":"자동 확대 꺼짐");lastZoom=SystemClock.elapsedRealtime();});controls.addView(autoButton,new LinearLayout.LayoutParams(0,-2,1));
        Button focus=Ui.button(this,"초점 맞춤",false);focus.setTextSize(12);focus.setOnClickListener(v->focus());controls.addView(focus,new LinearLayout.LayoutParams(0,-2,1));torchButton=Ui.button(this,"조명 꺼짐",false);torchButton.setTextSize(12);torchButton.setOnClickListener(v->toggleTorch());controls.addView(torchButton,new LinearLayout.LayoutParams(0,-2,1));root.addView(controls);
        Button back=Ui.button(this,"스캔 취소",false);back.setOnClickListener(v->finish());root.addView(back);setContentView(root);surface.getHolder().addCallback(this);
        if(checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.CAMERA},1);
    }
    static final class Guide extends View {
        final android.graphics.Paint paint=new android.graphics.Paint(3);
        Guide(android.content.Context c){super(c);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
        @Override protected void onDraw(android.graphics.Canvas c){float side=Math.min(getWidth()*.88f,getHeight()*.72f),left=(getWidth()-side)/2,top=(getHeight()-side)/2,k=side*.10f;paint.setColor(Ui.ACCENT);paint.setStrokeWidth(Ui.dp(getContext(),3));paint.setStrokeCap(android.graphics.Paint.Cap.ROUND);
            for(int x=0;x<2;x++)for(int y=0;y<2;y++){float a=left+x*side,b=top+y*side;c.drawLine(a,b,a+(x==0?k:-k),b,paint);c.drawLine(a,b,a,b+(y==0?k:-k),paint);}
        }
    }
    void open(){if(closed||camera!=null||!surface.getHolder().getSurface().isValid()||checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED)return;
        try{
            int id=-1;Camera.CameraInfo info=new Camera.CameraInfo();for(int i=0;i<Camera.getNumberOfCameras();i++){Camera.getCameraInfo(i,info);if(info.facing==Camera.CameraInfo.CAMERA_FACING_BACK){id=i;break;}}
            if(id<0)throw new IllegalStateException("back camera unavailable");camera=Camera.open(id);Camera.Parameters p=camera.getParameters();Camera.Size chosen=null;
            for(Camera.Size s:p.getSupportedPreviewSizes())if((long)s.width*s.height<=2560L*1440&&(chosen==null||(long)s.width*s.height>(long)chosen.width*chosen.height))chosen=s;
            if(chosen!=null)p.setPreviewSize(chosen.width,chosen.height);
            List<Integer> formats=p.getSupportedPreviewFormats();if(formats!=null&&formats.contains(ImageFormat.NV21))p.setPreviewFormat(ImageFormat.NV21);
            List<String> modes=p.getSupportedFocusModes();if(modes!=null){if(modes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE))p.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);else if(modes.contains(Camera.Parameters.FOCUS_MODE_AUTO))p.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);}
            if(p.getMaxNumFocusAreas()>0)p.setFocusAreas(Collections.singletonList(new Camera.Area(new android.graphics.Rect(-450,-450,450,450),1000)));
            if(p.getMaxNumMeteringAreas()>0)p.setMeteringAreas(Collections.singletonList(new Camera.Area(new android.graphics.Rect(-600,-600,600,600),800)));
            try{camera.setParameters(p);}catch(RuntimeException ignored){/* Keep driver defaults rather than losing camera access. */}
            p=camera.getParameters();List<Integer> ratios=p.isZoomSupported()?p.getZoomRatios():null;maxZoomIndex=0;if(ratios!=null)for(int i=0;i<ratios.size();i++)if(ratios.get(i)<=350)maxZoomIndex=i;
            zoom.setMax(maxZoomIndex);zoom.setEnabled(maxZoomIndex>0);autoButton.setEnabled(maxZoomIndex>0);
            List<String> flash=p.getSupportedFlashModes();torchButton.setEnabled(flash!=null&&flash.contains(Camera.Parameters.FLASH_MODE_TORCH));
            camera.setDisplayOrientation(90);camera.setPreviewDisplay(surface.getHolder());active=true;lastZoom=SystemClock.elapsedRealtime();
            final Camera.Size size=p.getPreviewSize();status.setText(size.width+" × "+size.height+" 고해상도 · QR을 중앙에 두면 자동 확대합니다.");
            camera.setPreviewCallbackWithBuffer((data,c)->{
                try{
                    if(data==null||decoding||!active||closed||SystemClock.elapsedRealtime()-last<450)return;
                    if(data.length<(long)size.width*size.height)return;
                    last=SystemClock.elapsedRealtime();decoding=true;final byte[] luma=Arrays.copyOf(data,size.width*size.height);
                    worker.submit(()->{try{
                        String result=QrDecoder.decode(luma,size.width,size.height);
                        runOnUiThread(()->{if(!active||closed||isFinishing())return;closed=true;setResult(RESULT_OK,new Intent().putExtra("qr",result));finish();});
                    }catch(NotFoundException ignored){runOnUiThread(()->sweepZoom());}
                    catch(Exception e){runOnUiThread(()->{if(active)status.setText("중국 붕괴3 공식 QR을 비춰 주세요.");});}
                    finally{decoding=false;}});
                }finally{if(active&&camera==c&&data!=null)try{c.addCallbackBuffer(data);}catch(RuntimeException ignored){}}
            });
            int bits=Math.max(12,ImageFormat.getBitsPerPixel(p.getPreviewFormat()));int bytes=size.width*size.height*bits/8+4096;camera.addCallbackBuffer(new byte[bytes]);camera.addCallbackBuffer(new byte[bytes]);camera.startPreview();handler.postDelayed(focusTick,1200);
        }catch(Exception e){release();status.setText("카메라를 열지 못했습니다. 권한을 확인하거나 이미지 QR을 사용하세요.");}
    }
    void focus(){Camera c=camera;if(!active||c==null)return;try{c.cancelAutoFocus();c.autoFocus((ok,current)->handler.postDelayed(()->{if(active&&camera==current)try{current.cancelAutoFocus();}catch(RuntimeException ignored){}},650));}catch(RuntimeException ignored){}}
    void setZoom(int index){if(camera==null||!active)return;try{Camera.Parameters p=camera.getParameters();if(!p.isZoomSupported())return;index=Math.max(0,Math.min(maxZoomIndex,index));p.setZoom(index);camera.setParameters(p);zoom.setProgress(index);zoomText.setText(String.format(Locale.ROOT,"확대 %.1f×",p.getZoomRatios().get(index)/100f));focus();}catch(RuntimeException ignored){}}
    void sweepZoom(){if(!active||!autoZoom||maxZoomIndex==0||SystemClock.elapsedRealtime()-lastZoom<2500)return;lastZoom=SystemClock.elapsedRealtime();try{List<Integer> ratios=camera.getParameters().getZoomRatios();float[] targets={1f,1.5f,2f,2.7f,3.5f};sweep=(sweep+1)%targets.length;int best=0;for(int i=0;i<=maxZoomIndex;i++)if(Math.abs(ratios.get(i)-targets[sweep]*100)<Math.abs(ratios.get(best)-targets[sweep]*100))best=i;setZoom(best);}catch(RuntimeException ignored){}}
    void toggleTorch(){if(camera==null||!active)return;try{Camera.Parameters p=camera.getParameters();List<String> modes=p.getSupportedFlashModes();if(modes==null||!modes.contains(Camera.Parameters.FLASH_MODE_TORCH))return;torch=!torch;p.setFlashMode(torch?Camera.Parameters.FLASH_MODE_TORCH:Camera.Parameters.FLASH_MODE_OFF);camera.setParameters(p);torchButton.setText(torch?"조명 켜짐":"조명 꺼짐");}catch(RuntimeException ignored){status.setText("이 카메라에서 조명을 제어할 수 없습니다.");}}
    void release(){active=false;handler.removeCallbacksAndMessages(null);if(camera!=null){try{camera.setPreviewCallbackWithBuffer(null);camera.stopPreview();}catch(RuntimeException ignored){}camera.release();camera=null;}}
    @Override public void surfaceCreated(SurfaceHolder h){open();}
    @Override public void surfaceChanged(SurfaceHolder h,int format,int width,int height){open();}
    @Override public void surfaceDestroyed(SurfaceHolder h){release();}
    @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){super.onRequestPermissionsResult(r,p,g);if(r==1&&g.length>0&&g[0]==PackageManager.PERMISSION_GRANTED)open();else status.setText("카메라 권한을 허용하거나 갤러리 QR을 사용하세요.");}
    @Override protected void onResume(){super.onResume();open();}
    @Override protected void onPause(){release();super.onPause();}
    @Override protected void onDestroy(){closed=true;worker.shutdownNow();super.onDestroy();}
}
