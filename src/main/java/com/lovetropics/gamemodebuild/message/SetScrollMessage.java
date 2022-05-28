package com.lovetropics.gamemodebuild.message;

import com.lovetropics.gamemodebuild.container.BuildContainer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

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
