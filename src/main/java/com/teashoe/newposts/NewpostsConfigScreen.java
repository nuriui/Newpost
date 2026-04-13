package com.teashoe.newposts;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import org.lwjgl.input.Keyboard;

public class NewpostsConfigScreen extends GuiScreen {

    private final GuiScreen parent;

    private GuiTextField galleryIdField;
    private GuiTextField geuldethapField;

    // 토글 버튼 ID
    private static final int BTN_SYSTEM_CHAT = 10;
    private static final int BTN_SHOW_IP     = 11;
    private static final int BTN_SHOW_UID    = 12;
    private static final int BTN_DONE        = 20;

    // 토글 상태 (init에서 config에서 읽어옴)
    private boolean useSystemChat;
    private boolean showIpAddress;
    private boolean showUid;

    public NewpostsConfigScreen(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);

        useSystemChat = NewpostsConfig.useSystemChat;
        showIpAddress = NewpostsConfig.showIpAddress;
        showUid       = NewpostsConfig.showUid;

        int cx = this.width / 2;
        int y  = 50;
        int gap = 28;
        int w  = 220;
        int h  = 20;

        // Gallery ID 텍스트 필드
        galleryIdField = new GuiTextField(this.fontRendererObj, cx - w / 2, y, w, h);
        galleryIdField.setMaxStringLength(64);
        galleryIdField.setText(NewpostsConfig.galleryId);
        galleryIdField.setFocused(true);

        // 깡계 글댓합 텍스트 필드
        geuldethapField = new GuiTextField(this.fontRendererObj, cx - w / 2, y + gap * 4, w, h);
        geuldethapField.setMaxStringLength(10);
        geuldethapField.setText(String.valueOf(NewpostsConfig.geuldethap));

        // 토글 버튼들
        this.buttonList.add(new GuiButton(BTN_SYSTEM_CHAT, cx - w / 2, y + gap,     w, h,
                "액션바에 표시: " + onOff(useSystemChat)));
        this.buttonList.add(new GuiButton(BTN_SHOW_IP,     cx - w / 2, y + gap * 2, w, h,
                "유동 IP 보기: " + onOff(showIpAddress)));
        this.buttonList.add(new GuiButton(BTN_SHOW_UID,    cx - w / 2, y + gap * 3, w, h,
                "식별코드 보기: " + onOff(showUid)));

        // 완료 버튼
        this.buttonList.add(new GuiButton(BTN_DONE, cx - 100, this.height - 35, 200, h, "완료"));
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        int cx = this.width / 2;
        int w  = 220;
        int y  = 50;
        int gap = 28;

        switch (button.id) {
            case BTN_SYSTEM_CHAT:
                useSystemChat = !useSystemChat;
                button.displayString = "액션바에 표시: " + onOff(useSystemChat);
                break;
            case BTN_SHOW_IP:
                showIpAddress = !showIpAddress;
                button.displayString = "유동 IP 보기: " + onOff(showIpAddress);
                break;
            case BTN_SHOW_UID:
                showUid = !showUid;
                button.displayString = "식별코드 보기: " + onOff(showUid);
                break;
            case BTN_DONE:
                save();
                this.mc.displayGuiScreen(this.parent);
                break;
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        // ESC → 저장 후 닫기
        if (keyCode == Keyboard.KEY_ESCAPE) {
            save();
            this.mc.displayGuiScreen(this.parent);
            return;
        }
        // 숫자만 허용 (깡계 필드)
        if (geuldethapField.isFocused()) {
            if (Character.isDigit(typedChar) || keyCode == Keyboard.KEY_BACK
                    || keyCode == Keyboard.KEY_DELETE || keyCode == Keyboard.KEY_LEFT
                    || keyCode == Keyboard.KEY_RIGHT || keyCode == Keyboard.KEY_HOME
                    || keyCode == Keyboard.KEY_END) {
                geuldethapField.textboxKeyTyped(typedChar, keyCode);
            }
        } else {
            galleryIdField.textboxKeyTyped(typedChar, keyCode);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        galleryIdField.mouseClicked(mouseX, mouseY, mouseButton);
        geuldethapField.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void updateScreen() {
        galleryIdField.updateCursorCounter();
        geuldethapField.updateCursorCounter();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();

        // 제목
        this.drawCenteredString(this.fontRendererObj, "Newposts \uc124\uc815",
                this.width / 2, 10, 0xFFFFFF);

        int cx = this.width / 2;
        int y  = 50;
        int gap = 28;

        // 라벨
        this.drawString(this.fontRendererObj, "Gallery ID",
                cx - 110, y - 10, 0xA0A0A0);
        this.drawString(this.fontRendererObj, "\uae65\uacc4 \uae00\ub313\ud569 (\uae00+\ub313 \ud569\uacc4 \uae30\uc900)",
                cx - 110, y + gap * 4 - 10, 0xA0A0A0);

        galleryIdField.drawTextBox();
        geuldethapField.drawTextBox();

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private void save() {
        NewpostsConfig.galleryId    = galleryIdField.getText().trim();
        NewpostsConfig.useSystemChat = useSystemChat;
        NewpostsConfig.showIpAddress = showIpAddress;
        NewpostsConfig.showUid       = showUid;

        String val = geuldethapField.getText().trim();
        if (!val.isEmpty()) {
            try {
                NewpostsConfig.geuldethap = Integer.parseInt(val);
            } catch (NumberFormatException ignored) {}
        }
        NewpostsConfig.save();

        // 갤러리 ID가 바뀌었으면 재초기화
        ClientEvents.initializePostNumbers();
    }

    private static String onOff(boolean value) {
        return value ? "ON" : "OFF";
    }
}
