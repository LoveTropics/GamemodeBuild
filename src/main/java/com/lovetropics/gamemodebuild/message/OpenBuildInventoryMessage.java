package com.lovetropics.gamemodebuild.message;

import java.util.function.Supplier;

import com.lovetropics.gamemodebuild.container.BuildContainer;
import com.lovetropics.gamemodebuild.state.GBPlayerStore;
import com.lovetropics.gamemodebuild.state.GBServerState;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.SimpleNamedContainerProvider;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.NetworkHooks;

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
				NetworkHooks.openGui(player, new SimpleNamedContainerProvider(BuildContainer::new, BuildContainer.title()), buf -> buf.writeString(GBPlayerStore.getList(player)));
			}
		});
		
		return true;
	}
}
