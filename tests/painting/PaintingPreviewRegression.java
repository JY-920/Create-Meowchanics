import cn.laowu.mod.client.painting.PaintingPreviewQueue;import java.util.*;import java.util.concurrent.*;
public class PaintingPreviewRegression {
 public static void main(String[] args){
  ArrayDeque<Runnable> executor=new ArrayDeque<>();List<Integer> results=new ArrayList<>();
  var queue=new PaintingPreviewQueue(executor::add);
  for(int i=0;i<1000;i++){int n=i;queue.submit(()->results.add(n));}
  if(executor.size()!=1)throw new AssertionError("Rapid editing must queue one worker, not 1000 decodes");
  executor.remove().run();if(!results.equals(List.of(999)))throw new AssertionError("Only latest preview may decode");
  queue.submit(()->{results.add(1000);queue.submit(()->results.add(1001));queue.submit(()->results.add(1002));});
  executor.remove().run();if(!results.equals(List.of(999,1000,1002)))throw new AssertionError("Coalesce updates while decoding");
  queue.close();if(queue.submit(()->results.add(-1)))throw new AssertionError("Closed screen must reject work");
  var rejecting=new PaintingPreviewQueue(r->{throw new RejectedExecutionException();});
  if(rejecting.submit(()->{}))throw new AssertionError("Scheduling failure must be visible to screen");
  var retry=new PaintingPreviewQueue(executor::add);
  var failure=new AssertionError("worker failed");
  retry.submit(()->{throw failure;});
  try{executor.remove().run();throw new IllegalStateException("Fatal worker error must not be hidden");}
  catch(AssertionError e){if(e!=failure)throw e;}
  retry.submit(()->results.add(1003));
  if(executor.isEmpty())throw new AssertionError("Refresh must schedule again after an aborted worker");
  executor.remove().run();
  if(results.get(results.size()-1)!=1003)throw new AssertionError("New preview must run after worker failure");
  System.out.println("PASS: coalesced latest preview, close, executor saturation, failure recovery");
 }
}
