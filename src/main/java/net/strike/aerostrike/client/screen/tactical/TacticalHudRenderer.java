package net.strike.aerostrike.client.screen.tactical;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.BiFunction;

/**
 * High-performance tactical HUD and MFD renderer.
 * Draws military glass cockpit status bars, vector flight routes,
 * animated marching dash lines, target lock reticles, and mission planning panels.
 */
public final class TacticalHudRenderer {

    // Color Palette
    public static final int COLOR_BG_PANEL       = 0xEA091016; // Very dark navy translucent
    public static final int COLOR_PANEL_BORDER   = 0xFF1E4632; // Tactical olive-green border
    public static final int COLOR_PANEL_HEADER   = 0xFF0D1F16;
    public static final int COLOR_TEXT_ACCENT    = 0xFF00FF88; // Neon tactical green
    public static final int COLOR_TEXT_AMBER     = 0xFFFF9900; // Warning amber
    public static final int COLOR_TEXT_RED       = 0xFFFF3333; // Target red
    public static final int COLOR_ROUTE_LEG      = 0xCC00E5FF; // Cyan route line
    public static final int COLOR_ROUTE_TERMINAL = 0xEEFF3344; // Red terminal attack leg
    public static final int COLOR_GRID_LINE      = 0x2200FF66; // Radar grid line

    public record ScreenPoint(double x, double y) {}

    public static void drawTopStatusBar(
            GuiGraphics graphics,
            Font font,
            int screenWidth,
            int cursorBlockX,
            int cursorBlockZ,
            double zoomScale,
            TacticalMissionState state
    ) {
        // Top background bar (24px height)
        graphics.fill(0, 0, screenWidth, 24, 0xF0080D12);
        graphics.fill(0, 23, screenWidth, 24, COLOR_PANEL_BORDER);

        // Military Title
        graphics.drawString(font, "§2[ §aAERO-STRIKE C2 TERMINAL §2] §8// MIL-SPEC B-4", 10, 8, 0xFFFFFFFF, false);

        // Center: Cursor coordinates & Zoom
        String coordText = String.format("MGRS: [X: %+d, Z: %+d]  |  ZOOM: %.2fx", cursorBlockX, cursorBlockZ, zoomScale);
        int coordWidth = font.width(coordText);
        graphics.drawString(font, "§7" + coordText, (screenWidth - coordWidth) / 2, 8, 0xFFE0E0E0, false);

        // Right side: Active mission status message
        String status = state.getStatusMessage();
        int statusW = font.width(status);
        graphics.drawString(font, status, screenWidth - statusW - 12, 8, state.getStatusColor(), false);
    }

    public static void drawControlPanelFrame(
            GuiGraphics graphics,
            Font font,
            int x, int y, int w, int h,
            LocalPlayer player,
            TacticalMissionState state
    ) {
        // Background and border
        graphics.fill(x, y, x + w, y + h, COLOR_BG_PANEL);
        drawHollowRect(graphics, x, y, w, h, COLOR_PANEL_BORDER);
        graphics.fill(x + 1, y + 1, x + w - 1, y + 18, COLOR_PANEL_HEADER);
        graphics.fill(x, y + 18, x + w, y + 19, COLOR_PANEL_BORDER);

        // Title
        graphics.drawString(font, "§a§lTACTICAL MISSION COMPUTER", x + 10, y + 6, 0xFFFFFFFF, false);

        // Payload Detection
        int payloadCount = state.getAvailablePayloadCount(player);
        int curY = y + 24;

        if (payloadCount > 0) {
            graphics.drawString(font, "§7PAYLOAD: §aStorm Shadow §f(x" + payloadCount + ")", x + 10, curY, 0xFFFFFFFF, false);
        } else {
            graphics.drawString(font, "§7PAYLOAD: §cNO MISSILES READY", x + 10, curY, 0xFFFFFFFF, false);
        }
        curY += 14;

        // Target Coordinates
        BlockPos target = state.getTargetPos();
        if (target != null) {
            graphics.drawString(font, "§7TARGET: §eX: " + target.getX() + "  Z: " + target.getZ(), x + 10, curY, 0xFFFFFFFF, false);
            curY += 12;

            if (player != null) {
                double totalDist = state.calculateTotalDistance(player.position());
                int eta = state.calculateEtaSeconds(totalDist);
                graphics.drawString(font, String.format("§7TOTAL DIST: §f%,d m", (int) totalDist), x + 10, curY, 0xFFFFFFFF, false);
                curY += 12;
                graphics.drawString(font, String.format("§7TIME TO IMPACT: §f~%d sec", eta), x + 10, curY, 0xFFFFFFFF, false);
            }
        } else {
            graphics.drawString(font, "§7TARGET: §cUNASSIGNED", x + 10, curY, 0xFFFFFFFF, false);
            curY += 14;
        }
        curY += 16;

        // Separator
        graphics.fill(x + 8, curY, x + w - 8, curY + 1, 0x44205530);
        curY += 6;

        // Waypoints info
        List<BlockPos> waypoints = state.getWaypoints();
        String wpText = waypoints.isEmpty() ? "§7ROUTE: §fDirect to Target" : "§6WAYPOINTS: §f" + waypoints.size() + " pts";
        graphics.drawString(font, wpText, x + 10, curY, 0xFFFFFFFF, false);
        curY += 14;

        // Add WP status indicator
        if (state.isAddWaypointMode()) {
            boolean blink = (System.currentTimeMillis() / 400) % 2 == 0;
            int blinkColor = blink ? 0xFFFFAA00 : 0xFFFF5500;
            graphics.drawString(font, "§e>> WP INSERT MODE ACTIVE <<", x + 10, curY, blinkColor, false);
        } else {
            graphics.drawString(font, "§8[Shift+Click to add WP]", x + 10, curY, 0xFF888888, false);
        }
    }

