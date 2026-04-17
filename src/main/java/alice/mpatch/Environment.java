package alice.mpatch;

import alice.log.Logger;

public class Environment {
    public static final boolean LAUNCHWRAPPER;
    public static final boolean FORGE_LEGACY;
    public static final boolean FORGE;
    public static final boolean OPTIFINE;
    public static final boolean FABRIC;
    public static final boolean QUILT;

    static {
        Logger.MAIN.info("Checking environment...");
        boolean tmp;
        try {
            Class.forName("net.minecraft.launchwrapper.IClassTransformer",false,ClassLoader.getSystemClassLoader());
            Logger.MAIN.info("LaunchWrapper detected.");
            tmp = true;
        } catch (ClassNotFoundException e) {
            tmp = false;
        }
        LAUNCHWRAPPER = tmp;
        try {
            Class.forName("net.minecraftforge.fml.relauncher.CoreModManager",false,ClassLoader.getSystemClassLoader());
            Logger.MAIN.info("Forge detected.");
            tmp = true;
        } catch (ClassNotFoundException e) {
            tmp = false;
        }
        FORGE = tmp;
        try {
            Class.forName("cpw.mods.fml.relauncher.CoreModManager",false,ClassLoader.getSystemClassLoader());
            Logger.MAIN.info("LegacyForge detected.");
            tmp = true;
        } catch (ClassNotFoundException e) {
            tmp = false;
        }
        FORGE_LEGACY = tmp;
        try {
            Class.forName("net.optifine.Config",false,ClassLoader.getSystemClassLoader());
            Logger.MAIN.info("Optifine detected.");
            tmp = true;
        } catch (ClassNotFoundException e) {
            tmp = false;
        }
        OPTIFINE = tmp;
        try {
            Class.forName("net.fabricmc.loader.impl.launch.knot.Knot",false,ClassLoader.getSystemClassLoader());
            Logger.MAIN.info("Fabric detected.");
            tmp = true;
        } catch (ClassNotFoundException e) {
            tmp = false;
        }
        FABRIC = tmp;
        try {
            Class.forName("org.quiltmc.loader.impl.launch.knot.Knot");
            Logger.MAIN.info("Quilt detected.");
            tmp = true;
        } catch (ClassNotFoundException e) {
            tmp = false;
        }
        QUILT = tmp;
        Logger.MAIN.info("Environment checked.");
    }
}
