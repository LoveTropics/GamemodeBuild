package com.lovetropics.gamemodebuild.container;

import com.lovetropics.gamemodebuild.GamemodeBuild;
import com.lovetropics.gamemodebuild.message.GBNetwork;
import com.lovetropics.gamemodebuild.message.OpenBuildInventoryMessage;
import com.lovetropics.gamemodebuild.state.GBClientState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenOpenEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@EventBusSubscriber(modid = GamemodeBuild.MODID, bus = Bus.FORGE, value = Dist.CLIENT)
public final class PlayerInventoryHooks {
	@SubscribeEvent
	public static void onOpenScreen(ScreenOpenEvent event) {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null) return;
		
		if (!GBClientState.isActive()) {
			return;
		}
		
		if (event.getScreen() instanceof InventoryScreen) {
			GBNetwork.CHANNEL.sendToServer(new OpenBuildInventoryMessage());

			final Inventory inventory = player.getInventory();
			BuildContainer container = new BuildContainer(0, inventory, player, null);
			event.setScreen(new BuildScreen(container, inventory, BuildContainer.title()));
		}
	}
	
	@SubscribeEvent
	public static void onToss(ItemTossEvent event) {
		if (GBStackMarker.isMarked(event.getEntityItem().getItem())) {
			event.setCanceled(true);
		}
	}
}
