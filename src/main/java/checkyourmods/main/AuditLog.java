package checkyourmods.main;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class AuditLog {
    private static final File AUDIT_FILE = new File("data", "checkyourmods_audit.log");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public enum AuditAction {
        PLAYER_BAN("PLAYER_BAN"),
        PLAYER_UNBAN("PLAYER_UNBAN"),
        PLAYER_WARN("PLAYER_WARN"),
        MOD_ALLOWED("MOD_ALLOWED"),
        MOD_BLOCKED("MOD_BLOCKED"),
        REQUIRED_MOD_MISSING("REQUIRED_MOD_MISSING"),
        BANNED_MOD_DETECTED("BANNED_MOD_DETECTED"),
        PACK_ALERT("PACK_ALERT"),
        CONFIG_RELOAD("CONFIG_RELOAD");

        public final String label;

        AuditAction(String label) {
            this.label = label;
        }
    }

    public static void log(AuditAction action, String playerName, String playerUUID, String details) {
        File dataDir = new File("data");
        if (!dataDir.exists()) dataDir.mkdirs();

        try (PrintWriter writer = new PrintWriter(new FileWriter(AUDIT_FILE, true))) {
            String timestamp = LocalDateTime.now().format(FORMATTER);
            String logLine = String.format("[%s] %s | Player: %s | UUID: %s | Details: %s",
                    timestamp, action.label, playerName, playerUUID, details);
            writer.println(logLine);
        } catch (IOException e) {
            ModLogger.log("ERROR: Failed to write audit log: " + e.getMessage());
        }
    }

    public static List<String> getAuditLog(int lines) {
        List<String> result = new ArrayList<>();
        if (!AUDIT_FILE.exists()) return result;

        try (BufferedReader reader = new BufferedReader(new FileReader(AUDIT_FILE))) {
            String line;
            LinkedList<String> buffer = new LinkedList<>();
            
            while ((line = reader.readLine()) != null) {
                buffer.add(line);
                if (buffer.size() > lines) {
                    buffer.removeFirst();
                }
            }
            
            result.addAll(buffer);
        } catch (IOException e) {
            ModLogger.log("ERROR: Failed to read audit log: " + e.getMessage());
        }

        return result;
    }
}

