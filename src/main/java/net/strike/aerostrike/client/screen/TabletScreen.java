package net.strike.aerostrike.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.ModList;
import net.strike.aerostrike.common.item.data.ItemDataFacade;
import net.strike.aerostrike.common.network.ModNetwork;
import net.strike.aerostrike.common.network.packet.c2s.LaunchAirMissilePacket;
import net.strike.aerostrike.core.registry.ModItems;

import java.util.ArrayList;
import java.util.List;

/**
 * Military Terminal Tablet Screen (MFD).
 * Features:
 * - Tactical radar map with range rings, compass, player heading, target crosshair, and waypoint route lines.
 * - Interactive click-to-aim: Left-click sets target, Right-click (or Shift-click) adds intermediate waypoints.
 * - Salvo selector (1x, 2x, 4x), cruising altitude adjuster (10m - 60m), and radar range selector (500m / 1000m / 2000m).
 * - Real-time payload detection (Storm Shadow).
 * - Soft-integration hook with Xaero's WorldMap.
 */
@OnlyIn(Dist.CLIENT)
public class TabletScreen extends Screen {

    private final ItemStack tabletStack;
    private static final int[] RANGES = {500, 1000, 2000};
    private int rangeIndex = 0;

    private BlockPos targetPos;
    private final List<BlockPos> waypoints = new ArrayList<>();
    private float cruiseAltitude = 15.0f;
    private int salvoCount = 1;

    // Layout
    private int panelWidth = 340;
    private int panelHeight = 220;
    private int leftPos;
    private int topPos;

    // Radar Center
    private int radarCenterX;
    private int radarCenterY;
    private final int radarRadius = 64;

    // Controls
    private EditBox xInput;
    private EditBox zInput;
    private Button launchButton;
    private Button salvoButton;
    private Button altButton;
    private Button rangeButton;

    public TabletScreen(ItemStack tabletStack) {
        super(Component.literal("AeroStrike Military Terminal"));
        this.tabletStack = tabletStack;
    }

