package com.lovetropics.gamemodebuild;

import com.lovetropics.gamemodebuild.message.GBNetwork;
import com.lovetropics.gamemodebuild.message.SetActiveMessage;
import com.lovetropics.gamemodebuild.state.GBClientState;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@EventBusSubscriber(modid = GamemodeBuild.MODID, bus = Bus.FORGE, value = Dist.CLIENT)
public class GBKeyBindings {
	public static final KeyMapping SWITCH_MODE = new KeyMapping("Enable/Disable Build Mode", InputConstants.KEY_B, "Build Mode");

	@SubscribeEvent
	public static void onKeyInput(ClientTickEvent event) {
		if (event.phase == Phase.END && SWITCH_MODE.consumeClick()) {
			LocalPlayer player = Minecraft.getInstance().player;
			if (player != null) {
				// don't set local state: await confirmation from the server
				boolean active = !GBClientState.isActive();
				GBNetwork.CHANNEL.sendToServer(new SetActiveMessage(active));
			}
		}
	}
}
