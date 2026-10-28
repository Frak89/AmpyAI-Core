package com.ampyai.core;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;

/** Simple true 3D wireframe cube with 3D rotation + perspective projection. */
public final class Frag3DView extends View {
    private final Paint pen=new Paint(3);
    private float yaw=.4f,pitch=.27f,previousX,previousY;
    public Frag3DView(Context c){super(c);}
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);
        canvas.drawColor(0xFF101C29);
        pen.setColor(0xFF77E3BF);pen.setStrokeWidth(3.5f);
        pen.setStyle(Paint.Style.STROKE);
        float scale=Math.min(getWidth(),getHeight())*.33f;
        float[][] vertices=new float[8][2];
        for(int i=0;i<8;i++){
            double x=(i&1)==0?-1:1,y=(i&2)==0?-1:1,z=(i&4)==0?-1:1;
            double xx=x*Math.cos(yaw)-z*Math.sin(yaw);
            double zz=x*Math.sin(yaw)+z*Math.cos(yaw);
            double yy=y*Math.cos(pitch)-zz*Math.sin(pitch);
            double zFinal=y*Math.sin(pitch)+zz*Math.cos(pitch);
            double perspective=3.5/(3.5+zFinal*.45);
            vertices[i][0]=(float)(getWidth()/2+xx*scale*perspective);
            vertices[i][1]=(float)(getHeight()/2+yy*scale*perspective);
        }
        for(int i=0;i<8;i++)
            for(int axis=0;axis<3;axis++){
                int j=i^(1<<axis);
                if(j<=i)continue;
                canvas.drawLine(vertices[i][0],vertices[i][1],
                        vertices[j][0],vertices[j][1],pen);
            }
    }
    @Override public boolean onTouchEvent(MotionEvent event){
        switch(event.getActionMasked()){
            case MotionEvent.ACTION_DOWN:
                previousX=event.getX();previousY=event.getY();return true;
            case MotionEvent.ACTION_MOVE:
                yaw+=(event.getX()-previousX)/140;
                pitch+=(event.getY()-previousY)/140;
                previousX=event.getX();previousY=event.getY();invalidate();return true;
            case MotionEvent.ACTION_UP:return true;
            default:return super.onTouchEvent(event);
        }
    }
}
