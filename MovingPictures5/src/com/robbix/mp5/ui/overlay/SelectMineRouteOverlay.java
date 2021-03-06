package com.robbix.mp5.ui.overlay;

import com.robbix.mp5.Game;
import com.robbix.mp5.ai.task.MineRouteTask;
import com.robbix.mp5.ui.DisplayGraphics;
import com.robbix.mp5.unit.Unit;
import com.robbix.utils.Position;

public class SelectMineRouteOverlay extends InputOverlay
{
	private Unit smelter, mine;
	private Unit[] trucks;
	
	public SelectMineRouteOverlay(Unit truck)
	{
		this(new Unit[]{truck});
		this.requiresPaintOnGrid = false;
	}
	
	public SelectMineRouteOverlay(Unit[] trucks)
	{
		super("dock");
		this.trucks = trucks;
	}
	
	public void paintImpl(DisplayGraphics g)
	{
		for (Unit truck : trucks)
			drawSelectedUnitBox(g, truck);
	}
	
	public void onLeftClick()
	{
		Unit selected = getPotentialSelection();
		
		if (selected != null)
		{
			if (selected.isMine())
			{
				if (smelter != null && smelter.is("Common") && !selected.is("Common"))
				{
					Game.game.playSound("beep4");
					return;
				}
				
				mine = selected;
				Game.game.playSound("beep6");
			}
			else if (selected.isSmelter())
			{
				if (mine != null && mine.is("Common") && !selected.is("Common"))
				{
					Game.game.playSound("beep4");
					return;
				}
				
				smelter = selected;
				Game.game.playSound("beep6");
			}
		}
		
		if (mine != null && smelter != null)
		{
			for (Unit truck : trucks)
				truck.assignNow(new MineRouteTask(mine, smelter));
			
			complete();
		}
	}
	
	private Unit getPotentialSelection()
	{
		Position pos = getCursorPosition();
		Position n = pos.shift(0, -1);
		Position e = pos.shift(1, 0);
		
		Unit occupant = panel.getMap().getUnit(pos);
		
		if (occupant != null && (occupant.isMine() || occupant.isSmelter()))
			return occupant;
		
		occupant = panel.getMap().getUnit(n);
		
		if (occupant != null && occupant.isSmelter())
			return occupant;
		
		occupant = panel.getMap().getUnit(e);
		
		if (occupant != null && occupant.isMine())
			return occupant;
		
		return null;
	}
}
