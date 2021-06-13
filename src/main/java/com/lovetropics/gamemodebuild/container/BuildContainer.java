package com.lovetropics.gamemodebuild.container;

import com.google.common.base.Strings;
import com.lovetropics.gamemodebuild.GBConfigs;
import com.lovetropics.gamemodebuild.GamemodeBuild;
import com.lovetropics.gamemodebuild.message.GBNetwork;
import com.lovetropics.gamemodebuild.message.SetScrollMessage;
import com.lovetropics.gamemodebuild.state.GBPlayerStore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScreenManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.StringTextComponent;
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
public class BuildContainer extends Container {
	public static final int WIDTH = 9;
	public static final int HEIGHT = 5;
	
	@ObjectHolder(GamemodeBuild.MODID + ":container")
	public static final ContainerType<BuildContainer> TYPE = null;
	
	private static final ThreadLocal<Boolean> SUPPRESS_SEND_CHANGES = new ThreadLocal<>();
	private boolean takeStacks = false;
	
	@SubscribeEvent
	public static void onContainerRegistry(RegistryEvent.Register<ContainerType<?>> event) {
		ContainerType<BuildContainer> type = IForgeContainerType.create(BuildContainer::new);
		event.getRegistry().register(type.setRegistryName("container"));
		
		DistExecutor.runWhenOn(Dist.CLIENT, () -> () -> ScreenManager.registerFactory(type, BuildScreen::new));
	}
	
	public static StringTextComponent title() {
		return new StringTextComponent("Build Mode");
	}
	
	public class InfiniteInventory implements IInventory {
		private final PlayerEntity player;
		private final List<ItemStack> masterItems;
		private final List<ItemStack> items;
		
		private InfiniteInventory(PlayerEntity player, List<ItemStack> items) {
			this.player = player;
			this.masterItems = items;
			this.items = new ArrayList<>();
			this.setFilter(new BitSet());
		}
		
		@Override
		public void clear() {
		}
		
		@Override
		public int getSizeInventory() {
			return this.items.size();
		}
		
		@Override
		public boolean isEmpty() {
			return this.items.isEmpty();
		}
		
