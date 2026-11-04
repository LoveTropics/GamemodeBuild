package com.lovetropics.gamemodebuild.client;

import com.lovetropics.gamemodebuild.GBKeyBindings;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

public final class BuildModeToast implements Toast {
	private static final ResourceLocation BACKGROUND_SPRITE = ResourceLocation.withDefaultNamespace("toast/advancement");
	public static final int DISPLAY_TIME = 5000;

	private final Component title;
	private final Component header;
	private final ItemStack icon;
	private Toast.Visibility wantedVisibility = Toast.Visibility.HIDE;

	private BuildModeToast(boolean enabled) {
		this.header = Component.translatable("gamemodebuild.toast.build_mode");
		this.title = enabled
				? Component.translatable("gamemodebuild.toast.build_mode.available", GBKeyBindings.SWITCH_MODE.getTranslatedKeyMessage())
				: Component.translatable("gamemodebuild.toast.build_mode.unavailable");
		this.icon = new ItemStack(enabled ? Items.GRASS_BLOCK : Items.BARRIER);
	}

	public static BuildModeToast create(boolean enabled) {
		return new BuildModeToast(enabled);
	}

	@Override
	public Toast.Visibility getWantedVisibility() {
		return this.wantedVisibility;
	}

	@Override
	public void update(ToastManager toastManager, long time) {
		this.wantedVisibility = time >= DISPLAY_TIME * toastManager.getNotificationDisplayTimeMultiplier()
				? Toast.Visibility.HIDE
				: Toast.Visibility.SHOW;
	}

    @Override
	public void render(GuiGraphics graphics, Font font, long time) {
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND_SPRITE, 0, 0, this.width(), this.height());
		List<FormattedCharSequence> lines = font.split(this.title, 125);
		int headerColor = -256;
		if (lines.size() == 1) {
			graphics.drawString(font, this.header, 30, 7, headerColor, false);
			graphics.drawString(font, lines.get(0), 30, 18, -1, false);
		} else if (time < 1500L) {
			int alpha = Mth.floor(Mth.clamp((float) (1500L - time) / 300.0F, 0.0F, 1.0F) * 255.0F);
			graphics.drawString(font, this.header, 30, 11, ARGB.color(alpha, headerColor), false);
		} else {
			int alpha = Mth.floor(Mth.clamp((float) (time - 1500L) / 300.0F, 0.0F, 1.0F) * 252.0F);
			int y = this.height() / 2 - lines.size() * 9 / 2;
			for (FormattedCharSequence line : lines) {
				graphics.drawString(font, line, 30, y, ARGB.color(alpha, -1), false);
				y += 9;
			}
		}
		graphics.renderFakeItem(this.icon, 8, 8);
	}
}
