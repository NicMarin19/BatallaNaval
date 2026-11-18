package com.batallanaval.ui;

import com.batallanaval.audio.SoundManager;
import com.batallanaval.model.GameSaveData;
import com.batallanaval.model.GameSaveManager;
import com.batallanaval.view.TextureGenerator;
import com.jme3.asset.AssetManager;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Texture;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Pantalla modal que examina la carpeta 'partidas' y despliega todas
 * las partidas disponibles para jugar o reanudar.
 */
public class SavedGamesModal extends Node {

    private static final float MODAL_W = 860f;
    private static final float MODAL_H = 550f;
    private static final int ITEMS_PER_PAGE = 4;

    private final float screenWidth;
    private final float screenHeight;
    private final AssetManager assetManager;
    private final BitmapFont font;

    private final List<GuiButton> buttons = new ArrayList<>();
    private final Node listContainer = new Node("SavedGamesListContainer");

    private List<GameSaveData> savesList = new ArrayList<>();
    private int currentPage = 0;

    private Consumer<GameSaveData> onLoadGameCallback;
    private Runnable onCloseCallback;

    private BitmapText titleText;
    private BitmapText folderInfoText;
    private BitmapText pageText;
    private BitmapText emptyNoticeText;

    private GuiButton btnPrev;
    private GuiButton btnNext;
    private GuiButton btnRefresh;
    private GuiButton btnClose;

    private float pulseTime = 0f;

    public SavedGamesModal(float screenWidth, float screenHeight, AssetManager assetManager) {
        super("SavedGamesModal");
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.assetManager = assetManager;
        this.font = assetManager.loadFont("Interface/Fonts/Default.fnt");

        build();
        setQueueBucket(RenderQueue.Bucket.Gui);
        hide();
    }

    private void build() {
        float x = (screenWidth - MODAL_W) * 0.5f;
        float y = (screenHeight - MODAL_H) * 0.5f;
        float top = y + MODAL_H;

        // Velo oscuro modal
        Quad veilQ = new Quad(screenWidth, screenHeight);
        Geometry veil = new Geometry("SaveModalVeil", veilQ);
        Material matVeil = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        matVeil.setColor("Color", new ColorRGBA(0.01f, 0.02f, 0.05f, 0.85f));
        matVeil.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        veil.setMaterial(matVeil);
        attachChild(veil);

        // Panel principal
        Quad panelQ = new Quad(MODAL_W, MODAL_H);
        Geometry panelBg = new Geometry("SaveModalPanelBg", panelQ);
        Material matPanel = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        Texture panelTex = TextureGenerator.createButtonTexture((int) MODAL_W, (int) MODAL_H,
                new ColorRGBA(0.025f, 0.055f, 0.10f, 0.98f),
                new ColorRGBA(0.25f, 0.72f, 1.0f, 0.95f));
        matPanel.setTexture("ColorMap", panelTex);
        matPanel.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        panelBg.setMaterial(matPanel);
        panelBg.setLocalTranslation(x, y, 1f);
        attachChild(panelBg);

        // Cabecera
        titleText = UiKit.label(assetManager, font, "CARPETA DE PARTIDAS GUARDADAS",
                1.40f, UiKit.ACCENT, x + 30f, top - 26f, 2f);
        attachChild(titleText);

        folderInfoText = UiKit.label(assetManager, font,
                "Directorio: /" + GameSaveManager.SAVES_FOLDER_NAME + "  |  Selecciona una partida para jugar",
                0.62f, UiKit.TEXT_DIM, x + 30f, top - 52f, 2f);
        attachChild(folderInfoText);

        attachChild(UiKit.hLine(assetManager, MODAL_W - 60f,
                new ColorRGBA(0.24f, 0.62f, 0.95f, 0.55f), x + 30f, top - 64f, 2f));

        // Contenedor de la lista de partidas
        attachChild(listContainer);

        // Texto informativo de carpeta vacía
        emptyNoticeText = UiKit.labelCentered(assetManager, font,
                "No hay partidas guardadas en la carpeta 'partidas'.\nInicia una nueva partida y guárdala desde el menú de pausa ([ESC] -> GUARDAR PARTIDA).",
                0.80f, new ColorRGBA(0.65f, 0.75f, 0.85f, 0.90f),
                screenWidth * 0.5f, y + MODAL_H * 0.52f, 2f);
        emptyNoticeText.setCullHint(CullHint.Always);
        attachChild(emptyNoticeText);

        // Barra inferior: paginación y acciones
        float botY = y + 20f;

        btnPrev = new GuiButton("savesPrev", "< ANTERIOR", 130f, 38f, assetManager, font,
                UiKit.BTN_BG, UiKit.BTN_BORDER, this::prevPage);
        btnPrev.setLocalTranslation(x + 30f, botY, 2f);
        attachChild(btnPrev);
        buttons.add(btnPrev);

        pageText = UiKit.labelCentered(assetManager, font, "Página 1 de 1", 0.72f,
                new ColorRGBA(0.8f, 0.9f, 1f, 1f), x + 230f, botY + 22f, 2f);
        attachChild(pageText);

        btnNext = new GuiButton("savesNext", "SIGUIENTE >", 130f, 38f, assetManager, font,
                UiKit.BTN_BG, UiKit.BTN_BORDER, this::nextPage);
        btnNext.setLocalTranslation(x + 300f, botY, 2f);
        attachChild(btnNext);
        buttons.add(btnNext);

        btnRefresh = new GuiButton("savesRefresh", "ACTUALIZAR", 150f, 38f, assetManager, font,
                UiKit.BTN_BG, UiKit.BTN_BORDER, this::refreshSaves);
        btnRefresh.setLocalTranslation(x + MODAL_W - 350f, botY, 2f);
        attachChild(btnRefresh);
        buttons.add(btnRefresh);

        btnClose = new GuiButton("savesClose", "CERRAR", 140f, 38f, assetManager, font,
                UiKit.BTN_RED_BG, UiKit.BTN_RED_BD, this::onCloseClicked);
        btnClose.setLocalTranslation(x + MODAL_W - 170f, botY, 2f);
        attachChild(btnClose);
        buttons.add(btnClose);
    }

