package com.lovetropics.gamemodebuild.state;

import com.lovetropics.gamemodebuild.GBConfigs;
import com.lovetropics.gamemodebuild.GamemodeBuild;
import com.lovetropics.gamemodebuild.container.GBStackMarker;
import com.lovetropics.gamemodebuild.mixin.InventoryAccessor;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.NonNullList;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.entity.EntityEquipment;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class GBPlayerStore {

	public static void setEnabled(Player player, boolean enabled) {
		getOrCreateData(player).enabled = enabled;
	}

	public static boolean isEnabled(Player player) {
		return getOrCreateData(player).enabled;
	}

	public static void setActive(Player player, boolean active) {
		getOrCreateData(player).active = active;
	}

	public static boolean isActive(Player player) {
		return getOrCreateData(player).active;
	}

	public static void setList(Player player, String list) {
		getOrCreateData(player).list = list;
	}

	public static String getList(Player player) {
		return getOrCreateData(player).list;
	}

	public static void switchToInventory(Player player, boolean buildMode) {
		GBPlayerAttachment attachment = getOrCreateData(player);
		if (buildMode) {
			switchInventories(player, attachment.playerInventory, attachment.buildInventory);
		} else {
			switchInventories(player, attachment.buildInventory, attachment.playerInventory);
		}
	}

	private static void switchInventories(Player player, List<ItemStackWithSlot> from, List<ItemStackWithSlot> to) {
		saveInventory(player.getInventory(), from);
		loadInventory(player.getInventory(), to);
	}

	public static void saveInventory(Inventory inventory, List<ItemStackWithSlot> items) {
		NonNullList<ItemStack> nonEquipmentItems = inventory.getNonEquipmentItems();
		List<ItemStackWithSlot> nonEquipmentItemsWithSlot = new ArrayList<>();
		for (int i = 0; i < nonEquipmentItems.size(); i++) {
			ItemStack nonEquipmentItem = nonEquipmentItems.get(i);
			if (!nonEquipmentItem.isEmpty()) {
				nonEquipmentItemsWithSlot.add(new ItemStackWithSlot(i, nonEquipmentItem.copy()));
			}
		}
		items.clear();
		items.addAll(nonEquipmentItemsWithSlot);
	}

	private static void loadInventory(Inventory inventory, List<ItemStackWithSlot> items) {
		inventory.getNonEquipmentItems().clear();

		// Keeping equipment items is fine, but don't allow them to keep them if they are from build mode.
		EntityEquipment equipment = ((InventoryAccessor) inventory).getEquipment();
		for (EquipmentSlot slot : Inventory.EQUIPMENT_SLOT_MAPPING.values()) {
			ItemStack itemStack = equipment.get(slot);
			if (GBStackMarker.isMarked(itemStack)) {
				equipment.set(slot, ItemStack.EMPTY);
			}
		}

		for (ItemStackWithSlot item : items) {
            if (!item.stack().isEmpty()) {
                inventory.setItem(item.slot(), item.stack().copy());
            }
        }
	}

	private static GBPlayerAttachment getOrCreateData(Player player) {
		if (player.hasData(GamemodeBuild.PLAYER_ATTACHMENT.value())) {
			return player.getData(GamemodeBuild.PLAYER_ATTACHMENT.value());
		} else {
			GBPlayerAttachment attachment = new GBPlayerAttachment();
			player.setData(GamemodeBuild.PLAYER_ATTACHMENT.value(), attachment);
			return attachment;
		}
	}

	public static class GBPlayerAttachment {

		private boolean active;
		private boolean enabled;
		private String list;
		private final List<ItemStackWithSlot> playerInventory;
		private final List<ItemStackWithSlot> buildInventory;

		public static final MapCodec<GBPlayerAttachment> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
				Codec.BOOL.fieldOf("active").forGetter(gbData -> gbData.active),
				Codec.BOOL.fieldOf("enabled").forGetter(gbData -> gbData.enabled),
				Codec.STRING.fieldOf("list").forGetter(gbData -> gbData.list),
				ItemStackWithSlot.CODEC.listOf().fieldOf("playerinv").forGetter(gbData -> gbData.playerInventory),
				ItemStackWithSlot.CODEC.listOf().fieldOf("buildinv").forGetter(gbPlayerAttachment -> gbPlayerAttachment.buildInventory)
		).apply(instance, GBPlayerAttachment::new));

		public GBPlayerAttachment() {
			this.active = false;
			this.enabled = GBConfigs.SERVER.playerDefaultEnabled();
			this.list = "default";
			this.playerInventory = new ArrayList<>();
			this.buildInventory = new ArrayList<>();
		}

		public GBPlayerAttachment(boolean active, boolean enabled, String list, List<ItemStackWithSlot> playerInventory, List<ItemStackWithSlot> buildInventory) {
			this.active = active;
			this.enabled = enabled;
			this.list = list;
			this.playerInventory = new ArrayList<>(playerInventory);
			this.buildInventory = new ArrayList<>(buildInventory);
		}
	}
}
