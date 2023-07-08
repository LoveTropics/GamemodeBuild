package com.lovetropics.gamemodebuild.message;

import com.lovetropics.gamemodebuild.state.GBClientState;
import com.lovetropics.gamemodebuild.state.GBServerState;
import com.lovetropics.gamemodebuild.state.GBServerState.NotificationType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SetActiveMessage(boolean enabled) {
    public SetActiveMessage(FriendlyByteBuf input) {
        this(input.readBoolean());
    }

    public void serialize(FriendlyByteBuf output) {
        output.writeBoolean(enabled);
    }

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        if (ctx.getDirection() == NetworkDirection.PLAY_TO_SERVER) {
            ServerPlayer player = ctx.getSender();
            if (player != null) {
                if (GBServerState.isEnabledFor(player)) {
                    GBServerState.setActiveFor(player, enabled);
                } else {
                    GBServerState.notifyPlayerActivity(false, player, NotificationType.ACTIVE);
                }
            }
        } else if (ctx.getDirection() == NetworkDirection.PLAY_TO_CLIENT) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> setClientState(enabled));
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static void setClientState(boolean state) {
        GBClientState.setActive(state);
    }
}
