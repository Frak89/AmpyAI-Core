package com.ampyai.core;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

public final class FrapCanvasView extends View {
    private final Paint paint=new Paint(3);
    private FrapEngine.Program program;
    private int green=0xFF79EAC5;
    public FrapCanvasView(Context ctx){super(ctx);}
    public void render(FrapEngine.Program p){program=p;invalidate();}
    @Override protected void onDraw(Canvas c){
        super.onDraw(c);c.drawColor(0xFF101C29);
        if(program==null)return;
        float margin=24*getResources().getDisplayMetrics().density;
        float side=Math.max(0,Math.min(getWidth(),getHeight())-2*margin);
        float x=(getWidth()-side)/2,y=(getHeight()-side)/2;
        paint.setColor(green);paint.setStrokeWidth(4);
        paint.setStyle(Paint.Style.STROKE);
        for(FrapEngine.Shape s:program.shapes){
            float[] a=s.args;
            switch(s.op){
                case "line":c.drawLine(x+a[0]*side,y+a[1]*side,x+a[2]*side,y+a[3]*side,paint);break;
                case "rect":c.drawRect(x+a[0]*side,y+a[1]*side,
                        x+(a[0]+a[2])*side,y+(a[1]+a[3])*side,paint);break;
                case "circle":c.drawCircle(x+a[0]*side,y+a[1]*side,a[2]*side,paint);break;
            }
        }
    }
}
