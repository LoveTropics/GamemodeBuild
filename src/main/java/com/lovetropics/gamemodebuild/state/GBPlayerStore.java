package com.lovetropics.gamemodebuild.state;

import com.lovetropics.gamemodebuild.GBConfigs;
import com.lovetropics.gamemodebuild.GamemodeBuild;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class GBPlayerStore {
	private static final String KEY_ACTIVE = "active";
	private static final String KEY_ENABLED = "enabled";
	private static final String KEY_PLAYER_INVENTORY = "playerinv";
	private static final String KEY_BUILD_INVENTORY = "buildinv";
	private static final String KEY_LIST = "list";

	public static void setEnabled(Player player, boolean enabled) {
		CompoundTag survivalPlus = getOrCreatePersistent(player, GamemodeBuild.MODID);
		survivalPlus.putBoolean(KEY_ENABLED, enabled);
	}

	public static boolean isEnabled(Player player) {
		CompoundTag survivalPlus = getOrCreatePersistent(player, GamemodeBuild.MODID);
		if (!survivalPlus.contains(KEY_ENABLED)) {
			return GBConfigs.SERVER.playerDefaultEnabled();
		}
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

	public static void switchToInventory(Player player, boolean buildMode) {
		if (buildMode) {
			switchInventories(player, KEY_PLAYER_INVENTORY, KEY_BUILD_INVENTORY);
		} else {
			switchInventories(player, KEY_BUILD_INVENTORY, KEY_PLAYER_INVENTORY);
		}
	}

	private static void switchInventories(Player player, String from, String to) {
		ListTag currentInventory = new ListTag();
		player.getInventory().save(currentInventory);
		ListTag newInventory = swapInventoryTag(player, from, to, currentInventory);
		loadInventory(player.getInventory(), newInventory);
	}

	private static ListTag swapInventoryTag(Player player, String from, String to, ListTag inventory) {
		CompoundTag tag = getOrCreatePersistent(player, GamemodeBuild.MODID);
		ListTag newInventory = tag.getList(to, Tag.TAG_COMPOUND);
		tag.remove(to);
		tag.put(from, inventory);
		return newInventory;
	}

	private static void loadInventory(Inventory inventory, ListTag tag) {
		List<ItemStack> armor = List.copyOf(inventory.armor);
		inventory.clearContent();
		inventory.load(tag);
		for (int i = 0; i < armor.size(); i++) {
			inventory.armor.set(i, armor.get(i));
		}
	}

	private static CompoundTag getOrCreatePersistent(Player player, String key) {
		CompoundTag nbt = player.getPersistentData();
		CompoundTag persisted = getOrCreateCompound(nbt, Player.PERSISTED_NBT_TAG);
		return getOrCreateCompound(persisted, key);
	}

	private static CompoundTag getOrCreateCompound(CompoundTag root, String key) {
		if (root.contains(key, Tag.TAG_COMPOUND)) {
			return root.getCompound(key);
		}

		CompoundTag compound = new CompoundTag();
		root.put(key, compound);
		return compound;
	}
}
