package com.lovetropics.gamemodebuild.state;

import com.lovetropics.gamemodebuild.GBConfigs;
import com.lovetropics.gamemodebuild.GamemodeBuild;
import com.lovetropics.gamemodebuild.message.GBNetwork;
import com.lovetropics.gamemodebuild.message.SetActiveMessage;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

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
		
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			notifyPlayerActivity(false, player, NotificationType.ENABLED);
		}
	}
	
	public static void setEnabledFor(ServerPlayer player, boolean enabled) {
		boolean wasActive = isActiveFor(player);
		GBPlayerStore.setEnabled(player, enabled);
		notifyPlayerActivity(wasActive, player, NotificationType.ENABLED);
	}
	
	public static boolean isGloballyEnabled() {
		return GBConfigs.SERVER.enabled();
	}
	
	public static boolean isEnabledFor(ServerPlayer player) {
		if (!isGloballyEnabled()) {
			return false;
		}
		return GBPlayerStore.isEnabled(player);
	}
	
	public static void setActiveFor(ServerPlayer player, boolean active) {
		if (isEnabledFor(player) || !active) {
			boolean wasActive = isActiveFor(player);
			GBPlayerStore.setActive(player, active);
			notifyPlayerActivity(wasActive, player, NotificationType.ACTIVE);
		}
	}
	
	public static boolean isActiveFor(ServerPlayer player) {
		return isEnabledFor(player) && GBPlayerStore.isActive(player);
	}
	
	public static void switchInventories(ServerPlayer player, boolean state) {
		if (state) {
			GBPlayerStore.switchToSPInventory(player);
		} else {
			GBPlayerStore.switchToPlayerInventory(player);
		}
	}
	
	public static void notifyPlayerActivity(boolean prevState, ServerPlayer player, NotificationType type) {
		boolean state = isActiveFor(player);
		if (type != NotificationType.INITIAL) {
			if (!GBServerState.isEnabledFor(player) && type == NotificationType.ACTIVE) {
				player.displayClientMessage(new TextComponent(GamemodeBuild.NAME + " is disabled!"), true);
			} else if (prevState != state) {
				GBServerState.switchInventories(player, state);
				if (state) {
					player.displayClientMessage(new TextComponent(GamemodeBuild.NAME + " activated"), true);
				} else {
	//				// Clear marked stacks from inventory
	//				for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
	//					if (SPStackMarker.isMarked(player.inventory.getStackInSlot(i))) {
	//						player.inventory.removeStackFromSlot(i);
	//					}
	//				}
					player.displayClientMessage(new TextComponent(GamemodeBuild.NAME + " deactivated"), true);
				}
			}
		}
		SetActiveMessage message = new SetActiveMessage(state);
		GBNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
	}
	
	@SubscribeEvent
	public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
		Player player = event.getPlayer();
		if (!player.level.isClientSide && player instanceof ServerPlayer) {
			// Previous state doesn't matter here
			notifyPlayerActivity(false, (ServerPlayer) player, NotificationType.INITIAL);
		}
	}
}
