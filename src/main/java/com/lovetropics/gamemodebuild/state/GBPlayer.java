package com.lovetropics.gamemodebuild.state;

import com.lovetropics.gamemodebuild.GBConfigs;
import com.lovetropics.gamemodebuild.GamemodeBuild;
import com.lovetropics.gamemodebuild.container.GBStackMarker;
import com.lovetropics.gamemodebuild.message.BuildModeNotificationMessage;
import com.lovetropics.gamemodebuild.message.SetActiveMessage;
import com.lovetropics.gamemodebuild.mixin.InventoryAccessor;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.entity.EntityEquipment;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

public class GBPlayer {

    private boolean active;
    private boolean enabled;
    private boolean effectiveActive;
    private boolean effectiveEnabled;

    private String list;
    private final List<ItemStackWithSlot> playerInventory;
    private final List<ItemStackWithSlot> buildInventory;

    public static final MapCodec<GBPlayer> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.BOOL.fieldOf("active").forGetter(gbData -> gbData.active),
            Codec.BOOL.fieldOf("enabled").forGetter(gbData -> gbData.enabled),
            Codec.BOOL.fieldOf("effective_active").forGetter(gbData -> gbData.effectiveActive),
            Codec.BOOL.fieldOf("effective_enabled").forGetter(gbData -> gbData.effectiveEnabled),
            Codec.STRING.fieldOf("list").forGetter(gbData -> gbData.list),
            ItemStackWithSlot.CODEC.listOf().fieldOf("playerinv").forGetter(gbData -> gbData.playerInventory),
            ItemStackWithSlot.CODEC.listOf().fieldOf("buildinv").forGetter(gbPlayer -> gbPlayer.buildInventory)
    ).apply(instance, GBPlayer::new));

    public GBPlayer() {
        active = false;
        enabled = GBConfigs.SERVER.playerDefaultEnabled();
        list = "default";
        playerInventory = new ArrayList<>();
        buildInventory = new ArrayList<>();
    }

    public GBPlayer(boolean active, boolean enabled, boolean effectiveActive, boolean effectiveEnabled, String list, List<ItemStackWithSlot> playerInventory, List<ItemStackWithSlot> buildInventory) {
        this.active = active;
        this.enabled = enabled;
        this.effectiveActive = effectiveActive;
        this.effectiveEnabled = effectiveEnabled;
        this.list = list;
        this.playerInventory = new ArrayList<>(playerInventory);
        this.buildInventory = new ArrayList<>(buildInventory);
    }

    public static GBPlayer get(Player player) {
        return player.getData(GamemodeBuild.PLAYER_ATTACHMENT.value());
    }

    public void setEnabled(ServerPlayer player, boolean enabled) {
        this.enabled = enabled;
        refreshState(player, false);
    }

    public void setActive(ServerPlayer player, boolean active) {
        this.active = active;
        refreshState(player, false);
    }

    public void setList(String list) {
        this.list = list;
    }

    public String getList() {
        return list;
    }

    public boolean isEffectivelyEnabled() {
        return effectiveEnabled;
    }

    public boolean isEffectivelyActive() {
        return effectiveActive;
    }

    public void refreshState(ServerPlayer player, boolean forceSend) {
        boolean lastEnabled = effectiveEnabled;
        boolean lastActive = effectiveActive;
        effectiveEnabled = GBServerState.isGloballyEnabled(player.level()) && enabled;
        effectiveActive = effectiveEnabled && active;

        if (effectiveEnabled != lastEnabled) {
            onAvailabilityChanged(player, effectiveEnabled);
        }
        if (effectiveActive != lastActive) {
            onActivityChanged(player, effectiveActive);
            PacketDistributor.sendToPlayer(player, new SetActiveMessage(effectiveActive));
        } else if (forceSend) {
            PacketDistributor.sendToPlayer(player, new SetActiveMessage(effectiveActive));
        }
    }

    private void onActivityChanged(ServerPlayer player, boolean active) {
        switchToInventory(player, active);
        if (active) {
            player.sendSystemMessage(Component.literal(GamemodeBuild.NAME + " activated"), true);
        } else {
            player.sendSystemMessage(Component.literal(GamemodeBuild.NAME + " deactivated"), true);
        }
        player.closeContainer();
    }

    private void onAvailabilityChanged(ServerPlayer player, boolean enabled) {
        PacketDistributor.sendToPlayer(player, new BuildModeNotificationMessage(enabled));
    }

    public void switchToInventory(Player player, boolean buildMode) {
        if (buildMode) {
            switchInventories(player, playerInventory, buildInventory);
        } else {
            switchInventories(player, buildInventory, playerInventory);
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
}
