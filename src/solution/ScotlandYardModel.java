package solution;

import scotlandyard.*;

import java.io.IOException;
import java.util.*;

public class ScotlandYardModel extends ScotlandYard
{
    private final Graph<Integer,Route> graph;
    
    private final int numPlayers;
    private final List<Piece> pieces;
    
    private final static Colour black = Colour.Black;
    private MrX mrX;
    
    private final List<Spectator> spectators;
    private GameHistory history;
    
    private final List<Boolean> rounds;
    private int round;
    private int currentPlayer;
    
    // Constructor to initialise all fields.
    public ScotlandYardModel(int numberOfDetectives, List<Boolean> rounds, String graphFileName) throws IOException
    {
        super(numberOfDetectives, rounds, graphFileName);
        
        this.graph = new ScotlandYardGraphReader().readGraph(graphFileName);
        this.rounds = new ArrayList<Boolean>(rounds);
        
        numPlayers = numberOfDetectives + 1;
        pieces = new ArrayList<Piece>(numPlayers);
        mrX = null;
        
        spectators = new ArrayList<Spectator>();
        
        currentPlayer = 0;
        round = 0;
    }
    
    // Add a new piece. Return false iff the piece is not permitted to join.
    @Override
    public boolean join(Player player, Colour colour, int location, Map<Ticket, Integer> tickets)
    {
        return join(player, colour, location, tickets, null);
    }
    
    public boolean join(Player player, Colour colour, int location, Map<Ticket, Integer> tickets, GameHistory history)
    {
        for (Piece p : pieces) if (p.colour == colour) return false;
        Piece newPiece;
        tickets = new HashMap<Ticket, Integer>(tickets);
        if (colour == black)
        {
            newPiece = new MrX(player, colour, location, rounds.get(0), tickets);
            pieces.add(0, newPiece);
            mrX = (MrX) pieces.get(0);
        }
        else
        {
            if (pieces.size() == numPlayers - (mrX == null ? 1 : 0)) return false;
            newPiece = new Detective(player, colour, location, tickets);
            pieces.add(newPiece);
        }
        return true;
    }
    
    // Add a spectator to the list of spectators to be notified of moves.
    @Override
    public void spectate(Spectator spectator)
    {
        spectators.add(spectator);
    }
    
    public void addHistory(GameHistory history)
    {
        this.history = history;
    }
    
    // Return true iff the expected number of players have joined the game.
    @Override
    public boolean isReady()
    {
        return (pieces.size() == numPlayers);
    }
    
    // Return a piece given its colour. Return null if no such piece exists.
    protected Piece getPiece(Colour colour)
    {
        for (Piece p : pieces) if (p.colour == colour) return p;
        return null;
    }
    
    // Return the list of valid single or double moves of a piece, given its colour.
    @Override
    protected List<Move> validMoves(Colour c)
    {
        Piece p = getPiece(c);
        if (p != mrX && (xCaught() || timeUp())) return Arrays.asList(new MovePass(c));
        List<Move> moves = new ArrayList<Move>();
        List<MoveTicket> singleMoves = validSingleMoves(c, p.find(), p.tickets);
        List<MoveDouble> doubleMoves = new ArrayList<MoveDouble>();
        if (p.tickets.get(Ticket.DoubleMove) > 0)
        {
            // For each valid first move, include a double move for each valid
            // move from the new location.
            Map<Ticket,Integer> newTickets;
            for (MoveTicket m1 : singleMoves)
            {
                newTickets = new HashMap<Ticket,Integer>(p.tickets);
                newTickets.replace(m1.ticket, p.tickets.get(m1.ticket) - 1);
                for (MoveTicket m2 : validSingleMoves(c, m1.target, newTickets))
                {
                    doubleMoves.add(new MoveDouble(c, m1, m2));
                }
            }
        }
        moves.addAll(singleMoves);
        moves.addAll(doubleMoves);
        if (p != mrX && moves.size() == 0) moves.add(new MovePass(c));
        return moves;
    }
    
    // Return the valid single moves from a location given a number of tickets.
    protected List<MoveTicket> validSingleMoves(Colour colour, int location, Map<Ticket,Integer> tickets)
    {
        List<MoveTicket> moves = new ArrayList<MoveTicket>();
        
        for (Edge<Integer,Route> e : graph.getEdges(location))
        {
            int other = e.other(location);
            boolean occ = false;
            for (Piece p : pieces) if (p != mrX && p.find() == other) occ = true;
            if (occ) continue;
            
            Ticket defaultTicket = Ticket.fromRoute(e.data());
            Ticket secret = Ticket.SecretMove;
            
            if (tickets.get(defaultTicket) > 0)
            {
                moves.add(new MoveTicket(colour, other, defaultTicket));
            }
            if (defaultTicket != secret && tickets.get(secret) > 0)
            {
                moves.add(new MoveTicket(colour, other, secret));
            }
        }
        return moves;
    }
    
