package systems.diath.visotaris_opmod.ui;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import com.google.gson.Gson;
import net.fabricmc.loader.api.FabricLoader;
import systems.diath.visotaris_opmod.VisotarisModClient;
import systems.diath.visotaris_opmod.cache.MarketCache;
import systems.diath.visotaris_opmod.cache.AuctionCache;
import systems.diath.visotaris_opmod.cache.ShardCache;
import systems.diath.visotaris_opmod.config.ConfigManager;
import systems.diath.visotaris_opmod.config.PriceAlertRule;
import systems.diath.visotaris_opmod.config.VisotarisConfig;
import systems.diath.visotaris_opmod.model.MarketPrice;
import systems.diath.visotaris_opmod.model.Auction;
import systems.diath.visotaris_opmod.model.MerchantCurrency;
import systems.diath.visotaris_opmod.model.ShardRate;
import systems.diath.visotaris_opmod.services.PriceAlertEngine;
import systems.diath.visotaris_opmod.services.PriceAlertInputValidator;
import systems.diath.visotaris_opmod.services.PriceAlertService;
import systems.diath.visotaris_opmod.util.ItemNameResolver;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/** Shared layout, local-cache view model, controls and keyboard behavior. */
public abstract class IngameUxScreenBase extends Screen {
    private static final Gson CONFIG_JSON = new Gson();
    private static final int[] NAVY = {0xFF102B55, 0xFF193B63};
    private static final int FRAME = 0xFF70B4D4;
    private static final int PANEL = 0xFF10233C;
    private static final int CONTENT = 0xFF142D49;
    private static final int CARD = 0xFF20405C;
    private static final int CARD_DARK = 0xFF102940;
    private static final int LINE = 0xFF416C8C;
    private static final int WHITE = 0xFFF0F5F7;
    private static final int MUTED = 0xFFB9C8D2;
    private static final int ICE = 0xFF69C9EE;
    private static final int INFO = 0xFF8CD9FA;
    private static final int GREEN = 0xFF9DDB76;
    private static final int YELLOW = 0xFFE8C878;
    private static final int OFF = 0xFF86939A;

    private static final String[] PAGE_TITLES = {
        "Übersicht", "Markt", "Shard & Händler", "Tooltips", "Preisalarme", "Schutz & Komfort", "System", "Auktionshaus"
    };
    private static final String[] PAGE_SHORT = {
        "Übersicht", "Markt", "Shard/Händler", "Tooltips", "Preisalarme", "Schutz/Komfort", "System", "Auktionshaus"
    };

    protected record Window(int x, int y, int width, int height, int navWidth, int bodyTop, int bodyBottom,
                            int contentX, int contentWidth, int contentBottom) { }

    private static final class Control {
        final Button widget;
        final Supplier<String> label;
        final BooleanSupplier selected;
        final Supplier<String> hoverText;
        final int x, y, width, height;
        final boolean accent;
        final boolean fixed;

        Control(Button widget, Supplier<String> label, BooleanSupplier selected, Supplier<String> hoverText,
                int x, int y, int width, int height, boolean accent, boolean fixed) {
            this.widget = widget;
            this.label = label;
            this.selected = selected;
            this.hoverText = hoverText;
            this.x = x; this.y = y; this.width = width; this.height = height;
            this.accent = accent; this.fixed = fixed;
        }
    }

    private final Screen parent;
    protected final VisotarisModClient mod;
    protected final ConfigManager configManager;
    protected VisotarisConfig cfg;
    private VisotarisConfig savedConfig;
    protected final MarketCache marketCache;
    protected final ShardCache shardCache;
    protected final AuctionCache auctionCache;
    private final List<Control> controls = new ArrayList<>();
    private final Map<String, String> names = new HashMap<>();
    private final List<String> categories = new ArrayList<>();
    private final List<MarketPrice> marketRows = new ArrayList<>();
    private final List<ShardRate> shardRows = new ArrayList<>();
    private final List<Auction> auctionRows = new ArrayList<>();
    private final List<Button> navigation = new ArrayList<>();
    private final ArrayDeque<Integer> pageHistory = new ArrayDeque<>();
    private Button footerBackButton;
    private Button footerSaveButton;
    private final List<Button> modalButtons = new ArrayList<>();
    private boolean showUnsavedDialog;
    private boolean changingScreen;
    private boolean alertEditorDirty;
    private Runnable pendingBackAction;

    private Window window;
    private int activePage;
    private int pageScroll;
    private int pageScrollTarget;
    private int pageMaxScroll;
    private long lastScrollFrameMs;
    private int marketScroll;
    private int shardScroll;
    private int auctionScroll;
    private int alertScroll;
    private double marketScrollPosition;
    private double marketScrollTarget;
    private double shardScrollPosition;
    private double shardScrollTarget;
    private double auctionScrollPosition;
    private double auctionScrollTarget;
    private double alertScrollPosition;
    private double alertScrollTarget;
    private int selectedMarketCategory;
    private int selectedShardTarget;
    private int marketSort;
    private boolean marketSortDescending;
    private static final String[] MARKET_SORTS = {
        "Itemname", "Kaufpreis", "Verkaufspreis", "Kaufaufträge", "Verkaufsaufträge", "Aufträge gesamt"
    };
    private EditBox marketSearch;
    private EditBox shardSearch;
    private EditBox auctionSearch;
    private EditBox alertItemSearch;
    private EditBox alertThresholdInput;
    private EditBox alertCooldownInput;
    private PriceAlertRule alertDraft;
    private String alertEditingId;
    private String alertDeleteId;
    private String alertItemQuery = "";
    private String alertThreshold = "";
    private String alertCooldown = "300";
    private int alertItemIndex;
    private String marketQuery = "";
    private String shardQuery = "";
    private String auctionQuery = "";
    private String auctionCategory = "";
    private int auctionSort;
    private boolean auctionSortDescending;
    private int shardSort;
    private boolean shardSortDescending;
    private boolean systemActionsView;
    private static final String[] SHARD_SORTS = {"Material", "Kurs", "Basiskurs", "Abweichung"};
    private long lastMarketUpdate;
    private long lastShardUpdate;
    private long lastAuctionUpdate;
    private long lastAlertEvent;
    private String transientStatus = "";
    private long transientStatusAt;
    private int hoveredRow = -1;
    private String hoveredDetails = "";
    private int visibleMarketRows = 4;
    private int visibleShardRows = 5;
    private int visibleAuctionRows = 4;
    private int visibleAlertRows = 4;
    private int rowHeight = 39;
    private int listTop;
    private int controlListTop;
    private int contentEnd;
    private int alertItemSearchBaseY;
    private int alertThresholdBaseY;
    private int alertCooldownBaseY;
    private int shardSearchBaseY;
    private int auctionSearchBaseY;

    protected IngameUxScreenBase(Screen parent) {
        super(Component.literal("Visotaris"));
        this.parent = parent;
        this.mod = VisotarisModClient.getInstance();
        this.configManager = mod.getConfigManager();
        this.cfg = new VisotarisConfig(configManager.getConfig());
        this.savedConfig = new VisotarisConfig(cfg);
        this.marketCache = mod.getMarketCache();
        this.shardCache = mod.getShardCache();
        this.auctionCache = mod.getAuctionCache();
        this.lastMarketUpdate = marketCache.getLastUpdatedMs();
        this.lastShardUpdate = shardCache.getLastUpdatedMs();
        this.lastAuctionUpdate = auctionCache.getLastUpdatedMs();
        this.lastAlertEvent = latestAlertTimestamp();
        refreshMarketRows();
        refreshShardRows();
        refreshAuctionRows();
    }

    protected abstract void showScreen(Screen screen);

    @Override
    protected void init() {
        controls.clear();
        navigation.clear();
        modalButtons.clear();
        shardSearch = null;
        auctionSearch = null;
        alertItemSearch = null;
        alertThresholdInput = null;
        alertCooldownInput = null;
        window = layout(width, height);
        pageMaxScroll = 0;
        contentEnd = window.bodyTop() + 12;
        controlListTop = window.bodyTop() + 12;
        int navHeight = Math.max(12, Math.min(30, (window.bodyBottom() - window.bodyTop() - 20) / PAGE_TITLES.length));
        int navGap = Math.max(1, Math.min(4, (window.bodyBottom() - window.bodyTop() - PAGE_TITLES.length * navHeight) / (PAGE_TITLES.length + 1)));
        int navX = window.x() + 10;
        int navW = window.navWidth() - 20;
        for (int i = 0; i < PAGE_TITLES.length; i++) {
            final int page = i;
            int y = window.bodyTop() + 8 + i * (navHeight + navGap);
            Button button = addInvisibleButton(PAGE_SHORT[i], navX, y, navW, navHeight,
                () -> selectPage(page));
            navigation.add(button);
        }
        if (getFocused() == null && !navigation.isEmpty()) navigation.get(activePage).setFocused(true);

        int x = window.contentX() + 14;
        int w = window.contentWidth() - 28;
        int y = window.bodyTop() + 36;
        switch (activePage) {
            case 0 -> buildOverview(x, y, w);
            case 1 -> buildMarket(x, y, w);
            case 2 -> buildShards(x, y, w);
            case 3 -> buildTooltips(x, y, w);
            case 4 -> buildAlerts(x, y, w);
            case 5 -> buildProtection(x, y, w);
            case 6 -> buildSystem(x, y, w);
            case 7 -> buildAuctions(x, y, w);
            default -> { }
        }
        contentEnd = Math.max(contentEnd, y);
        pageMaxScroll = Math.max(0, contentEnd - scrollViewportBottom() + 8);
        if (pageScroll > pageMaxScroll) pageScroll = pageMaxScroll;
        if (pageScrollTarget > pageMaxScroll) pageScrollTarget = pageMaxScroll;
        updateControlVisibility();
        initFooterButtons();
        if (showUnsavedDialog) initUnsavedDialogButtons();
    }

    private void initFooterButtons() {
        int footerTop = window.bodyBottom() + 7;
        int footerBottom = window.y() + window.height() - 4;
        int h = Math.max(14, Math.min(20, footerBottom - footerTop));
        int backW = Math.max(48, font.width("Zurück") + 14);
        int saveW = Math.max(58, font.width("Speichern") + 14);
        int gap = 5;
        int y = Math.min(footerBottom - h, footerTop);
        int saveX = window.x() + window.width() - 12 - saveW;
        int backX = saveX - gap - backW;
        footerBackButton = addInvisibleButton("Zurück", backX, y, backW, h, this::requestBack);
        footerSaveButton = addInvisibleButton("Speichern", saveX, y, saveW, h, this::saveChanges);
        footerBackButton.visible = !showUnsavedDialog;
        footerBackButton.active = !showUnsavedDialog;
        footerSaveButton.visible = !showUnsavedDialog;
        footerSaveButton.active = !showUnsavedDialog && hasUnsavedChanges();
    }

    private void initUnsavedDialogButtons() {
        int modalW = Math.min(330, window.width() - 28);
        int buttonW = modalW - 20;
        int x = window.x() + (window.width() - buttonW) / 2;
        int y = window.y() + window.height() / 2 + 13;
        int h = 20;
        modalButtons.add(addInvisibleButton("Speichern", x, y, buttonW, h, () -> resolveUnsavedDialog(true)));
        modalButtons.add(addInvisibleButton("Änderungen verwerfen", x, y + 22, buttonW, h, () -> resolveUnsavedDialog(false)));
        modalButtons.add(addInvisibleButton("Weiter bearbeiten", x, y + 44, buttonW, h, this::cancelUnsavedDialog));
        modalButtons.getFirst().setFocused(true);
        for (Control control : controls) { control.widget.visible = false; control.widget.active = false; }
        for (Button button : navigation) { button.visible = false; button.active = false; }
        if (footerBackButton != null) { footerBackButton.visible = false; footerBackButton.active = false; }
        if (footerSaveButton != null) { footerSaveButton.visible = false; footerSaveButton.active = false; }
        disableInput(marketSearch); disableInput(shardSearch); disableInput(alertItemSearch);
        disableInput(alertThresholdInput); disableInput(alertCooldownInput);
    }

    private static void disableInput(EditBox input) {
        if (input != null) { input.visible = false; input.active = false; }
    }

    private Window layout(int screenWidth, int screenHeight) {
        int fw = Math.max(260, Math.min(1120, screenWidth - 24));
        int fh = Math.max(148, Math.min(760, screenHeight - 22));
        fw = Math.min(fw, screenWidth - 8);
        fh = Math.min(fh, screenHeight - 8);
        int x = Math.max(4, (screenWidth - fw) / 2);
        int y = Math.max(4, (screenHeight - fh) / 2);
        int header = Math.min(36, Math.max(23, fh / 13));
        int footer = Math.min(32, Math.max(18, fh / 18));
        int nav = Math.min(196, Math.max(82, fw / 5));
        int bodyTop = y + header + 7;
        int bodyBottom = y + fh - footer - 7;
        int contentX = x + nav + 18;
        return new Window(x, y, fw, fh, nav, bodyTop, bodyBottom,
            contentX, Math.max(140, x + fw - 10 - contentX), bodyBottom - 4);
    }

