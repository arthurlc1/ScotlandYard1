package solution;

import scotlandyard.*;

import java.util.*;

public abstract class Piece
{
    public final Player player;
    public final Colour colour;
    private int location;
    public final Map<Ticket,Integer> tickets;
    
    public Piece(Player player, Colour colour, int location, Map<Ticket,Integer> tickets)
    {
        this.player = player;
        this.colour = colour;
        this.location = location;
        this.tickets = tickets;
    }
    
    public int find()
    {
        return location;
    }
    
    public void play(MoveDouble move)
    {
        tickets.replace(Ticket.DoubleMove, tickets.get(Ticket.DoubleMove) - 1);
    }
    
    public void play(MoveTicket move)
    {
        location = move.target;
        tickets.replace(move.ticket, tickets.get(move.ticket) - 1);
    }
    
    public String toString()
    {
        String str = "Piece " + colour.name() + " " + location;
        for (int i=0; i<ticket.length; i++) str = str + " " + tickets.get(ticket[i]);
        return str;
    }
    
    public static Map<Ticket,Integer> getMap(int t, int b, int u, int d, int s)
    {
        Map<Ticket,Integer> tickets = new HashMap<Ticket,Integer>();
        tickets.put(Ticket.Taxi,        t);
        tickets.put(Ticket.Bus,         b);
        tickets.put(Ticket.Underground, u);
        tickets.put(Ticket.DoubleMove,  d);
        tickets.put(Ticket.SecretMove,  s);
        return tickets;
    }
    
    private final static Ticket[] ticket = {Ticket.Taxi, Ticket.Bus, Ticket.Underground, Ticket.SecretMove, Ticket.DoubleMove};
}