    public static void drawFlightRoute(
            GuiGraphics graphics,
            List<ScreenPoint> points
    ) {
        if (points == null || points.size() < 2) return;

        long time = System.currentTimeMillis();
        // Moving dash animation offset
        int dashOffset = (int) ((time / 60) % 16);

        for (int i = 0; i < points.size() - 1; i++) {
            ScreenPoint p1 = points.get(i);
            ScreenPoint p2 = points.get(i + 1);

            boolean isTerminalLeg = (i == points.size() - 2);
            int lineColor = isTerminalLeg ? COLOR_ROUTE_TERMINAL : COLOR_ROUTE_LEG;

            drawDashedLine(graphics, p1.x, p1.y, p2.x, p2.y, 2.0f, lineColor, dashOffset);
        }
    }

    public static void drawPlayerMarker(
            GuiGraphics graphics,
            Font font,
            double x, double y,
            float yaw
    ) {
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(x, y, 0);

        // Green pulsing outer ring
        float pulse = (float) (Math.sin(System.currentTimeMillis() / 250.0) * 0.5 + 0.5);
        int ringAlpha = (int) (100 + pulse * 120);
        int ringColor = (ringAlpha << 24) | 0x00FF88;

        drawCircle(graphics, 0, 0, 7, ringColor);

        // Heading arrow rotated by yaw
        pose.pushPose();
        pose.mulPose(Axis.ZP.rotationDegrees(yaw + 180.0f));

        // Chevron aircraft triangle
        graphics.fill(-3, -4, 3, -3, 0xFF00FF88);
        graphics.fill(-2, -3, 2, 2, 0xFF00FF88);
        graphics.fill(-1, 2, 1, 5, 0xFF00FF88);
        // Heading line
        graphics.fill(0, 5, 1, 16, 0xAA00FF88);

        pose.popPose();
        pose.popPose();

        // Label
        graphics.drawString(font, "§aHOST PLATFORM", (int) x + 9, (int) y - 4, 0xFFFFFFFF, true);
    }

    public static void drawWaypointMarker(
            GuiGraphics graphics,
            Font font,
            double x, double y,
            int index,
            BlockPos pos
    ) {
        // Diamond icon
        int ix = (int) x;
        int iy = (int) y;

        graphics.fill(ix - 1, iy - 4, ix + 2, iy - 3, COLOR_TEXT_AMBER);
        graphics.fill(ix - 3, iy - 2, ix + 4, iy - 1, COLOR_TEXT_AMBER);
        graphics.fill(ix - 4, iy - 1, ix + 5, iy + 2, COLOR_TEXT_AMBER);
        graphics.fill(ix - 3, iy + 2, ix + 4, iy + 3, COLOR_TEXT_AMBER);
        graphics.fill(ix - 1, iy + 3, ix + 2, iy + 4, COLOR_TEXT_AMBER);
        // Center black hole
        graphics.fill(ix, iy, ix + 1, iy + 1, 0xFF000000);

        // Label
        String label = String.format("WP-%02d", index + 1);
        graphics.drawString(font, "§6" + label, ix + 6, iy - 4, 0xFFFFFFFF, true);
    }

