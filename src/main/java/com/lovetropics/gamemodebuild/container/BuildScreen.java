package com.lovetropics.gamemodebuild.container;

import com.lovetropics.gamemodebuild.GamemodeBuild;
import com.lovetropics.gamemodebuild.message.GBNetwork;
import com.lovetropics.gamemodebuild.message.UpdateFilterMessage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;

import java.util.BitSet;
import java.util.Objects;

public class BuildScreen extends AbstractContainerScreen<BuildContainer> {
	
	private static final ResourceLocation TEXTURE = new ResourceLocation(GamemodeBuild.MODID, "textures/gui/menu.png");
	
	private static final ResourceLocation TABS = new ResourceLocation("textures/gui/container/creative_inventory/tabs.png");
	
	private EditBox searchField;
	
	private float scrollAmount;
	private boolean draggingScroll;
	
	public BuildScreen(BuildContainer screenContainer, Inventory inv, Component titleIn) {
		super(screenContainer, inv, titleIn);
		this.imageWidth = 195;
		this.imageHeight = 136;// + 28;
	}
	
	@Override
	protected void init() {
		super.init();
		
        this.searchField = new EditBox(this.font, this.leftPos + 82, this.topPos + 6, 80, 9, new TranslatableComponent("itemGroup.search"));
        this.searchField.setMaxLength(50);
        this.searchField.setBordered(false);
        this.searchField.setVisible(true);
        this.searchField.setTextColor(16777215);
        this.children.add(this.searchField);
	}
	
	@Override
	public void tick() {
		super.tick();
		this.searchField.tick();
	}
	
	private void updateSearch() {
		BitSet filteredSlots = this.menu.applyFilter(this.searchField.getValue());
		GBNetwork.CHANNEL.sendToServer(new UpdateFilterMessage(filteredSlots));
		updateScroll(scrollAmount); // Refresh scrollbar
	}
	
	@Override
	public boolean charTyped(char p_charTyped_1_, int p_charTyped_2_) {
		String s = this.searchField.getValue();
		boolean ret = super.charTyped(p_charTyped_1_, p_charTyped_2_);
		if (!Objects.equals(s, this.searchField.getValue())) {
			updateSearch();
		}
		return ret;
	}

	// Logic from CreativeScreen
	@Override
	public boolean keyPressed(int p_keyPressed_1_, int p_keyPressed_2_, int p_keyPressed_3_) {
		String s = this.searchField.getValue();
		if (this.searchField.keyPressed(p_keyPressed_1_, p_keyPressed_2_, p_keyPressed_3_)) {
			if (!Objects.equals(s, this.searchField.getValue())) {
				this.updateSearch();
			}
			return true;
		} else {
			return this.searchField.isFocused() && this.searchField.isVisible() && p_keyPressed_1_ != 256 || super.keyPressed(p_keyPressed_1_, p_keyPressed_2_, p_keyPressed_3_);
		}
	}

	@Override
	public void render(PoseStack transform, int mouseX, int mouseY, float partialTicks) {
		this.renderBackground(transform);
		super.render(transform, mouseX, mouseY, partialTicks);
		RenderSystem.disableBlend();
		this.renderTooltip(transform, mouseX, mouseY);
	}

	@Override
	protected void renderLabels(PoseStack transform, int mouseX, int mouseY) {
		this.font.draw(transform, this.title.getString(), 8.0F, 6.0F/* + 28*/, 0x404040);
	}
	
	@Override
	protected void renderBg(PoseStack transform, float partialTicks, int mouseX, int mouseY) {
//		this.getMinecraft().getTextureManager().bindTexture(TABS);
//		for (int i = 1; i < 5; i++) {
//			this.blit(this.guiLeft + (i * 29), this.guiTop, i * 28, 0, 28, 32);
//		}
//		this.blit(this.guiLeft + this.xSize - 28, this.guiTop, 5 * 28, 0, 28, 32);
		
		this.getMinecraft().getTextureManager().bind(TEXTURE);
		this.blit(transform, this.leftPos, this.topPos/* + 28*/, 0, 0, this.imageWidth, this.imageHeight);
		
		this.getMinecraft().getTextureManager().bind(TABS);
//		this.blit(this.guiLeft, this.guiTop, 0, 32, 28, 32);
		
		if (this.menu.canScroll()) {
			Rect2i rect = this.scrollRect();
			this.blit(transform, rect.left, rect.top, 232, 0, rect.width, rect.height);
		}
		
		this.searchField.render(transform, mouseX, mouseY, partialTicks);
	}
	
	@Override
	public boolean mouseScrolled(double x, double y, double amount) {
		if (this.menu.canScroll()) {
			int scrollHeight = this.menu.scrollHeight();
			this.updateScroll((float) (this.scrollAmount - amount / scrollHeight));
			return true;
		}
		
		return false;
	}
	
	@Override
	public boolean mouseClicked(double x, double y, int button) {
		if (button == 0) {
			Rect2i rect = this.scrollRect();
			if (rect.contains(x, y)) {
				this.draggingScroll = true;
				return true;
			}
		}
		return super.mouseClicked(x, y, button);
	}
	
	@Override
	public boolean mouseReleased(double x, double y, int button) {
		if (this.draggingScroll) {
			this.draggingScroll = false;
			return true;
		}
		return super.mouseReleased(x, y, button);
	}
	
	@Override
	public boolean mouseDragged(double x, double y, int button, double p_mouseDragged_6_, double p_mouseDragged_8_) {
		if (this.draggingScroll) {
			Rect2i area = this.scrollArea();
			Rect2i rect = this.scrollRect();
			this.updateScroll((float) (y - area.top - rect.height / 2.0F) / (area.height - rect.height));
			return true;
		} else {
			return super.mouseDragged(x, y, button, p_mouseDragged_6_, p_mouseDragged_8_);
		}
	}
	
	private void updateScroll(float amount) {
		this.scrollAmount = Mth.clamp(amount, 0.0F, 1.0F);
		
		int scrollOffset = Math.round(this.scrollAmount * this.menu.scrollHeight());
		this.menu.setScrollOffset(scrollOffset);
	}
	
	private Rect2i scrollRect() {
		Rect2i area = this.scrollArea();
		
		int scrollLength = (int) ((area.height - 17) * this.scrollAmount);
		return new Rect2i(area.left, area.top + scrollLength, area.width, 15);
	}
	
	private Rect2i scrollArea() {
		int scrollLeft = this.leftPos + 175;
		int scrollTop = this.topPos + 18/* + 28*/;
		return new Rect2i(scrollLeft, scrollTop, 12, 112);
	}
	
	static class Rect2i {
		final int left;
		final int top;
		final int width;
		final int height;
		
		Rect2i(int left, int top, int width, int height) {
			this.left = left;
			this.top = top;
			this.width = width;
			this.height = height;
		}
		
		boolean contains(double x, double y) {
			return x >= left && y >= top && x <= left + width && y <= top + height;
		}
	}
}
