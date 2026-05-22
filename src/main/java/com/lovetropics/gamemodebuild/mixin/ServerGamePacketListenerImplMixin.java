package com.lovetropics.gamemodebuild.mixin;

import com.lovetropics.gamemodebuild.GBConfigs;
import com.lovetropics.gamemodebuild.container.GBStackMarker;
import com.lovetropics.gamemodebuild.state.GBPlayerStore;
import com.lovetropics.gamemodebuild.state.GBServerState;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Predicate;

@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerImplMixin {

    @Shadow
    public ServerPlayer player;

    // Don't allow picking blocks with custom data when build mode is active.
    @ModifyVariable(method = "handlePickItemFromBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;isEmpty()Z"), name = "includeData")
    private boolean modifyFlag(boolean includeData) {
        if (GBServerState.isActiveFor(player)) {
            return false;
        }
        return includeData;
    }

    @Inject(method = "tryPickItem", at = @At(value = "HEAD"), cancellable = true)
    private void handlePickItemFromBlock(ItemStack itemStack, CallbackInfo ci) {
        if (!GBServerState.isActiveFor(player)) {
            return;
        }

        FeatureFlagSet enabledFeatures = player.level().enabledFeatures();
        RegistryAccess registryAccess = player.level().registryAccess();
        Predicate<ItemStack> predicate = GBConfigs.SERVER.getFilter(GBPlayerStore.getList(player)).getStackPredicate(enabledFeatures, registryAccess);

        if (!predicate.test(itemStack)) {
            ci.cancel();
        }

        GBStackMarker.mark(itemStack);
    }
}
