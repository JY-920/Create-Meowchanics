package cn.laowu.mod.test;
import java.util.UUID;
import net.minecraft.gametest.framework.*;
import net.neoforged.neoforge.gametest.*;
@GameTestHolder("laowu") @PrefixGameTestTemplate(false)
public final class MaodieStateProbe {
 private static Object state() {
  try { return Class.forName("cn.laowu.mod.encounter.MaodieSummonState").getConstructor().newInstance(); }
  catch(ReflectiveOperationException e) { throw new GameTestAssertException("Missing Maodie summon state: "+e); }
 }
 private static Object invoke(Object s,String method,Class<?>[] types,Object...args) {
  try {return s.getClass().getMethod(method,types).invoke(s,args);}
  catch(ReflectiveOperationException e){throw new GameTestAssertException("State contract: "+e);}
 }
 private static String stroke(Object s,UUID id,long t){return invoke(s,"stroke",new Class<?>[]{UUID.class,long.class},id,t).toString();}
 private static int count(Object s){return (Integer)invoke(s,"count",new Class<?>[]{});}
 @GameTest(template="maodie_probe",timeoutTicks=20)
 public static void sameTickDuplicatesTimeoutAndOwnerChange(GameTestHelper h) {
  Object s=state(); UUID a=UUID.randomUUID(),b=UUID.randomUUID();
  h.assertTrue(stroke(s,a,600).equals("ACCEPTED")&&count(s)==1,"first stroke counts");
  h.assertTrue(stroke(s,a,600).equals("IGNORED")&&count(s)==1,"same tick duplicate ignored");
  h.assertTrue(stroke(s,a,602).equals("ACCEPTED")&&count(s)==2,"distinct fast clicks count");
  invoke(s,"reset",new Class<?>[]{});
  stroke(s,a,700);stroke(s,a,860);
  h.assertTrue(count(s)==1,"160 tick inactivity resets");
  stroke(s,b,870);h.assertTrue(count(s)==1,"another cat never borrows count");h.succeed();
 }
 @GameTest(template="maodie_probe",timeoutTicks=20)
 public static void thirdStrokeWaitsOnlyForHalfSecondHiss(GameTestHelper h) {
  Object s=state();UUID a=UUID.randomUUID();stroke(s,a,600);stroke(s,a,610);
  h.assertTrue(stroke(s,a,620).equals("READY"),"third is ready");
  invoke(s,"begin",new Class<?>[]{long.class},620L);
  h.assertTrue((Boolean)invoke(s,"active",new Class<?>[]{}),"last hiss pending");
  h.assertTrue(!(Boolean)invoke(s,"due",new Class<?>[]{long.class},629L)&&(Boolean)invoke(s,"due",new Class<?>[]{long.class},630L),"commit immediately after ten-tick hiss");
  h.assertTrue(stroke(s,a,625).equals("IGNORED"),"repeat during final hiss ignored");h.succeed();
 }
 @GameTest(template="maodie_probe",timeoutTicks=20)
 public static void blockedThirdStrokeCanRetry(GameTestHelper h) {
  Object s=state();UUID a=UUID.randomUUID();stroke(s,a,600);stroke(s,a,610);stroke(s,a,620);
  invoke(s,"rejectThirdStroke",new Class<?>[]{});
  h.assertTrue(count(s)==2&&stroke(s,a,630).equals("READY"),"blocked third keeps opportunity");h.succeed();
 }
}
