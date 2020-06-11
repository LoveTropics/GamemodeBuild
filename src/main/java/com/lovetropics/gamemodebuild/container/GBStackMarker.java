package com.lovetropics.gamemodebuild.container;

import com.lovetropics.gamemodebuild.GamemodeBuild;

import net.minecraft.item.ItemStack;

public final class GBStackMarker {
	public static void mark(ItemStack stack) {
		stack.getOrCreateChildTag(GamemodeBuild.MODID);
	}
	
	public static boolean isMarked(ItemStack stack) {
		return stack.getChildTag(GamemodeBuild.MODID) != null;
	}
}
