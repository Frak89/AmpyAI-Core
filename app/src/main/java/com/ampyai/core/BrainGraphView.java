package com.ampyai.core;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;
import java.util.Arrays;
import java.util.Locale;

/** Displays actual sampled neural weights and ALL actual hidden nodes as dots. */
public final class BrainGraphView extends View {
    private final Paint pen=new Paint(3);
    private PrimitiveLM.Model model;
    private String probe="";private int conceptCount;
    private final float density;
    public BrainGraphView(Context context) {
        super(context);
        density=getResources().getDisplayMetrics().density;
    }
    public void display(PrimitiveLM.Model current,String text,int concepts) {
        model=current;probe=text;conceptCount=concepts;invalidate();
    }
    private float dp(float n){return n*density;}
    @Override protected void onMeasure(int widthSpec,int heightSpec){
        setMeasuredDimension(MeasureSpec.getSize(widthSpec),(int)dp(370));
    }
    private void label(Canvas canvas,String txt,float x,float y,int color,int size) {
        pen.setColor(color);pen.setTextSize(dp(size));pen.setStyle(Paint.Style.FILL);
        canvas.drawText(txt,x,y,pen);
    }
    private void circle(Canvas c,float x,float y,int color,float radius) {
        pen.setColor(color);pen.setStyle(Paint.Style.FILL);c.drawCircle(x,y,dp(radius),pen);
    }
    @Override protected void onDraw(Canvas c){
        super.onDraw(c);c.drawColor(0xFF102329);
        if(model==null)return;
        label(c,"PSICOFRÅG · RETE NEURALE EFFETTIVA",dp(12),dp(25),0xFF96F5C4,12);
        label(c,model.hidden+" neuroni nascosti · "+
                PrimitiveLM.parameters(model)+" pesi/bias",dp(12),dp(48),0xFFC5D8D3,11);
        label(c,conceptCount+" concetti con TXT + PNG",dp(12),dp(66),0xFFB2C8CA,11);
        label(c,model.trainingSessions==0?"PESI CASUALI - MODELLO VUOTO":
                "PESI ADDESTRATI LOCALMENTE",dp(12),dp(85),
                model.trainingSessions==0?0xFFF4C3A0:0xFF6CE6B0,10);
        PrimitiveLM.Probe state=PrimitiveLM.inspect(model,probe);
        float left=dp(40),middle=getWidth()*.52f,right=getWidth()-dp(64);
        float top=dp(115),gap=dp(27);
        int[] best=topOutput(state.probs,6);
        for(int i=0;i<PrimitiveLM.CONTEXT;i++){
            float y=top+i*gap;
            String ch=PrimitiveLM.ALPHABET.substring(state.ids[i],state.ids[i]+1)
                    .replace("\n","↵").replace(" ","·");
            label(c,ch,left-dp(16),y+dp(4),0xFF80C2EF,11);
            circle(c,left,y,0xFF66B3E8,4);
            for(int j=0;j<Math.min(model.hidden,12);j++){
                float hy=top+j*dp(15);
                line(c,left,y,middle,hy,
                        model.input[j][i*PrimitiveLM.VOCAB+state.ids[i]]);
            }
        }
        for(int j=0;j<Math.min(model.hidden,12);j++){
            float y=top+j*dp(15);
            circle(c,middle,y,state.hidden[j]>0?0xFFA28BE8:0xFF485A6B,3.5f);
            for(int k=0;k<6;k++)
                line(c,middle,y,right,top+k*gap,model.output[best[k]][j]);
        }
        for(int k=0;k<6;k++){
            float y=top+k*gap;int id=best[k];
            circle(c,right,y,0xFF72DEB0,4);
            String chr=PrimitiveLM.ALPHABET.substring(id,id+1)
                    .replace("\n","↵").replace(" ","·");
            label(c,chr+" "+String.format(Locale.US,"%.0f%%",state.probs[id]*100),
                    right+dp(7),y+dp(4),0xFFC0F3D9,10);
        }
        int cols=Math.max(1,(int)((getWidth()-dp(24))/dp(9)));
        int visible=Math.min(model.hidden,cols*3);
        label(c,"Neuroni reali: "+model.hidden+" · mostrati "+visible,
                dp(12),dp(300),0xFFC3D9D5,10);
        // A literal dot per displayed neuron; very large networks remain bounded.
        for(int i=0;i<visible;i++){
            int col=i%cols,row=i/cols;
            circle(c,dp(16+col*9),dp(314+row*9),
                    0xFF7C9ED2,2.5f);
        }
        label(c,"Collegamenti e attivazioni: campione dei pesi reali.",
                dp(12),dp(359),0xFF96AEB0,9);
    }
    private void line(Canvas c,float x,float y,float xx,float yy,float weight){
        int alpha=Math.min(210,25+(int)(Math.abs(weight)*1100));
        pen.setColor((alpha<<24)|(weight>=0?0x0070DDB1:0x00D888AA));
        pen.setStrokeWidth(dp(.55f));
        c.drawLine(x,y,xx,yy,pen);
    }
    private int[] topOutput(float[] p,int n){
        Integer[] ids=new Integer[p.length];
        for(int i=0;i<p.length;i++)ids[i]=i;
        Arrays.sort(ids,(a,b)->Float.compare(p[b],p[a]));
        int[] result=new int[n];for(int i=0;i<n;i++)result[i]=ids[i];
        return result;
    }
}
