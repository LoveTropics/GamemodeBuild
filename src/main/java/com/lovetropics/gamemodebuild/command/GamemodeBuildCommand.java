package com.lovetropics.gamemodebuild.command;

import com.lovetropics.gamemodebuild.GBConfigs;
import com.lovetropics.gamemodebuild.GamemodeBuild;
import com.lovetropics.gamemodebuild.command.ItemFilterArgument.Result;
import com.lovetropics.gamemodebuild.message.ListUpdateMessage;
import com.lovetropics.gamemodebuild.state.GBPlayer;
import com.lovetropics.gamemodebuild.state.GBServerState;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Collection;
import java.util.Optional;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public final class GamemodeBuildCommand {
	private static final SimpleCommandExceptionType FILTER_DID_NOT_EXIST = new SimpleCommandExceptionType(Component.literal("That filter did not exist!"));
	private static final DynamicCommandExceptionType SAME_STATE = new DynamicCommandExceptionType(ac -> Component.literal(GamemodeBuild.NAME + " is already " + ((boolean) ac ? "enabled" : "disabled")));

	private static RequiredArgumentBuilder<CommandSourceStack, EntitySelector> getPlayerArg() {
		return argument("player", EntityArgument.players());
	}

	private static RequiredArgumentBuilder<CommandSourceStack, Result> getItemArg(CommandBuildContext buildContext) {
		return argument("item", ItemFilterArgument.itemFilter(buildContext));
	}

	// @formatter:off
	private static LiteralArgumentBuilder<CommandSourceStack> enable(String name, boolean enable, boolean activate) {
		return literal(name)
			.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
			.then(
				getPlayerArg()
				.executes(ctx -> enable(ctx, EntityArgument.getPlayers(ctx, "player"), enable, activate))
			);
	}

	private static RequiredArgumentBuilder<CommandSourceStack, String> nameArg() {
		return argument("name", StringArgumentType.word())
				.suggests((ctx, builder) -> SharedSuggestionProvider.suggest(GBConfigs.SERVER.getLists(), builder));
	}

	private static ArgumentBuilder<CommandSourceStack, ?> listCommands(boolean whitelist, CommandBuildContext buildContext) {
		return nameArg()
			.then(literal("add")
				.then(
					getItemArg(buildContext)
					.executes(getListAddCommand(whitelist))
				))
			.then(literal("remove")
				.then(
					getItemArg(buildContext)
					.suggests(getSuggestions(whitelist))
					.executes(getListRemoveCommand(whitelist))
				)
			)
			.then(
				literal("clear")
				.executes(getListClearCommand(whitelist))
			);
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext) {
		dispatcher.register(
			literal("build").requires(Commands.hasPermission(Commands.LEVEL_OWNERS))
				.then(enable("enable", true, false))
				.then(enable("disable", false, false))
                .then(enable("enableAndActivate", true, true))
                .then(literal("whitelist")
						.then(listCommands(true, buildContext)))
				.then(literal("blacklist")
						.then(listCommands(false, buildContext)))
				.then(literal("set_list").then(getPlayerArg().then(nameArg()
						.executes(ctx -> {
							Collection<ServerPlayer> players = EntityArgument.getPlayers(ctx, "player");
							players.forEach(player -> GBPlayer.get(player).setList(StringArgumentType.getString(ctx, "name")));
							return players.size();
						}))))
		);
	}
	// @formatter:on
	
	private static int enable(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> players, boolean state, boolean activate) {
		CommandSourceStack src = ctx.getSource();

        for (ServerPlayer p : players) {
			GBPlayer buildPlayer = GBPlayer.get(p);
			buildPlayer.setEnabled(p, state);
            if (activate) {
				buildPlayer.setActive(p, true);
            }
        }

        src.sendSuccess(() -> Component.literal((state ? "Enabled" : "Disabled") + " " + GamemodeBuild.NAME + " for " + players.size() + " player(s)"), false);

		if (state && players.stream().anyMatch(player -> !GBServerState.isGloballyEnabled(player.level()))) {
			src.sendSuccess(() -> Component.literal("Warning: This will have no effect as " + GamemodeBuild.NAME + " is disabled in the dimension!").withStyle(ChatFormatting.YELLOW), false);
		}

		return players.size();
	}

	private static Command<CommandSourceStack> getListAddCommand(boolean whitelist) {
		return (CommandContext<CommandSourceStack> ctx) -> {
			String name = StringArgumentType.getString(ctx, "name");
			ItemFilterArgument.Result filter = ItemFilterArgument.getItemFilter(ctx, "item");
			String entry = filter.asString();

			if (whitelist) {
				GBConfigs.SERVER.addToWhitelist(name, entry, true);
			} else {
				GBConfigs.SERVER.addToBlacklist(name, entry, true);
			}
			PacketDistributor.sendToAllPlayers(new ListUpdateMessage(ListUpdateMessage.Operation.ADD, whitelist, name, Optional.of(entry)));
			ctx.getSource().sendSuccess(() -> Component.literal("Added '" + entry + "' to " + name + (whitelist ? " whitelist" : " blacklist")), false);

			return Command.SINGLE_SUCCESS;
		};
	}

	private static Command<CommandSourceStack> getListRemoveCommand(boolean whitelist) {
		return (CommandContext<CommandSourceStack> ctx) -> {
			String name = StringArgumentType.getString(ctx, "name");
			ItemFilterArgument.Result filter = ItemFilterArgument.getItemFilter(ctx, "item");
			String entry = filter.asString();

			boolean found = whitelist ? GBConfigs.SERVER.removeFromWhitelist(name, entry, true) : GBConfigs.SERVER.removeFromBlacklist(name, entry, true);
			if (!found) throw FILTER_DID_NOT_EXIST.create();

			PacketDistributor.sendToAllPlayers(new ListUpdateMessage(ListUpdateMessage.Operation.REMOVE, whitelist, name, Optional.of(entry)));
			ctx.getSource().sendSuccess(() -> Component.literal("Removed '" + entry + "' from " + name + (whitelist ? " whitelist" : " blacklist")), false);

			return Command.SINGLE_SUCCESS;
		};
	}

	private static Command<CommandSourceStack> getListClearCommand(boolean whitelist) {
		return (CommandContext<CommandSourceStack> ctx) -> {
			String name = StringArgumentType.getString(ctx, "name");
			int count = whitelist ? GBConfigs.SERVER.clearWhitelist(name, true) : GBConfigs.SERVER.clearBlacklist(name, true);

			PacketDistributor.sendToAllPlayers(new ListUpdateMessage(ListUpdateMessage.Operation.CLEAR, whitelist, name, null));
			ctx.getSource().sendSuccess(() -> Component.literal("Removed " + count + " " +  (whitelist ? " whitelist" : " blacklist") + " entries from " + name), false);

			return Command.SINGLE_SUCCESS;
		};
	}

	private static SuggestionProvider<CommandSourceStack> getSuggestions(boolean whitelist) {
		return (ctx, builder) -> {
			String name = StringArgumentType.getString(ctx, "name");
			return SharedSuggestionProvider.suggest(whitelist ? GBConfigs.SERVER.getWhitelistStream(name) : GBConfigs.SERVER.getBlacklistStream(name), builder);
		};
	}
}
