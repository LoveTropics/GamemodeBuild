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

public record OpenBuildInventoryMessage() {
    public OpenBuildInventoryMessage(FriendlyByteBuf input) {
        this();
    }

    public void serialize(FriendlyByteBuf output) {
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player != null && GBServerState.isActiveFor(player)) {
            NetworkHooks.openScreen(player, new SimpleMenuProvider(BuildContainer::new, BuildContainer.title()), buf -> buf.writeUtf(GBPlayerStore.getList(player)));
        }
    }
}