    private Button addInvisibleButton(String accessibleLabel, int x, int y, int w, int h, Runnable action) {
        Button button = Button.builder(Component.literal(accessibleLabel), b -> action.run())
            .bounds(x, y, Math.max(1, w), Math.max(1, h)).build();
        button.setAlpha(0f);
        addRenderableWidget(button);
        return button;
    }

    private void addControl(int x, int rawY, int w, int h, Supplier<String> label,
                            BooleanSupplier selected, Supplier<String> hoverText,
                            boolean accent, Runnable action) {
        addControl(x, rawY, w, h, label, selected, hoverText, accent, false, action);
    }

    private void addFixedControl(int x, int y, int w, int h, Supplier<String> label,
                                 BooleanSupplier selected, Supplier<String> hoverText,
                                 boolean accent, Runnable action) {
        addControl(x, y, w, h, label, selected, hoverText, accent, true, action);
    }

    private void addControl(int x, int rawY, int w, int h, Supplier<String> label,
                            BooleanSupplier selected, Supplier<String> hoverText,
                            boolean accent, boolean fixed, Runnable action) {
        int y = rawY - (fixed ? 0 : pageScroll);
        Button button = addInvisibleButton(label.get(), x, y, w, h, action);
        controls.add(new Control(button, label, selected, hoverText, x, rawY, w, h, accent, fixed));
        if (!fixed) contentEnd = Math.max(contentEnd, rawY + h);
    }

    private void addToggle(int x, int y, int w, String label, BooleanSupplier getter,
                           java.util.function.Consumer<Boolean> setter, String explanation,
                           boolean unavailableInObserver) {
        addControl(x, y, w, 23, () -> label + "  ·  " + (getter.getAsBoolean() ? "AN" : "AUS"), getter,
            () -> explanation, false, () -> {
                if (unavailableInObserver && cfg.observerModeOnly) {
                    updateConfig(() -> setter.accept(!getter.getAsBoolean()));
                    setStatus(label + " vorgemerkt; im Observer-Modus derzeit inaktiv.");
                } else {
                    updateConfig(() -> setter.accept(!getter.getAsBoolean()));
                }
            });
    }

    private void updateConfig(Runnable update) {
        update.run();
        setStatus("Änderung vorgemerkt · zum Übernehmen Speichern drücken.");
        refreshWidgets();
    }

    private boolean hasUnsavedChanges() {
        return alertEditorDirty || !CONFIG_JSON.toJson(cfg).equals(CONFIG_JSON.toJson(savedConfig));
    }

    private void saveChanges() {
        if (alertDraft != null && !stageAlertDraft()) return;
        if (!hasUnsavedChanges()) { setStatus("Keine ungespeicherten Änderungen."); return; }
        VisotarisConfig candidate = cfg;
        mergeAlertRuntimeState(candidate, configManager.getConfig());
        boolean reconfigureNetwork = networkConfigChanged(savedConfig, candidate);
        if (!configManager.saveCandidate(candidate)) {
            setStatus("Speichern fehlgeschlagen · Änderungen bleiben vorgemerkt.");
            refreshWidgets();
            return;
        }
        cfg = candidate;
        savedConfig = new VisotarisConfig(candidate);
        alertEditorDirty = false;
        if (reconfigureNetwork) mod.applyWebUiConfig(false);
        setStatus("Änderungen erfolgreich gespeichert.");
        refreshWidgets();
    }

    public final boolean saveSubscreenDraft(VisotarisConfig candidate) {
        if (alertDraft != null && !stageAlertDraft()) return false;
        mergeAlertRuntimeState(candidate, configManager.getConfig());
        boolean reconfigureNetwork = networkConfigChanged(savedConfig, candidate);
        if (!configManager.saveCandidate(candidate)) return false;
        cfg = candidate;
        savedConfig = new VisotarisConfig(candidate);
        alertEditorDirty = false;
        if (reconfigureNetwork) mod.applyWebUiConfig(false);
        setStatus("Änderungen erfolgreich gespeichert.");
        return true;
    }

    private static void mergeAlertRuntimeState(VisotarisConfig candidate, VisotarisConfig current) {
        for (PriceAlertRule staged : candidate.priceAlertRules) {
            PriceAlertRule live = current.priceAlertRules.stream()
                .filter(rule -> Objects.equals(rule.id, staged.id)).findFirst().orElse(null);
            if (live == null || !Objects.equals(live.itemKey, staged.itemKey)
                || !Objects.equals(live.condition, staged.condition) || live.threshold != staged.threshold
                || live.repeat != staged.repeat || live.cooldownSeconds != staged.cooldownSeconds
                || live.rearmOnExit != staged.rearmOnExit
                || !Objects.equals(live.notificationChannel(), staged.notificationChannel())) continue;
            staged.lastTriggeredAtMs = live.lastTriggeredAtMs;
            staged.triggered = live.triggered;
            staged.latched = live.latched;
        }
    }

    private static boolean networkConfigChanged(VisotarisConfig before, VisotarisConfig after) {
        return before.marketRefreshIntervalSeconds != after.marketRefreshIntervalSeconds
            || before.merchantRefreshIntervalSeconds != after.merchantRefreshIntervalSeconds
            || before.enableWebUi != after.enableWebUi || before.webUiPort != after.webUiPort
            || before.proxyPort != after.proxyPort || !Objects.equals(before.proxyType, after.proxyType)
            || !Objects.equals(before.proxyHost, after.proxyHost)
            || !Objects.equals(before.customUserAgent, after.customUserAgent);
    }

    private void requestBack() {
        if (hasUnsavedChanges()) {
            pendingBackAction = activePage == 4 && alertDraft != null ? this::returnToAlerts : this::navigateBackNow;
            showUnsavedDialog = true;
            rebuildWidgets();
        } else navigateBackNow();
    }

    private void returnToAlerts() {
        alertDraft = null;
        alertEditorDirty = false;
        pageScroll = pageScrollTarget = 0;
        rebuildWidgets();
    }

    private void navigateBackNow() {
        showUnsavedDialog = false;
        if (alertDraft != null) {
            alertDraft = null;
            alertEditorDirty = false;
            pageScroll = pageScrollTarget = 0;
            rebuildWidgets();
            return;
        }
        if (!pageHistory.isEmpty()) {
            activePage = pageHistory.removeLast();
            systemActionsView = false;
            pageScroll = pageScrollTarget = 0;
            rebuildWidgets();
        } else if (activePage != 0) {
            activePage = 0;
            systemActionsView = false;
            pageScroll = pageScrollTarget = 0;
            rebuildWidgets();
        } else {
            changingScreen = true;
            showScreen(parent);
        }
    }

    private void resolveUnsavedDialog(boolean save) {
        Runnable action = pendingBackAction;
        pendingBackAction = null;
        if (save) {
            showUnsavedDialog = false;
            saveChanges();
            if (hasUnsavedChanges()) return;
        } else {
            cfg = new VisotarisConfig(savedConfig);
            alertDraft = null;
            alertEditorDirty = false;
            showUnsavedDialog = false;
        }
        if (action != null) action.run();
        else rebuildWidgets();
    }

    private void cancelUnsavedDialog() {
        showUnsavedDialog = false;
        pendingBackAction = null;
        rebuildWidgets();
    }

    public final void returnFromSubscreen() {
        changingScreen = true;
        showScreen(this);
        changingScreen = false;
        if (hasUnsavedChanges()) {
            pendingBackAction = null;
            showUnsavedDialog = true;
            rebuildWidgets();
        }
    }

    private void openSubscreen(Screen screen) {
        changingScreen = true;
        showScreen(screen);
        changingScreen = false;
    }

    private void refreshWidgets() {
        rebuildWidgets();
    }

    private void selectPage(int page) {
        if (page < 0 || page >= PAGE_TITLES.length || page == activePage) return;
        pageHistory.addLast(activePage);
        while (pageHistory.size() > 16) pageHistory.removeFirst();
        activePage = page;
        systemActionsView = false;
        pageScroll = 0;
        pageScrollTarget = 0;
        marketScroll = 0;
        marketScrollPosition = marketScrollTarget = 0;
        shardScroll = 0;
        shardScrollPosition = shardScrollTarget = 0;
        auctionScroll = 0;
        auctionScrollPosition = auctionScrollTarget = 0;
        alertScroll = 0;
        alertScrollPosition = alertScrollTarget = 0;
        alertDeleteId = null;
        rebuildWidgets();
    }

    private void updateControlVisibility() {
        int contentTop = interactiveContentTop();
        int contentBottom = scrollViewportBottom();
        for (Control control : controls) {
            int y = control.fixed ? control.y : control.y - pageScroll;
            if (activePage == 4 && alertDraft == null && y >= listTop)
                y -= (int) Math.round((alertScrollPosition - alertScroll) * 43);
            control.widget.setY(y);
            int lowerBound = control.fixed ? window.bodyBottom() : contentBottom;
            control.widget.visible = y >= contentTop && y + control.height <= lowerBound;
            control.widget.active = control.widget.visible;
        }
        if (alertItemSearch != null) setInputPosition(alertItemSearch, alertItemSearchBaseY, contentTop, contentBottom);
        if (alertThresholdInput != null) setInputPosition(alertThresholdInput, alertThresholdBaseY, contentTop, contentBottom);
        if (alertCooldownInput != null) setInputPosition(alertCooldownInput, alertCooldownBaseY, contentTop, contentBottom);
        if (shardSearch != null) setInputPosition(shardSearch, shardSearchBaseY, contentTop, contentBottom);
        if (auctionSearch != null) setInputPosition(auctionSearch, auctionSearchBaseY, contentTop, contentBottom);
    }

    private void setInputPosition(EditBox input, int baseY, int contentTop, int contentBottom) {
        int y = baseY - pageScroll;
        input.setY(y);
        input.visible = y >= contentTop && y + input.getHeight() <= contentBottom;
        input.active = input.visible;
    }

    private int scrollViewportBottom() {
        if (activePage == 4 && alertDraft != null)
            return Math.max(interactiveContentTop() + 1, Math.min(window.contentBottom(), window.bodyBottom() - 4));
        return window.contentBottom();
    }

    private int interactiveContentTop() {
        if (activePage == 4 && alertDraft != null)
            return Math.min(window.bodyTop() + 61, window.bodyBottom() - 29);
        if (activePage == 3 || activePage == 5 || activePage == 6 && systemActionsView) return window.bodyTop() + 82;
        return window.bodyTop() + 32;
    }

