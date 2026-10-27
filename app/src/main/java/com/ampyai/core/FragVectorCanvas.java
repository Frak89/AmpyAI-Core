package com.ampyai.core;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;
import java.util.Locale;

/** A bounded vector-program VM: M, L and Z only. Never evaluates Java or scripts. */
public final class FragVectorCanvas extends View {
    private final Paint pen=new Paint(3);
    private final Paint labelPen=new Paint(3);
    private String instructions="";
    private Path normalized;
    public FragVectorCanvas(Context context) {
        super(context);
        setMinimumHeight(dp(270));
    }
    public void setProgram(String path) {
        this.instructions=path;
        this.normalized=decode(path);
        invalidate();
    }

    private static Path decode(String code) {
        if(code==null || code.length()>1500)throw new IllegalArgumentException("Programma non valido.");
        String[] parts=code.trim().split("\\s+");
        if(parts.length<10 || parts.length>100)
            throw new IllegalArgumentException("Numero istruzioni non valido.");
        Path p=new Path();
        int index=0,vertices=0;
        boolean closed=false;
        while(index<parts.length) {
            String op=parts[index++];
            if(op.equals("Z")) {
                if(index!=parts.length||vertices<3)
                    throw new IllegalArgumentException("Chiusura non valida.");
                p.close();
                closed=true;
                break;
            }
            if(!(op.equals("M") && vertices==0) && !op.equals("L"))
                throw new IllegalArgumentException("Operazione vettoriale vietata.");
            if(index+1>=parts.length)throw new IllegalArgumentException("Punto incompleto.");
            float x=Float.parseFloat(parts[index++]);
            float y=Float.parseFloat(parts[index++]);
            if(!Float.isFinite(x)||!Float.isFinite(y)||x<-.001||y<-.001||x>1.001||y>1.001)
                throw new IllegalArgumentException("Coordinate non valide.");
            if(vertices==0)p.moveTo(x,y);else p.lineTo(x,y);
            vertices++;
        }
        if(!closed)throw new IllegalArgumentException("Contorno non chiuso.");
        return p;
    }
    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        c.drawColor(0xFF14212B);
        if(normalized==null)return;
        float margin=dp(23);
        float side=Math.min(getWidth()-2*margin,getHeight()-2*margin);
        if(side<=0)return;
        float left=(getWidth()-side)/2f,top=(getHeight()-side)/2f;
        c.save();c.translate(left,top);c.scale(side,side);
        pen.setColor(0xFF70EBC0);
        pen.setStyle(Paint.Style.STROKE);
        pen.setStrokeJoin(Paint.Join.ROUND);
        pen.setStrokeWidth(4f/side);
        c.drawPath(normalized,pen);
        c.restore();
    }
    private int dp(float v){return (int)(v*getResources().getDisplayMetrics().density+0.5f);}
}
