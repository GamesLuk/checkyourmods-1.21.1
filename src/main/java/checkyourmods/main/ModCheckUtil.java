package checkyourmods.main;

import java.io.*;
import java.util.*;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

public class ModCheckUtil {
    public static Map<String, ModListPayload.ModData> generateCurrentModList() {
        Map<String, ModListPayload.ModData> mods = new HashMap<>();
        File folder = new File("mods");
        if (folder.exists() && folder.isDirectory()) {
            File[] files = folder.listFiles((d, n) -> n.endsWith(".jar"));
            if (files != null) {
                for (File f : files) {
                    String mid = "unknown";
                    try (JarFile jar = new JarFile(f)) {
                        ZipEntry e = jar.getEntry("META-INF/neoforge.mods.toml");
                        if (e != null) {
                            try (BufferedReader r = new BufferedReader(new InputStreamReader(jar.getInputStream(e)))) {
                                String line;
                                while ((line = r.readLine()) != null) {
                                    if (line.trim().startsWith("modId")) {
                                        mid = line.split("=")[1].replace("\"", "").trim();
                                        break;
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {}
                    mods.put(f.getName(), new ModListPayload.ModData(mid, "0"));
                }
            }
        }
        return mods;
    }
}