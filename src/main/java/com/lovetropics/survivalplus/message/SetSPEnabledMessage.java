package com.lovetropics.survivalplus.message;

import java.util.function.Supplier;

import com.lovetropics.survivalplus.SPPlayerState;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

public final class SetSPEnabledMessage {
	private final boolean enabled;
	
	public SetSPEnabledMessage(boolean enabled) {
		this.enabled = enabled;
	}
	
	public void serialize(PacketBuffer buffer) {
		buffer.writeBoolean(this.enabled);
	}
	
	public static SetSPEnabledMessage deserialize(PacketBuffer buffer) {
		return new SetSPEnabledMessage(buffer.readBoolean());
	}
	
	public static boolean handle(SetSPEnabledMessage message, Supplier<NetworkEvent.Context> ctxSupplier) {
		NetworkEvent.Context ctx = ctxSupplier.get();
		ctx.enqueueWork(() -> {
			ServerPlayerEntity player = ctx.getSender();
			if (player != null) {
				SPPlayerState.setEnabled(player, message.enabled);
			}
		});
		
		return true;
	}
}
