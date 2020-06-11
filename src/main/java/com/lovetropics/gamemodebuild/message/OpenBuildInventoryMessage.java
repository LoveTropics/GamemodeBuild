package com.lovetropics.gamemodebuild.message;

import java.util.function.Supplier;

import com.lovetropics.gamemodebuild.container.BuildContainer;
import com.lovetropics.gamemodebuild.state.GBServerState;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.SimpleNamedContainerProvider;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

public final class OpenBuildInventoryMessage {
	public void serialize(PacketBuffer buffer) {
	}
	
	public static OpenBuildInventoryMessage deserialize(PacketBuffer buffer) {
		return new OpenBuildInventoryMessage();
	}
	
	public static boolean handle(OpenBuildInventoryMessage message, Supplier<NetworkEvent.Context> ctxSupplier) {
		NetworkEvent.Context ctx = ctxSupplier.get();
		ctx.enqueueWork(() -> {
			ServerPlayerEntity player = ctx.getSender();
			if (player != null && GBServerState.isActiveFor(player)) {
				player.openContainer(new SimpleNamedContainerProvider(BuildContainer::new, BuildContainer.title()));
			}
		});
		
		return true;
	}
}
