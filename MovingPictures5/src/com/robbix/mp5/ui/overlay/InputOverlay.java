package com.robbix.mp5.ui.overlay;

import java.awt.Color;
import java.awt.DefaultKeyboardFocusManager;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.KeyEventPostProcessor;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import com.robbix.mp5.Utils;
import com.robbix.mp5.basics.BorderRegion;
import com.robbix.mp5.basics.ColorScheme;
import com.robbix.mp5.basics.LShapedRegion;
import com.robbix.mp5.basics.LinearRegion;
import com.robbix.mp5.basics.Position;
import com.robbix.mp5.basics.Region;
import com.robbix.mp5.map.LayeredMap;
import com.robbix.mp5.ui.DisplayPanel;
import com.robbix.mp5.unit.Footprint;
import com.robbix.mp5.unit.HealthBracket;
import com.robbix.mp5.unit.Unit;
import com.robbix.mp5.unit.UnitType;

public abstract class InputOverlay
implements MouseListener, MouseMotionListener, MouseWheelListener, KeyListener
{
	public static final Font OVERLAY_FONT = Font.decode("Arial-12");
	public static final Font COMMAND_FONT = Font.decode("Arial-bold-20");
	public static final Color TRANS_RED = new Color(255, 0, 0, 127);
	public static final Color TRANS_YELLOW = new Color(255, 255, 0, 127);
	public static final Color TRANS_GREEN = new Color(0, 255, 0, 127);
	public static final Color TRANS_WHITE = new Color(255, 255, 255, 127);
	
	public static final ColorScheme RED    = ColorScheme.withTranslucentBody(Color.RED);
	public static final ColorScheme BLUE   = ColorScheme.withTranslucentBody(Color.BLUE);
	public static final ColorScheme YELLOW = ColorScheme.withTranslucentBody(Color.YELLOW);
	public static final ColorScheme GREEN  = ColorScheme.withTranslucentBody(Color.GREEN);
	public static final ColorScheme WHITE  = ColorScheme.withTranslucentBody(Color.WHITE);
	
	private static int DRAG_THRESHOLD = 8*8;
	private static final int LEFT   = MouseEvent.BUTTON1;
	private static final int MIDDLE = MouseEvent.BUTTON2;
	private static final int RIGHT  = MouseEvent.BUTTON3;
	
	protected DisplayPanel panel;
	private Point currentPoint = null;
	private Point pressedPoint = null;
	private Rectangle dragArea = null;
	private int shiftOption;
	private boolean shiftDown;
	private String animatedCursor = null;
	
	protected int shiftOptionCount = 2;
	
	protected boolean closesOnEscape            = true;
	protected boolean closesOnRightClick        = true;
	protected boolean requiresLeftClickOnGrid   = true;
	protected boolean requiresRightClickOnGrid  = false;
	protected boolean requiresMiddleClickOnGrid = true;
	
	protected InputOverlay()
	{
	}
	
	protected InputOverlay(String animatedCursor)
	{
		this.animatedCursor = animatedCursor;
	}
	
	public void setDisplay(DisplayPanel panel)
	{
		this.panel = panel;
	}
	
	public DisplayPanel getDisplay()
	{
		return panel;
	}
	
	public void push(InputOverlay overlay)
	{
		panel.pushOverlay(overlay);
	}
	
	public void complete()
	{
		panel.completeOverlay(this);
	}
	
	public void init()
	{
		panel.setAnimatedCursor(animatedCursor);
	}
	
	public void dispose()
	{
	}
	
	public void paintOverTerrain(Graphics g){}
	public void paintOverUnits(Graphics g){}
	
	public void drawSelectedUnitBox(Graphics g, Unit unit)
	{
		if (unit.isDead() || unit.isFloating()) return;
		
		if (panel.getScale() < 0)
		{
			panel.draw(g, WHITE.getEdgeOnly(), unit.getPosition());
			return;
		}
		
		g.translate(panel.getViewX(), panel.getViewY());
		
		int tileSize = panel.getTileSize();
		int absWidth = unit.getWidth() * tileSize;
		int absHeight = unit.getHeight() * tileSize;
		
		/*
		 * Draw borders
		 */
		int nwCornerX = unit.getAbsX();
		int nwCornerY = unit.getAbsY();
		int neCornerX = nwCornerX + absWidth;
		int neCornerY = nwCornerY;
		int swCornerX = nwCornerX;
		int swCornerY = nwCornerY + absHeight;
		int seCornerX = nwCornerX + absWidth;
		int seCornerY = nwCornerY + absHeight;
		
		g.setColor(Color.WHITE);
		g.drawLine(nwCornerX, nwCornerY, nwCornerX + 4, nwCornerY);
		g.drawLine(nwCornerX, nwCornerY, nwCornerX,     nwCornerY + 4);
		g.drawLine(neCornerX, neCornerY, neCornerX - 4, neCornerY);
		g.drawLine(neCornerX, neCornerY, neCornerX,     neCornerY + 4);
		g.drawLine(swCornerX, swCornerY, swCornerX + 4, swCornerY);
		g.drawLine(swCornerX, swCornerY, swCornerX,     swCornerY - 4);
		g.drawLine(seCornerX, seCornerY, seCornerX - 4, seCornerY);
		g.drawLine(seCornerX, seCornerY, seCornerX,     seCornerY - 4);
		
		/*
		 * Draw health bar
		 */
		double hpFactor = unit.getHP() / (double) unit.getType().getMaxHP();
		hpFactor = Math.min(hpFactor, 1.0f);
		hpFactor = Math.max(hpFactor, 0.0f);
		
		boolean isRed = unit.getHealthBracket() == HealthBracket.RED;
		
		int hpBarLength = absWidth - 14;
		int hpLength = (int) (hpBarLength * hpFactor);
		double hpHue = 0.333 - (1.0 - hpFactor) * 0.333;
		int hpAlpha = (int) ((2.0 - hpFactor) * 127);
		
		Color hpColor = Color.getHSBColor((float) hpHue, 1.0f, 1.0f);
		hpColor = Utils.getTranslucency(hpColor, hpAlpha);
		
		g.setColor(Color.BLACK);
		g.fillRect(nwCornerX + 7, nwCornerY - 2, hpBarLength, 4);
		
		/*
		 * Flash health bar when red 
		 */
		if (Utils.getTimeBasedSwitch(300, 2) || !isRed)
		{
			g.setColor(hpColor);
			g.fillRect(nwCornerX + 8, nwCornerY - 1, hpLength - 1, 3);
		}
		
		g.setColor(Color.WHITE);
		g.drawRect(nwCornerX + 7, nwCornerY - 2, hpBarLength, 4);
		
		g.translate(-panel.getViewX(), -panel.getViewY());
	}
	
	/**
	 * Returns tooltip text so it can be drawn on top of unit placement sprite.
	 */
	public String drawUnitFootprint(Graphics g, UnitType type, Position pos)
	{
		Footprint fp = type.getFootprint();
		Region inner = fp.getInnerRegion().move(pos);
		Region outer = inner.stretch(1);
		LayeredMap map = panel.getMap();
		ColorScheme scheme = null;
		String toolTip = null;
		
		if (type.getName().endsWith("Mine"))
		{
			boolean onMap = map.getBounds().contains(inner);
			boolean available = map.canPlaceUnit(pos, fp);
			boolean hasDeposit = map.canPlaceMine(pos);
			
			scheme = onMap && available && hasDeposit ? GREEN : RED;
			
			if      (!onMap)      toolTip = "Out of bounds";
			else if (!available)  toolTip = "Occupied";
			else if (!hasDeposit) toolTip = "No resource deposit";
		}
		else
		{
			boolean onMap = map.getBounds().contains(inner);
			boolean available = map.canPlaceUnit(pos, fp);
			boolean connected = !type.needsConnection() || map.willConnect(pos, fp);
			boolean noDeposit = !containsDeposit(outer);
			
			scheme = onMap && available ? (connected && noDeposit ? GREEN : YELLOW) : RED;
			
			if      (!onMap)     toolTip = "Out of bounds";
			else if (!available) toolTip = "Occupied";
			else if (!noDeposit) toolTip = "Covers resource deposit";
			else if (!connected) toolTip = "No tube connection";
		}
		
		panel.draw(g, scheme, inner);
		
		for (Position tubePos : fp.getTubePositions())
			panel.draw(g, WHITE.getFillOnly(), tubePos.shift(inner.x, inner.y));
		
		if (type.isStructureType() || type.isGuardPostType())
			panel.drawOutline(g, WHITE, outer);
		
		return toolTip;
	}
	
	private boolean containsDeposit(Region region)
	{
		LayeredMap map = panel.getMap();
		
		for (Position pos : region)
			if (map.getBounds().contains(pos) && map.hasResourceDeposit(pos))
				return true;
		
		return false;
	}
	
	public void onLeftClick(){}
	public void onRightClick(){}
	public void onMiddleClick(){}
	public void onAreaDragged(){}
	public void onCommand(String command){}
	
	public boolean isCursorOnGrid()
	{
		return currentPoint != null
			&& panel.getPosition(currentPoint) != null;
	}
	
	public Point getCursorPoint()
	{
		return currentPoint;
	}
	
	public Position getCursorPosition()
	{
		if (currentPoint == null)
			return null;
		
		return panel.getPosition(currentPoint);
	}
	
	public boolean isDragging()
	{
		return dragArea != null;
	}
	
	public Rectangle getDragArea()
	{
		return dragArea;
	}
	
	public Region getDragRegion()
	{
		return dragArea == null ? null : panel.getRegion(dragArea);
	}
	
	public Region getEnclosedDragRegion()
	{
		return dragArea == null ? null : panel.getEnclosedRegion(dragArea);
	}
	
	public boolean isDragRegionLinear()
	{
		Region fullRegion = panel.getRegion(dragArea);
		return fullRegion.w == 1 || fullRegion.h == 1;
	}
	
	public LinearRegion getLinearDragRegion()
	{
		Position origin = panel.getPosition(pressedPoint);
		Region fullRegion = panel.getRegion(dragArea);
		int endX = origin.x;
		int endY = origin.y;
		
		if (fullRegion.w > fullRegion.h)
		{
			endX = fullRegion.x < origin.x
					? fullRegion.getX()
					: fullRegion.getMaxX() - 1;
			endY = origin.y;
		}
		else
		{
			endX = origin.x;
			endY = fullRegion.y < origin.y
				? fullRegion.getY()
				: fullRegion.getMaxY() - 1;
		}
		
		return new LinearRegion(origin, new Position(endX, endY));
	}
	
	public LShapedRegion getLShapedDragRegion()
	{
		return getLShapedDragRegion(true);
	}
	
	public LShapedRegion getLShapedDragRegion(boolean verticalFirst)
	{
		Position origin = panel.getPosition(pressedPoint);
		Region fullRegion = panel.getRegion(dragArea);
		
		int farX = origin.x == fullRegion.x
			? fullRegion.getMaxX() - 1
			: fullRegion.getX();
		int farY = origin.y == fullRegion.y
			? fullRegion.getMaxY() - 1
			: fullRegion.getY();
		
		return new LShapedRegion(origin, new Position(farX, farY), verticalFirst);
	}
	
	public BorderRegion getBorderDragRegion()
	{
		return new BorderRegion(panel.getRegion(dragArea));
	}
	
	public boolean isShiftDown()
	{
		return shiftDown;
	}
	
	public boolean isShiftOptionSet()
	{
		return shiftOption % 2 == 0;
	}
	
	public int getShiftOption()
	{
		return shiftOption % shiftOptionCount;
	}
	
	public final void mousePressed(MouseEvent e)
	{
		pressedPoint = e.getPoint();
	}
	
	public final void mouseReleased(MouseEvent e)
	{
		if (pressedPoint != null)
		{
			if (pressedPoint.distanceSq(e.getPoint()) < DRAG_THRESHOLD)
			{
				if (e.getButton() == LEFT)
				{
					if (!requiresLeftClickOnGrid || isCursorOnGrid())
						onLeftClick();
				}
				else if (e.getButton() == MIDDLE)
				{
					if (!requiresMiddleClickOnGrid || isCursorOnGrid())
						onMiddleClick();
				}
				else if (e.getButton() == RIGHT)
				{
					if (!requiresRightClickOnGrid || isCursorOnGrid())
						onRightClick();
					
					if (closesOnRightClick)
						complete();
				}
			}
			else
			{
				prepNormalDragArea(e.getX(), e.getY());
				
				if (dragArea != null)
					onAreaDragged();
			}
			
			pressedPoint = null;
			dragArea = null;
		}
	}
	
	public final void mouseDragged(MouseEvent e)
	{
		if (pressedPoint.distanceSq(e.getPoint()) < DRAG_THRESHOLD)
			return;
			
		if (isCursorOnGrid() && pressedPoint != null)
		{
			prepNormalDragArea(e.getX(), e.getY());
		}
	}
	
	public final void mouseEntered(MouseEvent e)
	{
		prepCursorPoint(e.getX(), e.getY());
	}
	
	public final void mouseExited(MouseEvent e)
	{
		currentPoint = null;
	}
	
	public final void mouseMoved(MouseEvent e)
	{
		prepCursorPoint(e.getX(), e.getY());
	}
	
	public final void mouseClicked(MouseEvent e){}
	public final void mouseWheelMoved(MouseWheelEvent e){}
	public final void keyReleased(KeyEvent e){}
	public final void keyPressed(KeyEvent e){}
	public final void keyTyped(KeyEvent e){}
	
	private boolean panelContains(int x, int y) // in terms of relative co-ords
	{
		Point p = new Point(x, y);
		return panel.getPosition(p) != null;
	}
	
	private void prepCursorPoint(int x, int y) // in terms of relative co-ords
	{
		if (panelContains(x, y))
		{
			if (currentPoint == null)
			{
				currentPoint = new Point();
			}
			
			currentPoint.x = x;
			currentPoint.y = y;
		}
		else
		{
			currentPoint = null;
		}
	}
	
	private void prepNormalDragArea(int x, int y) // in terms of relative co-ords
	{
		prepCursorPoint(x, y);
		
		if (currentPoint == null)
		{
			return;
		}
		
		if (dragArea == null)
		{
			dragArea = new Rectangle();
		}
		
		dragArea.x = pressedPoint.x;
		dragArea.y = pressedPoint.y;
		dragArea.width  = currentPoint.x - dragArea.x;
		dragArea.height = currentPoint.y - dragArea.y;
		
		if (dragArea.width < 0)
		{
			dragArea.x += dragArea.width;
			dragArea.width = -dragArea.width;
		}
		
		if (dragArea.height < 0)
		{
			dragArea.y += dragArea.height;
			dragArea.height = -dragArea.height;
		}
		
	}
	
	public static class ListenerAdapter
	implements KeyListener,
			   MouseListener,
			   MouseMotionListener,
			   MouseWheelListener,
			   KeyEventPostProcessor
	{
		private InputOverlay overlay;
		
		public ListenerAdapter()
		{
			DefaultKeyboardFocusManager.getCurrentKeyboardFocusManager()
									   .addKeyEventPostProcessor(this);
		}
		
		public void setOverlay(InputOverlay overlay)
		{
			this.overlay = overlay;
		}
		
		public boolean hasOverlay()
		{
			return overlay != null;
		}
		
		public InputOverlay getOverlay()
		{
			return overlay;
		}
		
		public boolean postProcessKeyEvent(KeyEvent e)
		{
			if (overlay == null)
				return false;
			
			if (e.getKeyCode() == KeyEvent.VK_ESCAPE)
				if (e.getID() == KeyEvent.KEY_PRESSED)
					if (overlay.closesOnEscape)
						if (overlay.panel.getCurrentOverlay() == overlay)
							overlay.complete();
			
			if (!overlay.shiftDown && e.isShiftDown())
				overlay.shiftOption++;
			
			overlay.shiftDown = e.isShiftDown();
			return false;
		}
		
		private boolean ho() { return hasOverlay(); }
		
		public void mouseWheelMoved(MouseWheelEvent e) { if (ho()) overlay.mouseWheelMoved(e); }
		public void mouseDragged   (MouseEvent e)      { if (ho()) overlay.mouseDragged(e);    }
		public void mouseMoved     (MouseEvent e)      { if (ho()) overlay.mouseMoved(e);      }
		public void mouseClicked   (MouseEvent e)      { if (ho()) overlay.mouseClicked(e);    }
		public void mouseEntered   (MouseEvent e)      { if (ho()) overlay.mouseEntered(e);    }
		public void mouseExited    (MouseEvent e)      { if (ho()) overlay.mouseExited(e);     }
		public void mousePressed   (MouseEvent e)      { if (ho()) overlay.mousePressed(e);    }
		public void mouseReleased  (MouseEvent e)      { if (ho()) overlay.mouseReleased(e);   }
		public void keyPressed     (KeyEvent e)        { if (ho()) overlay.keyPressed(e);      }
		public void keyReleased    (KeyEvent e)        { if (ho()) overlay.keyReleased(e);     }
		public void keyTyped       (KeyEvent e)        { if (ho()) overlay.keyTyped(e);        }
	}
}
