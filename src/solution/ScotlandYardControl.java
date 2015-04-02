package solution;

import scotlandyard.*;

import java.util.*;
import java.util.concurrent.*;
import java.io.*;

import javax.swing.*;
import javax.swing.filechooser.*;
import java.awt.event.*;

public class ScotlandYardControl extends MouseAdapter implements Player, ActionListener
{
    private Thread mainThread;
    private Thread modelThread;
    
    private final static Colour black = Colour.Black;
    
    private ScotlandYardModel model;
    private ScotlandYardDisplay display;
    private GameHistory history;
    
    private Colour cP;
    private List<Move> validMoves;
    private List<Integer> targets;
    private Map<Ticket,Integer> tickets;
    private Map<Integer,Map<Ticket,Boolean>> usable;
    private boolean dbl;
    
    private CountDownLatch moveChosen;
    private Move chosenMove;
    
    public ScotlandYardControl(GameHistory history)
    {
        try { model = history.toGame(this); }
        catch (IOException e) { }
        
        launchModel();
    }
    
    public ScotlandYardControl(List<Colour> colours)
    {
        try { model = ScotlandYardModel.defaultGame(colours.size()); }
        catch (IOException e) { }
        history = new GameHistory(model);
        init(colours);
        
        launchModel();
    }
    
    public void init(List<Colour> colours)
    {
        Integer[] xS = {35,51,71,78,104,127,132,146,166,170};
        Integer[] dS = {13,26,29,34,50,53,91,94,103,112,117,123,138,141,155,174};
        List<Integer> xStarts = new ArrayList<Integer>(Arrays.asList(xS));
        List<Integer> dStarts = new ArrayList<Integer>(Arrays.asList(dS));
        Random rand = new Random();
        for (Colour c : colours)
        {
            int s;
            if (c == black) s = xStarts.get(rand.nextInt(xStarts.size()));
            else
            {
                s = dStarts.get(rand.nextInt(dStarts.size()));
                dStarts.remove(dStarts.indexOf(s));
            }
            Map<Ticket,Integer> t = (c == black ? MrX.getMap() : Detective.getMap());
            if (model.join(this, c, s, t)) history.join(c, s, t);
        }
    }
    
    public void addDisplay(ScotlandYardDisplay display)
    {
        this.display = display;
        display.init(getLocMap(), model.getPiece(black).find());
        display.makeMenu(this);
    }
    
    public Map<Colour,Integer> getLocMap()
    {
        Map<Colour,Integer> out = new HashMap<Colour,Integer>();
        for (Colour c : getColours()) out.put(c, model.getPlayerLocation(c));
        return out;
    }
    
    public List<Colour> getColours()
    {
        return model.getPlayers();
    }
    
    public void launchModel()
    {
        new Thread(this::startGame).start();
    }
    
    public void startGame()
    {
        model.start();
        String title;
        String message;
        if (model.getWinningPlayers().contains(black))
        {
            title = "Mr. X wins!";
            message = "Mr. X has evaded capture for long enough to escape! Game over.";
        }
        else
        {
            title = "Detectives win!";
            message = "The detective have successfully captured Mr. X! Game over.";
        }
        String[] options = {"Save Replay","Main Menu"};
        if (JOptionPane.showOptionDialog(display,message,title,JOptionPane.YES_NO_OPTION,JOptionPane.PLAIN_MESSAGE,null,options,1) == 0)
        {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(new FileNameExtensionFilter("Scotland Yard Replay (*.syr)", "syr"));
            if (chooser.showSaveDialog(display) == JFileChooser.APPROVE_OPTION)
            {
                File chosen = chooser.getSelectedFile();
                String path = chosen.getPath();
                if (!path.endsWith(".syr")) chosen = new File(path + ".syr");
                try { history.toFile(chosen); }
                catch (FileNotFoundException e) { }
            }
        }
        exitToMenu();
    }
    
    public Move notify(int location, List<Move> validMoves)
    {
        display.makeReady(this);
        chosenMove = null;
        dbl = false;
        if (validMoves.get(0) instanceof MovePass)
        {
            // Inform player of lack of valid moves.
            return validMoves.get(0);
        }
        if ((cP = validMoves.get(0).colour) == black)
        {
            String t = "Warning! Mr. X's turn.";
            String m = "Mr. X's turn is about to start. Other players, look away now!";
            JOptionPane.showMessageDialog(display, m, t, JOptionPane.WARNING_MESSAGE);
            display.setxTurn(true);
        }
        display.updateLoc(location);
        this.validMoves =  validMoves;
        tickets = model.getCurrentPlayerTickets();
        processMoves();
        moveChosen = new CountDownLatch(1);
        try { moveChosen.await(); }
        catch (Exception e) { throw new Error("Interrupted while waiting for move!"); }
        return chosenMove;
    }
    
