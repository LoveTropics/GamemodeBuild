package com.lovetropics.gamemodebuild;

import java.util.regex.Pattern;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.lovetropics.gamemodebuild.command.GamemodeBuildCommand;
import com.lovetropics.gamemodebuild.command.ItemFilterArgument;
import com.lovetropics.gamemodebuild.message.GBNetwork;
import com.lovetropics.gamemodebuild.message.ListUpdateMessage;
import com.lovetropics.gamemodebuild.message.OpenBuildInventoryMessage;
import com.lovetropics.gamemodebuild.message.SetActiveMessage;
import com.lovetropics.gamemodebuild.message.SetScrollMessage;
import com.lovetropics.gamemodebuild.message.UpdateFilterMessage;

import net.minecraft.command.arguments.ArgumentSerializer;
import net.minecraft.command.arguments.ArgumentTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig.Type;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;

@Mod(GamemodeBuild.MODID)
public class GamemodeBuild {
	public static final String MODID = "gamemodebuild";
	public static final String NAME = "Build Mode";
	
	private static final Logger LOGGER = LogManager.getLogger();

	public GamemodeBuild() {
    	// Compatible with all versions that match the semver (excluding the qualifier e.g. "-beta+42")
    	ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.DISPLAYTEST, () -> Pair.of(GamemodeBuild::getCompatVersion, (s, v) -> GamemodeBuild.isCompatibleVersion(s)));

        // Register the setup method for modloading
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
		// Register the doClientStuff method for modloading
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::doClientStuff);
		
		// Register ourselves for server and other game events we are interested in
		MinecraftForge.EVENT_BUS.register(this);
		
		ModLoadingContext.get().registerConfig(Type.SERVER, GBConfigs.serverSpec);
	}

    private static final Pattern QUALIFIER = Pattern.compile("-\\w+\\+\\d+");
    public static String getCompatVersion() {
    	return getCompatVersion(ModList.get().getModContainerById(MODID).orElseThrow(IllegalStateException::new).getModInfo().getVersion().toString());
    }
    private static String getCompatVersion(String fullVersion) {
    	return QUALIFIER.matcher(fullVersion).replaceAll("");
    }
    public static boolean isCompatibleVersion(String version) {
    	return getCompatVersion().equals(getCompatVersion(version));
    }

	private void setup(final FMLCommonSetupEvent event) {
		GBNetwork.register();
		ArgumentTypes.register(GamemodeBuild.MODID + ":item_filter", ItemFilterArgument.class, new ArgumentSerializer<>(ItemFilterArgument::itemFilter));
	}
	
	private void doClientStuff(final FMLClientSetupEvent event) {
		GBKeyBindings.register();
	}
	
	@SubscribeEvent
	public void serverStarting(FMLServerStartingEvent event) {
		GamemodeBuildCommand.register(event.getCommandDispatcher());
	}
}
