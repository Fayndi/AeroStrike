package net.strike.aerostrike.client.screen.xaero;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.strike.aerostrike.AeroStrike;
import net.strike.aerostrike.client.screen.tactical.TacticalHudRenderer;
import net.strike.aerostrike.client.screen.tactical.TacticalMissionState;
import net.strike.aerostrike.client.tracker.ClientMissileTracker;
import xaero.map.MapProcessor;
import xaero.map.gui.GuiMap;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Fullscreen Military Tactical C2 Screen powered by Xaero's World Map.
 * Completely localized in Russian, supports real-time missile tracking,
 * button hover tooltips, and precise synchronized world-to-screen coordinate math.
 */
public class TacticalXaeroMapScreen extends GuiMap {

    private final TacticalMissionState missionState;
    private final ItemStack tabletStack;

    // Reflection handles to Xaero's camera and scaling fields
    private static Field CAMERA_X_FIELD;
    private static Field CAMERA_Z_FIELD;
    private static Field SCALE_FIELD;
    private static Field SCREEN_SCALE_FIELD;

    static {
        try {
            for (Field f : GuiMap.class.getDeclaredFields()) {
                f.setAccessible(true);
                if ("cameraX".equals(f.getName())) CAMERA_X_FIELD = f;
                else if ("cameraZ".equals(f.getName())) CAMERA_Z_FIELD = f;
                else if ("scale".equals(f.getName())) SCALE_FIELD = f;
                else if ("screenScale".equals(f.getName())) SCREEN_SCALE_FIELD = f;
            }
        } catch (Exception e) {
            AeroStrike.LOGGER.error("[AeroStrike] Failed to bind Xaero GuiMap reflection fields", e);
        }
    }

    // Buttons
    private Button addWpButton;
    private Button clearRouteButton;
    private Button undoWpButton;
    private Button altButton;
    private Button salvoButton;
    private Button centerButton;
    private Button launchButton;

    // Panel Geometry
    private int panelW = 215;
    private int panelX;
    private int panelY = 28;
    private int panelH;

    public TacticalXaeroMapScreen(
            Screen parent,
            Screen escape,
            MapProcessor mapProcessor,
            Entity player,
            ItemStack tabletStack
    ) {
        super(parent, escape, mapProcessor, player);
        this.tabletStack = tabletStack;
        this.missionState = new TacticalMissionState(tabletStack);
    }

