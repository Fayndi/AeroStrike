package net.strike.aerostrike.common.entity.missile;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.strike.aerostrike.common.entity.base.AbstractCruiseMissileEntity;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;

/**
 * Storm Shadow / SCALP-EG - Long-range air-launched stealth cruise missile.
 *
 * Real-world specifications:
 * - Launch Mode: Air-drop (dropped from aircraft, gravity glide, then turbojet ignition).
 * - Propulsion: Microturbo TRI 60-30 turbojet engine (high-subsonic ~1,000 km/h).
 * - Warhead: BROACH multi-stage penetrating warhead (burrows through fortifications before detonation).
 * - Stealth: Faceted low-RCS design for radar avoidance.
 * - Flight Profile: Extreme low-altitude terrain hugging (14 blocks clearance).
 */
public class StormShadowEntity extends AbstractCruiseMissileEntity {

    protected static final RawAnimation FLY_ANIM = RawAnimation.begin().thenLoop("animation.storm_shadow.fly");
    protected static final RawAnimation DEPLOY_ANIM = RawAnimation.begin().thenPlay("animation.storm_shadow.deploy").thenLoop("animation.storm_shadow.fly");
    protected static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("animation.storm_shadow.idle");

    public StormShadowEntity(EntityType<? extends StormShadowEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public float getMaxCruiseSpeed() {
        return 1.4f; // ~28 blocks/second (~1,000 km/h scale)
    }

    @Override
    public float getAcceleration() {
        return 0.06f;
    }

    @Override
    public float getTurnRate() {
        return 4.2f; // Agile aerodynamic steering
    }

    @Override
    public int getBoosterDurationTicks() {
        return 15; // 15 ticks of air-drop before turbojet spools up
    }

    @Override
    public float getCruiseClearance() {
        return 14.0f; // Extreme low-altitude terrain hugging
    }

    @Override
    public float getWarheadYield() {
        return 12.0f; // 450 kg high-yield warhead
    }

    @Override
    public boolean isAirLaunched() {
        return true; // Air-drop profile
    }

    @Override
    public float getRadarSignature() {
        return 0.15f; // Stealth radar-absorbent faceted profile
    }

    @Override
    public int getPenetrationDepth() {
        return 3; // BROACH bunker-buster penetrates 3 blocks before detonation
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "flight_controller", 4, state -> {
            FlightPhase phase = getFlightPhase();
            if (phase == FlightPhase.STANDBY) {
                return state.setAndContinue(IDLE_ANIM);
            } else if (phase == FlightPhase.BOOST) {
                return state.setAndContinue(DEPLOY_ANIM);
            }
            return state.setAndContinue(FLY_ANIM);
        }));
    }
}
