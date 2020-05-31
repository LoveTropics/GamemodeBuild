package com.lovetropics.survivalplus;

import java.util.List;
import java.util.Random;

import net.minecraft.client.gui.ScreenManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.registry.Registry;
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

	private class InfiniteInventory implements IInventory {

		private ItemStack source;

		public InfiniteInventory(ItemStack source) {
			setSource(source);
		}

		public void setSource(ItemStack source) {
			this.source = source.copy();
			this.source.setCount(1);
		}

		@Override
		public void clear() {}

		@Override
		public int getSizeInventory() {
			return 1;
		}

		@Override
		public boolean isEmpty() {
			return false;
		}

		@Override
		public ItemStack getStackInSlot(int index) {
			return index == 0 ? source.copy() : ItemStack.EMPTY;
		}

		@Override
		public ItemStack decrStackSize(int index, int count) {
			if (index == 0) {
				ItemStack ret = source.copy();
				ret.setCount(count);
				return ret;
			}
			return ItemStack.EMPTY;
		}

		@Override
		public ItemStack removeStackFromSlot(int index) {
			return getStackInSlot(index);
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

	private class InfiniteSlot extends Slot {

		public InfiniteSlot(ItemStack resource, int xPosition, int yPosition) {
			super(new InfiniteInventory(resource), 0, xPosition, yPosition);
		}
		
		public void setSource(ItemStack source) {
			((InfiniteInventory)this.inventory).setSource(source);
		}

		@Override
		public boolean isItemValid(ItemStack stack) {
			return true;
		}
	}

	private final PlayerInventory playerInv;

	public SurvivalPlusContainer(int windowId, PlayerInventory playerInventory) {
		this(windowId, playerInventory, playerInventory.player);
	}
	
	public SurvivalPlusContainer(int windowId, PlayerInventory playerInventory, PlayerEntity player) {
		super(TYPE, windowId);
		this.playerInv = playerInventory;
		
		List<ItemStack> buildingStacks = SPConfigs.SERVER.getFilter().getAllStacks();
		
		int height = 5;
		for (int x = 0; x < 9; x++) {
			for (int y = 0; y < height; y++) {
				int i = x + (y * 9);
				ItemStack resource = i >= buildingStacks.size() ? ItemStack.EMPTY : buildingStacks.get(i);
				addSlot(new InfiniteSlot(resource, 9 + x * 18, 28 + 18 + y * 18));
			}
		}
		
		for (int h = 0; h < 9; h++) {
	         this.addSlot(new Slot(playerInventory, h, 9 + h * 18, 112 + 28));
		}
	}

	@OnlyIn(Dist.CLIENT)
	@Override
	public void setAll(List<ItemStack> stacks) {
		for (int i = 0; i < stacks.size(); ++i) {
			Slot s = this.getSlot(i);
			if (s instanceof InfiniteSlot) {
				((InfiniteSlot)s).setSource(stacks.get(i));
			} else {
				s.putStack(stacks.get(i));
			}
		}
	}

	@Override
	public boolean canInteractWith(PlayerEntity playerIn) {
		return true;
	}

	@Override
	public ItemStack transferStackInSlot(PlayerEntity playerIn, int index) {
		ItemStack itemstack = ItemStack.EMPTY;
		Slot slot = this.inventorySlots.get(index);
		if (slot != null && slot.getHasStack()) {
			ItemStack itemstack1 = slot.getStack();
			itemstack = itemstack1.copy();
			if (index < 5 * 9) {
				itemstack1.setCount(64);
				this.mergeItemStack(itemstack1, 5 * 9, this.inventorySlots.size(), false);
				return ItemStack.EMPTY;
			} else {
				// TODO make sure this is an item you can get from the inventory in the first place
				slot.putStack(ItemStack.EMPTY);
				return ItemStack.EMPTY;
			}
		}

		return itemstack;
	}
}
