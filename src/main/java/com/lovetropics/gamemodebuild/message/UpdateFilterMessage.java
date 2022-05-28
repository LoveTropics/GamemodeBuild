package com.lovetropics.gamemodebuild.message;

import java.util.BitSet;
import java.util.function.Supplier;

import com.lovetropics.gamemodebuild.container.BuildContainer;

import net.minecraft.inventory.container.Container;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

public class UpdateFilterMessage {
	
	private final BitSet filter;
	
	public UpdateFilterMessage(BitSet filter) {
		this.filter = filter;
	}

	public UpdateFilterMessage(PacketBuffer buf) {
		this(BitSet.valueOf(buf.readByteArray()));
	}
	
	public void serialize(PacketBuffer buf) {
		buf.writeByteArray(filter.toByteArray());
	}
	
	public static boolean handle(UpdateFilterMessage message, Supplier<NetworkEvent.Context> ctxSupplier) {
		ctxSupplier.get().enqueueWork(() -> {
			Container c = ctxSupplier.get().getSender().containerMenu;
			if (c instanceof BuildContainer) {
				((BuildContainer) c).setFilter(message.filter);
			}
		});
		return true;
	}
}
