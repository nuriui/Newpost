package com.teashoe.newposts;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.text.MutableText;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.minecraft.util.ActionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.*;
import java.net.URI;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;

public class Newposts implements ClientModInitializer {

    private boolean newPostAlertEnabled = true;
    private final Set<String> currentPostNumbers = new HashSet<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private static final Logger LOGGER = LoggerFactory.getLogger("newposts");

    // ✅ 통신사 캐시
    private static final Map<String, String> ISP_MAP = new HashMap<>();
    private static boolean ispLoaded = false;

    @Override
    public void onInitializeClient() {
        AutoConfig.register(ModConfig.class, GsonConfigSerializer::new);

        AutoConfig.getConfigHolder(ModConfig.class).registerSaveListener((configHolder, newConfig) -> {
            initializePostNumbers();
            return ActionResult.SUCCESS;
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> initializePostNumbers());
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (newPostAlertEnabled && !scheduler.isShutdown()) {
                scheduler.scheduleAtFixedRate(() -> checkNewPosts(client), 0, 60, TimeUnit.SECONDS);
            }
        });
    }

    private void initializePostNumbers() {
        String galleryId = ModConfig.get().galleryId;
        String url = "https://gall.dcinside.com/mgallery/board/lists?id=" + galleryId;

        try {
            Document document = Jsoup.connect(url).get();
            Elements postList = document.select(".ub-content.us-post");
            for (Element postElement : postList) {
                String number = postElement.select(".gall_num").text();
                currentPostNumbers.add(number);
            }
        } catch (IOException e) {
            LOGGER.error("게시물을 초기화하는 중 오류 발생: {}", e.getMessage());
        }
    }

    private void checkNewPosts(MinecraftClient client) {
        if (client.player == null) return;

        String galleryId = ModConfig.get().galleryId;
        String url = "https://gall.dcinside.com/mgallery/board/lists?id=" + galleryId;

        try {
            Document document = Jsoup.connect(url).get();
            Elements postList = document.select(".ub-content.us-post");

            for (Element postElement : postList) {
                String number = postElement.select(".gall_num").text();
                if (!currentPostNumbers.contains(number)) {
                    currentPostNumbers.add(number);

                    String subject = "";
                    Element subjectElement = postElement.selectFirst(".gall_subject");
                    if (subjectElement != null) {
                        Element innerP = subjectElement.selectFirst(".subject_inner");
                        subject = (innerP != null ? innerP.text().trim() : subjectElement.text().trim());
                    }

                    String title = postElement.select(".gall_tit.ub-word").text();
                    String author = postElement.select(".gall_writer.ub-writer .nickname em").text();
                    String dataIp = postElement.select(".gall_writer").attr("data-ip");
                    String dataUid = postElement.select(".gall_writer").attr("data-uid");

                    MutableText authorText = Text.literal("[" + author + "]")
                            .styled(style -> style.withColor(Formatting.WHITE));

                    boolean kkanggye = false; // 깡계 체크

                    boolean kkanggyecheck = ModConfig.get().checkgallog;

                    if (kkanggyecheck && !dataUid.isEmpty()) { // kkanggyecheck가 true일 때만 실행
                        String gallogUrl = "https://gallog.dcinside.com/" + dataUid;
                        try {
                            Document gallogDoc = Jsoup.connect(gallogUrl).get();
                            String postCountText = gallogDoc.select("h2.tit:contains(게시글) span.num")
                                    .text().replaceAll("[^0-9]", "");
                            int postCount = postCountText.isEmpty() ? 0 : Integer.parseInt(postCountText);

                            String commentCountText = gallogDoc.select("h2.tit:contains(댓글) span.num")
                                    .text().replaceAll("[^0-9]", "");
                            int commentCount = commentCountText.isEmpty() ? 0 : Integer.parseInt(commentCountText);
                            int totalActivity = postCount + commentCount;
                            int lowActivity = ModConfig.get().geuldethap;
                            if (totalActivity < lowActivity) {
                                kkanggye = true;
                            }
                        } catch (IOException e) {
                            LOGGER.warn("갤로그 불러오기 실패 ({}): {}", dataUid, e.getMessage());
                        }
                    }

                    if (!dataIp.isEmpty() && ModConfig.get().showIpAddress) {
                        String prefix = dataIp.split("\\.")[0] + "." + dataIp.split("\\.")[1];
                        String ispLabel = getIspLabel(prefix);

                        if (ispLabel != null) {
                            authorText.append(Text.literal(" (" + prefix + ")-" + ispLabel)
                                    .styled(style -> style.withColor(Formatting.RED)));
                        } else {
                            authorText.append(Text.literal(" (" + dataIp + ")")
                                    .styled(style -> style.withColor(Formatting.GRAY)));
                        }
                    }

                    if (!dataUid.isEmpty() && ModConfig.get().showuid) {
                        Formatting uidColor = kkanggye ? Formatting.RED : Formatting.GRAY;
                        MutableText uidText = Text.literal(" [" + dataUid + "]")
                                .styled(style -> style.withColor(uidColor));
                        authorText.append(uidText);
                    }

                    MutableText subjectPrefix = Text.literal("");
                    if (!subject.isEmpty()) {
                        subjectPrefix.append(Text.literal("[" + subject + "] ")
                                .styled(style -> style.withColor(Formatting.AQUA)));
                    }

                    MutableText newPostPrefix = Text.literal("[새글] ")
                            .styled(style -> style.withColor(Formatting.YELLOW));

                    MutableText postDetails = Text.literal(title + " ")
                            .styled(style -> style
                                    .withClickEvent(new ClickEvent.OpenUrl(
                                            URI.create("https://gall.dcinside.com/mgallery/board/view/?id=" + galleryId + "&no=" + number)))
                                    .withHoverEvent(new HoverEvent.ShowText(Text.literal("게시물 보기")))
                                    .withColor(Formatting.WHITE)
                            ).append(authorText);

                    MutableText combinedPrefix = newPostPrefix.append(subjectPrefix);
                    MutableText clickableMessage = combinedPrefix.append(postDetails);

                    client.execute(() -> {
                        if (client.player != null) {
                            boolean useSystemChat = ModConfig.get().useSystemChat;
                            client.player.sendMessage(clickableMessage, useSystemChat);
                            client.player.playSound(SoundEvents.ENTITY_ARROW_HIT_PLAYER, 1.0F, 1.0F);
                        }
                    });
                }
            }

        } catch (IOException e) {
            LOGGER.error("게시물을 가져오는 중 오류 발생: {}", e.getMessage());
        }
    }

    // ✅ 통신사 데이터 로드
    private void loadIspData() {
        if (ispLoaded) return;
        ispLoaded = true;

        try (InputStream input = getClass().getResourceAsStream("/merged_ip_list.txt")) {
            if (input == null) {
                LOGGER.warn("merged_ip_list.txt 파일을 찾을 수 없습니다.");
                return;
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || !line.contains("-")) continue;
                    String[] parts = line.split("-", 2);
                    if (parts.length == 2) {
                        ISP_MAP.put(parts[0].trim(), parts[1].trim());
                    }
                }
                LOGGER.info("IP 데이터 {}개 로드 완료.", ISP_MAP.size());
            }
        } catch (Exception e) {
            LOGGER.error("IP 데이터 로드 실패: {}", e.getMessage());
        }
    }

    private String getIspLabel(String prefix) {
        loadIspData();
        return ISP_MAP.get(prefix);
    }
}
