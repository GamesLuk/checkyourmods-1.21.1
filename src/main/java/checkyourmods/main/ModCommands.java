package checkyourmods.main;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
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

                // === REQUIRE ALL ===
                .then(Commands.literal("requireall")
                        .executes(c -> {
                            CommandSourceStack source = c.getSource();
                            int total = 0;

                            if (Main.OP_PLAYER_MODS != null) {
                                for (Map<String, ModListPayload.ModData> mods : Main.OP_PLAYER_MODS.values()) {
                                    for (ModListPayload.ModData mod : mods.values()) {
                                        String modId = mod.modId();
                                        if (modId == null || modId.isBlank()) continue;

                                        Config.addRequiredMod(modId);
                                        total++;
                                    }
                                }
                            }

                            if (total == 0) {
                                source.sendSystemMessage(
                                        Logging.PREFIX.copy()
                                                .append(Component.literal("No cached mods found! You must ").withStyle(ChatFormatting.GRAY))
                                                .append(Component.literal("rejoin with OP status").withStyle(ChatFormatting.RED, ChatFormatting.BOLD))
                                                .append(Component.literal(" first.").withStyle(ChatFormatting.GRAY))
                                );
                            } else {
                                source.sendSystemMessage(
                                        Logging.PREFIX.copy()
                                                .append(Component.literal(String.valueOf(total)).withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD))
                                                .append(Component.literal(" mods are now required.").withStyle(ChatFormatting.GREEN))
                                );
                            }
                            return 1;
                        })
                )

                // === CHECK ===
                .then(Commands.literal("check")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(c -> {
                                    ServerPlayer player = EntityArgument.getPlayer(c, "player");
                                    UUID uuid = player.getUUID();
                                    CommandSourceStack source = c.getSource();

                                    Map<String, ModListPayload.ModData> mods =
                                            (Main.OP_PLAYER_MODS != null) ? Main.OP_PLAYER_MODS.getOrDefault(uuid, Map.of()) : Map.of();

                                    List<String> disallowed = new ArrayList<>();
                                    // FIX: Null-Safe Guards hinzugefügt
                                    List<? extends String> reqMods = Config.REQUIRED_MOD_IDS.get() != null ? Config.REQUIRED_MOD_IDS.get() : List.of();
                                    List<? extends String> optMods = Config.OPTIONAL_MOD_IDS.get() != null ? Config.OPTIONAL_MOD_IDS.get() : List.of();

                                    for (ModListPayload.ModData mod : mods.values()) {
                                        String modId = mod.modId();
                                        if (modId == null || modId.isBlank()) continue;

                                        boolean allowed = (Main.SERVER_MODS_CACHE != null && Main.SERVER_MODS_CACHE.containsKey(modId)) ||
                                                reqMods.contains(modId) ||
                                                optMods.contains(modId);

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

                                    source.sendSystemMessage(Component.literal(msg.toString()).withStyle(ChatFormatting.RED));
                                    return 1;
                                })
                        )
                )

                // === LIST ===
                .then(Commands.literal("list")
                        .executes(c -> sendModList(c.getSource(), "all"))
                        .then(Commands.argument("type", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(List.of("installed", "required", "optional"), builder))
                                .executes(c -> {
                                    String type = StringArgumentType.getString(c, "type").toLowerCase();
                                    return sendModList(c.getSource(), type);
                                })
                        )
                )

                // === REQUIRE ===
                .then(literal("require")
                        .then(argument("modid", StringArgumentType.string())
                                .suggests((context, builder) -> Main.SERVER_MODS_CACHE != null ? SharedSuggestionProvider.suggest(Main.SERVER_MODS_CACHE.keySet(), builder) : builder.buildFuture())
                                .executes(c -> {
                                    CommandSourceStack context = c.getSource();
                                    String modId = StringArgumentType.getString(c, "modid");

                                    if (Config.addRequiredMod(modId)) {
                                        context.sendSystemMessage(Logging.PREFIX.copy()
                                                .append(Component.literal("Mod ").withStyle(ChatFormatting.GRAY))
                                                .append(Component.literal(modId).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
                                                .append(Component.literal(" is now ").withStyle(ChatFormatting.GRAY))
                                                .append(Component.literal("required").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD))
                                                .append(Component.literal(".").withStyle(ChatFormatting.GRAY)));
                                        Logging.log("[REQUIRE MOD] Added required mod: " + modId);
                                    } else {
                                        context.sendSystemMessage(Logging.PREFIX.copy()
                                                .append(Component.literal("Mod ").withStyle(ChatFormatting.GRAY))
                                                .append(Component.literal(modId).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
                                                .append(Component.literal(" is already ").withStyle(ChatFormatting.GRAY))
                                                .append(Component.literal("required").withStyle(ChatFormatting.RED, ChatFormatting.BOLD))
                                                .append(Component.literal(".").withStyle(ChatFormatting.GRAY)));
                                    }
                                    return 1;
                                })
                        )
                )

                // === ALLOW ===
                .then(literal("allow")
                        .then(argument("modid", StringArgumentType.string())
                                .suggests((context, builder) -> Main.SERVER_MODS_CACHE != null ? SharedSuggestionProvider.suggest(Main.SERVER_MODS_CACHE.keySet(), builder) : builder.buildFuture())
                                .executes(c -> {
                                    CommandSourceStack context = c.getSource();
                                    String modId = StringArgumentType.getString(c, "modid");

                                    if (Config.addOptionalMod(modId)) {
                                        context.sendSystemMessage(Logging.PREFIX.copy()
                                                .append(Component.literal("Mod ").withStyle(ChatFormatting.GRAY))
                                                .append(Component.literal(modId).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
                                                .append(Component.literal(" is now ").withStyle(ChatFormatting.GRAY))
                                                .append(Component.literal("optional").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD))
                                                .append(Component.literal(".").withStyle(ChatFormatting.GRAY)));
                                        Logging.log("[ALLOW MOD] Added optional mod: " + modId);
                                    } else {
                                        context.sendSystemMessage(Logging.PREFIX.copy()
                                                .append(Component.literal("Mod ").withStyle(ChatFormatting.GRAY))
                                                .append(Component.literal(modId).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
                                                .append(Component.literal(" is already ").withStyle(ChatFormatting.GRAY))
                                                .append(Component.literal("optional").withStyle(ChatFormatting.RED, ChatFormatting.BOLD))
                                                .append(Component.literal(".").withStyle(ChatFormatting.GRAY)));
                                    }
                                    return 1;
                                })
                        )
                )

                // === FORBID ===
                .then(literal("forbid")
                        .then(argument("modid", StringArgumentType.string())
                                // FIX: Null-Safe Guards für Autocomplete hinzugefügt
                                .suggests((context, builder) -> {
                                    List<String> currentAllowed = new ArrayList<>();
                                    if (Config.REQUIRED_MOD_IDS.get() != null) currentAllowed.addAll(Config.REQUIRED_MOD_IDS.get());
                                    if (Config.OPTIONAL_MOD_IDS.get() != null) currentAllowed.addAll(Config.OPTIONAL_MOD_IDS.get());
                                    return SharedSuggestionProvider.suggest(currentAllowed, builder);
                                })
                                .executes(c -> {
                                    CommandSourceStack context = c.getSource();
                                    String modId = StringArgumentType.getString(c, "modid");

                                    if (Config.removeMod(modId)) {
                                        context.sendSystemMessage(Logging.PREFIX.copy()
                                                .append(Component.literal("Mod ").withStyle(ChatFormatting.GRAY))
                                                .append(Component.literal(modId).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
                                                .append(Component.literal(" is now ").withStyle(ChatFormatting.GRAY))
                                                .append(Component.literal("forbidden").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD))
                                                .append(Component.literal(".").withStyle(ChatFormatting.GRAY)));
                                        Logging.log("[FORBID MOD] Removed mod from allowed lists: " + modId);
                                    } else {
                                        context.sendSystemMessage(Logging.PREFIX.copy()
                                                .append(Component.literal("Mod ").withStyle(ChatFormatting.GRAY))
                                                .append(Component.literal(modId).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
                                                .append(Component.literal(" was not found in any list.").withStyle(ChatFormatting.RED)));
                                    }
                                    return 1;
                                })
                        )
                )

                // === INSPECT ===
                .then(literal("inspect")
                        .then(Commands.argument("player", StringArgumentType.word())
                                .executes(c -> inspectPlayer(c, 24))
                                .then(Commands.argument("days", IntegerArgumentType.integer(1))
                                        .executes(c -> inspectPlayer(c, IntegerArgumentType.getInteger(c, "days"))))
                        )
                )

                // === RELOAD ===
                .then(Commands.literal("reload")
                        .executes(c -> {
                            Config.reload();
                            c.getSource().sendSystemMessage(Logging.PREFIX.copy().append(Component.literal("Configuration reloaded.").withStyle(ChatFormatting.GREEN)));
                            return 1;
                        })
                )
        );
    }

    private static int inspectPlayer(CommandContext<CommandSourceStack> c, int daysBack) {
        CommandSourceStack source = c.getSource();
        String player = StringArgumentType.getString(c, "player");
        JoinFailureInfo info = findLatestFailure(player, daysBack * 24);

        if (info == null) {
            source.sendSystemMessage(Logging.PREFIX.copy().append(Component.literal("[No failed join found for " + player + " in the last " + daysBack + " days.").withStyle(ChatFormatting.RED)));
            return 0;
        }

        source.sendSystemMessage(Component.literal(
                "========== CheckYourMods ==========\n\n" +
                        "Player: " + player + "\n" +
                        "Timestamp: " + info.timestamp + "\n\n" +
                        "Missing Required Mods (" + info.missingMods.size() + ")\n" +
                        String.join("\n", info.missingMods.stream().map(mod -> " • " + mod).toList()) +
                        "\n\nUnallowed Mods (" + info.unallowedMods.size() + ")\n" +
                        String.join("\n", info.unallowedMods.stream().map(mod -> " • " + mod).toList())
        ).withStyle(ChatFormatting.AQUA));
        return 1;
    }

    private static int sendModList(CommandSourceStack context, String type) {
        StringBuilder message = new StringBuilder();
        message.append("========== CheckYourMods ==========\n\n");

        if ((type.equals("all") || type.equals("installed")) && Main.SERVER_MODS_CACHE != null) {
            message.append("Server Installed Mods (").append(Main.SERVER_MODS_CACHE.size()).append(")\n");
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

        if (type.equals("all") || type.equals("required")) {
            // FIX: Null-Safe Guard für die Liste eingebaut
            List<String> required = new ArrayList<>(Config.REQUIRED_MOD_IDS.get() != null ? Config.REQUIRED_MOD_IDS.get() : List.of());
            required.sort(String.CASE_INSENSITIVE_ORDER);
            message.append("Required Mods (").append(required.size()).append(")\n");
            if (required.isEmpty()) {
                message.append(" - None\n");
            } else {
                required.forEach(mod -> message.append(" • ").append(mod).append("\n"));
            }
            message.append("\n");
        }

        if (type.equals("all") || type.equals("optional")) {
            // FIX: Null-Safe Guard für die Liste eingebaut
            List<String> optional = new ArrayList<>(Config.OPTIONAL_MOD_IDS.get() != null ? Config.OPTIONAL_MOD_IDS.get() : List.of());
            optional.sort(String.CASE_INSENSITIVE_ORDER);
            message.append("Optional Mods (").append(optional.size()).append(")\n");
            if (optional.isEmpty()) {
                message.append(" - None\n");
            } else {
                optional.forEach(mod -> message.append(" • ").append(mod).append("\n"));
            }
            message.append("\n");
        }

        message.append("==============================");
        context.sendSystemMessage(Component.literal(message.toString()).withStyle(ChatFormatting.AQUA));
        return 1;
    }
}