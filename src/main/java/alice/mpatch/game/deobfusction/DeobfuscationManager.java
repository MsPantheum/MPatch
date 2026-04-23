package alice.mpatch.game.deobfusction;

import alice.mpatch.Environment;
import alice.util.IOUtil;
import org.apache.commons.compress.compressors.lzma.LZMACompressorInputStream;

import java.io.IOException;
import java.net.URL;

public class DeobfuscationManager {

    public static DeobfuscationType TYPE;

    static {
        if (Environment.VANILLA) {
            TYPE = DeobfuscationType.NONE;
        } else if (Environment.FABRIC) {
            TYPE = DeobfuscationType.YARN;
            URL url = ClassLoader.getSystemResource("mappings/mappings.tiny");
            if(url != null){
                try {
                    System.out.println(new String(IOUtil.readURL(url)));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        } else if (Environment.LAUNCHWRAPPER) {
            URL url = ClassLoader.getSystemResource("deobfuscation_data-".concat(Environment.MC_VERSION).concat(".lzma"));
            try {
                LZMACompressorInputStream is = new LZMACompressorInputStream(url.openStream());
                byte[] data = IOUtil.getByteArray(is);
                System.out.println(new String(data));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public enum DeobfuscationType {
        NONE,
        OFFICIAL,
        YARN,
        MCP,
    }

    public static void init() {

    }
}
