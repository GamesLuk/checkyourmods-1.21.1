package checkyourmods.main;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerBanManager {
    private static final File BAN_FILE = new File("data", "checkyourmods_bans.txt");
    private static final Map<String, BanInfo> BANNED_PLAYERS = new ConcurrentHashMap<>();
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static class BanInfo {
        public String playerName;
        public String playerUUID;
        public List<String> bannedMods;
        public LocalDateTime banTime;
        public String reason;
        public boolean notified;

        public BanInfo(String playerName, String playerUUID, List<String> bannedMods, String reason) {
            this.playerName = playerName;
            this.playerUUID = playerUUID;
            this.bannedMods = new ArrayList<>(bannedMods);
            this.banTime = LocalDateTime.now();
            this.reason = reason;
            this.notified = false;
        }

        @Override
        public String toString() {
            return playerName + " | UUID: " + playerUUID + " | Mods: " + String.join(", ", bannedMods) + " | Reason: " + reason;
        }
    }

    static {
        loadBans();
    }

    private static void loadBans() {
        if (!BAN_FILE.exists()) return;

        try (BufferedReader reader = new BufferedReader(new FileReader(BAN_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("BANNED:")) {
                    parseBanLine(line);
                }
            }
        } catch (IOException e) {
            ModLogger.log("ERROR: Failed to load bans: " + e.getMessage());
        }
    }

    private static void parseBanLine(String line) {
        try {
            String[] parts = line.substring(7).split("\\|");
            if (parts.length >= 3) {
                String playerName = parts[0].trim();
                String uuid = parts[1].trim();
                String modsStr = parts[2].trim();
                String reason = parts.length > 3 ? parts[3].trim() : "Forbidden Mod";

                List<String> mods = Arrays.asList(modsStr.split(","));
                BANNED_PLAYERS.put(uuid, new BanInfo(playerName, uuid, mods, reason));
            }
        } catch (Exception e) {
            ModLogger.log("ERROR: Failed to parse ban line: " + line);
        }
    }

    public static void banPlayer(String playerName, String playerUUID, List<String> bannedMods, String reason) {
        BanInfo info = new BanInfo(playerName, playerUUID, bannedMods, reason);
        BANNED_PLAYERS.put(playerUUID, info);
        saveBan(info);
        ModLogger.logBan("PLAYER BANNED: " + info);
        AuditLog.log(AuditLog.AuditAction.PLAYER_BAN, playerName, playerUUID, 
                "Mods: " + String.join(", ", bannedMods) + " | Reason: " + reason);
    }

    private static void saveBan(BanInfo ban) {
        File dataDir = new File("data");
        if (!dataDir.exists()) dataDir.mkdirs();

        String modsLine = String.join(",", ban.bannedMods);
        String banLine = String.format("BANNED: %s | %s | %s | %s | [%s]",
                ban.playerName, ban.playerUUID, modsLine, ban.reason, FORMATTER.format(ban.banTime));

        try (PrintWriter writer = new PrintWriter(new FileWriter(BAN_FILE, true))) {
            writer.println(banLine);
        } catch (IOException e) {
            ModLogger.log("ERROR: Failed to save ban: " + e.getMessage());
        }
    }

    public static boolean isPlayerBanned(String playerUUID) {
        return BANNED_PLAYERS.containsKey(playerUUID);
    }

    public static BanInfo getBanInfo(String playerUUID) {
        return BANNED_PLAYERS.get(playerUUID);
    }

    public static Map<String, BanInfo> getAllBans() {
        return new ConcurrentHashMap<>(BANNED_PLAYERS);
    }

    public static void unbanPlayer(String playerUUID) {
        BanInfo info = BANNED_PLAYERS.remove(playerUUID);
        if (info != null) {
            saveBansToFile();
            ModLogger.logBan("PLAYER UNBANNED: " + playerUUID + " (" + info.playerName + ")");
        }
    }

    private static void saveBansToFile() {
        File dataDir = new File("data");
        if (!dataDir.exists()) dataDir.mkdirs();

        try (PrintWriter writer = new PrintWriter(new FileWriter(BAN_FILE))) {
            for (BanInfo ban : BANNED_PLAYERS.values()) {
                String modsLine = String.join(",", ban.bannedMods);
                String banLine = String.format("BANNED: %s | %s | %s | %s | [%s]",
                        ban.playerName, ban.playerUUID, modsLine, ban.reason, FORMATTER.format(ban.banTime));
                writer.println(banLine);
            }
        } catch (IOException e) {
            ModLogger.log("ERROR: Failed to save bans to file: " + e.getMessage());
        }
    }

    public static void setNotified(String playerUUID, boolean notified) {
        BanInfo info = BANNED_PLAYERS.get(playerUUID);
        if (info != null) {
            info.notified = notified;
        }
    }
}



