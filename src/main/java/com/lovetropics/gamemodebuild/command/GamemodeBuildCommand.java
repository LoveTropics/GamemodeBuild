package com.lovetropics.gamemodebuild.command;

import static net.minecraft.command.Commands.argument;
import static net.minecraft.command.Commands.literal;

import java.util.Collection;

import javax.annotation.Nullable;

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

import net.minecraft.command.CommandException;
import net.minecraft.command.CommandSource;
import net.minecraft.command.ISuggestionProvider;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.command.arguments.EntitySelector;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.network.PacketDistributor;

public final class GamemodeBuildCommand {
	private static final SimpleCommandExceptionType FILTER_DID_NOT_EXIST = new SimpleCommandExceptionType(new StringTextComponent("That filter did not exist!"));
	
	private static RequiredArgumentBuilder<CommandSource, EntitySelector> getPlayerArg() {
		return argument("player", EntityArgument.players());
	}

	private static RequiredArgumentBuilder<CommandSource, Result> getItemArg() {
		return argument("item", ItemFilterArgument.itemFilter());
	}

	// @formatter:off
	private static LiteralArgumentBuilder<CommandSource> enable(boolean enable) {
		return literal(enable ? "enable" : "disable")
			.requires(src -> src.hasPermissionLevel(4))
			.executes(ctx -> enable(ctx, null, enable))
			.then(
				getPlayerArg()
				.executes(ctx -> enable(ctx, EntityArgument.getPlayers(ctx, "player"), enable))
			);
	}

	private static RequiredArgumentBuilder<CommandSource, String> nameArg() {
		return argument("name", StringArgumentType.word())
				.suggests((ctx, builder) -> ISuggestionProvider.suggest(GBConfigs.SERVER.getLists(), builder));
	}

	private static ArgumentBuilder<CommandSource, ?> listCommands(boolean whitelist) {
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

	public static void register(CommandDispatcher<CommandSource> dispatcher) {
		dispatcher.register(
			literal("build").requires(src -> src.hasPermissionLevel(4))
				.then(enable(true))
				.then(enable(false))
				.then(literal("whitelist")
						.then(listCommands(true)))
				.then(literal("blacklist")
						.then(listCommands(false)))
				.then(literal("set_list").then(getPlayerArg().then(nameArg()
						.executes(ctx -> {
							Collection<ServerPlayerEntity> players = EntityArgument.getPlayers(ctx, "player");
							players.forEach(player -> GBPlayerStore.setList(player, StringArgumentType.getString(ctx, "name")));
							return players.size();
						}))))
		);
	}
	// @formatter:on
	
	private static int enable(CommandContext<CommandSource> ctx, @Nullable Collection<ServerPlayerEntity> players, boolean state) {
		CommandSource src = ctx.getSource();
		MinecraftServer server = src.getServer();

		if (players == null) {
			if (state == GBServerState.isGloballyEnabled()) {
				throw new CommandException(new StringTextComponent(GamemodeBuild.NAME + " is already " + (state ? "enabled" : "disabled")));
			}

			GBServerState.setGloballyEnabled(server, state);
			src.sendFeedback(new StringTextComponent((state ? "Enabled" : "Disabled") + " " + GamemodeBuild.NAME + " globally"), false);
			return Command.SINGLE_SUCCESS;
		}

		players.forEach(p -> GBServerState.setEnabledFor(p, state));

		src.sendFeedback(new StringTextComponent((state ? "Enabled" : "Disabled") + " " + GamemodeBuild.NAME + " for " + players.size() + " player(s)"), false);
		if (state && !GBServerState.isGloballyEnabled()) {
			src.sendFeedback(new StringTextComponent("Warning: This will have no effect as " + GamemodeBuild.NAME + " is currently globally disabled!").applyTextStyle(TextFormatting.YELLOW), false);
		}

		return players.size();
	}

	private static Command<CommandSource> getListAddCommand(boolean whitelist) {
		return (CommandContext<CommandSource> ctx) -> {
			String name = StringArgumentType.getString(ctx, "name");
			ItemFilterArgument.Result filter = ItemFilterArgument.getItemFilter(ctx, "item");
			String entry = filter.asString();

			if (whitelist) {
				GBConfigs.SERVER.addToWhitelist(name, entry, true);
			} else {
				GBConfigs.SERVER.addToBlacklist(name, entry, true);
			}
			GBNetwork.CHANNEL.send(PacketDistributor.ALL.noArg(), new ListUpdateMessage(ListUpdateMessage.Operation.ADD, whitelist, name, entry));
			ctx.getSource().sendFeedback(new StringTextComponent("Added '" + entry + "' to " + name + (whitelist ? " whitelist" : " blacklist")), false);

			return Command.SINGLE_SUCCESS;
		};
	}

	private static Command<CommandSource> getListRemoveCommand(boolean whitelist) {
		return (CommandContext<CommandSource> ctx) -> {
			String name = StringArgumentType.getString(ctx, "name");
			ItemFilterArgument.Result filter = ItemFilterArgument.getItemFilter(ctx, "item");
			String entry = filter.asString();

			boolean found = whitelist ? GBConfigs.SERVER.removeFromWhitelist(name, entry, true) : GBConfigs.SERVER.removeFromBlacklist(name, entry, true);
			if (!found) throw FILTER_DID_NOT_EXIST.create();

			GBNetwork.CHANNEL.send(PacketDistributor.ALL.noArg(), new ListUpdateMessage(ListUpdateMessage.Operation.REMOVE, whitelist, name, entry));
			ctx.getSource().sendFeedback(new StringTextComponent("Removed '" + entry + "' from " + name + (whitelist ? " whitelist" : " blacklist")), false);

			return Command.SINGLE_SUCCESS;
		};
	}

	private static Command<CommandSource> getListClearCommand(boolean whitelist) {
		return (CommandContext<CommandSource> ctx) -> {
			String name = StringArgumentType.getString(ctx, "name");
			int count = whitelist ? GBConfigs.SERVER.clearWhitelist(name, true) : GBConfigs.SERVER.clearBlacklist(name, true);

			GBNetwork.CHANNEL.send(PacketDistributor.ALL.noArg(), new ListUpdateMessage(ListUpdateMessage.Operation.CLEAR, whitelist, name, null));
			ctx.getSource().sendFeedback(new StringTextComponent("Removed " + count + " " +  (whitelist ? " whitelist" : " blacklist") + " entries from " + name), false);

			return Command.SINGLE_SUCCESS;
		};
	}

	private static SuggestionProvider<CommandSource> getSuggestions(boolean whitelist) {
		return (ctx, builder) -> {
			String name = StringArgumentType.getString(ctx, "name");
			return ISuggestionProvider.suggest(whitelist ? GBConfigs.SERVER.getWhitelistStream(name) : GBConfigs.SERVER.getBlacklistStream(name), builder);
		};
	}
}
