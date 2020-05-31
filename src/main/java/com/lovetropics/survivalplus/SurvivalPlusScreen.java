package com.lovetropics.survivalplus;

import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;

public class SurvivalPlusScreen extends ContainerScreen<SurvivalPlusContainer> {
	
	private static final ResourceLocation TEXTURE = new ResourceLocation(SurvivalPlus.MODID, "textures/gui/menu.png");
	
	private static final ResourceLocation TABS = new ResourceLocation("textures/gui/container/creative_inventory/tabs.png");

	public SurvivalPlusScreen(SurvivalPlusContainer screenContainer, PlayerInventory inv, ITextComponent titleIn) {
		super(screenContainer, inv, titleIn);
		xSize = 195;
		ySize = 136 + 28;
	}

	@Override
	protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
		this.font.drawString(this.title.getFormattedText(), 8.0F, 6.0F + 28, 0x404040);
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
		getMinecraft().getTextureManager().bindTexture(TABS);
		for (int i = 1; i < 5; i++) {
			blit(guiLeft + (i * 29), guiTop, i * 28, 0, 28, 32);
		}
		blit(guiLeft + xSize - 28, guiTop, 5 * 28, 0, 28, 32);
		
		getMinecraft().getTextureManager().bindTexture(TEXTURE);
		blit(guiLeft, guiTop + 28, 0, 0, xSize, ySize);
		
		getMinecraft().getTextureManager().bindTexture(TABS);
		blit(guiLeft, guiTop, 0, 32, 28, 32);
	}
}
