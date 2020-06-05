package com.lovetropics.survivalplus;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.gui.screen.inventory.InventoryScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@EventBusSubscriber(modid = SurvivalPlus.MODID, bus = Bus.FORGE, value = Dist.CLIENT)
public final class PlayerInventoryHooks {
	@SubscribeEvent
	public static void onOpenScreen(GuiOpenEvent event) {
		ClientPlayerEntity player = Minecraft.getInstance().player;
		if (player == null) return;
		
		if (!SPPlayerState.isEnabled(player)) {
			return;
		}
		
		if (event.getGui() instanceof InventoryScreen) {
			SurvivalPlus.NETWORK.sendToServer(new OpenSPInventoryMessage());
			
			SurvivalPlusContainer container = new SurvivalPlusContainer(0, player.inventory);
			event.setGui(new SurvivalPlusScreen(container, player.inventory, SurvivalPlusContainer.title()));
		}
	}
}
