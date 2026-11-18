package com.batallanaval.controller;

import com.jme3.renderer.Camera;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CameraControllerTest {

    private Camera camera;
    private CameraController controller;

    @BeforeEach
    public void setUp() {
        camera = new Camera(1280, 720);
        controller = new CameraController(camera);
    }

    @Test
    public void testInitialMode() {
        assertEquals(CameraController.CameraMode.GENERAL, controller.getCurrentMode());
        assertFalse(controller.isFocusedOnDice());
    }

    @Test
    public void testFocusOnDiceAndRestore() {
        controller.focusOnDice();
        assertTrue(controller.isFocusedOnDice(), "La cámara debe estar en modo enfoque de dados");

        controller.restoreSavedView();
        assertFalse(controller.isFocusedOnDice(), "La cámara debe haber restaurado el modo anterior");
        assertEquals(CameraController.CameraMode.GENERAL, controller.getCurrentMode());
    }

    @Test
    public void testCycleMode() {
        CameraController.CameraMode first = controller.getCurrentMode();
        controller.cycleMode();
        assertNotEquals(first, controller.getCurrentMode());
    }
}
