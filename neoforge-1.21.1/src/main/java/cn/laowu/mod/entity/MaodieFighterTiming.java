package cn.laowu.mod.entity;

/** One half-speed action clock for server effects and the unchanged approved v11 clips. */
public final class MaodieFighterTiming {
 private MaodieFighterTiming(){}
 private static final int ACTION_TICK_SCALE=2;
 public static int actionTick(int sourceTick){return sourceTick*ACTION_TICK_SCALE;}
 public static float clipSeconds(byte phase,float ticks){return ticks/(20F*(phase>=2&&phase<=9?ACTION_TICK_SCALE:1));}
 public static int pauseTicks(int roll){return 10+Math.floorMod(roll,7);}
 public static boolean isPullPulse(int tick){return tick>=actionTick(2)&&tick<=actionTick(6)&&tick%ACTION_TICK_SCALE==0;}
 public static boolean isEarlyCombo(float tick){return tick<actionTick(15);}
 public static double orbitProgress(int tick){return Math.max(0,Math.min(1,tick/(double)duration((byte)6)));}
 public static int duration(byte phase){
  return switch(phase){
   case 0 -> 20;
   case 1 -> 16;
   case 2 -> actionTick(18);
   case 3 -> actionTick(7);
   case 4,5 -> actionTick(10);
   case 6,8 -> actionTick(6);
   case 7 -> actionTick(5);
   case 9 -> actionTick(13);
   default -> 1;
  };
 }
 public static boolean isImpact(byte phase,int tick){
  if(tick<0||tick%ACTION_TICK_SCALE!=0)return false;
  tick/=ACTION_TICK_SCALE;
  return switch(phase){
   case 2 -> tick==2||tick==4||tick==7||tick==10||tick==12||tick==15;
   case 3 -> tick==3;
   case 4 -> tick==4;
   case 7 -> tick==2;
   case 9 -> tick==8;
   default -> false;
  };
 }
}
