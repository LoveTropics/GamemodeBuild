package com.lovetropics.gamemodebuild.message;

import com.lovetropics.gamemodebuild.state.GBClientState;
import com.lovetropics.gamemodebuild.state.GBServerState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SetActiveMessage(boolean active) {
    public SetActiveMessage(FriendlyByteBuf input) {
        this(input.readBoolean());
    }

    public void serialize(FriendlyByteBuf output) {
        output.writeBoolean(active);
    }

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        if (ctx.getDirection() == NetworkDirection.PLAY_TO_SERVER) {
            ServerPlayer player = ctx.getSender();
            if (player != null) {
                GBServerState.requestActive(player, active);
            }
        } else if (ctx.getDirection() == NetworkDirection.PLAY_TO_CLIENT) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> setClientState(active));
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static void setClientState(boolean state) {
        GBClientState.setActive(state);
    }
}
