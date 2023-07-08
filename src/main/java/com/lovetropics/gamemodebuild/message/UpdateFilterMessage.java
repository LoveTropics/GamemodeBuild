package com.lovetropics.gamemodebuild.message;

import com.lovetropics.gamemodebuild.container.BuildContainer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.BitSet;
import java.util.function.Supplier;

public record UpdateFilterMessage(BitSet filter) {
	public UpdateFilterMessage(FriendlyByteBuf input) {
		this(BitSet.valueOf(input.readByteArray()));
	}

	public void serialize(FriendlyByteBuf output) {
		output.writeByteArray(filter.toByteArray());
	}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ServerPlayer sender = ctx.get().getSender();
		if (sender != null && sender.containerMenu instanceof BuildContainer container) {
			container.setFilter(filter);
		}
	}
}
