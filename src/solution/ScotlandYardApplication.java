package solution;

import scotlandyard.*;

import javax.swing.*;

public class ScotlandYardApplication
{
    public static void main(String[] args)
    {
        Resources resources = new Resources();
        new Thread(resources::load).start();
        
        ScotlandYardApplication game = new ScotlandYardApplication();
        SwingUtilities.invokeLater(game::run);
    }
    
    public void run()
    {
        GameFrame w = new GameFrame();
    }
}