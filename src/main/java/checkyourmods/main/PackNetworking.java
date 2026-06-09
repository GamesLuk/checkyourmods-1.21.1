package checkyourmods.main;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

public class PackNetworking {
    private static final List<String> KEYWORDS = List.of("xray", "x-ray", "transparent", "vision", "ore", "fullbright");

    public static void handlePackVerification(ServerPlayer player, List<ResourcePackData> clientPacks) {
        List<? extends String> watchedHashes = Config.MANUAL_XRAY_HASHES.get();
        String playerName = player.getName().getString();
        List<ResourcePackData> unallowedPacks = new ArrayList<>();

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
                unallowedPacks.add(pack);
            }
        }

        if (!unallowedPacks.isEmpty()) {
            // Holt nur die Dateinamen für ein sauberes Server-Log
            List<String> unallowedNames = unallowedPacks.stream().map(ResourcePackData::fileName).toList();
            Logging.log("JOIN_CHECK_FAILED | Player=" + playerName + " | Unallowed Packs=" + unallowedNames);

            Component kickMessage =
                    Component.literal("CheckYourMods\n\n")
                            .withStyle(ChatFormatting.RED, ChatFormatting.BOLD)

                            .append(Component.literal("                                                    \n\n")
                                    .withStyle(ChatFormatting.DARK_GRAY).withStyle(ChatFormatting.STRIKETHROUGH))

                            .append(Component.literal("You cannot join this server right now.\n\n")
                                    .withStyle(ChatFormatting.WHITE));

            kickMessage = kickMessage.copy().append(
                    Component.literal("These resource packs are not allowed here:\n")
                            .withStyle(ChatFormatting.YELLOW)
            );

            int index = 1;
            for (ResourcePackData pack : unallowedPacks) {
                if (index > 5) {
                    kickMessage = kickMessage.copy().append(
                            Component.literal(" • and " + (unallowedPacks.size() - 5) + " more...\n\n")
                                    .withStyle(ChatFormatting.GRAY)
                    );
                    break;
                }
                // HIER ANGEPASST: Nutzt jetzt pack.fileName() statt dem ganzen Objekt
                kickMessage = kickMessage.copy().append(
                        Component.literal(" • " + pack.fileName() + "\n")
                                .withStyle(ChatFormatting.RED)
                );
                index++;
            }

            kickMessage = kickMessage.copy().append(Component.literal("\n"));

            kickMessage = kickMessage.copy().append(
                    Component.literal("Please disable or remove these packs and try again.")
                            .withStyle(ChatFormatting.WHITE)
            );

            player.connection.disconnect(kickMessage);
        }
    }
}