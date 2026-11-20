package cn.laowu.mod.client.painting;
import java.util.concurrent.*;

/** At most one decoder job is scheduled; edits replace the one pending snapshot. */
public final class PaintingPreviewQueue implements AutoCloseable {
 private final Executor executor;private Runnable pending;private boolean scheduled,closed;
 public PaintingPreviewQueue(Executor executor){this.executor=executor;}
 public synchronized boolean submit(Runnable job){
  if(closed)return false;pending=job;if(scheduled)return true;scheduled=true;
  try{executor.execute(this::drain);return true;}catch(RejectedExecutionException e){scheduled=false;pending=null;return false;}
 }
 private void drain(){
  for(;;){
   Runnable job;
   synchronized(this){if(closed||pending==null){scheduled=false;return;}job=pending;pending=null;}
   try{job.run();}catch(RuntimeException|Error e){synchronized(this){scheduled=false;pending=null;}throw e;}
  }
 }
 @Override public synchronized void close(){closed=true;pending=null;}
}
