package alice.mpatch.patcher;

import alice.log.Logger;
import alice.mpatch.Environment;
import alice.util.ReflectionUtil;
import alice.util.Unsafe;
import net.minecraft.launchwrapper.IClassNameTransformer;
import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.Launch;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;

public class FMLClassPatcher {

    private static boolean START_PATCHING;
    private static IClassNameTransformer FML_DEOBFUSCATING_TRANSFORMER;
    private static MethodHandle applyPatch;

    public static void setFmlDeobfuscatingTransformer(IClassNameTransformer transformer) {
        Logger.MAIN.info("Get FML_DEOBFUSCATING_TRANSFORMER.");
        FML_DEOBFUSCATING_TRANSFORMER = transformer;
        assert FML_DEOBFUSCATING_TRANSFORMER instanceof IClassTransformer;
    }

    public static void startPatching() {
        Logger.MAIN.info("FML patching starts.");
        Class<?> cls;
        try {
            cls = Launch.classLoader.findClass(Environment.FORGE_LEGACY ? "cpw.mods.fml.common.patcher.ClassPatchManager" : "net.minecraftforge.fml.common.patcher.ClassPatchManager");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        Unsafe.ensureClassInitialized(cls);
        try {
            Unsafe.ensureClassInitialized(Class.forName("LZMA.LzmaInputStream"));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        Field f = ReflectionUtil.getField(cls, "INSTANCE");
        applyPatch = ReflectionUtil.findVirtual(cls, "trueApplyPatch", MethodType.methodType(byte[].class, String.class, String.class, byte[].class))
                .bindTo(Unsafe.getObject(Unsafe.staticFieldBase(f), Unsafe.staticFieldOffset(f)));
        START_PATCHING = true;
    }

    public static byte[] transform(byte[] classBytes, String name) {
        String j_name = name.substring(0, name.length() - 6).replace("/", ".");
        String untransformed_name = j_name;
        String transformed_name = j_name;
        if (FML_DEOBFUSCATING_TRANSFORMER != null) {
            untransformed_name = FML_DEOBFUSCATING_TRANSFORMER.unmapClassName(j_name);
            transformed_name = FML_DEOBFUSCATING_TRANSFORMER.remapClassName(j_name);
        }
        if (START_PATCHING) {
            try {
                classBytes = (byte[]) applyPatch.invoke(untransformed_name, transformed_name, classBytes);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }
        return classBytes;
    }
}
