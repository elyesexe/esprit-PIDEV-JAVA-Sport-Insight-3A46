package tn.esprit.gui;

import javafx.application.Platform;
import javafx.embed.swing.SwingNode;
import javafx.scene.layout.StackPane;
import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.CefSettings;
import org.cef.SystemBootstrap;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefAppHandlerAdapter;
import org.cef.handler.CefLoadHandler;
import org.cef.handler.CefLoadHandlerAdapter;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executors;

public class ChromiumBrowserView extends StackPane {
    private static final Object CEF_APP_LOCK = new Object();
    private static CompletableFuture<CefApp> cefAppFuture;
    private static volatile Throwable lastError;
    private static boolean appHandlerRegistered;
    private static boolean nativeLibrariesLoaded;
    private static LocalPlayerServer localPlayerServer;

    private final SwingNode swingNode = new SwingNode();
    private CefClient client;
    private CefBrowser browser;

    public ChromiumBrowserView() {
        getChildren().setAll(swingNode);
        setStyle("-fx-background-color: #000000;");
    }

    public CompletableFuture<Boolean> loadUrl(String url) {
        return cefApp()
                .thenCompose(ChromiumBrowserView::ensureCefReady)
                .thenCompose(this::ensureBrowser)
                .thenApply(created -> {
                    SwingUtilities.invokeLater(() -> browser.loadURL(url));
                    return true;
                })
                .exceptionally(throwable -> {
                    lastError = unwrap(throwable);
                    lastError.printStackTrace(System.err);
                    return false;
                });
    }

    public void clear() {
        if (browser != null) {
            SwingUtilities.invokeLater(() -> browser.loadURL("about:blank"));
        }
    }

    public static CompletableFuture<Boolean> openPlayerWindow(String url, String title) {
        return openPlayerWindow(url, title, false);
    }

    public static CompletableFuture<Boolean> openPlayerWindow(String url, String title, boolean maximized) {
        return cefApp()
                .thenCompose(ChromiumBrowserView::ensureCefReady)
                .thenCompose(app -> {
                    CompletableFuture<Boolean> future = new CompletableFuture<>();
                    SwingUtilities.invokeLater(() -> {
                        try {
                            CefClient windowClient = app.createClient();
                            JLabel statusLabel = new JLabel("Loading highlight...", SwingConstants.CENTER);
                            statusLabel.setOpaque(true);
                            statusLabel.setBackground(new Color(5, 9, 21));
                            statusLabel.setForeground(Color.WHITE);
                            statusLabel.setFont(statusLabel.getFont().deriveFont(Font.BOLD, 14f));
                            windowClient.addLoadHandler(new CefLoadHandlerAdapter() {
                                @Override
                                public void onLoadEnd(CefBrowser browser, CefFrame frame, int httpStatusCode) {
                                    SwingUtilities.invokeLater(() -> statusLabel.setVisible(false));
                                }

                                @Override
                                public void onLoadError(CefBrowser browser, CefFrame frame, CefLoadHandler.ErrorCode errorCode, String errorText, String failedUrl) {
                                    lastError = new IllegalStateException(errorCode + ": " + errorText + " (" + failedUrl + ")");
                                    lastError.printStackTrace(System.err);
                                    SwingUtilities.invokeLater(() -> {
                                        statusLabel.setText("<html><div style='text-align:center'>Could not load highlight<br><span style='font-size:12px;color:#cbd5e1'>"
                                                + errorText + "</span></div></html>");
                                        statusLabel.setVisible(true);
                                    });
                                }
                            });
                            CefBrowser windowBrowser = windowClient.createBrowser(url, false, false);
                            Component browserComponent = windowBrowser.getUIComponent();
                            browserComponent.setPreferredSize(new Dimension(1280, 720));
                            browserComponent.setVisible(true);
                            JPanel panel = new JPanel(new BorderLayout());
                            panel.setBackground(Color.BLACK);
                            panel.add(browserComponent, BorderLayout.CENTER);
                            panel.add(statusLabel, BorderLayout.SOUTH);
                            panel.setPreferredSize(new Dimension(1280, 720));

                            JFrame frame = new JFrame(clean(title) == null ? "Sport Insight Highlights" : title);
                            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                            frame.setBackground(Color.BLACK);
                            frame.setContentPane(panel);
                            frame.pack();
                            if (maximized) {
                                frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
                            }
                            frame.setLocationRelativeTo(null);
                            frame.setVisible(true);
                            panel.revalidate();
                            panel.repaint();
                            windowBrowser.createImmediately();
                            SwingUtilities.invokeLater(() -> {
                                windowBrowser.loadURL(url);
                                browserComponent.requestFocusInWindow();
                            });
                            Timer retryTimer = new Timer(1200, event -> windowBrowser.loadURL(url));
                            retryTimer.setRepeats(false);
                            retryTimer.start();
                            frame.addWindowListener(new java.awt.event.WindowAdapter() {
                                @Override
                                public void windowClosed(java.awt.event.WindowEvent event) {
                                    retryTimer.stop();
                                    windowBrowser.close(true);
                                    windowClient.dispose();
                                }
                            });
                            future.complete(true);
                        } catch (Throwable throwable) {
                            lastError = unwrap(throwable);
                            lastError.printStackTrace(System.err);
                            future.complete(false);
                        }
                    });
                    return future;
                })
                .exceptionally(throwable -> {
                    lastError = unwrap(throwable);
                    lastError.printStackTrace(System.err);
                    return false;
                });
    }

