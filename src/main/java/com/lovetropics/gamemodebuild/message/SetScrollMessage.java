package com.lovetropics.gamemodebuild.message;

import com.lovetropics.gamemodebuild.container.BuildContainer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SetScrollMessage(int scrollOffset) {
	public SetScrollMessage(FriendlyByteBuf input) {
		this(input.readVarInt());
	}

	public void serialize(FriendlyByteBuf output) {
		output.writeVarInt(scrollOffset);
	}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ServerPlayer player = ctx.get().getSender();
		if (player != null && player.containerMenu instanceof BuildContainer container) {
			container.setScrollOffset(scrollOffset);
		}
	}
}
