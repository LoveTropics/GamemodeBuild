package com.lovetropics.gamemodebuild.message;

import com.lovetropics.gamemodebuild.GBConfigs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ListUpdateMessage(Operation operation, boolean whitelist, String name, String entry) {
	public void handle(Supplier<NetworkEvent.Context> ctx) {
		switch (operation) {
			case ADD -> {
				if (whitelist) {
					GBConfigs.SERVER.addToWhitelist(name, entry, false);
				} else {
					GBConfigs.SERVER.addToBlacklist(name, entry, false);
				}
			}
			case REMOVE -> {
				if (whitelist) {
					GBConfigs.SERVER.removeFromWhitelist(name, entry, false);
				} else {
					GBConfigs.SERVER.removeFromBlacklist(name, entry, false);
				}
			}
			case CLEAR -> {
				if (whitelist) {
					GBConfigs.SERVER.clearWhitelist(name, false);
				} else {
					GBConfigs.SERVER.clearBlacklist(name, false);
				}
			}
		}
	}

	public enum Operation {
		ADD, REMOVE, CLEAR;

		public byte serialize() {
			return (byte) switch (this) {
				case ADD -> 1;
				case REMOVE -> 2;
				case CLEAR -> 3;
			};
		}

		public static Operation deserialize(byte data) {
			return switch (data) {
				case 1 -> ADD;
				case 2 -> REMOVE;
				case 3 -> CLEAR;
				default -> throw new IllegalArgumentException("Operation can only be a value from 1 to 3");
			};
		}
	}

	public ListUpdateMessage(FriendlyByteBuf input) {
		this(Operation.deserialize(input.readByte()), input.readBoolean(), input.readNullable(b -> b.readUtf(64)), input.readNullable(b -> b.readUtf(100)));
	}

	public void serialize(FriendlyByteBuf output) {
		output.writeByte(operation.serialize());
		output.writeBoolean(whitelist);

		output.writeNullable(name, (b, s) -> b.writeUtf(s, 64));
		output.writeNullable(entry, (b, s) -> b.writeUtf(s, 100));
	}
}
