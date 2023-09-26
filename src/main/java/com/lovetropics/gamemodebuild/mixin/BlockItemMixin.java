package com.lovetropics.gamemodebuild.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.lovetropics.gamemodebuild.GamemodeBuild;
import com.lovetropics.gamemodebuild.container.GBStackMarker;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BlockItem.class)
public class BlockItemMixin extends Item {
    public BlockItemMixin(Properties pProperties) {
        super(pProperties);
    }

    @WrapOperation(method = "place", at = @At(value = "FIELD", opcode = Opcodes.GETFIELD, target = "Lnet/minecraft/world/entity/player/Abilities;instabuild:Z"))
    private boolean gamemodebuild$dontConsumeInBuildMode(Abilities abilities, Operation<Boolean> original, @Local(ordinal = 1) BlockPlaceContext ctx) {
        return original.call(abilities) || (GamemodeBuild.isActive(ctx.getPlayer()) && GBStackMarker.isMarked(ctx.getItemInHand()));
    }
}