    @Override
    protected void init() {
        super.init();

        this.leftPos = (this.width - this.panelWidth) / 2;
        this.topPos = (this.height - this.panelHeight) / 2;

        this.radarCenterX = this.leftPos + 80;
        this.radarCenterY = this.topPos + 105;

        // Load existing data from ItemDataFacade
        this.targetPos = ItemDataFacade.getTargetPos(this.tabletStack);
        this.waypoints.clear();
        this.waypoints.addAll(ItemDataFacade.getWaypoints(this.tabletStack));
        this.cruiseAltitude = ItemDataFacade.getCruiseClearance(this.tabletStack, 15.0f);
        this.salvoCount = ItemDataFacade.getSalvoCount(this.tabletStack, 1);

        LocalPlayer player = Minecraft.getInstance().player;
        if (this.targetPos == null && player != null) {
            // Default target: 300 blocks forward
            double yawRad = Math.toRadians(player.getYRot());
            int defX = (int) (player.getX() - Math.sin(yawRad) * 300);
            int defZ = (int) (player.getZ() + Math.cos(yawRad) * 300);
            int defY = (int) player.getY();
            this.targetPos = new BlockPos(defX, defY, defZ);
        }

        // Right side panel controls (X = leftPos + 175)
        int ctrlX = this.leftPos + 175;

        // Coordinate inputs
        this.xInput = new EditBox(this.font, ctrlX + 22, this.topPos + 35, 58, 16, Component.literal("Target X"));
        this.xInput.setValue(this.targetPos != null ? String.valueOf(this.targetPos.getX()) : "0");
        this.xInput.setResponder(this::onManualCoordChange);
        this.addRenderableWidget(this.xInput);

        this.zInput = new EditBox(this.font, ctrlX + 102, this.topPos + 35, 58, 16, Component.literal("Target Z"));
        this.zInput.setValue(this.targetPos != null ? String.valueOf(this.targetPos.getZ()) : "0");
        this.zInput.setResponder(this::onManualCoordChange);
        this.addRenderableWidget(this.zInput);

        // Radar Range Button
        this.rangeButton = Button.builder(Component.literal("Масштаб: " + RANGES[this.rangeIndex] + "м"), b -> {
            this.rangeIndex = (this.rangeIndex + 1) % RANGES.length;
            b.setMessage(Component.literal("Масштаб: " + RANGES[this.rangeIndex] + "м"));
        }).bounds(ctrlX, this.topPos + 60, 75, 18).build();
        this.addRenderableWidget(this.rangeButton);

        // Clear Waypoints Button
        Button clearWpBtn = Button.builder(Component.literal("Сброс ППМ"), b -> {
            this.waypoints.clear();
            saveToItemStack();
        }).bounds(ctrlX + 80, this.topPos + 60, 75, 18).build();
        this.addRenderableWidget(clearWpBtn);

        // Altitude selector
        this.altButton = Button.builder(Component.literal("Эшелон: " + (int) this.cruiseAltitude + "м"), b -> {
            if (this.cruiseAltitude == 15.0f) this.cruiseAltitude = 25.0f;
            else if (this.cruiseAltitude == 25.0f) this.cruiseAltitude = 40.0f;
            else if (this.cruiseAltitude == 40.0f) this.cruiseAltitude = 60.0f;
            else this.cruiseAltitude = 15.0f;

            b.setMessage(Component.literal("Эшелон: " + (int) this.cruiseAltitude + "м"));
            saveToItemStack();
        }).bounds(ctrlX, this.topPos + 85, 75, 18).build();
        this.addRenderableWidget(this.altButton);

        // Salvo selector
        this.salvoButton = Button.builder(Component.literal("Залп: " + this.salvoCount + "x"), b -> {
            if (this.salvoCount == 1) this.salvoCount = 2;
            else if (this.salvoCount == 2) this.salvoCount = 4;
            else this.salvoCount = 1;

            b.setMessage(Component.literal("Залп: " + this.salvoCount + "x"));
            saveToItemStack();
        }).bounds(ctrlX + 80, this.topPos + 85, 75, 18).build();
        this.addRenderableWidget(this.salvoButton);

        // Xaero WorldMap soft-button
        boolean hasXaero = ModList.get().isLoaded("xaeroworldmap");
        Button xaeroBtn = Button.builder(Component.literal(hasXaero ? "XAERO: СИНХР" : "XAERO: НЕТ"), b -> {
            if (hasXaero && player != null) {
                player.displayClientMessage(
                        Component.literal("§6[AeroStrike] §fЦель передана в координаты: §e" +
                                this.targetPos.getX() + ", " + this.targetPos.getZ()),
                        true
                );
            }
        }).bounds(ctrlX, this.topPos + 110, 155, 18).build();
        xaeroBtn.active = hasXaero;
        this.addRenderableWidget(xaeroBtn);

        // Tactical Big LAUNCH / DROP Button
        this.launchButton = Button.builder(Component.literal("ПУСК / AIR DROP"), b -> executeAirLaunch())
                .bounds(ctrlX, this.topPos + 165, 155, 28)
                .build();
        this.addRenderableWidget(this.launchButton);
    }

    private void onManualCoordChange(String val) {
        try {
            int x = Integer.parseInt(this.xInput.getValue().trim());
            int z = Integer.parseInt(this.zInput.getValue().trim());
            int y = this.targetPos != null ? this.targetPos.getY() : 64;
            this.targetPos = new BlockPos(x, y, z);
            saveToItemStack();
        } catch (NumberFormatException ignored) {}
    }

    private void saveToItemStack() {
        if (this.targetPos != null) {
            ItemDataFacade.setTargetPos(this.tabletStack, this.targetPos);
        }
        ItemDataFacade.setWaypoints(this.tabletStack, this.waypoints);
        ItemDataFacade.setCruiseClearance(this.tabletStack, this.cruiseAltitude);
        ItemDataFacade.setSalvoCount(this.tabletStack, this.salvoCount);
    }

