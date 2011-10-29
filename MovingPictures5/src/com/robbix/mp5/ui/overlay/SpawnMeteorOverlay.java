package com.robbix.mp5.ui.overlay;

import com.robbix.mp5.Mediator;

public class SpawnMeteorOverlay extends InputOverlay
{
	public SpawnMeteorOverlay()
	{
		super("meteor");
	}
	
	public void onLeftClick()
	{
		Mediator.doSpawnMeteor(getCursorPosition());
	}
	
	public void onRightClick()
	{
		complete();
	}
}
