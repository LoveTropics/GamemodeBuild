package com.lovetropics.gamemodebuild.message;

import com.lovetropics.gamemodebuild.GamemodeBuild;
import com.lovetropics.gamemodebuild.container.BuildContainer;
import com.lovetropics.gamemodebuild.state.GBPlayer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record OpenBuildInventoryMessage() implements CustomPacketPayload {
    public static final Type<OpenBuildInventoryMessage> TYPE = new Type<>(GamemodeBuild.id("open_build_inventory"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenBuildInventoryMessage> CODEC = StreamCodec.unit(new OpenBuildInventoryMessage());

    public void handle(IPayloadContext ctx) {
        Player player = ctx.player();
        GBPlayer buildPlayer = GBPlayer.get(player);
        if (buildPlayer.isEffectivelyActive()) {
            player.openMenu(new SimpleMenuProvider(BuildContainer::new, BuildContainer.title()), buf -> buf.writeUtf(buildPlayer.getList()));
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
