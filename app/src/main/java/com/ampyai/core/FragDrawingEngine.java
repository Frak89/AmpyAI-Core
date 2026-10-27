package com.ampyai.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Safe association -> executable vector-program compiler.
 * This is deterministic program synthesis from learned geometric constraints,
 * NOT open-ended Java code generation and not part of the 4-character neural LM.
 */
public final class FragDrawingEngine {
    public static final class Fact {
        public String name;
        public int sides; // 0=unknown
        public boolean equal;
        public boolean right;
        public boolean regular;
        public boolean closed;
        public String evidence;
        public boolean useful() {
            return name!=null && (sides>0 || equal || right || regular || closed);
        }
    }

    public static final class Concept {
        public final String name;
        public int sides;
        public boolean equal;
        public boolean right;
        public boolean regular;
        public boolean closed;
        public int observations;
        public Concept(String name) { this.name = cleanName(name); }
        public boolean merge(Fact f) {
            if(f.sides!=0 && sides!=0 && f.sides!=sides)
                throw new IllegalArgumentException("Contraddizione: erano "+sides+
                        " lati, il nuovo testo dichiara "+f.sides+". Non sostituisco il dato.");
            boolean changed = (sides==0 && f.sides!=0) ||
                    (!equal && f.equal) || (!right && f.right) ||
                    (!regular && f.regular) || (!closed && f.closed);
            if(f.sides!=0)sides=f.sides;
            equal|=f.equal;
            right|=f.right;
            regular|=f.regular;
            closed|=f.closed;
            observations++;
            return changed;
        }
        public String learned() {
            return name+" · lati="+(sides==0?"?":sides)+
                    " · uguali="+equal+" · angoli retti="+right+
                    " · regolare="+regular+" · chiuso="+closed+
                    " · lezioni="+observations;
        }
    }

    public static final class Point {
        public final double x,y;
        Point(double x,double y){this.x=x;this.y=y;}
    }

    public static final class Program {
        public final String name;
        public final int sides;
        public final List<Point> points;
        public final String verification;
        Program(String name,int sides,List<Point> pts,String verification) {
            this.name=name;this.sides=sides;this.points=pts;this.verification=verification;
        }
        private String num(double n) {
            return String.format(Locale.US,"%.5f",n);
        }
        /** Script parsed by FragVectorCanvas. Only M/L/Z and 0..1 coordinates. */
        public String path() {
            StringBuilder b=new StringBuilder();
            for(int i=0;i<points.size();i++) {
                Point p=points.get(i);
                b.append(i==0?"M ":" L ").append(num(p.x)).append(' ').append(num(p.y));
            }
            b.append(" Z");
            return b.toString();
        }
        /** Export a reproducible source snippet. This text is NOT compiled on device. */
        public String javaCode(int x,int y,int side) {
            if(side<20 || side>600)throw new IllegalArgumentException("Lato 20..600 px.");
            StringBuilder b=new StringBuilder(
                    "// Codice generato dalle associazioni di «"+name+"».\n"+
                    "// Eseguibile in un metodo onDraw(Canvas canvas) Android.\n"+
                    "android.graphics.Path path = new android.graphics.Path();\n");
            for(int i=0;i<points.size();i++) {
                Point p=points.get(i);
                b.append(i==0?"path.moveTo(":"path.lineTo(")
                  .append(num(x+p.x*side)).append("f, ")
                  .append(num(y+p.y*side)).append("f);\n");
            }
            b.append("path.close();\n")
             .append("android.graphics.Paint pen = new android.graphics.Paint(3);\n")
             .append("pen.setStyle(android.graphics.Paint.Style.STROKE);\n")
             .append("pen.setStrokeWidth(4f);\n")
             .append("pen.setColor(android.graphics.Color.CYAN);\n")
             .append("canvas.drawPath(path, pen);\n");
            return b.toString();
        }
    }

    private static final Pattern NUMBER_SIDES = Pattern.compile(
        "(?:\\b(3|4|5|6|7|8|9|10|11|12|tre|quattro|cinque|sei|sette|otto|nove|dieci|undici|dodici)\\s+(?:lati|segmenti)\\b)");
    private static final Pattern NAMED = Pattern.compile(
        "\\b(quadrato|triangolo|pentagono|esagono|ettagono|ottagono|ennagono|decagono|dodecagono|rettangolo|rombo)\\b");
    private static final Pattern CUSTOM = Pattern.compile(
        "\\b(?:figura|forma|poligono)\\s+(?:chiamat[oa]\\s+)?([a-zàèéìòù]{3,30})\\s+(?:ha|con|possiede|formata|è)\\b");

    public static String cleanName(String raw) {
        if(raw==null)return "";
        String text=raw.toLowerCase(Locale.ITALIAN).trim();
        if(!text.matches("[a-zàèéìòù][a-zàèéìòù0-9_-]{2,31}"))
            throw new IllegalArgumentException("Nome della forma non valido (3–32 caratteri).");
        return text;
    }

    private static int sides(String word) {
        switch(word) {
            case "tre":return 3;case "quattro":return 4;case "cinque":return 5;
            case "sei":return 6;case "sette":return 7;case "otto":return 8;
            case "nove":return 9;case "dieci":return 10;case "undici":return 11;
            case "dodici":return 12; default: return Integer.parseInt(word);
        }
    }

