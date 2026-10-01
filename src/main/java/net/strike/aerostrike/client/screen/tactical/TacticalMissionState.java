package net.strike.aerostrike.client.screen.tactical;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.strike.aerostrike.common.item.data.ItemDataFacade;
import net.strike.aerostrike.common.network.ModNetwork;
import net.strike.aerostrike.common.network.packet.c2s.LaunchAirMissilePacket;
import net.strike.aerostrike.core.registry.ModItems;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared state for the Tactical C2 Mission Planning system (Russian Localization).
 */
public class TacticalMissionState {

    private final ItemStack tabletStack;
    private BlockPos targetPos;
    private final List<BlockPos> waypoints = new ArrayList<>();
    private float cruiseAltitude = 14.0f;
    private int salvoCount = 1;
    private boolean addWaypointMode = false;

    private String statusMessage = "СИСТЕМА ГОТОВА // ОЖИДАНИЕ";
    private int statusColor = 0xFF55FF55; // Green
    private long statusMessageExpiry = 0;

    public static final float[] ALTITUDES = {14.0f, 30.0f, 60.0f, 120.0f};
    public static final String[] ALTITUDE_LABELS = {"14м (ПМВ)", "30м (НИЗКИЙ)", "60м (СРЕДНИЙ)", "120м (ВЫСОКИЙ)"};

    public TacticalMissionState(ItemStack tabletStack) {
        this.tabletStack = tabletStack;
    }

    public void load(LocalPlayer player) {
        this.targetPos = ItemDataFacade.getTargetPos(this.tabletStack);
        this.waypoints.clear();
        this.waypoints.addAll(ItemDataFacade.getWaypoints(this.tabletStack));
        this.cruiseAltitude = ItemDataFacade.getCruiseClearance(this.tabletStack, 14.0f);
        this.salvoCount = ItemDataFacade.getSalvoCount(this.tabletStack, 1);

        if (this.targetPos == null && player != null) {
            // Default target: 350 blocks in the direction the player is looking
            double yawRad = Math.toRadians(player.getYRot());
            int defX = (int) (player.getX() - Math.sin(yawRad) * 350);
            int defZ = (int) (player.getZ() + Math.cos(yawRad) * 350);
            int defY = (int) player.getY();
            this.targetPos = new BlockPos(defX, defY, defZ);
            save();
        }
    }

    public void save() {
        if (this.targetPos != null) {
            ItemDataFacade.setTargetPos(this.tabletStack, this.targetPos);
        }
        ItemDataFacade.setWaypoints(this.tabletStack, this.waypoints);
        ItemDataFacade.setCruiseClearance(this.tabletStack, this.cruiseAltitude);
        ItemDataFacade.setSalvoCount(this.tabletStack, this.salvoCount);
    }

    public void cycleAltitude() {
        int currentIndex = 0;
        for (int i = 0; i < ALTITUDES.length; i++) {
            if (Math.abs(ALTITUDES[i] - this.cruiseAltitude) < 0.1f) {
                currentIndex = i;
                break;
            }
        }
        int nextIndex = (currentIndex + 1) % ALTITUDES.length;
        this.cruiseAltitude = ALTITUDES[nextIndex];
        save();
    }

    public String getAltitudeLabel() {
        for (int i = 0; i < ALTITUDES.length; i++) {
            if (Math.abs(ALTITUDES[i] - this.cruiseAltitude) < 0.1f) {
                return ALTITUDE_LABELS[i];
            }
        }
        return (int) this.cruiseAltitude + "м";
    }

    public void cycleSalvo() {
        if (this.salvoCount == 1) {
            this.salvoCount = 2;
        } else if (this.salvoCount == 2) {
            this.salvoCount = 4;
        } else {
            this.salvoCount = 1;
        }
        save();
    }

    public String getSalvoLabel() {
        return switch (this.salvoCount) {
            case 2 -> "2x (ПАРА)";
            case 4 -> "4x (ЗАЛП)";
            default -> "1x (ОДИНОЧНЫЙ)";
        };
    }

    public void toggleAddWaypointMode() {
        this.addWaypointMode = !this.addWaypointMode;
    }

    public boolean isAddWaypointMode() {
        return this.addWaypointMode;
    }

    public void clearWaypoints() {
        this.waypoints.clear();
        save();
        setStatus("МАРШРУТ ОЧИЩЕН", 0xFFFFAA00);
    }

