package com.lovetropics.gamemodebuild.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.lovetropics.gamemodebuild.GamemodeBuild;
import com.lovetropics.gamemodebuild.container.GBStackMarker;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BucketItem.class)
public class BucketItemMixin extends Item {
    public BucketItemMixin(Properties pProperties) {
        super(pProperties);
    }

    @WrapOperation(method = "getEmptySuccessItem", at = @At(value = "FIELD", opcode = Opcodes.GETFIELD, target = "Lnet/minecraft/world/entity/player/Abilities;instabuild:Z"))
    private static boolean gamemodebuild$dontConsumeInBuildMode(Abilities abilities, Operation<Boolean> original, @Local(ordinal = 0) ItemStack stack, @Local Player player) {
        return original.call(abilities) || (GamemodeBuild.isActive(player) && GBStackMarker.isMarked(stack));
    }
}
