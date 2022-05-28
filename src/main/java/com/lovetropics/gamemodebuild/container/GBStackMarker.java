package com.lovetropics.gamemodebuild.container;

import com.lovetropics.gamemodebuild.GamemodeBuild;

import net.minecraft.world.item.ItemStack;

public final class GBStackMarker {
	public static void mark(ItemStack stack) {
		stack.getOrCreateTagElement(GamemodeBuild.MODID);
	}
	
	public static boolean isMarked(ItemStack stack) {
		return stack.getTagElement(GamemodeBuild.MODID) != null;
	}
}
