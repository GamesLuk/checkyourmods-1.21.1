package checkyourmods.main;

import net.neoforged.neoforge.common.ModConfigSpec;
import java.util.List;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.ConfigValue<List<? extends String>> ALLOWED_MOD_IDS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> MANUAL_XRAY_HASHES;

    static {
        BUILDER.comment("CheckYourMods - Advanced Security Configuration")
                .push("Detection_Logic_Explanation");

        // Detailed explanation for the administrator
        BUILDER.comment(
                "HOW MOD DETECTION WORKS:",
                "1. The mod scans the server's 'mods' folder and creates a 'Safe List'.",
                "2. When a player joins, it compares their mods against that 'Safe List'.",
                "3. If a mod ID is already present on the server, it is NOT announced (it's considered safe).",
                "4. If a mod ID is NOT on the server, it checks the 'allowed_mod_ids' below.",
                "5. If it's not in either list, an alert is sent to the chat and logs."
        );

        ALLOWED_MOD_IDS = BUILDER
                .comment("WHITELIST: Add Mod IDs for client-side mods that are NOT on the server but are allowed.",
                        "Common examples: ['optifine', 'iris', 'sodium', 'voicechat']")
                .defineList("allowed_mod_ids", List.of("optifine", "oculus"), o -> o instanceof String);

        BUILDER.pop();

        BUILDER.push("Resource_Pack_Detection");

        BUILDER.comment(
                "HOW RESOURCE PACK DETECTION WORKS:",
                "1. Smart Scan: Automatically detects keywords like 'xray' or 'transparent' in the pack's name or metadata.",
                "2. Hash Scan: Checks if the pack's SHA-256 fingerprint matches the list below."
        );

        MANUAL_XRAY_HASHES = BUILDER
                .comment("MANUAL TRACKING: Add specific SHA-256 hashes for packs that should always trigger an alert.")
                .defineList("manual_xray_hashes", List.of("insert_hash_here"), o -> o instanceof String);

        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();
}