    @Override
    public void init() {
        super.init();

        // Clear civilian Xaero buttons to provide an uncluttered military MFD
        this.clearWidgets();

        LocalPlayer player = Minecraft.getInstance().player;
        this.missionState.load(player);

        this.panelX = this.width - this.panelW - 10;
        this.panelH = this.height - 38;

        int btnX = this.panelX + 10;
        int btnW = this.panelW - 20;
        int curY = this.panelY + 115;

        // Button: Add Waypoint Toggle with Russian Tooltip
        this.addWpButton = this.addRenderableWidget(Button.builder(
                getAddWpText(),
                b -> {
                    this.missionState.toggleAddWaypointMode();
                    b.setMessage(getAddWpText());
                }
        ).tooltip(Tooltip.create(Component.literal("Включение режима ввода промежуточных точек маршрута (ППМ).\nКликните ЛКМ по карте для добавления точки.\nПКМ по точке — удаляет её.")))
        .bounds(btnX, curY, btnW, 18).build());
        curY += 21;

        // Button: Undo Last WP & Clear
        int halfW = (btnW - 4) / 2;
        this.undoWpButton = this.addRenderableWidget(Button.builder(
                Component.literal("§e< УДАЛИТЬ"),
                b -> this.missionState.removeLastWaypoint()
        ).tooltip(Tooltip.create(Component.literal("Удалить последнюю добавленную точку маршрута.")))
        .bounds(btnX, curY, halfW, 18).build());

        this.clearRouteButton = this.addRenderableWidget(Button.builder(
                Component.literal("§cСБРОСИТЬ"),
                b -> this.missionState.clearWaypoints()
        ).tooltip(Tooltip.create(Component.literal("Полностью сбросить маршрут и удалить все промежуточные точки.")))
        .bounds(btnX + halfW + 4, curY, halfW, 18).build());
        curY += 24;

        // Button: Cruise Altitude with Russian Tooltip
        this.altButton = this.addRenderableWidget(Button.builder(
                getAltText(),
                b -> {
                    this.missionState.cycleAltitude();
                    b.setMessage(getAltText());
                }
        ).tooltip(Tooltip.create(Component.literal("Выбор высоты крейсерского полета (эшелона):\n• 14м (ПМВ) — Сверхмалая высота, огибание рельефа, скрытность от радаров ПВО.\n• 30м — Низкий эшелон.\n• 60м — Средний эшелон.\n• 120м — Высотный полет над лесами и горами.")))
        .bounds(btnX, curY, btnW, 18).build());
        curY += 21;

        // Button: Salvo Size with Russian Tooltip
        this.salvoButton = this.addRenderableWidget(Button.builder(
                getSalvoText(),
                b -> {
                    this.missionState.cycleSalvo();
                    b.setMessage(getSalvoText());
                }
        ).tooltip(Tooltip.create(Component.literal("Количество выпускаемых ракет в одном залпе:\n• 1x — Одиночный пуск.\n• 2x — Пуск парой с интервалом.\n• 4x — Батарейный залп для прорыва ПВО.")))
        .bounds(btnX, curY, btnW, 18).build());
        curY += 21;

        // Button: Center on Aircraft with Russian Tooltip
        this.centerButton = this.addRenderableWidget(Button.builder(
                Component.literal("§fЦЕНТРИРОВАТЬ НА САМОЛЕТЕ"),
                b -> centerOnPlayer()
        ).tooltip(Tooltip.create(Component.literal("Переместить камеру карты на текущую позицию вашего самолета/персонажа.")))
        .bounds(btnX, curY, btnW, 18).build());
        curY += 26;

        // Button: BIG LAUNCH MISSILE with Russian Tooltip
        this.launchButton = this.addRenderableWidget(Button.builder(
                Component.literal("§c§l>>> ПУСК КРЫЛАТЫХ РАКЕТ <<<"),
                b -> {
                    boolean success = this.missionState.executeLaunch(Minecraft.getInstance().player);
                    if (success) {
                        this.onClose();
                    }
                }
        ).tooltip(Tooltip.create(Component.literal("АВТОРИЗАЦИЯ И СБРОС РАКЕТ:\nПроизводит немедленный сброс выбранного числа ракет Storm Shadow из инвентаря.\nРакеты ложатся на запрограммированный маршрут полета.")))
        .bounds(btnX, curY, btnW, 24).build());
    }

    private Component getAddWpText() {
        return this.missionState.isAddWaypointMode()
                ? Component.literal("§e[+ ТОЧКИ: АКТИВНО]")
                : Component.literal("§7[+ ВВОД ТОЧЕК (ППМ)]");
    }

    private Component getAltText() {
        return Component.literal("§bЭШЕЛОН: §f" + this.missionState.getAltitudeLabel());
    }

    private Component getSalvoText() {
        return Component.literal("§dЗАЛП: §f" + this.missionState.getSalvoLabel());
    }

