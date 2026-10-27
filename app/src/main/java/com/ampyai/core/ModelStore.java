package com.ampyai.core;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/** Durable binary weights, no imported pretrained knowledge. */
public final class ModelStore {
    private static final int MAGIC=0x46524147, FORMAT=1;
    private final File weights;
    public ModelStore(File root) {
        this.weights=new File(root,"brain.bin");
    }
    public PrimitiveLM.Model load() throws IOException {
        if(!weights.exists())return PrimitiveLM.blank();
        try(DataInputStream in=new DataInputStream(
                new BufferedInputStream(new FileInputStream(weights)))){
            int magic=in.readInt(),format=in.readInt(),hidden=in.readInt(),
                context=in.readInt(),vocab=in.readInt();
            if(magic!=MAGIC||format!=FORMAT||context!=PrimitiveLM.CONTEXT||
                vocab!=PrimitiveLM.VOCAB||hidden<PrimitiveLM.START_HIDDEN||
                hidden>PrimitiveLM.MAX_HIDDEN||hidden%4!=0)
                throw new IOException("Modello neurale incompatibile.");
            PrimitiveLM.Model m=PrimitiveLM.expanded(PrimitiveLM.blank(),hidden);
            m.processedCharacters=in.readLong();
            m.trainingSessions=in.readInt();m.lastLoss=in.readFloat();
            for(float[] row:m.input)for(int i=0;i<row.length;i++)row[i]=in.readFloat();
            for(int i=0;i<m.hb.length;i++)m.hb[i]=in.readFloat();
            for(float[] row:m.output)for(int i=0;i<row.length;i++)row[i]=in.readFloat();
            for(int i=0;i<m.ob.length;i++)m.ob[i]=in.readFloat();
            return m;
        }
    }
    public synchronized void save(PrimitiveLM.Model m) throws IOException {
        File temp=new File(weights.getParentFile(),"brain.bin.tmp");
        try(DataOutputStream out=new DataOutputStream(
                new BufferedOutputStream(new FileOutputStream(temp)))){
            out.writeInt(MAGIC);out.writeInt(FORMAT);out.writeInt(m.hidden);
            out.writeInt(PrimitiveLM.CONTEXT);out.writeInt(PrimitiveLM.VOCAB);
            out.writeLong(m.processedCharacters);out.writeInt(m.trainingSessions);
            out.writeFloat(m.lastLoss);
            for(float[] row:m.input)for(float v:row)out.writeFloat(v);
            for(float v:m.hb)out.writeFloat(v);
            for(float[] row:m.output)for(float v:row)out.writeFloat(v);
            for(float v:m.ob)out.writeFloat(v);
        }
        if(weights.exists()&&!weights.delete())
            throw new IOException("Non posso sostituire i pesi.");
        if(!temp.renameTo(weights))throw new IOException("Impossibile salvare i pesi.");
    }
}
