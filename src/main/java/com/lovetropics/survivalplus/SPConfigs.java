package com.lovetropics.survivalplus;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.base.Predicates;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.config.ModConfig;

@EventBusSubscriber(modid = SurvivalPlus.MODID, bus = Bus.MOD)
public class SPConfigs {

	public static class Server {

		final ConfigValue<List<? extends String>> whitelist;
		final ConfigValue<List<? extends String>> blacklist;

		ItemFilter filter;

		Server(ForgeConfigSpec.Builder builder) {
			whitelist = builder.defineList("whitelist", new ArrayList<>(), Predicates.alwaysTrue());
			blacklist = builder.defineList("blacklist", new ArrayList<>(), Predicates.alwaysTrue());
		}
		
		public void addWhitelist(String entry) {
			@SuppressWarnings("unchecked")
			List<String> whitelist = (List<String>) this.whitelist.get();
			whitelist.add(entry);
			
			this.whitelist.set(whitelist);
			this.whitelist.save();
			
			this.resetFilter();
		}
		
		public void addBlacklist(String entry) {
			@SuppressWarnings("unchecked")
			List<String> blacklist = (List<String>) this.blacklist.get();
			blacklist.add(entry);
			
			this.blacklist.set(blacklist);
			this.blacklist.save();
			
			this.resetFilter();
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
