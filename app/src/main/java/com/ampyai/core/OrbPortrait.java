package com.ampyai.core;

import android.content.Context;
import android.graphics.*;
import android.os.SystemClock;
import android.view.*;
import java.util.ArrayList;
import java.util.Comparator;

/** A lit triangle bust rendered in camera space, with articulated eyes and lips. */
final class OrbPortrait extends View {
    private static final int ICE=0xff7dffe1, SKIN=0xff539494, DARK=0xff203e50, GOLD=0xffffc57e;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path=new Path();
    private final ArrayList<Face> mesh=new ArrayList<>();
    private final ArrayList<Face> visible=new ArrayList<>();
    private final long born=SystemClock.uptimeMillis();
    private long entrance=born, spoken, previousTime;
    private float yaw,pitch,lastX,lastY, mouth;
    private boolean drag, thinking;
    private String word="";
    private final char[] glyphs=new char[32];
    private final long[] times=new long[32];
    private final float[] originsX=new float[32], originsY=new float[32];
    private float lipX,lipY; private int next;
    void emit(char letter){
        say(String.valueOf(letter));
        if(Character.isWhitespace(letter))return;
        int slot=next++ & 31; glyphs[slot]=letter;times[slot]=SystemClock.uptimeMillis();
        originsX[slot]=lipX;originsY[slot]=lipY;
    }
    private static class Face {
        final float[][] vertices; final int color, part;
        final float[] x=new float[3],y=new float[3];
        float depth,light;
        Face(float[] a,float[] b,float[] c,int color,int part){
            vertices=new float[][]{a,b,c};this.color=color;this.part=part;
        }
    }
    OrbPortrait(Context context){
        super(context);
        oval(0,-.92,0,.91,.44,.39,DARK,0);
        oval(0,-.57,0,.29,.40,.28,SKIN,0);
        oval(0,.08,0,.62,.73,.46,SKIN,0);
        oval(0,-.09,.36,.49,.30,.26,SKIN,0);
        for(int side:new int[]{-1,1}){
            oval(side*.35,.37,.23,.29,.25,.24,SKIN,0);
            oval(side*.35,.39,.43,.162,.135,.063,GOLD,1);
            oval(side*.35,.39,.484,.042,.097,.022,DARK,1);
            oval(side*.35-.029,.43,.503,.022,.024,.012,ICE,1);
            oval(side*.16,.004,.585,.032,.022,.018,DARK,0);
        }
        oval(0,-.20,.58,.255,.057,.035,0xff142e3b,2);
        oval(0,-.254,.545,.246,.038,.059,SKIN,3);
        setContentDescription("Fråg, ritratto tridimensionale ruotabile");
    }
    private static float[] point(double a,double b,float x,float y,float z,float w,float h,float d){
        return new float[]{x+w*(float)(Math.cos(b)*Math.cos(a)),y+h*(float)Math.sin(b),z+d*(float)(Math.cos(b)*Math.sin(a))};
    }
    private void oval(float x,float y,float z,float w,float h,float d,int color,int part){
        int rings=8,sides=16;
        float[][][] grid=new float[rings+1][sides][];
        for(int j=0;j<=rings;j++)for(int i=0;i<sides;i++)
            grid[j][i]=point(i*2*Math.PI/sides,j*Math.PI/rings-Math.PI/2,x,y,z,w,h,d);
        for(int j=0;j<rings;j++)for(int i=0;i<sides;i++){
            int k=(i+1)%sides;
            mesh.add(new Face(grid[j][i],grid[j+1][i],grid[j][k],color,part));
            mesh.add(new Face(grid[j][k],grid[j+1][i],grid[j+1][k],color,part));
        }
    }
    void request(){ entrance=SystemClock.uptimeMillis(); thinking=true;word="";invalidate(); }
    void finishThinking(){thinking=false;invalidate();}
    void say(String token){word=token.toLowerCase(java.util.Locale.ITALIAN);spoken=SystemClock.uptimeMillis();invalidate();}
    @Override protected void onDraw(Canvas c){
        long now=SystemClock.uptimeMillis();
        float t=(now-born)/1000f,dt=Math.min(.1f,(now-previousTime)/1000f);previousTime=now;
        if(!drag){float k=(float)Math.exp(-8*dt);yaw*=k;pitch*=k;}
        double turn=yaw+.065*Math.sin(t*.8),tilt=pitch+.033*Math.sin(t*1.1);
        float cy=(float)Math.cos(turn),sy=(float)Math.sin(turn),cx=(float)Math.cos(tilt),sx=(float)Math.sin(tilt);
        float r=Math.min(getWidth(),getHeight())*.46f,mx=getWidth()/2f,my=getHeight()/2f;
        float entry=Math.min(1,(now-entrance)/780f);
        float lift=1-entry;lift=lift*lift*2*r;
        int n=(int)((now-spoken)/78);char ph=n>=0&&n<word.length()?word.charAt(n):' ';
        float target="aàeèéiìoòuù".indexOf(ph)>=0?1:"fvlszrt".indexOf(ph)>=0?.32f:0;
        mouth+=(target-mouth)*Math.min(1,dt*18);
        float blink=(float)Math.min(1,Math.abs(((t+.17)%3.9)-.12)*11+.06);
        c.drawColor(0xff09131d);paint.setStyle(Paint.Style.FILL);paint.setColor(0xff142d38);c.drawCircle(mx,my,r,paint);
        path.reset();path.addCircle(mx,my,r-2,Path.Direction.CW);c.save();c.clipPath(path);
        float lipsY=-.20f, lipsZ=.58f*cy;
        float lipScale=r*.91f/(1-(lipsY*sx+lipsZ*cx)*.16f);
        lipX=mx+.58f*sy*lipScale;lipY=my+lift-(lipsY*cx-lipsZ*sx)*lipScale;
        visible.clear();
        for(Face f:mesh){
            float sum=0;
            for(int i=0;i<3;i++){
                float[] p=f.vertices[i];float x=p[0],y=p[1],z=p[2];
                if(f.part==1)y=.39f+(y-.39f)*blink;
                if(f.part==2){y=-.15f+(y+.15f)*(.3f+mouth*1.45f);if("ouòù".indexOf(ph)>=0)x*=.66f;}
                if(f.part==3)y-=mouth*.083f;
                float xx=x*cy+z*sy,zz=z*cy-x*sy,yy=y*cx-zz*sx;
                zz=y*sx+zz*cx;sum+=zz;
                float scale=r*.91f/(1-zz*.16f);
                f.x[i]=mx+xx*scale;f.y[i]=my+lift-yy*scale;
            }
            float area=(f.x[1]-f.x[0])*(f.y[2]-f.y[0])-(f.y[1]-f.y[0])*(f.x[2]-f.x[0]);
            if(area>0)continue;
            f.depth=sum/3; f.light=Math.max(.33f,Math.min(1.2f,.64f+.55f*f.depth+(mx-f.x[0])/(r*4)+(my+lift-f.y[0])/(r*6)));
            visible.add(f);
        }
        visible.sort(Comparator.comparingDouble(f->f.depth));
        for(Face f:visible){
            paint.setColor(Color.rgb(Math.min(255,(int)(Color.red(f.color)*f.light)),Math.min(255,(int)(Color.green(f.color)*f.light)),Math.min(255,(int)(Color.blue(f.color)*f.light))));
            path.reset();path.moveTo(f.x[0],f.y[0]);path.lineTo(f.x[1],f.y[1]);path.lineTo(f.x[2],f.y[2]);path.close();c.drawPath(path,paint);
        }
        c.restore();paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2);paint.setColor(ICE);c.drawCircle(mx,my,r,paint);
        if(thinking){paint.setStrokeWidth(5);paint.setColor(GOLD);c.drawArc(mx-r,my-r,mx+r,my+r,t*80,60,false,paint);}
        paint.setStyle(Paint.Style.FILL);paint.setTypeface(Typeface.MONOSPACE);
        for(int i=0;i<glyphs.length;i++){
            float u=(now-times[i])/560f;
            if(times[i]==0||u<0||u>=1)continue;
            float bend=(float)Math.sin(u*Math.PI);
            float x=originsX[i]+r*(.68f*u+.17f*bend);
            float y=originsY[i]-r*(.60f*u*u+.15f*bend);
            c.save();c.translate(x,y);c.scale(.25f+u*1.3f,1.45f-.42f*u);
            paint.setTextSize(r*.17f);paint.setColor(ICE);paint.setAlpha((int)(255*Math.min(1,(1-u)*4)));
            char ch=u<.24f?"01Σλアイ".charAt((int)((now/31+i)%6)):glyphs[i];
            c.drawText(String.valueOf(ch),0,0,paint);c.restore();
        }
        paint.setAlpha(255);if(isShown())postInvalidateDelayed(33);
    }
    @Override public boolean onTouchEvent(MotionEvent e){
        switch(e.getActionMasked()){
        case MotionEvent.ACTION_DOWN: drag=true;lastX=e.getX();lastY=e.getY();getParent().requestDisallowInterceptTouchEvent(true);return true;
        case MotionEvent.ACTION_MOVE: yaw=Math.max(-1.3f,Math.min(1.3f,yaw+(e.getX()-lastX)*.009f));pitch=Math.max(-.6f,Math.min(.6f,pitch+(e.getY()-lastY)*.008f));lastX=e.getX();lastY=e.getY();invalidate();return true;
        case MotionEvent.ACTION_UP: performClick();
        case MotionEvent.ACTION_CANCEL: drag=false;getParent().requestDisallowInterceptTouchEvent(false);invalidate();return true;
        default:return true;}
    }
    @Override public boolean performClick(){super.performClick();return true;}
}
