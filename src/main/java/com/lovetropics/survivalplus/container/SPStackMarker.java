package com.lovetropics.survivalplus.container;

import com.lovetropics.survivalplus.SurvivalPlus;
import net.minecraft.item.ItemStack;

public final class SPStackMarker {
	public static void mark(ItemStack stack) {
		stack.getOrCreateChildTag(SurvivalPlus.MODID);
	}
	
	public static boolean isMarked(ItemStack stack) {
		return stack.getChildTag(SurvivalPlus.MODID) != null;
	}
}