    public void processMoves()
    {
        usable = new HashMap<Integer,Map<Ticket,Boolean>>();
        for (int i=0; i<199; i++)
        {
            usable.put(i+1, new HashMap<Ticket,Boolean>());
            for (Ticket t : Ticket.values()) usable.get(i+1).put(t, false);
        }
        targets = new ArrayList<Integer>();
        for (Move m : validMoves)
        {
            MoveTicket mt = null;
            if (m instanceof MoveTicket && !dbl)
            {
                mt = (MoveTicket)m;
                targets.add(mt.target);
                usable.putIfAbsent(mt.target, new HashMap<Ticket,Boolean>());
                usable.get(mt.target).put(mt.ticket, true);
            }
            if (m instanceof MoveDouble)
            {
                mt = (MoveTicket) ((MoveDouble)m).moves.get(0);
                if (dbl && mt.target == ((MoveTicket)chosenMove).target)
                {
                    mt = (MoveTicket) ((MoveDouble)m).moves.get(1);
                    targets.add(mt.target);
                    usable.putIfAbsent(mt.target, new HashMap<Ticket,Boolean>());
                    usable.get(mt.target).put(mt.ticket, true);
                }
                else
                {
                    usable.putIfAbsent(mt.target, new HashMap<Ticket,Boolean>());
                    usable.get(mt.target).put(Ticket.DoubleMove, true);
                }
            }
        }
        display.updateTargets(targets);
    }
    
    public void play(int target, Ticket ticket)
    {
        if (ticket == Ticket.DoubleMove)
        {
            dbl = true;
            (targets = new ArrayList<Integer>()).add(target);
            usable.get(target).replace(Ticket.DoubleMove, false);
        }
        else
        {
            display.hideTicketMenu();
            MoveTicket move = new MoveTicket(cP, target, ticket);
            display.play(cP, target, ticket, model.getNextRound());
            if (dbl)
            {
                if (chosenMove == null)
                {
                    chosenMove = move;
                    processMoves();
                }
                else
                {
                    chosenMove = new MoveDouble(cP, chosenMove, move);
                    display.setxTurn(false);
                    moveChosen.countDown();
                }
            }
            else
            {
                chosenMove = move;
                display.setxTurn(false);
                moveChosen.countDown();
            }
        }
    }
    
    public void saveGame()
    {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Scotland Yard Savefile (*.syg)", "syg"));
        if (chooser.showSaveDialog(display) == JFileChooser.APPROVE_OPTION)
        {
            File chosen = chooser.getSelectedFile();
            String path = chosen.getPath();
            if (!path.endsWith(".syg")) chosen = new File(path + ".syg");
            try { history.toFile(chosen); }
            catch (FileNotFoundException e) { }
        }
    }
    
    public void loadGame()
    {
        
    }
    
    public void exitToMenu()
    {
        ScotlandYardApplication newGame = new ScotlandYardApplication();
        SwingUtilities.invokeLater(newGame::run);
        SwingUtilities.getWindowAncestor(display).dispose();
    }
    
    public void actionPerformed(ActionEvent e)
    {
        String cmd = e.getActionCommand();
        if      (cmd.equals("save")) saveGame();
        else if (cmd.equals("load")) loadGame();
        else                         exitToMenu();
    }
    
    @Override
    public void mouseClicked(MouseEvent e)
    {
        display.updateMouse(e);
        for (int i=0; i<199; i++)
        {
            if (display.mouseOver(i) && targets.contains(i+1))
            {
                display.makeTicketMenu(i+1, cP == black, tickets, usable.get(i+1), this);
                display.showTicketMenu();
                break;
            }
        }
    }
    
    @Override
    public void mouseMoved(MouseEvent e)
    {
        display.updateMouse(e);
        display.paintTargets();
    }
    
    @Override
    public void mousePressed(MouseEvent e)
    {
        display.startDrag(e);
    }
    
    @Override
    public void mouseDragged(MouseEvent e)
    {
        display.dragTick(e);
    }
    
    @Override
    public void mouseWheelMoved(MouseWheelEvent e)
    {
        display.updateMouse(e);
        display.zoomTick(-0.1f * (float)e.getPreciseWheelRotation());
    }
    
    private static Ticket s2t(String s)
    {
        if (s.equals("taxi"))        return Ticket.Taxi;
        else if (s.equals("bus"))    return Ticket.Bus;
        else if (s.equals("tube"))   return Ticket.Underground;
        else if (s.equals("secret")) return Ticket.SecretMove;
        else if (s.equals("double")) return Ticket.DoubleMove;
        else return null;
    }
    
    private static int t2i(Ticket t)
    {
        if      (t == Ticket.Taxi)        return 0;
        else if (t == Ticket.Bus)         return 1;
        else if (t == Ticket.Underground) return 2;
        else if (t == Ticket.SecretMove)  return 3;
        else if (t == Ticket.DoubleMove)  return 4;
        else return -1;
    }
}