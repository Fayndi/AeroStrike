package net.strike.aerostrike.common.item.missile;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.strike.aerostrike.common.entity.base.AbstractCruiseMissileEntity;
import net.strike.aerostrike.common.item.data.ItemDataFacade;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Supplier;

/**
 * Item used to deploy and launch cruise missiles.
 */
public class CruiseMissileItem<T extends AbstractCruiseMissileEntity> extends Item {

    private final Supplier<EntityType<T>> entityTypeSupplier;

    public CruiseMissileItem(Supplier<EntityType<T>> entityTypeSupplier, Properties properties) {
        super(properties);
        this.entityTypeSupplier = entityTypeSupplier;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        Player player = context.getPlayer();
        BlockPos clickedPos = context.getClickedPos();
        Direction clickedFace = context.getClickedFace();
        BlockPos spawnPos = clickedPos.relative(clickedFace);
        ItemStack stack = context.getItemInHand();

        if (level instanceof ServerLevel serverLevel) {
            EntityType<T> type = this.entityTypeSupplier.get();
            T missile = type.create(serverLevel);
            if (missile == null) {
                return InteractionResult.FAIL;
            }

            missile.setPos(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);

            // Read target from ItemDataFacade
            BlockPos targetPos = ItemDataFacade.getTargetPos(stack);

            if (targetPos != null) {
                // If programmed with target coordinates, launch directly!
                serverLevel.addFreshEntity(missile);
                missile.launch(targetPos);
                if (player != null) {
                    player.displayClientMessage(
                            Component.translatable("message.aerostrike.missile_launched",
                                    targetPos.getX(), targetPos.getY(), targetPos.getZ()),
                            true
                    );
                }
            } else {
                // Default test launch: 250 blocks forward along player's look vector
                if (player != null) {
                    double yawRad = Math.toRadians(player.getYRot());
                    int targetX = (int) (spawnPos.getX() - Math.sin(yawRad) * 250.0);
                    int targetZ = (int) (spawnPos.getZ() + Math.cos(yawRad) * 250.0);
                    BlockPos autoTarget = new BlockPos(targetX, spawnPos.getY(), targetZ);

                    serverLevel.addFreshEntity(missile);
                    missile.launch(autoTarget);

                    player.displayClientMessage(
                            Component.literal("§6[AeroStrike] §fFP-5 Flamingo запущена по азимуту! Цель: §e" +
                                    autoTarget.getX() + ", " + autoTarget.getY() + ", " + autoTarget.getZ()),
                            true
                    );
                } else {
                    serverLevel.addFreshEntity(missile);
                }
            }

            level.playSound(null, spawnPos, SoundEvents.ARMOR_EQUIP_NETHERITE, SoundSource.BLOCKS, 1.0f, 1.0f);

            if (player != null && !player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }

        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        BlockPos target = ItemDataFacade.getTargetPos(stack);
        if (target != null) {
            tooltip.add(Component.literal("§aЦель: §f" + target.getX() + ", " + target.getY() + ", " + target.getZ()));
        } else {
            tooltip.add(Component.literal("§7ПКМ по блоку — запуск на 250 блоков вперед по направлению взгляда"));
            tooltip.add(Component.literal("§8(Координаты можно запрограммировать планшетом/лазером)"));
        }
    }
}
