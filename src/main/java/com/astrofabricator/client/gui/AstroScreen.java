package com.astrofabricator.client.gui;

import com.astrofabricator.network.AstroPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public class AstroScreen extends Screen {

    private EditBox templateInput;
    private EditBox nameInput;
    private static final int UI_WIDTH = 200;

    public AstroScreen() {
        super(Component.literal("Astro Fabricator 控制面板"));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // --- 1. 维度模板输入框 (JSON ID) ---
        this.templateInput = new EditBox(this.font, centerX - 100, centerY - 40, UI_WIDTH, 20, Component.literal("模板ID"));
        this.templateInput.setHint(Component.literal("输入维度模板 (如 minecraft:overworld)"));
        this.addRenderableWidget(this.templateInput);

        // --- 2. 新维度名称输入框 ---
        this.nameInput = new EditBox(this.font, centerX - 100, centerY - 10, UI_WIDTH, 20, Component.literal("星球名称"));
        this.nameInput.setHint(Component.literal("输入新星域的自定义名称"));
        this.addRenderableWidget(this.nameInput);

        // --- 3. 创建按钮 ---
        this.addRenderableWidget(Button.builder(Component.literal("构建星域"), (button) -> {
            String template = this.templateInput.getValue();
            String name = this.nameInput.getValue();
            if (!template.isEmpty() && !name.isEmpty()) {
                PacketDistributor.sendToServer(new AstroPayload("create", template, name));
                this.onClose(); // 发送后关闭 GUI
            }
        }).bounds(centerX - 100, centerY + 20, 95, 20).build());

        // --- 4. 刷新列表按钮 ---
        this.addRenderableWidget(Button.builder(Component.literal("同步列表"), (button) -> {
            PacketDistributor.sendToServer(new AstroPayload("refresh_list", "", ""));
        }).bounds(centerX + 5, centerY + 20, 95, 20).build());

        // --- 5. 关闭按钮 ---
        this.addRenderableWidget(Button.builder(Component.literal("取消"), (button) -> this.onClose())
                .bounds(centerX - 100, centerY + 45, UI_WIDTH, 20).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 渲染背景遮罩
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        // 渲染标题
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);

        // 渲染输入框标签
        guiGraphics.drawString(this.font, "维度模板 JSON:", this.width / 2 - 100, this.height / 2 - 52, 0xAAAAAA);
        guiGraphics.drawString(this.font, "星域实例名称:", this.width / 2 - 100, this.height / 2 - 22, 0xAAAAAA);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false; // 打开 UI 时不暂停游戏
    }
}