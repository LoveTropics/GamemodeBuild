package com.lovetropics.gamemodebuild;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.config.ModConfig;

@EventBusSubscriber (modid = GamemodeBuild.MODID, bus = Bus.MOD)
public class GBConfigs {

	public static class ItemList {
		private final List<String> whitelist;
		private final List<String> blacklist;

		public ItemList(List<String> whitelist, List<String> blacklist) {
			this.whitelist = whitelist;
			this.blacklist = blacklist;
		}

		public ItemList() {
			this(new ArrayList<>(), new ArrayList<>());
		}

		public List<String> getWhitelist() {
			return whitelist;
		}

		public List<String> getBlacklist() {
			return blacklist;
		}
	}
	public static class Server {
		final Gson gson;
		final Type listsConfigType = new TypeToken<Map<String, ItemList>>() {}.getType();
		final String defaultList = "default";

		Map<String, ItemList> lists = new HashMap<>();
		final ConfigValue<String> listsConfig;

		final BooleanValue enabled;

		final Map<String, ItemFilter> filters = new HashMap<>();;

		Server(ForgeConfigSpec.Builder builder) {
			gson = new GsonBuilder().create();
			
			lists.put("default", new ItemList());
			listsConfig = builder.define("lists", gson.toJson(lists), o -> {
				if (o instanceof String) {
					try {
						gson.fromJson((String) o, listsConfigType);
						return true;
					} catch (Exception e) {}
				}
				return false;
			});

			enabled = builder.comment("Enable SurvivalPlus for all players").define("enabled", true);
		}

		void loadLists() {
			String configValue = listsConfig.get();
			lists = gson.fromJson(configValue, listsConfigType);
			if (lists == null) lists = new HashMap<>();
		}

		private void saveLists() {
			listsConfig.set(gson.toJson(lists));
			listsConfig.save();
		}

		public Collection<String> getLists() {
			return lists.keySet();
		}

		public <T> T modifyList(String name, Function<ItemList, T> function, boolean save) {
			ItemList list = lists.get(name);
			if (list == null) {
				list = new ItemList();
				lists.put(name, list);
			}

			T result = function.apply(list);

			if (save) saveLists();
			this.resetFilter();

			return result;
		}

		private void addToList(Function<ItemList, List<String>> getter, String name, String entry, boolean save) {
			modifyList(name, list -> {
				if (getter.apply(list).contains(entry)) return false;
				return getter.apply(list).add(entry);
			}, save);
		}

		public void addToWhitelist(String name, String entry, boolean save) {
			addToList(ItemList::getWhitelist, name, entry, save);
		}

		public void addToBlacklist(String name, String entry, boolean save) {
			addToList(ItemList::getBlacklist, name, entry, save);
		}

		public boolean removeFromWhitelist(String name, String entry, boolean save) {
			return this.modifyList(name, list -> list.getWhitelist().remove(entry), save);
		}

		public boolean removeFromBlacklist(String name, String entry, boolean save) {
			return this.modifyList(name, list -> list.getBlacklist().remove(entry), save);
		}
		
		public int clearWhitelist(String name, boolean save) {
			return this.modifyList(name, list -> {
				int size = list.getWhitelist().size();
				list.getWhitelist().clear();
				return size;
			}, save);
		}

		public int clearBlacklist(String name, boolean save) {
			return this.modifyList(name, list -> {
				int size = list.getBlacklist().size();
				list.getBlacklist().clear();
				return size;
			}, save);
		}

		public Stream<String> getWhitelistStream(String name) {
			List<String> list = lists.get(name).getWhitelist();
			if (list == null) list = new ArrayList<>();

			return list.stream();
		}

		public Stream<String> getBlacklistStream(String name) {
			List<String> list = lists.get(name).getBlacklist();
			if (list == null) list = new ArrayList<>();

			return list.stream();
		}

		public ItemFilter getFilter(String name) {
			return filters.computeIfAbsent(name, k -> {
				ItemList list = lists.getOrDefault(k, new ItemList());
				return ItemFilter.fromStrings(list.getWhitelist(), list.getBlacklist());
			});
		}

		void resetFilter() {
			filters.clear();
		}

		public void enable(boolean state) {
			this.enabled.set(state);
			this.enabled.save();
		}

		public boolean enabled() {
			return enabled.get();
		}
	}

	static final        ForgeConfigSpec serverSpec;
	public static final Server          SERVER;

	static {
		final Pair<Server, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(Server::new);
		serverSpec = specPair.getRight();
		SERVER = specPair.getLeft();

		SERVER.loadLists();
	}

	@SubscribeEvent
	public static void onLoad(final ModConfig.Loading configEvent) {
		SERVER.loadLists();
		SERVER.resetFilter();
	}

	@SubscribeEvent
	public static void onReload(final ModConfig.Reloading configEvent) {
		SERVER.loadLists();
		SERVER.resetFilter();
	}
}
