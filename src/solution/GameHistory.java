package solution;

import scotlandyard.*;

import java.util.*;
import java.io.*;

import javax.swing.filechooser.*;

public class GameHistory implements Spectator
{
    private final static Ticket[] ticketnames = {Ticket.Taxi, Ticket.Bus, Ticket.Underground, Ticket.SecretMove, Ticket.DoubleMove};
    private static Map<String,Colour> colours;
    
    private final List<Piece> pieces;
    private final List<Move> moves;
    private final List<Ticket> xTickets;
    
    public GameHistory(ScotlandYardModel model)
    {
        this();
        model.addHistory(this);
    }
    
    private GameHistory()
    {
    	pieces = new ArrayList<Piece>();
        moves = new ArrayList<Move>();
        xTickets = new ArrayList<Ticket>();
    }
    
    public void join(Colour c, int l, Map<Ticket, Integer> t)
    {
        Piece newPiece;
        t = new HashMap<Ticket, Integer>(t);
        if (c == Colour.Black)
        {
            newPiece = new MrX(null, c, l, false, t);
            pieces.add(0, newPiece);
        }
        else
        {
            newPiece = new Detective(null, c, l, t);
            pieces.add(newPiece);
        }
    }
    
    public void notify(Move m)
    {
        moves.add(m);
    }
    
    public void addxTicket(Move m)
    {
        if (m instanceof MoveTicket && m.colour == Colour.Black)
        {
            xTickets.add(((MoveTicket)m).ticket);
        }
        if (m instanceof MoveDouble)
        {
            for (Move mt : ((MoveDouble)m).moves)
            {
                xTickets.add(((MoveTicket)mt).ticket);
            }
        }
    }
    
    public ScotlandYardModel toGame(Player player) throws IOException
    {
        ScotlandYardModel model = ScotlandYardModel.defaultGame(pieces.size());
        
        for (Piece p : pieces) model.join(player, p.colour, p.find(), p.tickets);
        for (Move m : moves)
        {
            addxTicket(m);
            if (m instanceof MoveTicket) model.play((MoveTicket) m);
            if (m instanceof MoveDouble) model.play((MoveDouble) m);
            if (m instanceof MovePass) model.play((MovePass) m);
            model.nextPlayer();
        }
        model.addHistory(this);
        return model;
    }
    
    public List<Ticket> xTickets()
    {
        return xTickets;
    }
    
    public void toFile(File f) throws FileNotFoundException
    {
        PrintWriter w = new PrintWriter(f);
        for (Piece p : pieces) w.println(p.toString());
        for (Move m : moves)
        {
            if (m instanceof MoveTicket) w.println(((MoveTicket)m).toString());
            if (m instanceof MoveDouble) w.println(((MoveDouble)m).toString());
            if (m instanceof MovePass) w.println(((MovePass)m).toString());
        }
        w.println("END ");
        w.close();
    }
    
    public static GameHistory fromFile(File f) throws Exception
    {
        GameHistory history = new GameHistory();
        colours = new HashMap<String,Colour>();
        colours.put("Black",  Colour.Black);
        colours.put("Blue",   Colour.Blue);
        colours.put("Green",  Colour.Green);
        colours.put("Red",    Colour.Red);
        colours.put("White",  Colour.White);
        colours.put("Yellow", Colour.Yellow);
        Colour c;
        Scanner s = new Scanner(f);
        boolean dbl = false;
        MoveTicket buffer = null;
        while (s.hasNextLine())
        {
            String[] terms = s.nextLine().split(" ");
            if (terms[0].equals("END"))
            {
                break;
            }
            else if (terms[0].equals("Piece"))
            {
                c = colours.get(terms[1]);
                int location = Integer.parseInt(terms[2]);
                Map<Ticket,Integer> tickets = new HashMap<Ticket,Integer>();
                for (int i=0; i<ticketnames.length; i++)
                {
                    tickets.put(ticketnames[i], Integer.parseInt(terms[i+2]));
                }
                history.join(c, location, tickets);
            }
            else if (terms[0].equals("Move"))
            {
                c = colours.get(terms[2]);
                if (terms[0].equals("Pass")) history.notify(new MovePass(c));
                if (terms[0].equals("Double"))
                {
                    dbl = true;
                    buffer = null;
                }
            }
            else
            {
                c = colours.get(terms[0]);
                int location = Integer.parseInt(terms[1]);
                Ticket ticket = null;
                for (Ticket t : ticketnames) if (t.name().equals(terms[2])) ticket = t;
                MoveTicket mt = new MoveTicket(c, location, ticket);
                if (dbl)
                {
                    if (buffer != null) history.notify(new MoveDouble(c, buffer, mt));
                    else buffer = mt;
                }
                else history.notify(mt);
            }
        }
        return history;
    }
}