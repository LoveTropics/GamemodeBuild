package com.lovetropics.gamemodebuild.message;

import java.util.function.Supplier;

import com.lovetropics.gamemodebuild.container.BuildContainer;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkEvent;

public final class SetScrollMessage {
	private final int scrollOffset;
	
	public SetScrollMessage(int scrollOffset) {
		this.scrollOffset = scrollOffset;
	}
	
	public void serialize(PacketBuffer buffer) {
		buffer.writeVarInt(this.scrollOffset);
	}
	
	public static SetScrollMessage deserialize(PacketBuffer buffer) {
		return new SetScrollMessage(buffer.readVarInt());
	}
	
	public static boolean handle(SetScrollMessage message, Supplier<NetworkEvent.Context> ctxSupplier) {
		NetworkEvent.Context ctx = ctxSupplier.get();
		ctx.enqueueWork(() -> {
			if (ctx.getDirection() == NetworkDirection.PLAY_TO_SERVER) {
				ServerPlayerEntity player = ctx.getSender();
				if (player == null) return;
				
				if (player.openContainer instanceof BuildContainer) {
					((BuildContainer) player.openContainer).setScrollOffset(message.scrollOffset);
				}
			}
		});
		
		return true;
	}
}
