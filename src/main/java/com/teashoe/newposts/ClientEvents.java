package com.teashoe.newposts;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;

public class ClientEvents {

    public static final ClientEvents INSTANCE = new ClientEvents();

    private static final Logger LOGGER = LogManager.getLogger(Newposts.MOD_ID);
    private static final Set<String> currentPostNumbers = ConcurrentHashMap.newKeySet();
    private static final Map<String, String> ISP_MAP = new HashMap<>();
    // 메인 스레드에서 처리할 메시지 큐
    private static final Queue<IChatComponent> pendingMessages = new ConcurrentLinkedQueue<>();
    private static boolean ispLoaded = false;
    private static boolean initialized = false;
    private static String lastInitializedGalleryId = "";
    private static String invalidGalleryIdLogged = null;
    private static ScheduledExecutorService scheduler;

    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";

    // 연결 이벤트: FML 버스에 등록됨
    @SubscribeEvent
    public void onClientConnect(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        ClassLoader modClassLoader = ClientEvents.class.getClassLoader();
        Thread t = new Thread(() -> {
            try {
                initializePostNumbers();
                startScheduler();
            } catch (Exception e) {
                LOGGER.error("[newposts] 초기화 중 오류 발생", e);
            }
        });
        t.setContextClassLoader(modClassLoader);
        t.setDaemon(true);
        t.setName("newposts-init");
        t.start();
    }

