package solution;

import scotlandyard.*;

import java.io.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Scanner;
import java.util.concurrent.*;

import javax.swing.*;
import javax.imageio.*;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.*;

public class ScotlandYardDisplay extends JPanel implements ActionListener
{
    private GameHistory history;
    
    private final static Colour black = Colour.Black;
    private final static RenderingHints textRenderHints = new RenderingHints(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    private final static RenderingHints imageRenderHints = new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    private final static RenderingHints renderHints = new RenderingHints(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    
    private static BufferedImage bg;
    private static BufferedImage pGlow;
    private static BufferedImage tGlow;
    private static BufferedImage hGlow;
    private static Point[] ls = new Point[199];
    private static BufferedImage[] stations = new BufferedImage[199];
    private static BufferedImage[] highlights = new BufferedImage[199];
    private static Map<Colour,BufferedImage> imgP;
    
    private List<Integer> targets;
    private Map<Colour,Integer> locMap;
    private int locX;
    private int cL;
    private boolean xTurn = false;
    
    private AffineTransform mv, imv, sc, isc, sc_mv, imv_isc;
    private double MIN_Z, MAX_Z;
    private int MIN_X, MAX_X, MIN_Y, MAX_Y;
    private double z;
    private Point s, w, o0, o, v0, v, m;
    
    private boolean dragging = false;
    private boolean zooming = false;
    
    private GridBagConstraints gbc;
    private JButton menuB;
    private JPopupMenu menu;
    private JMenuItem save, quit;
    private TicketPanel ticketP;
    private JPopupMenu ticketM;
    protected TimelinePanel timeline;
    
    private boolean ready;
    private CountDownLatch guiReady;
    
    // Initialise SYC from list of colours, for new game.
    protected ScotlandYardDisplay(List<Colour> colours)
    {
        new ScotlandYardControl(colours).addDisplay(this);
    }
    
    // Initialise SYC from save file.
    protected ScotlandYardDisplay(GameHistory history)
    {
        this.history = history;
        new ScotlandYardControl(history).addDisplay(this);
    }
    
    protected void init(Map<Colour,Integer> locMap, int locX)
    {
        guiReady = new CountDownLatch(1);
        
        this.setPreferredSize(new Dimension(1200,650));
        this.setOpaque(false);
        this.setLayout(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        
        this.locMap = locMap;
        this.targets = new ArrayList<Integer>();
        this.locX = cL = locX;
        
        z = 0.4;
        o = new Point();
        m = new Point();
        v = new Point();
        
        try { getResources(); } catch(FileNotFoundException e) { }
        try { updateTransforms(); } catch (Exception e) { }
        
        makeTimeline();
    }
    
    protected void getResources() throws FileNotFoundException
    {
        bg = Resources.get("background");
        s = new Point(bg.getWidth(), bg.getHeight());
        String[] imgS = {"s-taxi","s-bus","s-tube"};
        Scanner nodeScanner = new Scanner(new File("src/solution/nodes.txt"));
        while (nodeScanner.hasNextLine())
        {
            String[] args = nodeScanner.nextLine().split(" ");
            int i = Integer.parseInt(args[0]) - 1;
            int x = Integer.parseInt(args[1]);
            int y = Integer.parseInt(args[2]);
            int t = Integer.parseInt(args[3]);
            ls[i] = new Point(x, y);
            stations[i] = Resources.get(imgS[t]);
        }
        pGlow = Resources.get("h-0");
        tGlow = Resources.get("h-1");
        hGlow = Resources.get("h-2");
    }
    
    protected void makeMenu(ScotlandYardControl control)
    {
        menuB = new JButton(new ImageIcon(Resources.get("menu")));
        menu = new JPopupMenu();
        save = new JMenuItem("Save");
        quit = new JMenuItem("Quit");
        menu.add(save);
        menu.add(quit);
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        gbc.insets = new Insets(5, 5, 5, 5);
        this.add(menuB, gbc);
        Dimension size = menuB.getPreferredSize();
        menuB.setBounds(5, 5, size.width - 3, size.height);
        menuB.setActionCommand("menu");
        menuB.addActionListener(this);
        save.setActionCommand("save");
        save.addActionListener(control);
        quit.setActionCommand("quit");
        quit.addActionListener(control);
    }
    
    protected void makeTimeline()
    {
        timeline = new TimelinePanel();
        gbc.anchor = GridBagConstraints.PAGE_END;
        gbc.insets = new Insets(0, 0, 0, 0);
        this.add(timeline, gbc);
        if (history != null)
        {
            List<Ticket> xT = history.xTickets();
            for (int i=0; i<xT.size(); i++) timeline.add(xT.get(i));
        }
    }
    
    protected void makeReady(MouseAdapter a)
    {
        try { guiReady.await(); } catch (Exception e) { }
        ready = true;
        this.addMouseListener(a);
        this.addMouseMotionListener(a);
        this.addMouseWheelListener(a);
    }
    
    protected void updateLoc(int location)
    {
        for (int i=0; i<199; i++) highlights[i] = null;
        highlights[location - 1] = pGlow;
        repaint();
    }
    
    protected void updateTargets(List<Integer> targets)
    {
        this.targets = targets;
    }
    
    protected void setxTurn(boolean xTurn)
    {
        this.xTurn = xTurn;
        repaint();
    }
    
    protected void makeTicketMenu(int location, boolean x, Map<Ticket,Integer> tickets, Map<Ticket,Boolean> usable, ScotlandYardControl control)
    {
        assert(tickets != null);
        assert(usable != null);
        ticketM = new JPopupMenu();
        ticketP = new TicketPanel(location, x, tickets, usable, control);
        ticketM.add(ticketP);
    }
    
    protected void showTicketMenu()
    {
        int i = ticketP.location;
        Point p = new Point(0, 0);
        sc_mv_o(i-1, 40, -40).transform(p, p);
        ticketM.show(this, p.x, p.y - ticketP.h);
    }
    
    protected void play(Colour c, int target, Ticket ticket, boolean xReveal)
    {
        if (c == black)
        {
            locX = target;
            timeline.add(ticket);
        }
        if (c != black || xReveal)
        {
            locMap.put(c,target);
        }
        repaint();
    }
    
    protected void hideTicketMenu()
    {
        ticketM.setVisible(false);
        repaint();
    }
    
    protected void updateTransforms() throws NoninvertibleTransformException
    {
        sc = new AffineTransform();
        sc.scale(z, z);
        isc = sc.createInverse();
        mv = new AffineTransform();
        mv.translate(o.x, o.y);
        imv = mv.createInverse();
        sc_mv = new AffineTransform();
        sc_mv.concatenate(mv);
        sc_mv.concatenate(sc);
        imv_isc = new AffineTransform();
        imv_isc.concatenate(isc);
        imv_isc.concatenate(imv);
    }
    
    protected AffineTransform sc_mv_o(int loc, int oX, int oY)
    {
        AffineTransform out = new AffineTransform();
        out.concatenate(sc_mv);
        out.translate(oX, oY);
        out.translate(ls[loc].x, ls[loc].y);
        return out;
    }
    
    protected AffineTransform sc_mv_o(int oX, int oY)
    {
        AffineTransform out = new AffineTransform();
        out.concatenate(sc_mv);
        out.translate(oX, oY);
        return out;
    }
    
    protected void updateLimits()
    {
        w = new Point(getWidth(), getHeight());
        MIN_Z = Math.max((double)w.x / (double)s.x, (double)w.y / (double)s.y);
        MIN_Z = Math.ceil(MIN_Z * 10 - 1) / 10;
        MAX_Z = 1.0;
        MIN_X = w.x - Math.round(s.x * (float)z);
        MAX_X = 0;
        MIN_Y = w.y - Math.round(s.y * (float)z);
        MAX_Y = 0;
    }
    
    protected void enforceLimits()
    {
        if (z < MIN_Z) z = MIN_Z;
        if (z > MAX_Z) z = MAX_Z;
        if (o.x < MIN_X) o.x = MIN_X;
        if (o.x > MAX_X) o.x = MAX_X;
        if (o.y < MIN_Y) o.y = MIN_Y;
        if (o.y > MAX_Y) o.y = MAX_Y;
    }
    
    @Override
    protected void paintComponent(Graphics g)
    {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHints(textRenderHints);
        g2d.setRenderingHints(imageRenderHints);
        g2d.setRenderingHints(renderHints);
        
        this.updateLimits();
        this.enforceLimits();
        try { updateTransforms(); } catch (Exception e) { }
        
        g2d.drawImage(bg, sc_mv, this);
        this.guiReady.countDown();
        
        for (int i=0; i<199; i++)
        {
            BufferedImage h = highlights[i];
            if (h != null) g2d.drawImage(h, sc_mv_o(i, -100, -100), this);
            g2d.drawImage(stations[i], sc_mv_o(i, -50, -40), this);
        }
        for (Colour c : locMap.keySet())
        {
            int loc = locMap.get(c);
            BufferedImage img = Resources.get(c, c == black);
            if (loc != 0) g2d.drawImage(img, sc_mv_o(loc-1, -50, -150), this);
        }
        if (xTurn) g2d.drawImage(Resources.get(black), sc_mv_o(locX-1, -50, -150), this);
        g2d.dispose();
        super.paintComponent(g);
    }
    
    protected void updateMouse(MouseEvent e)
    {
        imv_isc.transform(e.getPoint(), m);
    }
    
    protected boolean mouseOver(int i)
    {
        boolean inXrange = (ls[i].x - 40 < m.x) && (m.x < ls[i].x + 40);
        boolean inYrange = (ls[i].y - 40 < m.y) && (m.y < ls[i].y + 40);
        return inXrange && inYrange;
    }
    
    protected void paintTargets()
    {
        for (int i : targets) highlights[i-1] = mouseOver(i-1) ? hGlow : tGlow;
        this.repaint();
    }
    
    protected void zoomTick(float dz)
    {
        if (zooming) return;
        double z_ = z + dz;
        if (MIN_Z <= z_ && z_ <= MAX_Z)
        {
            z = z_;
            o.x -= Math.round(m.x * dz);
            o.y -= Math.round(m.y * dz);
        }
        this.repaint();
    }
    
    protected void startDrag(MouseEvent e)
    {
        isc.transform(e.getPoint(), v);
        o0 = new Point(o);
        v0 = new Point(v);
    }
    
    protected void dragTick(MouseEvent e)
    {
        if (dragging) return;
        isc.transform(e.getPoint(), v);
        o.x = o0.x + (v.x - v0.x);
        o.y = o0.y + (v.y - v0.y);
        this.repaint();
    }
    
    public void actionPerformed(ActionEvent e)
    {
        String cmd = e.getActionCommand();
        if (cmd.equals("menu")) menu.show(menuB, 0, menuB.getBounds().height);
    }
}