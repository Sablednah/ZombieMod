package com.sablednah.zombiemod.client;

import java.util.List;

import com.sablednah.zombiemod.core.DexEntry.Row;

import com.sablednah.zombiemod.ZombieModRegistries;
import com.sablednah.zombiemod.core.DexEntry;
import com.sablednah.zombiemod.core.Genus;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.LivingEntity;

/** One genus, in full. Reached by clicking a name in {@link DexScreen}. */
public final class DexInfoScreen extends Screen {

    private static final int WHITE = 0xFFFFFFFF;

    private final DexScreen parent;
    private final Identifier id;
    private final String displayName;

    private Holder.Reference<Genus> holder;
    private LivingEntity preview;
    private int selectedAbility = -1;

    public DexInfoScreen(DexScreen parent, Identifier id, String displayName) {
        super(Component.literal(displayName));
        this.parent = parent;
        this.id = id;
        this.displayName = displayName;
    }

    @Override
    protected void init() {
        // The genus registry is a datapack registry with a network codec, so the client has the
        // whole thing already - no request, no round trip, no waiting.
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            holder = mc.level.registryAccess().lookupOrThrow(ZombieModRegistries.GENUS)
                    .get(ResourceKey.create(ZombieModRegistries.GENUS, id)).orElse(null);
            if (holder != null) {
                preview = DexPreview.of(id, holder);
            }
        }
        addRenderableWidget(Button.builder(Component.literal("< Back"), b -> onClose())
                .bounds(width / 2 - 60, height - 28, 120, 20).build());
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partial) {
        super.render(gfx, mouseX, mouseY, partial);

        DexScreen.panel(gfx, width, height);
        gfx.drawCenteredString(font, Component.literal(displayName).withStyle(ChatFormatting.GOLD),
                width / 2, 14, WHITE);

        if (holder == null) {
            gfx.drawCenteredString(font, Component.literal("No such genus loaded.")
                    .withStyle(ChatFormatting.DARK_GRAY), width / 2, height / 2, WHITE);
            return;
        }
        Genus genus = holder.value();

        int left = Math.max(12, width / 2 - 170);
        currentY = 34;

        genus.description().ifPresent(d -> {
            // Wrapped by the font, so a long line does not run off the edge of a narrow window.
            for (var seq : font.split(Component.literal(d)
                    .withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY), 200)) {
                gfx.drawString(font, seq, left, currentY, WHITE, false);
                currentY += 10;
            }
        });
        int y = currentY + 4;

        for (DexEntry.Row row : DexEntry.stats(genus)) {
            gfx.drawString(font, Component.literal(row.label() + ": ")
                    .withStyle(ChatFormatting.DARK_GRAY)
                    .append(Component.literal(row.detail()).withStyle(ChatFormatting.WHITE)),
                    left, y, WHITE, false);
            y += 10;
        }

        List<Row> abilities = DexEntry.abilities(genus);
        if (!abilities.isEmpty()) {
            y += 4;
            gfx.drawString(font, Component.literal("Abilities").withStyle(ChatFormatting.DARK_GRAY),
                    left, y, WHITE, false);
            y += 11;
            abilityTop = y;
            for (int i = 0; i < abilities.size(); i++) {
                boolean hovered = mouseX >= left && mouseX <= left + 120
                        && mouseY >= y && mouseY < y + 10;
                gfx.drawString(font, Component.literal(
                                (selectedAbility == i ? "- " : "+ ") + abilities.get(i).label())
                        .withStyle(hovered || selectedAbility == i
                                ? ChatFormatting.YELLOW : ChatFormatting.GOLD),
                        left, y, WHITE, false);
                y += 10;
                if (selectedAbility == i) {
                    String why = abilities.get(i).detail();
                    if (!why.isEmpty()) {
                        for (var seq : font.split(Component.literal(why)
                                .withStyle(ChatFormatting.GRAY), 190)) {
                            gfx.drawString(font, seq, left + 8, y, WHITE, false);
                            y += 10;
                        }
                    }
                }
            }
        }

        // The mannequin, on the right, watching the cursor exactly as the inventory doll does.
        if (preview != null) {
            // Half again as large as the inventory doll, and scaled down only for genera that are
            // themselves oversized, so a Tank still fits the frame it shares with a Swarmling.
            int px = width - 110;
            int py = height / 2 + 70;
            int size = Math.max(20, (int) (62 / Math.max(1.0, genus.scale())));
            gfx.fill(px - 62, py - 168, px + 62, py + 8, 0x30000000);
            InventoryScreen.renderEntityInInventoryFollowsMouse(gfx, px - 60, py - 166, px + 60, py + 6,
                    size, 0.0625F, mouseX, mouseY, preview);
        }
    }

    /** Set inside render, read by the click handler - the ability rows move as the page grows. */
    private int abilityTop = -1;
    private int currentY = 34;

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubled) {
        if (super.mouseClicked(event, doubled)) {
            return true;
        }
        double mouseX = event.x();
        double mouseY = event.y();
        if (holder == null || abilityTop < 0) {
            return false;
        }
        List<Row> abilities = DexEntry.abilities(holder.value());
        int left = Math.max(12, width / 2 - 170);
        if (mouseX < left || mouseX > left + 120) {
            return false;
        }
        // Walk the rows the way render laid them out, so an expanded entry does not misalign the
        // ones under it.
        int y = abilityTop;
        for (int i = 0; i < abilities.size(); i++) {
            if (mouseY >= y && mouseY < y + 10) {
                selectedAbility = selectedAbility == i ? -1 : i;
                return true;
            }
            y += 10;
            if (selectedAbility == i) {
                String why = abilities.get(i).detail();
                if (!why.isEmpty()) {
                    y += font.split(Component.literal(why), 190).size() * 10;
                }
            }
        }
        return false;
    }
}
