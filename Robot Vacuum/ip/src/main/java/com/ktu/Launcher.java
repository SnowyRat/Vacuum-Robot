package com.ktu;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

import jade.core.Agent;
import jade.wrapper.AgentController;
import jade.wrapper.ControllerException;
import jade.wrapper.AgentContainer;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class Launcher extends Agent {
    @Override
    protected void setup() {
        new GridChooser();
        doDelete();
    }
    class GridChooser extends JFrame {

        public GridChooser() {
            setTitle("Three Buttons Frame");
            setSize(400, 200);
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            JPanel panel = new JPanel();

            JButton button1 = new JButton("Button 1");
            JButton button2 = new JButton("Button 2");
            JButton button3 = new JButton("Button 3");
    
            button1.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    button1Action();
                    try{
                    AgentContainer container = getContainerController();
                    createAndStartAgents(container,  new Object[][]{{"Env", "com.ktu.agents.Environment", new Object[]{'1'}}});
                    createAndStartAgents(container,  new Object[][]{{"Sen", "com.ktu.agents.Sensor", new Object[]{}}});
                    createAndStartAgents(container,  new Object[][]{{"Rob", "com.ktu.agents.Robot", new Object[]{}}});
                    createAndStartAgents(container,  new Object[][]{{"Act", "com.ktu.agents.Actuator", new Object[]{}}});
                    }
                    catch (ControllerException e1) {
                        e1.printStackTrace();
                    }
                    setVisible(false);
                }
            });
    
            button2.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    button2Action();
                    try{
                        AgentContainer container = getContainerController();
                        createAndStartAgents(container,  new Object[][]{{"Env", "com.ktu.agents.Environment", new Object[]{'2'}}});
                        createAndStartAgents(container,  new Object[][]{{"Sen", "com.ktu.agents.Sensor", new Object[]{}}});
                        createAndStartAgents(container,  new Object[][]{{"Rob", "com.ktu.agents.Robot", new Object[]{}}});
                        createAndStartAgents(container,  new Object[][]{{"Act", "com.ktu.agents.Actuator", new Object[]{}}});
                        }
                        catch (ControllerException e1) {
                            e1.printStackTrace();
                        }
                        setVisible(false);
                }
            });
    
            button3.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    button3Action();
                    try{
                        AgentContainer container = getContainerController();
                        createAndStartAgents(container,  new Object[][]{{"Env", "com.ktu.agents.Environment", new Object[]{'3'}}});
                        createAndStartAgents(container,  new Object[][]{{"Sen", "com.ktu.agents.Sensor", new Object[]{}}});
                        createAndStartAgents(container,  new Object[][]{{"Rob", "com.ktu.agents.Robot", new Object[]{}}});
                        createAndStartAgents(container,  new Object[][]{{"Act", "com.ktu.agents.Actuator", new Object[]{}}});
                        }
                        catch (ControllerException e1) {
                            e1.printStackTrace();
                        }
                        setVisible(false);
                }
            });
            panel.add(button1);
            panel.add(button2);
            panel.add(button3);
            add(panel);
            setVisible(true);
        }
    
        private void button1Action() {
            System.out.println("Button 1 was clicked!");
        }
    
        private void button2Action() {
            System.out.println("Button 2 was clicked!");
        }
    
        private void button3Action() {
            System.out.println("Button 3 was clicked!");
        }
    }

    private void createAndStartAgents(AgentContainer container, Object[][] configurations) throws ControllerException {
        for (Object[] config : configurations) {
            AgentController agent = container.createNewAgent((String) config[0], (String) config[1], (Object[]) config[2]);
            agent.start();
        }
    }
}