    public void removeLastWaypoint() {
        if (!this.waypoints.isEmpty()) {
            this.waypoints.remove(this.waypoints.size() - 1);
            save();
            setStatus("ПОСЛЕДНЯЯ ТОЧКА УДАЛЕНА", 0xFFFFAA00);
        }
    }

    public void addWaypoint(BlockPos pos) {
        this.waypoints.add(pos);
        save();
        setStatus("ППМ-" + String.format("%02d", this.waypoints.size()) + " ДОБАВЛЕНА: [" + pos.getX() + ", " + pos.getZ() + "]", 0xFF55FFFF);
    }

    public void setTarget(BlockPos pos) {
        this.targetPos = pos;
        save();
        setStatus("ЦЕЛЬ ЗАХВАЧЕНА: [" + pos.getX() + ", " + pos.getZ() + "]", 0xFFFF5555);
    }

    public double calculateTotalDistance(Vec3 origin) {
        if (this.targetPos == null) return 0;
        double dist = 0;
        Vec3 current = origin;

        for (BlockPos wp : this.waypoints) {
            Vec3 wpVec = new Vec3(wp.getX() + 0.5, wp.getY(), wp.getZ() + 0.5);
            dist += current.distanceTo(wpVec);
            current = wpVec;
        }

        Vec3 targetVec = new Vec3(this.targetPos.getX() + 0.5, this.targetPos.getY(), this.targetPos.getZ() + 0.5);
        dist += current.distanceTo(targetVec);
        return dist;
    }

    public int calculateEtaSeconds(double totalDistBlocks) {
        return (int) Math.max(1, totalDistBlocks / 35.0);
    }

    public int getAvailablePayloadCount(LocalPlayer player) {
        if (player == null) return 0;
        if (player.getAbilities().instabuild) return 99;
        int count = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(ModItems.STORM_SHADOW_ITEM.get())) {
                count += stack.getCount();
            }
        }
        return count;
    }

    public boolean executeLaunch(LocalPlayer player) {
        if (player == null || this.targetPos == null) {
            setStatus("ОШИБКА: НЕТ НАЗНАЧЕННОЙ ЦЕЛИ", 0xFFFF3333);
            if (player != null) player.playSound(SoundEvents.NOTE_BLOCK_BASS.value(), 1.0f, 0.5f);
            return false;
        }

        int available = getAvailablePayloadCount(player);
        if (available <= 0) {
            setStatus("ОТМЕНА: НЕТ STORM SHADOW В ИНВЕНТАРЕ", 0xFFFF3333);
            player.playSound(SoundEvents.NOTE_BLOCK_BASS.value(), 1.0f, 0.5f);
            return false;
        }

        int toFire = Math.min(this.salvoCount, available);

        // Send launch packet
        ModNetwork.CHANNEL.sendToServer(new LaunchAirMissilePacket(
                this.targetPos,
                this.waypoints,
                this.cruiseAltitude,
                toFire
        ));

        save();
        setStatus("СБРОС ЗАЛПА РАКЕТ АВТОРИЗОВАН", 0xFF55FF55);
        player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 1.0f, 1.4f);
        return true;
    }

    public void setStatus(String message, int color) {
        this.statusMessage = message;
        this.statusColor = color;
        this.statusMessageExpiry = System.currentTimeMillis() + 4500;
    }

    public String getStatusMessage() {
        if (System.currentTimeMillis() > this.statusMessageExpiry) {
            return this.addWaypointMode ? "КЛИКНИТЕ ДЛЯ ВВОДА ТОЧКИ (ППМ)" : "КЛИКНИТЕ ДЛЯ ЗАХВАТА ЦЕЛИ";
        }
        return this.statusMessage;
    }

    public int getStatusColor() {
        if (System.currentTimeMillis() > this.statusMessageExpiry) {
            return this.addWaypointMode ? 0xFFFFAA00 : 0xFF88CCFF;
        }
        return this.statusColor;
    }

    public ItemStack getTabletStack() {
        return this.tabletStack;
    }

    public BlockPos getTargetPos() {
        return this.targetPos;
    }

    public List<BlockPos> getWaypoints() {
        return this.waypoints;
    }

    public float getCruiseAltitude() {
        return this.cruiseAltitude;
    }

    public int getSalvoCount() {
        return this.salvoCount;
    }
}
