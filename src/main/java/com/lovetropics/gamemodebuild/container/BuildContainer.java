package com.lovetropics.gamemodebuild.container;

import com.google.common.base.Strings;
import com.lovetropics.gamemodebuild.GBConfigs;
import com.lovetropics.gamemodebuild.GamemodeBuild;
import com.lovetropics.gamemodebuild.message.GBNetwork;
import com.lovetropics.gamemodebuild.message.SetScrollMessage;
import com.lovetropics.gamemodebuild.state.GBPlayerStore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.TextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.extensions.IForgeContainerType;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.registries.ObjectHolder;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@EventBusSubscriber(modid = GamemodeBuild.MODID, bus = Bus.MOD)
public class BuildContainer extends AbstractContainerMenu {
	public static final int WIDTH = 9;
	public static final int HEIGHT = 5;
	
	@ObjectHolder(GamemodeBuild.MODID + ":container")
	public static final MenuType<BuildContainer> TYPE = null;
	
	private static final ThreadLocal<Boolean> SUPPRESS_SEND_CHANGES = new ThreadLocal<>();
	private boolean takeStacks = false;
	
	@SubscribeEvent
	public static void onContainerRegistry(RegistryEvent.Register<MenuType<?>> event) {
		MenuType<BuildContainer> type = IForgeContainerType.create(BuildContainer::new);
		event.getRegistry().register(type.setRegistryName("container"));
		
		DistExecutor.runWhenOn(Dist.CLIENT, () -> () -> MenuScreens.register(type, BuildScreen::new));
	}
	
	public static TextComponent title() {
		return new TextComponent("Build Mode");
	}
	
	public class InfiniteInventory implements Container {
		private final Player player;
		private final List<ItemStack> masterItems;
		private final List<ItemStack> items;
		
		private InfiniteInventory(Player player, List<ItemStack> items) {
			this.player = player;
			this.masterItems = items;
			this.items = new ArrayList<>();
			this.setFilter(new BitSet());
		}
		
		@Override
		public void clearContent() {
		}
		
		@Override
		public int getContainerSize() {
			return this.items.size();
		}
		
		@Override
		public boolean isEmpty() {
			return this.items.isEmpty();
		}
		
		@Override
		public ItemStack getItem(int index) {
			if (index >= 0 && index < this.items.size()) {
				ItemStack stack = this.items.get(index).copy();
				if (takeStacks) {
					stack.setCount(stack.getMaxStackSize());
				}
				GBStackMarker.mark(stack);
				return stack;
			}
			return ItemStack.EMPTY;
		}
		
		@Override
		public ItemStack removeItem(int index, int count) {
			ItemStack stack = this.getItem(index);
			if (!stack.isEmpty()) {
				stack.setCount(count);
				return stack;
			}
			return ItemStack.EMPTY;
		}
		
		@Override
		public ItemStack removeItemNoUpdate(int index) {
			return this.getItem(index);
		}
		
		@Override
		public void setItem(int index, ItemStack stack) {
		}
		
		@Override
		public void setChanged() {
		}
		
		@Override
		public boolean stillValid(Player player) {
			return true;
		}
		
		public void setFilter(BitSet filteredSlots) {
			this.items.clear();
			for (int i = 0; i < this.masterItems.size(); i++) {
				if (!filteredSlots.get(i)) {
					items.add(this.masterItems.get(i));
				}
			}
		}

		@OnlyIn(Dist.CLIENT)
		public BitSet applyFilter(String filter) {
			BitSet filteredSlots = new BitSet();
			Locale locale = Minecraft.getInstance().getLanguageManager().getSelected().getJavaLocale();
			filter = filter.toLowerCase(locale);
			if (!Strings.isNullOrEmpty(filter)) {
				for (int i = 0; i < this.masterItems.size(); i++) {
					ItemStack stack = this.masterItems.get(i);
					if (stack.isEmpty() || !stack.getHoverName().getString().toLowerCase(locale).contains(filter)) {
						filteredSlots.set(i);
					}
				}
			}
			setFilter(filteredSlots);
			return filteredSlots;
		}
	}
	
	public static class InfiniteSlot extends Slot {
		private int idxOffset;
		
		public InfiniteSlot(InfiniteInventory inventory, int index, int x, int y) {
			super(inventory, index, x, y);
		}
		
		@Override
		public boolean mayPickup(Player player) {
			return true;
		}
		
		@Override
		public boolean mayPlace(ItemStack stack) {
			// only allow items to be deleted if they are from the gui
			return GBStackMarker.isMarked(stack);
		}
		
		public void setScrollOffset(int offset) {
			this.idxOffset = Math.max(offset, 0) * WIDTH;
		}
		
		@Override
		public void set(ItemStack stack) {
		}
		
		@Override
		public ItemStack remove(int amount) {
			return this.container.removeItem(this.getSlotIndex() + this.idxOffset, amount);
		}
		
		@Override
		public ItemStack getItem() {
			// we don't want to synchronize anything when running detectAndSendChanges, so hide our real state
			Boolean suppressSendChanges = SUPPRESS_SEND_CHANGES.get();
			if (suppressSendChanges != null && suppressSendChanges) {
				return ItemStack.EMPTY;
			}
			
			return this.container.getItem(this.getSlotIndex() + this.idxOffset);
		}
	}
	
