package com.lovetropics.gamemodebuild;

import com.lovetropics.gamemodebuild.message.SetActiveMessage;
import com.lovetropics.gamemodebuild.state.GBClientState;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

@EventBusSubscriber(value = Dist.CLIENT)
public class GBKeyBindings {
	public static final KeyMapping.Category BUILD_MODE = new KeyMapping.Category(GamemodeBuild.rl("build_mode"));
	public static final KeyMapping SWITCH_MODE = new KeyMapping("Enable/Disable Build Mode", InputConstants.KEY_B, BUILD_MODE);

	@SubscribeEvent
	public static void onKeyInput(ClientTickEvent.Post event) {
		if (SWITCH_MODE.consumeClick()) {
			LocalPlayer player = Minecraft.getInstance().player;
			if (player != null) {
				// don't set local state: await confirmation from the server
				boolean active = !GBClientState.isActive();
				ClientPacketDistributor.sendToServer(new SetActiveMessage(active));
			}
		}
	}

	@SubscribeEvent
	public static void onRegisterEvent(RegisterKeyMappingsEvent event) {
		event.registerCategory(BUILD_MODE);
		event.register(SWITCH_MODE);
	}
}
