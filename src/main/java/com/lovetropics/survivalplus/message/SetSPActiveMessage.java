package com.lovetropics.survivalplus.message;

import java.util.function.Supplier;

import com.lovetropics.survivalplus.SPPlayerState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.ChatType;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkEvent;

public final class SetSPActiveMessage {
	private final boolean enabled;
	
	public SetSPActiveMessage(boolean enabled) {
		this.enabled = enabled;
	}
	
	public void serialize(PacketBuffer buffer) {
		buffer.writeBoolean(this.enabled);
	}
	
	public static SetSPActiveMessage deserialize(PacketBuffer buffer) {
		return new SetSPActiveMessage(buffer.readBoolean());
	}
	
	public static boolean handle(SetSPActiveMessage message, Supplier<NetworkEvent.Context> ctxSupplier) {
		NetworkEvent.Context ctx = ctxSupplier.get();
		ctx.enqueueWork(() -> {
			if (ctx.getDirection() == NetworkDirection.PLAY_TO_SERVER) {
				ServerPlayerEntity player = ctx.getSender();
				if (player != null) {
					if (!SPPlayerState.isEnabled(player)) {
						player.sendMessage(new StringTextComponent("SurvivalPlus is disabled!"), ChatType.GAME_INFO);
					} else {
						SPPlayerState.setActive(player, message.enabled);
						if (message.enabled) {
							player.sendMessage(new StringTextComponent("SurvivalPlus activated"), ChatType.GAME_INFO);
						} else {
							player.sendMessage(new StringTextComponent("SurvivalPlus deactivated"), ChatType.GAME_INFO);
						}
					}
				}
			} else if (ctx.getDirection() == NetworkDirection.PLAY_TO_CLIENT) {
				DistExecutor.runWhenOn(Dist.CLIENT,  () -> () -> {
					ClientPlayerEntity player = Minecraft.getInstance().player;
					if (player != null) {
						SPPlayerState.setActive(player, message.enabled);
					}
				});
			}
		});
		
		return true;
	}
}
