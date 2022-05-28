package com.lovetropics.gamemodebuild.command;

import com.lovetropics.gamemodebuild.GBConfigs;
import com.lovetropics.gamemodebuild.GamemodeBuild;
import com.lovetropics.gamemodebuild.command.ItemFilterArgument.Result;
import com.lovetropics.gamemodebuild.message.GBNetwork;
import com.lovetropics.gamemodebuild.message.ListUpdateMessage;
import com.lovetropics.gamemodebuild.state.GBPlayerStore;
import com.lovetropics.gamemodebuild.state.GBServerState;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandRuntimeException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.Collection;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public final class GamemodeBuildCommand {
	private static final SimpleCommandExceptionType FILTER_DID_NOT_EXIST = new SimpleCommandExceptionType(new TextComponent("That filter did not exist!"));
	
	private static RequiredArgumentBuilder<CommandSourceStack, EntitySelector> getPlayerArg() {
		return argument("player", EntityArgument.players());
	}

	private static RequiredArgumentBuilder<CommandSourceStack, Result> getItemArg() {
		return argument("item", ItemFilterArgument.itemFilter());
	}

	// @formatter:off
	private static LiteralArgumentBuilder<CommandSourceStack> enable(boolean enable) {
		return literal(enable ? "enable" : "disable")
			.requires(src -> src.hasPermission(4))
			.executes(ctx -> enable(ctx, null, enable))
			.then(
				getPlayerArg()
				.executes(ctx -> enable(ctx, EntityArgument.getPlayers(ctx, "player"), enable))
			);
	}

	private static RequiredArgumentBuilder<CommandSourceStack, String> nameArg() {
		return argument("name", StringArgumentType.word())
				.suggests((ctx, builder) -> SharedSuggestionProvider.suggest(GBConfigs.SERVER.getLists(), builder));
	}

	private static ArgumentBuilder<CommandSourceStack, ?> listCommands(boolean whitelist) {
		return nameArg()
			.then(literal("add")
				.then(
					getItemArg()
					.executes(getListAddCommand(whitelist))
				))
			.then(literal("remove")
				.then(
					getItemArg()
					.suggests(getSuggestions(whitelist))
					.executes(getListRemoveCommand(whitelist))
				)
			)
			.then(
				literal("clear")
				.executes(getListClearCommand(whitelist))
			);
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(
			literal("build").requires(src -> src.hasPermission(4))
				.then(enable(true))
				.then(enable(false))
				.then(literal("whitelist")
						.then(listCommands(true)))
				.then(literal("blacklist")
						.then(listCommands(false)))
				.then(literal("set_list").then(getPlayerArg().then(nameArg()
						.executes(ctx -> {
							Collection<ServerPlayer> players = EntityArgument.getPlayers(ctx, "player");
							players.forEach(player -> GBPlayerStore.setList(player, StringArgumentType.getString(ctx, "name")));
							return players.size();
						}))))
		);
	}
	// @formatter:on
	
	private static int enable(CommandContext<CommandSourceStack> ctx, @Nullable Collection<ServerPlayer> players, boolean state) {
		CommandSourceStack src = ctx.getSource();
		MinecraftServer server = src.getServer();

		if (players == null) {
			if (state == GBServerState.isGloballyEnabled()) {
				throw new CommandRuntimeException(new TextComponent(GamemodeBuild.NAME + " is already " + (state ? "enabled" : "disabled")));
			}

			GBServerState.setGloballyEnabled(server, state);
			src.sendSuccess(new TextComponent((state ? "Enabled" : "Disabled") + " " + GamemodeBuild.NAME + " globally"), false);
			return Command.SINGLE_SUCCESS;
		}

		players.forEach(p -> GBServerState.setEnabledFor(p, state));

		src.sendSuccess(new TextComponent((state ? "Enabled" : "Disabled") + " " + GamemodeBuild.NAME + " for " + players.size() + " player(s)"), false);
		if (state && !GBServerState.isGloballyEnabled()) {
			src.sendSuccess(new TextComponent("Warning: This will have no effect as " + GamemodeBuild.NAME + " is currently globally disabled!").withStyle(ChatFormatting.YELLOW), false);
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
			GBNetwork.CHANNEL.send(PacketDistributor.ALL.noArg(), new ListUpdateMessage(ListUpdateMessage.Operation.ADD, whitelist, name, entry));
			ctx.getSource().sendSuccess(new TextComponent("Added '" + entry + "' to " + name + (whitelist ? " whitelist" : " blacklist")), false);

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

			GBNetwork.CHANNEL.send(PacketDistributor.ALL.noArg(), new ListUpdateMessage(ListUpdateMessage.Operation.REMOVE, whitelist, name, entry));
			ctx.getSource().sendSuccess(new TextComponent("Removed '" + entry + "' from " + name + (whitelist ? " whitelist" : " blacklist")), false);

			return Command.SINGLE_SUCCESS;
		};
	}

	private static Command<CommandSourceStack> getListClearCommand(boolean whitelist) {
		return (CommandContext<CommandSourceStack> ctx) -> {
			String name = StringArgumentType.getString(ctx, "name");
			int count = whitelist ? GBConfigs.SERVER.clearWhitelist(name, true) : GBConfigs.SERVER.clearBlacklist(name, true);

			GBNetwork.CHANNEL.send(PacketDistributor.ALL.noArg(), new ListUpdateMessage(ListUpdateMessage.Operation.CLEAR, whitelist, name, null));
			ctx.getSource().sendSuccess(new TextComponent("Removed " + count + " " +  (whitelist ? " whitelist" : " blacklist") + " entries from " + name), false);

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
