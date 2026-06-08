package checkyourmods.main;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ModStatisticsManager {
    private static final File STATS_FILE = new File("data", "checkyourmods_stats.txt");
    private static final Map<String, ModStats> MOD_STATISTICS = new ConcurrentHashMap<>();
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static class ModStats {
        public String modId;
        public int usageCount;
        public int violations;
        public LocalDateTime lastSeen;
        public Set<String> playerNames;

        public ModStats(String modId) {
            this.modId = modId;
            this.usageCount = 0;
            this.violations = 0;
            this.lastSeen = LocalDateTime.now();
            this.playerNames = ConcurrentHashMap.newKeySet();
        }
    }

    static {
        loadStatistics();
    }

    private static void loadStatistics() {
        if (!STATS_FILE.exists()) return;

        try (BufferedReader reader = new BufferedReader(new FileReader(STATS_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("MOD_STAT:")) {
                    parseStatLine(line);
                }
            }
        } catch (IOException e) {
            ModLogger.log("ERROR: Failed to load statistics: " + e.getMessage());
        }
    }

    private static void parseStatLine(String line) {
        try {
            String[] parts = line.substring(9).split("\\|");
            if (parts.length >= 4) {
                String modId = parts[0].trim();
                int usage = Integer.parseInt(parts[1].trim());
                int violations = Integer.parseInt(parts[2].trim());

                ModStats stats = new ModStats(modId);
                stats.usageCount = usage;
                stats.violations = violations;
                MOD_STATISTICS.put(modId, stats);
            }
        } catch (Exception e) {
            ModLogger.log("ERROR: Failed to parse statistics line: " + line);
        }
    }

    public static void recordModUsage(String modId, String playerName) {
        if (!Config.ENABLE_STATS.get()) return;

        ModStats stats = MOD_STATISTICS.computeIfAbsent(modId, ModStats::new);
        stats.usageCount++;
        stats.lastSeen = LocalDateTime.now();
        stats.playerNames.add(playerName);
        saveStatistics();
    }

    public static void recordViolation(String modId) {
        if (!Config.ENABLE_STATS.get()) return;

        ModStats stats = MOD_STATISTICS.computeIfAbsent(modId, ModStats::new);
        stats.violations++;
        stats.lastSeen = LocalDateTime.now();
        saveStatistics();
    }

    private static void saveStatistics() {
        File dataDir = new File("data");
        if (!dataDir.exists()) dataDir.mkdirs();

        try (PrintWriter writer = new PrintWriter(new FileWriter(STATS_FILE))) {
            for (ModStats stats : MOD_STATISTICS.values()) {
                String line = String.format("MOD_STAT: %s | %d | %d | [%s]",
                        stats.modId, stats.usageCount, stats.violations, FORMATTER.format(stats.lastSeen));
                writer.println(line);
            }
        } catch (IOException e) {
            ModLogger.log("ERROR: Failed to save statistics: " + e.getMessage());
        }
    }

    public static Map<String, ModStats> getAllStatistics() {
        return new ConcurrentHashMap<>(MOD_STATISTICS);
    }

    public static ModStats getStatistics(String modId) {
        return MOD_STATISTICS.get(modId);
    }

    public static List<Map.Entry<String, ModStats>> getTopMods(int limit) {
        return MOD_STATISTICS.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue().usageCount, a.getValue().usageCount))
                .limit(limit)
                .toList();
    }
}

