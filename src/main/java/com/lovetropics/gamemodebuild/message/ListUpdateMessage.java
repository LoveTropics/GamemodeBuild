package com.lovetropics.gamemodebuild.message;

import com.lovetropics.gamemodebuild.GBConfigs;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class ListUpdateMessage {
	final String    name;
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

	public ListUpdateMessage(Operation operation, String name, String entry) {
		this.operation = operation;
		this.name = name;
		this.entry = entry;
	}

	public ListUpdateMessage(PacketBuffer buf) {
		this(Operation.deserialize(buf.readByte()), buf.readBoolean() ? buf.readString(64) : null, buf.readBoolean() ? buf.readString(100) : null);
	}

	public void serialize(PacketBuffer buf) {
		buf.writeByte(operation.serialize());

		buf.writeBoolean(name != null);
		if (name != null) buf.writeString(name, 64);

		buf.writeBoolean(entry != null);
		if (entry != null) buf.writeString(entry, 100);
	}

	public static boolean handle(ListUpdateMessage message, Supplier<NetworkEvent.Context> ctxSupplier) {
		ctxSupplier.get().enqueueWork(() -> {
			switch (message.operation) {
				case ADD:
					GBConfigs.SERVER.addToList(message.name, message.entry, false);
					break;
				case REMOVE:
					GBConfigs.SERVER.removeFromList(message.name, message.entry, false);
					break;
				case CLEAR:
					GBConfigs.SERVER.clearList(message.name, false);
					break;
			}
		});
		ctxSupplier.get().setPacketHandled(true);
		return true;
	}
}