    public void refreshSaves() {
        this.savesList = GameSaveManager.listSaves();
        int totalPages = Math.max(1, (int) Math.ceil((double) savesList.size() / ITEMS_PER_PAGE));
        if (currentPage >= totalPages) {
            currentPage = totalPages - 1;
        }
        if (currentPage < 0) {
            currentPage = 0;
        }
        renderCurrentPage();
    }

    private void renderCurrentPage() {
        // Limpiar botones de slots anteriores de la lista activa
        listContainer.detachAllChildren();
        buttons.removeIf(b -> b.getId().startsWith("slotPlay_") || b.getId().startsWith("slotDel_"));

        int totalSaves = savesList.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalSaves / ITEMS_PER_PAGE));

        folderInfoText.setText(String.format("Directorio: /%s  |  %d partida(s) disponible(s) para jugar",
                GameSaveManager.SAVES_FOLDER_NAME, totalSaves));
        pageText.setText(String.format("Página %d de %d", currentPage + 1, totalPages));

        btnPrev.setEnabled(currentPage > 0);
        btnNext.setEnabled(currentPage < totalPages - 1);

        if (totalSaves == 0) {
            emptyNoticeText.setCullHint(CullHint.Never);
            return;
        } else {
            emptyNoticeText.setCullHint(CullHint.Always);
        }

        float panelX = (screenWidth - MODAL_W) * 0.5f;
        float panelY = (screenHeight - MODAL_H) * 0.5f;
        float slotW = MODAL_W - 60f;
        float slotH = 82f;
        float startY = panelY + MODAL_H - 80f;

        int startIndex = currentPage * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, totalSaves);

        for (int i = startIndex; i < endIndex; i++) {
            GameSaveData save = savesList.get(i);
            int slotIndex = i - startIndex;
            float slotY = startY - (slotIndex + 1) * (slotH + 10f);

            // Fondo del slot
            Quad slotQ = new Quad(slotW, slotH);
            Geometry slotBg = new Geometry("SlotBg_" + i, slotQ);
            Material matSlot = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
            Texture slotTex = TextureGenerator.createButtonTexture((int) slotW, (int) slotH,
                    new ColorRGBA(0.04f, 0.09f, 0.16f, 0.95f),
                    new ColorRGBA(0.20f, 0.55f, 0.85f, 0.70f));
            matSlot.setTexture("ColorMap", slotTex);
            matSlot.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
            slotBg.setMaterial(matSlot);
            slotBg.setLocalTranslation(panelX + 30f, slotY, 2f);
            listContainer.attachChild(slotBg);

            // Barra lateral de color verde
            Geometry borderBar = UiKit.rect(assetManager, 4f, slotH,
                    new ColorRGBA(0.25f, 0.85f, 1.0f, 0.95f), panelX + 30f, slotY, 2.5f);
            listContainer.attachChild(borderBar);

            // Información de la partida
            String fileNameDisplay = save.fileName != null ? save.fileName : "Partida";
            BitmapText txtName = UiKit.label(assetManager, font, fileNameDisplay, 0.82f,
                    new ColorRGBA(0.40f, 0.90f, 1.0f, 1f), panelX + 44f, slotY + slotH - 14f, 2.8f);
            listContainer.attachChild(txtName);

            String dateDisplay = save.saveDate != null ? save.saveDate : "Fecha desconocida";
            BitmapText txtDate = UiKit.label(assetManager, font, "Guardada: " + dateDisplay, 0.58f,
                    UiKit.TEXT_DIM, panelX + 44f, slotY + slotH - 34f, 2.8f);
            listContainer.attachChild(txtDate);

            // Estadísticas de combate de esa partida
            int round = save.roundNumber;
            int playerAlive = save.getPlayerAliveCount();
            int enemySunk = save.getEnemySunkCount();
            float acc = save.getAccuracy();
            String stats = String.format("Ronda %d  |  Aliados: %d/5  |  Enemigos hundidos: %d/5  |  Precisión: %.1f%%  |  Tiempo: %s",
                    round, playerAlive, enemySunk, acc, UiKit.formatTime(save.elapsedSeconds));

            BitmapText txtStats = UiKit.label(assetManager, font, stats, 0.62f,
                    new ColorRGBA(0.85f, 0.92f, 1.0f, 1f), panelX + 44f, slotY + 20f, 2.8f);
            listContainer.attachChild(txtStats);

            // Botón [JUGAR / REANUDAR]
            float btnPlayW = 160f;
            float btnPlayH = 38f;
            float btnPlayX = panelX + slotW - 275f;
            float btnActionY = slotY + (slotH - btnPlayH) * 0.5f;

            final GameSaveData currentSave = save;
            GuiButton btnPlay = new GuiButton("slotPlay_" + i, "JUGAR", btnPlayW, btnPlayH,
                    assetManager, font, UiKit.BTN_GREEN_BG, UiKit.BTN_GREEN_BD,
                    () -> onPlaySave(currentSave));
            btnPlay.setLocalTranslation(btnPlayX, btnActionY, 3f);
            listContainer.attachChild(btnPlay);
            buttons.add(btnPlay);

            // Botón [ELIMINAR]
            float btnDelW = 100f;
            float btnDelH = 38f;
            float btnDelX = panelX + slotW - 100f;

            GuiButton btnDel = new GuiButton("slotDel_" + i, "BORRAR", btnDelW, btnDelH,
                    assetManager, font, UiKit.BTN_RED_BG, UiKit.BTN_RED_BD,
                    () -> onDeleteSave(currentSave));
            btnDel.setLocalTranslation(btnDelX, btnActionY, 3f);
            listContainer.attachChild(btnDel);
            buttons.add(btnDel);
        }
    }

    private void onPlaySave(GameSaveData save) {
        SoundManager.getInstance().play(SoundManager.SoundType.SONAR_PING);
        hide();
        if (onLoadGameCallback != null) {
            onLoadGameCallback.accept(save);
        }
    }

    private void onDeleteSave(GameSaveData save) {
        SoundManager.getInstance().play(SoundManager.SoundType.BUTTON_CLICK);
        if (save.fileName != null) {
            GameSaveManager.deleteSave(save.fileName);
        }
        refreshSaves();
    }

    private void prevPage() {
        if (currentPage > 0) {
            currentPage--;
            renderCurrentPage();
            SoundManager.getInstance().play(SoundManager.SoundType.BUTTON_CLICK);
        }
    }

    private void nextPage() {
        int totalPages = Math.max(1, (int) Math.ceil((double) savesList.size() / ITEMS_PER_PAGE));
        if (currentPage < totalPages - 1) {
            currentPage++;
            renderCurrentPage();
            SoundManager.getInstance().play(SoundManager.SoundType.BUTTON_CLICK);
        }
    }

    private void onCloseClicked() {
        hide();
        SoundManager.getInstance().play(SoundManager.SoundType.BUTTON_CLICK);
        if (onCloseCallback != null) {
            onCloseCallback.run();
        }
    }

    public void update(float tpf) {
        if (!isVisible()) return;
        pulseTime += tpf * 1.5f;
        float pulse = 0.90f + 0.10f * FastMath.sin(pulseTime);
        titleText.setColor(new ColorRGBA(0.30f * pulse, 0.85f * pulse, pulse, 1f));
    }

    public boolean handleMouseClick(float x, float y) {
        if (!isVisible()) return false;
        for (GuiButton btn : buttons) {
            if (btn.isEnabled() && btn.onClick(x, y)) {
                return true;
            }
        }
        return true; // Modal consume todos los clics
    }

    public void handleMouseMove(float x, float y) {
        if (!isVisible()) return;
        for (GuiButton btn : buttons) {
            btn.updateHover(x, y);
        }
    }

    public void show() {
        currentPage = 0;
        refreshSaves();
        setCullHint(CullHint.Inherit);
    }

    public void hide() {
        setCullHint(CullHint.Always);
    }

    public boolean isVisible() {
        return getCullHint() != CullHint.Always;
    }

    public void setOnLoadGame(Consumer<GameSaveData> callback) {
        this.onLoadGameCallback = callback;
    }

    public void setOnClose(Runnable callback) {
        this.onCloseCallback = callback;
    }
}
