package fi.dy.masa.tweakeroo.mixin.block;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import fi.dy.masa.tweakeroo.config.Configs;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.enums.ChestType;
import net.minecraft.util.BlockMirror;

@Mixin({ ChestBlock.class })
public class MixinChestBlock {
    @Inject(method = "mirror", at = @At("HEAD"), cancellable = true)
    public void mirrorFixed(BlockState state, BlockMirror mirror, CallbackInfoReturnable<BlockState> c) {
        if (Configs.Fixes.CHEST_MIRROR_FIX.getBooleanValue()) {
            ChestType type = state.get(ChestBlock.CHEST_TYPE);
            if (type != ChestType.SINGLE) {
                // Flip chest type. (Note: It would be nice to use chestType.getOpposite)
                state = state.with(ChestBlock.CHEST_TYPE, type == ChestType.LEFT ? ChestType.RIGHT : ChestType.LEFT);

                // Apply rotation
                state = state.rotate(mirror.getRotation(state.get(ChestBlock.FACING)));

                c.setReturnValue(state);
            }
        }
    }
}