package com.lovetropics.gamemodebuild.state;

import com.lovetropics.gamemodebuild.GBConfigs;
import com.lovetropics.gamemodebuild.GamemodeBuild;
import com.lovetropics.gamemodebuild.message.GBNetwork;
import com.lovetropics.gamemodebuild.message.SetActiveMessage;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.network.PacketDistributor;

@Mod.EventBusSubscriber(modid = GamemodeBuild.MODID)
public final class GBServerState {
	
	public enum NotificationType {
		
		INITIAL,
		ACTIVE,
		ENABLED,
		;
	}
	
	public static void setGloballyEnabled(MinecraftServer server, boolean enabled) {
		GBConfigs.SERVER.enable(enabled);
		
		for (ServerPlayerEntity player : server.getPlayerList().getPlayers()) {
			notifyPlayerActivity(player, NotificationType.ENABLED);
		}
	}
	
	public static void setEnabledFor(ServerPlayerEntity player, boolean enabled) {
		GBPlayerStore.setEnabled(player, enabled);
		notifyPlayerActivity(player, NotificationType.ENABLED);
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
			notifyPlayerActivity(player, NotificationType.ACTIVE);
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
	
	public static void notifyPlayerActivity(ServerPlayerEntity player, NotificationType type) {
		boolean state = isActiveFor(player);
		if (type != NotificationType.INITIAL) {
			if (!GBServerState.isEnabledFor(player) && type == NotificationType.ACTIVE) {
				player.sendStatusMessage(new StringTextComponent(GamemodeBuild.NAME + " is disabled!"), true);
			} else {
				GBServerState.switchInventories(player, state);
				if (state) {
					player.sendStatusMessage(new StringTextComponent(GamemodeBuild.NAME + " activated"), true);
				} else {
	//				// Clear marked stacks from inventory
	//				for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
	//					if (SPStackMarker.isMarked(player.inventory.getStackInSlot(i))) {
	//						player.inventory.removeStackFromSlot(i);
	//					}
	//				}
					player.sendStatusMessage(new StringTextComponent(GamemodeBuild.NAME + " deactivated"), true);
				}
			}
		}
		SetActiveMessage message = new SetActiveMessage(state);
		GBNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
	}
	
	@SubscribeEvent
	public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
		PlayerEntity player = event.getPlayer();
		if (!player.world.isRemote && player instanceof ServerPlayerEntity) {
			notifyPlayerActivity((ServerPlayerEntity) player, NotificationType.INITIAL);
		}
	}
}
