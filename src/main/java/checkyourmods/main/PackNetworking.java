package checkyourmods.main;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import java.util.List;

public class PackNetworking {
    private static final List<String> KEYWORDS = List.of("xray", "x-ray", "transparent", "vision", "ore", "fullbright");

    public static void handlePackVerification(ServerPlayer player, List<ResourcePackData> clientPacks) {
        List<? extends String> watchedHashes = Config.MANUAL_XRAY_HASHES.get();
        String playerName = player.getName().getString();

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
                // Ban the player
                Main.log("PACK ALERT: " + playerName + " | File: " + pack.fileName() + " | Hash: " + pack.hash());
            }
        }
    }
}