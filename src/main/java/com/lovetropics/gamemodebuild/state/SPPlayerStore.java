package com.lovetropics.gamemodebuild.state;

import com.lovetropics.gamemodebuild.SurvivalPlus;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraftforge.common.util.Constants;

public final class SPPlayerStore {
	private static final String KEY_ACTIVE = "active";
	private static final String KEY_ENABLED = "enabled";
	private static final String KEY_PLAYER_INVENTORY = "playerinv";
	private static final String KEY_SP_INVENTORY = "spitems";
	
	public static void setEnabled(PlayerEntity player, boolean enabled) {
		CompoundNBT survivalPlus = getOrCreatePersistent(player, SurvivalPlus.MODID);
		survivalPlus.putBoolean(KEY_ENABLED, enabled);
	}
	
	public static boolean isEnabled(PlayerEntity player) {
		CompoundNBT survivalPlus = getOrCreatePersistent(player, SurvivalPlus.MODID);
		return !survivalPlus.contains(KEY_ENABLED) || survivalPlus.getBoolean(KEY_ENABLED);
	}
	
	public static void setActive(PlayerEntity player, boolean active) {
		CompoundNBT survivalPlus = getOrCreatePersistent(player, SurvivalPlus.MODID);
		survivalPlus.putBoolean(KEY_ACTIVE, active);
	}
	
	public static boolean isActive(PlayerEntity player) {
		CompoundNBT survivalPlus = getOrCreatePersistent(player, SurvivalPlus.MODID);
		return survivalPlus.getBoolean(KEY_ACTIVE);
	}

	private static void switchInventories(PlayerEntity player, String from, String to) {
		CompoundNBT survivalPlus = getOrCreatePersistent(player, SurvivalPlus.MODID);
		ListNBT list = new ListNBT();
		player.inventory.write(list);
		survivalPlus.put(from, list);
		player.inventory.clear();

		player.inventory.read(survivalPlus.getList(to, Constants.NBT.TAG_COMPOUND));

	}

	public static void switchToSPInventory(PlayerEntity player) {
		switchInventories(player, KEY_PLAYER_INVENTORY, KEY_SP_INVENTORY);
	}

	public static void switchToPlayerInventory(PlayerEntity player) {
		switchInventories(player, KEY_SP_INVENTORY, KEY_PLAYER_INVENTORY);
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