    /**
     * A text lesson can teach several facts; does not rely on memorizing
     * a pre-programmed square drawing. Explicit "figura FOO ha ..." is supported.
     */
    public static Fact extract(String source) {
        if(source==null || source.trim().isEmpty())return new Fact();
        String normalized=source.toLowerCase(Locale.ITALIAN)
                .replace('’','\'').replaceAll("\\s+"," ").trim();
        Fact f=new Fact();
        Matcher name=NAMED.matcher(normalized);
        if(name.find())f.name=name.group(1);
        else {
            Matcher custom=CUSTOM.matcher(normalized);
            if(custom.find())f.name=custom.group(1);
        }
        if(f.name==null)return f;
        Matcher m=NUMBER_SIDES.matcher(normalized);
        if(m.find())f.sides=sides(m.group(1));
        f.equal=normalized.matches(".*(?:lati|segmenti).{0,90}(?:uguali|stessa lunghezza|congruenti).*")
             || normalized.matches(".*(?:uguali|congruenti).{0,45}(?:lati|segmenti).*");
        f.right=normalized.contains("angoli retti") ||
                normalized.matches(".*(?:angoli|angolo).{0,40}90\\s*(?:gradi|°).*");
        f.regular=normalized.matches(".*\\bregolar[ei]\\b.*");
        f.closed=normalized.contains("contorno chiuso") || normalized.contains("chiudere il contorno")
                || normalized.contains("figura chiusa");
        f.evidence=source.length()>500?source.substring(0,500):source;
        return f;
    }

    public static String limitation(Concept c) {
        if(c.sides<3 || c.sides>12)return "Servono 3–12 lati definiti.";
        if(c.sides==4 && !(c.equal && c.right))
            return "Per un quadrato servono quattro lati uguali e quattro angoli retti.";
        if(c.sides==3 && !c.equal && !c.regular)
            return "Per il triangolo serve sapere che è equilatero o regolare.";
        if(c.sides!=4 && c.sides!=3 && !c.regular)
            return "Per un poligono di "+c.sides+" lati occorre dichiarare che è regolare.";
        return "";
    }

    /** General algorithm for a regular polygon, not a hard-coded square picture. */
    public static Program compile(Concept c) {
        String error=limitation(c);
        if(!error.isEmpty())throw new IllegalStateException(error);
        List<Point> pts=new ArrayList<>();
        double start=c.sides==4 ? -3.0*Math.PI/4.0 : -Math.PI/2.0;
        double[] xs=new double[c.sides],ys=new double[c.sides];
        double minX=Double.POSITIVE_INFINITY,maxX=Double.NEGATIVE_INFINITY;
        double minY=Double.POSITIVE_INFINITY,maxY=Double.NEGATIVE_INFINITY;
        for(int i=0;i<c.sides;i++) {
            double angle=start+(Math.PI*2*i/c.sides);
            xs[i]=Math.cos(angle);ys[i]=Math.sin(angle);
            minX=Math.min(minX,xs[i]);maxX=Math.max(maxX,xs[i]);
            minY=Math.min(minY,ys[i]);maxY=Math.max(maxY,ys[i]);
        }
        double size=Math.max(maxX-minX,maxY-minY);
        double xPad=(1-(maxX-minX)/size)/2;
        double yPad=(1-(maxY-minY)/size)/2;
        for(int i=0;i<c.sides;i++)
            pts.add(new Point(xPad+(xs[i]-minX)/size,yPad+(ys[i]-minY)/size));
        Program p=new Program(c.name,c.sides,pts,
                "Poligono regolare: "+c.sides+" lati, chiusura verificata.");
        verify(p,c);
        return p;
    }

    public static void verify(Program p,Concept source) {
        if(p.points.size()!=source.sides)throw new IllegalStateException("Vertici incompleti.");
        double[] edges=new double[p.sides];
        for(int i=0;i<p.sides;i++) {
            Point a=p.points.get(i),b=p.points.get((i+1)%p.sides);
            if(!Double.isFinite(a.x)||!Double.isFinite(a.y)||a.x<-.00001||a.x>1.00001||
               a.y<-.00001||a.y>1.00001)
                throw new IllegalStateException("Coordinate fuori limite.");
            edges[i]=Math.hypot(b.x-a.x,b.y-a.y);
            if(edges[i]<0.01)throw new IllegalStateException("Lato troppo corto.");
        }
        double ref=edges[0];
        for(double v:edges)
            if(Math.abs(v-ref)>0.0001)throw new IllegalStateException("Lati non congruenti.");
        if(source.sides==4 && source.right) {
            for(int i=0;i<4;i++) {
                Point a=p.points.get(i),b=p.points.get((i+1)%4),
                      c=p.points.get((i+2)%4);
                double abX=b.x-a.x,abY=b.y-a.y;
                double bcX=c.x-b.x,bcY=c.y-b.y;
                if(Math.abs(abX*bcX+abY*bcY)>0.0001)
                    throw new IllegalStateException("Angolo non retto.");
            }
        }
    }
    private FragDrawingEngine() {}
}
