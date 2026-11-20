package cn.laowu.mod.client.painting;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/** One continuous casing rail, split only at its end caps and at lighting cells. */
public final class PaintingFrameUv {
    public static final float CAP=1f/16;
    public record Span(float from,float to,float u0,float u1) {
        public int cell(){return (int)from;}
    }
    private static final List<List<Span>> RAILS=IntStream.rangeClosed(1,8)
        .mapToObj(PaintingFrameUv::rail).toList();

    private PaintingFrameUv(){}
    public static List<Span> spans(int blocks){return RAILS.get(blocks-1);}

    private static List<Span> rail(int length){
        var spans=new ArrayList<Span>(length+2);
        spans.add(new Span(0,CAP,0,CAP));
        float from=CAP,u0=CAP;
        for(int cell=1;cell<=length;cell++){
            float to=cell==length?length-CAP:cell;
            float u1=cell==length?1-CAP:CAP+(to-CAP)*(1-2*CAP)/(length-2*CAP);
            spans.add(new Span(from,to,u0,u1));
            from=to;
            u0=u1;
        }
        spans.add(new Span(length-CAP,length,1-CAP,1));
        return List.copyOf(spans);
    }
}
