package com.robbix.mp5.ai.task;

import com.robbix.mp5.Mediator;
import com.robbix.mp5.basics.Direction;
import com.robbix.mp5.basics.Filter;
import com.robbix.mp5.basics.Position;

import static com.robbix.mp5.unit.Activity.*;

import com.robbix.mp5.unit.Activity;
import com.robbix.mp5.unit.Cargo;
import com.robbix.mp5.unit.Unit;

public class ConVecConstructTask extends Task
{
	private Unit target;
	private Position targetPos;
	
	public ConVecConstructTask(Unit target, Position targetPos)
	{
		super(true, new Filter<Unit>()
		{
			public boolean accept(Unit unit)
			{
				return unit.getType().getName().contains("ConVec");
			}
		});
		
		if (!target.isStructure() && !target.getType().isGuardPostType())
			throw new IllegalArgumentException("construct target not struct");
		
		this.target = target;
		this.targetPos = targetPos;
	}
	
	public void step(Unit unit)
	{
		if (unit.getActivity() != CONSTRUCT)
		{
			unit.setActivity(CONSTRUCT);
			unit.resetAnimationFrame();
			unit.setDirection(Direction.SW);
			unit.setCargo(Cargo.EMPTY);
			unit.getMap().putUnit(target, targetPos);
			int buildFrames = Mediator.game.getSpriteLibrary().getUnitSpriteSet(target.getType()).get(Activity.BUILD).getFrameCount();
			target.assignNow(new BuildTask(buildFrames, 50));
		}
		else if (target.getActivity() != BUILD)
		{
			unit.setActivity(MOVE);
			unit.completeTask(this);
		}
		else
		{
			unit.incrementAnimationFrame();
		}
	}
}