    private void centerOnPlayer() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && CAMERA_X_FIELD != null && CAMERA_Z_FIELD != null) {
            try {
                CAMERA_X_FIELD.setDouble(this, player.getX());
                CAMERA_Z_FIELD.setDouble(this, player.getZ());
            } catch (Exception ignored) {}
        }
    }

    public double getCameraX() {
        try {
            if (CAMERA_X_FIELD != null) return CAMERA_X_FIELD.getDouble(this);
        } catch (Exception ignored) {}
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null ? player.getX() : 0;
    }

    public double getCameraZ() {
        try {
            if (CAMERA_Z_FIELD != null) return CAMERA_Z_FIELD.getDouble(this);
        } catch (Exception ignored) {}
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null ? player.getZ() : 0;
    }

    public double getEffectiveScale() {
        try {
            double rawScale = (SCALE_FIELD != null) ? SCALE_FIELD.getDouble(this) : 1.0;
            double sScale = (SCREEN_SCALE_FIELD != null) ? SCREEN_SCALE_FIELD.getDouble(this) : 0.0;
            if (sScale <= 0) {
                sScale = Minecraft.getInstance().getWindow().getGuiScale();
            }
            return rawScale / sScale;
        } catch (Exception e) {
            return 1.0;
        }
    }

    public double worldXToScreen(double wx) {
        return (wx - getCameraX()) * getEffectiveScale() + this.width / 2.0;
    }

    public double worldZToScreen(double wz) {
        return (wz - getCameraZ()) * getEffectiveScale() + this.height / 2.0;
    }

    public double screenToWorldX(double sx) {
        return (sx - this.width / 2.0) / getEffectiveScale() + getCameraX();
    }

    public double screenToWorldZ(double sy) {
        return (sy - this.height / 2.0) / getEffectiveScale() + getCameraZ();
    }

    private boolean isInsideControlPanel(double mouseX, double mouseY) {
        return mouseX >= this.panelX && mouseX <= this.panelX + this.panelW &&
               mouseY >= this.panelY && mouseY <= this.panelY + this.panelH;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        // 1. Render Xaero's Live World Map Background
        super.render(guiGraphics, mouseX, mouseY, partialTicks);

        LocalPlayer player = Minecraft.getInstance().player;

        // 2. Render Tactical Flight Path Route
        List<TacticalHudRenderer.ScreenPoint> routePoints = new ArrayList<>();
        if (player != null) {
            routePoints.add(new TacticalHudRenderer.ScreenPoint(
                    worldXToScreen(player.getX()),
                    worldZToScreen(player.getZ())
            ));
        }
        for (BlockPos wp : this.missionState.getWaypoints()) {
            routePoints.add(new TacticalHudRenderer.ScreenPoint(
                    worldXToScreen(wp.getX() + 0.5),
                    worldZToScreen(wp.getZ() + 0.5)
            ));
        }
        BlockPos target = this.missionState.getTargetPos();
        if (target != null) {
            routePoints.add(new TacticalHudRenderer.ScreenPoint(
                    worldXToScreen(target.getX() + 0.5),
                    worldZToScreen(target.getZ() + 0.5)
            ));
        }

        TacticalHudRenderer.drawFlightRoute(guiGraphics, routePoints);

        // 3. Render Waypoint Markers
        List<BlockPos> waypoints = this.missionState.getWaypoints();
        for (int i = 0; i < waypoints.size(); i++) {
            BlockPos wp = waypoints.get(i);
            double sx = worldXToScreen(wp.getX() + 0.5);
            double sy = worldZToScreen(wp.getZ() + 0.5);
            TacticalHudRenderer.drawWaypointMarker(guiGraphics, this.font, sx, sy, i, wp);
        }

        // 4. Render Target Lock Reticle
        if (target != null) {
            double sx = worldXToScreen(target.getX() + 0.5);
            double sy = worldZToScreen(target.getZ() + 0.5);
            double dist = player != null ? Math.sqrt(player.distanceToSqr(target.getX(), player.getY(), target.getZ())) : 0;
            TacticalHudRenderer.drawTargetReticle(guiGraphics, this.font, sx, sy, target, dist);
        }

        // 5. Render Real-Time Tracked Active Missiles
        for (ClientMissileTracker.TrackedMissile missile : ClientMissileTracker.getActiveMissiles()) {
            double mx = worldXToScreen(missile.x());
            double my = worldZToScreen(missile.z());
            TacticalHudRenderer.drawTrackedMissile(guiGraphics, this.font, mx, my, missile);
        }

        // 6. Render Host Aircraft Marker
        if (player != null) {
            double px = worldXToScreen(player.getX());
            double py = worldZToScreen(player.getZ());
            TacticalHudRenderer.drawPlayerMarker(guiGraphics, this.font, px, py, player.getYRot());
        }

        // 7. Render Top Status Bar
        int cursorWorldX = (int) Math.floor(screenToWorldX(mouseX));
        int cursorWorldZ = (int) Math.floor(screenToWorldZ(mouseY));
        TacticalHudRenderer.drawTopStatusBar(
                guiGraphics,
                this.font,
                this.width,
                cursorWorldX,
                cursorWorldZ,
                getEffectiveScale(),
                this.missionState
        );

        // 8. Render Side Control Panel Frame
        TacticalHudRenderer.drawControlPanelFrame(
                guiGraphics,
                this.font,
                this.panelX, this.panelY, this.panelW, this.panelH,
                player,
                this.missionState
        );

        // 9. Render Widgets / Buttons & Tooltips
        for (GuiEventListener child : this.children()) {
            if (child instanceof Button btn) {
                btn.render(guiGraphics, mouseX, mouseY, partialTicks);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isInsideControlPanel(mouseX, mouseY) || mouseY < 24) {
            for (GuiEventListener listener : this.children()) {
                if (listener.mouseClicked(mouseX, mouseY, button)) {
                    return true;
                }
            }
            return true;
        }

        int blockX = (int) Math.floor(screenToWorldX(mouseX));
        int blockZ = (int) Math.floor(screenToWorldZ(mouseY));
        LocalPlayer player = Minecraft.getInstance().player;
        int playerY = player != null ? (int) player.getY() : 64;

        if (button == 0) { // Left Click
            if (this.missionState.isAddWaypointMode() || Screen.hasShiftDown()) {
                this.missionState.addWaypoint(new BlockPos(blockX, playerY, blockZ));
                if (player != null) player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 1.0f, 1.6f);
                return true;
            } else {
                this.missionState.setTarget(new BlockPos(blockX, playerY, blockZ));
                if (player != null) player.playSound(SoundEvents.ARROW_HIT_PLAYER, 0.8f, 1.2f);
                return super.mouseClicked(mouseX, mouseY, button);
            }
        } else if (button == 1) { // Right Click (Bypasses civilian Xaero menu!)
            List<BlockPos> waypoints = this.missionState.getWaypoints();
            for (int i = waypoints.size() - 1; i >= 0; i--) {
                BlockPos wp = waypoints.get(i);
                double sx = worldXToScreen(wp.getX() + 0.5);
                double sy = worldZToScreen(wp.getZ() + 0.5);
                if (Math.abs(sx - mouseX) <= 12 && Math.abs(sy - mouseY) <= 12) {
                    this.missionState.getWaypoints().remove(i);
                    this.missionState.save();
                    this.missionState.setStatus("ТОЧКА ППМ-" + (i + 1) + " УДАЛЕНА", 0xFFFFAA00);
                    if (player != null) player.playSound(SoundEvents.DISPENSER_FAIL, 0.8f, 1.4f);
                    return true;
                }
            }

            if (this.missionState.isAddWaypointMode() || Screen.hasShiftDown()) {
                this.missionState.addWaypoint(new BlockPos(blockX, playerY, blockZ));
                if (player != null) player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 1.0f, 1.6f);
            } else {
                this.missionState.setTarget(new BlockPos(blockX, playerY, blockZ));
                if (player != null) player.playSound(SoundEvents.ARROW_HIT_PLAYER, 0.8f, 1.2f);
            }
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (isInsideControlPanel(mouseX, mouseY)) {
            return false;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public void removed() {
        this.missionState.save();
        super.removed();
    }
}
