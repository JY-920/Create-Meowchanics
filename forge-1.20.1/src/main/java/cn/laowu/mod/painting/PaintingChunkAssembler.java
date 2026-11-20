package cn.laowu.mod.painting;

import java.io.IOException;
import java.util.*;

public final class PaintingChunkAssembler {
    public static final int MAX_CHUNK=24*1024;
    private final UUID session;
    private final byte[] data;
    private int next;
    private long last;
    public PaintingChunkAssembler(UUID session,int totalBytes,long nowMillis){
        if(session==null || totalBytes<1 || totalBytes>PaintingImages.MAX_PNG_BYTES)throw new IllegalArgumentException("upload_size");
        this.session=session;data=new byte[totalBytes];last=nowMillis;
    }
    public synchronized void accept(int offset,byte[] bytes,long nowMillis)throws IOException{
        if(expired(nowMillis))throw new IOException("upload_timeout");
        if(bytes==null || bytes.length<1 || bytes.length>MAX_CHUNK || offset<0 || offset>data.length-bytes.length || offset>next)
            throw new IOException("upload_chunk");
        if(offset<next){
            if(offset+bytes.length>next)throw new IOException("upload_overlap");
            for(int i=0;i<bytes.length;i++)if(data[offset+i]!=bytes[i])throw new IOException("upload_conflict");
        }else{System.arraycopy(bytes,0,data,next,bytes.length);next+=bytes.length;}
        last=nowMillis;
    }
    public synchronized int nextOffset(){return next;}
    public UUID session(){return session;}
    public synchronized boolean complete(){return next==data.length;}
    public synchronized byte[] finish()throws IOException{
        if(!complete())throw new IOException("upload_incomplete");
        return data.clone();
    }
    public synchronized boolean expired(long nowMillis){return nowMillis-last>30000;}
}
