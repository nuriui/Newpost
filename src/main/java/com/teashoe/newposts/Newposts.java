package com.teashoe.newposts;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.*;
import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;

public class Newposts implements ClientModInitializer {

    private boolean newPostAlertEnabled = true;
    private final Set<String> currentPostNumbers = ConcurrentHashMap.newKeySet();
    private String invalidGalleryIdLogged = null;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(task -> {
        Thread thread = new Thread(task, "newposts-scheduler");
        thread.setDaemon(true);
        return thread;
    });
    private ScheduledFuture<?> pollingTask;
    private static final Logger LOGGER = LoggerFactory.getLogger("newposts");

    // ✅ 통신사 캐시
    private static final Map<String, String> ISP_MAP = new HashMap<>();
    private static boolean ispLoaded = false;

    @Override
    public void onInitializeClient() {
        AutoConfig.register(ModConfig.class, GsonConfigSerializer::new);

        AutoConfig.getConfigHolder(ModConfig.class).registerSaveListener((configHolder, newConfig) -> {
            initializePostNumbers();
            return InteractionResult.SUCCESS;
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            currentPostNumbers.clear();
            scheduler.execute(this::initializePostNumbers);
            startPolling(client);
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> stopPolling());
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> shutdownScheduler());
    }

    private synchronized void startPolling(Minecraft client) {
        stopPolling();
        if (!newPostAlertEnabled || scheduler.isShutdown()) return;

        pollingTask = scheduler.scheduleAtFixedRate(
                () -> checkNewPosts(client), 1, 1, TimeUnit.SECONDS);
    }

    private synchronized void stopPolling() {
        if (pollingTask != null) {
            pollingTask.cancel(true);
            pollingTask = null;
        }
    }

    private void shutdownScheduler() {
        stopPolling();
        scheduler.shutdownNow();
        currentPostNumbers.clear();
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

    private void checkNewPosts(Minecraft client) {
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

                    MutableComponent authorText = Component.literal("[" + author + "]")
                            .withStyle(style -> style.withColor(ChatFormatting.WHITE));

                    boolean kkanggye = false; // 깡계 체크

                    boolean kkanggyecheck = galleryId.equals("steve");

                    if (kkanggyecheck && !dataUid.isEmpty()) { // kkanggyecheck가 true이고 갤러리가 steve일 때만 실행
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
                            authorText.append(Component.literal(" (" + prefix + ")-" + ispLabel)
                                    .withStyle(style -> style.withColor(ChatFormatting.RED)));
                        } else {
                            authorText.append(Component.literal(" (" + dataIp + ")")
                                    .withStyle(style -> style.withColor(ChatFormatting.GRAY)));
                        }
                    }

                    if (!dataUid.isEmpty() && ModConfig.get().showuid) {
                        ChatFormatting uidColor = kkanggye ? ChatFormatting.RED : ChatFormatting.GRAY;
                        MutableComponent uidText = Component.literal(" [" + dataUid + "]")
                                .withStyle(style -> style.withColor(uidColor));
                        authorText.append(uidText);
                    }

                    MutableComponent subjectPrefix = Component.literal("");
                    if (!subject.isEmpty()) {
                        subjectPrefix.append(Component.literal("[" + subject + "] ")
                                .withStyle(style -> style.withColor(ChatFormatting.AQUA)));
                    }

                    MutableComponent newPostPrefix = Component.literal("[새글] ")
                            .withStyle(style -> style.withColor(ChatFormatting.YELLOW));

                    MutableComponent postDetails = Component.literal(title + " ")
                            .withStyle(style -> style
                                    .withClickEvent(new ClickEvent.OpenUrl(
                                            URI.create("https://gall.dcinside.com/mgallery/board/view/?id=" + galleryId + "&no=" + number)))
                                    .withHoverEvent(new HoverEvent.ShowText(Component.literal("게시물 보기")))
                                    .withColor(ChatFormatting.WHITE)
                            ).append(authorText);

                    MutableComponent combinedPrefix = newPostPrefix.append(subjectPrefix);
                    MutableComponent clickableMessage = combinedPrefix.append(postDetails);

                    client.execute(() -> {
                        if (client.player != null) {
                            boolean useSystemChat = ModConfig.get().useSystemChat;
                            if (useSystemChat) {
                                client.player.sendOverlayMessage(clickableMessage);
                            } else {
                                client.player.sendSystemMessage(clickableMessage);
                            }
                            client.player.playSound(SoundEvents.ARROW_HIT_PLAYER, 1.0F, 1.0F);
                        }
                    });
                }
            }

        } catch (HttpStatusException e) {
            if (e.getStatusCode() == 404 && !galleryId.equals(invalidGalleryIdLogged)) {
                LOGGER.error("galleryID가 잘못되었습니다. ({})", galleryId);
                invalidGalleryIdLogged = galleryId;
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
