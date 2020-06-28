package com.lovetropics.gamemodebuild.container;

import java.util.*;
import java.util.function.Supplier;

import net.minecraft.item.ItemStack;

import com.lovetropics.gamemodebuild.GamemodeBuild;
import com.lovetropics.gamemodebuild.message.UpdateFilterMessage;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Items;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;

public class BuildScreen extends ContainerScreen<BuildContainer> {
	private static final ResourceLocation TEXTURE = new ResourceLocation(GamemodeBuild.MODID, "textures/gui/menu.png");
	
	private static final ResourceLocation TABS = new ResourceLocation("textures/gui/container/creative_inventory/tabs.png");
	
	private TextFieldWidget searchField;
	
	private float scrollAmount;
	private boolean draggingScroll;

	private int selectedTab = -1;

	List<CreativeTab> tabs;
	List<CreativeTab> additionalTabs;

	private class CreativeTab {
		final String name;
		final Supplier<ItemStack> iconItem;

		private CreativeTab(String name, Supplier<ItemStack> iconItem) {
			this.name = name;
			this.iconItem = iconItem;
		}
	}
	
	public BuildScreen(BuildContainer screenContainer, PlayerInventory inv, ITextComponent titleIn) {
		super(screenContainer, inv, titleIn);
		this.xSize = 195;
		this.ySize = 136 + (28 * 2);
	}
	
	@Override
	protected void init() {
		super.init();

		this.tabs = new ArrayList<>();
		this.additionalTabs = new ArrayList<>();

		// Uncomment lines below to test with mock data.
		// tabs.add(new CreativeTab("Test", () -> new ItemStack(Items.GRASS_BLOCK)));
		// tabs.add(new CreativeTab("Test 2", () -> new ItemStack(Items.CACTUS)));
		// additionalTabs.add(new CreativeTab("All", () -> new ItemStack(Items.COMPASS)));
		
        this.searchField = new TextFieldWidget(this.font, this.guiLeft + 82, this.guiTop + 6 + 28, 80, 9, I18n.format("itemGroup.search"));
        this.searchField.setMaxStringLength(50);
        this.searchField.setEnableBackgroundDrawing(false);
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
		BitSet filteredSlots = this.container.applyFilter(this.searchField.getText());
		GamemodeBuild.NETWORK.sendToServer(new UpdateFilterMessage(filteredSlots));

		updateScroll(0);
	}
	
	@Override
	public boolean charTyped(char p_charTyped_1_, int p_charTyped_2_) {
		String s = this.searchField.getText();
		boolean ret = super.charTyped(p_charTyped_1_, p_charTyped_2_);
		if (!Objects.equals(s, this.searchField.getText())) {
			updateSearch();
		}
		return ret;
	}

	// Logic from CreativeScreen
	@Override
	public boolean keyPressed(int p_keyPressed_1_, int p_keyPressed_2_, int p_keyPressed_3_) {
		String s = this.searchField.getText();
		if (this.searchField.keyPressed(p_keyPressed_1_, p_keyPressed_2_, p_keyPressed_3_)) {
			if (!Objects.equals(s, this.searchField.getText())) {
				this.updateSearch();
			}
			return true;
		} else {
			return this.searchField.isFocused() && this.searchField.getVisible() && p_keyPressed_1_ != 256 ? true
					: super.keyPressed(p_keyPressed_1_, p_keyPressed_2_, p_keyPressed_3_);
		}
	}
	
	@Override
	public void render(int mouseX, int mouseY, float partialTicks) {
		this.renderBackground();
		super.render(mouseX, mouseY, partialTicks);
		RenderSystem.disableBlend();
		this.renderHoveredToolTip(mouseX, mouseY);
		renderTabHoverTooltip(mouseX, mouseY);
	}
	
