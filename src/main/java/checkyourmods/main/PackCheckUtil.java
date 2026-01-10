package checkyourmods.main;

import java.io.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.*;

public class PackCheckUtil {
    public static List<ResourcePackData> generateCurrentPackList() {
        List<ResourcePackData> packs = new ArrayList<>();
        File folder = new File("resourcepacks");
        if (!folder.exists()) return packs;

        File[] files = folder.listFiles();
        if (files != null) {
            for (File f : files) {
                String desc = "no_description";
                String hash = "0";
                try {
                    hash = getFileChecksum(f);
                    if (f.getName().endsWith(".zip")) {
                        try (ZipFile zip = new ZipFile(f)) {
                            ZipEntry entry = zip.getEntry("pack.mcmeta");
                            if (entry != null) {
                                try (BufferedReader r = new BufferedReader(new InputStreamReader(zip.getInputStream(entry)))) {
                                    desc = r.lines().filter(l -> l.contains("\"description\"")).findFirst().orElse(desc);
                                }
                            }
                        }
                    }
                } catch (Exception e) {}
                packs.add(new ResourcePackData(f.getName(), desc.toLowerCase(), hash));
            }
        }
        return packs;
    }

    private static String getFileChecksum(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[1024];
            int count;
            while ((count = fis.read(buffer)) != -1) digest.update(buffer, 0, count);
        }
        StringBuilder sb = new StringBuilder();
        for (byte b : digest.digest()) sb.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
        return sb.toString();
    }
}