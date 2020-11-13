package com.lovetropics.gamemodebuild.message;

import com.lovetropics.gamemodebuild.GamemodeBuild;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;

public class GBNetwork {

	public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder.named(new ResourceLocation(GamemodeBuild.MODID, "net"))
			.networkProtocolVersion(GamemodeBuild::getCompatVersion)
			.clientAcceptedVersions(GamemodeBuild::isCompatibleVersion)
			.serverAcceptedVersions(GamemodeBuild::isCompatibleVersion)
			.simpleChannel();

	public static void register() {
		CHANNEL.messageBuilder(OpenBuildInventoryMessage.class, 0, NetworkDirection.PLAY_TO_SERVER)
				.encoder(OpenBuildInventoryMessage::serialize).decoder(OpenBuildInventoryMessage::deserialize)
				.consumer(OpenBuildInventoryMessage::handle)
				.add();

		CHANNEL.messageBuilder(SetActiveMessage.class, 1)
				.encoder(SetActiveMessage::serialize).decoder(SetActiveMessage::deserialize)
				.consumer(SetActiveMessage::handle)
				.add();
		
		CHANNEL.messageBuilder(SetScrollMessage.class, 2, NetworkDirection.PLAY_TO_SERVER)
				.encoder(SetScrollMessage::serialize).decoder(SetScrollMessage::deserialize)
				.consumer(SetScrollMessage::handle)
				.add();
		
		CHANNEL.messageBuilder(ListUpdateMessage.class, 3, NetworkDirection.PLAY_TO_CLIENT)
				.encoder(ListUpdateMessage::serialize).decoder(ListUpdateMessage::new)
				.consumer(ListUpdateMessage::handle)
				.add();
		
		CHANNEL.messageBuilder(UpdateFilterMessage.class, 4, NetworkDirection.PLAY_TO_SERVER)
				.encoder(UpdateFilterMessage::serialize).decoder(UpdateFilterMessage::new)
				.consumer(UpdateFilterMessage::handle)
				.add();

	}
}
