import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/** Runs without Minecraft; the real runtime consumes this same action clock. */
public final class MaodieFighterTimingRegression {
 public static void main(String[] args) throws Exception {
  Class<?> timing;
  try { timing=Class.forName("cn.laowu.mod.entity.MaodieFighterTiming"); }
  catch(ClassNotFoundException missing){throw new AssertionError("Fighter must expose the approved clip hit clock",missing);}
  Method hit=timing.getMethod("isImpact",byte.class,int.class);
  Method duration=timing.getMethod("duration",byte.class);
  int[][] expected={{},{},{4,8,14,20,24,30},{6},{8},{},{},{4},{},{16}};
  int[] ends={20,16,36,14,20,20,12,10,12,26};
  for(byte phase=0;phase<expected.length;phase++){
   List<Integer> actual=new ArrayList<>(),want=new ArrayList<>();
   for(int tick=-1;tick<=40;tick++)if((boolean)hit.invoke(null,phase,tick))actual.add(tick);
   for(int tick:expected[phase])want.add(tick);
   if(!actual.equals(want))throw new AssertionError("Phase "+phase+" damage ticks: "+actual+" instead of "+want);
   if((int)duration.invoke(null,phase)!=ends[phase])throw new AssertionError("Phase "+phase+" must finish with its v11 clip");
  }
  if((boolean)hit.invoke(null,(byte)99,2))throw new AssertionError("Unknown phases cannot damage");
  Method seconds=timing.getMethod("clipSeconds",byte.class,float.class);
  for(byte phase=2;phase<=9;phase++){
   if(Math.abs((float)seconds.invoke(null,phase,10F)-.25F)>.00001F)throw new AssertionError("Actions sample approved clips at half speed");
  }
  if(Math.abs((float)seconds.invoke(null,(byte)1,10F)-.5F)>.00001F)throw new AssertionError("Idle guard must keep its natural speed");
  Method pull=timing.getMethod("isPullPulse",int.class);
  List<Integer> pulses=new ArrayList<>();
  for(int tick=0;tick<=20;tick++)if((boolean)pull.invoke(null,tick))pulses.add(tick);
  if(!pulses.equals(List.of(4,6,8,10,12)))throw new AssertionError("Pull must retain five bounded impulses on slowed clock: "+pulses);
  Method early=timing.getMethod("isEarlyCombo",float.class);
  if(!(boolean)early.invoke(null,24F)||(boolean)early.invoke(null,30F))throw new AssertionError("Only final half-speed combo hit uses finishing knockback");
  Method orbit=timing.getMethod("orbitProgress",int.class);
  if(Math.abs((double)orbit.invoke(null,6)-.5)>.00001||Math.abs((double)orbit.invoke(null,12)-1)>.00001)throw new AssertionError("Orbit must move over the full slowed backstep");
  Method pause=timing.getMethod("pauseTicks",int.class);
  for(int roll=0;roll<7;roll++)if((int)pause.invoke(null,roll)!=10+roll)throw new AssertionError("Moves need a 10-16 tick readable pause");
  System.out.println("PASS: half-speed six-hit clock, shared animation clock, pull pulses, final impulse, orbit and readable pause");
 }
}