    public static CompletableFuture<Boolean> openYouTubePlayerWindow(String videoId, String title) {
        return openYouTubePlayerWindow(videoId, title, false);
    }

    public static CompletableFuture<Boolean> openYouTubePlayerWindow(String videoId, String title, boolean maximized) {
        return openYouTubePlayerWindow(videoId, null, title, maximized);
    }

    public static CompletableFuture<Boolean> openYouTubePlayerWindow(
            String videoId,
            String watchUrl,
            String title,
            boolean maximized
    ) {
        String cleanVideoId = clean(videoId);
        if (cleanVideoId == null) {
            lastError = new IllegalArgumentException("Missing YouTube video id.");
            return CompletableFuture.completedFuture(false);
        }

        try {
            String playerUrl = localPlayerServer().playerUrl(cleanVideoId, watchUrl);
            return openPlayerWindow(playerUrl, title, maximized);
        } catch (IOException e) {
            lastError = e;
            e.printStackTrace(System.err);
            return CompletableFuture.completedFuture(false);
        }
    }

    private CompletableFuture<Boolean> ensureBrowser(CefApp app) {
        if (browser != null) {
            return CompletableFuture.completedFuture(true);
        }

        CompletableFuture<Boolean> future = new CompletableFuture<>();
        SwingUtilities.invokeLater(() -> {
            try {
                client = app.createClient();
                browser = client.createBrowser("about:blank", false, false);

                JPanel panel = new JPanel(new BorderLayout());
                panel.add(browser.getUIComponent(), BorderLayout.CENTER);
                Platform.runLater(() -> {
                    swingNode.setContent(panel);
                    browser.createImmediately();
                    future.complete(true);
                });
            } catch (Throwable throwable) {
                lastError = throwable;
                throwable.printStackTrace(System.err);
                future.completeExceptionally(throwable);
            }
        });
        return future;
    }

    private static CompletableFuture<CefApp> ensureCefReady(CefApp app) {
        if (!isJbrJcefRuntime() || isCefState("INITIALIZED")) {
            return CompletableFuture.completedFuture(app);
        }

        return CompletableFuture.supplyAsync(() -> {
            long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(20);
            while (System.nanoTime() < deadline) {
                String state = cefStateName();
                if ("INITIALIZED".equals(state)) {
                    return app;
                }
                if ("INITIALIZATION_FAILED".equals(state)
                        || "TERMINATED".equals(state)
                        || "SHUTTING_DOWN".equals(state)) {
                    throw new IllegalStateException("JCEF initialization failed: " + state);
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new CompletionException(e);
                }
            }
            throw new IllegalStateException("JCEF initialization timed out: " + cefStateName());
        });
    }

    private static boolean isCefState(String expected) {
        return expected.equals(cefStateName());
    }

