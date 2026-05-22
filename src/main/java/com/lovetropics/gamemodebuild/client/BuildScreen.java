package com.lovetropics.gamemodebuild.client;

import com.lovetropics.gamemodebuild.GamemodeBuild;
import com.lovetropics.gamemodebuild.container.BuildContainer;
import com.lovetropics.gamemodebuild.message.UpdateFilterMessage;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.CommonColors;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.BitSet;
import java.util.Locale;

public class BuildScreen extends AbstractContainerScreen<BuildContainer> {

	private static final Identifier TEXTURE = GamemodeBuild.id("textures/gui/menu.png");

	private static final Identifier TABS = Identifier.withDefaultNamespace("textures/gui/container/creative_inventory/tab_items.png");
	private static final Identifier SCROLLER = Identifier.withDefaultNamespace("container/creative_inventory/scroller");

	private EditBox searchField;

	private float scrollAmount;
	private boolean draggingScroll;

	private String lastSearchFilter = "";

	public BuildScreen(final BuildContainer screenContainer, final Inventory inv, final Component titleIn) {
		super(screenContainer, inv, titleIn, 195, 136);
	}

	@Override
	protected void init() {
		super.init();

        searchField = new EditBox(font, leftPos + 82, topPos + 6, 80, 9, Component.translatable("itemGroup.search"));
        searchField.setMaxLength(50);
        searchField.setBordered(false);
        searchField.setVisible(true);
        searchField.setTextColor(CommonColors.WHITE);
		searchField.setResponder(searchFilter -> {
			if (!searchFilter.equals(lastSearchFilter)) {
				updateSearch(searchFilter);
			}
			lastSearchFilter = searchFilter;
		});
        addRenderableWidget(searchField);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (searchField.keyPressed(event)) {
			return true;
		} else if (searchField.isFocused() && searchField.isVisible() && event.key() != InputConstants.KEY_ESCAPE) {
			return true;
		}
		return super.keyPressed(event);
	}

	private void updateSearch(final String searchFilter) {
		Locale locale = Minecraft.getInstance().getLanguageManager().getJavaLocale();
		final BitSet filteredSlots = menu.applyFilter(locale, searchFilter);
		ClientPacketDistributor.sendToServer(new UpdateFilterMessage(filteredSlots));
		updateScroll(scrollAmount); // Refresh scrollbar
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractBackground(graphics, mouseX, mouseY, a);

		graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);

		if (menu.canScroll()) {
			final Rect2i rect = scrollRect();
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SCROLLER, rect.left, rect.top, rect.width, rect.height);
		}
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
		graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, -12566464, false);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (menu.canScroll()) {
			final int scrollHeight = menu.scrollHeight();
			updateScroll((float) (scrollAmount - scrollY / scrollHeight));
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
			final Rect2i rect = scrollRect();
			if (rect.contains(event.x(), event.y())) {
				draggingScroll = true;
				return true;
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (draggingScroll) {
			draggingScroll = false;
			return true;
		}
		return super.mouseReleased(event);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		if (draggingScroll) {
			final Rect2i area = scrollArea();
			final Rect2i rect = scrollRect();
			updateScroll((float) (event.y() - area.top - rect.height / 2.0F) / (area.height - rect.height));
			return true;
		}
		return super.mouseDragged(event, dx, dy);
	}

	private void updateScroll(final float amount) {
		scrollAmount = Mth.clamp(amount, 0.0F, 1.0F);

		final int scrollOffset = Math.round(scrollAmount * menu.scrollHeight());
		menu.setScrollOffset(scrollOffset);
	}

	private Rect2i scrollRect() {
		final Rect2i area = scrollArea();

		final int scrollLength = (int) ((area.height - 17) * scrollAmount);
		return new Rect2i(area.left, area.top + scrollLength, area.width, 15);
	}

	private Rect2i scrollArea() {
		final int scrollLeft = leftPos + 175;
		final int scrollTop = topPos + 18;
		return new Rect2i(scrollLeft, scrollTop, 12, 112);
	}

    private record Rect2i(int left, int top, int width, int height) {
        public boolean contains(final double x, final double y) {
            return x >= left && y >= top && x <= left + width && y <= top + height;
        }
    }
}
