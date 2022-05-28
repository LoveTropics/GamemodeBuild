package com.lovetropics.gamemodebuild.message;

import java.util.function.Supplier;

import com.lovetropics.gamemodebuild.container.BuildContainer;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkEvent;

public final class SetScrollMessage {
	private final int scrollOffset;
	
	public SetScrollMessage(int scrollOffset) {
		this.scrollOffset = scrollOffset;
	}
	
	public void serialize(FriendlyByteBuf buffer) {
		buffer.writeVarInt(this.scrollOffset);
	}
	
	public static SetScrollMessage deserialize(FriendlyByteBuf buffer) {
		return new SetScrollMessage(buffer.readVarInt());
	}
	
	public static boolean handle(SetScrollMessage message, Supplier<NetworkEvent.Context> ctxSupplier) {
		NetworkEvent.Context ctx = ctxSupplier.get();
		ctx.enqueueWork(() -> {
			if (ctx.getDirection() == NetworkDirection.PLAY_TO_SERVER) {
				ServerPlayer player = ctx.getSender();
				if (player == null) return;
				
				if (player.containerMenu instanceof BuildContainer) {
					((BuildContainer) player.containerMenu).setScrollOffset(message.scrollOffset);
				}
			}
		});
		
		return true;
	}
}
