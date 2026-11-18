package com.batallanaval.view;

import com.batallanaval.model.ShipType;
import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Cylinder;
import com.jme3.scene.shape.Sphere;

public class ShipMeshFactory {

    public static final float CELL_SIZE = 2.0f;

    private final AssetManager assetManager;
    private final Material matHullGrey;
    private final Material matDeckDark;
    private final Material matRedWaterline;
    private final Material matGunMetal;
    private final Material matWhiteStripe;
    private final Material matSubBlack;
    private final Material matTowerGlass;
    private final Material matRadome;
    private final Material matPhasedRadar;

    public ShipMeshFactory(AssetManager assetManager) {
        this.assetManager = assetManager;

        this.matHullGrey = createLightingMaterial(new ColorRGBA(0.40f, 0.44f, 0.48f, 1f));
        this.matDeckDark = createLightingMaterial(new ColorRGBA(0.20f, 0.22f, 0.24f, 1f));
        this.matRedWaterline = createLightingMaterial(new ColorRGBA(0.55f, 0.12f, 0.12f, 1f));
        this.matGunMetal = createLightingMaterial(new ColorRGBA(0.14f, 0.16f, 0.18f, 1f));
        this.matWhiteStripe = createLightingMaterial(new ColorRGBA(0.95f, 0.95f, 0.98f, 1f));
        this.matSubBlack = createLightingMaterial(new ColorRGBA(0.08f, 0.09f, 0.11f, 1f));
        this.matTowerGlass = createLightingMaterial(new ColorRGBA(0.12f, 0.65f, 0.85f, 1f));
        this.matRadome = createLightingMaterial(new ColorRGBA(0.92f, 0.93f, 0.95f, 1f));
        this.matPhasedRadar = createLightingMaterial(new ColorRGBA(0.30f, 0.35f, 0.38f, 1f));
    }

    private Material createLightingMaterial(ColorRGBA color) {
        Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        mat.setBoolean("UseMaterialColors", true);
        mat.setColor("Diffuse", color);
        mat.setColor("Ambient", color.mult(0.45f));
        mat.setColor("Specular", ColorRGBA.White.mult(0.35f));
        mat.setFloat("Shininess", 32f);
        return mat;
    }

    public Material createGhostMaterial(boolean valid) {
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        ColorRGBA col = valid ? new ColorRGBA(0.1f, 1.0f, 0.4f, 0.65f) : new ColorRGBA(1.0f, 0.15f, 0.15f, 0.65f);
        mat.setColor("Color", col);
        mat.getAdditionalRenderState().setBlendMode(com.jme3.material.RenderState.BlendMode.Alpha);
        return mat;
    }

    public Node createShipNode(ShipType type) {
        Node shipNode = new Node("Ship_" + type.name());
        float length = type.getSize() * CELL_SIZE;

        switch (type) {
            case PORTAVIONES:
                buildAircraftCarrier(shipNode, length);
                break;
            case ACORAZADO:
                buildBattleship(shipNode, length);
                break;
            case CRUCERO:
                buildCruiser(shipNode, length);
                break;
            case SUBMARINO:
                buildSubmarine(shipNode, length);
                break;
            case DESTRUCTOR:
                buildDestroyer(shipNode, length);
                break;
        }

        return shipNode;
    }

