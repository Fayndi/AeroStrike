package net.strike.aerostrike.client.screen.xaero;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
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
import xaero.map.MapProcessor;
import xaero.map.gui.GuiMap;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Fullscreen Military Tactical C2 Screen powered by Xaero's World Map.
 *
 * Integrates directly into Xaero's high-performance chunk rendering engine while
 * completely replacing the civilian waypoint GUI with a military Glass Cockpit HUD:
 * - Direct click-to-designate Target (no Xaero waypoint files/clutter).
 * - Multi-waypoint flight path planning with animated marching dashed route vector.
 * - Cruising altitude selection (14m Nap-of-the-Earth stealth, 30m, 60m, 120m).
 * - Salvo size selector (1x, 2x, 4x battery fire).
 * - Single-click air launch execution for Storm Shadow cruise missiles.
 */
public class TacticalXaeroMapScreen extends GuiMap {

    private final TacticalMissionState missionState;
    private final ItemStack tabletStack;

    // Reflection handles to Xaero's core camera fields
    private static Field CAMERA_X;
    private static Field CAMERA_Z;
    private static Field SCALE;

    static {
        try {
            CAMERA_X = GuiMap.class.getDeclaredField("cameraX");
            CAMERA_X.setAccessible(true);
            CAMERA_Z = GuiMap.class.getDeclaredField("cameraZ");
            CAMERA_Z.setAccessible(true);
            SCALE = GuiMap.class.getDeclaredField("scale");
            SCALE.setAccessible(true);
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
    private int panelW = 210;
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

        // Button: Add Waypoint Toggle
        this.addWpButton = this.addRenderableWidget(Button.builder(
                getAddWpText(),
                b -> {
                    this.missionState.toggleAddWaypointMode();
                    b.setMessage(getAddWpText());
                }
        ).bounds(btnX, curY, btnW, 18).build());
        curY += 21;

        // Button: Undo Last WP & Clear
        int halfW = (btnW - 4) / 2;
        this.undoWpButton = this.addRenderableWidget(Button.builder(
                Component.literal("§e< UNDO WP"),
                b -> this.missionState.removeLastWaypoint()
        ).bounds(btnX, curY, halfW, 18).build());

        this.clearRouteButton = this.addRenderableWidget(Button.builder(
                Component.literal("§cCLR ROUTE"),
                b -> this.missionState.clearWaypoints()
        ).bounds(btnX + halfW + 4, curY, halfW, 18).build());
        curY += 24;

        // Button: Cruise Altitude
        this.altButton = this.addRenderableWidget(Button.builder(
                getAltText(),
                b -> {
                    this.missionState.cycleAltitude();
                    b.setMessage(getAltText());
                }
        ).bounds(btnX, curY, btnW, 18).build());
        curY += 21;

        // Button: Salvo Size
        this.salvoButton = this.addRenderableWidget(Button.builder(
                getSalvoText(),
                b -> {
                    this.missionState.cycleSalvo();
                    b.setMessage(getSalvoText());
                }
        ).bounds(btnX, curY, btnW, 18).build());
        curY += 21;

        // Button: Center on Player
        this.centerButton = this.addRenderableWidget(Button.builder(
                Component.literal("§fCENTER ON AIRCRAFT"),
                b -> centerOnPlayer()
        ).bounds(btnX, curY, btnW, 18).build());
        curY += 26;

        // Button: BIG LAUNCH MISSILE
        this.launchButton = this.addRenderableWidget(Button.builder(
                Component.literal("§c§l>>> LAUNCH AIR MISSILE <<<"),
                b -> {
                    boolean success = this.missionState.executeLaunch(Minecraft.getInstance().player);
                    if (success) {
                        this.onClose();
                    }
                }
        ).bounds(btnX, curY, btnW, 24).build());
    }

    private Component getAddWpText() {
        return this.missionState.isAddWaypointMode()
                ? Component.literal("§e[+ WP MODE: ACTIVE]")
                : Component.literal("§7[+ ADD WAYPOINT]");
    }

    private Component getAltText() {
        return Component.literal("§bALT: §f" + this.missionState.getAltitudeLabel());
    }

    private Component getSalvoText() {
        return Component.literal("§dSALVO: §f" + this.missionState.getSalvoLabel());
    }

    private void centerOnPlayer() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && CAMERA_X != null && CAMERA_Z != null) {
            try {
                CAMERA_X.setDouble(this, player.getX());
                CAMERA_Z.setDouble(this, player.getZ());
            } catch (Exception ignored) {}
        }
    }

    public double getCameraX() {
        try {
            if (CAMERA_X != null) return CAMERA_X.getDouble(this);
        } catch (Exception ignored) {}
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null ? player.getX() : 0;
    }

    public double getCameraZ() {
        try {
            if (CAMERA_Z != null) return CAMERA_Z.getDouble(this);
        } catch (Exception ignored) {}
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null ? player.getZ() : 0;
    }

    public double getMapScale() {
        try {
            if (SCALE != null) return SCALE.getDouble(this);
        } catch (Exception ignored) {}
        return 1.0;
    }

    public double worldXToScreen(double wx) {
        return (wx - getCameraX()) * getMapScale() + this.width / 2.0;
    }

    public double worldZToScreen(double wz) {
        return (wz - getCameraZ()) * getMapScale() + this.height / 2.0;
    }

    public double screenToWorldX(double sx) {
        return (sx - this.width / 2.0) / getMapScale() + getCameraX();
    }

    public double screenToWorldZ(double sy) {
        return (sy - this.height / 2.0) / getMapScale() + getCameraZ();
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

        // 5. Render Host Aircraft Marker
        if (player != null) {
            double px = worldXToScreen(player.getX());
            double py = worldZToScreen(player.getZ());
            TacticalHudRenderer.drawPlayerMarker(guiGraphics, this.font, px, py, player.getYRot());
        }

        // 6. Render Top Status Bar
        int cursorWorldX = (int) Math.floor(screenToWorldX(mouseX));
        int cursorWorldZ = (int) Math.floor(screenToWorldZ(mouseY));
        TacticalHudRenderer.drawTopStatusBar(
                guiGraphics,
                this.font,
                this.width,
                cursorWorldX,
                cursorWorldZ,
                getMapScale(),
                this.missionState
        );

        // 7. Render Side Control Panel Frame
        TacticalHudRenderer.drawControlPanelFrame(
                guiGraphics,
                this.font,
                this.panelX, this.panelY, this.panelW, this.panelH,
                player,
                this.missionState
        );

        // 8. Render Widgets / Buttons
        for (GuiEventListener child : this.children()) {
            if (child instanceof Button btn) {
                btn.render(guiGraphics, mouseX, mouseY, partialTicks);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // If click is on the tactical control panel, let buttons handle it
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
                // Also pass to super so map can be dragged with left button
                return super.mouseClicked(mouseX, mouseY, button);
            }
        } else if (button == 1) { // Right Click (Bypasses civilian Xaero menu!)
            // Check if clicked close to an existing waypoint to delete it
            List<BlockPos> waypoints = this.missionState.getWaypoints();
            for (int i = waypoints.size() - 1; i >= 0; i--) {
                BlockPos wp = waypoints.get(i);
                double sx = worldXToScreen(wp.getX() + 0.5);
                double sy = worldZToScreen(wp.getZ() + 0.5);
                if (Math.abs(sx - mouseX) <= 12 && Math.abs(sy - mouseY) <= 12) {
                    this.missionState.getWaypoints().remove(i);
                    this.missionState.save();
                    this.missionState.setStatus("WAYPOINT WP-" + (i + 1) + " DELETED", 0xFFFFAA00);
                    if (player != null) player.playSound(SoundEvents.DISPENSER_FAIL, 0.8f, 1.4f);
                    return true;
                }
            }

            // Otherwise, right-click acts as quick-designate or quick-waypoint
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
