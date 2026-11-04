package com.lovetropics.gamemodebuild.client;

import net.minecraft.client.Minecraft;

public final class GBClientToast {
	public static void showBuildModeToast(boolean enabled) {
		Minecraft.getInstance().gui.toastManager().addToast(BuildModeToast.create(enabled));
	}
}
