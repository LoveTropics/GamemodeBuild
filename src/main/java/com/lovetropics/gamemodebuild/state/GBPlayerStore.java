package com.lovetropics.gamemodebuild.state;

import java.util.ArrayList;
import java.util.List;

import com.lovetropics.gamemodebuild.GamemodeBuild;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraftforge.common.util.Constants;

public final class GBPlayerStore {
	private static final String KEY_ACTIVE = "active";
	private static final String KEY_ENABLED = "enabled";
	private static final String KEY_PLAYER_INVENTORY = "playerinv";
	private static final String KEY_SP_INVENTORY = "buildinv";
	private static final String KEY_LIST = "list";

	public static void setEnabled(Player player, boolean enabled) {
		CompoundTag survivalPlus = getOrCreatePersistent(player, GamemodeBuild.MODID);
		survivalPlus.putBoolean(KEY_ENABLED, enabled);
	}

	public static boolean isEnabled(Player player) {
		CompoundTag survivalPlus = getOrCreatePersistent(player, GamemodeBuild.MODID);
		return !survivalPlus.contains(KEY_ENABLED) || survivalPlus.getBoolean(KEY_ENABLED);
	}

	public static void setActive(Player player, boolean active) {
		CompoundTag survivalPlus = getOrCreatePersistent(player, GamemodeBuild.MODID);
		survivalPlus.putBoolean(KEY_ACTIVE, active);
	}

	public static boolean isActive(Player player) {
		CompoundTag survivalPlus = getOrCreatePersistent(player, GamemodeBuild.MODID);
		return survivalPlus.getBoolean(KEY_ACTIVE);
	}

	public static void setList(Player player, String list) {
		CompoundTag survivalPlus = getOrCreatePersistent(player, GamemodeBuild.MODID);
		survivalPlus.putString(KEY_LIST, list);
	}

	public static String getList(Player player) {
		CompoundTag survivalPlus = getOrCreatePersistent(player, GamemodeBuild.MODID);
		return survivalPlus.contains(KEY_LIST) ? survivalPlus.getString(KEY_LIST) : "default";
	}

	private static void switchInventories(Player player, String from, String to) {
		CompoundTag survivalPlus = getOrCreatePersistent(player, GamemodeBuild.MODID);
		ListTag list = new ListTag();
		player.inventory.save(list);
		survivalPlus.put(from, list);
		List<ItemStack> armor = new ArrayList<>(player.inventory.armor);
		player.inventory.clearContent();

		player.inventory.load(survivalPlus.getList(to, Constants.NBT.TAG_COMPOUND));
		for (int i = 0; i < armor.size(); i++) {
			player.inventory.armor.set(i, armor.get(i));
		}
	}

	public static void switchToSPInventory(Player player) {
		switchInventories(player, KEY_PLAYER_INVENTORY, KEY_SP_INVENTORY);
	}

	public static void switchToPlayerInventory(Player player) {
		switchInventories(player, KEY_SP_INVENTORY, KEY_PLAYER_INVENTORY);
	}

	private static CompoundTag getOrCreatePersistent(Player player, String key) {
		CompoundTag nbt = player.getPersistentData();
		CompoundTag persisted = getOrCreateCompound(nbt, Player.PERSISTED_NBT_TAG);
		return getOrCreateCompound(persisted, key);
	}

	private static CompoundTag getOrCreateCompound(CompoundTag root, String key) {
		if (root.contains(key, Constants.NBT.TAG_COMPOUND)) {
			return root.getCompound(key);
		}

		CompoundTag compound = new CompoundTag();
		root.put(key, compound);
		return compound;
	}
}
