/** Dependency-free tests of the actual shelf gaze and single-hiss clock. */
public final class BoguShelfLookRegression {
 public static void main(String[] args)throws Exception{
  Class<?> type;
  try{type=Class.forName("cn.laowu.mod.entity.BoguShelfLook");}
  catch(ClassNotFoundException e){throw new AssertionError("Shelf gaze and one-shot clock are not implemented",e);}
  var yaw=type.getMethod("yaw",double.class,double.class,float.class,float.class);
  var pitch=type.getMethod("pitch",double.class,double.class,float.class);
  var active=type.getMethod("hissing",long.class,long.class);
  float y=0;
  for(int i=0;i<10;i++)y=(float)yaw.invoke(null,3D,3D,0F,y);
  check(Math.abs(y+45)<.001,"East/front player is tracked, not locked to shelf");
  for(int i=0;i<20;i++)y=(float)yaw.invoke(null,-3D,-3D,0F,y);
  check(Math.abs(y-65)<.001,"Player behind shelf is clamped rather than rotating body");
  float p=0;for(int i=0;i<10;i++)p=(float)pitch.invoke(null,8D,1D,p);
  check(Math.abs(p+35)<.001,"Vertical gaze is bounded");
  check(!(boolean)active.invoke(null,99L,100L),"Future/late state cannot start before its actual stroke");
  for(long t=100;t<110;t++)check((boolean)active.invoke(null,t,100L),"One continuous ten-tick hiss");
  for(long t:new long[]{110,111,120,1000})check(!(boolean)active.invoke(null,t,100L),"Expired hiss cannot resume or repeat");
  check(!(boolean)active.invoke(null,0L,-1L),"Loaded inactive seat never resumes a hiss");
  System.out.println("PASS: bounded shelf gaze and exact one-shot ten-tick hiss clock");
 }
 private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
}
