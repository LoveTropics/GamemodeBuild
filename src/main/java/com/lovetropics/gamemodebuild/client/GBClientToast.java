package com.lovetropics.gamemodebuild.client;

import net.minecraft.client.Minecraft;

public final class GBClientToast {
	public static void showBuildModeToast(boolean enabled) {
		Minecraft.getInstance().getToastManager().addToast(BuildModeToast.create(enabled));
	}
}