    private static String cefStateName() {
        Object state = CefApp.getState();
        return state == null ? "" : state.toString();
    }

    public static String getLastErrorMessage() {
        Throwable error = lastError;
        if (error == null) {
            return "No JCEF error details were reported.";
        }
        Throwable root = unwrap(error);
        String message = root.getMessage();
        return root.getClass().getSimpleName() + (message == null || message.isBlank() ? "" : ": " + message);
    }

    private static CompletableFuture<CefApp> cefApp() {
        synchronized (CEF_APP_LOCK) {
            if (cefAppFuture == null || cefAppFuture.isCompletedExceptionally()) {
                cefAppFuture = CompletableFuture.supplyAsync(ChromiumBrowserView::buildCefApp);
            }
            return cefAppFuture;
        }
    }

    private static LocalPlayerServer localPlayerServer() throws IOException {
        synchronized (CEF_APP_LOCK) {
            if (localPlayerServer == null) {
                localPlayerServer = new LocalPlayerServer();
            }
            return localPlayerServer;
        }
    }

    private static CefApp buildCefApp() {
        try {
            boolean jbrJcefRuntime = isJbrJcefRuntime();
            File installDir = jbrJcefRuntime ? resolveJbrJcefDir() : resolveInstallDir();
            if (!isInstalledBundle(installDir)) {
                throw new IllegalStateException("JCEF bundle is missing or incomplete at " + installDir.getAbsolutePath());
            }

            JbrCefConfig jbrConfig = jbrJcefRuntime ? loadJbrCefConfig() : null;
            Path installPath = installDir.toPath().toAbsolutePath().normalize();
            Path cachePath = jbrJcefRuntime
                    ? Path.of(System.getProperty("user.dir"), "jcef-cache", "runtime-" + ProcessHandle.current().pid()).toAbsolutePath().normalize()
                    : installPath.resolve("cache");
            Files.createDirectories(cachePath);

            List<String> args = new ArrayList<>(jbrConfig == null ? List.of() : List.of(jbrConfig.args()));
            appendDisableFeature(args, "CalculateNativeWinOcclusion");
            args.add("--autoplay-policy=no-user-gesture-required");
            args.add("--disable-gpu");
            args.add("--disable-gpu-compositing");
            args.add("--disable-direct-composition");
            args.add("--disable-background-timer-throttling");
            args.add("--disable-backgrounding-occluded-windows");
            args.add("--disable-renderer-backgrounding");
            args.add("--user-data-dir=" + cachePath);
            if (!jbrJcefRuntime) {
                args.add("--browser-subprocess-path=" + installPath.resolve("jcef_helper.exe"));
                args.add("--resources-dir-path=" + installPath);
                args.add("--locales-dir-path=" + installPath.resolve("locales"));
            }

            CefSettings settings = jbrConfig == null ? new CefSettings() : jbrConfig.settings().clone();
            settings.windowless_rendering_enabled = false;
            settings.persist_session_cookies = true;
            if (!jbrJcefRuntime) {
                settings.browser_subprocess_path = installPath.resolve("jcef_helper.exe").toString();
                settings.resources_dir_path = installPath.toString();
                settings.locales_dir_path = installPath.resolve("locales").toString();
            }
            settings.cache_path = cachePath.toString();
            settings.log_file = cachePath.resolve("debug.log").toString();

            String[] cefArgs = args.toArray(String[]::new);
            File runtimeFile = installDir;
            if (jbrJcefRuntime) {
                if (jbrConfig.loader() != null) {
                    SystemBootstrap.setLoader(jbrConfig.loader());
                }
                CefApp.startup(cefArgs);
                runtimeFile = jbrConfig.serverExe();
            } else {
                loadNativeLibraries(installDir);
            }
            registerAppHandler(cefArgs);
            CefApp app = createCefApp(cefArgs, settings, runtimeFile);
            lastError = null;
            return app;
        } catch (Throwable e) {
            lastError = e;
            e.printStackTrace(System.err);
            throw new IllegalStateException("Embedded Chromium is unavailable.", e);
        }
    }

