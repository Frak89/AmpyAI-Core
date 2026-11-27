package com.ampyai.core;

import android.content.Context;
import android.graphics.*;
import android.os.SystemClock;
import android.view.*;

/** Articulated textured relief. The grid is anchored at the picture's lips and eyelids. */
final class OrbPortrait extends View {
    private static final int N=48, MINT=0xff80ffcf;
    private final Bitmap portrait;
    private final Paint ink=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final Path mask=new Path();
    private final float[] vertices=new float[2*(N+1)*(N+1)];
    private final float[][] weights=new float[(N+1)*(N+1)][7];
    private final long born=SystemClock.uptimeMillis();
    private long entered=born,previous=born,lastLetter;
    private float yaw,pitch,tx,ty,lastX,lastY,open,round;
    private boolean drag,thinking;
    private char phoneme;
    private static final int COUNT=48;
    private final char[] letters=new char[COUNT]; private final long[] times=new long[COUNT];
    private int sequence;
    OrbPortrait(Context ctx){
        super(ctx);
        portrait=BitmapFactory.decodeResource(getResources(),R.drawable.frag_emerald);
        int i=0;
        for(int y=0;y<=N;y++)for(int x=0;x<=N;x++,i++){
            float u=x/(float)N,v=y/(float)N;
            float[] a=weights[i];
            a[0]=(float)(.18*falloff(u,v,.48,.55,.44,.62)+.55*falloff(u,v,.57,.35,.34,.29)+.30*falloff(u,v,.63,.39,.19,.13));
            a[1]=falloff(u,v,.60,.49,.18,.064);
            a[2]=falloff(u,v,.61,.425,.19,.032);
            a[3]=falloff(u,v,.385,.213,.092,.065);
            a[4]=falloff(u,v,.785,.268,.052,.052);
            a[5]=falloff(u,v,.55,.35,.38,.39);
            a[6]=falloff(u,v,.62,.455,.205,.081);
        }
        ink.setTypeface(Typeface.MONOSPACE);
        setContentDescription("Busto di Fråg · trascina per ruotarlo");
    }
    private static float falloff(float x,float y,double cx,double cy,double w,double h){
        double a=(x-cx)/w,b=(y-cy)/h;return(float)Math.exp(-a*a-b*b);
    }
    void request(){entered=SystemClock.uptimeMillis();thinking=true;invalidate();}
    void finishThinking(){thinking=false;invalidate();}
    void emit(char c){phoneme=Character.toLowerCase(c);lastLetter=SystemClock.uptimeMillis();
        if(!Character.isWhitespace(c)){int i=sequence++%COUNT;letters[i]=c;times[i]=lastLetter;}invalidate();}
    @Override protected void onDraw(Canvas c){
        long now=SystemClock.uptimeMillis();float dt=Math.max(0,Math.min(.12f,(now-previous)/1000f));previous=now;
        float t=(now-born)*.001f,damp=(float)Math.exp(-dt*(drag?20:8));
        yaw=yaw*damp+tx*(1-damp);pitch=pitch*damp+ty*(1-damp);
        float turn=yaw+.016f*(float)Math.sin(.84*t),tilt=pitch+.009f*(float)Math.sin(1.43*t);
        float r=.477f*Math.min(getWidth(),getHeight()),side=2*r,cx=getWidth()*.5f,cy=getHeight()*.5f;
        float ease=1-Math.min(1f,(now-entered)/820f),rise=side*ease*ease*ease;
        float blink=Math.max(0,1-Math.abs((t%4.37f)-3.85f)/.12f);
        boolean sounding=now-lastLetter<105;
        float target=sounding&&"aeiouàèéìòù".indexOf(phoneme)>=0?1:sounding&&"szfrl".indexOf(phoneme)>=0?.35f:0;
        float blend=1-(float)Math.exp(-dt*24);open+=(target-open)*blend;
        round+=((sounding&&"oòuù".indexOf(phoneme)>=0?1:0)-round)*blend;
        int i=0;float lipX=cx,lipY=cy;
        for(int y=0;y<=N;y++)for(int x=0;x<=N;x++,i++){
            float u=x/(float)N,v=y/(float)N;float[] a=weights[i];
            float dx=turn*a[0]*.19f-round*(u-.605f)*a[6]*.27f;
            float dy=tilt*a[0]*.13f+.003f*(float)Math.sin(t*1.43)*a[5]
                    +(open-.55f)*(.039f*a[1]-.014f*a[2])
                    -blink*((v-.22f)*a[3]+(v-.279f)*a[4])*.95f;
            vertices[i*2]=cx+side*(u-.5f+dx);vertices[i*2+1]=cy+side*(v-.5f+dy)+rise;
            if(x==30&&y==22){lipX=vertices[2*i];lipY=vertices[2*i+1];}
        }
        c.drawColor(0xff09131d);
        mask.reset();mask.addCircle(cx,cy,r,Path.Direction.CW);c.save();c.clipPath(mask);
        ink.setColor(0xff03121d);c.drawCircle(cx,cy,r,ink);ink.setColor(-1);
        c.drawBitmapMesh(portrait,N,N,vertices,0,null,0,ink);
        for(i=0;i<COUNT;i++){
            float u=(now-times[i])/740f;if(times[i]==0||u<0||u>=1)continue;
            float x=lipX+r*(.57f*u+.085f*(float)Math.sin(Math.PI*u));
            float y=lipY+r*(.045f*(float)Math.sin(2*Math.PI*u)-.62f*u*u);
            ink.setTextSize(r*(.026f+.13f*u));ink.setColor(MINT);
            ink.setAlpha((int)(255*Math.min(1,Math.min(u*12,(1-u)*6))));
            char ch=u<.23f?"01λΣアイ".charAt((int)((now/32+i)%6)):letters[i];
            c.save();c.scale(.48f+u*.72f,1.18f-.16f*u,x,y);c.drawText(String.valueOf(ch),x,y,ink);c.restore();
        }
        c.restore();ink.setColor(0xff2be5eb);ink.setStrokeWidth(Math.max(2,side*.004f));ink.setStyle(Paint.Style.STROKE);
        c.drawCircle(cx,cy,r,ink);
        if(thinking){ink.setColor(0xffb5ffdc);c.drawArc(cx-r,cy-r,cx+r,cy+r,t*80,42,false,ink);}
        ink.setStyle(Paint.Style.FILL);if(isShown())postInvalidateDelayed(33);
    }
    @Override public boolean onTouchEvent(MotionEvent e){
        switch(e.getActionMasked()){
        case MotionEvent.ACTION_DOWN:drag=true;lastX=e.getX();lastY=e.getY();getParent().requestDisallowInterceptTouchEvent(true);return true;
        case MotionEvent.ACTION_MOVE:tx=Math.max(-1,Math.min(1,tx+(e.getX()-lastX)*3/getWidth()));ty=Math.max(-.8f,Math.min(.8f,ty+(e.getY()-lastY)*3/getHeight()));lastX=e.getX();lastY=e.getY();invalidate();return true;
        case MotionEvent.ACTION_UP:performClick();
        case MotionEvent.ACTION_CANCEL:drag=false;tx=ty=0;getParent().requestDisallowInterceptTouchEvent(false);invalidate();return true;
        default:return true;}
    }
    @Override public boolean performClick(){super.performClick();return true;}
}