    // Notify a player with their valid moves and return their choice.
    @Override
    protected Move getPlayerMove(Colour colour)
    {
        Piece p = getPiece(colour);
        List<Move> moves = validMoves(colour);
        Move move = p.player.notify(p.find(), moves);
        return moves.contains(move) ? move : null;
    }
    
    // Play a given single move. Notify spectators.
    @Override
    protected void play(MoveTicket move)
    {
        Piece toMove = getPiece(move.colour);
        if (toMove == mrX)
        {
            boolean reveal = rounds.get(++round);
            mrX.play(move, reveal);
            if (history != null) history.notify(move);
            move = new MoveTicket(black, mrX.lastSeen(), move.ticket);
        }
        else ((Detective) toMove).play(move, mrX);
        for (Spectator s : spectators) s.notify(move);
    }
    
    // Play a given double move. Notify spectators.
    @Override
    protected void play(MoveDouble move)
    {
        for (Spectator s : spectators) s.notify(move);
        getPiece(move.colour).play(move);
        play((MoveTicket) move.moves.get(0));
        play((MoveTicket) move.moves.get(1));
    }
    
    // No change to locations, but notify spectators that a pass has been played.
    @Override
    protected void play(MovePass move)
    {
        for (Spectator s : spectators) s.notify(move);
    }
    
    // Rotate through the list of players.
    @Override
    protected void nextPlayer()
    {
        currentPlayer = (currentPlayer + 1) % pieces.size();
    }
    
    // Return the list of colours in the game.
    @Override
    public List<Colour> getPlayers()
    {
        List<Colour> colours = new ArrayList<Colour>(numPlayers);
        for (Piece p : pieces) colours.add(p.colour);
        return colours;
    }
    
    // Return the colour of the current player.
    @Override
    public Colour getCurrentPlayer()
    {
        return pieces.get(currentPlayer).colour;
    }
    
    // Return the location of a piece given its colour.
    @Override
    public int getPlayerLocation(Colour colour)
    {
        Piece toFind = getPiece(colour);
        return (toFind == mrX) ? mrX.lastSeen() : toFind.find();
    }
    
    // Return the number of tickets of a given type that a piece has, given its colour.
    @Override
    public int getPlayerTickets(Colour colour, Ticket ticket)
    {
        return getPiece(colour).tickets.get(ticket);
    }
    
    // Return a COPY of the current player's ticket map.
    public Map<Ticket, Integer> getCurrentPlayerTickets()
    {
        return new HashMap<Ticket,Integer>(pieces.get(currentPlayer).tickets);
    }
    
    // Return the current round number.
    @Override
    public int getRound()
    {
        return round;
    }
    
    // Return the value of the next round.
    public boolean getNextRound()
    {
        return rounds.get(round + 1);
    }
    
    // Return the list of rounds.
    @Override
    public List<Boolean> getRounds()
    {
        return rounds;
    }
    
    // Return true iff there are no more rounds in the game.
    protected boolean timeUp()
    {
        int end = rounds.size() - 1;
        return ((currentPlayer == 0 && round == end) || round > end);
    }
    
    // Return true iff none of the detectives can move.
    protected boolean dStuck()
    {
        boolean allStuck = true;
        for (Piece p : pieces)
        {
            if (p == mrX) continue;
            if (validMoves(p.colour).get(0) instanceof MoveTicket) allStuck = false;
        }
        return allStuck;
    }
    
    // Return true iff one of the detectives has moved onto Mr X's location.
    protected boolean xCaught()
    {
        for (Piece p : pieces) if (p != mrX && p.find() == mrX.find()) return true;
        return false;
    }
    
    // Return true iff Mr X cannot move on his turn.
    protected boolean xTrapped()
    {
        return (currentPlayer == 0 && validMoves(black).size() == 0);
    }
    
    // Return true iff any one of the endgame conditions has been met.
    @Override
    public boolean isGameOver()
    {
        return (isReady() && (timeUp() || dStuck() || xCaught() || xTrapped() || pieces.size() == 1));
    }
    
    // If Mr X has won return {black}. Otherwise return {colours} / {black}.
    @Override
    public Set<Colour> getWinningPlayers()
    {
        Set<Colour> winners = new HashSet<Colour>();
        if (isGameOver())
        {
            boolean xWins = ((timeUp() || dStuck()) && !(xCaught() || xTrapped()));
            if (xWins) winners.add(black);
            else for (Colour c : getPlayers()) if (c != black) winners.add(c);
        }
        return winners;
    }
    
    public static ScotlandYardModel defaultGame(int n) throws IOException
    {
        List<Boolean> r = str2bools("0001000010000100001000001");
        int numD = n - 1;
        return new ScotlandYardModel(numD, r, "src/solution/graph.txt");
    }
    
    private static List<Boolean> str2bools(String str)
    {
        List<Boolean> out = new ArrayList<Boolean>();
        for (char c : str.toCharArray()) out.add(c == '1');
        return out;
    }
}