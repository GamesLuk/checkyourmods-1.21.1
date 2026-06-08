package checkyourmods.main;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.ModConfigSpec;
import java.util.List;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.ConfigValue<List<? extends String>> REQUIRED_MOD_IDS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> OPTIONAL_MOD_IDS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> MANUAL_XRAY_HASHES;
    public static final ModConfigSpec.BooleanValue ENABLE_BAN_NOTIFICATIONS;
    public static final ModConfigSpec.ConfigValue<String> BAN_NOTIFICATION_PERMISSION;

    static {
        BUILDER.comment("CheckYourMods - Advanced Security Configuration")
                .push("Detection_Logic_Explanation");

        // Detailed explanation for the administrator
        BUILDER.comment(
                "DETECTION LOGIC EXPLANATION:",
                "1. When a player attempts to join, the server checks their mod list against the REQUIRED_MOD_IDS and OPTIONAL_MOD_IDS.",
                "2. If the player is missing any required mods, they are immediately kicked and logged.",
                "3. If the player has optional mods, they are allowed to join without issue.",
                "4. If the player has mods that are not in either list, they are banned and logged as well.",
                "5. Resource packs are scanned for suspicious keywords or matching hashes, and alerts are sent if any are detected."
        );

        REQUIRED_MOD_IDS = BUILDER
                .comment("REQUIRED MODS & ALLOWED MODS OVERRIDE:",
                        "Players without these mods will be kicked and logged.",
                        "Since required mods specify what clients must have, any strictly allowed mods should be configured here or installed on the server.")
                .defineListAllowEmpty("required_mod_ids", List.of(), o -> o instanceof String);

        OPTIONAL_MOD_IDS = BUILDER
                .comment("OPTIONAL MODS: Players with these mods will be allowed to join.",
                        "These mods are not required.")
                .defineListAllowEmpty("optional_mod_ids", List.of(), o -> o instanceof String);

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
                .comment("Enable notifications to admins when a player is banned for unallowed mods")
                .define("enable_ban_notifications", true);

        BAN_NOTIFICATION_PERMISSION = BUILDER
                .comment("Permission level required to receive ban notifications (0=everyone, 2=ops, 3=super_ops, 4=server_owner)")
                .define("ban_notification_permission", "4");

        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    public static void addMod(String modId, String entry) {
        CommentedFileConfig config =
                CommentedFileConfig.of(FMLPaths.CONFIGDIR.get().resolve("checkyourmods-server.toml"));

        config.load();

        List<String> mods = config.get(entry);

        if (!mods.contains(modId)) {
            mods.add(modId);
            config.set(entry, mods);
            config.save();
        }

        config.close();
    }

    public static void removeMod(String modId, String entry) {
        CommentedFileConfig config =
                CommentedFileConfig.of(FMLPaths.CONFIGDIR.get().resolve("checkyourmods-server.toml"));

        config.load();

        List<String> mods = config.get(entry);

        if (mods.contains(modId)) {
            mods.remove(modId);
            config.set(entry, mods);
            config.save();
        }

        config.close();
    }

    public static void addRequiredMod(String modId) {
        addMod(modId, "required_mod_ids");
        removeMod(modId, "optional_mod_ids");
    }

    public static void addOptionalMod(String modId) {
        addMod(modId, "optional_mod_ids");
        removeMod(modId, "required_mod_ids");
    }

    public static void removeMod(String modId) {
        removeMod(modId, "required_mod_ids");
        removeMod(modId, "optional_mod_ids");
    }
}