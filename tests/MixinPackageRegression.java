import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarFile;

/** Check shipped bytecode without loading game classes or touching a world. */
public final class MixinPackageRegression {
    private static int checks;
    private static final String BRIDGE = "cn/laowu/mod/CatNavigationAccessor";

    public static void main(String[] args) throws Exception {
        for (String path : args) {
            try (JarFile jar = new JarFile(path)) {
                JsonObject config;
                try (InputStream in = jar.getInputStream(jar.getJarEntry("laowu.mixins.json"))) {
                    config = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8))
                            .getAsJsonObject();
                }
                String prefix = config.get("package").getAsString().replace('.', '/') + "/";
                Set<String> registered = new HashSet<>();
                for (String section : new String[] {"mixins", "client", "server"}) {
                    if (config.has(section)) config.getAsJsonArray(section).forEach(
                            value -> registered.add(prefix + value.getAsString().replace('.', '/')));
                }
                for (String mixin : registered) {
                    check(jar.getJarEntry(mixin + ".class") != null, "Missing registered mixin: " + mixin);
                }
                var entries = jar.entries();
                while (entries.hasMoreElements()) {
                    String name = entries.nextElement().getName();
                    if (!name.startsWith(prefix) || !name.endsWith(".class") || name.contains("$")) continue;
                    ClassInfo info = read(jar, name);
                    // Mixin reserves the entire package, even for unregistered ordinary helpers.
                    check(info.mixin, "Ordinary runtime type in reserved mixin package: " + name);
                    check(registered.contains(name.substring(0, name.length() - 6)),
                            "Unregistered mixin: " + name);
                }
                check(jar.getJarEntry(prefix + "CatNavigationAccessor.class") == null,
                        "Stale navigation bridge left in reserved package");
                ClassInfo bridge = read(jar, BRIDGE + ".class");
                check(!bridge.mixin && (bridge.access & Opcodes.ACC_INTERFACE) != 0,
                        "Navigation bridge must be an ordinary public runtime interface");
                check(!BRIDGE.startsWith(prefix), "Bridge cannot be in the mixin package");
                check(bridge.methods.contains("laowu$setNavigation") && bridge.methods.contains("laowu$setMoveControl"),
                        "Bridge must keep both navigation setters");
                ClassInfo navigation = read(jar, prefix + "CatNavigationMixin.class");
                check(navigation.interfaces.contains(BRIDGE), "Cat mixin must implement relocated bridge");
                boolean forge = path.contains("forge-1.20.1");
                check(navigation.writtenFields.contains(forge ? "f_21344_" : "navigation"),
                        "Navigation field must have the production mapping");
                check(navigation.writtenFields.contains(forge ? "f_21342_" : "moveControl"),
                        "Move control field must have the production mapping");
                ClassInfo flight = read(jar, "cn/laowu/mod/CatCommandFlight.class");
                check(flight.castTypes.contains(BRIDGE), "Flight controller must use relocated bridge");
                check(!flight.castTypes.contains(prefix + "CatNavigationAccessor"),
                        "Flight controller must not reference the forbidden class");
                System.out.println(path + ": mixin package and navigation bytecode OK");
            }
        }
        if (args.length == 0) throw new IllegalArgumentException("Pass the Forge and NeoForge production jars");
        System.out.println("Mixin package regression passed: " + checks + " checks");
    }

    private static ClassInfo read(JarFile jar, String entry) throws Exception {
        check(jar.getJarEntry(entry) != null, "Missing class: " + entry);
        ClassInfo info = new ClassInfo();
        try (InputStream in = jar.getInputStream(jar.getJarEntry(entry))) {
            new ClassReader(in).accept(info, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        }
        return info;
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static final class ClassInfo extends ClassVisitor {
        int access;
        boolean mixin;
        final Set<String> interfaces = new HashSet<>(), methods = new HashSet<>(),
                writtenFields = new HashSet<>(), castTypes = new HashSet<>();
        ClassInfo() { super(Opcodes.ASM9); }
        @Override public void visit(int version, int access, String name, String signature,
                                    String superName, String[] interfaces) {
            this.access = access;
            this.interfaces.addAll(Set.of(interfaces));
        }
        @Override public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            if (descriptor.equals("Lorg/spongepowered/asm/mixin/Mixin;")) mixin = true;
            return null;
        }
        @Override public MethodVisitor visitMethod(int access, String name, String descriptor,
                                                   String signature, String[] exceptions) {
            methods.add(name);
            return new MethodVisitor(Opcodes.ASM9) {
                @Override public void visitFieldInsn(int opcode, String owner, String field, String descriptor) {
                    if (opcode == Opcodes.PUTFIELD) writtenFields.add(field);
                }
                @Override public void visitTypeInsn(int opcode, String type) {
                    if (opcode == Opcodes.CHECKCAST) castTypes.add(type);
                }
            };
        }
    }
}
