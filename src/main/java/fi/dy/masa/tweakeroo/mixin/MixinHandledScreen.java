package fi.dy.masa.tweakeroo.mixin;


import com.mojang.blaze3d.systems.RenderSystem;
import fi.dy.masa.malilib.MaLiLib;
import fi.dy.masa.tweakeroo.config.Configs;
import fi.dy.masa.tweakeroo.config.FeatureToggle;
import fi.dy.masa.tweakeroo.items.ItemList;
import fi.dy.masa.tweakeroo.renderer.RenderUtils;
import net.minecraft.block.Block;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Iterator;

import static net.minecraft.client.gui.screen.ingame.HandledScreen.drawSlotHighlight;

@Mixin(HandledScreen.class)
public abstract class MixinHandledScreen extends DrawableHelper {



    @Inject(method = "drawSlot",at=@At("HEAD"))
    public void injectItemHighlight(MatrixStack matrices, Slot slot, CallbackInfo ci) {


        if (!FeatureToggle.TWEAK_SELECTED_SCAN_ITEM_HIGHLIGHT.getBooleanValue()) return;

        if (ItemList.INSTANCE.isItemSelected(slot.getStack().getItem()) || isShulkerAndMatches(slot.getStack())) {

            fi.dy.masa.malilib.render.RenderUtils.drawRect(slot.x , slot.y, 16, 16, Configs.Generic.CONTAINER_SCAN_ITEM_HIGHLIGHT_COLOR.getColor().intValue,32);
            // Not drawing a border. Maybe one should be drawn?
            //fi.dy.masa.malilib.render.RenderUtils.drawOutline(slot.x, slot.y, 16, 16, 1, 0xFF800080,32);
        }

    }


    public boolean isShulkerAndMatches(ItemStack stack) {

        if (!(Block.getBlockFromItem(stack.getItem()) instanceof ShulkerBoxBlock)) return false;

        NbtCompound nbtCompound = BlockItem.getBlockEntityNbt(stack);
        if (nbtCompound != null) {

            if (nbtCompound.contains("Items", 9)) {
                DefaultedList<ItemStack> defaultedList = DefaultedList.ofSize(27, ItemStack.EMPTY);
                Inventories.readNbt(nbtCompound, defaultedList);

                for (ItemStack itemStack : defaultedList) {
                    Item item = itemStack.getItem();
                    if (ItemList.INSTANCE.isItemSelected(item)) {
                        return true;
                    }
                }

            }
        }
        return false;

    }

}
