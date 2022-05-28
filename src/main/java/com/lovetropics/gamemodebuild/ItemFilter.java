package com.lovetropics.gamemodebuild;

import com.google.common.base.Suppliers;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.commons.lang3.Validate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ItemFilter {
	
	private static class LazyItemFilter implements Predicate<Item> {
		private final Supplier<Item> item;
		
		LazyItemFilter(String itemName) {
			this.item = Suppliers.memoize(() -> ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemName)));
		}
		
		@Override
		public boolean test(Item t) {
			Item i = item.get();
			return i != null && i == t;
		}
	}

	private record TagFilter(TagKey<Item> tag) implements Predicate<Item> {
		@Override
		public boolean test(Item item) {
			return item.builtInRegistryHolder().is(tag);
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
			return item -> true;
		} else if (predicate.startsWith("#")) {
			final ResourceLocation tagLocation = new ResourceLocation(predicate.substring(1));
			return new TagFilter(TagKey.create(Registry.ITEM_REGISTRY, tagLocation));
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
		return getStacks(CreativeModeTab.TAB_SEARCH);
	}
	
	public List<ItemStack> getStacks(CreativeModeTab group) {
		if (whitelistPredicates.isEmpty()) {
			return Collections.emptyList();
		}

		NonNullList<ItemStack> ret = NonNullList.create();
		for (Item item : Registry.ITEM) {
			item.fillItemCategory(group, ret);
		}
		ret.removeIf(s -> whitelistPredicates.stream().noneMatch(p -> p.test(s.getItem())));
		ret.removeIf(s -> blacklistPredicates.stream().anyMatch(p -> p.test(s.getItem())));
		return ret;
	}
}
