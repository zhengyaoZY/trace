package com.zhengyaozy.trace;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.view.View;

/** Offline, resolution-independent illustrations and line icons for the UI. */
final class TraceArt extends View {
    private final int scene;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    TraceArt(Context context, int scene) { super(context); this.scene=scene; setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO); }
    private void color(String value){paint.setColor(Color.parseColor(value));paint.setStyle(Paint.Style.FILL);}
    private void hill(Canvas c, float y, float peak, String shade){
        color(shade); Path p=new Path();p.moveTo(-20,y);p.cubicTo(80,y-80,110,peak,180,peak);p.cubicTo(250,peak,300,y-90,420,y-20);p.lineTo(420,260);p.lineTo(-20,260);p.close();c.drawPath(p,paint);
    }
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas); canvas.save();canvas.scale(getWidth()/400f,getHeight()/240f);
        color(scene==1?"#F5E9D7":scene==2?"#E5EBE5":"#DCEEE9");canvas.drawRect(0,0,400,240,paint);
        color(scene==1?"#E8B67F":"#F7F4DF");canvas.drawCircle(310,57,26,paint);
        hill(canvas,210,80,"#BCD5CA");hill(canvas,235,127,"#90B7A7");
        color("#AACFC3");canvas.drawRect(0,177,400,240,paint);
        if(scene==1){
            color("#355D50");canvas.drawRect(185,94,219,179,paint);
            for(int i=0;i<5;i++){float y=90+i*17;Path roof=new Path();roof.moveTo(202,y-12);roof.lineTo(170-i*2,y+4);roof.lineTo(234+i*2,y+4);roof.close();canvas.drawPath(roof,paint);}
            paint.setStrokeWidth(3);canvas.drawLine(202,53,202,88,paint);
            color("#DFD4B4");for(int i=0;i<4;i++)canvas.drawRect(197,103+i*17,207,113+i*17,paint);
        }else if(scene==2){
            color("#54796B");Path bridge=new Path();bridge.moveTo(36,183);bridge.quadTo(190,57,364,183);bridge.lineTo(364,201);bridge.quadTo(190,94,36,201);bridge.close();canvas.drawPath(bridge,paint);
            paint.setStrokeWidth(4);for(int i=0;i<9;i++){float x=55+i*36;float t=(x-200)/165;float y=120+57*t*t;canvas.drawLine(x,y-10,x,y+5,paint);}
        }else{
            color("#537C6B");canvas.drawRect(82,141,88,189,paint);canvas.drawRect(128,141,134,189,paint);
            Path roof=new Path();roof.moveTo(65,142);roof.quadTo(110,124,151,142);roof.lineTo(145,148);roof.lineTo(72,148);roof.close();canvas.drawPath(roof,paint);
            color("#355E50");Path boat=new Path();boat.moveTo(232,205);boat.lineTo(289,205);boat.lineTo(278,214);boat.lineTo(245,214);boat.close();canvas.drawPath(boat,paint);
        }
        color("#E3F0E7");paint.setStrokeWidth(2);paint.setStrokeCap(Paint.Cap.ROUND);
        for(int i=0;i<4;i++)canvas.drawLine(30+i*74,220+(i%2)*9,68+i*74,220+(i%2)*9,paint);
        // Foreground willow leaves give each cover a consistent local identity.
        color("#416C59");paint.setStrokeWidth(3);canvas.drawLine(0,0,60,70,paint);
        for(int i=0;i<5;i++){float x=10+i*10,y=12+i*11;canvas.drawOval(x,y,x+8,y+25,paint);}
        canvas.restore();
    }

    static final class Icon extends Drawable {
        private final int kind,color;
        private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        Icon(int kind,int color){this.kind=kind;this.color=color;}
        @Override public void draw(Canvas canvas){
            canvas.save();canvas.translate(getBounds().left,getBounds().top);canvas.scale(getBounds().width()/24f,getBounds().height()/24f);
            p.setColor(color);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.7f);p.setStrokeJoin(Paint.Join.ROUND);p.setStrokeCap(Paint.Cap.ROUND);
            if(kind==0){canvas.drawCircle(12,12,9,p);Path a=new Path();a.moveTo(16,8);a.lineTo(13,14);a.lineTo(8,16);a.lineTo(11,10);a.close();canvas.drawPath(a,p);}
            else if(kind==1){Path a=new Path();a.moveTo(3,5);a.lineTo(9,3);a.lineTo(15,6);a.lineTo(21,4);a.lineTo(21,19);a.lineTo(15,21);a.lineTo(9,18);a.lineTo(3,20);a.close();canvas.drawPath(a,p);canvas.drawLine(9,3,9,18,p);canvas.drawLine(15,6,15,21,p);}
            else {canvas.drawCircle(12,8,3.5f,p);canvas.drawArc(5,13,19,25,190,160,false,p);}
            canvas.restore();
        }
        @Override public void setAlpha(int a){p.setAlpha(a);}
        @Override public void setColorFilter(ColorFilter f){p.setColorFilter(f);}
        @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
    }
}
