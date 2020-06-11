package com.lovetropics.gamemodebuild.state;

public final class SPClientState {
	private static boolean active;
	
	public static void setActive(boolean active) {
		SPClientState.active = active;
	}
	
	public static boolean isActive() {
		return SPClientState.active;
	}
}
