package checkyourmods.main;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue ENABLE_MOD_CHECKS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> REQUIRED_MOD_IDS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> OPTIONAL_MOD_IDS;

    public static final ModConfigSpec.BooleanValue ENABLE_RESOURCE_PACK_CHECKS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> MANUAL_XRAY_HASHES;

    public static final ModConfigSpec.BooleanValue BAN_ON_UNAPPROVED_MODS;

    static {
        BUILDER.comment("CheckYourMods - Advanced Security Configuration")
                .push("Detection_Logic_Explanation");

        BUILDER.comment(
                "DETECTION LOGIC EXPLANATION:",
                "1. When a player attempts to join, the server checks their mod list against the REQUIRED_MOD_IDS and OPTIONAL_MOD_IDS.",
                "2. If the player is missing any required mods, they are immediately kicked and logged.",
                "3. If the player has optional mods, they are allowed to join without issue.",
                "4. If the player has mods that are not in either list, they are banned and logged as well.",
                "5. Resource packs are scanned for suspicious keywords or matching hashes, and alerts are sent if any are detected."
        );

        ENABLE_MOD_CHECKS = BUILDER
                .comment("Whether to enable mod checking logic entirely.")
                .define("enable_mod_checks", true);

        // FIX: "defineListAllowEmpty" durch das modernere "defineList" ersetzt
        REQUIRED_MOD_IDS = BUILDER
                .comment("REQUIRED MODS & ALLOWED MODS OVERRIDE:",
                        "Players without these mods will be kicked and logged.",
                        "Since required mods specify what clients must have, any strictly allowed mods should be configured here or installed on the server.")
                .defineList("required_mod_ids", List.of(), o -> o instanceof String);

        OPTIONAL_MOD_IDS = BUILDER
                .comment("OPTIONAL MODS: Players with these mods will be allowed to join.",
                        "These mods are not required.")
                .defineList("optional_mod_ids", List.of(), o -> o instanceof String);

        BUILDER.pop();

        BUILDER.push("Resource_Pack_Detection");

        BUILDER.comment(
                "HOW RESOURCE PACK DETECTION WORKS:",
                "1. Smart Scan: Automatically detects keywords like 'xray' or 'transparent' in the pack's name or metadata.",
                "2. Hash Scan: Checks if the pack's SHA-256 fingerprint matches the list below."
        );

        ENABLE_RESOURCE_PACK_CHECKS = BUILDER
                .comment("Whether to enable resource pack checking logic entirely.")
                .define("enable_resource_pack_checks", true);

        // FIX: Auch hier "defineList" genutzt
        MANUAL_XRAY_HASHES = BUILDER
                .comment("MANUAL TRACKING: Add specific SHA-256 hashes for packs that should always trigger an alert.")
                .defineList("manual_xray_hashes", List.of("insert_hash_here"), o -> o instanceof String);

        BUILDER.pop();

        BUILDER.push("Ban_System");

        BAN_ON_UNAPPROVED_MODS = BUILDER
                .comment("Whether to ban players who have unapproved mods or resource packs, or just kick them.")
                .define("enable_ban_on_unapproved_mods", true);

        BUILDER.pop();
    }

    public static void reload() {
        Path path = FMLPaths.CONFIGDIR.get().resolve("checkyourmods-server.toml");
        CommentedFileConfig config = CommentedFileConfig.builder(path).sync().build();
        config.load();

        ENABLE_MOD_CHECKS.set(config.get("Detection_Logic_Explanation.enable_mod_checks"));
        REQUIRED_MOD_IDS.set(config.get("Detection_Logic_Explanation.required_mod_ids"));
        OPTIONAL_MOD_IDS.set(config.get("Detection_Logic_Explanation.optional_mod_ids"));

        ENABLE_RESOURCE_PACK_CHECKS.set(config.get("Resource_Pack_Detection.enable_resource_pack_checks"));
        MANUAL_XRAY_HASHES.set(config.get("Resource_Pack_Detection.manual_xray_hashes"));

        BAN_ON_UNAPPROVED_MODS.set(config.get("Ban_System.enable_ban_on_unapproved_mods"));

        config.close();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    private static String getFullPath(String entry) {
        if (entry.equals("required_mod_ids") || entry.equals("optional_mod_ids")) {
            return "Detection_Logic_Explanation." + entry;
        }
        return entry;
    }

    private static boolean addMod(String modId, String entry) {
        Path path = FMLPaths.CONFIGDIR.get().resolve("checkyourmods-server.toml");
        CommentedFileConfig config = CommentedFileConfig.builder(path).sync().build();
        config.load();

        String fullPath = getFullPath(entry);

        // FIX: Typsicheres Auslesen ohne jegliche Compiler-Warnungen
        List<String> mods = new ArrayList<>();
        Object rawMods = config.get(fullPath);
        if (rawMods instanceof List<?> list) {
            for (Object o : list) {
                if (o instanceof String s) {
                    mods.add(s);
                }
            }
        }

        boolean changed = false;
        if (!mods.contains(modId)) {
            mods.add(modId);
            config.set(fullPath, mods);
            config.save();
            changed = true;
        }
        config.close();

        if (entry.equals("optional_mod_ids")) {
            List<String> current = new ArrayList<>(OPTIONAL_MOD_IDS.get() != null ? OPTIONAL_MOD_IDS.get() : List.of());
            if (!current.contains(modId)) {
                current.add(modId);
                OPTIONAL_MOD_IDS.set(current);
                changed = true;
            }
        } else if (entry.equals("required_mod_ids")) {
            List<String> current = new ArrayList<>(REQUIRED_MOD_IDS.get() != null ? REQUIRED_MOD_IDS.get() : List.of());
            if (!current.contains(modId)) {
                current.add(modId);
                REQUIRED_MOD_IDS.set(current);
                changed = true;
            }
        }
        return changed;
    }

    public static boolean removeMod(String modId, String entry) {
        Path path = FMLPaths.CONFIGDIR.get().resolve("checkyourmods-server.toml");
        CommentedFileConfig config = CommentedFileConfig.builder(path).sync().build();
        config.load();

        String fullPath = getFullPath(entry);

        // FIX: Typsicheres Auslesen ohne jegliche Compiler-Warnungen
        List<String> mods = new ArrayList<>();
        Object rawMods = config.get(fullPath);
        if (rawMods instanceof List<?> list) {
            for (Object o : list) {
                if (o instanceof String s) {
                    mods.add(s);
                }
            }
        }

        boolean changed = false;
        if (mods.contains(modId)) {
            mods.remove(modId);
            config.set(fullPath, mods);
            config.save();
            changed = true;
        }
        config.close();

        if (entry.equals("optional_mod_ids")) {
            List<String> current = new ArrayList<>(OPTIONAL_MOD_IDS.get() != null ? OPTIONAL_MOD_IDS.get() : List.of());
            if (current.contains(modId)) {
                current.remove(modId);
                OPTIONAL_MOD_IDS.set(current);
                changed = true;
            }
        } else if (entry.equals("required_mod_ids")) {
            List<String> current = new ArrayList<>(REQUIRED_MOD_IDS.get() != null ? REQUIRED_MOD_IDS.get() : List.of());
            if (current.contains(modId)) {
                current.remove(modId);
                REQUIRED_MOD_IDS.set(current);
                changed = true;
            }
        }
        return changed;
    }

    public static boolean addRequiredMod(String modId) {
        removeMod(modId, "optional_mod_ids");
        return addMod(modId, "required_mod_ids");
    }

    public static boolean addOptionalMod(String modId) {
        removeMod(modId, "required_mod_ids");
        return addMod(modId, "optional_mod_ids");
    }

    public static boolean removeMod(String modId) {
        boolean removedRequired = removeMod(modId, "required_mod_ids");
        boolean removedOptional = removeMod(modId, "optional_mod_ids");
        return removedRequired || removedOptional;
    }
}