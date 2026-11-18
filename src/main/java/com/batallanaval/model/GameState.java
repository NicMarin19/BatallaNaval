package com.batallanaval.model;

public enum GameState {
    MENU,
    PLACEMENT,
    PLAYER_TURN,
    ANIMATING_PLAYER_SHOT,
    PLAYER_DOUBLE_SHOT,       // After dice ability grants double-shot
    DICE_ROLL,                // Every 8 rounds: dice ability modal
    AI_TURN,
    ANIMATING_AI_SHOT,
    VICTORY,
    DEFEAT,
    PAUSED
}
