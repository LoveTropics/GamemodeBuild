package com.lovetropics.survivalplus;

import java.util.List;

import net.minecraft.client.gui.ScreenManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.registries.ObjectHolder;

@EventBusSubscriber(modid = SurvivalPlus.MODID, bus = Bus.MOD)
public class SurvivalPlusContainer extends Container {

	@ObjectHolder(SurvivalPlus.MODID + ":container")
	public static final ContainerType<SurvivalPlusContainer> TYPE = null;

	@SubscribeEvent
	public static void onContainerRegistry(RegistryEvent.Register<ContainerType<?>> event) {
		ContainerType<SurvivalPlusContainer> type = new ContainerType<>(SurvivalPlusContainer::new);
		event.getRegistry().register(type.setRegistryName("container"));

		DistExecutor.runWhenOn(Dist.CLIENT, () -> () -> ScreenManager.registerFactory(type, SurvivalPlusScreen::new));
	}

	public static StringTextComponent title() {
		return new StringTextComponent("SurvivalPlus");
	}

	private static class InfiniteInventory implements IInventory {
		private final PlayerEntity player;
		private List<ItemStack> items;

		private InfiniteInventory(PlayerEntity player, List<ItemStack> items) {
			this.player = player;
			this.items = items;
		}

		public void setAvailableItems(List<ItemStack> items) {
			this.items = items;
		}

		@Override
		public void clear() { }

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
			return index < this.items.size() ? this.items.get(index).copy() : ItemStack.EMPTY;
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
	}

	public static class InfiniteSlot extends Slot {
		public InfiniteSlot(InfiniteInventory inventory, int index, int x, int y) {
			super(inventory, index, x, y);
		}

		@Override
		public boolean canTakeStack(PlayerEntity player) {
			return true;
		}
	}

	private final PlayerInventory playerInv;
	private final InfiniteInventory inventory;

	public SurvivalPlusContainer(int windowId, PlayerInventory playerInventory) {
		this(windowId, playerInventory, playerInventory.player);
	}

	public SurvivalPlusContainer(int windowId, PlayerInventory playerInventory, PlayerEntity player) {
		super(TYPE, windowId);
		this.playerInv = playerInventory;

		List<ItemStack> buildingStacks = SPConfigs.SERVER.getFilter().getAllStacks();
		this.inventory = new InfiniteInventory(player, buildingStacks);

		int width = 9;
		int height = 5;

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int i = x + y * width;
				addSlot(new InfiniteSlot(inventory, i, 9 + x * 18, 28 + 18 + y * 18));
			}
		}

		for (int h = 0; h < width; h++) {
			this.addSlot(new Slot(playerInventory, h, 9 + h * 18, 112 + 28));
		}
	}

	@OnlyIn(Dist.CLIENT)
	@Override
	public void setAll(List<ItemStack> stacks) {
		this.inventory.setAvailableItems(stacks);
	}

	@Override
	public boolean canInteractWith(PlayerEntity playerIn) {
		return true;
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
				// TODO make sure this is an item you can get from the inventory in the first place
				slot.putStack(ItemStack.EMPTY);
				return ItemStack.EMPTY;
			}
		}

		return ItemStack.EMPTY;
	}
}