		@Override
		public ItemStack getStackInSlot(int index) {
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
		public ItemStack decrStackSize(int index, int count) {
			ItemStack stack = this.getStackInSlot(index);
			if (!stack.isEmpty()) {
				stack.setCount(count);
				return stack;
			}
			return ItemStack.EMPTY;
		}
		
		@Override
		public ItemStack removeStackFromSlot(int index) {
			return this.getStackInSlot(index);
		}
		
		@Override
		public void setInventorySlotContents(int index, ItemStack stack) {
		}
		
		@Override
		public void markDirty() {
		}
		
		@Override
		public boolean isUsableByPlayer(PlayerEntity player) {
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
			Locale locale = Minecraft.getInstance().getLanguageManager().getCurrentLanguage().getJavaLocale();
			filter = filter.toLowerCase(locale);
			if (!Strings.isNullOrEmpty(filter)) {
				for (int i = 0; i < this.masterItems.size(); i++) {
					ItemStack stack = this.masterItems.get(i);
					if (stack.isEmpty() || !stack.getDisplayName().getString().toLowerCase(locale).contains(filter)) {
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
		public boolean canTakeStack(PlayerEntity player) {
			return true;
		}
		
		@Override
		public boolean isItemValid(ItemStack stack) {
			// only allow items to be deleted if they are from the gui
			return GBStackMarker.isMarked(stack);
		}
		
		public void setScrollOffset(int offset) {
			this.idxOffset = Math.max(offset, 0) * WIDTH;
		}
		
		@Override
		public void putStack(ItemStack stack) {
		}
		
		@Override
		public ItemStack decrStackSize(int amount) {
			return this.inventory.decrStackSize(this.getSlotIndex() + this.idxOffset, amount);
		}
		
		@Override
		public ItemStack getStack() {
			// we don't want to synchronize anything when running detectAndSendChanges, so hide our real state
			Boolean suppressSendChanges = SUPPRESS_SEND_CHANGES.get();
			if (suppressSendChanges != null && suppressSendChanges) {
				return ItemStack.EMPTY;
			}
			
			return this.inventory.getStackInSlot(this.getSlotIndex() + this.idxOffset);
		}
	}
	
	private final PlayerEntity player;
	public final InfiniteInventory inventory;
	
	private int scrollOffset;

	// Server container
	public BuildContainer(int windowId, PlayerInventory playerInventory, PlayerEntity player) {
		this(windowId, playerInventory, player, GBPlayerStore.getList(player));
	}

	// Client container
	public BuildContainer(int windowId, PlayerInventory playerInventory, PacketBuffer extraData) {
		this(windowId, playerInventory, playerInventory.player, extraData.readString());
	}

	public BuildContainer(int windowId, PlayerInventory playerInventory, PlayerEntity player, @Nullable String list) {
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
			
			if (player.world.isRemote) {
				GBNetwork.CHANNEL.sendToServer(new SetScrollMessage(scrollOffset));
			}
			
			for (Slot slot : this.inventorySlots) {
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
	public void detectAndSendChanges() {
		SUPPRESS_SEND_CHANGES.set(true);
		try {
			super.detectAndSendChanges();
		} finally {
			SUPPRESS_SEND_CHANGES.set(false);
		}
	}
	
	@Override
	public boolean canInteractWith(PlayerEntity playerIn) {
		return true;
	}
	
	@Override
	public ItemStack slotClick(int slotId, int dragType, ClickType clickTypeIn, PlayerEntity player) {
		if (slotId < 0 || slotId >= HEIGHT * WIDTH) {
			// This is not an infinite slot, we don't need to do anything special
			return super.slotClick(slotId, dragType, clickTypeIn, player);
		}
		this.takeStacks = clickTypeIn == ClickType.SWAP;
		ItemStack oldCursor = player.inventory.getItemStack().copy();
		if ((clickTypeIn == ClickType.PICKUP || clickTypeIn == ClickType.PICKUP_ALL) && getSlot(slotId).getStack().isItemEqual(oldCursor)) {
			// Allow pulling single items into an existing stack
			ItemStack ret = oldCursor.copy();
			if (ret.getCount() < ret.getMaxStackSize()) {
				ret.grow(1);
			}
			player.inventory.setItemStack(ret);
			return getSlot(slotId).getStack();
		}
		ItemStack ret = super.slotClick(slotId, dragType, clickTypeIn, player);
		ItemStack newCursor = player.inventory.getItemStack();
		if (!oldCursor.isEmpty() && GBStackMarker.isMarked(oldCursor) && GBStackMarker.isMarked(newCursor)) {
			if (!oldCursor.isItemEqual(newCursor)) {
				player.inventory.setItemStack(ItemStack.EMPTY);
			} else {
				newCursor.setCount(Math.max(oldCursor.getCount(), newCursor.getCount()));
				player.inventory.setItemStack(newCursor);
			}
		}
		this.takeStacks = false;
		return ret;
	}
	
	@Override
	public ItemStack transferStackInSlot(PlayerEntity player, int index) {
		Slot slot = this.inventorySlots.get(index);
		
		// recreate shift-click to pick up max stack behaviour
		if (slot instanceof InfiniteSlot) {
			ItemStack stack = slot.getStack().copy();
			stack.setCount(stack.getMaxStackSize());
			player.inventory.setItemStack(stack);
			
			return ItemStack.EMPTY;
		}
		
		if (slot != null && slot.getHasStack()) {
			ItemStack stack = slot.getStack();
			if (index < 5 * 9) {
				stack.setCount(64);
				this.mergeItemStack(stack, 5 * 9, this.inventorySlots.size(), false);
				return ItemStack.EMPTY;
			} else {
				if (GBStackMarker.isMarked(stack)) {
					slot.putStack(ItemStack.EMPTY);
				}
				return ItemStack.EMPTY;
			}
		}
		
		return ItemStack.EMPTY;
	}
}