    // =========================================================================
    // 1. AIRCRAFT CARRIER (5 Tiles)
    // =========================================================================
    private void buildAircraftCarrier(Node root, float totalLength) {
        float bodyLen = totalLength * 0.72f;
        float bowLen = totalLength * 0.28f;
        float width = CELL_SIZE * 0.75f;
        float halfW = width * 0.45f;

        // --- Waterline Hull (Red Anti-fouling) ---
        Box botBody = new Box(bodyLen * 0.5f, 0.12f, halfW * 0.9f);
        Geometry gBotBody = new Geometry("BotBody", botBody);
        gBotBody.setMaterial(matRedWaterline);
        gBotBody.setLocalTranslation(bodyLen * 0.5f, 0.12f, 0);
        root.attachChild(gBotBody);

        Mesh botBowMesh = WedgeMeshBuilder.createBowMesh(bowLen, 0.24f, halfW * 0.9f);
        Geometry gBotBow = new Geometry("BotBow", botBowMesh);
        gBotBow.setMaterial(matRedWaterline);
        gBotBow.setLocalTranslation(bodyLen, 0.0f, 0);
        root.attachChild(gBotBow);

        // --- Main Hull (Naval Grey) ---
        Box mainBody = new Box(bodyLen * 0.5f, 0.26f, halfW);
        Geometry gMainBody = new Geometry("MainBody", mainBody);
        gMainBody.setMaterial(matHullGrey);
        gMainBody.setLocalTranslation(bodyLen * 0.5f, 0.50f, 0);
        root.attachChild(gMainBody);

        Mesh mainBowMesh = WedgeMeshBuilder.createBowMesh(bowLen, 0.52f, halfW);
        Geometry gMainBow = new Geometry("MainBow", mainBowMesh);
        gMainBow.setMaterial(matHullGrey);
        gMainBow.setLocalTranslation(bodyLen, 0.24f, 0);
        root.attachChild(gMainBow);

        // --- Angled Modern Flight Deck ---
        float deckLen = totalLength - 0.2f;
        float deckWidth = CELL_SIZE * 0.92f;
        Box flightDeck = new Box(deckLen * 0.5f, 0.06f, deckWidth * 0.5f);
        Geometry gDeck = new Geometry("FlightDeck", flightDeck);
        gDeck.setMaterial(matDeckDark);
        gDeck.setLocalTranslation(deckLen * 0.5f + 0.1f, 0.80f, 0);
        root.attachChild(gDeck);

        // Runway Centerline Stripe (White)
        Box runway = new Box(deckLen * 0.42f, 0.01f, 0.04f);
        Geometry gRunway = new Geometry("RunwayStripe", runway);
        gRunway.setMaterial(matWhiteStripe);
        gRunway.setLocalTranslation(deckLen * 0.46f, 0.87f, -deckWidth * 0.08f);
        root.attachChild(gRunway);

        // Angled Recovery Lane (angled at ~9 degrees)
        Box angleLane = new Box(deckLen * 0.35f, 0.01f, 0.04f);
        Geometry gAngleLane = new Geometry("AngleLane", angleLane);
        gAngleLane.setMaterial(matWhiteStripe);
        gAngleLane.setLocalTranslation(deckLen * 0.38f, 0.87f, deckWidth * 0.16f);
        gAngleLane.rotate(0, 0.15f, 0);
        root.attachChild(gAngleLane);

        // 3 Arresting Gear Wire Stripes (Cables de frenado en cubierta de popa)
        for (int i = 0; i < 3; i++) {
            Box wire = new Box(0.02f, 0.01f, deckWidth * 0.35f);
            Geometry gWire = new Geometry("ArrestWire_" + i, wire);
            gWire.setMaterial(matWhiteStripe);
            gWire.setLocalTranslation(0.6f + i * 0.55f, 0.87f, deckWidth * 0.14f);
            root.attachChild(gWire);
        }

        // --- Modern Stealth Island (Superstructure on Starboard) ---
        float islandX = deckLen * 0.65f;
        float islandZ = deckWidth * 0.38f;

        Box islandBase = new Box(0.75f, 0.38f, 0.20f);
        Geometry gIsland = new Geometry("IslandBase", islandBase);
        gIsland.setMaterial(matHullGrey);
        gIsland.setLocalTranslation(islandX, 1.24f, islandZ);
        root.attachChild(gIsland);

        // Flight Control Pri-Fly Windows
        Box windows = new Box(0.65f, 0.08f, 0.22f);
        Geometry gWin = new Geometry("IslandWindows", windows);
        gWin.setMaterial(matTowerGlass);
        gWin.setLocalTranslation(islandX, 1.52f, islandZ);
        root.attachChild(gWin);

        // Phased Array Flat Radar Panel
        Box radarPanel = new Box(0.25f, 0.18f, 0.02f);
        Geometry gRadar = new Geometry("RadarPanel", radarPanel);
        gRadar.setMaterial(matPhasedRadar);
        gRadar.setLocalTranslation(islandX, 1.45f, islandZ + 0.21f);
        root.attachChild(gRadar);

        // Communication Radome Dome
        Sphere radome = new Sphere(8, 8, 0.14f);
        Geometry gRadome = new Geometry("Radome", radome);
        gRadome.setMaterial(matRadome);
        gRadome.setLocalTranslation(islandX - 0.35f, 1.76f, islandZ);
        root.attachChild(gRadome);

        // Primary Sensor Mast
        Cylinder mast = new Cylinder(8, 8, 0.035f, 0.7f, true);
        Geometry gMast = new Geometry("Mast", mast);
        gMast.setMaterial(matGunMetal);
        gMast.setLocalRotation(new Quaternion().fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_X));
        gMast.setLocalTranslation(islandX + 0.25f, 1.95f, islandZ);
        root.attachChild(gMast);

