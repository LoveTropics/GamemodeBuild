package com.lovetropics.gamemodebuild;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.Validate;

import com.google.common.base.Predicates;

import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.Tag;
import net.minecraft.util.LazyValue;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import net.minecraftforge.registries.ForgeRegistries;

public class ItemFilter {
	
	private static class LazyItemFilter implements Predicate<Item> {
		
		private final LazyValue<Item> item;
		
		LazyItemFilter(String itemName) {
			this.item = new LazyValue<>(() -> ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemName)));
		}
		
		@Override
		public boolean test(Item t) {
			Item i = item.getValue();
			return i != null && i == t;
		}
	}
	
	private static class LazyTagFilter implements Predicate<Item> {
		
		private final LazyValue<Tag<Item>> tag;
		
		LazyTagFilter(String tagName) {
			this.tag = new LazyValue<>(() -> ItemTags.getCollection().get(new ResourceLocation(tagName)));
		}
		
		@Override
		public boolean test(Item item) {
			Tag<Item> t = tag.getValue();
			return t != null && t.contains(item);
		}
	}
	
	public static ItemFilter fromStrings(List<String> whitelist, List<String> blacklist) {
		return new ItemFilter(parsePredicates(whitelist), parsePredicates(blacklist));
	}
	
	private static List<Predicate<Item>> parsePredicates(List<String> predicates) {
		if (predicates == null) return new ArrayList<>();

		return predicates.stream()
				.map(ItemFilter::parsePredicate)
				.collect(Collectors.toList());
	}
	
	private static Predicate<Item> parsePredicate(String predicate) {
		Validate.notNull(predicate);
		if ("*".equals(predicate)) {
			return Predicates.alwaysTrue();
		} else if (predicate.startsWith("#")) {
			return new LazyTagFilter(predicate.substring(1));
		} else {
			return new LazyItemFilter(predicate);
		}
	}
	
	private final List<Predicate<Item>> whitelistPredicates;
	private final List<Predicate<Item>> blacklistPredicates;
	
	private ItemFilter(List<Predicate<Item>> whitelist, List<Predicate<Item>> blacklist) {
		this.whitelistPredicates = new ArrayList<>(whitelist);
		this.blacklistPredicates = new ArrayList<>(blacklist);
	}
	
	public List<ItemStack> getAllStacks() {
		return getStacks(ItemGroup.SEARCH);
	}
	
	public List<ItemStack> getStacks(ItemGroup group) {
		if (whitelistPredicates.isEmpty()) {
			return Collections.emptyList();
		}

		NonNullList<ItemStack> ret = NonNullList.create();
		for (Item item : Registry.ITEM) {
			item.fillItemGroup(group, ret);
		}
		ret.removeIf(s -> whitelistPredicates.stream().noneMatch(p -> p.test(s.getItem())));
		ret.removeIf(s -> blacklistPredicates.stream().anyMatch(p -> p.test(s.getItem())));
		return ret;
	}
}
