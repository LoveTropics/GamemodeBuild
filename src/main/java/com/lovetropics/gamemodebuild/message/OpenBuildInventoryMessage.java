package com.lovetropics.gamemodebuild.message;

import com.lovetropics.gamemodebuild.container.BuildContainer;
import com.lovetropics.gamemodebuild.state.GBPlayerStore;
import com.lovetropics.gamemodebuild.state.GBServerState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkHooks;

import java.util.function.Supplier;

public final class OpenBuildInventoryMessage {
	public void serialize(FriendlyByteBuf buffer) {
	}
	
	public static OpenBuildInventoryMessage deserialize(FriendlyByteBuf buffer) {
		return new OpenBuildInventoryMessage();
	}
	
	public static boolean handle(OpenBuildInventoryMessage message, Supplier<NetworkEvent.Context> ctxSupplier) {
		NetworkEvent.Context ctx = ctxSupplier.get();
		ctx.enqueueWork(() -> {
			ServerPlayer player = ctx.getSender();
			if (player != null && GBServerState.isActiveFor(player)) {
				NetworkHooks.openGui(player, new SimpleMenuProvider(BuildContainer::new, BuildContainer.title()), buf -> buf.writeUtf(GBPlayerStore.getList(player)));
			}
		});
		
		return true;
	}
}
