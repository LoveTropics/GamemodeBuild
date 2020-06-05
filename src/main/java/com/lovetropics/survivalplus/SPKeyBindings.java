package com.lovetropics.survivalplus;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@EventBusSubscriber(modid = SurvivalPlus.MODID, bus = Bus.FORGE, value = Dist.CLIENT)
public class SPKeyBindings {
	
	public static final KeyBinding SWITCH_MODE = new KeyBinding("Enable/Disable Survival+ Mode", GLFW.GLFW_KEY_P, "Survival+");
	
	@SubscribeEvent
	public static void onKeyInput(ClientTickEvent event) {
		if (event.phase == Phase.END && SWITCH_MODE.isPressed()) {
			CompoundNBT nbt = Minecraft.getInstance().player.getPersistentData();
			CompoundNBT persisted = nbt.getCompound(PlayerEntity.PERSISTED_NBT_TAG);
			CompoundNBT subTag = persisted.getCompound(SurvivalPlus.MODID);
			subTag.putBoolean("enabled", !subTag.getBoolean("enabled"));
			nbt.put(SurvivalPlus.MODID, subTag);
			persisted.put(PlayerEntity.PERSISTED_NBT_TAG, persisted);
		}
	}
	
	public static void register() {}
}
