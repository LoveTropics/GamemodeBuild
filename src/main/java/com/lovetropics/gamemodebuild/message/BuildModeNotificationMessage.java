package com.lovetropics.gamemodebuild.message;

import com.lovetropics.gamemodebuild.GamemodeBuild;
import com.lovetropics.gamemodebuild.client.GBClientToast;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record BuildModeNotificationMessage(boolean enabled) implements CustomPacketPayload {
	public static final Type<BuildModeNotificationMessage> TYPE = new Type<>(GamemodeBuild.rl("build_mode_notification"));
	public static final StreamCodec<RegistryFriendlyByteBuf, BuildModeNotificationMessage> CODEC = StreamCodec.composite(
			ByteBufCodecs.BOOL, BuildModeNotificationMessage::enabled,
			BuildModeNotificationMessage::new
	);

	public void handle(IPayloadContext context) {
		context.enqueueWork(() -> GBClientToast.showBuildModeToast(enabled));
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
