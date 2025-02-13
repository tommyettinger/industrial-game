package com.gadarts.industrial;

import com.gadarts.industrial.shared.model.pickups.PlayerWeaponsDefinitions;

import static com.badlogic.gdx.Application.LOG_DEBUG;
import static com.gadarts.industrial.systems.ui.menu.NewGameMenuOptions.OFFICE;

public final class DebugSettings {
	public static final boolean HIDE_GROUND = false;
	public static final boolean HIDE_WALLS = false;
	public static final boolean HIDE_ENEMIES = false;
	public static final boolean HIDE_CHARACTERS = false;
	public static final boolean HIDE_ENVIRONMENT_OBJECTS = false;
	public static final boolean HIDE_CURSOR = false;
	public static final boolean MENU_ON_STARTUP = true;
	public static final boolean DISABLE_LIGHTS = false;
	public static final boolean DISPLAY_CURSOR_POSITION = true;
	public static final boolean SHOW_GL_PROFILING = true;
	public static final boolean DISPLAY_HUD_OUTLINES = false;
	public static final boolean MELODY_ENABLED = true;
	public static final boolean SFX_ENABLED = true;
	public static final boolean FULL_SCREEN = true;
	public static final boolean DISABLE_FRUSTUM_CULLING = false;
	public static final int LOG_LEVEL = LOG_DEBUG;
	public static final PlayerWeaponsDefinitions STARTING_WEAPON = PlayerWeaponsDefinitions.PUNCH;
	public static final boolean ALLOW_STATIC_SHADOWS = true;
	public static final boolean DISABLE_FOW = false;
	public static final boolean PARALYZED_ENEMIES = false;
	public static final boolean LOW_HP_FOR_ENEMIES = false;
	public static final boolean LOW_HP_FOR_PLAYER = false;
	public static final boolean GOD_MODE = false;
	public static final boolean ENEMY_INVULNERABLE = false;
	public static final boolean ENEMY_CANT_MOVE = false;
	public static final boolean SPACE_BAR_SKIPS_PLAYER = false;
	public static final String TEST_LEVEL = OFFICE.name().toLowerCase();
}
