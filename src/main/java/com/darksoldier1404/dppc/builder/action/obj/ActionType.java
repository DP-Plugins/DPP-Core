package com.darksoldier1404.dppc.builder.action.obj;

public enum ActionType {
    // --- Timing ---
    DELAY,

    // --- Messages ---
    SEND_MESSAGE,
    SEND_TITLE,
    SEND_ACTIONBAR,
    BROADCAST,
    BROADCAST_WORLD,

    // --- Commands ---
    EXECUTE_AS_ADMIN,
    EXECUTE_AS_PLAYER,

    // --- Player Movement ---
    TELEPORT,

    // --- Player State ---
    CLOSE_INVENTORY,
    SET_GAMEMODE,
    GIVE_EXP,
    TAKE_EXP,
    SET_HEALTH,
    SET_HUNGER,
    KICK,

    // --- Effects ---
    PLAY_SOUND,
    PLAY_PARTICLE,
    ADD_POTION_EFFECT,
    REMOVE_POTION_EFFECT,
    CLEAR_EFFECTS,

    // --- Inventory ---
    GIVE_ITEM,
    TAKE_ITEM,

    // --- Variables (Temporary: per-execution only) ---
    SET_TEMP_VARIABLE,
    ADD_TEMP_VARIABLE,
    RANDOM_TEMP_NUMBER,

    // --- Variables (Player: persisted per player) ---
    SET_PLAYER_VARIABLE,
    ADD_PLAYER_VARIABLE,
    RANDOM_PLAYER_NUMBER,

    // --- Variables (Global: persisted server-wide) ---
    SET_GLOBAL_VARIABLE,
    ADD_GLOBAL_VARIABLE,
    RANDOM_GLOBAL_NUMBER,

    // --- Conditions ---
    IF_HAS_PERMISSION,
    IF_NOT_PERMISSION,
    IF_TEMP_VARIABLE_EQUALS,
    IF_TEMP_VARIABLE_NOT_EQUALS,
    IF_TEMP_VARIABLE_GREATER,
    IF_TEMP_VARIABLE_LESS,
    IF_PLAYER_VARIABLE_EQUALS,
    IF_PLAYER_VARIABLE_NOT_EQUALS,
    IF_PLAYER_VARIABLE_GREATER,
    IF_PLAYER_VARIABLE_LESS,
    IF_GLOBAL_VARIABLE_EQUALS,
    IF_GLOBAL_VARIABLE_NOT_EQUALS,
    IF_GLOBAL_VARIABLE_GREATER,
    IF_GLOBAL_VARIABLE_LESS,
    ELSE,
    END_IF,

    // --- Flow Control ---
    CANCEL,
    CALL_ACTION,
}