        // --- Phalanx CIWS Anti-Missile Gatling Mounts on Sponsons ---
        addCIWS(root, deckLen * 0.15f, 0.85f, -deckWidth * 0.52f);
        addCIWS(root, deckLen * 0.88f, 0.85f, deckWidth * 0.52f);

        // --- Parked Naval Strike Fighters (F-35C style) ---
        addFighterJet(root, deckLen * 0.24f, 0.88f, -deckWidth * 0.32f, 0.2f);
        addFighterJet(root, deckLen * 0.42f, 0.88f, -deckWidth * 0.34f, -0.1f);
        addFighterJet(root, deckLen * 0.80f, 0.88f, deckWidth * 0.18f, 2.8f);
    }

    // =========================================================================
    // 2. BATTLESHIP (4 Tiles)
    // =========================================================================
    private void buildBattleship(Node root, float totalLength) {
        float bodyLen = totalLength * 0.70f;
        float bowLen = totalLength * 0.30f;
        float width = CELL_SIZE * 0.65f;
        float halfW = width * 0.44f;

        // Red Waterline Bottom
        Box botBody = new Box(bodyLen * 0.5f, 0.12f, halfW * 0.9f);
        Geometry gBotBody = new Geometry("BotBody", botBody);
        gBotBody.setMaterial(matRedWaterline);
        gBotBody.setLocalTranslation(bodyLen * 0.5f, 0.12f, 0);
        root.attachChild(gBotBody);

        Mesh botBow = WedgeMeshBuilder.createBowMesh(bowLen, 0.24f, halfW * 0.9f);
        Geometry gBotBow = new Geometry("BotBow", botBow);
        gBotBow.setMaterial(matRedWaterline);
        gBotBow.setLocalTranslation(bodyLen, 0.0f, 0);
        root.attachChild(gBotBow);

        // Armored Hull
        Box mainBody = new Box(bodyLen * 0.5f, 0.35f, halfW);
        Geometry gMainBody = new Geometry("MainBody", mainBody);
        gMainBody.setMaterial(matHullGrey);
        gMainBody.setLocalTranslation(bodyLen * 0.5f, 0.59f, 0);
        root.attachChild(gMainBody);

        Mesh mainBow = WedgeMeshBuilder.createBowMesh(bowLen, 0.70f, halfW);
        Geometry gMainBow = new Geometry("MainBow", mainBow);
        gMainBow.setMaterial(matHullGrey);
        gMainBow.setLocalTranslation(bodyLen, 0.24f, 0);
        root.attachChild(gMainBow);

        // Armored Teak Deck
        Box mainDeck = new Box(bodyLen * 0.5f - 0.05f, 0.05f, halfW - 0.04f);
        Geometry gDeck = new Geometry("MainDeck", mainDeck);
        gDeck.setMaterial(matDeckDark);
        gDeck.setLocalTranslation(bodyLen * 0.5f, 0.99f, 0);
        root.attachChild(gDeck);

        // Central Armored Citadel Superstructure
        Box citadel = new Box(1.2f, 0.45f, halfW * 0.62f);
        Geometry gCitadel = new Geometry("Citadel", citadel);
        gCitadel.setMaterial(matHullGrey);
        gCitadel.setLocalTranslation(bodyLen * 0.5f, 1.45f, 0);
        root.attachChild(gCitadel);

        // Forward Bridge Tower & Windows
        Box bridgeTower = new Box(0.6f, 0.35f, halfW * 0.5f);
        Geometry gBridge = new Geometry("BridgeTower", bridgeTower);
        gBridge.setMaterial(matHullGrey);
        gBridge.setLocalTranslation(bodyLen * 0.5f + 0.55f, 2.05f, 0);
        root.attachChild(gBridge);

        Box bridgeWin = new Box(0.55f, 0.08f, halfW * 0.52f);
        Geometry gWin = new Geometry("BridgeWin", bridgeWin);
        gWin.setMaterial(matTowerGlass);
        gWin.setLocalTranslation(bodyLen * 0.5f + 0.58f, 2.22f, 0);
        root.attachChild(gWin);

        // Rangefinder Director on Citadel Roof
        Box rangefinder = new Box(0.15f, 0.10f, 0.45f);
        Geometry gRf = new Geometry("Rangefinder", rangefinder);
        gRf.setMaterial(matGunMetal);
        gRf.setLocalTranslation(bodyLen * 0.5f + 0.55f, 2.50f, 0);
        root.attachChild(gRf);

        // Twin Naval Funnels with Raked Cowls
        for (int i = 0; i < 2; i++) {
            float funnelX = bodyLen * 0.5f - 0.35f - i * 0.65f;
            Cylinder funnel = new Cylinder(10, 10, 0.14f, 0.75f, true);
            Geometry gFunnel = new Geometry("Funnel_" + i, funnel);
            gFunnel.setMaterial(matGunMetal);
            gFunnel.setLocalRotation(new Quaternion().fromAngleAxis(FastMath.HALF_PI + 0.15f, Vector3f.UNIT_X));
            gFunnel.setLocalTranslation(funnelX, 1.95f, 0);
            root.attachChild(gFunnel);
        }

        // Heavy Artillery: Triple 16-Inch Gun Turrets
        // Turret 1 (Forward Lower - Anton)
        addTripleTurret(root, bodyLen * 0.5f + 1.85f, 1.15f, 0, 0.48f, 1.05f);
        // Turret 2 (Forward Elevated - Bruno)
        addTripleTurret(root, bodyLen * 0.5f + 1.05f, 1.45f, 0, 0.46f, 1.05f);
        // Turret 3 (Aft - Caesar)
        addTripleTurret(root, bodyLen * 0.5f - 1.85f, 1.15f, 0, 0.48f, -1.05f);

        // Secondary 5-inch Dual Gun Mounts along flanks
        addSecondaryGun(root, bodyLen * 0.5f - 0.15f, 1.25f, halfW * 0.85f);
        addSecondaryGun(root, bodyLen * 0.5f - 0.15f, 1.25f, -halfW * 0.85f);
    }

    // =========================================================================
    // 3. GUIDED MISSILE CRUISER (3 Tiles)
    // =========================================================================
    private void buildCruiser(Node root, float totalLength) {
        float bodyLen = totalLength * 0.70f;
        float bowLen = totalLength * 0.30f;
        float width = CELL_SIZE * 0.56f;
        float halfW = width * 0.44f;

        // Red Bottom
        Box botBody = new Box(bodyLen * 0.5f, 0.10f, halfW * 0.9f);
        Geometry gBotBody = new Geometry("BotBody", botBody);
        gBotBody.setMaterial(matRedWaterline);
        gBotBody.setLocalTranslation(bodyLen * 0.5f, 0.10f, 0);
        root.attachChild(gBotBody);

        Mesh botBow = WedgeMeshBuilder.createBowMesh(bowLen, 0.20f, halfW * 0.9f);
        Geometry gBotBow = new Geometry("BotBow", botBow);
        gBotBow.setMaterial(matRedWaterline);
        gBotBow.setLocalTranslation(bodyLen, 0.0f, 0);
        root.attachChild(gBotBow);

        // Sleek Hull
        Box mainBody = new Box(bodyLen * 0.5f, 0.30f, halfW);
        Geometry gMainBody = new Geometry("MainBody", mainBody);
        gMainBody.setMaterial(matHullGrey);
        gMainBody.setLocalTranslation(bodyLen * 0.5f, 0.50f, 0);
        root.attachChild(gMainBody);

        Mesh mainBow = WedgeMeshBuilder.createBowMesh(bowLen, 0.60f, halfW);
        Geometry gMainBow = new Geometry("MainBow", mainBow);
        gMainBow.setMaterial(matHullGrey);
        gMainBow.setLocalTranslation(bodyLen, 0.20f, 0);
        root.attachChild(gMainBow);

        // Deck
        Box deck = new Box(bodyLen * 0.5f - 0.04f, 0.04f, halfW - 0.03f);
        Geometry gDeck = new Geometry("Deck", deck);
        gDeck.setMaterial(matDeckDark);
        gDeck.setLocalTranslation(bodyLen * 0.5f, 0.84f, 0);
        root.attachChild(gDeck);

        // Aegis Deckhouse Superstructure
        Box deckhouse = new Box(0.95f, 0.38f, halfW * 0.65f);
        Geometry gSuper = new Geometry("Superstructure", deckhouse);
        gSuper.setMaterial(matHullGrey);
        gSuper.setLocalTranslation(bodyLen * 0.5f + 0.15f, 1.22f, 0);
        root.attachChild(gSuper);

        // SPY-1 Phased Radar Array Panels (Aegis Shield)
        Box spyPanelPort = new Box(0.28f, 0.22f, 0.02f);
        Geometry gSpyPort = new Geometry("SpyPort", spyPanelPort);
        gSpyPort.setMaterial(matPhasedRadar);
        gSpyPort.setLocalTranslation(bodyLen * 0.5f + 0.35f, 1.30f, halfW * 0.66f);
        root.attachChild(gSpyPort);

        Geometry gSpyStbd = new Geometry("SpyStbd", spyPanelPort);
        gSpyStbd.setMaterial(matPhasedRadar);
        gSpyStbd.setLocalTranslation(bodyLen * 0.5f + 0.35f, 1.30f, -halfW * 0.66f);
        root.attachChild(gSpyStbd);

        // Bridge Windows
        Box win = new Box(0.40f, 0.08f, halfW * 0.67f);
        Geometry gWin = new Geometry("CruiserWin", win);
        gWin.setMaterial(matTowerGlass);
        gWin.setLocalTranslation(bodyLen * 0.5f + 0.45f, 1.48f, 0);
        root.attachChild(gWin);

        // VLS (Vertical Launch System) Missile Cell Grid on Forecastle
        Box vls = new Box(0.45f, 0.02f, 0.28f);
        Geometry gVls = new Geometry("VLS_Grid", vls);
        gVls.setMaterial(matGunMetal);
        gVls.setLocalTranslation(bodyLen * 0.5f + 1.45f, 0.89f, 0);
        root.attachChild(gVls);

        // Forward Stealth 5-Inch Gun
        addStealthTurret(root, bodyLen * 0.5f + 2.15f, 0.98f, 0, 0.32f, 0.75f);

        // Harpoon Anti-Ship Missile Launchers (angled canisters)
        Box harpoon = new Box(0.25f, 0.12f, 0.18f);
        Geometry gHarp = new Geometry("Harpoon", harpoon);
        gHarp.setMaterial(matGunMetal);
        gHarp.setLocalTranslation(bodyLen * 0.5f - 0.70f, 1.05f, 0);
        gHarp.rotate(0, 0, 0.35f);
        root.attachChild(gHarp);

        // Helipad (Hangar & Landing Circle on Stern)
        Box helipad = new Box(0.65f, 0.02f, halfW * 0.85f);
        Geometry gHeli = new Geometry("Helipad", helipad);
        gHeli.setMaterial(matDeckDark);
        gHeli.setLocalTranslation(0.75f, 0.89f, 0);
        root.attachChild(gHeli);

        Cylinder heliCircle = new Cylinder(16, 16, 0.32f, 0.01f, true);
        Geometry gCircle = new Geometry("HeliCircle", heliCircle);
        gCircle.setMaterial(matWhiteStripe);
        gCircle.setLocalRotation(new Quaternion().fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_X));
        gCircle.setLocalTranslation(0.75f, 0.91f, 0);
        root.attachChild(gCircle);

        // Radar Mast
        Cylinder mast = new Cylinder(8, 8, 0.035f, 0.8f, true);
        Geometry gMast = new Geometry("CruiserMast", mast);
        gMast.setMaterial(matGunMetal);
        gMast.setLocalRotation(new Quaternion().fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_X));
        gMast.setLocalTranslation(bodyLen * 0.5f - 0.15f, 1.85f, 0);
        root.attachChild(gMast);
    }

    // =========================================================================
    // 4. NUCLEAR ATTACK SUBMARINE (3 Tiles)
    // =========================================================================
    private void buildSubmarine(Node root, float totalLength) {
        float radius = CELL_SIZE * 0.24f;
        float bodyLen = totalLength * 0.65f;
        float noseLen = totalLength * 0.18f;
        float tailLen = totalLength * 0.17f;

        // Sonar Dome Rounded Nose (Hemisphere)
        Sphere nose = new Sphere(16, 16, radius);
        Geometry gNose = new Geometry("SubNose", nose);
        gNose.setMaterial(matSubBlack);
        gNose.setLocalTranslation(totalLength - radius - 0.1f, radius + 0.12f, 0);
        root.attachChild(gNose);

        // Main Cylindrical Pressure Hull (Anechoic Black)
        Cylinder hull = new Cylinder(20, 20, radius, bodyLen, true);
        Geometry gHull = new Geometry("SubHull", hull);
        gHull.setMaterial(matSubBlack);
        gHull.setLocalRotation(new Quaternion().fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_Y));
        gHull.setLocalTranslation(bodyLen * 0.5f + tailLen, radius + 0.12f, 0);
        root.attachChild(gHull);

        // Tapered Stern Cone
        Cylinder tailCone = new Cylinder(16, 16, radius, 0.08f, tailLen, true, false);
        Geometry gTail = new Geometry("TailCone", tailCone);
        gTail.setMaterial(matSubBlack);
        gTail.setLocalRotation(new Quaternion().fromAngleAxis(-FastMath.HALF_PI, Vector3f.UNIT_Y));
        gTail.setLocalTranslation(tailLen * 0.5f + 0.1f, radius + 0.12f, 0);
        root.attachChild(gTail);

        // Shrouded Pump-jet Propulsor Hub
        Cylinder shroud = new Cylinder(12, 12, 0.18f, 0.14f, 0.22f, true, false);
        Geometry gShroud = new Geometry("PumpJetShroud", shroud);
        gShroud.setMaterial(matGunMetal);
        gShroud.setLocalRotation(new Quaternion().fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_Y));
        gShroud.setLocalTranslation(0.18f, radius + 0.12f, 0);
        root.attachChild(gShroud);

        // Hydrodynamic Sail (Conning Tower)
        Box sail = new Box(0.55f, 0.40f, 0.13f);
        Geometry gSail = new Geometry("SubSail", sail);
        gSail.setMaterial(matSubBlack);
        gSail.setLocalTranslation(totalLength * 0.60f, radius * 2f + 0.42f, 0);
        root.attachChild(gSail);

        // Conformal Fairwater Dive Planes (Hydroplanes on sail)
        Box divePlanes = new Box(0.18f, 0.02f, 0.38f);
        Geometry gPlanes = new Geometry("DivePlanes", divePlanes);
        gPlanes.setMaterial(matSubBlack);
        gPlanes.setLocalTranslation(totalLength * 0.62f, radius * 2f + 0.55f, 0);
        root.attachChild(gPlanes);

        // Periscope & Optronic Mast
        Cylinder peri1 = new Cylinder(8, 8, 0.02f, 0.45f, true);
        Geometry gPeri1 = new Geometry("Periscope1", peri1);
        gPeri1.setMaterial(matGunMetal);
        gPeri1.setLocalRotation(new Quaternion().fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_X));
        gPeri1.setLocalTranslation(totalLength * 0.64f, radius * 2f + 0.95f, 0.04f);
        root.attachChild(gPeri1);

        Cylinder peri2 = new Cylinder(8, 8, 0.015f, 0.35f, true);
        Geometry gPeri2 = new Geometry("Periscope2", peri2);
        gPeri2.setMaterial(matGunMetal);
        gPeri2.setLocalRotation(new Quaternion().fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_X));
        gPeri2.setLocalTranslation(totalLength * 0.58f, radius * 2f + 0.88f, -0.04f);
        root.attachChild(gPeri2);

        // Cruciform Stern Rudders (Cross-fins)
        Box horizFin = new Box(0.35f, 0.03f, radius * 1.6f);
        Geometry gHFin = new Geometry("HorizFin", horizFin);
        gHFin.setMaterial(matSubBlack);
        gHFin.setLocalTranslation(0.35f, radius + 0.12f, 0);
        root.attachChild(gHFin);

        Box vertFin = new Box(0.35f, radius * 1.6f, 0.03f);
        Geometry gVFin = new Geometry("VertFin", vertFin);
        gVFin.setMaterial(matSubBlack);
        gVFin.setLocalTranslation(0.35f, radius + 0.12f, 0);
        root.attachChild(gVFin);
    }

    // =========================================================================
    // 5. DESTROYER (2 Tiles)
    // =========================================================================
    private void buildDestroyer(Node root, float totalLength) {
        float bodyLen = totalLength * 0.68f;
        float bowLen = totalLength * 0.32f;
        float width = CELL_SIZE * 0.48f;
        float halfW = width * 0.44f;

        // Red Bottom
        Box botBody = new Box(bodyLen * 0.5f, 0.08f, halfW * 0.9f);
        Geometry gBotBody = new Geometry("BotBody", botBody);
        gBotBody.setMaterial(matRedWaterline);
        gBotBody.setLocalTranslation(bodyLen * 0.5f, 0.08f, 0);
        root.attachChild(gBotBody);

        Mesh botBow = WedgeMeshBuilder.createBowMesh(bowLen, 0.16f, halfW * 0.9f);
        Geometry gBotBow = new Geometry("BotBow", botBow);
        gBotBow.setMaterial(matRedWaterline);
        gBotBow.setLocalTranslation(bodyLen, 0.0f, 0);
        root.attachChild(gBotBow);

        // Agile Hull
        Box mainBody = new Box(bodyLen * 0.5f, 0.25f, halfW);
        Geometry gMainBody = new Geometry("MainBody", mainBody);
        gMainBody.setMaterial(matHullGrey);
        gMainBody.setLocalTranslation(bodyLen * 0.5f, 0.41f, 0);
        root.attachChild(gMainBody);

        Mesh mainBow = WedgeMeshBuilder.createBowMesh(bowLen, 0.50f, halfW);
        Geometry gMainBow = new Geometry("MainBow", mainBow);
        gMainBow.setMaterial(matHullGrey);
        gMainBow.setLocalTranslation(bodyLen, 0.16f, 0);
        root.attachChild(gMainBow);

        // Deck
        Box deck = new Box(bodyLen * 0.5f - 0.03f, 0.04f, halfW - 0.03f);
        Geometry gDeck = new Geometry("Deck", deck);
        gDeck.setMaterial(matDeckDark);
        gDeck.setLocalTranslation(bodyLen * 0.5f, 0.70f, 0);
        root.attachChild(gDeck);

        // Slanted Stealth Bridge
        Box bridge = new Box(0.55f, 0.28f, halfW * 0.70f);
        Geometry gBridge = new Geometry("Bridge", bridge);
        gBridge.setMaterial(matHullGrey);
        gBridge.setLocalTranslation(bodyLen * 0.5f + 0.15f, 1.02f, 0);
        root.attachChild(gBridge);

        Box bridgeWin = new Box(0.35f, 0.07f, halfW * 0.72f);
        Geometry gWin = new Geometry("DestroyerWin", bridgeWin);
        gWin.setMaterial(matTowerGlass);
        gWin.setLocalTranslation(bodyLen * 0.5f + 0.35f, 1.22f, 0);
        root.attachChild(gWin);

        // Forward Rapid Stealth Cannon
        addStealthTurret(root, bodyLen * 0.5f + 1.25f, 0.82f, 0, 0.26f, 0.55f);

        // Angled Smokestack
        Cylinder funnel = new Cylinder(8, 8, 0.09f, 0.48f, true);
        Geometry gFunnel = new Geometry("DestroyerFunnel", funnel);
        gFunnel.setMaterial(matGunMetal);
        gFunnel.setLocalRotation(new Quaternion().fromAngleAxis(FastMath.HALF_PI + 0.18f, Vector3f.UNIT_X));
        gFunnel.setLocalTranslation(bodyLen * 0.5f - 0.25f, 1.15f, 0);
        root.attachChild(gFunnel);

        // Triple Torpedo Tubes
        Box torpTubes = new Box(0.20f, 0.06f, 0.15f);
        Geometry gTorp = new Geometry("TorpTubes", torpTubes);
        gTorp.setMaterial(matGunMetal);
        gTorp.setLocalTranslation(bodyLen * 0.5f - 0.65f, 0.80f, 0);
        root.attachChild(gTorp);

        // Aft Radome on Mast
        Sphere radome = new Sphere(8, 8, 0.11f);
        Geometry gRadome = new Geometry("DestroyerRadome", radome);
        gRadome.setMaterial(matRadome);
        gRadome.setLocalTranslation(bodyLen * 0.5f - 0.95f, 0.95f, 0);
        root.attachChild(gRadome);
    }

    // =========================================================================
    // HELPER WEAPON & COMPONENT BUILDERS
    // =========================================================================

    private void addTripleTurret(Node root, float x, float y, float z, float size, float barrelDir) {
        Box turretBox = new Box(size, size * 0.55f, size * 0.82f);
        Geometry gTurret = new Geometry("TripleTurret", turretBox);
        gTurret.setMaterial(matHullGrey);
        gTurret.setLocalTranslation(x, y, z);
        root.attachChild(gTurret);

        float barrelLen = Math.abs(barrelDir);
        float sign = Math.signum(barrelDir);
        float[] zOffsets = {-size * 0.5f, 0f, size * 0.5f};

        for (float zo : zOffsets) {
            Cylinder barrel = new Cylinder(8, 8, 0.038f, barrelLen, true);
            Geometry gBarrel = new Geometry("TripleBarrel", barrel);
            gBarrel.setMaterial(matGunMetal);
            gBarrel.setLocalRotation(new Quaternion().fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_Y));
            gBarrel.setLocalTranslation(x + sign * (size + barrelLen * 0.45f), y + size * 0.05f, z + zo);
            root.attachChild(gBarrel);
        }
    }

    private void addStealthTurret(Node root, float x, float y, float z, float size, float barrelDir) {
        Box turretBox = new Box(size, size * 0.5f, size * 0.65f);
        Geometry gTurret = new Geometry("StealthTurret", turretBox);
        gTurret.setMaterial(matHullGrey);
        gTurret.setLocalTranslation(x, y, z);
        root.attachChild(gTurret);

        float barrelLen = Math.abs(barrelDir);
        float sign = Math.signum(barrelDir);
        Cylinder barrel = new Cylinder(8, 8, 0.032f, barrelLen, true);
        Geometry gBarrel = new Geometry("StealthBarrel", barrel);
        gBarrel.setMaterial(matGunMetal);
        gBarrel.setLocalRotation(new Quaternion().fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_Y));
        gBarrel.setLocalTranslation(x + sign * (size + barrelLen * 0.45f), y + size * 0.08f, z);
        root.attachChild(gBarrel);
    }

    private void addSecondaryGun(Node root, float x, float y, float z) {
        Box turret = new Box(0.18f, 0.12f, 0.16f);
        Geometry gTurret = new Geometry("SecondaryGun", turret);
        gTurret.setMaterial(matGunMetal);
        gTurret.setLocalTranslation(x, y, z);
        root.attachChild(gTurret);
    }

    private void addCIWS(Node root, float x, float y, float z) {
        Cylinder dome = new Cylinder(8, 8, 0.08f, 0.18f, true);
        Geometry gDome = new Geometry("CIWS_Dome", dome);
        gDome.setMaterial(matRadome);
        gDome.setLocalRotation(new Quaternion().fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_X));
        gDome.setLocalTranslation(x, y + 0.12f, z);
        root.attachChild(gDome);
    }

    private void addFighterJet(Node root, float x, float y, float z, float rotY) {
        Node jet = new Node("FighterJet");

        // Delta wing
        Box wing = new Box(0.24f, 0.015f, 0.22f);
        Geometry gWing = new Geometry("JetWing", wing);
        gWing.setMaterial(matGunMetal);
        jet.attachChild(gWing);

        // Fuselage
        Box fuse = new Box(0.32f, 0.035f, 0.06f);
        Geometry gFuse = new Geometry("JetFuse", fuse);
        gFuse.setMaterial(matHullGrey);
        gFuse.setLocalTranslation(0.04f, 0.03f, 0);
        jet.attachChild(gFuse);

        // Twin angled vertical stabilizers
        for (int i = -1; i <= 1; i += 2) {
            Box fin = new Box(0.08f, 0.08f, 0.01f);
            Geometry gFin = new Geometry("JetFin", fin);
            gFin.setMaterial(matGunMetal);
            gFin.setLocalTranslation(-0.16f, 0.09f, i * 0.08f);
            gFin.rotate(0, 0, i * 0.25f);
            jet.attachChild(gFin);
        }

        jet.rotate(0, rotY, 0);
        jet.setLocalTranslation(x, y, z);
        root.attachChild(jet);
    }
}
