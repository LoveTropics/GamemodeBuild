package com.lovetropics.gamemodebuild.state;

import java.util.ArrayList;
import java.util.List;

import com.lovetropics.gamemodebuild.GamemodeBuild;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraftforge.common.util.Constants;

public final class GBPlayerStore {
	private static final String KEY_ACTIVE = "active";
	private static final String KEY_ENABLED = "enabled";
	private static final String KEY_PLAYER_INVENTORY = "playerinv";
	private static final String KEY_SP_INVENTORY = "buildinv";

	public static void setEnabled(PlayerEntity player, boolean enabled) {
		CompoundNBT survivalPlus = getOrCreatePersistent(player, GamemodeBuild.MODID);
		survivalPlus.putBoolean(KEY_ENABLED, enabled);
	}

	public static boolean isEnabled(PlayerEntity player) {
		CompoundNBT survivalPlus = getOrCreatePersistent(player, GamemodeBuild.MODID);
		return !survivalPlus.contains(KEY_ENABLED) || survivalPlus.getBoolean(KEY_ENABLED);
	}

	public static void setActive(PlayerEntity player, boolean active) {
		CompoundNBT survivalPlus = getOrCreatePersistent(player, GamemodeBuild.MODID);
		survivalPlus.putBoolean(KEY_ACTIVE, active);
	}

	public static boolean isActive(PlayerEntity player) {
		CompoundNBT survivalPlus = getOrCreatePersistent(player, GamemodeBuild.MODID);
		return survivalPlus.getBoolean(KEY_ACTIVE);
	}

	private static void switchInventories(PlayerEntity player, String from, String to) {
		CompoundNBT survivalPlus = getOrCreatePersistent(player, GamemodeBuild.MODID);
		ListNBT list = new ListNBT();
		player.inventory.write(list);
		survivalPlus.put(from, list);
		List<ItemStack> armor = new ArrayList<>(player.inventory.armorInventory);
		player.inventory.clear();

		player.inventory.read(survivalPlus.getList(to, Constants.NBT.TAG_COMPOUND));
		for (int i = 0; i < armor.size(); i++) {
			player.inventory.armorInventory.set(i, armor.get(i));
		}
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
