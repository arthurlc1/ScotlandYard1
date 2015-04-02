package solution;

import scotlandyard.*;

import java.util.*;

public class MrX extends Piece
{
    private int lastSeen;
    
    public MrX(Player player, Colour colour, int location, boolean reveal, Map<Ticket,Integer> tickets)
    {
        super(player, colour, location, tickets);
        lastSeen = reveal ? location : 0;
    }
    
    public int lastSeen()
    {
        return lastSeen;
    }
    
    public void giveTicket(Ticket ticket)
    {
        tickets.replace(ticket, tickets.get(ticket) + 1);
    }
    
    public void play(MoveTicket move, boolean reveal)
    {
        play(move);
        if (reveal) lastSeen = find();
    }
    
    public static Map<Ticket,Integer> getMap()
    {
        return Piece.getMap(4, 3, 3, 2, 5);
    }
}