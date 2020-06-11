package com.lovetropics.gamemodebuild;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.base.Predicates;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.config.ModConfig;

@EventBusSubscriber(modid = GamemodeBuild.MODID, bus = Bus.MOD)
public class GBConfigs {
	
	public static class Server {
		
		final ConfigValue<List<? extends String>> whitelist;
		final ConfigValue<List<? extends String>> blacklist;
		final BooleanValue enabled;
		
		ItemFilter filter;
		
		Server(ForgeConfigSpec.Builder builder) {
			whitelist = builder.defineList("whitelist", new ArrayList<>(), Predicates.alwaysTrue());
			blacklist = builder.defineList("blacklist", new ArrayList<>(), Predicates.alwaysTrue());
			enabled = builder.comment("Enable SurvivalPlus for all players").define("enabled", true);
		}
		
		public void addWhitelist(String entry, boolean save) {
			this.modifyWhitelist(whitelist -> whitelist.add(entry), save);
		}
		
		public void addBlacklist(String entry, boolean save) {
			this.modifyBlacklist(blacklist -> blacklist.add(entry), save);
		}
		
		public boolean removeWhitelist(String entry, boolean save) {
			return this.modifyWhitelist(whitelist -> whitelist.remove(entry), save);
		}
		
		public boolean removeBlacklist(String entry, boolean save) {
			return this.modifyBlacklist(blacklist -> blacklist.remove(entry), save);
		}
		
		public int clearWhitelist(boolean save) {
			return GBConfigs.SERVER.modifyWhitelist(whitelist -> {
				int size = whitelist.size();
				whitelist.clear();
				return size;
			}, save);
		}
		
		public int clearBlacklist(boolean save) {
			return GBConfigs.SERVER.modifyBlacklist(blacklist -> {
				int size = blacklist.size();
				blacklist.clear();
				return size;
			}, save);
		}
		
		@SuppressWarnings("unchecked")
		public Stream<String> whitelist() {
			return ((List<String>) this.whitelist.get()).stream();
		}
		
		@SuppressWarnings("unchecked")
		public Stream<String> blacklist() {
			return ((List<String>) this.blacklist.get()).stream();
		}
		
		public <T> T modifyWhitelist(Function<List<String>, T> function, boolean save) {
			@SuppressWarnings("unchecked")
			List<String> whitelist = (List<String>) this.whitelist.get();
			T result = function.apply(whitelist);
			
			this.whitelist.set(whitelist);
			if (save) {
				this.whitelist.save();
			}
			this.resetFilter();
			
			return result;
		}
		
		public <T> T modifyBlacklist(Function<List<String>, T> function, boolean save) {
			@SuppressWarnings("unchecked")
			List<String> blacklist = (List<String>) this.blacklist.get();
			T result = function.apply(blacklist);
			
			this.blacklist.set(blacklist);
			if (save) {
				this.blacklist.save();
			}
			this.resetFilter();
			
			return result;
		}
		
		public ItemFilter getFilter() {
			if (filter == null) {
				filter = ItemFilter.fromStrings(whitelist.get(), blacklist.get());
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
	
	static final ForgeConfigSpec serverSpec;
	public static final Server SERVER;
	
	static {
		final Pair<Server, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(Server::new);
		serverSpec = specPair.getRight();
		SERVER = specPair.getLeft();
	}
	
	@SubscribeEvent
	public static void onLoad(final ModConfig.Loading configEvent) {
		SERVER.resetFilter();
	}
	
	@SubscribeEvent
	public static void onReload(final ModConfig.Reloading configEvent) {
		SERVER.resetFilter();
	}
}
