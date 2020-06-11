package com.lovetropics.gamemodebuild.command;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

import com.lovetropics.gamemodebuild.GBConfigs;
import com.lovetropics.gamemodebuild.GamemodeBuild;
import com.lovetropics.gamemodebuild.message.ListUpdateMessage;
import com.lovetropics.gamemodebuild.state.GBPlayerStore;
import com.lovetropics.gamemodebuild.state.GBServerState;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.command.CommandException;
import net.minecraft.command.CommandSource;
import net.minecraft.command.ISuggestionProvider;
import net.minecraft.command.arguments.GameProfileArgument;
import net.minecraft.entity.ai.brain.task.UpdateActivityTask;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.network.PacketDistributor;

import static net.minecraft.command.Commands.argument;
import static net.minecraft.command.Commands.literal;

public final class GamemodeBuildCommand {
	private static final SimpleCommandExceptionType FILTER_DID_NOT_EXIST = new SimpleCommandExceptionType(new StringTextComponent("That filter did not exist!"));
	
	public static void register(CommandDispatcher<CommandSource> dispatcher) {
		// @formatter:off
		dispatcher.register(literal("build")
				.then(literal("disable")
					.requires(src -> src.hasPermissionLevel(4))
					.executes(ctx -> enable(ctx, null, false))
					.then(argument("player", GameProfileArgument.gameProfile())
						.executes(ctx -> enable(ctx, GameProfileArgument.getGameProfiles(ctx, "player"), false))))
				.then(literal("enable")
						.requires(src -> src.hasPermissionLevel(4))
						.executes(ctx -> enable(ctx, null, true))
						.then(argument("player", GameProfileArgument.gameProfile())
							.executes(ctx -> enable(ctx, GameProfileArgument.getGameProfiles(ctx, "player"), true))))
				.then(literal("whitelist")
					.requires(src -> src.hasPermissionLevel(4))
					.then(literal("add")
						.then(argument("item", ItemFilterArgument.itemFilter())
						.executes(GamemodeBuildCommand::addWhitelist)))
					.then(literal("remove")
						.then(argument("item", ItemFilterArgument.itemFilter())
						.suggests(whitelistSuggestions())
						.executes(GamemodeBuildCommand::removeWhitelist))
					.then(literal("clear")
						.executes(GamemodeBuildCommand::clearWhitelist)))
				)
				.then(literal("blacklist")
					.requires(src -> src.hasPermissionLevel(4))
					.then(literal("add")
						.then(argument("item", ItemFilterArgument.itemFilter())
						.executes(GamemodeBuildCommand::addBlacklist)))
						.then(literal("remove")
							.then(argument("item", ItemFilterArgument.itemFilter())
							.suggests(blacklistSuggestions())
							.executes(GamemodeBuildCommand::removeBlacklist))
						.then(literal("clear")
							.executes(GamemodeBuildCommand::clearBlacklist)))
				)
		);
		// @formatter:on
	}
	
	private static int enable(CommandContext<CommandSource> ctx, @Nullable Collection<GameProfile> profiles, boolean state) {
		CommandSource src = ctx.getSource();
		MinecraftServer server = src.getServer();
		
		if (profiles != null) {
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
		} else {
			if (state == GBServerState.isGloballyEnabled()) {
				throw new CommandException(new StringTextComponent(GamemodeBuild.NAME + " is already " + (state ? "enabled" : "disabled")));
			}
			GBServerState.setGloballyEnabled(server, state);
			src.sendFeedback(new StringTextComponent((state ? "Enabled" : "Disabled") + " " + GamemodeBuild.NAME + " globally"), false);
			return Command.SINGLE_SUCCESS;
		}
	}
	
	private static int addWhitelist(CommandContext<CommandSource> ctx) {
		ItemFilterArgument.Result filter = ItemFilterArgument.getItemFilter(ctx, "item");
		
		String entry = filter.asString();
		GBConfigs.SERVER.addWhitelist(entry, true);
		GamemodeBuild.NETWORK.send(PacketDistributor.ALL.noArg(), new ListUpdateMessage(true, true, entry));
		ctx.getSource().sendFeedback(new StringTextComponent("Added '" + entry + "' to whitelist"), false);
		
		return Command.SINGLE_SUCCESS;
	}
	
	private static int addBlacklist(CommandContext<CommandSource> ctx) {
		ItemFilterArgument.Result filter = ItemFilterArgument.getItemFilter(ctx, "item");
		
		String entry = filter.asString();
		GBConfigs.SERVER.addBlacklist(entry, true);
		GamemodeBuild.NETWORK.send(PacketDistributor.ALL.noArg(), new ListUpdateMessage(false, true, entry));
		ctx.getSource().sendFeedback(new StringTextComponent("Added '" + entry + "' to blacklist"), false);
		
		return Command.SINGLE_SUCCESS;
	}
	
	private static int removeWhitelist(CommandContext<CommandSource> ctx) throws CommandSyntaxException {
		ItemFilterArgument.Result filter = ItemFilterArgument.getItemFilter(ctx, "item");
		
		String entry = filter.asString();
		if (GBConfigs.SERVER.removeWhitelist(entry, true)) {
			GamemodeBuild.NETWORK.send(PacketDistributor.ALL.noArg(), new ListUpdateMessage(true, false, entry));
			ctx.getSource().sendFeedback(new StringTextComponent("Removed '" + entry + "' from whitelist"), false);
		} else {
			throw FILTER_DID_NOT_EXIST.create();
		}
		
		return Command.SINGLE_SUCCESS;
	}
	
	private static int removeBlacklist(CommandContext<CommandSource> ctx) throws CommandSyntaxException {
		ItemFilterArgument.Result filter = ItemFilterArgument.getItemFilter(ctx, "item");
		
		String entry = filter.asString();
		if (GBConfigs.SERVER.removeBlacklist(entry, true)) {
			GamemodeBuild.NETWORK.send(PacketDistributor.ALL.noArg(), new ListUpdateMessage(false, false, entry));
			ctx.getSource().sendFeedback(new StringTextComponent("Removed '" + entry + "' from blacklist"), false);
		} else {
			throw FILTER_DID_NOT_EXIST.create();
		}
		
		return Command.SINGLE_SUCCESS;
	}
	
	private static int clearWhitelist(CommandContext<CommandSource> ctx) {
		int count = GBConfigs.SERVER.clearWhitelist(true);
		GamemodeBuild.NETWORK.send(PacketDistributor.ALL.noArg(), new ListUpdateMessage(true, false, null));
		
		ctx.getSource().sendFeedback(new StringTextComponent("Removed " + count + " whitelist entries"), false);
		
		return Command.SINGLE_SUCCESS;
	}
	
	private static int clearBlacklist(CommandContext<CommandSource> ctx) {
		int count = GBConfigs.SERVER.clearBlacklist(true);
		GamemodeBuild.NETWORK.send(PacketDistributor.ALL.noArg(), new ListUpdateMessage(false, false, null));
		
		ctx.getSource().sendFeedback(new StringTextComponent("Removed " + count + " blacklist entries"), false);
		
		return Command.SINGLE_SUCCESS;
	}
	
	private static SuggestionProvider<CommandSource> whitelistSuggestions() {
		return (ctx, builder) -> {
			return ISuggestionProvider.suggest(GBConfigs.SERVER.whitelist(), builder);
		};
	}
	
	private static SuggestionProvider<CommandSource> blacklistSuggestions() {
		return (ctx, builder) -> {
			return ISuggestionProvider.suggest(GBConfigs.SERVER.blacklist(), builder);
		};
	}
}