    private static JbrCefConfig loadJbrCefConfig() {
        try {
            Class<?> configClass = Class.forName("com.jetbrains.cef.JCefAppConfig");
            Object config = configClass.getMethod("getInstance").invoke(null);
            String[] args = (String[]) configClass.getMethod("getAppArgs").invoke(config);
            CefSettings settings = (CefSettings) configClass.getMethod("getCefSettings").invoke(config);
            SystemBootstrap.Loader loader = (SystemBootstrap.Loader) configClass.getMethod("getLoader").invoke(config);
            File serverExe = (File) configClass.getMethod("getServerExe").invoke(config);
            return new JbrCefConfig(args == null ? new String[0] : args, settings, loader, serverExe);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not read JetBrains JCEF configuration.", e);
        }
    }

    private static void appendDisableFeature(List<String> args, String feature) {
        for (int index = 0; index < args.size(); index++) {
            String arg = args.get(index);
            if (arg != null && arg.startsWith("--disable-features=")) {
                if (!arg.contains(feature)) {
                    args.set(index, arg + "," + feature);
                }
                return;
            }
        }
        args.add("--disable-features=" + feature);
    }

    private static void loadNativeLibraries(File installDir) {
        if (nativeLibrariesLoaded) {
            return;
        }

        String libraryPath = System.getProperty("java.library.path", "");
        String installPath = installDir.getAbsolutePath();
        if (!libraryPath.contains(installPath)) {
            System.setProperty("java.library.path", libraryPath
                    + (libraryPath.endsWith(File.pathSeparator) || libraryPath.isBlank() ? "" : File.pathSeparator)
                    + installPath);
        }

        SystemBootstrap.setLoader(libraryName -> {
            // Native libraries are loaded explicitly from jcef-bundle.
        });

        try {
            System.loadLibrary("jawt");
        } catch (UnsatisfiedLinkError ignored) {
            // JavaFX/Swing may have loaded JAWT already.
        }

        if (isWindows()) {
            loadLibraryFile(installDir, "chrome_elf.dll");
            loadLibraryFile(installDir, "libcef.dll");
            loadLibraryFile(installDir, "jcef.dll");
        } else if (isLinux()) {
            loadLibraryFile(installDir, "libjcef.so");
            CefApp.startup(new String[0]);
            loadLibraryFile(installDir, "libcef.so");
        } else if (isMac()) {
            loadLibraryFile(installDir, "libjcef.dylib");
        }

        nativeLibrariesLoaded = true;
    }

    private static void loadLibraryFile(File installDir, String fileName) {
        File library = new File(installDir, fileName);
        if (!library.isFile()) {
            throw new IllegalStateException("Missing JCEF native library: " + library.getAbsolutePath());
        }
        try {
            System.load(library.getAbsolutePath());
        } catch (UnsatisfiedLinkError e) {
            String message = e.getMessage();
            if (message == null || !message.toLowerCase().contains("already loaded")) {
                throw e;
            }
        }
    }

    private static void registerAppHandler(String[] cefArgs) {
        if (appHandlerRegistered) {
            return;
        }
        try {
            CefApp.addAppHandler(new CefAppHandlerAdapter(cefArgs) {
            });
        } catch (IllegalStateException ignored) {
            // CEF was already initialized; use the running instance.
        }
        appHandlerRegistered = true;
    }

    private static CefApp createCefApp(String[] cefArgs, CefSettings settings, File installDir)
            throws ReflectiveOperationException {
        for (CefAppFactory factory : List.<CefAppFactory>of(
                () -> invokeCefApp("getInstance", new Class<?>[]{String[].class, CefSettings.class, File.class}, cefArgs, settings, installDir),
                () -> invokeCefApp("getInstance", new Class<?>[]{String[].class, CefSettings.class}, cefArgs, settings),
                () -> invokeCefApp("getInstance", new Class<?>[]{CefSettings.class}, settings),
                () -> invokeCefApp("getInstance", new Class<?>[]{String[].class}, (Object) cefArgs),
                () -> invokeCefApp("getInstance", new Class<?>[0])
        )) {
            try {
                return factory.create();
            } catch (NoSuchMethodException ignored) {
                // Try the next JCEF API shape.
            }
        }
        throw new NoSuchMethodException("No supported CefApp.getInstance overload exists on the runtime classpath.");
    }

