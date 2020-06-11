package com.lovetropics.gamemodebuild.state;

import com.lovetropics.gamemodebuild.SPConfigs;
import com.lovetropics.gamemodebuild.SurvivalPlus;
import com.lovetropics.gamemodebuild.message.SetSPActiveMessage;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.network.PacketDistributor;

@Mod.EventBusSubscriber(modid = SurvivalPlus.MODID)
public final class SPServerState {
	public static void setGloballyEnabled(MinecraftServer server, boolean enabled) {
		SPConfigs.SERVER.enable(enabled);
		
		for (ServerPlayerEntity player : server.getPlayerList().getPlayers()) {
			notifyPlayerActivity(player);
		}
	}
	
	public static void setEnabledFor(ServerPlayerEntity player, boolean enabled) {
		SPPlayerStore.setEnabled(player, enabled);
		notifyPlayerActivity(player);
	}
	
	public static boolean isGloballyEnabled() {
		return SPConfigs.SERVER.enabled();
	}
	
	public static boolean isEnabledFor(ServerPlayerEntity player) {
		if (!isGloballyEnabled()) {
			return false;
		}
		return SPPlayerStore.isEnabled(player);
	}
	
	public static void setActiveFor(ServerPlayerEntity player, boolean active) {
		if (isEnabledFor(player) || !active) {
			SPPlayerStore.setActive(player, active);
			notifyPlayerActivity(player);
		}
	}
	
	public static boolean isActiveFor(ServerPlayerEntity player) {
		return isEnabledFor(player) && SPPlayerStore.isActive(player);
	}
	
	public static void switchInventories(ServerPlayerEntity player, boolean state) {
		if (state) {
			SPPlayerStore.switchToSPInventory(player);
		} else {
			SPPlayerStore.switchToPlayerInventory(player);
		}
	}
	
	private static void notifyPlayerActivity(ServerPlayerEntity player) {
		SetSPActiveMessage message = new SetSPActiveMessage(isActiveFor(player));
		SurvivalPlus.NETWORK.send(PacketDistributor.PLAYER.with(() -> player), message);
	}
	
	@SubscribeEvent
	public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
		PlayerEntity player = event.getPlayer();
		if (!player.world.isRemote && player instanceof ServerPlayerEntity) {
			notifyPlayerActivity((ServerPlayerEntity) player);
		}
	}
}
