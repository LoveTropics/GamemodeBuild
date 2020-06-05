package com.lovetropics.survivalplus.message;

import java.util.function.Supplier;

import com.lovetropics.survivalplus.SPPlayerState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkDirection;
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
			if (ctx.getDirection() == NetworkDirection.PLAY_TO_SERVER) {
				ServerPlayerEntity player = ctx.getSender();
				if (player != null) {
					SPPlayerState.setEnabled(player, message.enabled);
				}
			} else if (ctx.getDirection() == NetworkDirection.PLAY_TO_CLIENT) {
				DistExecutor.runWhenOn(Dist.CLIENT,  () -> () -> {
					ClientPlayerEntity player = Minecraft.getInstance().player;
					if (player != null) {
						SPPlayerState.setEnabled(player, message.enabled);
					}
				});
			}
		});
		
		return true;
	}
}
