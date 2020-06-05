package com.lovetropics.survivalplus;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.inventory.container.PlayerContainer;
import net.minecraft.inventory.container.SimpleNamedContainerProvider;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@EventBusSubscriber(modid = SurvivalPlus.MODID, bus = Bus.FORGE)
public class PlayerContainerHooks {
	
	@SubscribeEvent
	public static void onPlayerInteract(PlayerInteractEvent.RightClickBlock event) {
		if (event.getPlayer().world.isRemote) return;
		BlockState block = event.getPlayer().world.getBlockState(event.getPos());
		if (block.getBlock() == Blocks.GRASS_BLOCK) {
			event.getPlayer().openContainer(new SimpleNamedContainerProvider(SurvivalPlusContainer::new, new StringTextComponent("Test")));
		}
	}
	
	@SubscribeEvent // TODO use screen open event and send a packet to server to replace container there
	// Show a blank version of the UI predictively on the client, to be replaced by the incoming packet from the server
	// after the container is replaced
	public static void onOpenContainer(PlayerContainerEvent.Open event) {
		if (event.getContainer() instanceof PlayerContainer) {
			event.getPlayer().openContainer(new SimpleNamedContainerProvider(SurvivalPlusContainer::new, new StringTextComponent("Test")));
		}
	}
}
