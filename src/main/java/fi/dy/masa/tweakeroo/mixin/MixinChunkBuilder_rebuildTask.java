package fi.dy.masa.tweakeroo.mixin;

import java.util.Iterator;

import com.google.common.collect.AbstractIterator;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.util.math.BlockPos;
import net.optifine.BlockPosM;
import fi.dy.masa.tweakeroo.config.FeatureToggle;
import fi.dy.masa.tweakeroo.tweaks.RenderTweaks;

@Mixin(targets = "net.minecraft.client.render.chunk.ChunkBuilder$BuiltChunk$RebuildTask")
public abstract class MixinChunkBuilder_rebuildTask
{

    @Redirect(method = "Lnet/minecraft/client/render/chunk/ChunkBuilder$BuiltChunk$RebuildTask;render(FFFLnet/minecraft/client/render/chunk/ChunkBuilder$ChunkData;Lnet/minecraft/client/render/chunk/BlockBufferBuilderStorage;)Ljava/util/Set;", at = @At(value = "INVOKE",
    target = "Lnet/optifine/BlockPosM;getAllInBoxMutable(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/math/BlockPos;)Ljava/lang/Iterable;", ordinal = 0))
    private Iterable<BlockPosM> iterateProxy(BlockPos start, BlockPos end) {
         Iterator<BlockPos> iterator = BlockPos.iterate(start, end).iterator();

         return () -> {
            return new AbstractIterator<BlockPosM>() {
               protected BlockPosM computeNext() {
                  if (!iterator.hasNext()) {
                     return (BlockPosM) this.endOfData();
                  } else {
                     BlockPos pos = iterator.next();
                     if (!RenderTweaks.isPositionValidForRendering(pos))
                           return this.computeNext();
                     return new BlockPosM(pos.getX(),pos.getY(),pos.getZ());
                  }
               }
            };
         };   
    }
}
