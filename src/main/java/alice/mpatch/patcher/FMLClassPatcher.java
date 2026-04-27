package alice.mpatch.patcher;

import alice.log.Logger;
import alice.mpatch.Environment;
import alice.util.ReflectionUtil;
import alice.util.Unsafe;
import net.minecraft.launchwrapper.Launch;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;

public class FMLClassPatcher {

    private static boolean START_DEOBFUSCATION;
    private static boolean START_PATCHING;
    private static MethodHandle applyPatch;
    private static MethodHandle map;
    private static MethodHandle unmap;

    public static void startDeobfuscation() {
        Logger.MAIN.info("FML deobfuscation started.");
        try {
            Class<?> remapper = Class.forName(Environment.FORGE_LEGACY ? "cpw.mods.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper" : "net.minecraftforge.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper", true, Launch.classLoader);
            MethodHandles.Lookup lookup = ReflectionUtil.lookup();
            Object REMAPPER = remapper.getDeclaredField("INSTANCE").get(null);
            map = lookup.findVirtual(remapper, "map", MethodType.methodType(String.class, String.class)).bindTo(REMAPPER);
            unmap = lookup.findVirtual(remapper, "unmap", MethodType.methodType(String.class, String.class)).bindTo(REMAPPER);
            START_DEOBFUSCATION = true;
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
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
        applyPatch = ReflectionUtil.findVirtual(cls, "trueApplyPatch", MethodType.methodType(byte[].class, String.class, String.class, byte[].class)).bindTo(Unsafe.getObject(Unsafe.staticFieldBase(f), Unsafe.staticFieldOffset(f)));
        START_PATCHING = true;
    }

    public static byte[] transform(byte[] classBytes, String name) {
        String _name = name.substring(0, name.length() - 6);
        String j_name = _name.replace("/", ".");
        String untransformed_name = j_name;
        String transformed_name = j_name;
        try {
            if (START_DEOBFUSCATION) {
                untransformed_name = ((String) unmap.invoke(_name)).replace("/", ".");
                transformed_name = ((String) map.invoke(_name)).replace("/", ".");
            }
            if (START_PATCHING) {
                classBytes = (byte[]) applyPatch.invoke(untransformed_name, transformed_name, classBytes);
            }
        } catch (Throwable ignored) {

        }
        return classBytes;
    }
}
