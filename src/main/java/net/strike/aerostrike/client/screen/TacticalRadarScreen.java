package net.strike.aerostrike.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.strike.aerostrike.client.screen.tactical.TacticalHudRenderer;
import net.strike.aerostrike.client.screen.tactical.TacticalMissionState;
import net.strike.aerostrike.client.tracker.ClientMissileTracker;

import java.util.ArrayList;
import java.util.List;

/**
 * Fullscreen Standalone Tactical Radar Screen (Russian Localization).
 * Provides pan & zoom, range rings, cardinal compass, active missile tracking,
 * and button hover tooltips.
 */
@OnlyIn(Dist.CLIENT)
public class TacticalRadarScreen extends Screen {

    private final TacticalMissionState missionState;
    private final ItemStack tabletStack;

    // Pan & Zoom Engine
    private double panWorldX;
    private double panWorldZ;
    private double zoom = 0.4; // Pixels per block
    private boolean isDragging = false;
    private double lastDragMouseX;
    private double lastDragMouseY;

    // Side Control Panel Geometry
    private int panelW = 215;
    private int panelX;
    private int panelY = 28;
    private int panelH;

    // Buttons
    private Button addWpButton;
    private Button clearRouteButton;
    private Button undoWpButton;
    private Button altButton;
    private Button salvoButton;
    private Button centerButton;
    private Button launchButton;

    public TacticalRadarScreen(ItemStack tabletStack) {
        super(Component.literal("AeroStrike Тактический Радар C2"));
        this.tabletStack = tabletStack;
        this.missionState = new TacticalMissionState(tabletStack);
    }

