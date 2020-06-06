package com.lovetropics.survivalplus;

import com.lovetropics.survivalplus.message.SetSPActiveMessage;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.network.PacketDistributor;

@Mod.EventBusSubscriber(modid = SurvivalPlus.MODID)
public final class SPPlayerState {
	
	private static final String KEY_ACTIVE = "active";
	private static final String KEY_ENABLED = "enabled";
	
	public static void setEnabled(PlayerEntity player, boolean enabled) {
		CompoundNBT survivalPlus = getOrCreatePersistent(player, SurvivalPlus.MODID);
		survivalPlus.putBoolean(KEY_ENABLED, enabled);
	}
	
	public static boolean isEnabled(PlayerEntity player) {
		if (!SPConfigs.SERVER.enabled()) {
			return false;
		}
		return isSpecificallyEnabled(player);
	}
	
	public static boolean isSpecificallyEnabled(PlayerEntity player) {
		CompoundNBT survivalPlus = getOrCreatePersistent(player, SurvivalPlus.MODID);
		return !survivalPlus.contains(KEY_ENABLED) || survivalPlus.getBoolean(KEY_ENABLED);
	}
	
	public static void setActive(PlayerEntity player, boolean active) {
		if (isEnabled(player) || !active) {
			CompoundNBT survivalPlus = getOrCreatePersistent(player, SurvivalPlus.MODID);
			survivalPlus.putBoolean(KEY_ACTIVE, active);
		}
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
	
	@SubscribeEvent
	public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
		PlayerEntity player = event.getPlayer();
		if (!player.world.isRemote && player instanceof ServerPlayerEntity) {
			SetSPActiveMessage message = new SetSPActiveMessage(isActive(player));
			SurvivalPlus.NETWORK.send(PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity) player), message);
		}
	}
}
