package nl.henk.helpers;

import java.util.UUID;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;

/** Private NPC conversation using the normal scrolling Minecraft chat display. */
public final class WorkerChatScreen extends ChatScreen {
    private final UUID worker;
    private final Component name;
    private final java.util.List<Button> controls = new java.util.ArrayList<>();
    private Button followButton;
    private Button gapButton;

    public WorkerChatScreen(UUID worker, Component name) {
        super("");
        this.worker = worker;
        this.name = name;
    }

    @Override protected void init() {
        super.init();
        input.setMaxLength(500);
        input.setHint(Component.literal("Typ een vraag of opdracht..."));
        controls.clear();
        int left = Math.max(4, (width - 316) / 2);
        controls.add(addRenderableWidget(Button.builder(Component.literal("Rugzak openen"), b -> control("inventory"))
                .bounds(left, 78, 100, 20).build()));
        controls.add(addRenderableWidget(Button.builder(Component.literal("Haal spullen op"), b -> control("collect"))
                .bounds(left + 104, 78, 100, 20).build()));
        controls.add(addRenderableWidget(Button.builder(Component.literal("Stop taak"), b -> control("stop"))
                .bounds(left + 208, 78, 104, 20).build()));
        controls.add(addRenderableWidget(Button.builder(Component.literal("Koppel kist/vat"), b -> control("bind"))
                .bounds(left, 102, 100, 20).build()));
        followButton = addRenderableWidget(Button.builder(Component.literal("Volg mij"), b -> control("follow"))
                .bounds(left + 104, 102, 100, 20).build());
        gapButton = addRenderableWidget(Button.builder(Component.literal("Afstand: 4/2"), b -> control("gap"))
                .bounds(left + 208, 102, 104, 20).build());
        controls.add(followButton);
        controls.add(gapButton);
        controls.add(addRenderableWidget(Button.builder(Component.literal("Wis gesprek"), b -> control("forget"))
                .bounds(left, 126, 152, 20).build()));
        controls.add(addRenderableWidget(Button.builder(Component.literal("Sluiten"), b -> onClose())
                .bounds(left + 156, 126, 156, 20).build()));
    }

    private void control(String action) {
        if (minecraft.player != null)
            minecraft.player.connection.sendCommand("helper control " + worker + " " + action);
        setFocused(input);
    }

    @Override public void handleChatInput(String text, boolean addToHistory) {
        text = text.strip().replaceAll("[\\p{Cntrl}]", " ");
        if (text.isBlank() || minecraft.player == null) return;
        minecraft.player.connection.sendCommand("helper talk " + worker + " " + text);
        minecraft.gui.getChat().addMessage(Component.literal("Jij tegen " + name.getString() + ": " + text));
        input.setValue("");
    }

    @Override public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (key == 257 || key == 335) {
            handleChatInput(input.getValue(), false);
            setFocused(input);
            return true;
        }
        return super.keyPressed(key, scanCode, modifiers);
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.enableScissor(0, 154, width, height);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.disableScissor();
        int left = Math.max(4, (width - 324) / 2);
        graphics.fill(left, 4, Math.min(width - 4, left + 324), 152, 0xED172331);
        graphics.fill(left, 4, Math.min(width - 4, left + 324), 6, 0xFF62C8A5);
        graphics.drawString(font, "HENKHULP  |  Werk & voorraad", left + 8, 12, 0x92E4C4);
        Worker npc = null;
        if (minecraft.level != null) for (var entity : minecraft.level.entitiesForRendering()) {
            if (entity instanceof Worker candidate && candidate.getUUID().equals(worker)) { npc = candidate; break; }
        }
        if (npc != null) {
            followButton.setMessage(Component.literal(npc.isFollowing() ? "Blijf hier" : "Volg mij"));
            gapButton.setMessage(Component.literal("Afstand: " + npc.followStartDistance() + "/" + npc.followStopDistance()));
            graphics.drawString(font, font.plainSubstrByWidth(npc.displayStatus(), 302), left + 8, 27, 0xFFFFFF);
            graphics.renderItem(npc.getMainHandItem(), left + 8, 43);
            graphics.drawString(font, npc.getMainHandItem().isEmpty() ? "Geen bijl" : "Gereedschap uitgerust", left + 28, 47, 0xD5DEEA);
            graphics.renderItem(new ItemStack(Items.OAK_PLANKS), left + 190, 43);
            graphics.drawString(font, "" + npc.displayPlanks() + " planken", left + 210, 47, 0xD5DEEA);
            graphics.drawString(font, font.plainSubstrByWidth(npc.displaySupply(), 302), left + 8, 65, 0xA8B9CD);
        }
        for (var button : controls) button.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawString(font, "Enter: versturen | Esc: sluiten", 4, height - 28, 0xFFFFFF);
    }
}
