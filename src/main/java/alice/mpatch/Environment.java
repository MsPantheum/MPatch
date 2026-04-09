package alice.mpatch;

public class Environment {
    public static final boolean LAUNCHWRAPPER;
    public static final boolean FORGE_LEGACY;
    public static final boolean FORGE;
    public static final boolean OPTIFINE;

    static {
        boolean tmp;
        try {
            Class.forName("net.minecraft.launchwrapper.IClassTransformer",false,ClassLoader.getSystemClassLoader());
            tmp = true;
        } catch (ClassNotFoundException e) {
            tmp = false;
        }
        LAUNCHWRAPPER = tmp;
        try {
            Class.forName("net.minecraftforge.fml.relauncher.CoreModManager",false,ClassLoader.getSystemClassLoader());
            tmp = true;
        } catch (ClassNotFoundException e) {
            tmp = false;
        }
        FORGE = tmp;
        try {
            Class.forName("cpw.mods.fml.relauncher.CoreModManager",false,ClassLoader.getSystemClassLoader());
            tmp = true;
        } catch (ClassNotFoundException e) {
            tmp = false;
        }
        FORGE_LEGACY = tmp;
        try {
            Class.forName("net.optifine.Config",false,ClassLoader.getSystemClassLoader());
            tmp = true;
        } catch (ClassNotFoundException e) {
            tmp = false;
        }
        OPTIFINE = tmp;
    }
}
