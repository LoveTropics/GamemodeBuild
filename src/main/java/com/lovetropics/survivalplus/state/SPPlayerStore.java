package com.lovetropics.survivalplus.state;

import com.lovetropics.survivalplus.SurvivalPlus;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.common.util.Constants;

public final class SPPlayerStore {
	private static final String KEY_ACTIVE = "active";
	private static final String KEY_ENABLED = "enabled";
	
	public static void setEnabled(PlayerEntity player, boolean enabled) {
		CompoundNBT survivalPlus = getOrCreatePersistent(player, SurvivalPlus.MODID);
		survivalPlus.putBoolean(KEY_ENABLED, enabled);
	}
	
	public static boolean isEnabled(PlayerEntity player) {
		CompoundNBT survivalPlus = getOrCreatePersistent(player, SurvivalPlus.MODID);
		return !survivalPlus.contains(KEY_ENABLED) || survivalPlus.getBoolean(KEY_ENABLED);
	}
	
	public static boolean setActive(PlayerEntity player, boolean active) {
		if (isEnabled(player) || !active) {
			CompoundNBT survivalPlus = getOrCreatePersistent(player, SurvivalPlus.MODID);
			survivalPlus.putBoolean(KEY_ACTIVE, active);
			return true;
		}
		return false;
	}
	
	public static boolean isActive(PlayerEntity player) {
		if (isEnabled(player)) {
			CompoundNBT survivalPlus = getOrCreatePersistent(player, SurvivalPlus.MODID);
			return survivalPlus.getBoolean(KEY_ACTIVE);
		}
		return false;
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
