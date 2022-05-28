package com.lovetropics.gamemodebuild.command;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class ItemFilterArgument implements ArgumentType<ItemFilterArgument.Result> {
	private static final Collection<String> EXAMPLES = Arrays.asList("stick", "minecraft:stick", "#stick");
	private static final SimpleCommandExceptionType INVALID_FILTER = new SimpleCommandExceptionType(new TextComponent("Invalid filter!"));
	
	public static ItemFilterArgument itemFilter() {
		return new ItemFilterArgument();
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
		
		ItemParser parser = new ItemParser(reader, true).parse();
		if (parser.getItem() != null) {
			return new ItemResult(parser.getItem());
		} else if (parser.getTag() != null) {
			return new TagResult(parser.getTag());
		}
		
		throw INVALID_FILTER.create();
	}
	
	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> ctx, SuggestionsBuilder builder) {
		StringReader reader = new StringReader(builder.getInput());
		reader.setCursor(builder.getStart());
		
		ItemParser parser = new ItemParser(reader, true);
		try {
			parser.parse();
		} catch (CommandSyntaxException var6) {
		}

		return parser.fillSuggestions(builder, Registry.ITEM);
	}
	
	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}
	
	public interface Result {
		String asString();
	}

	public record ItemResult(Item item) implements Result {
		@Override
		public String asString() {
			return this.item.getRegistryName().toString();
		}
	}

	public record TagResult(TagKey<?> tag) implements Result {
		@Override
		public String asString() {
			return "#" + this.tag.location();
		}
	}
	
	public static class WildcardResult implements Result {
		@Override
		public String asString() {
			return "*";
		}
	}
}