    @Override
    protected void init() {
        super.init();

        LocalPlayer player = Minecraft.getInstance().player;
        this.missionState.load(player);

        if (player != null) {
            this.panWorldX = player.getX();
            this.panWorldZ = player.getZ();
        }

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
        ).tooltip(Tooltip.create(Component.literal("Включение режима ввода промежуточных точек маршрута (ППМ).\nКликните ЛКМ по радару для добавления точки.\nПКМ по точке — удаляет её.")))
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
        ).tooltip(Tooltip.create(Component.literal("Переместить радар на текущую позицию вашего самолета/персонажа.")))
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
        if (player != null) {
            this.panWorldX = player.getX();
            this.panWorldZ = player.getZ();
        }
    }

    public double worldXToScreen(double wx) {
        return (wx - this.panWorldX) * this.zoom + this.width / 2.0;
    }

    public double worldZToScreen(double wz) {
        return (wz - this.panWorldZ) * this.zoom + this.height / 2.0;
    }

    public double screenToWorldX(double sx) {
        return (sx - this.width / 2.0) / this.zoom + this.panWorldX;
    }

    public double screenToWorldZ(double sy) {
        return (sy - this.height / 2.0) / this.zoom + this.panWorldZ;
    }

    private boolean isInsideControlPanel(double mouseX, double mouseY) {
        return mouseX >= this.panelX && mouseX <= this.panelX + this.panelW &&
               mouseY >= this.panelY && mouseY <= this.panelY + this.panelH;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        // 1. Dark CRT Tactical Canvas
        guiGraphics.fill(0, 0, this.width, this.height, 0xFF050B10);

        LocalPlayer player = Minecraft.getInstance().player;

        // 2. Render Tactical Vector Grid & Range Rings
        renderRadarGrid(guiGraphics, player);

        // 3. Render Flight Route Vector
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

        // 4. Render Waypoint Markers
        List<BlockPos> waypoints = this.missionState.getWaypoints();
        for (int i = 0; i < waypoints.size(); i++) {
            BlockPos wp = waypoints.get(i);
            double sx = worldXToScreen(wp.getX() + 0.5);
            double sy = worldZToScreen(wp.getZ() + 0.5);
            TacticalHudRenderer.drawWaypointMarker(guiGraphics, this.font, sx, sy, i, wp);
        }

        // 5. Render Target Lock Reticle
        if (target != null) {
            double sx = worldXToScreen(target.getX() + 0.5);
            double sy = worldZToScreen(target.getZ() + 0.5);
            double dist = player != null ? Math.sqrt(player.distanceToSqr(target.getX(), player.getY(), target.getZ())) : 0;
            TacticalHudRenderer.drawTargetReticle(guiGraphics, this.font, sx, sy, target, dist);
        }

        // 6. Render Real-Time Tracked Active Missiles
        for (ClientMissileTracker.TrackedMissile missile : ClientMissileTracker.getActiveMissiles()) {
            double mx = worldXToScreen(missile.x());
            double my = worldZToScreen(missile.z());
            TacticalHudRenderer.drawTrackedMissile(guiGraphics, this.font, mx, my, missile);
        }

        // 7. Render Host Aircraft Marker
        if (player != null) {
            double px = worldXToScreen(player.getX());
            double py = worldZToScreen(player.getZ());
            TacticalHudRenderer.drawPlayerMarker(guiGraphics, this.font, px, py, player.getYRot());
        }

        // 8. Render Top Tactical Status Bar
        int cursorWorldX = (int) Math.floor(screenToWorldX(mouseX));
        int cursorWorldZ = (int) Math.floor(screenToWorldZ(mouseY));
        TacticalHudRenderer.drawTopStatusBar(
                guiGraphics,
                this.font,
                this.width,
                cursorWorldX,
                cursorWorldZ,
                this.zoom,
                this.missionState
        );

        // 9. Render Side Control Panel Frame
        TacticalHudRenderer.drawControlPanelFrame(
                guiGraphics,
                this.font,
                this.panelX, this.panelY, this.panelW, this.panelH,
                player,
                this.missionState
        );

        // 10. Render Widgets / Buttons & Tooltips
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    private void renderRadarGrid(GuiGraphics graphics, LocalPlayer player) {
        int step = (this.zoom > 0.8) ? 100 : (this.zoom > 0.3 ? 250 : 500);

        int minWorldX = (int) screenToWorldX(0);
        int maxWorldX = (int) screenToWorldX(this.width);
        int minWorldZ = (int) screenToWorldZ(0);
        int maxWorldZ = (int) screenToWorldZ(this.height);

        int startX = (minWorldX / step) * step;
        int startZ = (minWorldZ / step) * step;

        // Draw Vertical Grid Lines
        for (int x = startX; x <= maxWorldX; x += step) {
            int sx = (int) worldXToScreen(x);
            boolean isMajor = (x % (step * 2) == 0);
            int lineColor = isMajor ? 0x2E00FF88 : 0x1400FF88;
            graphics.fill(sx, 24, sx + 1, this.height, lineColor);
            if (isMajor && this.height > 60) {
                graphics.drawString(this.font, String.valueOf(x), sx + 3, 28, 0x5500FF88, false);
            }
        }

        // Draw Horizontal Grid Lines
        for (int z = startZ; z <= maxWorldZ; z += step) {
            int sy = (int) worldZToScreen(z);
            boolean isMajor = (z % (step * 2) == 0);
            int lineColor = isMajor ? 0x2E00FF88 : 0x1400FF88;
            graphics.fill(0, sy, this.width, sy + 1, lineColor);
            if (isMajor && sy > 35 && sy < this.height - 10) {
                graphics.drawString(this.font, String.valueOf(z), 6, sy - 8, 0x5500FF88, false);
            }
        }

        // Concentric Radar Rings from Player
        if (player != null) {
            double px = worldXToScreen(player.getX());
            double py = worldZToScreen(player.getZ());
            int[] ringDistances = {250, 500, 1000, 2000, 4000};
            for (int dist : ringDistances) {
                int rPx = (int) (dist * this.zoom);
                if (rPx > 10 && rPx < Math.max(this.width, this.height) * 2) {
                    TacticalHudRenderer.drawCircle(graphics, (int) px, (int) py, rPx, 0x2200FF88);
                    graphics.drawString(this.font, dist + "м", (int) px + rPx + 3, (int) py - 4, 0x4400FF88, false);
                }
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isInsideControlPanel(mouseX, mouseY) || mouseY < 24) {
            return super.mouseClicked(mouseX, mouseY, button);
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
                this.isDragging = true;
                this.lastDragMouseX = mouseX;
                this.lastDragMouseY = mouseY;
                return true;
            }
        } else if (button == 1) { // Right Click
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
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            this.isDragging = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.isDragging && button == 0) {
            double deltaX = mouseX - this.lastDragMouseX;
            double deltaY = mouseY - this.lastDragMouseY;
            this.panWorldX -= deltaX / this.zoom;
            this.panWorldZ -= deltaY / this.zoom;
            this.lastDragMouseX = mouseX;
            this.lastDragMouseY = mouseY;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (isInsideControlPanel(mouseX, mouseY)) {
            return false;
        }
        double factor = (delta > 0) ? 1.25 : 0.8;
        double newZoom = Mth.clamp(this.zoom * factor, 0.05, 3.0);

        double worldUnderMouseX = screenToWorldX(mouseX);
        double worldUnderMouseZ = screenToWorldZ(mouseY);

        this.zoom = newZoom;

        this.panWorldX = worldUnderMouseX - (mouseX - this.width / 2.0) / this.zoom;
        this.panWorldZ = worldUnderMouseZ - (mouseY - this.height / 2.0) / this.zoom;

        return true;
    }

    @Override
    public void removed() {
        this.missionState.save();
        super.removed();
    }
}
