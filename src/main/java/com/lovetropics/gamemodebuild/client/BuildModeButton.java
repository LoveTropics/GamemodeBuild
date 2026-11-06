package com.lovetropics.gamemodebuild.client;

import com.lovetropics.gamemodebuild.GamemodeBuild;
import com.lovetropics.gamemodebuild.message.SetActiveMessage;
import com.lovetropics.gamemodebuild.state.GBClientState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jspecify.annotations.Nullable;

public final class BuildModeButton extends Button {
    private static final Identifier UNPRESSED = GamemodeBuild.id("widget/build_mode_unpressed");
    private static final Identifier PRESSED = GamemodeBuild.id("widget/build_mode_pressed");
    private boolean selected;
    private @Nullable Boolean requestedActive;

    public BuildModeButton(AbstractContainerScreen<?> screen) {
        super(Button.builder(Component.translatable("gui.gamemodebuild.build_mode"), button -> {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null) {
                // Don't set local state: await confirmation from the server.
                boolean active = !GBClientState.isActive();
                ((BuildModeButton) button).requestedActive = active;
                ClientPacketDistributor.sendToServer(new SetActiveMessage(active));
            }
        })
                .size(80, 20));
        update(screen);
        setTooltip(Tooltip.create(stateDescription()));
    }

    public boolean shouldKeepInventoryOpen() {
        return requestedActive != null && requestedActive == GBClientState.isActive();
    }

    public void update(AbstractContainerScreen<?> screen) {
        // Follow the inventory when opening the recipe book shifts it horizontally.
        setPosition(screen.getLeftPos() + screen.getImageWidth() - getWidth(), screen.getTopPos() - getHeight() - 4);
        if (selected != GBClientState.isActive()) {
            selected = GBClientState.isActive();
            setTooltip(Tooltip.create(stateDescription()));
        }
    }

    private MutableComponent stateDescription() {
        return Component.translatable(selected ? "gui.gamemodebuild.build_mode.on" : "gui.gamemodebuild.build_mode.off");
    }

    @Override
    protected MutableComponent createNarrationMessage() {
        return Component.translatable("gui.narrate.button", stateDescription());
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, selected ? PRESSED : UNPRESSED,
                getX(), getY(), getWidth(), getHeight(), alpha);
        if (isHoveredOrFocused()) {
            graphics.outline(getX(), getY(), getWidth(), getHeight(), 0xFFFFFFFF);
        }
        extractDefaultLabel(graphics.textRendererForWidget(this, GuiGraphicsExtractor.HoveredTextEffects.NONE));
    }
}
