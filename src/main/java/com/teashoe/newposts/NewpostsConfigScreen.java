package com.teashoe.newposts;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class NewpostsConfigScreen extends Screen {

    private final Screen parent;

    private EditBox galleryIdBox;
    private EditBox geuldethapBox;

    public NewpostsConfigScreen(Screen parent) {
        super(Component.literal("Newposts 설정"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int y = 50;
        int gap = 28;
        int w = 220;
        int h = 20;

        // Gallery ID 입력
        this.galleryIdBox = new EditBox(this.font, cx - w / 2, y, w, h, Component.literal("Gallery ID"));
        this.galleryIdBox.setValue(NewpostsConfig.GALLERY_ID.get());
        this.addRenderableWidget(this.galleryIdBox);

        // 액션바에 표시
        this.addRenderableWidget(CycleButton.booleanBuilder(
                        Component.literal("ON"), Component.literal("OFF"))
                .withInitialValue(NewpostsConfig.USE_SYSTEM_CHAT.get())
                .create(cx - w / 2, y + gap, w, h,
                        Component.literal("액션바에 표시"),
                        (btn, val) -> NewpostsConfig.USE_SYSTEM_CHAT.set(val)));

        // 유동 IP 보기
        this.addRenderableWidget(CycleButton.booleanBuilder(
                        Component.literal("ON"), Component.literal("OFF"))
                .withInitialValue(NewpostsConfig.SHOW_IP_ADDRESS.get())
                .create(cx - w / 2, y + gap * 2, w, h,
                        Component.literal("유동 IP 보기"),
                        (btn, val) -> NewpostsConfig.SHOW_IP_ADDRESS.set(val)));

        // 식별코드 보기
        this.addRenderableWidget(CycleButton.booleanBuilder(
                        Component.literal("ON"), Component.literal("OFF"))
                .withInitialValue(NewpostsConfig.SHOW_UID.get())
                .create(cx - w / 2, y + gap * 3, w, h,
                        Component.literal("식별코드 보기"),
                        (btn, val) -> NewpostsConfig.SHOW_UID.set(val)));

        // 갤로그 체크
        this.addRenderableWidget(CycleButton.booleanBuilder(
                        Component.literal("ON"), Component.literal("OFF"))
                .withInitialValue(NewpostsConfig.CHECK_GALLOG.get())
                .create(cx - w / 2, y + gap * 4, w, h,
                        Component.literal("갤로그 체크"),
                        (btn, val) -> NewpostsConfig.CHECK_GALLOG.set(val)));

        // 깡계 글댓합 입력
        this.geuldethapBox = new EditBox(this.font, cx - w / 2, y + gap * 5, w, h, Component.literal("깡계 글댓합"));
        this.geuldethapBox.setValue(String.valueOf(NewpostsConfig.GEULDETHAP.get()));
        this.geuldethapBox.setFilter(s -> s.matches("\\d*"));
        this.addRenderableWidget(this.geuldethapBox);

        // 완료 버튼
        this.addRenderableWidget(Button.builder(Component.literal("완료"), btn -> this.onClose())
                .bounds(cx - 100, this.height - 35, 200, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        g.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);

        int cx = this.width / 2;
        int y = 50;
        int gap = 28;
        // EditBox 라벨
        g.drawString(this.font, "Gallery ID", cx - 110, y - 10, 0xA0A0A0);
        g.drawString(this.font, "깡계 글댓합 (글+댓 합계 기준)", cx - 110, y + gap * 5 - 10, 0xA0A0A0);

        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        // 값 저장
        NewpostsConfig.GALLERY_ID.set(this.galleryIdBox.getValue());
        String val = this.geuldethapBox.getValue();
        if (!val.isEmpty()) {
            try {
                NewpostsConfig.GEULDETHAP.set(Integer.parseInt(val));
            } catch (NumberFormatException ignored) {}
        }
        NewpostsConfig.SPEC.save();

        this.minecraft.setScreen(this.parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
