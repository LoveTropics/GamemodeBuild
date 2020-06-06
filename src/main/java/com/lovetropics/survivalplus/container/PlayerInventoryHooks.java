package com.lovetropics.survivalplus.container;

import com.lovetropics.survivalplus.SurvivalPlus;
import com.lovetropics.survivalplus.message.OpenSPInventoryMessage;
import com.lovetropics.survivalplus.state.SPClientState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.gui.screen.inventory.InventoryScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@EventBusSubscriber(modid = SurvivalPlus.MODID, bus = Bus.FORGE, value = Dist.CLIENT)
public final class PlayerInventoryHooks {
	@SubscribeEvent
	public static void onOpenScreen(GuiOpenEvent event) {
		ClientPlayerEntity player = Minecraft.getInstance().player;
		if (player == null) return;
		
		if (!SPClientState.isActive()) {
			return;
		}
		
		if (event.getGui() instanceof InventoryScreen) {
			SurvivalPlus.NETWORK.sendToServer(new OpenSPInventoryMessage());
			
			SurvivalPlusContainer container = new SurvivalPlusContainer(0, player.inventory);
			event.setGui(new SurvivalPlusScreen(container, player.inventory, SurvivalPlusContainer.title()));
		}
	}
	
	@SubscribeEvent
	public static void onToss(ItemTossEvent event) {
		if (SPStackMarker.isMarked(event.getEntityItem().getItem())) {
			event.setCanceled(true);
		}
	}
}
