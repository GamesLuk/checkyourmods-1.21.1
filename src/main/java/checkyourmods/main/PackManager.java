package checkyourmods.main;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PackManager {
    private static final File SETTINGS_FILE = new File("data", "pack_settings.txt");
    private static final File PACKS_FILE = new File("data", "tracked_packs.txt");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static boolean strictModeEnabled = false;

    // Track packs: hash -> PackInfo
    private static final Map<String, PackInfo> trackedPacks = new ConcurrentHashMap<>();

    public static class PackInfo {
        public String hash;
        public String name;
        public LocalDateTime firstSeen;
        public boolean allowed;

        public PackInfo(String hash, String name, LocalDateTime firstSeen, boolean allowed) {
            this.hash = hash;
            this.name = name;
            this.firstSeen = firstSeen;
            this.allowed = allowed;
        }
    }

    static {
        loadSettings();
        loadPacks();
    }

    public static boolean isStrictModeEnabled() {
        return strictModeEnabled;
    }

    public static void setStrictModeEnabled(boolean enabled) {
        strictModeEnabled = enabled;
        saveSettings();
    }

    private static void loadSettings() {
        if (!SETTINGS_FILE.exists()) return;
        try (BufferedReader reader = new BufferedReader(new FileReader(SETTINGS_FILE))) {
            String line = reader.readLine();
            if (line != null && line.startsWith("STRICT_MODE=")) {
                strictModeEnabled = Boolean.parseBoolean(line.split("=")[1].trim());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void saveSettings() {
        File dataDir = new File("data");
        if (!dataDir.exists()) dataDir.mkdirs();
        try (PrintWriter writer = new PrintWriter(new FileWriter(SETTINGS_FILE))) {
            writer.println("STRICT_MODE=" + strictModeEnabled);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void loadPacks() {
        if (!PACKS_FILE.exists()) return;
        try (BufferedReader reader = new BufferedReader(new FileReader(PACKS_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|");
                if (parts.length >= 4) {
                    String hash = parts[0];
                    String name = parts[1];
                    LocalDateTime firstSeen = LocalDateTime.parse(parts[2], FORMATTER);
                    boolean allowed = Boolean.parseBoolean(parts[3]);
                    trackedPacks.put(hash, new PackInfo(hash, name, firstSeen, allowed));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void savePacks() {
        File dataDir = new File("data");
        if (!dataDir.exists()) dataDir.mkdirs();
        try (PrintWriter writer = new PrintWriter(new FileWriter(PACKS_FILE))) {
            for (PackInfo info : trackedPacks.values()) {
                writer.println(info.hash + "|" + info.name + "|" + FORMATTER.format(info.firstSeen) + "|" + info.allowed);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static PackInfo getOrRegisterPack(String hash, String name) {
        return trackedPacks.computeIfAbsent(hash, k -> {
            PackInfo info = new PackInfo(hash, name, LocalDateTime.now(), false);
            savePacks();
            return info;
        });
    }

    public static void allowPack(String hash) {
        PackInfo info = trackedPacks.get(hash);
        if (info != null) {
            info.allowed = true;
            savePacks();
        } else {
            trackedPacks.put(hash, new PackInfo(hash, "Unknown", LocalDateTime.now(), true));
            savePacks();
        }
    }

    public static boolean hasExpired(PackInfo info) {
        if (info.allowed) return false;
        long hours = ChronoUnit.HOURS.between(info.firstSeen, LocalDateTime.now());
        return hours >= 24;
    }
}
