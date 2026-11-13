package dev.local.bililight;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.*;
import android.content.res.ColorStateList;
import android.view.*;
import android.widget.*;

/** Small, dependency-free native design system. No remote fonts or analytics. */
final class Ui {
    static final int BG=0xff0b111d,SURFACE=0xff141e2d,SURFACE2=0xff1b293b,LINE=0xff29394c,TEXT=0xffedf3fc,MUTED=0xff99acc4,ACCENT=0xff56d8c5,AMBER=0xffefc37f;
    static int dp(Context c,int n){return Math.round(n*c.getResources().getDisplayMetrics().density);}
    static GradientDrawable shape(Context c,int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(c,radius));return d;}
    static TextView label(Context c,String s,int size,int color,boolean bold){TextView t=new TextView(c);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setFontFeatureSettings("kern");t.setTypeface(Typeface.create(bold?"sans-serif-medium":"sans-serif",bold?Typeface.NORMAL:Typeface.NORMAL));t.setLineSpacing(dp(c,2),1.08f);return t;}
    static LinearLayout column(Context c){LinearLayout v=new LinearLayout(c);v.setOrientation(1);return v;}
    static LinearLayout card(Context c){LinearLayout v=column(c);v.setPadding(dp(c,20),dp(c,19),dp(c,20),dp(c,19));v.setBackground(shape(c,SURFACE,22));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(c,14);v.setLayoutParams(p);return v;}
    static void gap(LinearLayout parent,int dp){View v=new View(parent.getContext());parent.addView(v,new LinearLayout.LayoutParams(1,Ui.dp(parent.getContext(),dp)));}
    static TextView badge(Context c,String s,int color){TextView t=label(c,s,11,color,true);t.setPadding(dp(c,9),dp(c,5),dp(c,9),dp(c,5));t.setBackground(shape(c,(color&0xffffff)|0x20000000,8));return t;}
    static Button button(Context c,String text,boolean primary){Button b=new Button(c);b.setText(text);b.setAllCaps(false);b.setTextSize(15);b.setTypeface(Typeface.create("sans-serif-medium",0));b.setTextColor(primary?BG:TEXT);b.setMinHeight(dp(c,52));b.setMinimumHeight(dp(c,52));b.setPadding(dp(c,14),dp(c,9),dp(c,14),dp(c,9));b.setStateListAnimator(null);b.setBackground(new RippleDrawable(ColorStateList.valueOf(0x30ffffff),shape(c,primary?ACCENT:SURFACE2,14),null));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(c,9);b.setLayoutParams(p);return b;}
    static EditText input(Context c,String hint,int type){EditText e=new EditText(c);e.setHint(hint);e.setInputType(type);e.setTextSize(15);e.setTextColor(TEXT);e.setHintTextColor(MUTED);e.setPadding(dp(c,15),dp(c,14),dp(c,15),dp(c,14));e.setMinHeight(dp(c,54));e.setBackground(shape(c,SURFACE2,13));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(c,10);e.setLayoutParams(p);e.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);return e;}
    static final class Mark extends View {
        final Paint p=new Paint(3);final int kind,color;
        Mark(Context c,int kind,int color){super(c);this.kind=kind;this.color=color;setContentDescription(kind==0?"QR 스캔":"연결 상태");}
        @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);float scale=Math.min(getWidth(),getHeight())/48f;canvas.save();canvas.scale(scale,scale);p.setColor(color);p.setStrokeWidth(2.6f);p.setStyle(Paint.Style.STROKE);p.setStrokeCap(Paint.Cap.ROUND);
            if(kind==0){canvas.drawRoundRect(5,5,20,20,3,3,p);canvas.drawRoundRect(28,5,43,20,3,3,p);canvas.drawRoundRect(5,28,20,43,3,3,p);canvas.drawLine(29,29,43,29,p);canvas.drawLine(29,29,29,36,p);canvas.drawLine(36,36,43,36,p);canvas.drawLine(36,36,36,43,p);canvas.drawLine(43,40,43,43,p);}
            else{canvas.drawCircle(24,17,7,p);canvas.drawArc(9,27,39,52,185,170,false,p);}canvas.restore();
        }
    }
}
