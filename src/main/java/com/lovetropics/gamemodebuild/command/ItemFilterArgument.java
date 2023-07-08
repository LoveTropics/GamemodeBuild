package com.lovetropics.gamemodebuild.command;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class ItemFilterArgument implements ArgumentType<ItemFilterArgument.Result> {
	private static final Collection<String> EXAMPLES = Arrays.asList("stick", "minecraft:stick", "#stick");

	private final HolderLookup<Item> items;

	public ItemFilterArgument(CommandBuildContext buildContext) {
		items = buildContext.holderLookup(Registries.ITEM);
	}

	public static ItemFilterArgument itemFilter(CommandBuildContext buildContext) {
		return new ItemFilterArgument(buildContext);
	}
	
	public static Result getItemFilter(CommandContext<CommandSourceStack> context, String name) {
		return context.getArgument(name, Result.class);
	}
	
	@Override
	public ItemFilterArgument.Result parse(StringReader reader) throws CommandSyntaxException {
		if (reader.canRead(1) && reader.peek() == '*') {
			reader.skip();
			return new WildcardResult();
		}

		return ItemParser.parseForTesting(items, reader).map(
				item -> new ItemResult(item.item()),
				tag -> new TagResult(tag.tag())
		);
	}
	
	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> ctx, SuggestionsBuilder builder) {
		return ItemParser.fillSuggestions(items, builder, true);
	}
	
	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}
	
	public interface Result {
		String asString();
	}

	public record ItemResult(Holder<Item> item) implements Result {
		@Override
		public String asString() {
			return item.unwrapKey().map(key -> key.location().toString()).orElseThrow();
		}
	}

	public record TagResult(HolderSet<Item> set) implements Result {
		@Override
		public String asString() {
			return set.unwrapKey().map(key -> "#" + key.location()).orElseThrow();
		}
	}
	
	public static class WildcardResult implements Result {
		@Override
		public String asString() {
			return "*";
		}
	}
}
