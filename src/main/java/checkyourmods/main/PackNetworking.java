package checkyourmods.main;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import java.util.ArrayList;
import java.util.List;

public class PackNetworking {
    private static final List<String> KEYWORDS = List.of("xray", "x-ray", "transparent", "vision", "ore", "fullbright");

    public static void handlePackVerification(ServerPlayer player, List<ResourcePackData> clientPacks) {
        List<? extends String> watchedHashes = Config.MANUAL_XRAY_HASHES.get();
        String playerName = player.getName().getString();
        String playerUUID = player.getUUID().toString();

        List<String> unverifiedPacks = new ArrayList<>();
        List<String> pendingPacks = new ArrayList<>();

        for (ResourcePackData pack : clientPacks) {
            boolean isSuspicious = false;
            String textToScan = (pack.fileName() + " " + pack.description()).toLowerCase();

            for (String word : KEYWORDS) {
                if (textToScan.contains(word)) {
                    isSuspicious = true;
                    break;
                }
            }

            if (isSuspicious || watchedHashes.contains(pack.hash())) {
                player.getServer().getPlayerList().broadcastSystemMessage(
                        Component.literal("§6[CheckYourMods] §cALERT! §e" + playerName + " §fis using a suspicious pack: §b" + pack.fileName()), false);
                ModLogger.log("PACK ALERT: " + playerName + " | File: " + pack.fileName() + " | Hash: " + pack.hash());
                AuditLog.log(AuditLog.AuditAction.PACK_ALERT, playerName, playerUUID, "Pack: " + pack.fileName() + " | Hash: " + pack.hash());
            }

            if (PackManager.isStrictModeEnabled()) {
                PackManager.PackInfo info = PackManager.getOrRegisterPack(pack.hash(), pack.fileName());
                if (!info.allowed) {
                    if (PackManager.hasExpired(info)) {
                        unverifiedPacks.add(pack.fileName() + " (Hash: " + pack.hash() + ")");
                    } else {
                        pendingPacks.add(pack.fileName() + " (Hash: " + pack.hash() + ")");
                    }
                }
            }
        }

        if (PackManager.isStrictModeEnabled()) {
            if (!unverifiedPacks.isEmpty()) {
                player.connection.disconnect(Component.literal(
                        "§c[CheckYourMods] Resource Pack Verification Required!\n" +
                        "§7The following packs have exceeded the 24h grace period without approval:\n\n" +
                        "§e" + String.join("\n", unverifiedPacks) + "\n\n" +
                        "§7Please consult the Server Administrator."
                ));
            } else if (!pendingPacks.isEmpty()) {
                player.sendSystemMessage(Component.literal(
                        "§e[CheckYourMods] §cYou have unverified resource packs! §7They must be approved within 24 hours:\n§f" + String.join("\n", pendingPacks)
                ));
            }
        }
    }
}