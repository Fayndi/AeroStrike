package net.strike.aerostrike.core.registry;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.strike.aerostrike.AeroStrike;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, AeroStrike.MOD_ID);

    public static final RegistryObject<SoundEvent> JET_ENGINE_LOOP = registerSound("jet_engine_loop");
    public static final RegistryObject<SoundEvent> ROCKET_LAUNCH = registerSound("rocket_launch");
    public static final RegistryObject<SoundEvent> SONIC_BOOM = registerSound("sonic_boom");
    public static final RegistryObject<SoundEvent> HEAVY_EXPLOSION = registerSound("heavy_explosion");
    public static final RegistryObject<SoundEvent> RADAR_PING = registerSound("radar_ping");
    public static final RegistryObject<SoundEvent> RADAR_ALARM = registerSound("radar_alarm");
    public static final RegistryObject<SoundEvent> DRONE_MOTOR_LOOP = registerSound("drone_motor_loop");

    private static RegistryObject<SoundEvent> registerSound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(AeroStrike.MOD_ID, name)));
    }

    private ModSounds() {}
}
