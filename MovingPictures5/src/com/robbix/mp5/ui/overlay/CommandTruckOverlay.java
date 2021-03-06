package com.robbix.mp5.ui.overlay;

import java.util.ArrayList;
import java.util.Collection;

import com.robbix.mp5.Game;
import com.robbix.mp5.ai.task.DockTask;
import com.robbix.mp5.ai.task.DumpTask;
import com.robbix.mp5.ai.task.MineTask;
import com.robbix.mp5.map.LayeredMap;
import com.robbix.mp5.map.Ore;
import com.robbix.mp5.player.Player;
import com.robbix.mp5.ui.DisplayGraphics;
import com.robbix.mp5.unit.Cargo;
import com.robbix.mp5.unit.Command;
import com.robbix.mp5.unit.Unit;
import com.robbix.utils.JListDialog;
import com.robbix.utils.Position;

public class CommandTruckOverlay extends InputOverlay
{
	private Unit[] trucks;
	
	public CommandTruckOverlay(Unit truck)
	{
		super("move");
		this.trucks = new Unit[]{truck};
		this.requiresPaintOnGrid = false;
	}
	
	public CommandTruckOverlay(Unit[] trucks)
	{
		super("move");
		this.trucks = trucks;
		this.requiresPaintOnGrid = false;
	}
	
	public void init()
	{
		super.init();
		panel.showStatus(trucks[0]);
	}
	
	public void dispose()
	{
		super.dispose();
		panel.showStatus((Unit)null);
	}
	
	public void paintImpl(DisplayGraphics g)
	{
		for (Unit truck : trucks)
			drawSelectedUnitBox(g, truck);
	}
	
	public void onCommand(Command command)
	{
		if (command == Command.SELF_DESTRUCT)
		{
			for (Unit truck : trucks)
				Game.game.selfDestruct(truck);
			
			complete();
		}
		else if (command == Command.KILL)
		{
			for (Unit truck : trucks)
				Game.game.kill(truck);
			
			complete();
		}
		else if (command == Command.DUMP)
		{
			for (Unit truck : trucks)
				if (!truck.isCargoEmpty())
				{
					Game.game.playSound("dump", truck.getPosition());
					truck.interrupt(new DumpTask());
				}
		}
		else if (command == Command.PATROL)
		{
			push(new SelectMineRouteOverlay(trucks));
		}
		else if (command == Command.TRANSFER)
		{
			Collection<Player> players = Game.game.getPlayers();
			players = new ArrayList<Player>(players);
			players.remove(trucks[0].getOwner());
			
			if (players.isEmpty())
				return;
			
			Object result = JListDialog.showDialog(players.toArray());
			
			if (result == null)
				return;
			
			Player player = (Player) result;
			
			for (Unit truck : trucks)
				truck.setOwner(player);
			
			complete();
		}
		else if (command == Command.DOCK && trucks.length == 1)
		{
			Unit truck = trucks[0];
			
			if (truck.isCargoEmpty())
				return;
			
			Position adj = truck.getPosition().shift(0, -1);
			LayeredMap map = panel.getMap();
			
			if (map.getBounds().contains(adj))
			{
				Unit smelter = map.getUnit(adj);
				
				if (smelter != null && smelter.getType().getName().contains("Smelter"))
				{
					if (!smelter.isDead() && !smelter.isDisabled())
					{
						truck.assignNow(new DockTask(smelter, Cargo.EMPTY));
					}
				}
			}
		}
		else if (command == Command.MINE && trucks.length == 1)
		{
			Unit truck = trucks[0];
			
			if (!truck.isCargoEmpty())
				return;
			
			Position adj = truck.getPosition().shift(1, 0);
			LayeredMap map = panel.getMap();
			
			if (map.getBounds().contains(adj))
			{
				Unit mine = map.getUnit(adj);
				Ore deposit = map.getOre(adj);
				
				if (mine != null && mine.getType().getName().contains("Mine"))
				{
					if (deposit == null)
						throw new IllegalStateException("mine doesn't have deposit");
					
					if (!mine.isDead() && !mine.isDisabled())
					{
						truck.assignNow(new MineTask(deposit.getLoad()));
					}
				}
			}
		}
	}
	
	public void onLeftClick()
	{
		Position pos = getCursorPosition();
		
		for (Unit truck : trucks)		
			Game.game.doMove(truck, pos);
		
		Game.game.playSound("beep2");
	}
}