    private int getStormShadowCount() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return 0;
        if (player.getAbilities().instabuild) return 99;
        int count = 0;
        for (ItemStack s : player.getInventory().items) {
            if (s.is(ModItems.STORM_SHADOW_ITEM.get())) {
                count += s.getCount();
            }
        }
        return count;
    }

    private void executeAirLaunch() {
        if (this.targetPos == null) return;

        saveToItemStack();

        // Send launch packet to server
        ModNetwork.sendToServer(new LaunchAirMissilePacket(
                this.targetPos,
                this.waypoints,
                this.cruiseAltitude,
                this.salvoCount
        ));

        // Play client UI launch chirp
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            player.playSound(SoundEvents.UI_BUTTON_CLICK.get(), 1.0f, 1.5f);
        }

        this.onClose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Check if click was inside radar circular scope
        double distFromCenter = Math.hypot(mouseX - this.radarCenterX, mouseY - this.radarCenterY);
        if (distFromCenter <= this.radarRadius) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null) {
                int currentRange = RANGES[this.rangeIndex];
                double scale = (double) currentRange / this.radarRadius;

                double offsetX = (mouseX - this.radarCenterX) * scale;
                double offsetZ = (mouseY - this.radarCenterY) * scale;

                int worldX = (int) (player.getX() + offsetX);
                int worldZ = (int) (player.getZ() + offsetZ);
                int worldY = (int) player.getY();

                BlockPos clickedWorldPos = new BlockPos(worldX, worldY, worldZ);

                if (button == 1 || hasShiftDown()) {
                    // Right-click or Shift-click: add intermediate waypoint
                    if (this.waypoints.size() < 6) {
                        this.waypoints.add(clickedWorldPos);
                        player.playSound(SoundEvents.NOTE_BLOCK_BELL.get(), 0.5f, 1.8f);
                    }
                } else {
                    // Left-click: set final target
                    this.targetPos = clickedWorldPos;
                    this.xInput.setValue(String.valueOf(worldX));
                    this.zInput.setValue(String.valueOf(worldZ));
                    player.playSound(SoundEvents.NOTE_BLOCK_PLING.get(), 0.5f, 1.5f);
                }

                saveToItemStack();
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);

        // 1. Tablet main chassis (Dark military carbon glass)
        graphics.fill(this.leftPos, this.topPos, this.leftPos + this.panelWidth, this.topPos + this.panelHeight, 0xE6101416);
        // Emerald tactical border
        graphics.fill(this.leftPos, this.topPos, this.leftPos + this.panelWidth, this.topPos + 2, 0xFF00AA44);
        graphics.fill(this.leftPos, this.topPos + this.panelHeight - 2, this.leftPos + this.panelWidth, this.topPos + this.panelHeight, 0xFF00AA44);
        graphics.fill(this.leftPos, this.topPos, this.leftPos + 2, this.topPos + this.panelHeight, 0xFF00AA44);
        graphics.fill(this.leftPos + this.panelWidth - 2, this.topPos, this.leftPos + this.panelWidth, this.topPos + this.panelHeight, 0xFF00AA44);

        // Header Title
        graphics.drawString(this.font, "§2[MFD] §aAEROSTRIKE TACTICAL TERMINAL", this.leftPos + 10, this.topPos + 8, 0x00FF66, false);

        // Divider
        graphics.fill(this.leftPos + 165, this.topPos + 24, this.leftPos + 167, this.topPos + this.panelHeight - 10, 0x4000AA44);

        // 2. Render Tactical Radar Scope
        renderRadarScope(graphics);

        // 3. Render Right Control Panel Labels
        int ctrlX = this.leftPos + 175;
        graphics.drawString(this.font, "§7ЦЕЛЬ:  X:          Z:", ctrlX, this.topPos + 24, 0xAAAAAA, false);

        // Payload Status
        int missiles = getStormShadowCount();
        if (missiles > 0) {
            graphics.drawString(this.font, "§aБОЕКОМПЛЕКТ: §e" + missiles + "x Storm Shadow", ctrlX, this.topPos + 135, 0x00FF66, false);
            this.launchButton.active = (this.targetPos != null);
        } else {
            graphics.drawString(this.font, "§cБОЕКОМПЛЕКТ: НЕТ РАКЕТ", ctrlX, this.topPos + 135, 0xFF4444, false);
            this.launchButton.active = false;
        }

        // Target distance and azimuth info
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && this.targetPos != null) {
            double dx = this.targetPos.getX() - player.getX();
            double dz = this.targetPos.getZ() - player.getZ();
            double dist = Math.hypot(dx, dz);
            float azimuth = (float) ((Math.toDegrees(Mth.atan2(dx, -dz)) + 360) % 360);

            graphics.drawString(this.font, String.format("§7Дист: §f%.0fм §7| Азимут: §e%.0f°", dist, azimuth), ctrlX, this.topPos + 148, 0xCCCCCC, false);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderRadarScope(GuiGraphics graphics) {
        int cx = this.radarCenterX;
        int cy = this.radarCenterY;
        int r = this.radarRadius;

        // Radar dark circular background
        for (int y = -r; y <= r; y++) {
            int span = (int) Math.sqrt(r * r - y * y);
            graphics.fill(cx - span, cy + y, cx + span, cy + y + 1, 0x90081C10);
        }

        // Radar Range Rings (1/3, 2/3, and 3/3 radius)
        drawRadarRing(graphics, cx, cy, (int) (r * 0.33), 0x3000FF66);
        drawRadarRing(graphics, cx, cy, (int) (r * 0.66), 0x4000FF66);
        drawRadarRing(graphics, cx, cy, r, 0xFF00AA44);

        // Crosshairs
        graphics.fill(cx - r, cy, cx + r, cy + 1, 0x3000FF66);
        graphics.fill(cx, cy - r, cx, cy + r, 0x3000FF66);

        // Compass letters
        graphics.drawString(this.font, "N", cx - 3, cy - r + 3, 0x00FF66, false);
        graphics.drawString(this.font, "S", cx - 3, cy + r - 10, 0x00FF66, false);
        graphics.drawString(this.font, "W", cx - r + 3, cy - 4, 0x00FF66, false);
        graphics.drawString(this.font, "E", cx + r - 9, cy - 4, 0x00FF66, false);

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        int currentRange = RANGES[this.rangeIndex];
        double scale = (double) r / currentRange;

        // Draw Waypoint Route Lines: Player -> WP1 -> WP2 -> Target
        int prevSX = cx;
        int prevSY = cy;

        for (int i = 0; i < this.waypoints.size(); i++) {
            BlockPos wp = this.waypoints.get(i);
            int sx = (int) (cx + (wp.getX() - player.getX()) * scale);
            int sy = (int) (cy + (wp.getZ() - player.getZ()) * scale);

            // Draw line segment
            drawTacticalLine(graphics, prevSX, prevSY, sx, sy, 0xFFFFAA00);

            // Draw Waypoint Node (Yellow diamond)
            graphics.fill(sx - 2, sy - 2, sx + 3, sy + 3, 0xFFFFAA00);
            graphics.drawString(this.font, String.valueOf(i + 1), sx + 4, sy - 4, 0xFFFFAA00, false);

            prevSX = sx;
            prevSY = sy;
        }

        // Draw Line to Target
        if (this.targetPos != null) {
            int tx = (int) (cx + (this.targetPos.getX() - player.getX()) * scale);
            int ty = (int) (cy + (this.targetPos.getZ() - player.getZ()) * scale);

            drawTacticalLine(graphics, prevSX, prevSY, tx, ty, 0xFFFF3333);

            // Draw Target Crosshair (Red 🎯)
            graphics.fill(tx - 4, ty, tx + 5, ty + 1, 0xFFFF2222);
            graphics.fill(tx, ty - 4, tx + 1, ty + 5, 0xFFFF2222);
            graphics.fill(tx - 2, ty - 2, tx + 3, ty + 3, 0xFFFF4444);
        }

        // Player marker at center: Green dot + heading pointer
        graphics.fill(cx - 2, cy - 2, cx + 3, cy + 3, 0xFF00FF66);
        float yawRad = (float) Math.toRadians(player.getYRot());
        int hx = (int) (cx - Math.sin(yawRad) * 8);
        int hy = (int) (cy + Math.cos(yawRad) * 8);
        graphics.fill(hx - 1, hy - 1, hx + 2, hy + 2, 0xFFFFFFFF);

        // Radar footer instructions
        graphics.drawString(this.font, "§7ЛКМ: Цель | ПКМ: Добавить ППМ", this.leftPos + 6, this.topPos + this.panelHeight - 16, 0x888888, false);
    }

    private void drawRadarRing(GuiGraphics graphics, int cx, int cy, int radius, int color) {
        for (int a = 0; a < 360; a += 10) {
            double rad = Math.toRadians(a);
            int px = (int) (cx + Math.cos(rad) * radius);
            int py = (int) (cy + Math.sin(rad) * radius);
            graphics.fill(px, py, px + 1, py + 1, color);
        }
    }

    private void drawTacticalLine(GuiGraphics graphics, int x0, int y0, int x1, int y1, int color) {
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;

        int currX = x0;
        int currY = y0;

        for (int i = 0; i < 200; i++) {
            graphics.fill(currX, currY, currX + 1, currY + 1, color);
            if (currX == x1 && currY == y1) break;
            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                currX += sx;
            }
            if (e2 < dx) {
                err += dx;
                currY += sy;
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
