package com.batallanaval.view;

import com.batallanaval.model.Board;
import com.batallanaval.model.CellState;
import com.batallanaval.model.Coordinate;
import com.batallanaval.model.Ship;
import com.batallanaval.model.ShipType;
import com.jme3.asset.AssetManager;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Cylinder;
import com.jme3.texture.Texture;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Board3DView extends Node {

    public static final float CELL_SIZE = 2.0f;
    public static final float BOARD_DIM = Board.SIZE * CELL_SIZE;

    private final String boardId;
    private final boolean isPlayerBoard;
    private final AssetManager assetManager;
    private final ShipMeshFactory shipFactory;

    private final Geometry[][] cellGeometries;
    private final Marker3D[][] markerNodes;
    private final Map<Ship, Node> shipNodes;
    private final Map<Ship, Vector3f> shipBasePos;
    private final Map<Ship, Quaternion> shipBaseRot;
    private final boolean[][] cellExploded;

    private final Material matNormalCell;
    private final Material matHoverCell;
    private final Material matExplodedBlackCell;
    private final Material matBorderPlatform;
    private final Material matHazardStripe;

    private final TacticalReticle3D tacticalReticle;
    private final BoardWaveOverlay3D boardWaves;
    private final List<SinkingShipAnimation> sinkingShips;
    private final List<FloatingShipComment3D> activeComments;

    private Node ghostShipNode;
    private HintGlow3D hintGlow;
    private int hintGlowX = -1;
    private int hintGlowY = -1;
    private int currentHoverX = -1;
    private int currentHoverY = -1;
    private float waveTime = 0f;
    private Camera activeCamera;

    public Board3DView(String boardId, boolean isPlayerBoard, AssetManager assetManager, ShipMeshFactory shipFactory) {
        super("Board3D_" + boardId);
        this.boardId = boardId;
        this.isPlayerBoard = isPlayerBoard;
        this.assetManager = assetManager;
        this.shipFactory = shipFactory;

        this.cellGeometries = new Geometry[Board.SIZE][Board.SIZE];
        this.markerNodes = new Marker3D[Board.SIZE][Board.SIZE];
        this.shipNodes = new HashMap<>();
        this.shipBasePos = new HashMap<>();
        this.shipBaseRot = new HashMap<>();
        this.cellExploded = new boolean[Board.SIZE][Board.SIZE];
        this.sinkingShips = new ArrayList<>();
        this.activeComments = new ArrayList<>();

        // Colors
        ColorRGBA borderColor = isPlayerBoard ? new ColorRGBA(0.15f, 0.65f, 0.95f, 0.95f) : new ColorRGBA(0.95f, 0.45f, 0.15f, 0.95f);
        ColorRGBA fillColor = isPlayerBoard ? new ColorRGBA(0.04f, 0.14f, 0.24f, 0.88f) : new ColorRGBA(0.12f, 0.14f, 0.18f, 0.88f);

        Texture normalTex = TextureGenerator.createGridCellTexture(64, borderColor, fillColor);
        this.matNormalCell = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        this.matNormalCell.setTexture("ColorMap", normalTex);
        this.matNormalCell.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);

        ColorRGBA hoverBorder = new ColorRGBA(1.0f, 0.95f, 0.15f, 1f);
        ColorRGBA hoverFill = isPlayerBoard ? new ColorRGBA(0.18f, 0.45f, 0.65f, 0.90f) : new ColorRGBA(0.55f, 0.22f, 0.12f, 0.90f);
        Texture hoverTex = TextureGenerator.createGridCellTexture(64, hoverBorder, hoverFill);
        this.matHoverCell = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        this.matHoverCell.setTexture("ColorMap", hoverTex);
        this.matHoverCell.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);

        // Casilla carbonizada / negra para explosiones e impactos
        Texture scorchedTex = TextureGenerator.createScorchedCellTexture(64);
        this.matExplodedBlackCell = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        this.matExplodedBlackCell.setTexture("ColorMap", scorchedTex);
        this.matExplodedBlackCell.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);

        // Platform steel armor material
        this.matBorderPlatform = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        this.matBorderPlatform.setBoolean("UseMaterialColors", true);
        ColorRGBA platCol = isPlayerBoard ? new ColorRGBA(0.16f, 0.20f, 0.25f, 1f) : new ColorRGBA(0.24f, 0.18f, 0.18f, 1f);
        this.matBorderPlatform.setColor("Diffuse", platCol);
        this.matBorderPlatform.setColor("Ambient", platCol.mult(0.5f));
        this.matBorderPlatform.setColor("Specular", ColorRGBA.White.mult(0.25f));
        this.matBorderPlatform.setFloat("Shininess", 16f);

        // Hazard stripes material
        Texture hazardTex = TextureGenerator.createHazardStripeTexture(128, 16);
        hazardTex.setWrap(Texture.WrapMode.Repeat);
        this.matHazardStripe = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        this.matHazardStripe.setTexture("ColorMap", hazardTex);

        // Tactical 3D Holographic Reticle
        this.tacticalReticle = new TacticalReticle3D(assetManager);
        attachChild(tacticalReticle);

        // Capa de oleaje suave sobre el sector del tablero
        this.boardWaves = new BoardWaveOverlay3D(assetManager, BOARD_DIM, BOARD_DIM);
        attachChild(boardWaves);

        buildPlatform();
        buildGrid();
        buildCoordinateLabels();
    }

    private void buildPlatform() {
        float borderMargin = 1.4f;
        float totalWidth = BOARD_DIM + borderMargin * 2;
        float totalDepth = BOARD_DIM + borderMargin * 2;
        float thickness = 0.7f;

        // Main Armored Offshore Platform Deck
        Box platformBox = new Box(totalWidth * 0.5f, thickness * 0.5f, totalDepth * 0.5f);
        Geometry platformGeom = new Geometry("Platform_" + boardId, platformBox);
        platformGeom.setMaterial(matBorderPlatform);
        platformGeom.setLocalTranslation(BOARD_DIM * 0.5f, -thickness * 0.5f - 0.02f, BOARD_DIM * 0.5f);
        attachChild(platformGeom);

        // Hazard Warning Borders along Top and Bottom Rims
        Box hazardRim = new Box(totalWidth * 0.5f, 0.04f, 0.12f);
        Geometry gHazTop = new Geometry("HazardTop", hazardRim);
        gHazTop.setMaterial(matHazardStripe);
        gHazTop.setLocalTranslation(BOARD_DIM * 0.5f, 0.01f, -0.65f);
        attachChild(gHazTop);

        Geometry gHazBot = new Geometry("HazardBot", hazardRim);
        gHazBot.setMaterial(matHazardStripe);
        gHazBot.setLocalTranslation(BOARD_DIM * 0.5f, 0.01f, BOARD_DIM + 0.65f);
        attachChild(gHazBot);

        // 4 Corner Offshore Mooring Pylons with Beacon Lights
        float[] cornerX = {-borderMargin * 0.5f, BOARD_DIM + borderMargin * 0.5f};
        float[] cornerZ = {-borderMargin * 0.5f, BOARD_DIM + borderMargin * 0.5f};

        Material matBeaconLight = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        ColorRGBA bCol = isPlayerBoard ? new ColorRGBA(0.2f, 0.8f, 1.0f, 1f) : new ColorRGBA(1.0f, 0.5f, 0.1f, 1f);
        matBeaconLight.setColor("Color", bCol);

        for (float cx : cornerX) {
            for (float cz : cornerZ) {
                Cylinder pylon = new Cylinder(8, 8, 0.28f, 1.2f, true);
                Geometry gPylon = new Geometry("Pylon", pylon);
                gPylon.setMaterial(matBorderPlatform);
                gPylon.setLocalRotation(new Quaternion().fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_X));
                gPylon.setLocalTranslation(cx, 0.35f, cz);
                attachChild(gPylon);

                Cylinder beaconRing = new Cylinder(8, 8, 0.32f, 0.12f, true);
                Geometry gBeacon = new Geometry("BeaconRing", beaconRing);
                gBeacon.setMaterial(matBeaconLight);
                gBeacon.setLocalRotation(new Quaternion().fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_X));
                gBeacon.setLocalTranslation(cx, 0.85f, cz);
                attachChild(gBeacon);
            }
        }
    }

    private void buildGrid() {
        Box cellBox = new Box(CELL_SIZE * 0.48f, 0.02f, CELL_SIZE * 0.48f);

        for (int y = 0; y < Board.SIZE; y++) {
            for (int x = 0; x < Board.SIZE; x++) {
                Geometry cellGeom = new Geometry("Cell_" + boardId + "_" + x + "_" + y, cellBox);
                cellGeom.setMaterial(matNormalCell);
                Vector3f pos = getCellCenter(x, y);
                cellGeom.setLocalTranslation(pos.x, 0.02f, pos.z);
                cellGeom.setUserData("gridX", x);
                cellGeom.setUserData("gridY", y);
                cellGeom.setUserData("boardId", boardId);
                attachChild(cellGeom);
                cellGeometries[y][x] = cellGeom;
            }
        }
    }

    private void buildCoordinateLabels() {
        BitmapFont font = assetManager.loadFont("Interface/Fonts/Default.fnt");

        // Column Numbers: 1 to 10 along Top
        for (int x = 0; x < Board.SIZE; x++) {
            BitmapText txt = new BitmapText(font, false);
            txt.setSize(0.85f);
            txt.setText(String.valueOf(x + 1));
            txt.setColor(isPlayerBoard ? new ColorRGBA(0.6f, 0.9f, 1.0f, 1f) : new ColorRGBA(1.0f, 0.8f, 0.5f, 1f));
            txt.setQueueBucket(RenderQueue.Bucket.Transparent);
            txt.setLocalRotation(new Quaternion().fromAngleAxis(-FastMath.HALF_PI, Vector3f.UNIT_X));
            txt.setLocalTranslation((x + 0.35f) * CELL_SIZE, 0.05f, -0.2f);
            attachChild(txt);
        }

        // Row Letters: A to J along Left
        for (int y = 0; y < Board.SIZE; y++) {
            BitmapText txt = new BitmapText(font, false);
            txt.setSize(0.85f);
            txt.setText(String.valueOf((char) ('A' + y)));
            txt.setColor(isPlayerBoard ? new ColorRGBA(0.6f, 0.9f, 1.0f, 1f) : new ColorRGBA(1.0f, 0.8f, 0.5f, 1f));
            txt.setQueueBucket(RenderQueue.Bucket.Transparent);
            txt.setLocalRotation(new Quaternion().fromAngleAxis(-FastMath.HALF_PI, Vector3f.UNIT_X));
            txt.setLocalTranslation(-0.75f, 0.05f, (y + 0.75f) * CELL_SIZE);
            attachChild(txt);
        }

        // Title banner on board
        BitmapText titleTxt = new BitmapText(font, false);
        titleTxt.setSize(1.05f);
        titleTxt.setText(isPlayerBoard ? "SECTOR DEFENSIVO // FLOTA ALIADA" : "RADAR TACTICO // OBJETIVOS ENEMIGOS");
        titleTxt.setColor(isPlayerBoard ? new ColorRGBA(0.35f, 0.9f, 1.0f, 1f) : new ColorRGBA(1.0f, 0.55f, 0.2f, 1f));
        titleTxt.setQueueBucket(RenderQueue.Bucket.Transparent);
        titleTxt.setLocalRotation(new Quaternion().fromAngleAxis(-FastMath.HALF_PI, Vector3f.UNIT_X));
        titleTxt.setLocalTranslation(CELL_SIZE * 0.8f, 0.05f, -0.95f);
        attachChild(titleTxt);
    }

    public Vector3f getCellCenter(int x, int y) {
        return new Vector3f((x + 0.5f) * CELL_SIZE, 0.05f, (y + 0.5f) * CELL_SIZE);
    }

    public Vector3f getWorldCellCenter(int x, int y) {
        Vector3f local = getCellCenter(x, y);
        return localToWorld(local, null);
    }

    public void setHoverCell(int x, int y) {
        if (currentHoverX == x && currentHoverY == y) return;

        // Restore previous cell
        if (currentHoverX >= 0 && currentHoverX < Board.SIZE && currentHoverY >= 0 && currentHoverY < Board.SIZE) {
            restoreCellMaterial(currentHoverX, currentHoverY);
        }

        currentHoverX = x;
        currentHoverY = y;

        if (x >= 0 && x < Board.SIZE && y >= 0 && y < Board.SIZE) {
            cellGeometries[y][x].setMaterial(matHoverCell);

            // Show 3D Holographic Reticle if enemy board
            if (!isPlayerBoard) {
                tacticalReticle.showAt(getCellCenter(x, y), true);
            }
        } else {
            tacticalReticle.hide();
        }
    }

    public void clearHover() {
        setHoverCell(-1, -1);
        tacticalReticle.hide();
    }

    private void restoreCellMaterial(int x, int y) {
        if (cellExploded[y][x]) {
            cellGeometries[y][x].setMaterial(matExplodedBlackCell);
        } else {
            cellGeometries[y][x].setMaterial(matNormalCell);
        }
    }

    /**
     * Apunta la mira directamente en la posición exacta 3D del puntero sobre el tablero,
     * iluminando a su vez la casilla sobre la cual se encuentra el cursor.
     */
    public void setCursorPointer(Vector3f worldContactPoint, int gridX, int gridY) {
        if (gridX != currentHoverX || gridY != currentHoverY) {
            if (currentHoverX >= 0 && currentHoverX < Board.SIZE && currentHoverY >= 0 && currentHoverY < Board.SIZE) {
                restoreCellMaterial(currentHoverX, currentHoverY);
            }
            currentHoverX = gridX;
            currentHoverY = gridY;
            if (gridX >= 0 && gridX < Board.SIZE && gridY >= 0 && gridY < Board.SIZE) {
                cellGeometries[gridY][gridX].setMaterial(matHoverCell);
            }
        }

        if (!isPlayerBoard && worldContactPoint != null) {
            Vector3f local = worldToLocal(worldContactPoint, null);
            local.y = 0.08f;
            // Delimitar dentro de la superficie del tablero
            local.x = Math.max(0.2f, Math.min(BOARD_DIM - 0.2f, local.x));
            local.z = Math.max(0.2f, Math.min(BOARD_DIM - 0.2f, local.z));
            tacticalReticle.showAt(local, true);
        }
    }

    /**
     * Marca una casilla en negro azabache carbonizado tras un impacto o explosión.
     */
    public void markCellExploded(int x, int y) {
        if (x < 0 || x >= Board.SIZE || y < 0 || y >= Board.SIZE) return;
        cellExploded[y][x] = true;
        cellGeometries[y][x].setMaterial(matExplodedBlackCell);
    }

    /**
     * Marca todas las casillas del barco destruido en negro carbonizado.
     */
    public void markShipExploded(Ship ship) {
        if (ship == null) return;
        for (Coordinate c : ship.getOccupiedCoordinates()) {
            markCellExploded(c.x, c.y);
        }
    }

    public void updateCellMarker(int x, int y, CellState state) {
        if (x < 0 || x >= Board.SIZE || y < 0 || y >= Board.SIZE) return;

        if (x == hintGlowX && y == hintGlowY) {
            clearHintGlow();
        }

        if (markerNodes[y][x] != null) {
            markerNodes[y][x].removeFromParent();
            markerNodes[y][x] = null;
        }

        if (state == CellState.MISS || state == CellState.HIT || state == CellState.SUNK) {
            Marker3D marker = new Marker3D(assetManager, state);
            Vector3f pos = getCellCenter(x, y);
            marker.setLocalTranslation(pos);
            attachChild(marker);
            markerNodes[y][x] = marker;

            // Si es un impacto o hundimiento, poner la casilla en negro carbonizado
            if (state == CellState.HIT || state == CellState.SUNK) {
                markCellExploded(x, y);
            }
        }
    }

    public void showShip(Ship ship) {
        removeShip(ship);

        Node model = shipFactory.createShipNode(ship.getType());
        positionShipNode(model, ship);

        // Si ya está hundido, iniciar animación de hundimiento en el agua
        if (ship.isSunk()) {
            startShipSinking(ship, model);
        }

        attachChild(model);
        shipNodes.put(ship, model);
    }

    /**
     * Inicia la animación de hundimiento gradual del barco en el agua.
     */
    public void startShipSinking(Ship ship) {
        Node model = shipNodes.get(ship);
        if (model != null) {
            startShipSinking(ship, model);
        }
    }

    private void startShipSinking(Ship ship, Node model) {
        Vector3f basePos = shipBasePos.get(ship);
        Quaternion baseRot = shipBaseRot.get(ship);
        if (basePos == null) basePos = model.getLocalTranslation().clone();
        if (baseRot == null) baseRot = model.getLocalRotation().clone();

        // Evitar duplicar la animación para el mismo barco
        sinkingShips.removeIf(sa -> sa.ship == ship);
        boolean alternate = (ship.getStartX() + ship.getStartY()) % 2 == 0;
        sinkingShips.add(new SinkingShipAnimation(ship, model, basePos, baseRot, alternate));
    }

    /**
     * Muestra un comentario táctico flotante 3D sobre el barco explotado.
     */
    public void showFloatingComment(Ship ship, String phrase) {
        if (ship == null || phrase == null || phrase.isBlank()) return;
        Vector3f center = getShipCenterLocal(ship);
        FloatingShipComment3D comment = new FloatingShipComment3D(assetManager, center, phrase);
        attachChild(comment);
        activeComments.add(comment);
    }

    public Vector3f getShipCenterLocal(Ship ship) {
        float size = ship.getType().getSize();
        float startX = ship.getStartX();
        float startY = ship.getStartY();
        if (ship.isHorizontal()) {
            float cx = (startX + size * 0.5f) * CELL_SIZE;
            float cz = (startY + 0.5f) * CELL_SIZE;
            return new Vector3f(cx, 0.05f, cz);
        } else {
            float cx = (startX + 0.5f) * CELL_SIZE;
            float cz = (startY + size * 0.5f) * CELL_SIZE;
            return new Vector3f(cx, 0.05f, cz);
        }
    }

    public Vector3f getShipCenterWorld(Ship ship) {
        return localToWorld(getShipCenterLocal(ship), null);
    }

    public void removeShip(Ship ship) {
        Node existing = shipNodes.remove(ship);
        if (existing != null) {
            existing.removeFromParent();
        }
        shipBasePos.remove(ship);
        shipBaseRot.remove(ship);
        sinkingShips.removeIf(sa -> sa.ship == ship);
    }

    public void clearAllShips() {
        for (Node n : shipNodes.values()) {
            n.removeFromParent();
        }
        shipNodes.clear();
        shipBasePos.clear();
        shipBaseRot.clear();
        sinkingShips.clear();

        for (FloatingShipComment3D c : activeComments) {
            c.removeFromParent();
        }
        activeComments.clear();
    }

    public void clearAllMarkers() {
        for (int y = 0; y < Board.SIZE; y++) {
            for (int x = 0; x < Board.SIZE; x++) {
                if (markerNodes[y][x] != null) {
                    markerNodes[y][x].removeFromParent();
                    markerNodes[y][x] = null;
                }
                cellExploded[y][x] = false;
                cellGeometries[y][x].setMaterial(matNormalCell);
            }
        }
        clearHintGlow();
        for (FloatingShipComment3D c : activeComments) {
            c.removeFromParent();
        }
        activeComments.clear();
    }

    /** Muestra una baliza luminosa sobre una casilla para el sistema de ayudas. */
    public void showHintGlow(int x, int y) {
        if (x < 0 || x >= Board.SIZE || y < 0 || y >= Board.SIZE) {
            return;
        }
        clearHintGlow();

        hintGlow = new HintGlow3D(assetManager);
        hintGlow.setLocalTranslation(getCellCenter(x, y));
        attachChild(hintGlow);
        hintGlowX = x;
        hintGlowY = y;
    }

    public void clearHintGlow() {
        if (hintGlow != null) {
            hintGlow.removeFromParent();
            hintGlow = null;
        }
        hintGlowX = -1;
        hintGlowY = -1;
    }

    public boolean hasHintGlow() {
        return hintGlow != null;
    }

    private void positionShipNode(Node node, Ship ship) {
        int x = ship.getStartX();
        int y = ship.getStartY();
        boolean horizontal = ship.isHorizontal();

        Vector3f basePos;
        Quaternion baseRot;
        if (horizontal) {
            baseRot = Quaternion.IDENTITY.clone();
            basePos = new Vector3f(x * CELL_SIZE, 0.05f, (y + 0.5f) * CELL_SIZE);
        } else {
            baseRot = new Quaternion().fromAngleAxis(-FastMath.HALF_PI, Vector3f.UNIT_Y);
            basePos = new Vector3f((x + 0.5f) * CELL_SIZE, 0.05f, y * CELL_SIZE);
        }
        node.setLocalRotation(baseRot);
        node.setLocalTranslation(basePos);
        shipBasePos.put(ship, basePos);
        shipBaseRot.put(ship, baseRot);
    }

    public void showGhostShip(ShipType type, int startX, int startY, boolean horizontal, boolean valid) {
        hideGhostShip();

        ghostShipNode = shipFactory.createShipNode(type);
        Material ghostMat = shipFactory.createGhostMaterial(valid);
        applyMaterialRecursively(ghostShipNode, ghostMat);

        if (horizontal) {
            ghostShipNode.setLocalRotation(Quaternion.IDENTITY);
            ghostShipNode.setLocalTranslation(startX * CELL_SIZE, 0.15f, (startY + 0.5f) * CELL_SIZE);
        } else {
            Quaternion q = new Quaternion().fromAngleAxis(-FastMath.HALF_PI, Vector3f.UNIT_Y);
            ghostShipNode.setLocalRotation(q);
            ghostShipNode.setLocalTranslation((startX + 0.5f) * CELL_SIZE, 0.15f, startY * CELL_SIZE);
        }

        attachChild(ghostShipNode);
    }

    public void hideGhostShip() {
        if (ghostShipNode != null) {
            ghostShipNode.removeFromParent();
            ghostShipNode = null;
        }
    }

    private void applyMaterialRecursively(Node node, Material mat) {
        node.setQueueBucket(RenderQueue.Bucket.Transparent);
        for (com.jme3.scene.Spatial child : node.getChildren()) {
            child.setQueueBucket(RenderQueue.Bucket.Transparent);
            if (child instanceof Geometry) {
                ((Geometry) child).setMaterial(mat);
            } else if (child instanceof Node) {
                applyMaterialRecursively((Node) child, mat);
            }
        }
    }

    public void update(float tpf) {
        update(tpf, activeCamera);
    }

    public void update(float tpf, Camera camera) {
        this.activeCamera = camera;
        waveTime += tpf;

        // 1. Actualizar oleaje del sector del tablero
        if (boardWaves != null) {
            boardWaves.update(tpf);
        }

        // 2. Ondear / balancear suavemente los barcos que siguen a flote
        for (Map.Entry<Ship, Node> entry : shipNodes.entrySet()) {
            Ship ship = entry.getKey();
            Node node = entry.getValue();

            boolean isSinking = false;
            for (SinkingShipAnimation sa : sinkingShips) {
                if (sa.ship == ship) {
                    isSinking = true;
                    break;
                }
            }
            if (isSinking || ship.isSunk()) {
                continue;
            }

            Vector3f basePos = shipBasePos.get(ship);
            Quaternion baseRot = shipBaseRot.get(ship);
            if (basePos != null && baseRot != null) {
                float phase = waveTime * 1.9f + basePos.x * 0.38f + basePos.z * 0.44f;
                float phase2 = waveTime * 1.3f - basePos.x * 0.25f + basePos.z * 0.35f;

                // Elevación suave y balanceo de quilla y proa
                float heave = FastMath.sin(phase) * 0.045f + FastMath.cos(phase2) * 0.02f;
                float roll = FastMath.sin(phase) * 0.030f;
                float pitch = FastMath.cos(phase2) * 0.020f;

                Quaternion swayRot = baseRot.mult(new Quaternion().fromAngles(pitch, 0, roll));
                node.setLocalTranslation(basePos.x, basePos.y + heave, basePos.z);
                node.setLocalRotation(swayRot);
            }
        }

        // 3. Actualizar animación de hundimiento de barcos destruidos
        for (int i = sinkingShips.size() - 1; i >= 0; i--) {
            SinkingShipAnimation sa = sinkingShips.get(i);
            if (sa.update(tpf)) {
                sinkingShips.remove(i);
            }
        }

        // 4. Actualizar comentarios flotantes 3D sobre barcos destruidos
        for (int i = activeComments.size() - 1; i >= 0; i--) {
            FloatingShipComment3D comment = activeComments.get(i);
            comment.update(tpf, camera);
            if (comment.isFinished()) {
                activeComments.remove(i);
            }
        }

        // 5. Retícula táctica y marcadores de disparo
        tacticalReticle.update(tpf);

        if (hintGlow != null) {
            hintGlow.update(tpf);
        }

        for (int y = 0; y < Board.SIZE; y++) {
            for (int x = 0; x < Board.SIZE; x++) {
                if (markerNodes[y][x] != null) {
                    markerNodes[y][x].update(tpf);
                }
            }
        }
    }

    public void setActiveCamera(Camera camera) {
        this.activeCamera = camera;
    }

    public String getBoardId() {
        return boardId;
    }

    public boolean isPlayerBoard() {
        return isPlayerBoard;
    }

    /**
     * Animación cinemática de hundimiento gradual para barcos explotados.
     */
    private static class SinkingShipAnimation {
        final Ship ship;
        final Node model;
        final Vector3f basePos;
        final Quaternion baseRot;
        final float targetRoll;
        final float targetPitch;
        float elapsed = 0f;
        static final float DURATION = 3.8f;

        SinkingShipAnimation(Ship ship, Node model, Vector3f basePos, Quaternion baseRot, boolean alternateTilt) {
            this.ship = ship;
            this.model = model;
            this.basePos = basePos.clone();
            this.baseRot = baseRot.clone();
            this.targetRoll = alternateTilt ? 0.38f : -0.38f;
            this.targetPitch = -0.24f; // Inclinación hacia la proa
        }

        boolean update(float tpf) {
            elapsed += tpf;
            float p = Math.min(1.0f, elapsed / DURATION);

            // Inclinación escorada progresiva
            float tiltProgress = Math.min(1.0f, p * 1.5f);
            float smoothTilt = tiltProgress * tiltProgress * (3f - 2f * tiltProgress);
            float r = targetRoll * smoothTilt;
            float pitch = targetPitch * smoothTilt;

            // Inmersión progresiva bajo el nivel del agua
            float depth = basePos.y - (p * p * 2.25f);

            Quaternion tilt = new Quaternion().fromAngles(pitch, 0f, r);
            model.setLocalRotation(baseRot.mult(tilt));
            model.setLocalTranslation(basePos.x, depth, basePos.z);

            return elapsed >= DURATION;
        }
    }
}
