package com.ampyai.core;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Psycofråg neural microscope: only actual stored model parameters and
 * actual forward-pass activations. Draws a clearly labeled sampled subset;
 * never claims that the sampled nodes are all of the physical neurons.
 */
public final class FragBrainGraphView extends View {
    private final Paint p = new Paint(3);
    private FragNeuralTrainer.Snapshot snapshot;
    private final List<Double> losses = new ArrayList<>();
    private String query = "crea un pulsante con scritto Salva";
    private int inputCount = FragNeuralTrainer.FEATURES;
    private int hiddenCount = FragNeuralTrainer.HIDDEN;
    private int epoch = 0, total = 0;
    private float density;

    public FragBrainGraphView(Context context) {
        super(context);
        density = getResources().getDisplayMetrics().density;
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        setMinimumHeight(dp(610));
    }

    public void setView(FragNeuralTrainer.Snapshot data, String input, int configuredHidden) {
        this.snapshot = data;
        this.query = input;
        this.hiddenCount = data == null ? configuredHidden : data.hidden;
        invalidate();
    }
    public void addTrainingPoint(int epoch, int total, double loss, FragNeuralTrainer.Snapshot data) {
        this.epoch = epoch;
        this.total = total;
        losses.add(loss);
        while (losses.size() > 120) losses.remove(0);
        if (data != null) {
            this.snapshot = data;
            this.hiddenCount = data.hidden;
        }
        invalidate();
    }
    public void clearLoss() {
        losses.clear();
        epoch = 0;
        total = 0;
        invalidate();
    }

    @Override protected void onMeasure(int width, int height) {
        setMeasuredDimension(MeasureSpec.getSize(width), dp(610));
    }

    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        float w = getWidth(), pad = dp(13);
        c.drawColor(0xFF101820);
        text(c, "FRÅG / PSICONEUROSCOPIO", pad, dp(24), 0xFFACF2CD, 15, true);
        text(c, snapshot == null ? "NESSUN PESO ADDESTRATO" : "PESI + ATTIVAZIONI REALI",
                pad, dp(44), snapshot == null ? 0xFFFFBB85 : 0xFF90E4B4, 11, true);
        text(c, "384 input hash    →    " + hiddenCount + " ReLU    →    7 softmax",
                pad, dp(65), 0xFFE4F1F1, 11, false);
        text(c, "Frase: " + truncate(query, 43), pad, dp(84), 0xFFB8C3CA, 10, false);
        final float top = dp(126), bottom = dp(407);
        final float x0 = dp(43), x1 = w * 0.52f, x2 = w - dp(69);
        text(c, "INPUT", x0 - dp(19), dp(110), 0xFF8EC8FE, 10, true);
        text(c, "HIDDEN", x1 - dp(23), dp(110), 0xFFDAB1FF, 10, true);
        text(c, "OUTPUT", x2 - dp(22), dp(110), 0xFF90E6C8, 10, true);

