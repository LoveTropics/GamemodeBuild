package com.lovetropics.gamemodebuild;

import com.lovetropics.gamemodebuild.mixin.CreativeModeTabAccessor;
import net.minecraft.Util;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.SingleKeyCache;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.*;
import net.minecraftforge.common.util.MutableHashedLinkedMap;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.fml.ModLoader;
import org.apache.commons.lang3.Validate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ItemFilter {
	
	private record ItemTypeFilter(ResourceKey<Item> item) implements Predicate<Item> {
		@Override
		public boolean test(Item item) {
			return item.builtInRegistryHolder().is(this.item);
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
			return new TagFilter(TagKey.create(Registries.ITEM, tagLocation));
		} else {
			final ResourceLocation location = new ResourceLocation(predicate);
			return new ItemTypeFilter(ResourceKey.create(Registries.ITEM, location));
		}
	}
	
	private final List<Predicate<Item>> whitelistPredicates;
	private final List<Predicate<Item>> blacklistPredicates;
	private final SingleKeyCache<Key, List<ItemStack>> cache;

	private ItemFilter(List<Predicate<Item>> whitelist, List<Predicate<Item>> blacklist) {
		this.whitelistPredicates = new ArrayList<>(whitelist);
		this.blacklistPredicates = new ArrayList<>(blacklist);
		cache = Util.singleKeyCache(key -> computeStacks(key.featureFlags(), key.registryAccess()));
	}
	
	public List<ItemStack> getAllStacks(FeatureFlagSet enabledFeatures, RegistryAccess registryAccess) {
		return cache.getValue(new Key(enabledFeatures, registryAccess));
	}

	private List<ItemStack> computeStacks(FeatureFlagSet enabledFeatures, HolderLookup.Provider registryAccess) {
		if (whitelistPredicates.isEmpty()) {
			return List.of();
		}

		Set<ItemStack> items = ItemStackLinkedSet.createTypeAndTagSet();
		CreativeModeTab.Output output = createFilteredOutput(items);

		CreativeModeTab.ItemDisplayParameters parameters = new CreativeModeTab.ItemDisplayParameters(enabledFeatures, true, registryAccess);
		for (Map.Entry<ResourceKey<CreativeModeTab>, CreativeModeTab> entry : BuiltInRegistries.CREATIVE_MODE_TAB.entrySet()) {
			CreativeModeTab tab = entry.getValue();
            if (tab.getType() != CreativeModeTab.Type.SEARCH) {
                generateItems(entry.getKey(), tab, parameters, output);
            }
        }

		return List.copyOf(items);
	}

	// We have to invoke this logic ourselves, as Forge hooks the Vanilla path in such a way that it classloads client-side code
	private static void generateItems(ResourceKey<CreativeModeTab> tabKey, CreativeModeTab tab, CreativeModeTab.ItemDisplayParameters parameters, CreativeModeTab.Output output) {
		CreativeModeTab.DisplayItemsGenerator originalGenerator = ((CreativeModeTabAccessor) tab).getDisplayItemsGenerator();

		MutableHashedLinkedMap<ItemStack, CreativeModeTab.TabVisibility> entries = new MutableHashedLinkedMap<>(ItemStackLinkedSet.TYPE_AND_TAG, (key, left, right) -> CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
		originalGenerator.accept(parameters, entries::put);
		ModLoader.get().postEvent(new BuildCreativeModeTabContentsEvent(tab, tabKey, parameters, entries));

		entries.forEach(e -> output.accept(e.getKey()));
	}

	private CreativeModeTab.Output createFilteredOutput(final Set<ItemStack> items) {
		final Predicate<ItemStack> whitelist = stack -> {
			for (final Predicate<Item> predicate : whitelistPredicates) {
				if (predicate.test(stack.getItem())) {
					return true;
				}
			}
			return false;
		};

		final Predicate<ItemStack> blacklist;
		if (!blacklistPredicates.isEmpty()) {
			blacklist = stack -> {
				for (final Predicate<Item> predicate : blacklistPredicates) {
					if (predicate.test(stack.getItem())) {
						return false;
					}
				}
				return true;
			};
		} else {
			blacklist = stack -> true;
		}

        return (stack, visibility) -> {
			if (whitelist.test(stack) && blacklist.test(stack)) {
				items.add(stack);
			}
		};
	}

	private record Key(FeatureFlagSet featureFlags, HolderLookup.Provider registryAccess) {
	}
}
