package com.lovetropics.gamemodebuild.message;

import java.util.function.Supplier;

import com.lovetropics.gamemodebuild.GBConfigs;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

public class ListUpdateMessage {
	
	final boolean whitelist;
	final boolean add;
	final String entry;
	
	public ListUpdateMessage(boolean whitelist, boolean add, String entry) {
		this.whitelist = whitelist;
		this.add = add;
		this.entry = entry;
	}

	public ListUpdateMessage(PacketBuffer buf) {
		this(buf.readBoolean(), buf.readBoolean(), buf.readBoolean() ? buf.readString(100) : null);
	}
	
	public void serialize(PacketBuffer buf) {
		buf.writeBoolean(whitelist);
		buf.writeBoolean(add);
		buf.writeBoolean(entry != null);
		if (this.entry != null) {
			buf.writeString(this.entry, 100);
		}
	}
	
	public static boolean handle(ListUpdateMessage message, Supplier<NetworkEvent.Context> ctxSupplier) {
		ctxSupplier.get().enqueueWork(() -> {
			if (message.whitelist) {
				if (message.entry == null) {
					GBConfigs.SERVER.clearWhitelist(false);
				} else if (message.add) {
					GBConfigs.SERVER.addWhitelist(message.entry, false);
				} else {
					GBConfigs.SERVER.removeWhitelist(message.entry, false);
				}
			} else {
				if (message.entry == null) {
					GBConfigs.SERVER.clearBlacklist(false);
				} else if (message.add) {
					GBConfigs.SERVER.addBlacklist(message.entry, false);
				} else {
					GBConfigs.SERVER.removeBlacklist(message.entry, false);
				}
			}
		});
		ctxSupplier.get().setPacketHandled(true);
		return true;
	}
}
