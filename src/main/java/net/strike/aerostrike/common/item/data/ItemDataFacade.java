package net.strike.aerostrike.common.item.data;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Data Facade / Adapter Layer.
 *
 * In Minecraft 1.20.1 Forge, item custom data is stored in ItemStack NBT tags.
 * In Minecraft 1.21.1 NeoForge, NBT is completely replaced by DataComponentType.
 *
 * By isolating all read/write logic for target coordinates, waypoints, frequencies,
 * and entity bindings into this facade, transitioning to 1.21.1 will require updating
 * only this single class without modifying any entity, GUI, or item logic.
 */
public final class ItemDataFacade {
    private static final String TAG_TARGET_POS = "TargetPos";
    private static final String TAG_WAYPOINTS = "Waypoints";
    private static final String TAG_CRUISE_CLEARANCE = "CruiseClearance";
    private static final String TAG_SALVO_COUNT = "SalvoCount";
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

    public static void setWaypoints(ItemStack stack, List<BlockPos> waypoints) {
        CompoundTag tag = stack.getOrCreateTag();
        ListTag list = new ListTag();
        for (BlockPos wp : waypoints) {
            list.add(NbtUtils.writeBlockPos(wp));
        }
        tag.put(TAG_WAYPOINTS, list);
    }

    public static List<BlockPos> getWaypoints(ItemStack stack) {
        List<BlockPos> result = new ArrayList<>();
        if (stack.hasTag() && stack.getTag().contains(TAG_WAYPOINTS, Tag.TAG_LIST)) {
            ListTag list = stack.getTag().getList(TAG_WAYPOINTS, Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                result.add(NbtUtils.readBlockPos(list.getCompound(i)));
            }
        }
        return result;
    }

    public static void setCruiseClearance(ItemStack stack, float clearance) {
        stack.getOrCreateTag().putFloat(TAG_CRUISE_CLEARANCE, clearance);
    }

    public static float getCruiseClearance(ItemStack stack, float defaultClearance) {
        if (!stack.hasTag() || !stack.getTag().contains(TAG_CRUISE_CLEARANCE)) {
            return defaultClearance;
        }
        return stack.getTag().getFloat(TAG_CRUISE_CLEARANCE);
    }

    public static void setSalvoCount(ItemStack stack, int count) {
        stack.getOrCreateTag().putInt(TAG_SALVO_COUNT, count);
    }

    public static int getSalvoCount(ItemStack stack, int defaultCount) {
        if (!stack.hasTag() || !stack.getTag().contains(TAG_SALVO_COUNT)) {
            return defaultCount;
        }
        return stack.getTag().getInt(TAG_SALVO_COUNT);
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