    private static CefApp invokeCefApp(String methodName, Class<?>[] parameterTypes, Object... args)
            throws ReflectiveOperationException {
        try {
            Method method = CefApp.class.getMethod(methodName, parameterTypes);
            return (CefApp) method.invoke(null, args);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof ReflectiveOperationException reflectiveOperationException) {
                throw reflectiveOperationException;
            }
            throw new ReflectiveOperationException(cause == null ? e : cause);
        }
    }

    private static File resolveInstallDir() {
        String configured = firstNonBlank(
                System.getProperty("sportinsight.jcef.dir"),
                System.getenv("SPORT_INSIGHT_JCEF_DIR")
        );
        if (configured != null) {
            return Path.of(configured).toAbsolutePath().normalize().toFile();
        }

        List<Path> candidates = new ArrayList<>();
        candidates.add(Path.of(System.getProperty("user.dir"), "jcef-bundle").toAbsolutePath().normalize());

        try {
            URI location = ChromiumBrowserView.class.getProtectionDomain().getCodeSource().getLocation().toURI();
            Path codeLocation = Path.of(location).toAbsolutePath().normalize();
            if (Files.isRegularFile(codeLocation)) {
                candidates.add(codeLocation.getParent().resolve("jcef-bundle").normalize());
            } else {
                candidates.add(codeLocation.resolve("jcef-bundle").normalize());
                Path parent = codeLocation.getParent();
                if (parent != null) {
                    candidates.add(parent.resolve("jcef-bundle").normalize());
                    Path grandParent = parent.getParent();
                    if (grandParent != null) {
                        candidates.add(grandParent.resolve("jcef-bundle").normalize());
                    }
                }
            }
        } catch (Exception ignored) {
            // user.dir remains the default installation location.
        }

        return candidates.stream()
                .filter(ChromiumBrowserView::isInstalledBundle)
                .findFirst()
                .orElse(candidates.get(0))
                .toFile();
    }

    private static boolean isJbrJcefRuntime() {
        try {
            Object source = CefApp.class.getProtectionDomain().getCodeSource();
            return source != null && source.toString().contains("jrt:/jcef");
        } catch (SecurityException ignored) {
            return false;
        }
    }

    private static File resolveJbrJcefDir() {
        Path jbrBin = Path.of(System.getProperty("java.home"), "bin").toAbsolutePath().normalize();
        return jbrBin.toFile();
    }

    private static boolean isInstalledBundle(File installDir) {
        return installDir != null && isInstalledBundle(installDir.toPath());
    }

    private static boolean isInstalledBundle(Path installDir) {
        if (installDir == null) {
            return false;
        }
        if (isJbrJcefRuntime()) {
            return Files.isRegularFile(installDir.resolve("jcef.dll"))
                    && Files.isRegularFile(installDir.resolve("libcef.dll"))
                    && Files.isRegularFile(installDir.resolve("jcef_helper.exe"))
                    && Files.isDirectory(installDir.resolve("locales"));
        }
        return Files.isRegularFile(installDir.resolve("install.lock"))
                && Files.isRegularFile(installDir.resolve("build_meta.json"))
                && Files.isRegularFile(installDir.resolve("jcef.dll"))
                && Files.isRegularFile(installDir.resolve("libcef.dll"));
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    private static boolean isLinux() {
        return System.getProperty("os.name", "").toLowerCase().contains("nux");
    }

    private static boolean isMac() {
        String os = System.getProperty("os.name", "").toLowerCase();
        return os.contains("mac") || os.contains("darwin");
    }

    private static String firstNonBlank(String first, String second) {
        String cleanFirst = clean(first);
        return cleanFirst == null ? clean(second) : cleanFirst;
    }

    private static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private static String urlDecode(String value) {
        return URLDecoder.decode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private static String htmlEscape(String value) {
        return (value == null ? "" : value)
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private static String jsString(String value) {
        StringBuilder builder = new StringBuilder("\"");
        String safeValue = value == null ? "" : value;
        for (int index = 0; index < safeValue.length(); index++) {
            char ch = safeValue.charAt(index);
            switch (ch) {
                case '\\' -> builder.append("\\\\");
                case '"' -> builder.append("\\\"");
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                case '\t' -> builder.append("\\t");
                case '<' -> builder.append("\\u003C");
                case '>' -> builder.append("\\u003E");
                case '&' -> builder.append("\\u0026");
                default -> builder.append(ch);
            }
        }
        return builder.append('"').toString();
    }

    private static Throwable unwrap(Throwable throwable) {
        Throwable current = throwable;
        while ((current instanceof CompletionException || current instanceof IllegalStateException)
                && current.getCause() != null) {
            current = current.getCause();
        }
        return current == null ? throwable : current;
    }

    @FunctionalInterface
    private interface CefAppFactory {
        CefApp create() throws ReflectiveOperationException;
    }

    private record JbrCefConfig(
            String[] args,
            CefSettings settings,
            SystemBootstrap.Loader loader,
            File serverExe
    ) {
    }

    private static final class LocalPlayerServer {
        private static final String LOOPBACK_HOST = "127.0.0.1";

        private final HttpServer server;
        private final String origin;

        private LocalPlayerServer() throws IOException {
            server = HttpServer.create(new InetSocketAddress(InetAddress.getByName(LOOPBACK_HOST), 0), 0);
            server.createContext("/youtube-player", this::handleYoutubePlayer);
            server.createContext("/health", this::handleHealth);
            server.setExecutor(Executors.newCachedThreadPool(runnable -> {
                Thread thread = new Thread(runnable, "sport-insight-player-server");
                thread.setDaemon(true);
                return thread;
            }));
            server.start();
            origin = "http://" + LOOPBACK_HOST + ":" + server.getAddress().getPort();
        }

        private String playerUrl(String videoId, String watchUrl) {
            String cleanVideoId = clean(videoId);
            String cleanWatchUrl = clean(watchUrl);
            if (cleanWatchUrl == null) {
                cleanWatchUrl = "https://www.youtube.com/watch?v=" + urlEncode(cleanVideoId);
            }
            if (!cleanWatchUrl.contains("autoplay=")) {
                cleanWatchUrl += (cleanWatchUrl.contains("?") ? "&" : "?") + "autoplay=1";
            }
            return origin
                    + "/youtube-player?videoId=" + urlEncode(cleanVideoId)
                    + "&watchUrl=" + urlEncode(cleanWatchUrl);
        }

        private void handleHealth(HttpExchange exchange) throws IOException {
            sendResponse(exchange, 200, "ok", "text/plain; charset=UTF-8");
        }

        private void handleYoutubePlayer(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Method not allowed", "text/plain; charset=UTF-8");
                return;
            }

            Map<String, String> query = parseQuery(exchange.getRequestURI().getRawQuery());
            String videoId = clean(query.get("videoId"));
            if (videoId == null) {
                sendResponse(exchange, 400, "Missing videoId", "text/plain; charset=UTF-8");
                return;
            }

            String watchUrl = clean(query.get("watchUrl"));
            if (watchUrl == null) {
                watchUrl = "https://www.youtube.com/watch?v=" + urlEncode(videoId) + "&autoplay=1";
            }

            sendResponse(exchange, 200, buildYoutubePlayerHtml(videoId, watchUrl), "text/html; charset=UTF-8");
        }

        private Map<String, String> parseQuery(String rawQuery) {
            Map<String, String> values = new LinkedHashMap<>();
            if (rawQuery == null || rawQuery.isBlank()) {
                return values;
            }

            for (String pair : rawQuery.split("&")) {
                int separator = pair.indexOf('=');
                String key = separator < 0 ? pair : pair.substring(0, separator);
                String value = separator < 0 ? "" : pair.substring(separator + 1);
                values.put(urlDecode(key), urlDecode(value));
            }
            return values;
        }

        private void sendResponse(HttpExchange exchange, int status, String body, String contentType) throws IOException {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.getResponseHeaders().set("Cache-Control", "no-store, max-age=0");
            exchange.getResponseHeaders().set("Referrer-Policy", "strict-origin-when-cross-origin");
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(bytes);
            }
        }

        private String buildYoutubePlayerHtml(String videoId, String watchUrl) {
            String embedUrl = "https://www.youtube.com/embed/" + urlEncode(videoId)
                    + "?autoplay=1"
                    + "&rel=0"
                    + "&modestbranding=1"
                    + "&playsinline=1"
                    + "&enablejsapi=1"
                    + "&origin=" + urlEncode(origin);

            return """
                    <!doctype html>
                    <html>
                    <head>
                      <meta charset="UTF-8">
                      <meta name="referrer" content="strict-origin-when-cross-origin">
                      <meta name="viewport" content="width=device-width, initial-scale=1.0">
                      <title>Sport Insight Highlights</title>
                      <style>
                        html, body {
                          margin: 0;
                          width: 100%%;
                          height: 100%%;
                          overflow: hidden;
                          background: #000;
                          color: #fff;
                          font-family: "Segoe UI", Arial, sans-serif;
                        }
                        #ytFrame {
                          position: absolute;
                          inset: 0;
                          width: 100%%;
                          height: 100%%;
                          border: 0;
                          background: #000;
                        }
                        #status {
                          position: fixed;
                          left: 0;
                          right: 0;
                          bottom: 0;
                          padding: 10px 14px;
                          background: rgba(5, 9, 21, 0.88);
                          color: #fff;
                          text-align: center;
                          font-weight: 700;
                          z-index: 5;
                        }
                        #fallback {
                          position: fixed;
                          inset: 0;
                          display: none;
                          align-items: center;
                          justify-content: center;
                          background: #050915;
                          text-align: center;
                          z-index: 10;
                        }
                        #fallback button {
                          margin-top: 18px;
                          border: 0;
                          border-radius: 999px;
                          padding: 12px 22px;
                          background: #ffffff;
                          color: #050915;
                          font-weight: 800;
                          cursor: pointer;
                        }
                      </style>
                    </head>
                    <body>
                      <iframe id="ytFrame"
                        src="%s"
                        title="YouTube video player"
                        referrerpolicy="strict-origin-when-cross-origin"
                        allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
                        allowfullscreen></iframe>
                      <div id="status">Loading highlight...</div>
                      <div id="fallback">
                        <div>
                          <h2>Opening YouTube player inside Sport Insight...</h2>
                          <p>This video rejected the embedded iframe, so the app is switching to the full YouTube player in this same window.</p>
                          <button type="button" onclick="switchToWatchPage('manual')">Open in-app YouTube player now</button>
                        </div>
                      </div>
                      <script>
                        const WATCH_URL = %s;
                        let playbackStarted = false;
                        let switched = false;
                        const statusEl = document.getElementById('status');
                        const fallbackEl = document.getElementById('fallback');
                        const fallbackTimer = setTimeout(() => {
                          if (!playbackStarted) {
                            switchToWatchPage('embed timeout');
                          }
                        }, 5000);

                        function switchToWatchPage(reason) {
                          if (switched) {
                            return;
                          }
                          switched = true;
                          fallbackEl.style.display = 'flex';
                          statusEl.textContent = 'Switching to YouTube player inside the app...';
                          setTimeout(() => window.location.replace(WATCH_URL), 350);
                        }

                        function onYouTubeIframeAPIReady() {
                          try {
                            new YT.Player('ytFrame', {
                              events: {
                                onReady: event => {
                                  statusEl.textContent = 'Starting highlight...';
                                  try {
                                    event.target.playVideo();
                                  } catch (ignored) {
                                  }
                                },
                                onStateChange: event => {
                                  if (event.data === YT.PlayerState.PLAYING || event.data === YT.PlayerState.BUFFERING || event.data === YT.PlayerState.CUED) {
                                    playbackStarted = true;
                                    clearTimeout(fallbackTimer);
                                    statusEl.style.display = 'none';
                                  }
                                },
                                onError: event => switchToWatchPage('youtube error ' + event.data)
                              }
                            });
                          } catch (error) {
                            switchToWatchPage('iframe api failed');
                          }
                        }
                      </script>
                      <script src="https://www.youtube.com/iframe_api"></script>
                    </body>
                    </html>
                    """.formatted(
                    htmlEscape(embedUrl),
                    jsString(watchUrl)
            );
        }
    }
}
