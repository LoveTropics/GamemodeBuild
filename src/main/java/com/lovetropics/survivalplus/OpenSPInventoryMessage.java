package com.lovetropics.survivalplus;

import java.util.function.Supplier;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.SimpleNamedContainerProvider;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

public final class OpenSPInventoryMessage {
	public void serialize(PacketBuffer buffer) {
	}
	
	public static OpenSPInventoryMessage deserialize(PacketBuffer buffer) {
		return new OpenSPInventoryMessage();
	}
	
	public static boolean handle(OpenSPInventoryMessage message, Supplier<NetworkEvent.Context> ctxSupplier) {
		NetworkEvent.Context ctx = ctxSupplier.get();
		ctx.enqueueWork(() -> {
			ServerPlayerEntity player = ctx.getSender();
			if (player != null && SPPlayerState.isEnabled(player)) {
				player.openContainer(new SimpleNamedContainerProvider(SurvivalPlusContainer::new, SurvivalPlusContainer.title()));
			}
		});
		
		return true;
	}
}
