package com.lovetropics.gamemodebuild.message;

import com.lovetropics.gamemodebuild.GamemodeBuild;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class GBNetwork {

	public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder.named(new ResourceLocation(GamemodeBuild.MODID, "net"))
			.networkProtocolVersion(GamemodeBuild::getCompatVersion)
			.clientAcceptedVersions(GamemodeBuild::isCompatibleVersion)
			.serverAcceptedVersions(GamemodeBuild::isCompatibleVersion)
			.simpleChannel();

	public static void register() {
		CHANNEL.messageBuilder(OpenBuildInventoryMessage.class, 0, NetworkDirection.PLAY_TO_SERVER)
				.encoder(OpenBuildInventoryMessage::serialize).decoder(OpenBuildInventoryMessage::new)
				.consumerMainThread(OpenBuildInventoryMessage::handle)
				.add();

		CHANNEL.messageBuilder(SetActiveMessage.class, 1)
				.encoder(SetActiveMessage::serialize).decoder(SetActiveMessage::new)
				.consumerMainThread(SetActiveMessage::handle)
				.add();
		
		CHANNEL.messageBuilder(SetScrollMessage.class, 2, NetworkDirection.PLAY_TO_SERVER)
				.encoder(SetScrollMessage::serialize).decoder(SetScrollMessage::new)
				.consumerMainThread(SetScrollMessage::handle)
				.add();
		
		CHANNEL.messageBuilder(ListUpdateMessage.class, 3, NetworkDirection.PLAY_TO_CLIENT)
				.encoder(ListUpdateMessage::serialize).decoder(ListUpdateMessage::new)
				.consumerMainThread(ListUpdateMessage::handle)
				.add();
		
		CHANNEL.messageBuilder(UpdateFilterMessage.class, 4, NetworkDirection.PLAY_TO_SERVER)
				.encoder(UpdateFilterMessage::serialize).decoder(UpdateFilterMessage::new)
				.consumerMainThread(UpdateFilterMessage::handle)
				.add();

	}
}
