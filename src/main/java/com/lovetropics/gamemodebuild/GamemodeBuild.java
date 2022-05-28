package com.lovetropics.gamemodebuild;

import com.lovetropics.gamemodebuild.command.GamemodeBuildCommand;
import com.lovetropics.gamemodebuild.command.ItemFilterArgument;
import com.lovetropics.gamemodebuild.message.GBNetwork;
import net.minecraft.commands.synchronization.EmptyArgumentSerializer;
import net.minecraft.commands.synchronization.ArgumentTypes;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig.Type;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.regex.Pattern;

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
		ArgumentTypes.register(GamemodeBuild.MODID + ":item_filter", ItemFilterArgument.class, new EmptyArgumentSerializer<>(ItemFilterArgument::itemFilter));
	}
	
	private void doClientStuff(final FMLClientSetupEvent event) {
		GBKeyBindings.register();
	}
	
	@SubscribeEvent
	public void registerCommands(RegisterCommandsEvent event) {
		GamemodeBuildCommand.register(event.getDispatcher());
	}
}
