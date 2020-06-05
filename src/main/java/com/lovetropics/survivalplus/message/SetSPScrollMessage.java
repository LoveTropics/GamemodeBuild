package com.lovetropics.survivalplus.message;

import java.util.function.Supplier;

import com.lovetropics.survivalplus.SurvivalPlusContainer;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkEvent;

public final class SetSPScrollMessage {
	private final int scrollOffset;
	
	public SetSPScrollMessage(int scrollOffset) {
		this.scrollOffset = scrollOffset;
	}
	
	public void serialize(PacketBuffer buffer) {
		buffer.writeVarInt(this.scrollOffset);
	}
	
	public static SetSPScrollMessage deserialize(PacketBuffer buffer) {
		return new SetSPScrollMessage(buffer.readVarInt());
	}
	
	public static boolean handle(SetSPScrollMessage message, Supplier<NetworkEvent.Context> ctxSupplier) {
		NetworkEvent.Context ctx = ctxSupplier.get();
		ctx.enqueueWork(() -> {
			if (ctx.getDirection() == NetworkDirection.PLAY_TO_SERVER) {
				ServerPlayerEntity player = ctx.getSender();
				if (player == null) return;
				
				if (player.openContainer instanceof SurvivalPlusContainer) {
					((SurvivalPlusContainer) player.openContainer).setScrollOffset(message.scrollOffset);
				}
			}
		});
		
		return true;
	}
}
