package checkyourmods.main;

import net.neoforged.neoforge.common.ModConfigSpec;
import java.util.List;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.ConfigValue<List<? extends String>> REQUIRED_MOD_IDS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> BANNED_MOD_IDS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> MANUAL_XRAY_HASHES;
    public static final ModConfigSpec.BooleanValue ENABLE_BAN_NOTIFICATIONS;
    public static final ModConfigSpec.ConfigValue<String> BAN_NOTIFICATION_PERMISSION;
    public static final ModConfigSpec.BooleanValue ENABLE_MOD_WARNINGS;
    public static final ModConfigSpec.BooleanValue ENABLE_STATS;

    static {
        BUILDER.comment("CheckYourMods - Advanced Security Configuration")
                .push("Detection_Logic_Explanation");

        // Detailed explanation for the administrator
        BUILDER.comment(
                "HOW MOD DETECTION WORKS:",
                "1. The mod scans the server's 'mods' folder and creates a 'Safe List'.",
                "2. When a player joins, it compares their mods against that 'Safe List'.",
                "3. If a mod ID is already present on the server, it is NOT announced (it's considered safe).",
                "4. If a mod ID is NOT on the server, it checks player specific allowed mods and warns if it's new.",
                "5. If it's not safe, an alert is sent to the chat and logs."
        );

        REQUIRED_MOD_IDS = BUILDER
                .comment("REQUIRED MODS & ALLOWED MODS OVERRIDE:",
                        "Players without these mods will be kicked and logged.",
                        "Since required mods specify what clients must have, any strictly allowed mods should be configured here or installed on the server.")
                .defineListAllowEmpty("required_mod_ids", List.of(), o -> o instanceof String);

        BANNED_MOD_IDS = BUILDER
                .comment("BANNED MODS: Players with these mods will be permanently banned.",
                        "Bans are tracked and logged with player details.")
                .defineListAllowEmpty("banned_mod_ids", List.of(), o -> o instanceof String);

        BUILDER.pop();

        BUILDER.push("Resource_Pack_Detection");

        BUILDER.comment(
                "HOW RESOURCE PACK DETECTION WORKS:",
                "1. Smart Scan: Automatically detects keywords like 'xray' or 'transparent' in the pack's name or metadata.",
                "2. Hash Scan: Checks if the pack's SHA-256 fingerprint matches the list below."
        );

        MANUAL_XRAY_HASHES = BUILDER
                .comment("MANUAL TRACKING: Add specific SHA-256 hashes for packs that should always trigger an alert.")
                .defineListAllowEmpty("manual_xray_hashes", List.of("insert_hash_here"), o -> o instanceof String);

        BUILDER.pop();

        BUILDER.push("Ban_System");

        ENABLE_BAN_NOTIFICATIONS = BUILDER
                .comment("Enable notifications to admins when a player is banned for forbidden mods")
                .define("enable_ban_notifications", true);

        BAN_NOTIFICATION_PERMISSION = BUILDER
                .comment("Permission level required to receive ban notifications (0=everyone, 2=ops, 3=super_ops, 4=server_owner)")
                .define("ban_notification_permission", "2");

        BUILDER.pop();

        BUILDER.push("Features");

        ENABLE_MOD_WARNINGS = BUILDER
                .comment("Enable warnings for players who used forbidden mods")
                .define("enable_mod_warnings", true);

        ENABLE_STATS = BUILDER
                .comment("Enable collection of mod statistics for /modcheck stats command")
                .define("enable_stats", true);

        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();
}