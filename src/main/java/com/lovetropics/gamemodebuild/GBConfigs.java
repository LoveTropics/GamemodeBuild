package com.lovetropics.gamemodebuild;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.commons.lang3.tuple.Pair;

import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

@EventBusSubscriber (modid = GamemodeBuild.MODID, bus = Bus.MOD)
public class GBConfigs {
	public static class Server {
		final Gson gson;
		final Type listsConfigType = new TypeToken<Map<String, List<String>>>() {}.getType();

		Map<String, List<String>> lists;
		final ConfigValue<String> listsConfig;

		final BooleanValue enabled;

		ItemFilter filter;

		Server(ForgeConfigSpec.Builder builder) {
			gson = new Gson();
			listsConfig = builder.define("lists", "", o -> true);

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

		public <T> T modifyList(String name, Function<List<String>, T> function, boolean save) {
			List<String> list = lists.get(name);
			if (list == null) list = new ArrayList<>();

			T result = function.apply(list);
			lists.put(name, list);

			if (save) saveLists();
			this.resetFilter();

			return result;
		}

		public void addToList(String name, String entry, boolean save) {
			modifyList(name, list -> {
				if (list.contains(entry)) return true;
				return list.add(entry);
			}, save);
		}

		public boolean removeFromList(String name, String entry, boolean save) {
			return this.modifyList(name, list -> list.remove(entry), save);
		}

		public int clearList(String name, boolean save) {
			return this.modifyList(name, list -> {
				int size = list.size();
				list.clear();
				return size;
			}, save);
		}

		public Stream<String> getListStream(String name) {
			List<String> list = lists.get(name);
			if (list == null) list = new ArrayList<>();

			return list.stream();
		}

		public ItemFilter getFilter() {
			if (filter == null) {
				filter = ItemFilter.fromStrings(lists.get("whitelist"), lists.get("blacklist"));
			}
			return filter;
		}

		void resetFilter() {
			filter = null;
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
