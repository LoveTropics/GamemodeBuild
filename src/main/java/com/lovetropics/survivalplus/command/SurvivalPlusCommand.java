package com.lovetropics.survivalplus.command;

import com.lovetropics.survivalplus.SPConfigs;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.command.CommandSource;
import net.minecraft.command.ISuggestionProvider;
import net.minecraft.util.text.StringTextComponent;

import static net.minecraft.command.Commands.argument;
import static net.minecraft.command.Commands.literal;

public final class SurvivalPlusCommand {
	private static final SimpleCommandExceptionType FILTER_DID_NOT_EXIST = new SimpleCommandExceptionType(new StringTextComponent("That filter did not exist!"));
	
	public static void register(CommandDispatcher<CommandSource> dispatcher) {
		// @formatter:off
		dispatcher.register(literal("survivalplus")
				.then(literal("whitelist")
					.requires(src -> src.hasPermissionLevel(4))
					.then(literal("add")
						.then(argument("item", ItemFilterArgument.itemFilter())
						.executes(SurvivalPlusCommand::addWhitelist)))
					.then(literal("remove")
						.then(argument("item", ItemFilterArgument.itemFilter())
						.suggests(whitelistSuggestions())
						.executes(SurvivalPlusCommand::removeWhitelist)))
				)
				.then(literal("blacklist")
					.requires(src -> src.hasPermissionLevel(4))
					.then(literal("add")
						.then(argument("item", ItemFilterArgument.itemFilter())
						.executes(SurvivalPlusCommand::addBlacklist)))
					.then(literal("remove")
						.then(argument("item", ItemFilterArgument.itemFilter())
						.suggests(blacklistSuggestions())
						.executes(SurvivalPlusCommand::removeBlacklist)))
				)
		);
		// @formatter:on
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
