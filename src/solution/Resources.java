package solution;

import scotlandyard.*;

import java.io.*;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.*;

import java.awt.*;
import java.awt.image.*;
import javax.imageio.*;

public class Resources
{
    private static final Colour black = Colour.Black;
    
    private static final String[] pieces =
    {
        "null-lo",
        "mr-x-lo",
        "mr-x-last",
        "det-b-lo",
        "det-g-lo",
        "det-r-lo",
        "det-w-lo",
        "det-y-lo"
    };
    private static Map<Colour,BufferedImage> imgPiece;
    private static BufferedImage xLastIMG;
    private static BufferedImage nullIMG;
    private static CountDownLatch lockPieces;
    
    private static final String[] tickets =
    {
        "t-taxi-lo",
        "t-bus-lo",
        "t-tube-lo",
        "t-secret-lo",
        "t-double-lo",
        "t-taxi-d",
        "t-bus-d",
        "t-tube-d",
        "t-secret-d",
        "t-double-d"
    };
    private static Map<Ticket,BufferedImage> imgTicket;
    private static Map<Ticket,BufferedImage> imgTicketD;
    private static CountDownLatch lockTickets;
    
    private static final String[] misc =
    {
        "background",
        "s-taxi",
        "s-bus",
        "s-tube",
        "h-0",
        "h-1",
        "h-2",
        "menu",
        "timeline"
    };
    private static Map<String,BufferedImage> imgMisc;
    private static CountDownLatch lockMisc;
    
    public Resources()
    {
        imgPiece = new HashMap<Colour,BufferedImage>();
        lockPieces = new CountDownLatch(1);
        
        imgTicket = new HashMap<Ticket,BufferedImage>();
        imgTicketD = new HashMap<Ticket,BufferedImage>();
        lockTickets = new CountDownLatch(1);
        
        imgMisc = new HashMap<String,BufferedImage>();
        lockMisc = new CountDownLatch(1);
    }
    
    private BufferedImage getIMG(String s)
    {
        try { return ImageIO.read(new File("src/solution/" + s + ".png")); }
        catch (IOException e) { System.err.println(s + ".png not found."); }
        return null;
    }
    
    public void load()
    {
        imgPiece.put(Colour.Black,  getIMG("mr-x-lo"));
        imgPiece.put(Colour.Blue,   getIMG("det-b-lo"));
        imgPiece.put(Colour.Green,  getIMG("det-g-lo"));
        imgPiece.put(Colour.Red,    getIMG("det-r-lo"));
        imgPiece.put(Colour.White,  getIMG("det-w-lo"));
        imgPiece.put(Colour.Yellow, getIMG("det-y-lo"));
        xLastIMG                  = getIMG("mr-x-last");
        nullIMG                   = getIMG("null-lo");
        lockPieces.countDown();
        
        imgTicket.put(Ticket.Taxi,        getIMG("t-taxi-lo"));
        imgTicket.put(Ticket.Bus,         getIMG("t-bus-lo"));
        imgTicket.put(Ticket.Underground, getIMG("t-tube-lo"));
        imgTicket.put(Ticket.SecretMove,  getIMG("t-secret-lo"));
        imgTicket.put(Ticket.DoubleMove,  getIMG("t-double-lo"));
        imgTicketD.put(Ticket.Taxi,        getIMG("t-taxi-d"));
        imgTicketD.put(Ticket.Bus,         getIMG("t-bus-d"));
        imgTicketD.put(Ticket.Underground, getIMG("t-tube-d"));
        imgTicketD.put(Ticket.SecretMove,  getIMG("t-secret-d"));
        imgTicketD.put(Ticket.DoubleMove,  getIMG("t-double-d"));
        lockTickets.countDown();
        
        for (String s : misc) imgMisc.put(s, getIMG(s));
        lockMisc.countDown();
        
        System.err.println("Done!");
    }
    
    public static BufferedImage get(Ticket t)
    {
        return get(t, false);
    }
    
    public static BufferedImage get(Ticket t, boolean disabled)
    {
        try { lockTickets.await(); }
        catch (InterruptedException e) { }
        return disabled ? imgTicketD.get(t) : imgTicket.get(t);
    }
    
    public static BufferedImage get(Colour c)
    {
        return get(c, false);
    }
    
    public static BufferedImage get(Colour c, boolean alt)
    {
        try { lockPieces.await(); }
        catch (InterruptedException e) { }
        if (alt) return c == black ? xLastIMG : nullIMG;
        else return imgPiece.get(c);
    }
    
    public static BufferedImage get(String s)
    {
        try { lockMisc.await(); }
        catch (InterruptedException e) { }
        return imgMisc.get(s);
    }
}