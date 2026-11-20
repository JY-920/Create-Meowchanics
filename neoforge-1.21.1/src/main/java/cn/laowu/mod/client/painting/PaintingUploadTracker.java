package cn.laowu.mod.client.painting;
import java.util.UUID;
/** Correlates initial acknowledgements too, including close/reopen and concurrent downloads. */
public final class PaintingUploadTracker {
 private UUID active;
 public UUID start(){active=UUID.randomUUID();return active;}
 public boolean accepts(UUID id){return active!=null && active.equals(id);}
 public boolean end(UUID id){if(!accepts(id))return false;active=null;return true;}
 public void clear(){active=null;}
}
