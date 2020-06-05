package com.lovetropics.survivalplus.command;

import com.lovetropics.survivalplus.SPConfigs;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.CommandSource;
import net.minecraft.util.text.StringTextComponent;

import static net.minecraft.command.Commands.argument;
import static net.minecraft.command.Commands.literal;

public final class SurvivalPlusCommand {
	public static void register(CommandDispatcher<CommandSource> dispatcher) {
		// @formatter:off
		dispatcher.register(literal("survivalplus")
				.then(literal("whitelist")
					.requires(src -> src.hasPermissionLevel(4))
					.then(literal("add")
					.then(argument("item", ItemFilterArgument.itemFilter())
					.executes(SurvivalPlusCommand::addWhitelist))))
				.then(literal("blacklist")
					.requires(src -> src.hasPermissionLevel(4))
					.then(literal("add")
					.then(argument("item", ItemFilterArgument.itemFilter())
					.executes(SurvivalPlusCommand::addBlacklist))))
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
}
