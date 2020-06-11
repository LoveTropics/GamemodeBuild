package com.lovetropics.gamemodebuild.state;

public final class GBClientState {
	private static boolean active;
	
	public static void setActive(boolean active) {
		GBClientState.active = active;
	}
	
	public static boolean isActive() {
		return GBClientState.active;
	}
}
