package com.lovetropics.survivalplus.command;

import static net.minecraft.command.Commands.argument;
import static net.minecraft.command.Commands.literal;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.lovetropics.survivalplus.SPConfigs;
import com.lovetropics.survivalplus.SPPlayerState;
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
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;

public final class SurvivalPlusCommand {
	private static final SimpleCommandExceptionType FILTER_DID_NOT_EXIST = new SimpleCommandExceptionType(new StringTextComponent("That filter did not exist!"));
	
	public static void register(CommandDispatcher<CommandSource> dispatcher) {
		// @formatter:off
		dispatcher.register(literal("survivalplus")
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
						.executes(SurvivalPlusCommand::addWhitelist)))
					.then(literal("remove")
						.then(argument("item", ItemFilterArgument.itemFilter())
						.suggests(whitelistSuggestions())
						.executes(SurvivalPlusCommand::removeWhitelist))
					.then(literal("clear")
						.executes(SurvivalPlusCommand::clearWhitelist)))
				)
				.then(literal("blacklist")
					.requires(src -> src.hasPermissionLevel(4))
					.then(literal("add")
						.then(argument("item", ItemFilterArgument.itemFilter())
						.executes(SurvivalPlusCommand::addBlacklist)))
						.then(literal("remove")
							.then(argument("item", ItemFilterArgument.itemFilter())
							.suggests(blacklistSuggestions())
							.executes(SurvivalPlusCommand::removeBlacklist))
						.then(literal("clear")
							.executes(SurvivalPlusCommand::clearBlacklist)))
				)
		);
		// @formatter:on
	}
	
	private static int enable(CommandContext<CommandSource> ctx, @Nullable Collection<GameProfile> profiles, boolean state) {
		if (profiles != null) {
			List<ServerPlayerEntity> players = profiles.stream()
				   .map(g -> ctx.getSource().getServer().getPlayerList().getPlayerByUUID(g.getId()))
				   .filter(p -> SPPlayerState.isSpecificallyEnabled(p) != state)
				   .collect(Collectors.toList());
			players.forEach(p -> SPPlayerState.setEnabled(p, state));
			ctx.getSource().sendFeedback(new StringTextComponent((state ? "Enabled" : "Disabled") + " SurvivalPlus for " + players.size() + " player(s)"), false);
			if (state && !SPConfigs.SERVER.enabled()) {
				ctx.getSource().sendFeedback(new StringTextComponent("Warning: This will have no effect as SurvivalPlus is currently globally disabled!").applyTextStyle(TextFormatting.YELLOW), false);
			}
			return players.size();
		} else {
			if (state == SPConfigs.SERVER.enabled()) {
				throw new CommandException(new StringTextComponent("SurvivalPlus is already " + (state ? "enabled" : "disabled")));
			}
			SPConfigs.SERVER.enable(state);
			ctx.getSource().sendFeedback(new StringTextComponent((state ? "Enabled" : "Disabled") + " SurvivalPlus globally"), false);
			return Command.SINGLE_SUCCESS;
		}
	}
	
	private static int addWhitelist(CommandContext<CommandSource> ctx) {
		ItemFilterArgument.Result filter = ItemFilterArgument.getItemFilter(ctx, "item");
		
		String entry = filter.asString();
		SPConfigs.SERVER.addWhitelist(entry);
		ctx.getSource().sendFeedback(new StringTextComponent("Added '" + entry + "' to whitelist"), false);
		
		return Command.SINGLE_SUCCESS;
	}
	
	private static int addBlacklist(CommandContext<CommandSource> ctx) {
		ItemFilterArgument.Result filter = ItemFilterArgument.getItemFilter(ctx, "item");
		
		String entry = filter.asString();
		SPConfigs.SERVER.addBlacklist(entry);
		ctx.getSource().sendFeedback(new StringTextComponent("Added '" + entry + "' to blacklist"), false);
		
		return Command.SINGLE_SUCCESS;
	}
	
	private static int removeWhitelist(CommandContext<CommandSource> ctx) throws CommandSyntaxException {
		ItemFilterArgument.Result filter = ItemFilterArgument.getItemFilter(ctx, "item");
		
		String entry = filter.asString();
		if (SPConfigs.SERVER.removeWhitelist(entry)) {
			ctx.getSource().sendFeedback(new StringTextComponent("Removed '" + entry + "' from whitelist"), false);
		} else {
			throw FILTER_DID_NOT_EXIST.create();
		}
		
		return Command.SINGLE_SUCCESS;
	}
	
	private static int removeBlacklist(CommandContext<CommandSource> ctx) throws CommandSyntaxException {
		ItemFilterArgument.Result filter = ItemFilterArgument.getItemFilter(ctx, "item");
		
		String entry = filter.asString();
		if (SPConfigs.SERVER.removeBlacklist(entry)) {
			ctx.getSource().sendFeedback(new StringTextComponent("Removed '" + entry + "' from blacklist"), false);
		} else {
			throw FILTER_DID_NOT_EXIST.create();
		}
		
		return Command.SINGLE_SUCCESS;
	}
	
	private static int clearWhitelist(CommandContext<CommandSource> ctx) {
		int count = SPConfigs.SERVER.modifyWhitelist(whitelist -> {
			int size = whitelist.size();
			whitelist.clear();
			return size;
		});
		
		ctx.getSource().sendFeedback(new StringTextComponent("Removed " + count + " whitelist entries"), false);
		
		return Command.SINGLE_SUCCESS;
	}
	
	private static int clearBlacklist(CommandContext<CommandSource> ctx) {
		int count = SPConfigs.SERVER.modifyBlacklist(blacklist -> {
			int size = blacklist.size();
			blacklist.clear();
			return size;
		});
		
		ctx.getSource().sendFeedback(new StringTextComponent("Removed " + count + " blacklist entries"), false);
		
		return Command.SINGLE_SUCCESS;
	}
	
	private static SuggestionProvider<CommandSource> whitelistSuggestions() {
		return (ctx, builder) -> {
			return ISuggestionProvider.suggest(SPConfigs.SERVER.whitelist(), builder);
		};
	}
	
	private static SuggestionProvider<CommandSource> blacklistSuggestions() {
		return (ctx, builder) -> {
			return ISuggestionProvider.suggest(SPConfigs.SERVER.blacklist(), builder);
		};
	}
}
