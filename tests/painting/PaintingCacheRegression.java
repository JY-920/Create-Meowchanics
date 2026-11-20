import cn.laowu.mod.client.painting.PaintingUploadTracker;
import java.util.*;
public class PaintingCacheRegression {
 public static void main(String[] args){
  var state=new PaintingUploadTracker();UUID first=state.start();
  if(state.end(UUID.randomUUID()) || !state.accepts(first))throw new AssertionError("Unrelated download error cancelled upload");
  state.clear();UUID next=state.start();
  if(state.accepts(first)||state.end(first)||!state.accepts(next))throw new AssertionError("Late old session must not replace/cancel current upload");
  if(!state.end(next)||state.accepts(next)||state.end(next))throw new AssertionError("Finish once only");
  System.out.println("PASS: upload session correlation and stale replies");
 }
}