        if (snapshot == null) {
            dashedColumn(c, x0, top, bottom, 14, 0xFF507DA0);
            dashedColumn(c, x1, top, bottom, Math.min(hiddenCount, 14), 0xFF9475A7);
            dashedColumn(c, x2, top, bottom, 7, 0xFF5B9878);
            text(c, "Rete configurata ma non ancora addestrata.",
                    pad, dp(435), 0xFFFFC29A, 12, true);
            text(c, "Usa «esempi base» e «allena Fråg».",
                    pad, dp(455), 0xFFC8D4DA, 11, false);
        } else {
            float[] inputY = distribute(top, bottom, snapshot.featureIds.length);
            float[] hiddenY = distribute(top, bottom, snapshot.hiddenIds.length);
            float[] outputY = distribute(top, bottom, snapshot.outputs);
            // Real sampled matrices, with width and hue representing trained weight.
            for (int h = 0; h < hiddenY.length; h++) {
                for (int f = 0; f < inputY.length; f++)
                    line(c, x0 + dp(6), inputY[f], x1 - dp(6), hiddenY[h],
                         snapshot.inputWeights[h][f]);
            }
            for (int o = 0; o < outputY.length; o++) {
                for (int h = 0; h < hiddenY.length; h++)
                    line(c, x1 + dp(6), hiddenY[h], x2 - dp(6), outputY[o],
                         snapshot.outputWeights[o][h]);
            }
            for (int f = 0; f < inputY.length; f++) {
                dot(c, x0, inputY[f], snapshot.featureValues[f] > 0f,
                        0xFF67B7EF);
                text(c, "f" + snapshot.featureIds[f], dp(1), inputY[f] + dp(3),
                     0xFFBFDAE9, 9, false);
            }
            for (int h = 0; h < hiddenY.length; h++) {
                dot(c, x1, hiddenY[h], snapshot.hiddenValues[h] > 0f,
                        0xFFB58BE6);
            }
            for (int o = 0; o < outputY.length; o++) {
                dot(c, x2, outputY[o], snapshot.outputProbabilities[o] > 0.3f,
                        0xFF59CFA9);
                text(c, FragNeuralTrainer.LABELS[o],
                     x2 + dp(10), outputY[o] - dp(2), 0xFFE6F8EE, 9, false);
                text(c, String.format(Locale.US, "%.0f%%",
                             snapshot.outputProbabilities[o] * 100f),
                     x2 + dp(10), outputY[o] + dp(10), 0xFF9AC4B6, 9, false);
            }
            text(c, "Mostrati " + snapshot.featureIds.length + "/" + inputCount
                   + " input e " + snapshot.hiddenIds.length + "/" + snapshot.hidden
                   + " neuroni nascosti.",
                   pad, dp(435), 0xFFE2DBEF, 11, true);
            text(c, "Linee reali campionate: verde + / rosa −.",
                   pad, dp(454), 0xFFBECBCB, 10, false);
            text(c, "Dimensione e colore derivano dai pesi reali.",
                   pad, dp(471), 0xFFBECBCB, 10, false);
        }
        float graphTop = dp(514), graphBottom = dp(585);
        text(c, "LOSS DURANTE ADDESTRAMENTO", pad, dp(502), 0xFFA1D4C0, 11, true);
        rect(c, pad, graphTop, w-pad, graphBottom, 0xFF172630);
        if (!losses.isEmpty()) {
            double min = Double.MAX_VALUE, max = -Double.MAX_VALUE;
            for (double value : losses) {
                min = Math.min(min, value); max = Math.max(max, value);
            }
            double diff = Math.max(0.0001, max-min);
            float space = w - 2*pad - dp(12);
            float[] last = null;
            for (int i=0;i<losses.size();i++) {
                float x = pad+dp(6)+(losses.size()==1 ? 0 : space*i/(losses.size()-1));
                float y = graphBottom-dp(6)-
                        (float)((losses.get(i)-min)/diff) * (graphBottom-graphTop-dp(12));
                if (last != null) {
                    p.setColor(0xFF7DDFB3);
                    p.setStrokeWidth(dp(2));
                    c.drawLine(last[0],last[1],x,y,p);
                }
                last = new float[]{x,y};
            }
            String info = String.format(Locale.US,"Epoch %d/%d · loss %.4f",
                    epoch,total,losses.get(losses.size()-1));
            text(c,info,pad+dp(8),graphTop+dp(15),0xFFF0F9F3,10,true);
        } else text(c, "La curva appare solo durante il training effettivo.",
                     pad+dp(8),graphTop+dp(20),0xFF99A9B2,10,false);
    }
    private void dashedColumn(Canvas c,float x,float top,float bottom,int count,int col){
        float[] y=distribute(top,bottom,count);
        for(float pos:y) dot(c,x,pos,false,col);
    }
    private float[] distribute(float top,float bottom,int n){
        float[] pos=new float[n];
        for(int i=0;i<n;i++) pos[i]=top+(bottom-top)*(i+0.5f)/n;
        return pos;
    }
    private void dot(Canvas c,float x,float y,boolean active,int color){
        p.setColor(active?color:0xFF334653);
        p.setStyle(Paint.Style.FILL);
        c.drawCircle(x,y,dp(active?5:3.3f),p);
        p.setStyle(Paint.Style.STROKE);
        p.setColor(color);p.setStrokeWidth(dp(.75f));
        c.drawCircle(x,y,dp(6),p);
        p.setStyle(Paint.Style.FILL);
    }
    private void line(Canvas c,float x1,float y1,float x2,float y2,float weight){
        float a=Math.min(1f, Math.abs(weight)*1.9f);
        p.setColor((Math.round((0.1f+a*0.6f)*255)<<24) |
            (weight >= 0 ? 0x0071D8AA : 0x00F180A8));
        p.setStrokeWidth(dp(0.5f+a));
        c.drawLine(x1,y1,x2,y2,p);
    }
    private void rect(Canvas c,float l,float t,float r,float b,int color){
        p.setColor(color); p.setStyle(Paint.Style.FILL); c.drawRoundRect(l,t,r,b,dp(8),dp(8),p);
    }
    private void text(Canvas c,String s,float x,float y,int color,float size,boolean bold){
        p.setColor(color);
        p.setTextSize(dp(size));
        p.setTypeface(bold ? android.graphics.Typeface.DEFAULT_BOLD : android.graphics.Typeface.DEFAULT);
        p.setStrokeWidth(1f);
        c.drawText(s,x,y,p);
    }
    private int dp(float v){return (int)(density*v+.5f);}
    private String truncate(String s,int max){return s.length()<=max?s:s.substring(0,max-1)+"…";}
}
