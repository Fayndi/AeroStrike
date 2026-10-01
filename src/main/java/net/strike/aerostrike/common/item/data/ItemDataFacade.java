package net.strike.aerostrike.common.item.data;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Data Facade / Adapter Layer.
 *
 * In Minecraft 1.20.1 Forge, item custom data is stored in ItemStack NBT tags.
 * In Minecraft 1.21.1 NeoForge, NBT is completely replaced by DataComponentType.
 *
 * By isolating all read/write logic for target coordinates, control frequencies,
 * and entity bindings into this facade, transitioning to 1.21.1 will require updating
 * only this single class without modifying any entity, GUI, or item logic.
 */
public final class ItemDataFacade {
    private static final String TAG_TARGET_POS = "TargetPos";
    private static final String TAG_FREQUENCY = "ControlFrequency";
    private static final String TAG_LINKED_ENTITY_UUID = "LinkedEntityUUID";

    public static void setTargetPos(ItemStack stack, BlockPos pos) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.put(TAG_TARGET_POS, NbtUtils.writeBlockPos(pos));
    }

    @Nullable
    public static BlockPos getTargetPos(ItemStack stack) {
        if (!stack.hasTag() || !stack.getTag().contains(TAG_TARGET_POS)) {
            return null;
        }
        return NbtUtils.readBlockPos(stack.getTag().getCompound(TAG_TARGET_POS));
    }

    public static void setFrequency(ItemStack stack, int frequency) {
        stack.getOrCreateTag().putInt(TAG_FREQUENCY, frequency);
    }

    public static int getFrequency(ItemStack stack, int defaultFrequency) {
        if (!stack.hasTag() || !stack.getTag().contains(TAG_FREQUENCY)) {
            return defaultFrequency;
        }
        return stack.getTag().getInt(TAG_FREQUENCY);
    }

    public static void setLinkedEntity(ItemStack stack, UUID entityId) {
        stack.getOrCreateTag().putUUID(TAG_LINKED_ENTITY_UUID, entityId);
    }

    @Nullable
    public static UUID getLinkedEntity(ItemStack stack) {
        if (!stack.hasTag() || !stack.getTag().hasUUID(TAG_LINKED_ENTITY_UUID)) {
            return null;
        }
        return stack.getTag().getUUID(TAG_LINKED_ENTITY_UUID);
    }

    private ItemDataFacade() {}
}
