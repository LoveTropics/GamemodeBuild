package com.lovetropics.survivalplus;

import com.lovetropics.survivalplus.message.SetSPEnabledMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = SurvivalPlus.MODID, bus = Bus.FORGE, value = Dist.CLIENT)
public class SPKeyBindings {
	
	public static final KeyBinding SWITCH_MODE = new KeyBinding("Enable/Disable Survival+ Mode", GLFW.GLFW_KEY_P, "Survival+");
	
	@SubscribeEvent
	public static void onKeyInput(ClientTickEvent event) {
		if (event.phase == Phase.END && SWITCH_MODE.isPressed()) {
			ClientPlayerEntity player = Minecraft.getInstance().player;
			if (player != null) {
				boolean enabled = !SPPlayerState.isEnabled(player);
				
				SurvivalPlus.NETWORK.sendToServer(new SetSPEnabledMessage(enabled));
				SPPlayerState.setEnabled(player, enabled);
			}
		}
	}
	
	public static void register() {
	}
}
