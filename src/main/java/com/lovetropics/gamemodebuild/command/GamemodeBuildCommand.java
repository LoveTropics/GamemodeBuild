package com.lovetropics.gamemodebuild.command;

import com.lovetropics.gamemodebuild.GBConfigs;
import com.lovetropics.gamemodebuild.GamemodeBuild;
import com.lovetropics.gamemodebuild.command.ItemFilterArgument.Result;
import com.lovetropics.gamemodebuild.message.ListUpdateMessage;
import com.lovetropics.gamemodebuild.state.GBPlayerStore;
import com.lovetropics.gamemodebuild.state.GBServerState;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.command.CommandException;
import net.minecraft.command.CommandSource;
import net.minecraft.command.ISuggestionProvider;
import net.minecraft.command.arguments.GameProfileArgument;
import net.minecraft.command.arguments.GameProfileArgument.IProfileProvider;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static net.minecraft.command.Commands.argument;
import static net.minecraft.command.Commands.literal;

public final class GamemodeBuildCommand {
	private static final SimpleCommandExceptionType FILTER_DID_NOT_EXIST = new SimpleCommandExceptionType(new StringTextComponent("That filter did not exist!"));
	
	private static RequiredArgumentBuilder<CommandSource, IProfileProvider> getPlayerArg() {
		return argument("player", GameProfileArgument.gameProfile());
	}

	private static RequiredArgumentBuilder<CommandSource, Result> getItemArg() {
		return argument("item", ItemFilterArgument.itemFilter());
	}

	// @formatter:off
	private static LiteralArgumentBuilder<CommandSource> getEnableLiteral(boolean enable) {
		return literal(enable ? "enable" : "disable")
			.requires(src -> src.hasPermissionLevel(4))
			.executes(ctx -> enable(ctx, null, enable))
			.then(
				getPlayerArg()
				.executes(ctx -> enable(ctx, GameProfileArgument.getGameProfiles(ctx, "player"), enable))
			);
	}

	private static LiteralArgumentBuilder<CommandSource> getListLiteral(String name) {
		return literal(name)
			.requires(src -> src.hasPermissionLevel(4))
			.then(
				literal("add")
				.then(
					getItemArg()
					.executes(getListAddCommand(name))
				)
			)
			.then(
				literal("remove")
				.then(
					getItemArg()
					.suggests(getSuggestions(name))
					.executes(getListRemoveCommand(name))
				)
			)
			.then(
				literal("clear")
				.executes(getListClearCommand(name))
			);
	}

	public static void register(CommandDispatcher<CommandSource> dispatcher) {
		dispatcher.register(
			literal("build")
				.then(getEnableLiteral(true))
				.then(getEnableLiteral(false))
				.then(getListLiteral("whitelist"))
				.then(getListLiteral("blacklist"))
		);
	}
	// @formatter:on
	
	private static int enable(CommandContext<CommandSource> ctx, @Nullable Collection<GameProfile> profiles, boolean state) {
		CommandSource src = ctx.getSource();
		MinecraftServer server = src.getServer();

		if (profiles == null) {
			if (state == GBServerState.isGloballyEnabled()) {
				throw new CommandException(new StringTextComponent(GamemodeBuild.NAME + " is already " + (state ? "enabled" : "disabled")));
			}

			GBServerState.setGloballyEnabled(server, state);
			src.sendFeedback(new StringTextComponent((state ? "Enabled" : "Disabled") + " " + GamemodeBuild.NAME + " globally"), false);
			return Command.SINGLE_SUCCESS;
		}
		
		List<ServerPlayerEntity> players = profiles.stream()
			   .map(g -> server.getPlayerList().getPlayerByUUID(g.getId()))
			   .filter(p -> GBPlayerStore.isEnabled(p) != state)
			   .collect(Collectors.toList());

		players.forEach(p -> GBServerState.setEnabledFor(p, state));

		src.sendFeedback(new StringTextComponent((state ? "Enabled" : "Disabled") + " " + GamemodeBuild.NAME + " for " + players.size() + " player(s)"), false);
		if (state && !GBServerState.isGloballyEnabled()) {
			src.sendFeedback(new StringTextComponent("Warning: This will have no effect as " + GamemodeBuild.NAME + " is currently globally disabled!").applyTextStyle(TextFormatting.YELLOW), false);
		}

		return players.size();
	}

	private static Command<CommandSource> getListAddCommand(String name) {
		return (CommandContext<CommandSource> ctx) -> {
			ItemFilterArgument.Result filter = ItemFilterArgument.getItemFilter(ctx, "item");
			String entry = filter.asString();

			GBConfigs.SERVER.addToList(name, entry, true);
			GamemodeBuild.NETWORK.send(PacketDistributor.ALL.noArg(), new ListUpdateMessage(ListUpdateMessage.Operation.ADD, name, entry));
			ctx.getSource().sendFeedback(new StringTextComponent("Added '" + entry + "' to " + name), false);

			return Command.SINGLE_SUCCESS;
		};
	}

	private static Command<CommandSource> getListRemoveCommand(String name) {
		return (CommandContext<CommandSource> ctx) -> {
			ItemFilterArgument.Result filter = ItemFilterArgument.getItemFilter(ctx, "item");
			String entry = filter.asString();

			if (!GBConfigs.SERVER.removeFromList(name, entry, true)) throw FILTER_DID_NOT_EXIST.create();

			GamemodeBuild.NETWORK.send(PacketDistributor.ALL.noArg(), new ListUpdateMessage(ListUpdateMessage.Operation.REMOVE, name, entry));
			ctx.getSource().sendFeedback(new StringTextComponent("Removed '" + entry + "' from " + name), false);

			return Command.SINGLE_SUCCESS;
		};
	}

	private static Command<CommandSource> getListClearCommand(String name) {
		return (CommandContext<CommandSource> ctx) -> {
			int count = GBConfigs.SERVER.clearList(name, true);

			GamemodeBuild.NETWORK.send(PacketDistributor.ALL.noArg(), new ListUpdateMessage(ListUpdateMessage.Operation.CLEAR, name, null));
			ctx.getSource().sendFeedback(new StringTextComponent("Removed " + count + " " + name + " entries"), false);

			return Command.SINGLE_SUCCESS;
		};
	}

	private static SuggestionProvider<CommandSource> getSuggestions(String name) {
		return (ctx, builder) -> ISuggestionProvider.suggest(GBConfigs.SERVER.getListStream(name), builder);
	}
}
