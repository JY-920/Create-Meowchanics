import cn.laowu.mod.client.painting.PaintingFrameUv;

public final class PaintingFrameRegression {
    private static void check(boolean value,String problem){
        if(!value)throw new AssertionError(problem);
    }
    public static void main(String[] args){
        for(int size=1;size<=8;size++){
            var spans=PaintingFrameUv.spans(size);
            var first=spans.get(0);
            var last=spans.get(spans.size()-1);
            check(first.from()==0&&first.to()==1f/16&&first.u0()==0&&first.u1()==1f/16,"First corner keeps its original texel");
            check(last.from()==size-1f/16&&last.to()==size&&last.u0()==15f/16&&last.u1()==1,"Last corner keeps its original texel");
            for(int i=1;i<spans.size();i++){
                var a=spans.get(i-1);var b=spans.get(i);
                check(a.to()==b.from()&&a.u1()==b.u0(),"Adjacent frame pieces must share both geometry and UV");
                check(b.u0()<b.u1(),"Frame UV must never wrap back to a corner texture");
            }
            for(int i=1;i<size;i++){
                final int joint=i;
                var before=spans.stream().filter(p->p.to()==joint).findFirst().orElseThrow();
                check(before.u1()>1f/16&&before.u1()<15f/16,"Block joins sample the middle rail, not a corner");
            }
        }
        System.out.println("PASS: continuous frame rail across widths and heights 1–8");
    }
}
