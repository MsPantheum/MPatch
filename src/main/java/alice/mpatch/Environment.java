package alice.mpatch;

import alice.log.Logger;
import alice.util.FileUtil;
import alice.util.IOUtil;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Provides environment information like the game version and whether there are other mod loaders.<br>Note that Optifine isn't count as a mod loader.
 */
public class Environment {

    public static final boolean LAUNCHWRAPPER;
    public static final boolean MODLAUNCHER;
    public static final boolean FORGE_LEGACY;
    public static final boolean FORGE;
    public static final boolean OPTIFINE;
    public static final boolean FABRIC;
    public static final boolean QUILT;
    public static final boolean VANILLA;
    public static final boolean CLEANROOM;
    public static final String MC_VERSION;

    static {
        Logger.MAIN.info("Checking environment...");
        boolean tmp;
        try {
            Class.forName("net.minecraft.launchwrapper.IClassTransformer", false, ClassLoader.getSystemClassLoader());
            Logger.MAIN.info("LaunchWrapper detected.");
            tmp = true;
        } catch (ClassNotFoundException e) {
            tmp = false;
        }
        LAUNCHWRAPPER = tmp;
        try {
            Class.forName("cpw.mods.bootstraplauncher.BootstrapLauncher");
            Logger.MAIN.info("Modlauncher detected.");
            tmp = true;
        } catch (ClassNotFoundException e) {
            tmp = false;
        }
        MODLAUNCHER = tmp;
        try {
            Class.forName("net.minecraftforge.fml.relauncher.CoreModManager", false, ClassLoader.getSystemClassLoader());
            Logger.MAIN.info("Forge detected.");
            tmp = true;
        } catch (ClassNotFoundException e) {
            tmp = false;
        }
        FORGE = tmp;
        try {
            Class.forName("cpw.mods.fml.relauncher.CoreModManager", false, ClassLoader.getSystemClassLoader());
            Logger.MAIN.info("LegacyForge detected.");
            tmp = true;
        } catch (ClassNotFoundException e) {
            tmp = false;
        }
        FORGE_LEGACY = tmp;
        try {
            Class.forName("net.optifine.Config", false, ClassLoader.getSystemClassLoader());
            Logger.MAIN.info("Optifine detected.");
            tmp = true;
        } catch (ClassNotFoundException e) {
            tmp = false;
        }
        OPTIFINE = tmp;
        try {
            Class.forName("net.fabricmc.loader.impl.launch.knot.Knot", false, ClassLoader.getSystemClassLoader());
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
        try {
            Class.forName("top.outlands.foundation.boot.Foundation");
            tmp = true;
        } catch (ClassNotFoundException e) {
            tmp = false;
        }
        CLEANROOM = tmp;
        final String[] tmp_str = {null};
        if (System.getProperty("mpatch.mc_version") != null) {
            tmp_str[0] = System.getProperty("mpatch.mc_version");
        } else {
            Path path = FileUtil.WORKING_DIR;
            if (FileUtil.isDirectory(path.resolve("versions"))) {
                path = path.resolve("versions");
                String[] classpath = System.getProperty("java.class.path").split(File.pathSeparator);
                final String s = path.toString();
                Optional<String> location = Arrays.stream(classpath).filter(p -> p.contains(s)).findFirst();
                if (location.isPresent()) {
                    path = Paths.get(location.get()).getParent();
                    String client_jar = location.get();
                    try (ZipFile zip = new ZipFile(client_jar)) {
                        ZipEntry entry = zip.getEntry("version.json");
                        if (entry != null) {
                            InputStream is = zip.getInputStream(entry);
                            byte[] version_json = IOUtil.getByteArray(is);
                            String json_string = new String(version_json);
                            JSONObject json = new JSONObject(json_string);
                            tmp_str[0] = json.getString("id");
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    Logger.MAIN.warn("Failed to locate versions directory.");
                }
            }
            if (tmp_str[0] == null) {
                Path version_json = path.resolve(path.getFileName().toString() + ".json");
                String json_string = new String(FileUtil.read(version_json));
                JSONObject json = new JSONObject(json_string);
                JSONArray patches = json.getJSONArray("patches");
                patches.forEach(o -> {
                    if(o instanceof JSONObject) {
                        JSONObject patch = (JSONObject) o;
                        if(patch.getString("id").equals("game")){
                            tmp_str[0] = patch.getString("version");
                        }
                    }
                });
            }
        }
        if(tmp_str[0] == null) {
            throw new IllegalStateException("Failed to get Minecraft version!");
        }
        MC_VERSION = tmp_str[0];
        VANILLA = !FORGE && !FORGE_LEGACY && !FABRIC && !QUILT && !MODLAUNCHER;
        Logger.MAIN.info("Minecraft version is ".concat(MC_VERSION).concat("."));
        Logger.MAIN.info("Environment checked.");
    }
}
