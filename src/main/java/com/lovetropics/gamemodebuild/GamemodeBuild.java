package com.lovetropics.gamemodebuild;

import com.lovetropics.gamemodebuild.command.GamemodeBuildCommand;
import com.lovetropics.gamemodebuild.command.ItemFilterArgument;
import com.lovetropics.gamemodebuild.container.BuildContainer;
import com.lovetropics.gamemodebuild.message.GBNetwork;
import com.lovetropics.gamemodebuild.state.GBClientState;
import com.lovetropics.gamemodebuild.state.GBPlayerStore;
import com.lovetropics.gamemodebuild.state.GBServerState;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig.Type;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.regex.Pattern;

@Mod(GamemodeBuild.MODID)
public class GamemodeBuild {
	public static final String MODID = "gamemodebuild";
	public static final String NAME = "Build Mode";

	private static final DeferredRegister<ArgumentTypeInfo<?, ?>> ARGUMENT_REGISTER = DeferredRegister.create(ForgeRegistries.COMMAND_ARGUMENT_TYPES, MODID);

	private static final RegistryObject<ArgumentTypeInfo<?, ?>> ITEM_FILTER_ARGUMENT = ARGUMENT_REGISTER.register("item_filter", () -> ArgumentTypeInfos.registerByClass(ItemFilterArgument.class, SingletonArgumentInfo.contextAware(ItemFilterArgument::new)));

	public GamemodeBuild() {
    	// Compatible with all versions that match the semver (excluding the qualifier e.g. "-beta+42")
    	ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class, () -> new IExtensionPoint.DisplayTest(GamemodeBuild::getCompatVersion, (s, v) -> GamemodeBuild.isCompatibleVersion(s)));

		IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
		modBus.addListener(this::setup);
		ARGUMENT_REGISTER.register(modBus);
		BuildContainer.REGISTER.register(modBus);

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
	}

	@SubscribeEvent
	public void registerCommands(RegisterCommandsEvent event) {
		GamemodeBuildCommand.register(event.getDispatcher(), event.getBuildContext());
	}

	@SubscribeEvent
	public void onBreakSpeed(final PlayerEvent.BreakSpeed event) {
		if (event.getEntity().onGround()) return;

		if (GBConfigs.SERVER.removeBreakSpeedDebuff() && isActive(event.getEntity())) {
			// See Player#getDigSpeed, if the player is flying they break blocks 5 times slower.
			// Let's revert that as it's an annoying limitation in build mode
			event.setNewSpeed(event.getNewSpeed() * 5f);
		}
	}

	public static boolean isActive(Player player) {
		if (player.level().isClientSide()) {
			return GBClientState.isActive();
		}
		return player instanceof ServerPlayer sp ? GBServerState.isActiveFor(sp) : GBPlayerStore.isActive(player);
	}
}
