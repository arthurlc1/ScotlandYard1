package solution;

import scotlandyard.*;

import java.io.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.filechooser.*;
import java.awt.*;
import java.awt.event.*;

public class MainMenuPanel extends JPanel implements ActionListener
{
    JPanel buttons;
    JButton newB;
    JButton loadB;
    JButton quitB;
    
    public MainMenuPanel()
    {
        buttons = new JPanel();
        newB = new JButton("New Game");
        newB.setActionCommand("new");
        newB.addActionListener(this);
        loadB = new JButton("Load Game");
        loadB.setActionCommand("load");
        loadB.addActionListener(this);
        quitB = new JButton("Quit");
        quitB.setActionCommand("quit");
        quitB.addActionListener(this);
        
        buttons.setLayout(new GridLayout(3, 1, 10, 10));
        buttons.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        buttons.setPreferredSize(new Dimension(200, 150));
        
        buttons.add(newB);
        buttons.add(loadB);
        buttons.add(quitB);
        
        this.setLayout(new GridBagLayout());
        this.setPreferredSize(new Dimension(220, 190));
        this.add(buttons, new GridBagConstraints());
    }
    
    public void newGame()
    {
        GameFrame w = (GameFrame) SwingUtilities.getWindowAncestor(this);
        w.setScreen(new NewGamePanel());
    }
    
    public void loadGame()
    {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Scotland Yard Savefile (*.syg)", "syg"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
        {
            GameHistory history = null;
            try { history = GameHistory.fromFile(chooser.getSelectedFile()); }
            catch(Exception e)
            {
                System.err.println("Could not load save file.");
                return;
            }
            GameFrame w = (GameFrame) SwingUtilities.getWindowAncestor(this);
            w.setScreen(new ScotlandYardDisplay(history));
        }
    }
    
    public void quit()
    {
        System.exit(0);
    }
    
    public void actionPerformed(ActionEvent e)
    {
        switch (e.getActionCommand())
        {
            case "new":
                newGame();
                break;
            case "load":
                loadGame();
                break;
            case "quit":
                quit();
                break;
            default:
        }
    }
}