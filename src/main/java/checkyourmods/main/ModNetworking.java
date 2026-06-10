package checkyourmods.main;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@EventBusSubscriber(modid = "checkyourmods")
public class ModNetworking {

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(ModListPayload.TYPE, ModListPayload.STREAM_CODEC, ModNetworking::handleServer);
    }

    private static void handleServer(ModListPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            MinecraftServer server = player.getServer();
            if (server == null || Main.SERVER_MODS_CACHE == null) return;

            String expectedVersion = net.neoforged.fml.ModList.get().getModContainerById("checkyourmods")
                    .map(c -> c.getModInfo().getVersion().toString())
                    .orElse("unknown");

            if (!expectedVersion.equals(payload.cymVersion())) {
                player.connection.disconnect(Component.literal(
                        "§c[CheckYourMods] Version mismatch!\n§7Server expects version: §e" + expectedVersion + "\n§7You have: §e" + payload.cymVersion()
                ));
                Logging.log("VERSION MISMATCH: " + player.getName().getString() + " tried to join with " + payload.cymVersion() + ", expected " + expectedVersion);
                return;
            }

            // FIX: Nutzt jetzt die Server-PlayerList statt der Entity-Permissions, da diese sofort bereit ist!
            if (server.getPlayerList().isOp(player.getGameProfile())) {
                UUID playerUUID = player.getUUID();

                // Sicherheitsnetz: Falls die Map in Main null ist, initialisieren
                if (Main.OP_PLAYER_MODS == null) {
                    Main.OP_PLAYER_MODS = new java.util.concurrent.ConcurrentHashMap<>();
                }

                Main.OP_PLAYER_MODS.put(playerUUID, payload.mods());
                Logging.log("[OP CACHE] Saved " + payload.mods().size() + " mods for OP-Player: " + player.getName().getString());
                return;
            }

            processMods(player, server, payload.mods());
            PackNetworking.handlePackVerification(player, payload.packs());
        });
    }

    private static void processMods(ServerPlayer player, MinecraftServer server, Map<String, ModListPayload.ModData> clientMods) {
        UUID playerUUID = player.getUUID();
        String playerName = player.getName().getString();
        List<? extends String> requiredIds = Config.REQUIRED_MOD_IDS.get();
        List<? extends String> optionalIds = Config.OPTIONAL_MOD_IDS.get();

        Set<String> serverIds = new HashSet<>();
        Main.SERVER_MODS_CACHE.values().forEach(d -> serverIds.add(d.modId()));

        // Check for missing required mods
        List<String> missingRequired = new ArrayList<>();
        for (String required : requiredIds) {
            if (!serverIds.contains(required)) {
                boolean found = false;
                for (ModListPayload.ModData data : clientMods.values()) {
                    String cleanId = data.modId().split("#")[0].trim();
                    if (cleanId.equals(required)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    missingRequired.add(required);
                }
            }
        }

        // Check for extra/optional mods
        List<String> unallowedMods = new ArrayList<>();
        for (ModListPayload.ModData data : clientMods.values()) {
            String cleanId = data.modId().split("#")[0].trim();
            if (!serverIds.contains(cleanId) && !optionalIds.contains(cleanId)) {
                unallowedMods.add(cleanId);
            }
        }

        if (!unallowedMods.isEmpty() || !missingRequired.isEmpty()) {
            Logging.log(
                    "JOIN_CHECK_FAILED | Player=" + playerName +
                            " | UUID=" + playerUUID +
                            " | Missing=" + missingRequired +
                            " | Unallowed=" + unallowedMods
            );

            Component kickMessage =
                    Component.literal("CheckYourMods\n\n")
                            .withStyle(ChatFormatting.RED, ChatFormatting.BOLD)
                            .append(Component.literal("                                                    \n\n")
                                    .withStyle(ChatFormatting.DARK_GRAY).withStyle(ChatFormatting.STRIKETHROUGH))
                            .append(Component.literal("You cannot join this server right now.\n\n")
                                    .withStyle(ChatFormatting.WHITE));

            if (!missingRequired.isEmpty()) {
                kickMessage = kickMessage.copy().append(
                        Component.literal("You are missing required mods:\n\n").withStyle(ChatFormatting.YELLOW)
                );

                int index = 1;
                for (String mod : missingRequired) {
                    if(index > 5) {
                        kickMessage = kickMessage.copy().append(
                                Component.literal(" • and " + (missingRequired.size() - 5) + " more...\n\n").withStyle(ChatFormatting.GRAY)
                        );
                        break;
                    }
                    kickMessage = kickMessage.copy().append(
                            Component.literal(" • " + mod + "\n\n").withStyle(ChatFormatting.RESET).withStyle(ChatFormatting.AQUA)
                    );
                    index++;
                }
                kickMessage = kickMessage.copy().append(Component.literal("\n"));
            }

            if (!unallowedMods.isEmpty()) {
                kickMessage = kickMessage.copy().append(
                        Component.literal("These mods are not allowed here:\n").withStyle(ChatFormatting.YELLOW)
                );

                int index = 1;
                for (String mod : unallowedMods) {
                    if(index > 5) {
                        kickMessage = kickMessage.copy().append(
                                Component.literal(" • and " + (unallowedMods.size() - 5) + " more...\n\n").withStyle(ChatFormatting.GRAY)
                        );
                        break;
                    }
                    kickMessage = kickMessage.copy().append(
                            Component.literal(" • " + mod + "\n").withStyle(ChatFormatting.RED)
                    );
                    index++;
                }
                kickMessage = kickMessage.copy().append(Component.literal("\n"));
            }

            kickMessage = kickMessage.copy().append(
                    Component.literal("Please update your mod setup and try again.").withStyle(ChatFormatting.WHITE)
            );

            player.connection.disconnect(kickMessage);

            if (Config.BAN_ON_UNAPPROVED_MODS.get() && !unallowedMods.isEmpty()) {
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd. MMMM yyyy 'at' HH:mm"));

                String banMessage = "§c§lUnallowed Mods :(\n" +
                        "\n§fHey! You are using some mods that aren't allowed here yet.\n" +
                        "§fIf you think they're fair and we should add them, just let us know!\n" +
                        "\n§7Banned on: §e" + timestamp + "\n" +
                        "\n§8§m                                                    §r\n" +
                        "\n§7Just text us if you want to get your mods approved\n" +
                        "§7or if you think this was a mistake! :)";

                Main.banPlayer(playerName, playerUUID, banMessage, kickMessage);
            }
        }
    }
}