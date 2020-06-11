package com.lovetropics.gamemodebuild.command;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.command.arguments.ItemParser;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.StringTextComponent;

public class ItemFilterArgument implements ArgumentType<ItemFilterArgument.Result> {
	private static final Collection<String> EXAMPLES = Arrays.asList("stick", "minecraft:stick", "#stick");
	private static final SimpleCommandExceptionType INVALID_FILTER = new SimpleCommandExceptionType(new StringTextComponent("Invalid filter!"));
	
	public static ItemFilterArgument itemFilter() {
		return new ItemFilterArgument();
	}
	
	public static Result getItemFilter(CommandContext<CommandSource> context, String name) {
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
		
		return parser.fillSuggestions(builder);
	}
	
	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}
	
	public interface Result {
		String asString();
	}
	
	public static class ItemResult implements Result {
		private final Item item;
		
		ItemResult(Item item) {
			this.item = item;
		}
		
		@Override
		public String asString() {
			return this.item.getRegistryName().toString();
		}
	}
	
	public static class TagResult implements Result {
		private final ResourceLocation tagId;
		
		TagResult(ResourceLocation tagId) {
			this.tagId = tagId;
		}
		
		@Override
		public String asString() {
			return "#" + this.tagId;
		}
	}
	
	public static class WildcardResult implements Result {
		@Override
		public String asString() {
			return "*";
		}
	}
}
