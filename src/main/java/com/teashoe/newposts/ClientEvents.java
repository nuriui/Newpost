package com.teashoe.newposts;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Mod.EventBusSubscriber(modid = Newposts.MOD_ID, value = Dist.CLIENT)
public class ClientEvents {

    private static final Logger LOGGER = LoggerFactory.getLogger("newposts");
    // ConcurrentHashMap 기반 Set: 스케줄러 스레드와 config 리로드 스레드가 동시에 접근해도 안전
    private static final Set<String> currentPostNumbers = ConcurrentHashMap.newKeySet();
    private static final Map<String, String> ISP_MAP = new HashMap<>();
    private static boolean ispLoaded = false;
    private static ScheduledExecutorService scheduler;

    // DC Inside 접속 시 필요한 브라우저 헤더
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";

    @SubscribeEvent
    public static void onPlayerLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        // Forge mod classloader를 유지하는 스레드에서 초기화 (AppClassLoader로 jsoup이 로드되는 문제 방지)
        ClassLoader modClassLoader = ClientEvents.class.getClassLoader();
        Thread t = new Thread(() -> {
            initializePostNumbers();
            startScheduler();
        });
        t.setContextClassLoader(modClassLoader);
        t.setDaemon(true);
        t.start();
    }

    @SubscribeEvent
    public static void onPlayerLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            scheduler = null;
        }
        currentPostNumbers.clear();
    }

    private static void startScheduler() {
        if (scheduler != null && !scheduler.isShutdown()) return;
        // Forge mod classloader를 스케줄러 스레드에도 전달
        ClassLoader modClassLoader = ClientEvents.class.getClassLoader();
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r);
            t.setContextClassLoader(modClassLoader);
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(() -> {
            // try-catch 필수: 예외가 밖으로 나가면 scheduleAtFixedRate가 조용히 중단됨
            try {
                Minecraft mc = Minecraft.getInstance();
                checkNewPosts(mc);
            } catch (Exception e) {
                LOGGER.error("[newposts] 스케줄러 실행 중 오류 (다음 주기에 재시도): {}", e.getMessage(), e);
            }
        }, 5, 5, TimeUnit.SECONDS);
    }

    public static void initializePostNumbers() {
        String galleryId = NewpostsConfig.GALLERY_ID.get();
        String url = "https://gall.dcinside.com/mgallery/board/lists?id=" + galleryId;

        try {
            Document document = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .header("Referer", "https://gall.dcinside.com/")
                    .timeout(10000)
                    .get();
            Elements postList = document.select(".ub-content.us-post");
            currentPostNumbers.clear();
            for (Element postElement : postList) {
                String number = postElement.select(".gall_num").text();
                currentPostNumbers.add(number);
            }
        } catch (IOException e) {
            LOGGER.error("[newposts] 게시물 초기화 실패: {}", e.getMessage());
        }
    }

    private static void checkNewPosts(Minecraft client) {
        if (client.player == null) return;

        String galleryId = NewpostsConfig.GALLERY_ID.get();
        String url = "https://gall.dcinside.com/mgallery/board/lists?id=" + galleryId;

        try {
            Document document = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .header("Referer", "https://gall.dcinside.com/")
                    .timeout(10000)
                    .get();
            Elements postList = document.select(".ub-content.us-post");

            if (postList.isEmpty()) {
                LOGGER.warn("[newposts] 게시물 목록이 비어있습니다. 갤러리 ID를 확인하세요: {}", galleryId);
                return;
            }

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

                    boolean kkanggye = false;
                    if (NewpostsConfig.CHECK_GALLOG.get() && !dataUid.isEmpty()) {
                        String gallogUrl = "https://gallog.dcinside.com/" + dataUid;
                        try {
                            Document gallogDoc = Jsoup.connect(gallogUrl)
                                    .userAgent(USER_AGENT)
                                    .timeout(8000)
                                    .get();
                            String postCountText = gallogDoc.select("h2.tit:contains(게시글) span.num")
                                    .text().replaceAll("[^0-9]", "");
                            int postCount = postCountText.isEmpty() ? 0 : Integer.parseInt(postCountText);

                            String commentCountText = gallogDoc.select("h2.tit:contains(댓글) span.num")
                                    .text().replaceAll("[^0-9]", "");
                            int commentCount = commentCountText.isEmpty() ? 0 : Integer.parseInt(commentCountText);

                            if (postCount + commentCount < NewpostsConfig.GEULDETHAP.get()) {
                                kkanggye = true;
                            }
                        } catch (IOException e) {
                            LOGGER.warn("[newposts] 갤로그 불러오기 실패 ({}): {}", dataUid, e.getMessage());
                        }
                    }

                    if (!dataIp.isEmpty() && NewpostsConfig.SHOW_IP_ADDRESS.get()) {
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

                    if (!dataUid.isEmpty() && NewpostsConfig.SHOW_UID.get()) {
                        ChatFormatting uidColor = kkanggye ? ChatFormatting.RED : ChatFormatting.GRAY;
                        authorText.append(Component.literal(" [" + dataUid + "]")
                                .withStyle(style -> style.withColor(uidColor)));
                    }

                    MutableComponent subjectPrefix = Component.literal("");
                    if (!subject.isEmpty()) {
                        subjectPrefix.append(Component.literal("[" + subject + "] ")
                                .withStyle(style -> style.withColor(ChatFormatting.AQUA)));
                    }

                    MutableComponent newPostPrefix = Component.literal("[새글] ")
                            .withStyle(style -> style.withColor(ChatFormatting.YELLOW));

                    String postUrl = "https://gall.dcinside.com/mgallery/board/view/?id=" + galleryId + "&no=" + number;
                    MutableComponent postDetails = Component.literal(title + " ")
                            .withStyle(style -> style
                                    .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, postUrl))
                                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("게시물 보기")))
                                    .withColor(ChatFormatting.WHITE)
                            ).append(authorText);

                    MutableComponent clickableMessage = newPostPrefix.append(subjectPrefix).append(postDetails);

                    client.execute(() -> {
                        if (client.player != null) {
                            client.player.displayClientMessage(clickableMessage, NewpostsConfig.USE_SYSTEM_CHAT.get());
                            client.player.playSound(SoundEvents.ARROW_HIT_PLAYER, 1.0F, 1.0F);
                        }
                    });
                }
            }
        } catch (IOException e) {
            LOGGER.error("[newposts] 게시물 가져오기 실패: {}", e.getMessage());
        }
    }

    private static void loadIspData() {
        if (ispLoaded) return;
        ispLoaded = true;

        try (InputStream input = ClientEvents.class.getResourceAsStream("/merged_ip_list.txt")) {
            if (input == null) {
                LOGGER.warn("[newposts] merged_ip_list.txt 파일을 찾을 수 없습니다.");
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
            }
        } catch (Exception e) {
            LOGGER.error("[newposts] IP 데이터 로드 실패: {}", e.getMessage());
        }
    }

    private static String getIspLabel(String prefix) {
        loadIspData();
        return ISP_MAP.get(prefix);
    }
}
