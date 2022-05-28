package com.lovetropics.gamemodebuild.message;

import java.util.BitSet;
import java.util.function.Supplier;

import com.lovetropics.gamemodebuild.container.BuildContainer;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.fml.network.NetworkEvent;

public class UpdateFilterMessage {
	
	private final BitSet filter;
	
	public UpdateFilterMessage(BitSet filter) {
		this.filter = filter;
	}

	public UpdateFilterMessage(FriendlyByteBuf buf) {
		this(BitSet.valueOf(buf.readByteArray()));
	}
	
	public void serialize(FriendlyByteBuf buf) {
		buf.writeByteArray(filter.toByteArray());
	}
	
	public static boolean handle(UpdateFilterMessage message, Supplier<NetworkEvent.Context> ctxSupplier) {
		ctxSupplier.get().enqueueWork(() -> {
			AbstractContainerMenu c = ctxSupplier.get().getSender().containerMenu;
			if (c instanceof BuildContainer) {
				((BuildContainer) c).setFilter(message.filter);
			}
		});
		return true;
	}
}
