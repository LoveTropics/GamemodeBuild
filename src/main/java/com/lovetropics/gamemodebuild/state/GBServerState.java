package com.lovetropics.gamemodebuild.state;

import com.lovetropics.gamemodebuild.GamemodeBuild;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.GameRuleChangedEvent;

@EventBusSubscriber
public final class GBServerState {
	@SubscribeEvent
	public static void onGameRuleChanged(GameRuleChangedEvent event) {
		if (event.getGameRule() == GamemodeBuild.ENABLE_BUILD_MODE.get()) {
			for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
				GBPlayer.get(player).refreshState(player, false);
			}
		}
	}

	public static boolean isGloballyEnabled(ServerLevel level) {
		return level.getGameRules().get(GamemodeBuild.ENABLE_BUILD_MODE.get());
	}

	public static void requestActive(ServerPlayer player, boolean active) {
		GBPlayer buildPlayer = GBPlayer.get(player);
		if (!buildPlayer.isEffectivelyEnabled()) {
			player.sendSystemMessage(Component.literal(GamemodeBuild.NAME + " is disabled!"), true);
			return;
		}
		buildPlayer.setActive(player, active);
	}

	@SubscribeEvent
	public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) {
			GBPlayer.get(player).refreshState(player, true);
		}
	}

	@SubscribeEvent
	public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
		// This isn't great. We're using this to detect players reloading from minigames, to ensure resetting state on the server does not cause a desync
		if (event.getEntity() instanceof ServerPlayer player) {
			GBPlayer.get(player).refreshState(player, true);
		}
	}
}
