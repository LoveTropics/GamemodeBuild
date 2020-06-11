package com.lovetropics.gamemodebuild.container;

import com.lovetropics.gamemodebuild.SurvivalPlus;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;

public class SurvivalPlusScreen extends ContainerScreen<SurvivalPlusContainer> {
	
	private static final ResourceLocation TEXTURE = new ResourceLocation(SurvivalPlus.MODID, "textures/gui/menu_nosearch.png");
	
	private static final ResourceLocation TABS = new ResourceLocation("textures/gui/container/creative_inventory/tabs.png");
	
	private float scrollAmount;
	private boolean draggingScroll;
	
	public SurvivalPlusScreen(SurvivalPlusContainer screenContainer, PlayerInventory inv, ITextComponent titleIn) {
		super(screenContainer, inv, titleIn);
		this.xSize = 195;
		this.ySize = 136;// + 28;
	}
	
	@Override
	public void render(int mouseX, int mouseY, float partialTicks) {
		this.renderBackground();
		super.render(mouseX, mouseY, partialTicks);
		RenderSystem.disableBlend();
		this.renderHoveredToolTip(mouseX, mouseY);
	}
	
	@Override
	protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
		this.font.drawString(this.title.getFormattedText(), 8.0F, 6.0F/* + 28*/, 0x404040);
	}
	
	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
//		this.getMinecraft().getTextureManager().bindTexture(TABS);
//		for (int i = 1; i < 5; i++) {
//			this.blit(this.guiLeft + (i * 29), this.guiTop, i * 28, 0, 28, 32);
//		}
//		this.blit(this.guiLeft + this.xSize - 28, this.guiTop, 5 * 28, 0, 28, 32);
		
		this.getMinecraft().getTextureManager().bindTexture(TEXTURE);
		this.blit(this.guiLeft, this.guiTop/* + 28*/, 0, 0, this.xSize, this.ySize);
		
		this.getMinecraft().getTextureManager().bindTexture(TABS);
//		this.blit(this.guiLeft, this.guiTop, 0, 32, 28, 32);
		
		if (this.container.canScroll()) {
			Rect2i rect = this.scrollRect();
			this.blit(rect.left, rect.top, 232, 0, rect.width, rect.height);
		}
	}
	
	@Override
	public boolean mouseScrolled(double x, double y, double amount) {
		if (this.container.canScroll()) {
			int scrollHeight = this.container.scrollHeight();
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
		this.scrollAmount = MathHelper.clamp(amount, 0.0F, 1.0F);
		
		int scrollOffset = Math.round(this.scrollAmount * this.container.scrollHeight());
		this.container.setScrollOffset(scrollOffset);
	}
	
	private Rect2i scrollRect() {
		Rect2i area = this.scrollArea();
		
		int scrollLength = (int) ((area.height - 17) * this.scrollAmount);
		return new Rect2i(area.left, area.top + scrollLength, area.width, 15);
	}
	
	private Rect2i scrollArea() {
		int scrollLeft = this.guiLeft + 175;
		int scrollTop = this.guiTop + 18/* + 28*/;
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