    // 연결 해제 이벤트: FML 버스에 등록됨
    @SubscribeEvent
    public void onClientDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            scheduler = null;
        }
        currentPostNumbers.clear();
        pendingMessages.clear();
        initialized = false;
        lastInitializedGalleryId = "";
    }

    // 클라이언트 틱: 백그라운드 스레드가 쌓아둔 메시지를 메인 스레드에서 처리
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (pendingMessages.isEmpty()) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return;

        IChatComponent msg;
        while ((msg = pendingMessages.poll()) != null) {
            mc.thePlayer.addChatMessage(msg);
            // 알림음
            mc.thePlayer.playSound("random.orb", 1.0f, 1.0f);
        }
    }

    private static void startScheduler() {
        if (scheduler != null && !scheduler.isShutdown()) {
            LOGGER.warn("[newposts] 스케줄러가 이미 실행 중입니다. 중복 시작 방지됨.");
            return;
        }
        ClassLoader modClassLoader = ClientEvents.class.getClassLoader();
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r);
            t.setContextClassLoader(modClassLoader);
            t.setDaemon(true);
            t.setName("newposts-scheduler");
            return t;
        });
        scheduler.scheduleAtFixedRate(() -> {
            try {
                checkNewPosts();
            } catch (Exception e) {
                LOGGER.error("[newposts] 스케줄러 실행 중 오류: {}", e.getMessage(), e);
            }
        }, 5, 5, TimeUnit.SECONDS);
    }

    public static void initializePostNumbers() {
        initialized = false;
        String galleryId = NewpostsConfig.galleryId;
        String url = "https://gall.dcinside.com/mgallery/board/lists?id=" + galleryId;
        try {
            Document document = fetchDocument(url, USER_AGENT, "https://gall.dcinside.com/", 10000);
            Elements postList = document.select(".ub-content.us-post");
            currentPostNumbers.clear();
            for (Element postElement : postList) {
                String number = postElement.select(".gall_num").text();
                currentPostNumbers.add(number);
            }
            initialized = true;
            lastInitializedGalleryId = galleryId;
            LOGGER.info("[newposts] 초기화 완료 - {}개 게시물 등록됨", currentPostNumbers.size());
        } catch (IOException e) {
            LOGGER.error("[newposts] 게시물 초기화 실패 (갤러리: {}): {}", galleryId, e.getMessage(), e);
        } catch (Exception e) {
            LOGGER.error("[newposts] 게시물 초기화 중 예상치 못한 오류", e);
        }
    }

    private static void checkNewPosts() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return;

        if (!initialized) {
            initializePostNumbers();
            return;
        }

        String galleryId = NewpostsConfig.galleryId;

        if (!galleryId.equals(lastInitializedGalleryId)) {
            initializePostNumbers();
            return;
        }

        String url = "https://gall.dcinside.com/mgallery/board/lists?id=" + galleryId;

        try {
            Document document = fetchDocument(url, USER_AGENT, "https://gall.dcinside.com/", 10000);
            Elements postList = document.select(".ub-content.us-post");

            if (postList.isEmpty()) return;

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

                    // 깡계 체크 (steve 갤러리, 고정닉인 경우)
                    boolean kkanggye = false;
                    if (galleryId.equals("steve") && !dataUid.isEmpty()) {
                        String gallogUrl = "https://gallog.dcinside.com/" + dataUid;
                        try {
                            Document gallogDoc = fetchDocument(gallogUrl, USER_AGENT, null, 8000);
                            String postCountText = gallogDoc.select("h2.tit:contains(게시글) span.num")
                                    .text().replaceAll("[^0-9]", "");
                            int postCount = postCountText.isEmpty() ? 0 : Integer.parseInt(postCountText);

                            String commentCountText = gallogDoc.select("h2.tit:contains(댓글) span.num")
                                    .text().replaceAll("[^0-9]", "");
                            int commentCount = commentCountText.isEmpty() ? 0 : Integer.parseInt(commentCountText);

                            if (postCount + commentCount < NewpostsConfig.geuldethap) {
                                kkanggye = true;
                            }
                        } catch (IOException e) {
                            LOGGER.warn("[newposts] 갤로그 불러오기 실패 ({}): {}", dataUid, e.getMessage());
                        }
                    }

                    // 메시지 조립
                    IChatComponent message = buildMessage(
                            galleryId, number, title, author, dataIp, dataUid, subject, kkanggye);
                    pendingMessages.offer(message);
                }
            }
        } catch (IOException e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("404") && !galleryId.equals(invalidGalleryIdLogged)) {
                LOGGER.error("[newposts] galleryID가 잘못되었습니다. ({})", galleryId);
                invalidGalleryIdLogged = galleryId;
            } else if (msg == null || !msg.contains("404")) {
                LOGGER.error("[newposts] 게시물 가져오기 실패 (갤러리: {}): {}", galleryId, msg, e);
            }
        } catch (Exception e) {
            LOGGER.error("[newposts] 게시물 확인 중 예상치 못한 오류", e);
        }
    }

    private static IChatComponent buildMessage(
            String galleryId, String number, String title, String author,
            String dataIp, String dataUid, String subject, boolean kkanggye) {

        // 작성자 텍스트
        ChatComponentText authorText = new ChatComponentText("[" + author + "]");
        authorText.setChatStyle(new ChatStyle().setColor(EnumChatFormatting.WHITE));

        // IP 표시
        if (!dataIp.isEmpty() && NewpostsConfig.showIpAddress) {
            String[] parts = dataIp.split("\\.");
            if (parts.length >= 2) {
                String prefix = parts[0] + "." + parts[1];
                String ispLabel = getIspLabel(prefix);
                ChatComponentText ipComp;
                if (ispLabel != null) {
                    ipComp = new ChatComponentText(" (" + prefix + ")-" + ispLabel);
                    ipComp.setChatStyle(new ChatStyle().setColor(EnumChatFormatting.RED));
                } else {
                    ipComp = new ChatComponentText(" (" + dataIp + ")");
                    ipComp.setChatStyle(new ChatStyle().setColor(EnumChatFormatting.GRAY));
                }
                authorText.appendSibling(ipComp);
            }
        }

        // UID 표시
        if (!dataUid.isEmpty() && NewpostsConfig.showUid) {
            EnumChatFormatting uidColor = kkanggye ? EnumChatFormatting.RED : EnumChatFormatting.GRAY;
            ChatComponentText uidComp = new ChatComponentText(" [" + dataUid + "]");
            uidComp.setChatStyle(new ChatStyle().setColor(uidColor));
            authorText.appendSibling(uidComp);
        }

        // 말머리 표시
        ChatComponentText subjectComp = new ChatComponentText("");
        if (!subject.isEmpty()) {
            ChatComponentText subjectText = new ChatComponentText("[" + subject + "] ");
            subjectText.setChatStyle(new ChatStyle().setColor(EnumChatFormatting.AQUA));
            subjectComp.appendSibling(subjectText);
        }

        // [새글] 접두사
        ChatComponentText prefix = new ChatComponentText("[새글] ");
        prefix.setChatStyle(new ChatStyle().setColor(EnumChatFormatting.YELLOW));

        // 제목 (클릭 이벤트 포함)
        String postUrl = "https://gall.dcinside.com/mgallery/board/view/?id=" + galleryId + "&no=" + number;
        ChatComponentText titleComp = new ChatComponentText(title + " ");
        ChatStyle titleStyle = new ChatStyle()
                .setColor(EnumChatFormatting.WHITE)
                .setChatClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, postUrl))
                .setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        new ChatComponentText("게시물 보기")));
        titleComp.setChatStyle(titleStyle);
        titleComp.appendSibling(authorText);

        // 최종 조립: [새글] [말머리] 제목 [작성자]...
        prefix.appendSibling(subjectComp).appendSibling(titleComp);
        return prefix;
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
            LOGGER.error("[newposts] IP 데이터 로드 실패", e);
        }
    }

    private static String getIspLabel(String prefix) {
        loadIspData();
        return ISP_MAP.get(prefix);
    }

    private static Document fetchDocument(String url, String userAgent, String referer, int timeoutMs) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestProperty("User-Agent", userAgent);
        if (referer != null) conn.setRequestProperty("Referer", referer);
        conn.setConnectTimeout(timeoutMs);
        conn.setReadTimeout(timeoutMs);
        conn.setInstanceFollowRedirects(true);
        try (InputStream in = conn.getInputStream()) {
            return Jsoup.parse(in, "UTF-8", url);
        }
    }
}
