import cn.laowu.mod.PetTeamPolicy;
import cn.laowu.mod.client.LaserPointerFrame;
import java.util.UUID;
import org.joml.*;

public final class PetTeamLaserRegression {
    private static void check(boolean value) { if (!value) throw new AssertionError(); }
    public static void main(String[] args) {
        UUID a = new UUID(0,1), b = new UUID(0,2);
        // Same owner, different collars: battle even when that owner has a team.
        check(!PetTeamPolicy.friendly(1,2,a,a,"red","red"));
        check(!PetTeamPolicy.friendly(1,2,a,b,"red","blue"));
        check(!PetTeamPolicy.friendly(1,2,a,b,null,null));
        check(PetTeamPolicy.friendly(1,2,a,b,"red","red"));
        check(PetTeamPolicy.friendly(1,1,a,b,null,null));
        check(PetTeamPolicy.friendly(1,1,a,a,null,null));
        check(!PetTeamPolicy.friendly(1,2,null,b,"red","red"));
        int cases = 0;
        for (int hand : new int[]{-1,1}) for (int yaw = -180; yaw <= 180; yaw += 15)
            for (int pitch = -90; pitch <= 90; pitch += 15)
                for (float roll : new float[]{-.6f,0,.6f}) {
                    Matrix4f matrix = new Matrix4f().translate(.55f*hand,-.4f,-.7f)
                        .rotateXYZ((float) java.lang.Math.toRadians(pitch),
                            (float) java.lang.Math.toRadians(yaw),roll).scale(.8f);
                    Vector3f origin = matrix.getTranslation(new Vector3f());
                    Vector3f target = new Vector3f(0,0,-32);
                    Vector3f local = new Matrix4f(matrix).invert().transformPosition(new Vector3f(target));
                    check(LaserPointerFrame.orient(matrix,local,new Vector3f(0,1,0),new Vector3f(0,0,1)));
                    Vector3f direction = matrix.transformDirection(new Vector3f(0,0,1)).normalize();
                    check(direction.dot(new Vector3f(target).sub(origin).normalize()) > .99999f);
                    check(matrix.getTranslation(new Vector3f()).distance(origin) < .00001f);
                    check(matrix.determinant() > 0);
                    cases++;
                }
        // Vertical look uses a finite fallback frame.
        Matrix4f vertical = new Matrix4f();
        check(LaserPointerFrame.orient(vertical,new Vector3f(0,32,0),
            new Vector3f(0,1,0),new Vector3f(0,0,1)));
        check(vertical.isFinite());
        System.out.println("PASS: team policy + " + cases + " hand poses + vertical aim");
    }
}
