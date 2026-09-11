import java.util.*;
/** Reproduce the installed JECharacters search adapter without changing its JAR/config. */
public final class CreativeSearchProbe {
    public static void main(String[] args) throws Exception {
        Class<?> type = Class.forName("me.towdium.jecharacters.utils.FakeArray");
        Object array = type.getConstructor().newInstance();
        Object pouch = new Object(), box = new Object(), food = new Object();
        var add = type.getMethod("add", Object.class, String.class);
        for (String line : List.of("猫袋", "已存放猫饼", "右键放入猫饼")) add.invoke(array, pouch, line);
        for (String line : List.of("猫盒", "已存放猫榴弹", "右键放入猫榴弹")) add.invoke(array, box, line);
        for (String line : List.of("信息素猫粮", "给猫吃", "猫咪变成宠物", "小猫立即长大")) add.invoke(array, food, line);
        List<?> result = (List<?>) type.getMethod("search", String.class).invoke(array, "猫");
        System.out.println("Installed JECharacters: pouch=" + Collections.frequency(result, pouch)
                + ", box=" + Collections.frequency(result, box) + ", food=" + Collections.frequency(result, food)
                + ", unique=" + new HashSet<>(result).size());
        if (Collections.frequency(result, pouch) != 3 || Collections.frequency(result, box) != 3
                || Collections.frequency(result, food) != 4) throw new AssertionError("Reported duplication not reproduced");
    }
}