    public static void drawTargetReticle(
            GuiGraphics graphics,
            Font font,
            double x, double y,
            BlockPos targetPos,
            double distFromPlayer
    ) {
        int ix = (int) x;
        int iy = (int) y;

        long time = System.currentTimeMillis();
        // Pulsing red animation
        float pulse = (float) (Math.sin(time / 200.0) * 0.5 + 0.5);
        int redColor = 0xFFFF0000 | ((int) (180 + pulse * 75) << 16);

        // Reticle brackets (size 12)
        int r = 10;
        // Top-left bracket
        graphics.fill(ix - r, iy - r, ix - r + 5, iy - r + 1, redColor);
        graphics.fill(ix - r, iy - r, ix - r + 1, iy - r + 5, redColor);

        // Top-right bracket
        graphics.fill(ix + r - 4, iy - r, ix + r + 1, iy - r + 1, redColor);
        graphics.fill(ix + r, iy - r, ix + r + 1, iy - r + 5, redColor);

        // Bottom-left bracket
        graphics.fill(ix - r, iy + r, ix - r + 5, iy + r + 1, redColor);
        graphics.fill(ix - r, iy + r - 4, ix - r + 1, iy + r + 1, redColor);

        // Bottom-right bracket
        graphics.fill(ix + r - 4, iy + r, ix + r + 1, iy + r + 1, redColor);
        graphics.fill(ix + r, iy + r - 4, ix + r + 1, iy + r + 1, redColor);

        // Center crosshair
        graphics.fill(ix - 3, iy, ix + 4, iy + 1, redColor);
        graphics.fill(ix, iy - 3, ix + 1, iy + 4, redColor);

        // Target Information Tag
        String targetTag = String.format("TARGET LOCK [%d, %d]", targetPos.getX(), targetPos.getZ());
        String distTag = String.format("%,d m", (int) distFromPlayer);
        graphics.drawString(font, "§c" + targetTag, ix + r + 4, iy - 8, 0xFFFFFFFF, true);
        graphics.drawString(font, "§e" + distTag, ix + r + 4, iy + 2, 0xFFFFFFFF, true);
    }

    public static void drawDashedLine(
            GuiGraphics graphics,
            double x1, double y1,
            double x2, double y2,
            float thickness,
            int color,
            int animOffset
    ) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double length = Math.sqrt(dx * dx + dy * dy);
        if (length < 2) return;

        float angle = (float) Math.toDegrees(Math.atan2(dy, dx));

        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(x1, y1, 0);
        pose.mulPose(Axis.ZP.rotationDegrees(angle));

        int dashSize = 8;
        int gapSize = 6;
        int patternSize = dashSize + gapSize;

        int start = -animOffset;
        while (start < length) {
            int dStart = Math.max(0, start);
            int dEnd = Math.min((int) length, start + dashSize);
            if (dEnd > dStart) {
                graphics.fill(dStart, (int) -thickness / 2, dEnd, (int) (thickness / 2 + 1), color);
            }
            start += patternSize;
        }

        pose.popPose();
    }

    public static void drawHollowRect(GuiGraphics graphics, int x, int y, int w, int h, int color) {
        graphics.fill(x, y, x + w, y + 1, color);
        graphics.fill(x, y + h - 1, x + w, y + h, color);
        graphics.fill(x, y, x + 1, y + h, color);
        graphics.fill(x + w - 1, y, x + w, y + h, color);
    }

    public static void drawCircle(GuiGraphics graphics, int cx, int cy, int radius, int color) {
        int r2 = radius * radius;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                int d2 = dx * dx + dy * dy;
                if (d2 >= r2 - radius * 2 && d2 <= r2) {
                    graphics.fill(cx + dx, cy + dy, cx + dx + 1, cy + dy + 1, color);
                }
            }
        }
    }

    private TacticalHudRenderer() {}
}
