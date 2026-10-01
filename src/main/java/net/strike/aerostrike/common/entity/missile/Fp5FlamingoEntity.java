package net.strike.aerostrike.common.entity.missile;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.strike.aerostrike.common.entity.base.AbstractCruiseMissileEntity;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

/**
 * FP-5 Flamingo - Heavy ground-launched straight-wing cruise missile.
 *
 * Real-world specifications implemented:
 * - Warhead: 1,150 kg heavy warhead (high yield blast radius).
 * - Propulsion: Solid rocket booster launch phase + AI-25TL turbofan cruise engine.
 * - Cruise Speed: ~900 km/h (scaled to Minecraft: ~1.2 blocks/tick).
 * - Turning radius: realistic inertia with limited degrees per tick.
 */
public class Fp5FlamingoEntity extends AbstractCruiseMissileEntity {

    protected static final RawAnimation FLY_ANIM = RawAnimation.begin().thenLoop("animation.flamingo.fly");
    protected static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("animation.flamingo.idle");

    public Fp5FlamingoEntity(EntityType<? extends Fp5FlamingoEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public float getMaxCruiseSpeed() {
        return 1.25f; // ~25 blocks per second
    }

    @Override
    public float getAcceleration() {
        return 0.04f;
    }

    @Override
    public float getTurnRate() {
        return 3.2f; // Realistic turn rate in degrees per tick
    }

    @Override
    public int getBoosterDurationTicks() {
        return 40; // 2 seconds on solid rocket booster
    }

    @Override
    public float getCruiseClearance() {
        return 22.0f; // 22 blocks above obstacles/ground
    }

    @Override
    public float getWarheadYield() {
        return 16.0f; // 1,150 kg warhead yield
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main_controller", 5, state -> {
            if (getFlightPhase() != FlightPhase.STANDBY) {
                return state.setAndContinue(FLY_ANIM);
            }
            return state.setAndContinue(IDLE_ANIM);
        }));
    }
}
