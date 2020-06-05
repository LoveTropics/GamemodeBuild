package com.lovetropics.survivalplus;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.common.util.Constants;

public final class SPPlayerState {
	public static void setEnabled(PlayerEntity player, boolean enabled) {
		CompoundNBT survivalPlus = getOrCreatePersistent(player, SurvivalPlus.MODID);
		survivalPlus.putBoolean("enabled", enabled);
	}
	
	public static boolean isEnabled(PlayerEntity player) {
		CompoundNBT survivalPlus = getOrCreatePersistent(player, SurvivalPlus.MODID);
		return survivalPlus.getBoolean("enabled");
	}
	
	private static CompoundNBT getOrCreatePersistent(PlayerEntity player, String key) {
		CompoundNBT nbt = player.getPersistentData();
		CompoundNBT persisted = getOrCreateCompound(nbt, PlayerEntity.PERSISTED_NBT_TAG);
		return getOrCreateCompound(persisted, key);
	}
	
	private static CompoundNBT getOrCreateCompound(CompoundNBT root, String key) {
		if (root.contains(key, Constants.NBT.TAG_COMPOUND)) {
			return root.getCompound(key);
		}
		
		CompoundNBT compound = new CompoundNBT();
		root.put(key, compound);
		return compound;
	}
}
