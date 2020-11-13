package com.lovetropics.gamemodebuild.state;

import com.lovetropics.gamemodebuild.GBConfigs;
import com.lovetropics.gamemodebuild.GamemodeBuild;
import com.lovetropics.gamemodebuild.message.GBNetwork;
import com.lovetropics.gamemodebuild.message.SetActiveMessage;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.network.PacketDistributor;

@Mod.EventBusSubscriber(modid = GamemodeBuild.MODID)
public final class GBServerState {
	public static void setGloballyEnabled(MinecraftServer server, boolean enabled) {
		GBConfigs.SERVER.enable(enabled);
		
		for (ServerPlayerEntity player : server.getPlayerList().getPlayers()) {
			notifyPlayerActivity(player);
		}
	}
	
	public static void setEnabledFor(ServerPlayerEntity player, boolean enabled) {
		GBPlayerStore.setEnabled(player, enabled);
		notifyPlayerActivity(player);
	}
	
	public static boolean isGloballyEnabled() {
		return GBConfigs.SERVER.enabled();
	}
	
	public static boolean isEnabledFor(ServerPlayerEntity player) {
		if (!isGloballyEnabled()) {
			return false;
		}
		return GBPlayerStore.isEnabled(player);
	}
	
	public static void setActiveFor(ServerPlayerEntity player, boolean active) {
		if (isEnabledFor(player) || !active) {
			GBPlayerStore.setActive(player, active);
			notifyPlayerActivity(player);
		}
	}
	
	public static boolean isActiveFor(ServerPlayerEntity player) {
		return isEnabledFor(player) && GBPlayerStore.isActive(player);
	}
	
	public static void switchInventories(ServerPlayerEntity player, boolean state) {
		if (state) {
			GBPlayerStore.switchToSPInventory(player);
		} else {
			GBPlayerStore.switchToPlayerInventory(player);
		}
	}
	
	private static void notifyPlayerActivity(ServerPlayerEntity player) {
		SetActiveMessage message = new SetActiveMessage(isActiveFor(player));
		GBNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
	}
	
	@SubscribeEvent
	public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
		PlayerEntity player = event.getPlayer();
		if (!player.world.isRemote && player instanceof ServerPlayerEntity) {
			notifyPlayerActivity((ServerPlayerEntity) player);
		}
	}
}
