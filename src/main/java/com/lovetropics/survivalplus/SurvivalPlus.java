package com.lovetropics.survivalplus;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig.Type;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;

@Mod(SurvivalPlus.MODID)
public class SurvivalPlus
{
	public static final String MODID = "survivalplus";

	private static final Logger LOGGER = LogManager.getLogger();

	private static final String NET_PROTOCOL = "1";

	public static final SimpleChannel NETWORK = NetworkRegistry.ChannelBuilder.named(new ResourceLocation(MODID, "net"))
			.networkProtocolVersion(() -> NET_PROTOCOL)
			.clientAcceptedVersions(NET_PROTOCOL::equals)
			.serverAcceptedVersions(NET_PROTOCOL::equals)
			.simpleChannel();

	public SurvivalPlus() {
		// Register the setup method for modloading
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
		// Register the doClientStuff method for modloading
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::doClientStuff);

		// Register ourselves for server and other game events we are interested in
		MinecraftForge.EVENT_BUS.register(this);
		
		ModLoadingContext.get().registerConfig(Type.SERVER, SPConfigs.serverSpec);
	}

	private void setup(final FMLCommonSetupEvent event) {
		NETWORK.messageBuilder(OpenSPInventoryMessage.class, 0, NetworkDirection.PLAY_TO_SERVER)
				.encoder(OpenSPInventoryMessage::serialize).decoder(OpenSPInventoryMessage::deserialize)
				.consumer(OpenSPInventoryMessage::handle)
				.add();
		
		NETWORK.messageBuilder(SetSPEnabledMessage.class, 1, NetworkDirection.PLAY_TO_SERVER)
				.encoder(SetSPEnabledMessage::serialize).decoder(SetSPEnabledMessage::deserialize)
				.consumer(SetSPEnabledMessage::handle)
				.add();
	}

	private void doClientStuff(final FMLClientSetupEvent event) {
		SPKeyBindings.register();
	}
}
