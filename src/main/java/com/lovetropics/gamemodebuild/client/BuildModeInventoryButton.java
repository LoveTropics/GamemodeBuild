package com.lovetropics.gamemodebuild.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

@EventBusSubscriber(Dist.CLIENT)
public final class BuildModeInventoryButton {
    private static boolean supports(Screen screen) {
        return screen instanceof InventoryScreen || screen instanceof BuildScreen;
    }

    @SubscribeEvent
    public static void onInit(ScreenEvent.Init.Post event) {
        if (supports(event.getScreen())) {
            event.addListener(new BuildModeButton((AbstractContainerScreen<?>) event.getScreen()));
        }
    }

    @SubscribeEvent
    public static void onCloseScreen(ScreenEvent.Closing event) {
        Screen currentScreen = event.getScreen();
        Minecraft minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (!supports(currentScreen) || player == null) {
            return;
        }

        for (var child : currentScreen.children()) {
            if (child instanceof BuildModeButton button && button.shouldKeepInventoryOpen()) {
                // Finish closing the old menu before opening the confirmed mode's inventory.
                // The existing inventory hook opens the build inventory when active.
                minecraft.schedule(() -> {
                    if (minecraft.player == player && player.isAlive() && minecraft.gui.screen() == null) {
                        minecraft.gui.setScreen(new InventoryScreen(player));
                    }
                });
                return;
            }
        }
    }

    @SubscribeEvent
    public static void onRender(ScreenEvent.Render.Pre event) {
        if (supports(event.getScreen())) {
            for (var child : event.getScreen().children()) {
                if (child instanceof BuildModeButton button) {
                    button.update((AbstractContainerScreen<?>) event.getScreen());
                }
            }
        }
    }
}
