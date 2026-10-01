package net.strike.aerostrike.common.item.tablet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.strike.aerostrike.client.screen.TabletOpener;
import net.strike.aerostrike.common.item.data.ItemDataFacade;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Military Terminal Tablet (MFD).
 *
 * Used to plan missile routes, set intermediate waypoints, configure cruising altitude,
 * manage salvo drops, and execute air-launches (Storm Shadow).
 */
public class MilitaryTabletItem extends Item {

    public MilitaryTabletItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> TabletOpener.open(stack));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§7Тактический терминал планирования ударов"));
        BlockPos target = ItemDataFacade.getTargetPos(stack);
        if (target != null) {
            tooltip.add(Component.literal("§aЦель: §f" + target.getX() + ", " + target.getY() + ", " + target.getZ()));
            List<BlockPos> waypoints = ItemDataFacade.getWaypoints(stack);
            if (!waypoints.isEmpty()) {
                tooltip.add(Component.literal("§6ППМ (Waypoints): §e" + waypoints.size() + " точек"));
            }
            float alt = ItemDataFacade.getCruiseClearance(stack, 15.0f);
            tooltip.add(Component.literal("§bЭшелон: §f" + (int) alt + "м"));
        } else {
            tooltip.add(Component.literal("§eПКМ — открыть тактический экран и радар"));
        }
    }
}