    @Override
    public void tick() {
        super.tick();
        long marketUpdated = marketCache.getLastUpdatedMs();
        long shardUpdated = shardCache.getLastUpdatedMs();
        long auctionUpdated = auctionCache.getLastUpdatedMs();
        long eventUpdated = latestAlertTimestamp();
        if (marketUpdated != lastMarketUpdate || shardUpdated != lastShardUpdate || auctionUpdated != lastAuctionUpdate || eventUpdated != lastAlertEvent) {
            lastMarketUpdate = marketUpdated;
            lastShardUpdate = shardUpdated;
            lastAuctionUpdate = auctionUpdated;
            lastAlertEvent = eventUpdated;
            refreshMarketRows();
            refreshShardRows();
            refreshAuctionRows();
            rebuildWidgets();
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (showUnsavedDialog) {
            if (event.key() == GLFW.GLFW_KEY_ESCAPE) { cancelUnsavedDialog(); return true; }
            if (event.key() == GLFW.GLFW_KEY_S) { resolveUnsavedDialog(true); return true; }
            if (event.key() == GLFW.GLFW_KEY_D) { resolveUnsavedDialog(false); return true; }
        }
        if (event.key() == GLFW.GLFW_KEY_ESCAPE && activePage == 4 && alertDraft != null) {
            requestBack();
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) { requestBack(); return true; }
        if ((event.key() == GLFW.GLFW_KEY_UP || event.key() == GLFW.GLFW_KEY_DOWN)
            && getFocused() != null && navigation.contains(getFocused())) {
            int direction = event.key() == GLFW.GLFW_KEY_DOWN ? 1 : -1;
            int index = navigation.indexOf(getFocused());
            navigation.get(Math.floorMod(index + direction, navigation.size())).setFocused(true);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (window != null && mouseX >= window.contentX() && mouseX < window.contentX() + window.contentWidth()
            && mouseY >= interactiveContentTop() && mouseY < scrollViewportBottom()) {
            if (activePage == 1) {
                marketScrollTarget = clamp(marketScrollTarget - verticalAmount * 3, 0,
                    Math.max(0, marketRows.size() - visibleMarketRows));
                return true;
            }
            if (activePage == 2) {
                shardScrollTarget = clamp(shardScrollTarget - verticalAmount * 3, 0,
                    Math.max(0, shardRows.size() - visibleShardRows));
                return true;
            }
            if (activePage == 7) {
                auctionScrollTarget = clamp(auctionScrollTarget - verticalAmount * 3, 0,
                    Math.max(0, auctionRows.size() - visibleAuctionRows));
                return true;
            }
            if (activePage == 4) {
                if (alertDraft != null) {
                    pageScrollTarget = clamp(pageScrollTarget - (int) Math.round(verticalAmount * 36), 0, pageMaxScroll);
                    return true;
                }
                alertScrollTarget = clamp(alertScrollTarget - verticalAmount * 2, 0,
                    Math.max(0, cfg.priceAlertRules.size() - visibleAlertRows));
                return true;
            }
            if (pageMaxScroll > 0) {
                pageScrollTarget = clamp(pageScrollTarget - (int) Math.round(verticalAmount * 36), 0, pageMaxScroll);
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void onClose() {
        if (!changingScreen) requestBack();
    }

    protected final void renderUx(IngameUxCanvas canvas, int mouseX, int mouseY, float delta) {
        if (window == null) return;
        advanceScrollAnimation();
        hoveredRow = -1;
        hoveredDetails = "";
        drawWindow(canvas);
        canvas.enableScissor(window.contentX(), window.bodyTop() + 6,
            window.contentX() + window.contentWidth(), scrollViewportBottom());
        drawPage(canvas, mouseX, mouseY);
        canvas.disableScissor();
        canvas.enableScissor(window.contentX(), interactiveContentTop(),
            window.contentX() + window.contentWidth(), window.bodyBottom());
        drawControls(canvas);
        canvas.disableScissor();
        drawFooter(canvas);
        if (showUnsavedDialog) {
            drawUnsavedDialog(canvas);
            return;
        }
        if (hoveredRow >= 0 && !hoveredDetails.isBlank()) {
            drawTooltip(canvas, hoveredDetails, mouseX, mouseY);
        } else {
            for (Control control : controls) {
                if (control.widget.visible && control.widget.isHovered()) {
                    String detail = control.hoverText.get();
                    if (detail != null && !detail.isBlank()) drawTooltip(canvas, detail, mouseX, mouseY);
                    break;
                }
            }
            if (hoveredRow < 0) {
                for (int i = 0; i < navigation.size(); i++) {
                    Button nav = navigation.get(i);
                    if (nav.visible && nav.isHovered() && nav.getWidth() < canvas.textWidth(PAGE_TITLES[i]) + 14) {
                        drawTooltip(canvas, PAGE_TITLES[i], mouseX, mouseY);
                        break;
                    }
                }
            }
        }
    }

    private void advanceScrollAnimation() {
        long now = System.currentTimeMillis();
        if (lastScrollFrameMs == 0) lastScrollFrameMs = now;
        long elapsed = Math.max(0, Math.min(80, now - lastScrollFrameMs));
        lastScrollFrameMs = now;
        if (elapsed == 0) return;
        double amount = 1.0 - Math.exp(-elapsed / 75.0);
        if (pageScroll != pageScrollTarget) {
            int next = (int) Math.round(pageScroll + (pageScrollTarget - pageScroll) * amount);
            if (next == pageScroll) next += Integer.signum(pageScrollTarget - pageScroll);
            pageScroll = clamp(next, 0, pageMaxScroll);
            updateControlVisibility();
        }
        marketScrollPosition = approach(marketScrollPosition, marketScrollTarget, amount);
        shardScrollPosition = approach(shardScrollPosition, shardScrollTarget, amount);
        auctionScrollPosition = approach(auctionScrollPosition, auctionScrollTarget, amount);
        double priorAlertPosition = alertScrollPosition;
        alertScrollPosition = approach(alertScrollPosition, alertScrollTarget, amount);
        int nextAlertScroll = (int) Math.floor(alertScrollPosition);
        if (nextAlertScroll != alertScroll) {
            alertScroll = nextAlertScroll;
            rebuildWidgets();
        } else if (priorAlertPosition != alertScrollPosition && activePage == 4 && alertDraft == null) {
            updateControlVisibility();
        }
        marketScroll = (int) Math.floor(marketScrollPosition);
        shardScroll = (int) Math.floor(shardScrollPosition);
        auctionScroll = (int) Math.floor(auctionScrollPosition);
    }

    private static double approach(double current, double target, double amount) {
        if (Math.abs(target - current) < 0.01) return target;
        return current + (target - current) * amount;
    }

    private void drawWindow(IngameUxCanvas c) {
        Window a = window;
        c.fill(0, 0, width, height, 0xB8091119);
        c.fill(a.x() - 3, a.y() - 3, a.x() + a.width() + 3, a.y() + a.height() + 3, 0xFF10161D);
        c.fill(a.x(), a.y(), a.x() + a.width(), a.y() + a.height(), PANEL);
        c.fill(a.x(), a.y(), a.x() + a.width(), a.y() + 2, FRAME);
        c.fill(a.x(), a.y() + a.height() - 2, a.x() + a.width(), a.y() + a.height(), FRAME);
        c.fill(a.x(), a.y(), a.x() + 2, a.y() + a.height(), FRAME);
        c.fill(a.x() + a.width() - 2, a.y(), a.x() + a.width(), a.y() + a.height(), FRAME);
        int header = a.bodyTop() - a.y() - 7;
        c.fill(a.x() + 4, a.y() + 4, a.x() + a.width() - 4, a.y() + header, NAVY[0]);
        c.fill(a.x() + 4, a.y() + header - 2, a.x() + a.width() - 4, a.y() + header, ICE);
        int titleY = a.y() + Math.max(6, (header - c.lineHeight()) / 2);
        c.text("VISOTARIS  ·  " + PAGE_TITLES[activePage], a.x() + 13, titleY, WHITE, true);
        String close = "ESC  Schließen";
        c.text(close, a.x() + a.width() - c.textWidth(close) - 14, titleY, MUTED, false);

        int navX = a.x() + 8;
        int navTop = a.bodyTop();
        c.fill(navX, navTop, navX + a.navWidth() - 7, a.bodyBottom(), PANEL);
        c.fill(navX, navTop, navX + a.navWidth() - 7, navTop + 1, LINE);
        c.fill(a.contentX() - 8, navTop, a.x() + a.width() - 8, a.bodyBottom(), CONTENT);
        c.fill(a.contentX() - 8, navTop, a.x() + a.width() - 8, navTop + 1, LINE);
        c.fill(a.contentX() - 8, navTop, a.contentX() - 7, a.bodyBottom(), LINE);
        c.fill(a.contentX() - 8, a.bodyBottom() - 1, a.x() + a.width() - 8, a.bodyBottom(), LINE);

        c.fill(a.x() + 5, a.y() + a.height() - 5 - Math.max(20, a.height() / 13),
            a.x() + a.width() - 5, a.y() + a.height() - 5, CARD_DARK);
        for (int i = 0; i < navigation.size(); i++) {
            Button nav = navigation.get(i);
            if (!nav.visible) continue;
            int color = i == activePage ? ICE : (nav.isHovered() ? 0xFF3D4C5B : CARD);
            c.fill(nav.getX(), nav.getY(), nav.getX() + nav.getWidth(), nav.getY() + nav.getHeight(), color);
            c.fill(nav.getX(), nav.getY(), nav.getX() + 2, nav.getY() + nav.getHeight(), i == activePage ? INFO : LINE);
            int textColor = i == activePage ? NAVY[0] : WHITE;
            String label = fit(c, PAGE_SHORT[i], nav.getWidth() - 14);
            int ty = nav.getY() + Math.max(1, (nav.getHeight() - c.lineHeight()) / 2);
            c.text(label, nav.getX() + 7, ty, textColor, false);
            if (nav.isFocused()) outline(c, nav.getX(), nav.getY(), nav.getWidth(), nav.getHeight(), INFO);
        }
        // Footer text belongs to drawFooter: one measured layout for all pages.
    }

    private void drawPage(IngameUxCanvas c, int mouseX, int mouseY) {
        int x = window.contentX() + 8;
        int y = window.bodyTop() + 9;
        int w = window.contentWidth() - 16;
        drawSectionTitle(c, PAGE_TITLES[activePage], x, y);
        switch (activePage) {
            case 0 -> drawOverview(c, x, y + 23, w);
            case 1 -> drawMarket(c, x, y + 24, w, mouseX, mouseY);
            case 2 -> drawShards(c, x, y + 24, w, mouseX, mouseY);
            case 3 -> drawTooltipSummary(c, x, y + 25, w);
            case 4 -> {
                if (alertDraft != null) drawAlertEditor(c, x, y + 24, w);
                else drawAlerts(c, x, y + 24, w, mouseX, mouseY);
            }
            case 5 -> drawProtectionSummary(c, x, y + 25, w);
            case 6 -> {
                if (systemActionsView) drawInfoBox(c, x, y + 24, w, 42,
                    "Nur die ausdrücklich gewählte Aktualisierung fragt öffentliche API-Daten ab. Andere Aktionen öffnen lokale Einstellungen.", INFO);
                else drawSystem(c, x, y + 24, w);
            }
            case 7 -> drawAuctions(c, x, y + 24, w, mouseX, mouseY);
            default -> { }
        }
    }

    private void drawOverview(IngameUxCanvas c, int x, int y, int w) {
        int gap = 8;
        int cardW = Math.max(64, (w - gap * 2) / 3);
        drawStatusCard(c, x, y, cardW, "MARKT-CACHE", marketCache.snapshot().size() + " Items",
            cacheState(marketCache.getLastUpdatedMs(), marketCache.getAgeSeconds(), cfg.marketRefreshIntervalSeconds),
            marketCache.isEmpty() ? YELLOW : INFO);
        drawStatusCard(c, x + cardW + gap, y, cardW, "PREISALARME", activeAlertCount() + " aktiv",
            latestAlertLine(), activeAlertCount() > 0 ? GREEN : MUTED);
        drawStatusCard(c, x + (cardW + gap) * 2, y, cardW, "KURSE / HÄNDLER", shardCache.snapshot().size() + " Kurse",
            cacheState(shardCache.getLastUpdatedMs(), shardCache.getAgeSeconds(), cfg.merchantRefreshIntervalSeconds),
            shardCache.isEmpty() ? YELLOW : INFO);

        int rowY = y + 80;
        drawDataLine(c, x, rowY, w, "Letzte Markt-Aktualisierung", ageText(marketCache.getLastUpdatedMs()), INFO);
        rowY += 27;
        drawDataLine(c, x, rowY, w, "Letzte Kurs-Aktualisierung", ageText(shardCache.getLastUpdatedMs()), INFO);
        rowY += 27;
        drawDataLine(c, x, rowY, w, "Observer-Modus", cfg.observerModeOnly ? "AN · Eingriffe deaktiviert" : "AUS", cfg.observerModeOnly ? YELLOW : GREEN);
        rowY += 27;
        if (rowY + 22 < window.contentBottom()) {
            drawDataLine(c, x, rowY, w, "Auktionshaus", auctionCache.snapshot().size() + " aktiv · " + ageText(auctionCache.getLastUpdatedMs()),
                auctionCache.snapshot().isEmpty() ? YELLOW : INFO);
            rowY += 27;
        }
        String newest = latestAlertLine();
        if (rowY + 22 < window.contentBottom())
            drawDataLine(c, x, rowY, w, "Letzter Preisalarm", newest, MUTED);
    }

    private void drawStatusCard(IngameUxCanvas c, int x, int y, int w, String title, String value, String hint, int valueColor) {
        int h = Math.max(58, Math.min(76, window.bodyBottom() - y - 38));
        card(c, x, y, w, h);
        c.text(title, x + 8, y + 7, MUTED, false);
        c.text(fit(c, value, w - 16), x + 8, y + 7 + c.lineHeight() + 3, valueColor, true);
        if (h >= 58) c.text(fit(c, hint, w - 16), x + 8, y + h - c.lineHeight() - 5, hintColor(hint), false);
    }

    private void drawMarket(IngameUxCanvas c, int x, int y, int w, int mouseX, int mouseY) {
        int searchY = y + 3;
        drawInput(c, marketSearch, "Item suchen …");
        int headerY = searchY + 57;
        int nameW = Math.max(40, w - 190);
        c.text("ITEM", x + 6, headerY, MUTED, true);
        if (w >= 280) {
            c.text("KAUF", x + nameW, headerY, MUTED, true);
            c.text("VERKAUF", x + nameW + 72, headerY, MUTED, true);
        }
        int rowsY = headerY + c.lineHeight() + 6;
        int footerPad = c.lineHeight() * 2 + 7;
        int available = Math.max(24, window.contentBottom() - rowsY - footerPad);
        rowHeight = Math.max(27, Math.min(42, available / 4));
        visibleMarketRows = Math.max(1, available / rowHeight);
        if (marketRows.isEmpty()) {
            String message = marketCache.isEmpty() ? "Noch keine Marktdaten synchronisiert." : "Keine Items passen zu Suche und Kategorie.";
            drawEmptyState(c, x + 5, rowsY + 10, w - 10, message,
                marketCache.isEmpty() ? "Die nächste reguläre Synchronisierung abwarten oder System → Jetzt aktualisieren wählen." : "Suche oder Kategorie anpassen.");
        } else {
            c.enableScissor(x + 2, rowsY, x + w - 2, rowsY + available);
            double fraction = marketScrollPosition - marketScroll;
            int end = Math.min(marketRows.size(), marketScroll + visibleMarketRows + (fraction > 0.01 ? 1 : 0));
            int offset = (int) Math.round(fraction * rowHeight);
            for (int i = marketScroll; i < end; i++) {
                MarketPrice price = marketRows.get(i);
                int index = i - marketScroll;
                int ry = rowsY + index * rowHeight - offset;
                card(c, x + 2, ry, w - 4, rowHeight - 2);
                String title = itemName(price.getItemKey());
                if (w >= 280) {
                    c.text(fit(c, title, nameW - 15), x + 11, ry + 4, WHITE, false);
                    String orders = "K " + price.getBuyOrders() + " · V " + price.getSellOrders() + " aktiv";
                    c.text(fit(c, orders, nameW - 15), x + 11, ry + c.lineHeight() + 5, MUTED, false);
                    c.text(marketPriceText(price.getBuy()), x + nameW, ry + 6, INFO, true);
                    c.text(marketPriceText(price.getSell()), x + nameW + 72, ry + 6, INFO, true);
                } else {
                    c.text(fit(c, title, w - 18), x + 10, ry + 3, WHITE, false);
                    c.text("K " + marketPriceText(price.getBuy()) + "   V " + marketPriceText(price.getSell()), x + 10, ry + c.lineHeight() + 3, INFO, true);
                    if (rowHeight >= 39) c.text(price.getBuyOrders() + " / " + price.getSellOrders() + " aktive Aufträge", x + 10, ry + c.lineHeight() * 2 + 3, MUTED, false);
                }
                if (mouseX >= x && mouseX <= x + w && mouseY >= rowsY && mouseY < rowsY + available
                    && mouseY >= ry && mouseY < ry + rowHeight - 2) {
                    hoveredRow = i;
                    hoveredDetails = title + "\nInterner Item-Key: " + price.getItemKey()
                        + "\nAktive Kaufaufträge: " + price.getBuyOrders()
                        + "\nAktive Verkaufsaufträge: " + price.getSellOrders();
                    c.fill(x + 2, ry, x + 4, ry + rowHeight - 2, ICE);
                }
            }
            c.disableScissor();
        }
        int summaryY = window.contentBottom() - c.lineHeight() * 2 - 8;
        c.text(fit(c, marketSummary(), w - 8), x + 4, summaryY, MUTED, false);
        c.text(fit(c, activeOrdersSummary(), w - 8), x + 4, summaryY + c.lineHeight(), MUTED, false);
    }

    private void drawShards(IngameUxCanvas c, int x, int y, int w, int mouseX, int mouseY) {
        drawInput(c, shardSearch, "Material suchen …");
        String age = ageText(shardCache.getLastUpdatedMs());
        c.text(fit(c, "Cache · " + age, w - 12), x + 6, y + 50,
            shardCache.isStale(Math.max(60, cfg.tooltipMaxAgeSeconds)) ? YELLOW : MUTED, false);
        int headY = y + 66;
        c.text("ZIEL / WEG", x + 6, headY, MUTED, true);
        c.text("KURS", x + Math.max(70, w - 115), headY, MUTED, true);
        int rowsY = headY + c.lineHeight() + 6;
        int available = Math.max(26, window.contentBottom() - rowsY - c.lineHeight() - 20);
        int rh = Math.max(24, Math.min(34, available / 4));
        visibleShardRows = Math.max(1, available / rh);
        if (shardRows.isEmpty()) {
            drawEmptyState(c, x + 5, rowsY + 10, w - 10,
                shardCache.isEmpty() ? "Noch keine Shard- oder Händlerkurse synchronisiert." : "Keine Kurse vorhanden.",
                "Der Menüaufruf fragt keine API ab. Werte kommen aus dem regulären Händler-Cache.");
        } else {
            c.enableScissor(x + 2, rowsY, x + w - 2, rowsY + available);
            double fraction = shardScrollPosition - shardScroll;
            int end = Math.min(shardRows.size(), shardScroll + visibleShardRows + (fraction > 0.01 ? 1 : 0));
            int offset = (int) Math.round(fraction * rh);
            for (int i = shardScroll; i < end; i++) {
                ShardRate rate = shardRows.get(i);
                int index = i - shardScroll;
                int ry = rowsY + index * rh - offset;
                card(c, x + 2, ry, w - 4, rh - 2);
                String itemKey = rate.getSource() == null ? "" : rate.getSource().toLowerCase(Locale.ROOT);
                if (itemKey.startsWith("minecraft:")) itemKey = itemKey.substring("minecraft:".length());
                if (itemKey.equals("paper#625")) c.icon("shard/holzbuendel", x + 7, ry + 4, 16);
                else if (itemKey.equals("paper#626")) c.icon("shard/graebergemisch", x + 7, ry + 4, 16);
                else if (itemKey.equals("paper#635")) c.icon("shard/steinplatten", x + 7, ry + 4, 16);
                else c.item(itemKey.contains("#") ? itemKey.substring(0, itemKey.indexOf('#')) : itemKey, x + 7, ry + 4);
                String source = shardName(rate);
                MerchantCurrency currency = MerchantCurrency.fromApiTarget(rate.getTarget());
                String target = currency == MerchantCurrency.UNKNOWN ? currencyLabel(rate.getTarget()) : currency.getDisplayUnit();
                String description = source + "  →  " + target;
                int rateX = x + Math.max(70, w - 115);
                c.text(fit(c, description, Math.max(40, rateX - x - 40)), x + 30, ry + 5, WHITE, false);
                c.text(formatRate(rate.getExchangeRate()), rateX, ry + 5, INFO, true);
                if (rh >= 38) c.text("Basis: " + formatRate(rate.getBase()), x + 10, ry + c.lineHeight() + 6, MUTED, false);
                if (mouseX >= x && mouseX <= x + w && mouseY >= rowsY && mouseY < rowsY + available
                    && mouseY >= ry && mouseY < ry + rh - 2) {
                    hoveredRow = i;
                    hoveredDetails = description + "\nQuelle: " + rate.getSource()
                        + "\nBasis: " + formatRate(rate.getBase())
                        + "\nWechselkurs: " + formatRate(rate.getExchangeRate());
                    c.fill(x + 2, ry, x + 4, ry + rh - 2, ICE);
                }
            }
            c.disableScissor();
        }
        c.text(fit(c, shardRows.size() + " Kurse im lokalen Cache", w - 10),
            x + 5, window.contentBottom() - c.lineHeight() - 4, MUTED, false);
    }

    private void drawAuctions(IngameUxCanvas c, int x, int y, int w, int mouseX, int mouseY) {
        drawInput(c, auctionSearch, "Auktion suchen …");
        c.text(fit(c, "Aktiv · " + auctionRows.size() + " Auktionen · " + ageText(auctionCache.getLastUpdatedMs()), w - 12), x + 6, y + 50, MUTED, false);
        int rowsY = y + 68;
        int rh = 38;
        int available = Math.max(30, window.contentBottom() - rowsY - c.lineHeight() - 8);
        int count = Math.max(1, available / rh);
        if (auctionRows.isEmpty()) {
            drawEmptyState(c, x + 5, rowsY + 10, w - 10,
                auctionCache.snapshot().isEmpty() ? "Noch keine aktiven Auktionen synchronisiert." : "Keine Auktionen passen zu Suche oder Kategorie.",
                "Das Menü liest nur den lokalen Auktions-Cache; keine Aktion wird an OPSUCHT gesendet.");
            return;
        }
        visibleAuctionRows = Math.max(1, available / rh);
        int maxScroll = Math.max(0, auctionRows.size() - visibleAuctionRows);
        auctionScrollTarget = clamp(auctionScrollTarget, 0, maxScroll);
        auctionScrollPosition = clamp(auctionScrollPosition, 0, maxScroll);
        auctionScroll = (int) Math.floor(auctionScrollPosition);
        double fraction = auctionScrollPosition - auctionScroll;
        int start = auctionScroll;
        int end = Math.min(auctionRows.size(), start + visibleAuctionRows + (fraction > 0.01 ? 1 : 0));
        for (int i = start; i < end; i++) {
            Auction auction = auctionRows.get(i);
            int ry = rowsY + (int) Math.round((i - start - fraction) * rh);
            card(c, x + 2, ry, w - 4, rh - 2);
            String material = auction.item() == null ? "barrier" : auction.item().material().toLowerCase(Locale.ROOT);
            c.item(material, x + 7, ry + 5);
            String name = auction.item() == null || auction.item().displayName() == null ? material : auction.item().displayName();
            c.text(fit(c, name, Math.max(50, w - 150)), x + 30, ry + 4, WHITE, false);
            String price = "Gebot " + formatMoney(auction.currentBid());
            if (auction.instantBuyPrice() != null && auction.instantBuyPrice() > 0) price += " · Sofort " + formatMoney(auction.instantBuyPrice());
            c.text(fit(c, price, Math.max(50, w - 42)), x + 30, ry + c.lineHeight() + 6, INFO, false);
            if (mouseX >= x && mouseX <= x + w && mouseY >= ry && mouseY < ry + rh - 2) {
                hoveredRow = i;
                hoveredDetails = name + "\nKategorie: " + auction.category() + "\nStatus: " + auction.state()
                    + "\nAktuelles Gebot: " + formatMoney(auction.currentBid())
                    + (auction.instantBuyPrice() != null ? "\nSofortkauf: " + formatMoney(auction.instantBuyPrice()) : "")
                    + "\nUID: " + auction.uid();
                c.fill(x + 2, ry, x + 4, ry + rh - 2, ICE);
            }
        }
    }

    private void drawTooltipSummary(IngameUxCanvas c, int x, int y, int w) {
        drawInfoBox(c, x, y, w, 42,
            "Normale Slot-Tooltips beim Zeigen im Inventar, in Kisten oder Menüs. Observer-Modus blendet sie aus; permanentes HUD, Alarm-HUD und Containeranzeige sind getrennt.",
            cfg.observerModeOnly ? YELLOW : INFO);
    }

    private void drawAlerts(IngameUxCanvas c, int x, int y, int w, int mouseX, int mouseY) {
        if (cfg.priceAlertRules.isEmpty()) {
            drawEmptyState(c, x + 4, y + 74, w - 8,
                "Noch keine Preisalarm-Regeln vorhanden.",
                "Mit „Alarm anlegen“ eine Regel aus dem lokalen Markt-Cache erstellen.");
        }
    }

    private void drawAlertEditor(IngameUxCanvas c, int x, int y, int w) {
        c.text(fit(c, alertEditingId == null ? "Neue Alarmregel" : "Alarmregel bearbeiten", w - 12), x + 5, y + 2, WHITE, true);
        int first = y + 24 - pageScroll;
        if (first >= interactiveContentTop() && first < window.contentBottom())
            c.text("Item suchen (lokaler Markt-Cache)", x + 5, first, MUTED, false);
        drawInput(c, alertItemSearch, "Itemnamen oder Key eingeben …");
        int thresholdY = y + 118 - pageScroll;
        if (thresholdY >= interactiveContentTop() && thresholdY < window.contentBottom())
            c.text("Schwellenwert", x + 5, thresholdY, MUTED, false);
        drawInput(c, alertThresholdInput, "z. B. 120,50");
        int cooldownY = y + 242 - pageScroll;
        if (cooldownY >= interactiveContentTop() && cooldownY < window.contentBottom())
            c.text("Cooldown in Sekunden (10–86400)", x + 5, cooldownY, MUTED, false);
        drawInput(c, alertCooldownInput, "300");
    }

    private void drawInput(IngameUxCanvas c, EditBox input, String placeholder) {
        if (input == null || !input.visible) return;
        String value = input.getValue();
        int x = input.getX(), y = input.getY(), w = input.getWidth(), h = input.getHeight();
        c.fill(x, y, x + w, y + h, CARD_DARK);
        outline(c, x, y, w, h, input.isFocused() ? ICE : LINE);
        c.text(fit(c, value.isBlank() ? placeholder : value, w - 12), x + 6,
            y + Math.max(1, (h - c.lineHeight()) / 2), value.isBlank() ? MUTED : WHITE, false);
        if (input.isFocused() && (System.currentTimeMillis() / 500) % 2 == 0) {
            int caretAt = Math.min(value.length(), input.getCursorPosition());
            int caretX = x + 6 + c.textWidth(fit(c, value.substring(0, caretAt), w - 12));
            c.fill(Math.min(x + w - 6, caretX), y + 4, Math.min(x + w - 5, caretX + 1), y + h - 4, INFO);
        }
    }

    private void drawProtectionSummary(IngameUxCanvas c, int x, int y, int w) {
        drawInfoBox(c, x, y + 1, w, 42,
            cfg.observerModeOnly
                ? "Observer-Modus ist aktiv: HUD, Tooltips, Container, Schutz und Komfort greifen nicht ein. Cache-Ansichten bleiben verfügbar."
                : "Observer-Modus schaltet Ingame-Eingriffe gesammelt aus. Anzeige-, Schutz- und Komfortoptionen bleiben einzeln konfigurierbar.",
            cfg.observerModeOnly ? YELLOW : MUTED);
    }

    private void drawSystem(IngameUxCanvas c, int x, int y, int w) {
        int rowY = y + 2;
        String minecraftVersion = FabricLoader.getInstance().getModContainer("minecraft")
            .map(container -> container.getMetadata().getVersion().getFriendlyString()).orElse("Unbekannt");
        drawDataLine(c, x, rowY, w, "Minecraft", minecraftVersion, INFO);
        rowY += 27;
        drawDataLine(c, x, rowY, w, "Markt-Cache", marketCache.snapshot().size() + " Items · " + ageText(marketCache.getLastUpdatedMs()),
            marketCache.isEmpty() ? YELLOW : INFO);
        rowY += 27;
        drawDataLine(c, x, rowY, w, "Händler-Cache", shardCache.snapshot().size() + " Kurse · " + ageText(shardCache.getLastUpdatedMs()),
            shardCache.isEmpty() ? YELLOW : INFO);
        rowY += 27;
        var server = mod.getWebServer();
        boolean running = server != null && server.isRunning();
        String webStatus = running ? "Lokal aktiv · 127.0.0.1:" + server.getPort()
            : cfg.enableWebUi ? "Konfiguriert, aktuell nicht aktiv" : "Deaktiviert";
        drawDataLine(c, x, rowY, w, "Web-Interface", webStatus, running ? GREEN : MUTED);
        rowY += 35;
        drawInfoBox(c, x, rowY, w, 39,
            "Statuskarten enthalten keine Proxy-, Webhook- oder Passwortdaten. Öffnen und Navigieren fragen keine API ab.", MUTED);
    }

    private void drawControls(IngameUxCanvas c) {
        for (Control control : controls) {
            Button b = control.widget;
            if (!b.visible) continue;
            boolean active = control.selected.getAsBoolean();
            boolean focused = b.isFocused();
            int background = active ? 0xFF31526A : b.isHovered() ? 0xFF3D4C5B : CARD;
            int border = focused ? INFO : active || control.accent ? ICE : LINE;
            int y = b.getY();
            c.fill(control.x, y, control.x + control.width, y + control.height, background);
            c.fill(control.x, y, control.x + control.width, y + 1, border);
            c.fill(control.x, y + control.height - 1, control.x + control.width, y + control.height, border);
            c.fill(control.x, y, control.x + 1, y + control.height, border);
            c.fill(control.x + control.width - 1, y, control.x + control.width, y + control.height, border);
            String label = fit(c, control.label.get(), control.width - 14);
            int color = active ? WHITE : (control.accent ? INFO : WHITE);
            c.text(label, control.x + 7, y + Math.max(1, (control.height - c.lineHeight()) / 2), color, focused);
            if (focused) outline(c, control.x - 2, y - 2, control.width + 4, control.height + 4, INFO);
        }
    }

    private void drawFooter(IngameUxCanvas c) {
        int footerTop = window.bodyBottom() + 7;
        String build = "Visotaris " + FabricLoader.getInstance().getModContainer("visotaris_opmod")
            .map(container -> container.getMetadata().getVersion().getFriendlyString()).orElse("?");
        String status = !transientStatus.isBlank() && System.currentTimeMillis() - transientStatusAt < 5000
            ? transientStatus : hasUnsavedChanges() ? "Ungespeicherte Änderungen" : "ESC zurück · TAB Fokus";
        int x = window.x() + 12;
        if (footerBackButton == null || footerSaveButton == null) return;
        int textRight = footerBackButton.getX() - 8;
        int available = Math.max(0, textRight - x);
        String display = available > c.textWidth(build) + c.textWidth(status) + 12
            ? build + " · " + status : status;
        int color = hasUnsavedChanges() ? YELLOW : INFO;
        c.text(fit(c, display, available), x, footerTop + 3, color, false);
        drawFooterButton(c, footerBackButton, "Zurück", false);
        drawFooterButton(c, footerSaveButton, "Speichern", hasUnsavedChanges());
    }

    private void drawFooterButton(IngameUxCanvas c, Button button, String label, boolean enabled) {
        int x = button.getX(), y = button.getY(), w = button.getWidth(), h = button.getHeight();
        int fill = !enabled && "Speichern".equals(label) ? CARD_DARK : button.isFocused() ? ICE
            : button.isHovered() ? 0xFF3D6682 : CARD;
        int edge = button.isFocused() ? INFO : "Speichern".equals(label) && enabled ? GREEN : LINE;
        c.fill(x, y, x + w, y + h, fill);
        c.fill(x, y, x + w, y + 1, edge);
        c.fill(x, y + h - 1, x + w, y + h, edge);
        c.fill(x, y, x + 1, y + h, edge);
        c.fill(x + w - 1, y, x + w, y + h, edge);
        int color = !enabled && "Speichern".equals(label) ? OFF : button.isFocused() ? NAVY[0] : WHITE;
        c.text(fit(c, label, w - 8), x + 4, y + Math.max(1, (h - c.lineHeight()) / 2), color, button.isFocused());
    }

    private void drawUnsavedDialog(IngameUxCanvas c) {
        c.fill(0, 0, width, height, 0xB8000000);
        int modalW = Math.min(330, window.width() - 28);
        int modalH = 154;
        int x = window.x() + (window.width() - modalW) / 2;
        int y = window.y() + (window.height() - modalH) / 2;
        c.fill(x - 2, y - 2, x + modalW + 2, y + modalH + 2, FRAME);
        c.fill(x, y, x + modalW, y + modalH, PANEL);
        c.fill(x, y, x + modalW, y + 22, NAVY[1]);
        c.text("Ungespeicherte Änderungen", x + 10, y + 7, WHITE, true);
        c.text("Nicht gespeicherte Werte beibehalten?", x + 10, y + 31, MUTED, false);
        String[] labels = {"Speichern", "Änderungen verwerfen", "Weiter bearbeiten"};
        for (int i = 0; i < modalButtons.size(); i++) {
            Button button = modalButtons.get(i);
            boolean primary = i == 0;
            int fill = button.isFocused() ? ICE : button.isHovered() ? 0xFF3D6682 : CARD;
            c.fill(button.getX(), button.getY(), button.getX() + button.getWidth(), button.getY() + button.getHeight(), fill);
            outline(c, button.getX(), button.getY(), button.getWidth(), button.getHeight(), primary ? GREEN : LINE);
            String label = fit(c, labels[i], button.getWidth() - 6);
            c.text(label, button.getX() + Math.max(3, (button.getWidth() - c.textWidth(label)) / 2),
                button.getY() + Math.max(1, (button.getHeight() - c.lineHeight()) / 2),
                button.isFocused() ? NAVY[0] : WHITE, button.isFocused());
        }
    }

    private void buildOverview(int x, int y, int w) {
        contentEnd = window.bodyTop() + 12;
    }

    private void buildMarket(int x, int y, int w) {
        int categoryW = Math.min(190, Math.max(100, w / 3));
        int searchW = Math.max(50, w - categoryW - 8);
        marketSearch = new EditBox(font, x, y, searchW, 19, Component.literal("Marktitems suchen"));
        marketSearch.setMaxLength(80);
        marketSearch.setValue(marketQuery);
        marketSearch.setAlpha(0f);
        marketSearch.setResponder(value -> { marketQuery = value; marketScroll = 0; marketScrollPosition = marketScrollTarget = 0; refreshMarketRows(); });
        addRenderableWidget(marketSearch);
        addControl(x + searchW + 8, y, categoryW, 19,
            () -> "Kategorie: " + selectedCategoryName() + "  ▾", () -> selectedMarketCategory > 0,
            () -> "Aktuell: " + selectedCategoryName() + ". Verfügbare Kategorien aus dem Markt-Cache; keine zusätzlichen Abrufe.", false,
            () -> {
                if (categories.isEmpty()) { setStatus("Im Cache sind noch keine Kategorien vorhanden."); return; }
                selectedMarketCategory = (selectedMarketCategory + 1) % (categories.size() + 1);
                marketScroll = 0;
                marketScrollPosition = marketScrollTarget = 0;
                refreshMarketRows();
                rebuildWidgets();
            });
        int sortY = y + 24;
        int sortW = Math.max(90, w - 88);
        addControl(x, sortY, sortW, 20,
            () -> "Sortierung: " + MARKET_SORTS[marketSort], () -> false,
            () -> "Sortieren nach " + MARKET_SORTS[marketSort] + ". Klicken für das nächste Kriterium.", false,
            () -> { marketSort = (marketSort + 1) % MARKET_SORTS.length; marketSortDescending = false; refreshMarketRows(); rebuildWidgets(); });
        addControl(x + sortW + 5, sortY, w - sortW - 5, 20,
            () -> marketSortDescending ? "Z–A / ↓" : "A–Z / ↑", () -> marketSortDescending,
            () -> "Sortierrichtung umkehren.", false,
            () -> { marketSortDescending = !marketSortDescending; refreshMarketRows(); rebuildWidgets(); });
        listTop = y + 52;
        int bottom = window.contentBottom() - font.lineHeight - 8;
        int available = Math.max(26, bottom - listTop);
        visibleMarketRows = Math.max(1, available / 43);
        contentEnd = bottom;
    }

    private void buildShards(int x, int y, int w) {
        int filterW = Math.min(190, Math.max(100, w / 3));
        int searchW = Math.max(50, w - filterW - 6);
        shardSearchBaseY = y;
        shardSearch = new EditBox(font, x, y - pageScroll, searchW, 19, Component.literal("Material suchen"));
        shardSearch.setMaxLength(80);
        shardSearch.setValue(shardQuery);
        shardSearch.setResponder(value -> { shardQuery = value; shardScroll = 0; shardScrollPosition = shardScrollTarget = 0; refreshShardRows(); });
        addRenderableWidget(shardSearch);
        addControl(x + searchW + 6, y, filterW, 19, () -> "Ziel: " + shardTargetFilterLabel() + "  ▾",
            () -> selectedShardTarget > 0, () -> "Aktuelles Ziel: " + shardTargetFilterLabel() + ". Filtert lokale Händler-Cache-Werte.", false, () -> {
                selectedShardTarget = (selectedShardTarget + 1) % shardTargetCount();
                shardScroll = 0;
                shardScrollPosition = shardScrollTarget = 0;
                refreshShardRows();
                rebuildWidgets();
            });
        int sortW = Math.max(72, w - 80);
        addControl(x, y + 24, sortW, 20, () -> "Sortierung: " + SHARD_SORTS[shardSort], () -> false,
            () -> "Material, Kurs, Basiskurs oder Abweichung sortieren.", false,
            () -> { shardSort = (shardSort + 1) % SHARD_SORTS.length; shardSortDescending = false; refreshShardRows(); rebuildWidgets(); });
        addControl(x + sortW + 5, y + 24, w - sortW - 5, 20,
            () -> shardSortDescending ? "Z–A / ↓" : "A–Z / ↑", () -> shardSortDescending,
            () -> "Sortierrichtung umkehren.", false,
            () -> { shardSortDescending = !shardSortDescending; refreshShardRows(); rebuildWidgets(); });
        listTop = y + 78;
        visibleShardRows = Math.max(1, (window.contentBottom() - listTop - 24) / 39);
        contentEnd = window.contentBottom();
    }

    private void buildTooltips(int x, int y, int w) {
        int row = 27;
        int start = y + 52;
        addToggle(x, start, w, "Marktpreise im normalen Item-Tooltip", () -> cfg.showMarketTooltips,
            value -> cfg.showMarketTooltips = value,
            "Nur der Item-Slot-Tooltip beim Zeigen mit der Maus.", true);
        addToggle(x, start + row, w, "Kaufpreis anzeigen", () -> cfg.tooltipShowBuyPrice,
            value -> cfg.tooltipShowBuyPrice = value, "Kaufpreis im normalen Item-Tooltip.", true);
        addToggle(x, start + row * 2, w, "Verkaufspreis anzeigen", () -> cfg.tooltipShowSellPrice,
            value -> cfg.tooltipShowSellPrice = value, "Verkaufspreis im normalen Item-Tooltip.", true);
        addToggle(x, start + row * 3, w, "Shardkurse anzeigen", () -> cfg.tooltipShowShardRates,
            value -> cfg.tooltipShowShardRates = value, "OPShard-Kurse im normalen Item-Tooltip.", true);
        addToggle(x, start + row * 4, w, "Sonstige Händlerkurse anzeigen", () -> cfg.tooltipShowMerchantRates,
            value -> cfg.tooltipShowMerchantRates = value, "Zum Beispiel Redcoin-Kurse im normalen Item-Tooltip.", true);
        addToggle(x, start + row * 5, w, "Alter der Cache-Daten anzeigen", () -> cfg.tooltipShowDataAge,
            value -> cfg.tooltipShowDataAge = value, "Datenalter wird im Tooltip ergänzt.", true);
        addToggle(x, start + row * 6, w, "Veraltete Daten weiterhin anzeigen", () -> cfg.tooltipShowStaleData,
            value -> cfg.tooltipShowStaleData = value, "Wenn aus, werden zu alte Werte ausgeblendet.", true);
        int ageY = start + row * 7;
        int[] options = {300, 900, 1800, 3600};
        addControl(x, ageY, w, 23, () -> "Veraltet ab  ·  " + ageTextSeconds(cfg.tooltipMaxAgeSeconds),
            () -> false, () -> "Grenzwert für Markt- und Händler-Cache im Tooltip.", false, () -> {
                int currentIndex = 0;
                for (int i = 0; i < options.length; i++) if (options[i] == cfg.tooltipMaxAgeSeconds) currentIndex = i;
                final int next = options[(currentIndex + 1) % options.length];
                updateConfig(() -> cfg.tooltipMaxAgeSeconds = next);
            });
        contentEnd = ageY + 26;
    }

    private void buildAuctions(int x, int y, int w) {
        int fieldY = y + 2;
        auctionSearchBaseY = fieldY;
        auctionSearch = new EditBox(font, x + 3, auctionSearchBaseY - pageScroll, Math.max(96, w - 6), 19, Component.literal("Auktion suchen"));
        auctionSearch.setMaxLength(80);
        auctionSearch.setValue(auctionQuery);
        auctionSearch.setResponder(value -> { auctionQuery = value; refreshAuctionRows(); });
        addRenderableWidget(auctionSearch);
        addControl(x, y + 28, w / 2 - 3, 23, () -> "Kategorie: " + auctionCategoryLabel() + "  ▾", () -> false,
            () -> "Filtert ausschließlich lokale aktive Auktionsdaten.", false, () -> { cycleAuctionCategory(); refreshAuctionRows(); rebuildWidgets(); });
        addControl(x + w / 2 + 3, y + 28, w / 2 - 3, 23, () -> "Sortierung: " + auctionSortLabel() + (auctionSortDescending ? " ↓" : " ↑"), () -> false,
            () -> "Lokale Sortierung: Name, Gebot, Sofortkauf oder Ablauf.", false, () -> { auctionSort = (auctionSort + 1) % 4; auctionSortDescending = false; refreshAuctionRows(); rebuildWidgets(); });
        contentEnd = y + 55;
    }
    private void buildAlerts(int x, int y, int w) {
        alertItemSearch = null;
        alertThresholdInput = null;
        alertCooldownInput = null;
        if (alertDraft != null) { buildAlertEditor(x, y, w); return; }
        addControl(x, y, w, 23, () -> "Alle Preisalarme  ·  " + (cfg.priceAlertsEnabled ? "AN" : "AUS"),
            () -> cfg.priceAlertsEnabled, () -> "Globale Aktivierung der lokalen Preisalarmprüfung.", true,
            () -> updateConfig(() -> cfg.priceAlertsEnabled = !cfg.priceAlertsEnabled));
        addControl(x, y + 27, w, 23, () -> "Neuen Preisalarm anlegen", () -> false,
            () -> "Regel aus einem Item im lokalen Markt-Cache erstellen.", true, () -> openAlertEditor(null));
        listTop = y + 56;
        int avail = Math.max(26, window.contentBottom() - listTop - 26);
        visibleAlertRows = Math.max(1, avail / 43);
        List<PriceAlertRule> rules = cfg.priceAlertRules;
        int maxAlertScroll = Math.max(0, rules.size() - visibleAlertRows);
        alertScrollTarget = clamp(alertScrollTarget, 0, maxAlertScroll);
        alertScrollPosition = clamp(alertScrollPosition, 0, maxAlertScroll);
        alertScroll = Math.min(alertScroll, maxAlertScroll);
        int start = Math.min(alertScroll, Math.max(0, rules.size() - 1));
        int end = Math.min(rules.size(), start + visibleAlertRows);
        for (int i = start; i < end; i++) {
            PriceAlertRule rule = rules.get(i);
            int ry = listTop + (i - start) * 43;
            int actionW = Math.min(56, Math.max(32, w / 6));
            addControl(x, ry, w - actionW * 2 - 8, 36,
                () -> itemName(rule.itemKey) + " · " + conditionCompact(rule.condition) + " " + formatMoney(rule.threshold)
                    + " · " + (rule.enabled ? "AN" : "AUS"), () -> rule.enabled,
                () -> itemName(rule.itemKey) + " · " + conditionText(rule.condition) + " " + formatMoney(rule.threshold)
                    + " · aktuell " + currentAlertValue(rule) + " · " + alertState(rule)
                    + " · " + channelText(rule.notificationChannel()),
                false, () -> openAlertEditor(rule));
            addControl(x + w - actionW * 2 - 4, ry, actionW, 36,
                () -> rule.enabled ? "Pause" : "Aktiv", () -> rule.enabled,
                () -> rule.enabled ? "Regel pausieren" : "Regel aktivieren", false,
                () -> updateConfig(() -> rule.enabled = !rule.enabled));
            addControl(x + w - actionW, ry, actionW, 36,
                () -> Objects.equals(alertDeleteId, rule.id) ? "Sicher?" : "Löschen", () -> false,
                () -> Objects.equals(alertDeleteId, rule.id) ? "Erneut klicken, um diese Regel zu löschen." : "Löschen mit Bestätigung.", false,
                () -> {
                    if (!Objects.equals(alertDeleteId, rule.id)) { alertDeleteId = rule.id; rebuildWidgets(); return; }
                    updateConfig(() -> cfg.priceAlertRules.removeIf(r -> Objects.equals(r.id, rule.id)));
                    alertDeleteId = null;
                });
        }
        contentEnd = window.contentBottom();
    }

    private void openAlertEditor(PriceAlertRule source) {
        alertDraft = source == null ? new PriceAlertRule() : PriceAlertRule.fromMap(source.toMap());
        alertEditingId = source == null ? null : source.id;
        alertEditorDirty = source == null;
        alertItemQuery = "";
        alertThreshold = source == null ? "" : Double.toString(source.threshold);
        alertCooldown = Integer.toString(alertDraft.cooldownSeconds);
        alertItemIndex = 0;
        if (source == null && !marketCache.snapshot().isEmpty())
            alertDraft.itemKey = sortedMarketKeys().get(0);
        pageScroll = 0;
        rebuildWidgets();
    }

    private List<String> sortedMarketKeys() {
        return marketCache.snapshot().keySet().stream()
            .sorted(Comparator.comparing(this::itemName, String.CASE_INSENSITIVE_ORDER)).toList();
    }

    private List<String> matchingAlertItems() {
        String query = alertItemQuery.toLowerCase(Locale.ROOT).trim();
        return sortedMarketKeys().stream().filter(key -> query.isEmpty() || key.toLowerCase(Locale.ROOT).contains(query)
            || itemName(key).toLowerCase(Locale.ROOT).contains(query)).toList();
    }

    private void buildAlertEditor(int x, int y, int w) {
        int fieldW = Math.max(95, w - 6);
        alertItemSearchBaseY = y + 39;
        alertItemSearch = new EditBox(font, x + 3, alertItemSearchBaseY - pageScroll, fieldW, 19, Component.literal("Item suchen"));
        alertItemSearch.setMaxLength(80);
        alertItemSearch.setValue(alertItemQuery);
        alertItemSearch.setResponder(value -> { alertItemQuery = value; alertItemIndex = -1; });
        addRenderableWidget(alertItemSearch);
        addControl(x, y + 63, w, 23,
            () -> "Item: " + itemName(alertDraft.itemKey) + "  ▾", () -> false,
            () -> "Auswahl aus dem lokalen Markt-Cache. Vollständiger Key: " + alertDraft.itemKey, false,
            () -> {
                List<String> keys = matchingAlertItems();
                if (keys.isEmpty()) { setStatus("Kein passendes Marktitem im Cache."); return; }
                alertItemIndex = (alertItemIndex + 1) % keys.size();
                alertDraft.itemKey = keys.get(alertItemIndex);
                alertEditorDirty = true;
                rebuildWidgets();
            });
        String[] conditions = {"BUY_ABOVE", "BUY_BELOW", "SELL_ABOVE", "SELL_BELOW", "SPREAD_ABOVE", "SPREAD_BELOW"};
        addControl(x, y + 91, w, 23, () -> "Bedingung: " + conditionText(alertDraft.condition), () -> false,
            () -> "Kaufpreis, Verkaufspreis oder Spanne über/unter Schwelle.", false,
            () -> { int index = java.util.Arrays.asList(conditions).indexOf(alertDraft.condition);
                alertDraft.condition = conditions[(index + 1) % conditions.length]; alertEditorDirty = true; rebuildWidgets(); });
        alertThresholdBaseY = y + 133;
        alertThresholdInput = new EditBox(font, x + 3, alertThresholdBaseY - pageScroll, fieldW, 19, Component.literal("Schwellenwert"));
        alertThresholdInput.setMaxLength(32);
        alertThresholdInput.setValue(alertThreshold);
        alertThresholdInput.setResponder(value -> {
            alertThreshold = value; alertEditorDirty = true;
            if (footerSaveButton != null) footerSaveButton.active = true;
        });
        addRenderableWidget(alertThresholdInput);
        String[] channels = {"HUD", "WEB", "HUD_WEB"};
        addControl(x, y + 158, w, 23, () -> "Ausgabe: " + channelText(alertDraft.notificationChannel()), () -> false,
            () -> "HUD, Web-UI bei geöffnetem Browser oder beides; niemals Minecraft-Chat.", false,
            () -> { int index = java.util.Arrays.asList(channels).indexOf(alertDraft.notificationChannel());
                alertDraft.notification = channels[(index + 1) % channels.length]; alertEditorDirty = true; rebuildWidgets(); });
        addControl(x, y + 186, w, 23, () -> "Wiederholung: " + (alertDraft.repeat ? "AN" : "AUS"),
            () -> alertDraft.repeat, () -> "Wiederholt nach dem konfigurierten Cooldown melden.", false,
            () -> { alertDraft.repeat = !alertDraft.repeat; alertEditorDirty = true; rebuildWidgets(); });
        addControl(x, y + 214, w, 23, () -> "Nach Verlassen erneut scharf: " + (alertDraft.rearmOnExit ? "AN" : "AUS"),
            () -> alertDraft.rearmOnExit, () -> "Erst nach Rückkehr aus dem Schwellenbereich erneut melden.", false,
            () -> { alertDraft.rearmOnExit = !alertDraft.rearmOnExit; alertEditorDirty = true; rebuildWidgets(); });
        alertCooldownBaseY = y + 257;
        alertCooldownInput = new EditBox(font, x + 3, alertCooldownBaseY - pageScroll, fieldW, 19, Component.literal("Cooldown in Sekunden"));
        alertCooldownInput.setMaxLength(6);
        alertCooldownInput.setValue(alertCooldown);
        alertCooldownInput.setResponder(value -> {
            alertCooldown = value; alertEditorDirty = true;
            if (footerSaveButton != null) footerSaveButton.active = true;
        });
        addRenderableWidget(alertCooldownInput);
        addControl(x, y + 283, w, 23, () -> "Regel: " + (alertDraft.enabled ? "aktiv" : "pausiert"),
            () -> alertDraft.enabled, () -> "Individuelle Aktivierung dieser Alarmregel.", false,
            () -> { alertDraft.enabled = !alertDraft.enabled; alertEditorDirty = true; rebuildWidgets(); });
        contentEnd = y + 306;
    }

    private boolean stageAlertDraft() {
        if (alertEditingId == null && cfg.priceAlertRules.size() >= 200) {
            setStatus("Maximal 200 Alarmregeln möglich."); return false;
        }
        double threshold;
        int cooldown;
        try { threshold = Double.parseDouble(alertThreshold.trim().replace(',', '.')); cooldown = Integer.parseInt(alertCooldown.trim()); }
        catch (NumberFormatException e) { setStatus("Bitte gültigen Schwellenwert und Cooldown eingeben."); return false; }
        PriceAlertRule valid = PriceAlertInputValidator.create(alertDraft.itemKey, alertDraft.condition, threshold,
            alertDraft.enabled, alertDraft.repeat, cooldown, alertDraft.rearmOnExit, alertDraft.notificationChannel());
        if (valid == null || (alertEditingId == null && !marketCache.snapshot().containsKey(valid.itemKey))) {
            setStatus("Ungültige Regel oder Item nicht im lokalen Markt-Cache."); return false;
        }
        if (alertEditingId != null) {
            PriceAlertRule old = cfg.priceAlertRules.stream().filter(r -> Objects.equals(r.id, alertEditingId)).findFirst().orElse(null);
            if (old == null) { setStatus("Regel wurde inzwischen entfernt."); return false; }
            valid.id = old.id;
            boolean definitionChanged = !Objects.equals(old.itemKey, valid.itemKey)
                || !Objects.equals(old.condition, valid.condition) || old.threshold != valid.threshold
                || old.repeat != valid.repeat || old.cooldownSeconds != valid.cooldownSeconds
                || old.rearmOnExit != valid.rearmOnExit || !Objects.equals(old.notificationChannel(), valid.notificationChannel());
            if (!definitionChanged && (!valid.enabled || old.enabled)) {
                valid.lastTriggeredAtMs = old.lastTriggeredAtMs;
                valid.triggered = old.triggered;
                valid.latched = old.latched;
            }
            cfg.priceAlertRules.set(cfg.priceAlertRules.indexOf(old), valid);
        } else cfg.priceAlertRules.add(valid);
        alertDraft = null;
        alertEditorDirty = false;
        pageScroll = 0;
        setStatus("Alarmänderung vorgemerkt · Speichern übernimmt die Regel dauerhaft.");
        rebuildWidgets();
        return true;
    }

    private void buildProtection(int x, int y, int w) {
        int row = 27;
        int start = y + 50;
        addControl(x, start, w, 25, () -> "Observer-Modus  ·  " + (cfg.observerModeOnly ? "AN" : "AUS"),
            () -> cfg.observerModeOnly, () -> "Schaltet lokale Eingriffe aus. Cache-Ansichten bleiben nutzbar.",
            cfg.observerModeOnly, () -> updateConfig(() -> cfg.observerModeOnly = !cfg.observerModeOnly));
        addToggle(x, start + row, w, "Permanentes HUD", () -> cfg.showHud, value -> cfg.showHud = value,
            "Job- und Inventaranzeige. Alarmmeldungen haben ihren eigenen Kanal.", true);
        addToggle(x, start + row * 2, w, "Container-Preisanzeige", () -> cfg.showContainerOverlay,
            value -> cfg.showContainerOverlay = value, "Gesamtwertanzeige in geöffneten Containern.", true);
        addToggle(x, start + row * 3, w, "Schnellzugriff-Buttons", () -> cfg.showQuickButtons,
            value -> cfg.showQuickButtons = value, "Zusatzbuttons in unterstützten Inventaren.", true);
        addToggle(x, start + row * 4, w, "Shulker-Rekursion", () -> cfg.shulkerRecursion,
            value -> cfg.shulkerRecursion = value, "Verschachtelte Shulker-Inhalte bei Inventarwerten berücksichtigen.", true);
        addToggle(x, start + row * 5, w, "Rename-Schutz", () -> cfg.enableRenameProtection,
            value -> cfg.enableRenameProtection = value, "Bestätigung vor /rename.", true);
        addToggle(x, start + row * 6, w, "Sign-Schutz", () -> cfg.enableSignProtection,
            value -> cfg.enableSignProtection = value, "Bestätigung vor /sign.", true);
        addToggle(x, start + row * 7, w, "Offhand-Blocker", () -> cfg.enableOffhandBlocker,
            value -> cfg.enableOffhandBlocker = value, "Blockiert den Offhand-Tastendruck.", true);
        addToggle(x, start + row * 8, w, "Inventarwarnung", () -> cfg.enableInventoryWarning,
            value -> cfg.enableInventoryWarning = value, "Lokale Warnung bei vollem Inventar.", true);
        addToggle(x, start + row * 9, w, "Job-Tracker", () -> cfg.enableJobTracker,
            value -> cfg.enableJobTracker = value, "Liest passende sichtbare Chatmeldungen clientseitig aus.", true);
        addToggle(x, start + row * 10, w, "Command-Kurzformen", () -> cfg.enableCommandShortforms,
            value -> cfg.enableCommandShortforms = value, "Wandelt z. B. 1k vor dem Senden in 1000 um.", true);
        addToggle(x, start + row * 11, w, "Amboss-Normalisierung", () -> cfg.enableAnvilNormalization,
            value -> cfg.enableAnvilNormalization = value, "Erweitert Kurzformen im Amboss-Eingabefeld.", true);
        contentEnd = start + row * 12;
    }

    private void buildSystem(int x, int y, int w) {
        if (!systemActionsView) {
            addControl(x, y + 161, w, 23, () -> "Systemaktionen öffnen …", () -> false,
                () -> "Manueller Refresh und vorhandene lokale Einstellungsseiten.", true,
                () -> { systemActionsView = true; pageScroll = 0; pageScrollTarget = 0; rebuildWidgets(); });
            contentEnd = y + 186;
            return;
        }
        int row = 29;
        int start = y + 72;
        addControl(x, start, w, 24, () -> "Jetzt Markt- und Händlerdaten aktualisieren",
            () -> false, () -> "Nur diese ausdrücklich gewählte Aktion löst einen externen API-Abruf aus.", true, () -> {
                mod.getMarketSyncService().refresh();
                mod.getMerchantSyncService().refresh();
                setStatus("Aktualisierung angefragt. Cache-Zeitstempel ändern sich nach erfolgreicher Antwort.");
            });
        addControl(x, start + row, w, 23, () -> "Visotaris-Menüshortcut in MaLiLib verwalten …", () -> false,
            () -> "Öffnet MaLiLibs Hotkey-Editor nur mit der Visotaris-Menübelegung. Leer lassen deaktiviert den Shortcut.", false,
            systems.diath.visotaris_opmod.services.VisotarisMenuHotkey::openConfigScreen);
        addControl(x, start + row * 2, w, 23, () -> "Web & Netzwerk konfigurieren …", () -> false,
            () -> "Lokale Minecraft-Einstellungsseite. Zugangsdaten erscheinen nicht in Statuskarten.", false,
            () -> openSubscreen(new systems.diath.visotaris_opmod.config.NetworkSettingsScreen(this, cfg, this::saveSubscreenDraft)));
        addControl(x, start + row * 3, w, 23, () -> "Discord und Screenshot-Ziele …", () -> false,
            () -> "Lokale Minecraft-Einstellungsseite für Discord.", false,
            () -> openSubscreen(new systems.diath.visotaris_opmod.config.DiscordScreenshotSettingsScreen(this, cfg, this::saveSubscreenDraft)));
        addControl(x, start + row * 4, w, 23, () -> "Zurück zum Systemstatus", () -> false,
            () -> "Statuskarten und Cache-Daten anzeigen.", false,
            () -> { systemActionsView = false; rebuildWidgets(); });
        contentEnd = start + row * 4 + 25;
    }

    private void refreshMarketRows() {
        marketRows.clear();
        categories.clear();
        categories.addAll(marketCache.snapshot().values().stream()
            .map(MarketPrice::getCategory).filter(Objects::nonNull).map(String::trim).filter(s -> !s.isEmpty())
            .distinct().sorted(String.CASE_INSENSITIVE_ORDER).toList());
        String category = selectedCategoryName();
        String query = marketQuery.toLowerCase(Locale.ROOT).trim();
        for (MarketPrice price : marketCache.snapshot().values()) {
            if (selectedMarketCategory > 0 && !Objects.equals(category, price.getCategory() == null ? null : price.getCategory().trim())) continue;
            if (!query.isEmpty()) {
                String key = price.getItemKey().toLowerCase(Locale.ROOT);
                String display = itemName(price.getItemKey()).toLowerCase(Locale.ROOT);
                if (!key.contains(query) && !display.contains(query)) continue;
            }
            marketRows.add(price);
        }
        Comparator<MarketPrice> comparator = switch (marketSort) {
            case 1 -> Comparator.comparingDouble(MarketPrice::getBuy);
            case 2 -> Comparator.comparingDouble(MarketPrice::getSell);
            case 3 -> Comparator.comparingInt(MarketPrice::getBuyOrders);
            case 4 -> Comparator.comparingInt(MarketPrice::getSellOrders);
            case 5 -> Comparator.comparingInt(p -> p.getBuyOrders() + p.getSellOrders());
            default -> Comparator.comparing(p -> itemName(p.getItemKey()), String.CASE_INSENSITIVE_ORDER);
        };
        if (marketSortDescending) comparator = comparator.reversed();
        // Missing prices stay last in both directions; they are never treated as zero.
        if (marketSort == 1) comparator = Comparator.comparing((MarketPrice p) -> p.getBuy() <= 0 || !Double.isFinite(p.getBuy())).thenComparing(comparator);
        if (marketSort == 2) comparator = Comparator.comparing((MarketPrice p) -> p.getSell() <= 0 || !Double.isFinite(p.getSell())).thenComparing(comparator);
        marketRows.sort(comparator.thenComparing(MarketPrice::getItemKey));
        int maxScroll = Math.max(0, marketRows.size() - visibleMarketRows);
        marketScrollTarget = clamp(marketScrollTarget, 0, maxScroll);
        marketScrollPosition = clamp(marketScrollPosition, 0, maxScroll);
        marketScroll = (int) Math.floor(marketScrollPosition);
    }

    private void refreshAuctionRows() {
        auctionRows.clear();
        String query = auctionQuery.toLowerCase(Locale.ROOT).trim();
        for (Auction auction : auctionCache.snapshot().values()) {
            if (!auctionCategory.isEmpty() && !auctionCategory.equalsIgnoreCase(auction.category())) continue;
            String itemName = auction.item() == null || auction.item().displayName() == null ? "" : auction.item().displayName();
            String material = auction.item() == null || auction.item().material() == null ? "" : auction.item().material();
            if (!query.isEmpty() && !itemName.toLowerCase(Locale.ROOT).contains(query)
                && !material.toLowerCase(Locale.ROOT).contains(query) && !String.valueOf(auction.category()).toLowerCase(Locale.ROOT).contains(query)) continue;
            auctionRows.add(auction);
        }
        Comparator<Auction> comparator = switch (auctionSort) {
            case 1 -> Comparator.comparingDouble(Auction::currentBid);
            case 2 -> Comparator.comparing(a -> a.instantBuyPrice() == null ? Double.POSITIVE_INFINITY : a.instantBuyPrice());
            case 3 -> Comparator.comparing(a -> a.endTime() == null ? java.time.Instant.MAX : a.endTime());
            default -> Comparator.comparing(a -> a.item() == null || a.item().displayName() == null ? "" : a.item().displayName(), String.CASE_INSENSITIVE_ORDER);
        };
        if (auctionSortDescending) comparator = comparator.reversed();
        auctionRows.sort(comparator);
    }

    private String auctionCategoryLabel() {
        if (auctionCategory.isEmpty()) return "Alle";
        return auctionCache.categories().stream().filter(c -> auctionCategory.equals(c.name())).map(c -> c.displayName()).findFirst().orElse(auctionCategory);
    }
    private void cycleAuctionCategory() {
        List<String> values = new ArrayList<>(); values.add(""); auctionCache.categories().forEach(c -> values.add(c.name()));
        int index = values.indexOf(auctionCategory); auctionCategory = values.get((index + 1) % values.size());
    }
    private String auctionSortLabel() { return switch (auctionSort) { case 1 -> "Gebot"; case 2 -> "Sofortkauf"; case 3 -> "Ablauf"; default -> "Itemname"; }; }

    private void refreshShardRows() {
        shardRows.clear();
        String target = shardTargetFilter();
        String query = shardQuery.toLowerCase(Locale.ROOT).trim();
        for (ShardRate rate : shardCache.snapshot().values()) {
            if (target != null && !target.equalsIgnoreCase(rate.getTarget())) continue;
            if (!query.isEmpty() && !rate.getSource().toLowerCase(Locale.ROOT).contains(query)
                && !shardName(rate).toLowerCase(Locale.ROOT).contains(query)) continue;
            shardRows.add(rate);
        }
        Comparator<ShardRate> comparator = switch (shardSort) {
            case 1 -> Comparator.comparingDouble(ShardRate::getExchangeRate);
            case 2 -> Comparator.comparingDouble(ShardRate::getBase);
            case 3 -> Comparator.comparingDouble(r -> r.getBase() > 0
                ? (r.getExchangeRate() - r.getBase()) / r.getBase() : Double.NaN);
            default -> Comparator.comparing(this::shardName, String.CASE_INSENSITIVE_ORDER);
        };
        if (shardSortDescending) comparator = comparator.reversed();
        if (shardSort == 2) comparator = Comparator.comparing((ShardRate r) -> r.getBase() <= 0 || !Double.isFinite(r.getBase())).thenComparing(comparator);
        if (shardSort == 3) comparator = Comparator.comparing((ShardRate r) -> r.getBase() <= 0 || !Double.isFinite(r.getBase())).thenComparing(comparator);
        shardRows.sort(comparator.thenComparing(ShardRate::getSource));
        int maxScroll = Math.max(0, shardRows.size() - visibleShardRows);
        shardScrollTarget = clamp(shardScrollTarget, 0, maxScroll);
        shardScrollPosition = clamp(shardScrollPosition, 0, maxScroll);
        shardScroll = (int) Math.floor(shardScrollPosition);
    }

    private String selectedCategoryName() {
        if (selectedMarketCategory <= 0 || categories.isEmpty()) return "Alle";
        return categories.get(Math.min(selectedMarketCategory - 1, categories.size() - 1));
    }

    private String shardTargetFilter() {
        List<String> targets = shardTargets();
        return selectedShardTarget <= 0 || targets.isEmpty() ? null : targets.get(Math.min(selectedShardTarget - 1, targets.size() - 1));
    }

    private String shardTargetFilterLabel() {
        String target = shardTargetFilter();
        return target == null ? "Alle" : currencyLabel(target);
    }

    private int shardTargetCount() { return shardTargets().size() + 1; }

    private List<String> shardTargets() {
        return shardCache.snapshot().values().stream().map(ShardRate::getTarget).filter(Objects::nonNull)
            .map(String::trim).filter(s -> !s.isEmpty()).distinct().sorted(String.CASE_INSENSITIVE_ORDER).toList();
    }

    private String marketSummary() {
        return marketRows.size() + " Treffer · " + categories.size() + " Kategorien · Cache "
            + ageText(marketCache.getLastUpdatedMs());
    }

    private String activeOrdersSummary() {
        long activeBuy = 0, activeSell = 0;
        for (MarketPrice p : marketRows) { activeBuy += Math.max(0, p.getBuyOrders()); activeSell += Math.max(0, p.getSellOrders()); }
        return activeBuy + " Kauf / " + activeSell + " Verkaufsaufträge aktiv";
    }

    private int activeAlertCount() {
        if (!cfg.priceAlertsEnabled) return 0;
        return (int) cfg.priceAlertRules.stream().filter(r -> r.enabled).count();
    }

    private long latestAlertTimestamp() {
        PriceAlertService service = mod.getPriceAlertService();
        if (service == null || service.events().isEmpty()) return 0L;
        return service.events().get(0).timestampMs();
    }

    private String latestAlertLine() {
        PriceAlertService service = mod.getPriceAlertService();
        if (service == null || service.events().isEmpty()) return "Noch kein Alarm ausgelöst";
        PriceAlertService.Event event = service.events().get(0);
        return itemName(event.itemKey()) + " · " + conditionText(event.condition()) + " " + formatMoney(event.currentValue());
    }

    private String alertState(PriceAlertRule rule) {
        if (!cfg.priceAlertsEnabled) return "Global pausiert";
        if (!rule.enabled) return "Pausiert";
        MarketPrice price = marketCache.get(rule.itemKey).orElse(null);
        if (price == null) return "Wartet auf Marktdaten";
        double value = PriceAlertEngine.value(rule.condition, price);
        if (!Double.isFinite(value)) return "Wert nicht verfügbar";
        boolean matched = PriceAlertEngine.matches(rule.condition, value, rule.threshold);
        if (rule.repeat && rule.lastTriggeredAtMs > 0) {
            long left = rule.cooldownSeconds - (System.currentTimeMillis() - rule.lastTriggeredAtMs) / 1000;
            if (left > 0) return "Cooldown " + left + " s";
        }
        if (matched && rule.triggered) return "Ausgelöst · Schwelle aktiv";
        if (matched) return "Schwelle aktuell erfüllt";
        return rule.triggered && !rule.repeat ? "Einmalig ausgelöst" : "Beobachtet";
    }

    private String currentAlertValue(PriceAlertRule rule) {
        MarketPrice price = marketCache.get(rule.itemKey).orElse(null);
        if (price == null) return "keine Daten";
        double value = PriceAlertEngine.value(rule.condition, price);
        return Double.isFinite(value) ? formatMoney(value) : "nicht verfügbar";
    }

    private String itemName(String key) {
        if (key == null) return "Unbekanntes Item";
        return names.computeIfAbsent(key, ItemNameResolver::resolve);
    }

    private String shardName(ShardRate rate) {
        if (rate.getDisplayName() != null && !rate.getDisplayName().isBlank()) return rate.getDisplayName();
        String source = rate.getSource();
        String localized = itemName(source);
        return localized;
    }

    private static String currencyLabel(String target) {
        MerchantCurrency currency = MerchantCurrency.fromApiTarget(target);
        return currency == MerchantCurrency.UNKNOWN ? target : currency.getDisplayLabel();
    }

    private static String channelText(String channel) {
        return switch (channel) {
            case "HUD" -> "Minecraft-HUD";
            case "WEB" -> "Web-UI (solange offen)";
            case "HUD_WEB" -> "Minecraft-HUD und Web-UI";
            default -> "Minecraft-HUD";
        };
    }

    private static String conditionText(String condition) {
        return PriceAlertService.conditionLabel(condition);
    }

    private static String conditionCompact(String condition) {
        return switch (condition) {
            case "BUY_ABOVE" -> "Kauf >";
            case "BUY_BELOW" -> "Kauf <";
            case "SELL_ABOVE" -> "Verkauf >";
            case "SELL_BELOW" -> "Verkauf <";
            case "SPREAD_ABOVE" -> "Spanne >";
            case "SPREAD_BELOW" -> "Spanne <";
            default -> condition;
        };
    }

    private static String cacheState(long updatedAt, long age, int interval) {
        if (updatedAt == 0 || age == Long.MAX_VALUE) return "Noch nie synchronisiert";
        if (age > Math.max(60, interval) * 2L) return "Veraltet · " + compactAge(updatedAt);
        if (age > Math.max(60, interval)) return "Überfällig · " + compactAge(updatedAt);
        return "Aktuell · " + compactAge(updatedAt);
    }

    private static String compactAge(long updatedAt) {
        if (updatedAt <= 0) return "nie";
        long seconds = Math.max(0, (System.currentTimeMillis() - updatedAt) / 1000);
        if (seconds < 60) return seconds + " s";
        if (seconds < 3600) return seconds / 60 + " Min.";
        return seconds / 3600 + " Std.";
    }

    private static String ageText(long updatedAt) {
        if (updatedAt <= 0) return "noch nie synchronisiert";
        long seconds = Math.max(0, (System.currentTimeMillis() - updatedAt) / 1000);
        if (seconds < 60) return "vor " + seconds + " s";
        if (seconds < 3600) return "vor " + seconds / 60 + " Min.";
        return "vor " + seconds / 3600 + " Std.";
    }

    private static String ageTextSeconds(int seconds) { return seconds < 3600 ? (seconds / 60) + " Min." : (seconds / 3600) + " Std."; }

    private static String formatMoney(double value) {
        if (!Double.isFinite(value)) return "–";
        return String.format(Locale.GERMANY, "%,.2f", value);
    }

    private static String marketPriceText(double value) {
        return value > 0 && Double.isFinite(value) ? formatMoney(value) : "–";
    }

    private static String formatRate(double value) {
        if (!Double.isFinite(value)) return "–";
        return String.format(Locale.GERMANY, "%,.2f", value);
    }

    private static int hintColor(String hint) {
        if (hint.contains("veraltet") || hint.contains("nie")) return YELLOW;
        return MUTED;
    }

    private void setStatus(String status) {
        transientStatus = status;
        transientStatusAt = System.currentTimeMillis();
    }

    private void drawSectionTitle(IngameUxCanvas c, String title, int x, int y) {
        c.text(fit(c, title, window.contentWidth() - 30), x, y, WHITE, true);
        int lineY = y + c.lineHeight() + 5;
        c.fill(x, lineY, window.x() + window.width() - 12, lineY + 1, LINE);
        c.fill(x, lineY, Math.min(window.x() + window.width() - 12, x + c.textWidth(title)), lineY + 2, ICE);
    }

    private void drawDataLine(IngameUxCanvas c, int x, int y, int w, String label, String value, int color) {
        card(c, x, y, w, 22);
        int labelW = Math.min(Math.max(56, c.textWidth(label) + 4), Math.max(56, w / 2));
        c.text(fit(c, label, labelW), x + 7, y + 6, MUTED, false);
        c.text(fit(c, value, w - labelW - 20), x + labelW + 12, y + 6, color, false);
    }

    private void drawEmptyState(IngameUxCanvas c, int x, int y, int w, String title, String detail) {
        card(c, x, y, w, 64);
        c.text(fit(c, title, w - 18), x + 9, y + 10, WHITE, true);
        c.text(fit(c, detail, w - 18), x + 9, y + 30, MUTED, false);
    }

    private void drawInfoBox(IngameUxCanvas c, int x, int y, int w, int h, String text, int color) {
        card(c, x, y, w, h);
        c.fill(x, y, x + 2, y + h, color);
        drawWrapped(c, text, x + 8, y + 5, w - 16, color);
    }

    private void drawWrapped(IngameUxCanvas c, String value, int x, int y, int maxWidth, int color) {
        StringBuilder line = new StringBuilder();
        int row = 0;
        for (String word : value.split(" ")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (c.textWidth(candidate) > maxWidth && !line.isEmpty()) {
                c.text(line.toString(), x, y + row++ * c.lineHeight(), color, false);
                line = new StringBuilder(word);
                if (row >= 3) break;
            } else line = new StringBuilder(candidate);
        }
        if (!line.isEmpty() && row < 3) c.text(line.toString(), x, y + row * c.lineHeight(), color, false);
    }

    private void drawTooltip(IngameUxCanvas c, String value, int mouseX, int mouseY) {
        int maxWidth = Math.min(310, Math.max(120, width - 20));
        List<String> lines = wrapTooltip(c, value, maxWidth - 12);
        int boxW = Math.min(maxWidth, lines.stream().mapToInt(c::textWidth).max().orElse(0) + 12);
        int boxH = lines.size() * c.lineHeight() + 10;
        int x = clamp(mouseX + 12, 5, width - boxW - 5);
        int y = mouseY + boxH + 12 > height ? Math.max(5, mouseY - boxH - 10) : mouseY + 10;
        c.fill(x - 1, y - 1, x + boxW + 1, y + boxH + 1, 0xFF090E13);
        c.fill(x, y, x + boxW, y + boxH, 0xF01B2A36);
        c.fill(x, y, x + boxW, y + 1, ICE);
        for (int i = 0; i < lines.size(); i++) c.text(lines.get(i), x + 6, y + 5 + i * c.lineHeight(), WHITE, true);
    }

    private static List<String> wrapTooltip(IngameUxCanvas c, String value, int maxWidth) {
        List<String> lines = new ArrayList<>();
        for (String paragraph : value.split("\\n")) {
            StringBuilder line = new StringBuilder();
            for (String word : paragraph.split(" ")) {
                String candidate = line.isEmpty() ? word : line + " " + word;
                if (c.textWidth(candidate) > maxWidth && !line.isEmpty()) {
                    lines.add(line.toString());
                    line = new StringBuilder(word);
                } else line = new StringBuilder(candidate);
            }
            if (!line.isEmpty()) lines.add(line.toString());
        }
        return lines;
    }

    private static void card(IngameUxCanvas c, int x, int y, int w, int h) {
        c.fill(x, y, x + w, y + h, CARD);
        c.fill(x, y, x + w, y + 1, LINE);
        c.fill(x, y + h - 1, x + w, y + h, LINE);
        c.fill(x, y, x + 1, y + h, LINE);
        c.fill(x + w - 1, y, x + w, y + h, LINE);
    }

    private static void outline(IngameUxCanvas c, int x, int y, int w, int h, int color) {
        c.fill(x, y, x + w, y + 1, color);
        c.fill(x, y + h - 1, x + w, y + h, color);
        c.fill(x, y, x + 1, y + h, color);
        c.fill(x + w - 1, y, x + w, y + h, color);
    }

    private static String fit(IngameUxCanvas c, String value, int maxWidth) {
        if (value == null) return "";
        if (maxWidth < 1) return "";
        if (c.textWidth(value) <= maxWidth) return value;
        String result = value;
        while (!result.isEmpty() && c.textWidth(result + "…") > maxWidth) result = result.substring(0, result.length() - 1);
        return result.isEmpty() ? "" : result + "…";
    }

    private static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }
    private static double clamp(double value, double min, double max) { return Math.max(min, Math.min(max, value)); }
}