	private final Player player;
	public final InfiniteInventory inventory;
	
	private int scrollOffset;

	// Server container
	public BuildContainer(int windowId, Inventory playerInventory, Player player) {
		this(windowId, playerInventory, player, GBPlayerStore.getList(player));
	}

	// Client container
	public BuildContainer(int windowId, Inventory playerInventory, FriendlyByteBuf extraData) {
		this(windowId, playerInventory, playerInventory.player, extraData.readUtf());
	}

	public BuildContainer(int windowId, Inventory playerInventory, Player player, @Nullable String list) {
		super(TYPE, windowId);
		this.player = player;
		
		List<ItemStack> buildingStacks = list == null ? Collections.emptyList() : GBConfigs.SERVER.getFilter(list).getAllStacks();
		this.inventory = new InfiniteInventory(player, buildingStacks);
		
		for (int y = 0; y < HEIGHT; y++) {
			for (int x = 0; x < WIDTH; x++) {
				int i = x + y * WIDTH;
				addSlot(new InfiniteSlot(inventory, i, 9 + x * 18, /*28 + */18 + y * 18));
			}
		}
		
		for (int h = 0; h < WIDTH; h++) {
			this.addSlot(new Slot(playerInventory, h, 9 + h * 18, 112/* + 28*/));
		}
	}
	
	public void setScrollOffset(int scrollOffset) {
		scrollOffset = Math.max(scrollOffset, 0);
		
		if (this.scrollOffset != scrollOffset) {
			this.scrollOffset = scrollOffset;
			
			if (player.level.isClientSide) {
				GBNetwork.CHANNEL.sendToServer(new SetScrollMessage(scrollOffset));
			}
			
			for (Slot slot : this.slots) {
				if (slot instanceof InfiniteSlot) {
					((InfiniteSlot) slot).setScrollOffset(scrollOffset);
				}
			}
		}
	}
	
	public int scrollHeight() {
		return (this.inventory.items.size() + WIDTH - 1) / WIDTH - HEIGHT;
	}
	
	public boolean canScroll() {
		return this.inventory.items.size() > WIDTH * HEIGHT;
	}
	
	@OnlyIn(Dist.CLIENT)
	public BitSet applyFilter(String filter) {
		return ((InfiniteInventory)this.inventory).applyFilter(filter);
	}
	
	public void setFilter(BitSet filteredSlots) {
		((InfiniteInventory)this.inventory).setFilter(filteredSlots);
	}
	
	@OnlyIn(Dist.CLIENT)
	@Override
	public void setAll(List<ItemStack> stacks) {
	}
	
	@Override
	public void broadcastChanges() {
		SUPPRESS_SEND_CHANGES.set(true);
		try {
			super.broadcastChanges();
		} finally {
			SUPPRESS_SEND_CHANGES.set(false);
		}
	}
	
	@Override
	public boolean stillValid(Player playerIn) {
		return true;
	}
	
	@Override
	public ItemStack clicked(int slotId, int dragType, ClickType clickTypeIn, Player player) {
		if (slotId < 0 || slotId >= HEIGHT * WIDTH) {
			// This is not an infinite slot, we don't need to do anything special
			return super.clicked(slotId, dragType, clickTypeIn, player);
		}
		this.takeStacks = clickTypeIn == ClickType.SWAP;
		ItemStack oldCursor = player.inventory.getCarried().copy();
		if ((clickTypeIn == ClickType.PICKUP || clickTypeIn == ClickType.PICKUP_ALL) && getSlot(slotId).getItem().sameItem(oldCursor)) {
			// Allow pulling single items into an existing stack
			ItemStack ret = oldCursor.copy();
			if (ret.getCount() < ret.getMaxStackSize()) {
				ret.grow(1);
			}
			player.inventory.setCarried(ret);
			return getSlot(slotId).getItem();
		}
		ItemStack ret = super.clicked(slotId, dragType, clickTypeIn, player);
		ItemStack newCursor = player.inventory.getCarried();
		if (!oldCursor.isEmpty() && GBStackMarker.isMarked(oldCursor) && GBStackMarker.isMarked(newCursor)) {
			if (!oldCursor.sameItem(newCursor)) {
				player.inventory.setCarried(ItemStack.EMPTY);
			} else {
				newCursor.setCount(Math.max(oldCursor.getCount(), newCursor.getCount()));
				player.inventory.setCarried(newCursor);
			}
		}
		this.takeStacks = false;
		return ret;
	}
	
	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		Slot slot = this.slots.get(index);
		
		// recreate shift-click to pick up max stack behaviour
		if (slot instanceof InfiniteSlot) {
			ItemStack stack = slot.getItem().copy();
			stack.setCount(stack.getMaxStackSize());
			player.inventory.setCarried(stack);
			
			return ItemStack.EMPTY;
		}
		
		if (slot != null && slot.hasItem()) {
			ItemStack stack = slot.getItem();
			if (index < 5 * 9) {
				stack.setCount(64);
				this.moveItemStackTo(stack, 5 * 9, this.slots.size(), false);
				return ItemStack.EMPTY;
			} else {
				if (GBStackMarker.isMarked(stack)) {
					slot.set(ItemStack.EMPTY);
				}
				return ItemStack.EMPTY;
			}
		}
		
		return ItemStack.EMPTY;
	}
}
