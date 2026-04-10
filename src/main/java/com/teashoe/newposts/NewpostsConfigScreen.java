package com.teashoe.newposts;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NewpostsConfigScreen extends Screen {

    private final Screen parent;

    private EditBox galleryIdBox;
    private EditBox geuldethapBox;
    private final List<Map.Entry<AbstractWidget, Component>> tooltipMap = new ArrayList<>();

    public NewpostsConfigScreen(Screen parent) {
        super(Component.literal("Newposts 설정"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        tooltipMap.clear();
        int cx = this.width / 2;
        int y = 50;
        int gap = 28;
        int w = 220;
        int h = 20;

        // Gallery ID 입력
        this.galleryIdBox = new EditBox(this.font, cx - w / 2, y, w, h, Component.literal("Gallery ID"));
        this.galleryIdBox.setValue(NewpostsConfig.GALLERY_ID.get());
        this.addRenderableWidget(this.galleryIdBox);
        tooltipMap.add(Map.entry(this.galleryIdBox, Component.literal("알림을 받을 갤러리 ID")));

        // 액션바에 표시
        CycleButton<Boolean> useSystemChatBtn = CycleButton.booleanBuilder(
                        Component.literal("ON"), Component.literal("OFF"))
                .withInitialValue(NewpostsConfig.USE_SYSTEM_CHAT.get())
                .create(cx - w / 2, y + gap, w, h,
                        Component.literal("액션바에 표시"),
                        (btn, val) -> NewpostsConfig.USE_SYSTEM_CHAT.set(val));
        this.addRenderableWidget(useSystemChatBtn);
        tooltipMap.add(Map.entry(useSystemChatBtn, Component.literal("ON: 액션바에 표시 / OFF: 채팅창에 표시")));

        // 유동 IP 보기
        CycleButton<Boolean> showIpBtn = CycleButton.booleanBuilder(
                        Component.literal("ON"), Component.literal("OFF"))
                .withInitialValue(NewpostsConfig.SHOW_IP_ADDRESS.get())
                .create(cx - w / 2, y + gap * 2, w, h,
                        Component.literal("유동 IP 보기"),
                        (btn, val) -> NewpostsConfig.SHOW_IP_ADDRESS.set(val));
        this.addRenderableWidget(showIpBtn);
        tooltipMap.add(Map.entry(showIpBtn, Component.literal("유동닉 게시물의 IP 앞자리 표시")));

        // 식별코드 보기
        CycleButton<Boolean> showUidBtn = CycleButton.booleanBuilder(
                        Component.literal("ON"), Component.literal("OFF"))
                .withInitialValue(NewpostsConfig.SHOW_UID.get())
                .create(cx - w / 2, y + gap * 3, w, h,
                        Component.literal("식별코드 보기"),
                        (btn, val) -> NewpostsConfig.SHOW_UID.set(val));
        this.addRenderableWidget(showUidBtn);
        tooltipMap.add(Map.entry(showUidBtn, Component.literal("고정닉 게시물의 식별코드 표시")));

        // 갤로그 체크
        CycleButton<Boolean> checkGallogBtn = CycleButton.booleanBuilder(
                        Component.literal("ON"), Component.literal("OFF"))
                .withInitialValue(NewpostsConfig.CHECK_GALLOG.get())
                .create(cx - w / 2, y + gap * 4, w, h,
                        Component.literal("갤로그 체크"),
                        (btn, val) -> NewpostsConfig.CHECK_GALLOG.set(val));
        this.addRenderableWidget(checkGallogBtn);
        tooltipMap.add(Map.entry(checkGallogBtn, Component.literal("스티브갤에서만 동작 / 깡계 여부를 갤로그로 확인")));

        // 깡계 글댓합 입력
        this.geuldethapBox = new EditBox(this.font, cx - w / 2, y + gap * 5, w, h, Component.literal("깡계 글댓합"));
        this.geuldethapBox.setValue(String.valueOf(NewpostsConfig.GEULDETHAP.get()));
        this.geuldethapBox.setFilter(s -> s.matches("\\d*"));
        this.addRenderableWidget(this.geuldethapBox);
        tooltipMap.add(Map.entry(this.geuldethapBox, Component.literal("이 값보다 글+댓 합계가 낮으면 깡계로 표시")));

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

        for (Map.Entry<AbstractWidget, Component> entry : tooltipMap) {
            AbstractWidget widget = entry.getKey();
            if (mouseX >= widget.getX() && mouseX <= widget.getX() + widget.getWidth()
                    && mouseY >= widget.getY() && mouseY <= widget.getY() + widget.getHeight()) {
                g.renderComponentTooltip(this.font, List.of(entry.getValue()), mouseX, mouseY);
                break;
            }
        }
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
