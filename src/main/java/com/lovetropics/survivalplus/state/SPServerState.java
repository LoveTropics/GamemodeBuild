package com.lovetropics.survivalplus.state;

import com.lovetropics.survivalplus.SPConfigs;
import com.lovetropics.survivalplus.SurvivalPlus;
import com.lovetropics.survivalplus.message.SetSPActiveMessage;
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
		SPPlayerStore.setActive(player, active);
		notifyPlayerActivity(player);
	}
	
	public static boolean isActiveFor(ServerPlayerEntity player) {
		return SPPlayerStore.isActive(player);
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
