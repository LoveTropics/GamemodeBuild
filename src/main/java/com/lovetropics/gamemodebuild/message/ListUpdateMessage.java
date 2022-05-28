package com.lovetropics.gamemodebuild.message;

import com.lovetropics.gamemodebuild.GBConfigs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class ListUpdateMessage {
	final String    name;
	final boolean   whitelist;
	final Operation operation;
	final String    entry;

	public enum Operation {
		ADD, REMOVE, CLEAR;

		public byte serialize() {
			switch (this) {
				case ADD:
					return 1;
				case REMOVE:
					return 2;
				case CLEAR:
					return 3;
				default:
					throw new IllegalStateException("HOW???");
			}
		}

		public static Operation deserialize(byte data) {
			switch (data) {
				case 1:
					return ADD;
				case 2:
					return REMOVE;
				case 3:
					return CLEAR;
				default:
					throw new IllegalArgumentException("Operation can only be a value from 1 to 3");
			}
		}
	}

	public ListUpdateMessage(Operation operation, boolean whitelist, String name, String entry) {
		this.operation = operation;
		this.whitelist = whitelist;
		this.name = name;
		this.entry = entry;
	}

	public ListUpdateMessage(FriendlyByteBuf buf) {
		this(Operation.deserialize(buf.readByte()), buf.readBoolean(), buf.readBoolean() ? buf.readUtf(64) : null, buf.readBoolean() ? buf.readUtf(100) : null);
	}

	public void serialize(FriendlyByteBuf buf) {
		buf.writeByte(operation.serialize());
		buf.writeBoolean(whitelist);

		buf.writeBoolean(name != null);
		if (name != null) buf.writeUtf(name, 64);

		buf.writeBoolean(entry != null);
		if (entry != null) buf.writeUtf(entry, 100);
	}

	public static boolean handle(ListUpdateMessage message, Supplier<NetworkEvent.Context> ctxSupplier) {
		ctxSupplier.get().enqueueWork(() -> {
			switch (message.operation) {
				case ADD:
					if (message.whitelist) {
						GBConfigs.SERVER.addToWhitelist(message.name, message.entry, false);
					} else {
						GBConfigs.SERVER.addToBlacklist(message.name, message.entry, false);
					}
					break;
				case REMOVE:
					if (message.whitelist) {
						GBConfigs.SERVER.removeFromWhitelist(message.name, message.entry, false);
					} else {
						GBConfigs.SERVER.removeFromBlacklist(message.name, message.entry, false);
					}
					break;
				case CLEAR:
					if (message.whitelist) {
						GBConfigs.SERVER.clearWhitelist(message.name, false);
					} else {
						GBConfigs.SERVER.clearBlacklist(message.name, false);
					}
					break;
			}
		});
		ctxSupplier.get().setPacketHandled(true);
		return true;
	}
}
