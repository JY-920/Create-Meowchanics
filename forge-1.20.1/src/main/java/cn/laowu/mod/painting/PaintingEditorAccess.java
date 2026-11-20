package cn.laowu.mod.painting;
import java.util.UUID;
public interface PaintingEditorAccess {
 long revision();
 boolean lock(UUID token,long expectedRevision);
 void unlock(UUID token);
 boolean commit(UUID token,long expectedRevision,CatPaintingData data);
}