	@Override
	protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
		this.font.drawString(this.title.getFormattedText(), 8.0F, 6.0F + 28, 0x404040);
	}
	
	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
		renderTabs();

		this.getMinecraft().getTextureManager().bindTexture(TEXTURE);
		blit(this.guiLeft, this.guiTop + 28, 0, 0, this.xSize, this.ySize);

		renderSelectedTab();

		this.getMinecraft().getTextureManager().bindTexture(TABS);

		if (this.container.canScroll()) {
			Rect2i rect = this.scrollRect();
			blit(rect.left, rect.top, 232, 0, rect.width, rect.height);
		} else {
			Rect2i rect = this.scrollArea();
			blit(rect.left, rect.top, 244, 0, rect.width, rect.height);
		}
		
		this.searchField.render(mouseX, mouseY, partialTicks);

		renderTabItems();
	}

	/**
	 *
	 * @param x X coordinate
	 * @param y Y coordinate
	 * @param textureX X coordinate on the texture
	 * @param textureY Y coordinate on the texture
	 * @param width Blit width
	 * @param height Blit height
	 */
	public void blit(int x, int y, int textureX, int textureY, int width, int height) {
		super.blit(x, y, textureX, textureY, width, height);
	}

	private void renderTabs() {
		this.getMinecraft().getTextureManager().bindTexture(TABS);

		int numTabs = tabs.size();
		int bottomTabs = numTabs > 5 ? numTabs - 5 : 0;
		int topTabs = numTabs - bottomTabs;

		//top tabs
		blit(this.guiLeft, this.guiTop, 0, 0, 28 * topTabs, 32);

		//bottom tabs
		blit(this.guiLeft, this.guiTop + this.ySize - 32, 0, 64, 28 * bottomTabs, 32);

		numTabs = additionalTabs.size();
		bottomTabs = numTabs / 2;
		topTabs = bottomTabs + (numTabs % 2);

		//additional tabs
		blit(this.guiLeft + this.xSize - (28 * topTabs), this.guiTop, (6 - topTabs) * 28, 0, 28 * topTabs, 32);
		blit(this.guiLeft + this.xSize - (28 * bottomTabs), this.guiTop + this.ySize - 32, (6 - bottomTabs) * 28, 64, 28 * bottomTabs, 32);
	}

	private void renderSelectedTab() {
		this.getMinecraft().getTextureManager().bindTexture(TABS);

		if (tabs.size() == 0 && selectedTab >= 0) return;
		if (additionalTabs.size() == 0 && selectedTab < 0) return;

		int xAdd, textureXadd;
		int y = this.guiTop;
		int textureY = 32;

		if (selectedTab < 0) {
			xAdd = xSize - 28;
			textureXadd = 28 * 5;

			if (selectedTab % 2 == 0) {
				y += this.ySize - 32;
				textureY += 64;
			}
		} else {
			if (selectedTab < 5) {
				xAdd = selectedTab * 28;
			} else {
				xAdd = (selectedTab - 5) * 28;
				y += this.ySize - 32;
				textureY += 64;
			}

			textureXadd = xAdd;
		}

		blit(this.guiLeft + xAdd, y, textureXadd, textureY, 28, 32);
	}

	private void renderTabItems() {
		RenderSystem.color3f(1F, 1F, 1F); //Forge: Reset color in case Items change it.
		RenderSystem.enableBlend(); //Forge: Make sure blend is enabled else tabs show a white border.
		RenderSystem.enableRescaleNormal();

		this.setBlitOffset(100);
		this.itemRenderer.zLevel = 100.0F;

		for (int i = 0; i < tabs.size(); i++) {
			CreativeTab tab = tabs.get(i);
			Rect2i coords = getTabItemCoords(i);

			ItemStack iconItem = tab.iconItem.get();

			itemRenderer.renderItemAndEffectIntoGUI(iconItem, coords.left, coords.top);
			itemRenderer.renderItemOverlays(this.font, iconItem, coords.left, coords.top);
		}

		for (int i = 0; i < additionalTabs.size(); i++) {
			CreativeTab tab = additionalTabs.get(i);
			Rect2i coords = getTabItemCoords(-i - 1);

			ItemStack iconItem = tab.iconItem.get();

			itemRenderer.renderItemAndEffectIntoGUI(iconItem, coords.left, coords.top);
			itemRenderer.renderItemOverlays(this.font, iconItem, coords.left, coords.top);
		}

		this.itemRenderer.zLevel = 0.0F;
		this.setBlitOffset(0);
	}

	private void renderTabHoverTooltip(int mouseX, int mouseY) {
		for (int i = 0; i < tabs.size(); i++) {
			CreativeTab tab = tabs.get(i);
			Rect2i coords = getTabCoords(i);

			if (isPointInRegion(coords.left - this.guiLeft, coords.top - this.guiTop, coords.width, coords.height, mouseX, mouseY)) {
				this.renderTooltip(tab.name, mouseX, mouseY);
			}
		}

		for (int i = 0; i < additionalTabs.size(); i++) {
			CreativeTab tab = additionalTabs.get(i);
			Rect2i coords = getTabCoords(-i - 1);

			if (isPointInRegion(coords.left - this.guiLeft, coords.top - this.guiTop, coords.width, coords.height, mouseX, mouseY)) {
				this.renderTooltip(tab.name, mouseX, mouseY);
			}
		}
	}

	private Rect2i getTabItemCoords(int index) {
		Rect2i tabCoords = getTabCoords(index);

		return new Rect2i(tabCoords.left + 6, tabCoords.top + 8, 16, 16);
	}

	private Rect2i getTabCoords(int index) {
		int x = this.guiLeft;
		int y = this.guiTop;

		if (index < 0) {
			x += xSize - 28;

			if (index % 2 == 0) {
				y += this.ySize - 32;
			}
		} else {
			if (index < 5) {
				x += index * 28;
			} else {
				x += (index - 5) * 28;
				y += this.ySize - 32;
			}
		}

		return new Rect2i(x, y, 28, 32);
	}

	private void selectTab(int index) {
		selectedTab = index;
		//TODO: change contents of inventory
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

			for (int i = 0; i < tabs.size(); i++) {
				Rect2i tabCoords = getTabCoords(i);

				if (tabCoords.contains(x, y)) {
					selectTab(i);
					return true;
				}
			}

			for (int i = 0; i < additionalTabs.size(); i++) {
				Rect2i tabCoords = getTabCoords(-i - 1);

				if (tabCoords.contains(x, y)) {
					selectTab(-i - 1);
					return true;
				}
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
		int scrollTop = this.guiTop + 18 + 28;
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
