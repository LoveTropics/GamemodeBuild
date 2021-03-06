package com.lovetropics.gamemodebuild.message;

import com.lovetropics.gamemodebuild.state.GBClientState;
import com.lovetropics.gamemodebuild.state.GBServerState;
import com.lovetropics.gamemodebuild.state.GBServerState.NotificationType;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public final class SetActiveMessage {
	private final boolean enabled;
	
	public SetActiveMessage(boolean enabled) {
		this.enabled = enabled;
	}
	
	public void serialize(PacketBuffer buffer) {
		buffer.writeBoolean(this.enabled);
	}
	
	public static SetActiveMessage deserialize(PacketBuffer buffer) {
		return new SetActiveMessage(buffer.readBoolean());
	}
	
	public static boolean handle(SetActiveMessage message, Supplier<NetworkEvent.Context> ctxSupplier) {
		NetworkEvent.Context ctx = ctxSupplier.get();
		ctx.enqueueWork(() -> {
			if (ctx.getDirection() == NetworkDirection.PLAY_TO_SERVER) {
				ServerPlayerEntity player = ctx.getSender();
				if (player != null) {
					if (GBServerState.isEnabledFor(player)) {
						GBServerState.setActiveFor(player, message.enabled);
					} else {
						GBServerState.notifyPlayerActivity(player, NotificationType.ACTIVE);
					}
				}
			} else if (ctx.getDirection() == NetworkDirection.PLAY_TO_CLIENT) {
				DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> setClientState(message.enabled));
			}
		});
		
		return true;
	}
	
	@OnlyIn(Dist.CLIENT)
	private static void setClientState(boolean state) {
		GBClientState.setActive(state);
	}
}
