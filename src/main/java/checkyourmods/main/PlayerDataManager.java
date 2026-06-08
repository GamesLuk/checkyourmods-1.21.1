package checkyourmods.main;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerDataManager {
    private static final File DATA_FILE = new File("data", "checkyourmods_player_data.txt");
    private static final Map<String, PlayerData> PLAYER_DATA = new ConcurrentHashMap<>();
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static class PlayerData {
        public String playerUUID;
        public String playerName;
        public Set<String> warnedAboutMods; // Mods the player was warned about
        public Set<String> optionallyAllowedMods; // Mods the player allowed to be optional
        public LocalDateTime lastWarning;
        public int warningCount;

        public PlayerData(String playerUUID, String playerName) {
            this.playerUUID = playerUUID;
            this.playerName = playerName;
            this.warnedAboutMods = ConcurrentHashMap.newKeySet();
            this.optionallyAllowedMods = ConcurrentHashMap.newKeySet();
            this.lastWarning = LocalDateTime.now();
            this.warningCount = 0;
        }
    }

    static {
        loadPlayerData();
    }

    private static void loadPlayerData() {
        if (!DATA_FILE.exists()) return;

        try (BufferedReader reader = new BufferedReader(new FileReader(DATA_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("PLAYER:")) {
                    parsePlayerLine(line);
                }
            }
        } catch (IOException e) {
            ModLogger.log("ERROR: Failed to load player data: " + e.getMessage());
        }
    }

    private static void parsePlayerLine(String line) {
        try {
            String[] parts = line.substring(7).split("\\|");
            if (parts.length >= 3) {
                String uuid = parts[0].trim();
                String name = parts[1].trim();
                String warned = parts.length > 2 ? parts[2].trim() : "";
                String optional = parts.length > 3 ? parts[3].trim() : "";

                PlayerData data = new PlayerData(uuid, name);
                if (!warned.isEmpty()) {
                    data.warnedAboutMods.addAll(Arrays.asList(warned.split(",")));
                }
                if (!optional.isEmpty()) {
                    data.optionallyAllowedMods.addAll(Arrays.asList(optional.split(",")));
                }
                PLAYER_DATA.put(uuid, data);
            }
        } catch (Exception e) {
            ModLogger.log("ERROR: Failed to parse player line: " + line);
        }
    }

    public static PlayerData getOrCreatePlayerData(String playerUUID, String playerName) {
        return PLAYER_DATA.computeIfAbsent(playerUUID, k -> {
            PlayerData data = new PlayerData(playerUUID, playerName);
            savePlayerData(data);
            return data;
        });
    }

    public static void addWarning(String playerUUID, String playerName, String modId) {
        PlayerData data = getOrCreatePlayerData(playerUUID, playerName);
        data.warnedAboutMods.add(modId);
        data.lastWarning = LocalDateTime.now();
        data.warningCount++;
        savePlayerData(data);
        ModLogger.logWarning("Warning issued to " + playerName + " for mod: " + modId);
        AuditLog.log(AuditLog.AuditAction.PLAYER_WARN, playerName, playerUUID, "Mod: " + modId);
    }

    public static void allowModOptionally(String playerUUID, String playerName, String modId) {
        PlayerData data = getOrCreatePlayerData(playerUUID, playerName);
        data.optionallyAllowedMods.add(modId);
        savePlayerData(data);
        ModLogger.log("MOD ALLOWED OPTIONAL: " + playerName + " allowed mod: " + modId);
    }

    public static boolean hasBeenWarned(String playerUUID, String modId) {
        PlayerData data = PLAYER_DATA.get(playerUUID);
        return data != null && data.warnedAboutMods.contains(modId);
    }

    public static boolean isOptionallyAllowed(String playerUUID, String modId) {
        PlayerData data = PLAYER_DATA.get(playerUUID);
        return data != null && data.optionallyAllowedMods.contains(modId);
    }

    public static Set<String> getWarnedAboutMods(String playerUUID) {
        PlayerData data = PLAYER_DATA.get(playerUUID);
        return data != null ? new HashSet<>(data.warnedAboutMods) : new HashSet<>();
    }

    public static Set<String> getOptionallyAllowedMods(String playerUUID) {
        PlayerData data = PLAYER_DATA.get(playerUUID);
        return data != null ? new HashSet<>(data.optionallyAllowedMods) : new HashSet<>();
    }

    private static void savePlayerData(PlayerData data) {
        File dataDir = new File("data");
        if (!dataDir.exists()) dataDir.mkdirs();

        String warnedStr = String.join(",", data.warnedAboutMods);
        String optionalStr = String.join(",", data.optionallyAllowedMods);
        String line = String.format("PLAYER: %s | %s | %s | %s | [%s]",
                data.playerUUID, data.playerName, warnedStr, optionalStr, FORMATTER.format(data.lastWarning));

        // Read all data, update or add this player, write back
        Map<String, String> allData = new HashMap<>();
        if (DATA_FILE.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(DATA_FILE))) {
                String readLine;
                while ((readLine = reader.readLine()) != null) {
                    if (readLine.contains(data.playerUUID)) {
                        continue; // Skip the old entry
                    }
                    allData.put(readLine, readLine);
                }
            } catch (IOException e) {
                ModLogger.log("ERROR: Failed to read player data file: " + e.getMessage());
            }
        }

        // Write back all data
        try (PrintWriter writer = new PrintWriter(new FileWriter(DATA_FILE))) {
            for (String entry : allData.values()) {
                writer.println(entry);
            }
            writer.println(line);
        } catch (IOException e) {
            ModLogger.log("ERROR: Failed to save player data: " + e.getMessage());
        }
    }

    public static Map<String, PlayerData> getAllPlayerData() {
        return new ConcurrentHashMap<>(PLAYER_DATA);
    }
}


