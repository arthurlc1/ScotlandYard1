package solution;

import scotlandyard.*;

import java.util.Map;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

public class TicketPanel extends JPanel implements ActionListener
{
    private static String[] names = {"taxi","bus","tube","secret","double"};
    private static Ticket[] ticket =
    {Ticket.Taxi, Ticket.Bus, Ticket.Underground, Ticket.SecretMove, Ticket.DoubleMove};
    
    private final ScotlandYardControl control;
    public final int location;
    public final int h;
    
    private final Map<Ticket,Integer> tickets;
    
    private JLabel[] img = new JLabel[5];
    private JLabel[] num = new JLabel[5];
    private JButton[] play = new JButton[5];
    
    public TicketPanel(int location, boolean x, Map<Ticket,Integer> tickets, Map<Ticket,Boolean> usable, ScotlandYardControl control)
    {
        this.location = location;
        this.tickets = tickets;
        this.control = control;
        
        h = x ? 155 : 95;
        int n = x ? 5 : 3;
        
        img = new JLabel[n];
        num = new JLabel[n];
        play = new JButton[n];
        
        this.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.insets = new Insets(2, 5, 2, 5);
        
        for (int i=0; i<n; i++)
        {
            Ticket t = ticket[i];
            img[i] = new JLabel(new ImageIcon(Resources.get(t, !usable.get(t))));
            gbc.gridy = i;
            gbc.gridx = 0;
            this.add(img[i], gbc);
            
            num[i] = new JLabel("x" + tickets.get(t));
            if (!usable.get(t)) num[i].setForeground(Color.GRAY);
            gbc.gridx = 1;
            this.add(num[i], gbc);
            
            play[i] = new JButton("Play");
            if (!usable.get(t)) play[i].setEnabled(false);
            //else
            //{
            play[i].setActionCommand(location + "-" + names[i]);
            play[i].addActionListener(this);
            //}
            gbc.gridx = 2;
            this.add(play[i], gbc);
        }
    }
    
    public void actionPerformed(ActionEvent e)
    {
        String name = e.getActionCommand().split("-")[1];
        for (int i=0; i<names.length; i++)
        {
            if (names[i].equals(name))
            {
                int num_ = tickets.get(ticket[i]) - 1;
                tickets.replace(ticket[i], num_);
                num[i].setText("x" + num_);
                if (i == 4 || num_ == 0)
                {
                    img[i].setIcon(new ImageIcon(Resources.get(ticket[i])));
                    num[i].setForeground(Color.GRAY);
                    play[i].setEnabled(false);
                }
                repaint();
                control.play(location, ticket[i]);
                break;
            }
        }
    }
}