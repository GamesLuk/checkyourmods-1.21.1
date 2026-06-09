package checkyourmods.main;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import java.util.*;

import static checkyourmods.main.Logging.findLatestFailure;
import static net.minecraft.commands.Commands.*;

@EventBusSubscriber(modid = "checkyourmods")
public class ModCommands {
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(literal("checkyourmods")
                .requires(c -> c.hasPermission(4))
                .then(Commands.literal("requireall")
                        .executes(c -> {
                            CommandSourceStack source = c.getSource();

                            int total = 0;

                            for (Map<String, ModListPayload.ModData> mods : Main.OP_PLAYER_MODS.values()) {
                                for (ModListPayload.ModData mod : mods.values()) {

                                    String modId = mod.modId();

                                    if (modId == null || modId.isBlank()) continue;

                                    Config.addRequiredMod(modId);

                                    total++;
                                }
                            }

                            Logging.PREFIX.copy().append(
                                    Component.literal(String.valueOf(total))
                                            .withStyle(ChatFormatting.YELLOW))
                                    .append(Component.literal(" mods are now required.")
                                            .withStyle(ChatFormatting.GREEN));

                            return 1;
                        })
                )

                .then(Commands.literal("check")
                        .then(Commands.argument("player", EntityArgument.player()))
                                .executes(c -> {

                                    ServerPlayer player = EntityArgument.getPlayer(c, "player");
                                    UUID uuid = player.getUUID();

                                    CommandSourceStack source = c.getSource();

                                    Map<String, ModListPayload.ModData> mods =
                                            Main.OP_PLAYER_MODS.getOrDefault(uuid, Map.of());

                                    List<String> disallowed = new ArrayList<>();

                                    for (ModListPayload.ModData mod : mods.values()) {

                                        String modId = mod.modId();

                                        if (modId == null || modId.isBlank()) continue;

                                        boolean allowed =
                                                (Main.SERVER_MODS_CACHE != null && Main.SERVER_MODS_CACHE.containsKey(modId)) ||
                                                        Config.REQUIRED_MOD_IDS.get().contains(modId) ||
                                                        Config.OPTIONAL_MOD_IDS.get().contains(modId);

                                        if (!allowed) {
                                            disallowed.add(modId);
                                        }
                                    }

                                    disallowed.sort(String.CASE_INSENSITIVE_ORDER);

                                    StringBuilder msg = new StringBuilder();

                                    msg.append("========== OP MOD CHECK ==========\n\n");
                                    msg.append("Player: ").append(player.getName().getString()).append("\n\n");

                                    msg.append("Disallowed Mods (").append(disallowed.size()).append(")\n");

                                    if (disallowed.isEmpty()) {
                                        msg.append(" - None\n");
                                    } else {
                                        for (String m : disallowed) {
                                            msg.append(" • ").append(m).append("\n");
                                        }
                                    }

                                    msg.append("\n==============================");

                                    source.sendSystemMessage(
                                            Component.literal(msg.toString())
                                                    .withStyle(ChatFormatting.RED)
                                    );

                                    return 1;
                                })
                        )
                .then(Commands.literal("list")
                        .executes(c -> {
                            return sendModList(c.getSource(), "all");
                        })
                        .then(Commands.argument("type", StringArgumentType.word())
                                .executes(c -> {
                                    String type = StringArgumentType.getString(c, "type").toLowerCase();
                                    return sendModList(c.getSource(), type);
                                })
                        )
                )
                .then(literal("require")
                        .then(argument("modid", com.mojang.brigadier.arguments.StringArgumentType.word()))
                                .executes(c -> {
                                    CommandSourceStack context = c.getSource();

                                    String modId = StringArgumentType.getString(c, "modid");

                                    if (Config.addRequiredMod(modId)) {
                                        context.sendSystemMessage(
                                                Logging.PREFIX.copy().append(
                                                        Component.literal("Mod ")
                                                                .withStyle(ChatFormatting.WHITE))
                                                        .append(Component.literal(modId)
                                                                .withStyle(ChatFormatting.YELLOW))
                                                        .append(Component.literal(" is now required.")
                                                                .withStyle(ChatFormatting.GREEN))
                                        );

                                        Logging.log("[REQUIRE MOD] Added required mod: " + modId);
                                    } else {
                                        context.sendSystemMessage(
                                                Logging.PREFIX.copy().append(
                                                        Component.literal("Mod ")
                                                                .withStyle(ChatFormatting.WHITE))
                                                        .append(Component.literal(modId)
                                                                .withStyle(ChatFormatting.YELLOW))
                                                        .append(Component.literal(" is already required.")
                                                                .withStyle(ChatFormatting.RED))
                                        );
                                    }

                                    return 1;
                                }))
                .then(literal("allow")
                        .then(argument("modid", com.mojang.brigadier.arguments.StringArgumentType.word())
                                .executes(c -> {
                                    CommandSourceStack context = c.getSource();

                                    String modId = StringArgumentType.getString(c, "modid");

                                    if (Config.addOptionalMod(modId)) {
                                        context.sendSystemMessage(
                                                Logging.PREFIX.copy().append(
                                                        Component.literal("Mod ")
                                                                .withStyle(ChatFormatting.WHITE))
                                                        .append(Component.literal(modId)
                                                                .withStyle(ChatFormatting.YELLOW))
                                                        .append(Component.literal(" is now optional.")
                                                                .withStyle(ChatFormatting.GREEN))
                                        );

                                        Logging.log("[ALLOW MOD] Added optional mod: " + modId);
                                    } else {
                                        context.sendSystemMessage(
                                                Logging.PREFIX.copy().append(
                                                        Component.literal("Mod ")
                                                                .withStyle(ChatFormatting.WHITE))
                                                        .append(Component.literal(modId)
                                                                .withStyle(ChatFormatting.YELLOW))
                                                        .append(Component.literal(" is already optional.")
                                                                .withStyle(ChatFormatting.RED))
                                        );
                                    }

                                    return 1;
                                })))
                .then(literal("forbid")
                        .then(argument("modid", com.mojang.brigadier.arguments.StringArgumentType.word())
                                .executes(c -> {
                                    CommandSourceStack context = c.getSource();

                                    String modId = StringArgumentType.getString(c, "modid");

                                    if (Config.removeMod(modId)) {
                                        context.sendSystemMessage(
                                                Logging.PREFIX.copy().append(
                                                        Component.literal("Mod ")
                                                                .withStyle(ChatFormatting.WHITE))
                                                        .append(Component.literal(modId)
                                                                .withStyle(ChatFormatting.YELLOW))
                                                        .append(Component.literal(" is now forbidden.")
                                                                .withStyle(ChatFormatting.GREEN))
                                        );

                                        Logging.log("[FORBID MOD] Removed mod from allowed lists: " + modId);
                                    } else {
                                        context.sendSystemMessage(
                                                Logging.PREFIX.copy().append(
                                                        Component.literal("Mod ")
                                                                .withStyle(ChatFormatting.WHITE))
                                                        .append(Component.literal(modId)
                                                                .withStyle(ChatFormatting.YELLOW))
                                                        .append(Component.literal(" was not present in any configured list.")
                                                                .withStyle(ChatFormatting.RED)
                                        ));
                                    }

                                    return 1;
                                })))
                .then(literal("inspect")
                        .then(Commands.argument("player",
                                        StringArgumentType.word())
                                .executes(c -> inspectPlayer(c, 24))
                                .then(Commands.argument("days",
                                                IntegerArgumentType.integer(1))
                                        .executes(c ->
                                                inspectPlayer(
                                                        c,
                                                        IntegerArgumentType.getInteger(c, "days")
                                                )))))
                .then(Commands.literal("reload")
                        .executes(c -> {
                            CommandSourceStack source = c.getSource();

                            Config.reload();

                            source.sendSystemMessage(
                                    Logging.PREFIX.copy()
                                            .append(Component.literal("Configuration reloaded.")
                                                    .withStyle(ChatFormatting.GREEN))
                            );

                            return 1;
                        })
                )
        );
    }

    private static int inspectPlayer(
            CommandContext<CommandSourceStack> c,
            int daysBack
    ) {
        CommandSourceStack source = c.getSource();

        String player =
                StringArgumentType.getString(c, "player");

        JoinFailureInfo info =
                findLatestFailure(player, daysBack * 24);

        if (info == null) {
            source.sendSystemMessage(
                    Logging.PREFIX.copy().append(
                    Component.literal(
                            "[No failed join found for "
                                    + player
                                    + " in the last "
                                    + daysBack
                                    + " days."
                    ).withStyle(ChatFormatting.RED)
            ));

            return 0;
        }

        source.sendSystemMessage(
                Component.literal(
                        "========== CheckYourMods ==========\n\n" +
                                "Player: " + player + "\n" +
                                "Timestamp: " + info.timestamp + "\n\n" +

                                "Missing Required Mods (" +
                                info.missingMods.size() +
                                ")\n" +

                                String.join("\n",
                                        info.missingMods.stream()
                                                .map(mod -> " • " + mod)
                                                .toList()
                                ) +

                                "\n\nUnallowed Mods (" +
                                info.unallowedMods.size() +
                                ")\n" +

                                String.join("\n",
                                        info.unallowedMods.stream()
                                                .map(mod -> " • " + mod)
                                                .toList()
                                )
                ).withStyle(ChatFormatting.AQUA)
        );

        return 1;
    }

    private static int sendModList(CommandSourceStack context, String type) {

        StringBuilder message = new StringBuilder();

        message.append("========== CheckYourMods ==========\n\n");

        // SERVER MODS
        if (type.equals("all") || type.equals("installed")) {
            message.append("Server Installed Mods (")
                    .append(Main.SERVER_MODS_CACHE.size())
                    .append(")\n");

            if (Main.SERVER_MODS_CACHE.isEmpty()) {
                message.append(" - None\n");
            } else {
                Main.SERVER_MODS_CACHE.values().stream()
                        .map(ModListPayload.ModData::modId)
                        .sorted(String.CASE_INSENSITIVE_ORDER)
                        .forEach(mod -> message.append(" • ").append(mod).append("\n"));
            }

            message.append("\n");
        }

        // REQUIRED MODS
        if (type.equals("all") || type.equals("required")) {
            List<String> required = new ArrayList<>(Config.REQUIRED_MOD_IDS.get());
            required.sort(String.CASE_INSENSITIVE_ORDER);

            message.append("Required Mods (")
                    .append(required.size())
                    .append(")\n");

            if (required.isEmpty()) {
                message.append(" - None\n");
            } else {
                required.forEach(mod -> message.append(" • ").append(mod).append("\n"));
            }

            message.append("\n");
        }

        // OPTIONAL MODS
        if (type.equals("all") || type.equals("optional")) {
            List<String> optional = new ArrayList<>(Config.OPTIONAL_MOD_IDS.get());
            optional.sort(String.CASE_INSENSITIVE_ORDER);

            message.append("Optional Mods (")
                    .append(optional.size())
                    .append(")\n");

            if (optional.isEmpty()) {
                message.append(" - None\n");
            } else {
                optional.forEach(mod -> message.append(" • ").append(mod).append("\n"));
            }

            message.append("\n");
        }

        message.append("==============================");

        context.sendSystemMessage(
                Component.literal(message.toString())
                        .withStyle(ChatFormatting.AQUA)
        );

        return 1;
    }
}
