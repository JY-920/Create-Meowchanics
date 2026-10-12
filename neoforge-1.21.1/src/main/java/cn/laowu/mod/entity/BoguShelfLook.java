package cn.laowu.mod.entity;

/** Relative shelf-head angles and the non-looping ten-tick pet response. */
public final class BoguShelfLook {
 private BoguShelfLook(){}
 public static boolean hissing(long now,long start){return start>=0&&now>=start&&now-start<10;}
 public static float yaw(double dx,double dz,float bodyYaw,float current){
  float desired=(float)Math.toDegrees(Math.atan2(-dx,dz))-bodyYaw;
  desired=(desired%360+540)%360-180;
  return approach(current,clamp(desired,-65,65));
 }
 public static float pitch(double dy,double horizontal,float current){
  return approach(current,clamp((float)-Math.toDegrees(Math.atan2(dy,horizontal)),-35,35));
 }
 public static float approach(float current,float desired){return current+clamp(desired-current,-12,12);}
 private static float clamp(float value,float min,float max){return Math.max(min,Math.min(max,value));}
}
