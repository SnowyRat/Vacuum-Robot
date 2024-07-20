package com.ktu.agents;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import javax.swing.*;
import java.awt.*;
import java.util.Random;

/*
 * Initializina grida nxn
 * Processina aktuatoriaus judesio requestus, parasyta turi taip but UP RIGHT DOWN LEFT
 * Sensoriui duoda informacija apie aplinka
 * 0 = unexplored, 1 = cleaned, 2 = dirty, 3 = obstacle, 4 = robot
 * 
 * 
 */

public class Environment extends Agent {
    private int[][] grid;
    private int robotX, robotY;
    private GridFrame gridFrame;

    @Override
    protected void setup() {
        // Initialize environment grid
        grid = new int[6][6]; // Example size
        Object[] args = getArguments();
        if (args != null && args.length > 0 && args[0].equals('1')) {
            initializeGrid();
        } else if (args[0].equals('2')) {
            initializeGrid1();
        } else {
            initializeGrid2();
        }
        registerWithDF();

        // Register with the DF
        // Initialize the GUI
        gridFrame = new GridFrame(grid);
        gridFrame.setVisible(true);

        /*
         * This behaviour is used to process the robot's movement requests
         */
        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
                ACLMessage msg = receive(mt);
                if (msg != null) {
                    String content = msg.getContent();
                    switch (content) {
                        case "UP":
                            moveUp();
                            break;
                        case "DOWN":
                            moveDown();
                            break;
                        case "LEFT":
                            moveLeft();
                            break;
                        case "RIGHT":
                            moveRight();
                            break;
                        default:
                            System.err.println("Received unknown command: " + content);
                            break;
                    }
                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.INFORM);
                    send(reply);

                    // Update the GUI
                    gridFrame.repaint();

                    // Check if all dirty spots are cleaned
                    if (isAllCleaned()) {
                        System.out.println("All spots are cleaned!");
                        JOptionPane.showMessageDialog(gridFrame, "All spots are cleaned!");
                        doDelete(); // Stop the agent
                    }
                } else {
                    block();
                }
            }
        });

        /*
         * This behaviour is used to provide information about the environment to the
         * robot
         */
        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.QUERY_IF);
                ACLMessage msg = receive(mt);
                if (msg != null) {
                    String content = msg.getContent();
                    ACLMessage reply = msg.createReply();

                    if (content.equals("DIFFERENT_INFORMATION")) {
                        reply.setPerformative(ACLMessage.INFORM);
                        reply.setContent(getBigGrid());
                    } else {
                        reply.setPerformative(ACLMessage.INFORM);
                        reply.setContent(getSurroundingGrid());
                    }
                    send(reply);

                    // Update the GUI
                    gridFrame.repaint();
                } else {
                    block();
                }
            }
        });
    }

    private void initializeGrid() {
        for (int i = 0; i < grid.length; i++) {
            for (int j = 0; j < grid[i].length; j++) {
                grid[i][j] = 2;
            }
        }
        grid[2][2] = 3;
        grid[2][3] = 3;
        grid[2][5] = 3;
        robotX = 2;
        robotY = 0;
        grid[robotX][robotY] = 4;
    }

    private void initializeGrid1() {
        Random random = new Random();
        // Initialize grid with clean cells
        for (int i = 0; i < grid.length; i++) {
            for (int j = 0; j < grid[i].length; j++) {
                grid[i][j] = 2;
            }
        }
        // Randomly set some dirty cells
        int numberOfDirtyCells = (grid.length * grid[0].length) / 5;
        for (int i = 0; i < numberOfDirtyCells; i++) {
            int x = random.nextInt(grid.length);
            int y = random.nextInt(grid[0].length);
            if (grid[x][y] == 2) {
                grid[x][y] = 1;
            } else {
                i--;
            }
        }
        int numberOfObstacles = (grid.length * grid[0].length) / 20;
        for (int i = 0; i < numberOfObstacles; i++) {
            int x = random.nextInt(grid.length);
            int y = random.nextInt(grid[0].length);
            if (grid[x][y] == 1) {
                grid[x][y] = 3;
            } else {
                i--;
            }
        }
        // random robot pos
        do {
            robotX = random.nextInt(grid.length);
            robotY = random.nextInt(grid[0].length);
        } while (grid[robotX][robotY] != 1);
        grid[robotX][robotY] = 4;
    }

    private void initializeGrid2() {
        for (int i = 0; i < grid.length; i++) {
            for (int j = 0; j < grid[i].length; j++) {
                grid[i][j] = 2;
            }
        }
        grid[2][2] = 3;
        grid[2][3] = 3;
        grid[0][3] = 3;
        grid[1][3] = 3;
        grid[3][3] = 3;
        grid[5][3] = 3;
        grid[4][5] = 3;
        grid[1][3] = 3;
        grid[3][0] = 3;
        grid[2][4] = 3;
        grid[5][3] = 3;


        // robot initial position
        robotX = 0;
        robotY = 0;
        grid[robotX][robotY] = 4;
    }

    private String getBigGrid() {
        // Provide the entire grid
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < grid.length; i++) {
            for (int j = 0; j < grid[i].length; j++) {
                sb.append(grid[i][j]).append(" ");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    /*
     * Returns a 2x2 submatrix around the robot
     */
    private String getSurroundingGrid() {
        StringBuilder sb = new StringBuilder();
        for (int i = Math.max(0, robotX - 1); i <= Math.min(grid.length - 1, robotX + 1); i++) {
            for (int j = Math.max(0, robotY - 1); j <= Math.min(grid[i].length - 1, robotY + 1); j++) {
                sb.append(grid[i][j]).append(" ");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    /*
     * Registers the agent with the DF
     */
    private void registerWithDF() {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("environment-agent");
        sd.setName(getLocalName() + "-environment-agent");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }

    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }

    private boolean moveUp() {
        if (robotX > 0 && grid[robotX - 1][robotY] != 3) {
            grid[robotX][robotY] = 1; // Mark the current cell as cleaned
            robotX--;
            grid[robotX][robotY] = 4; // Update the robot's new position
            return true;
        }
        return false;
    }

    private boolean moveDown() {
        if (robotX < grid.length - 1 && grid[robotX + 1][robotY] != 3) {
            grid[robotX][robotY] = 1; // Mark the current cell as cleaned
            robotX++;
            grid[robotX][robotY] = 4; // Update the robot's new position
            return true;
        }
        return false;
    }

    private boolean moveLeft() {
        if (robotY > 0 && grid[robotX][robotY - 1] != 3) {
            grid[robotX][robotY] = 1; // Mark the current cell as cleaned
            robotY--;
            grid[robotX][robotY] = 4; // Update the robot's new position
            return true;
        }
        return false;
    }

    private boolean moveRight() {
        if (robotY < grid[0].length - 1 && grid[robotX][robotY + 1] != 3) {
            grid[robotX][robotY] = 1; // Mark the current cell as cleaned
            robotY++;
            grid[robotX][robotY] = 4; // Update the robot's new position
            return true;
        }
        return false;
    }

    /*
     * Checks if all dirty spots are cleaned
     */
    private boolean isAllCleaned() {
        for (int i = 0; i < grid.length; i++) {
            for (int j = 0; j < grid[i].length; j++) {
                if (grid[i][j] == 2) {
                    return false;
                }
            }
        }
        return true;
    }

    class GridFrame extends JFrame {


        public GridFrame(int[][] grid) {
            setTitle("Environment Grid");
            setSize(500, 500);
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setLayout(new BorderLayout());
            GridPanel gridPanel = new GridPanel(grid);
            add(gridPanel, BorderLayout.CENTER);
        }
    }

    class GridPanel extends JPanel {
        private int[][] grid;

        public GridPanel(int[][] grid) {
            this.grid = grid;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            int cellSize = Math.min(getWidth() / grid.length, getHeight() / grid[0].length);

            for (int i = 0; i < grid.length; i++) {
                for (int j = 0; j < grid[i].length; j++) {
                    switch (grid[i][j]) {
                        case 0:
                            g.setColor(Color.GRAY);
                            break;
                        case 1:
                            g.setColor(Color.GREEN);
                            break;
                        case 2:
                            g.setColor(Color.YELLOW);
                            break;
                        case 3:
                            g.setColor(Color.RED);
                            break;
                        case 4:
                            g.setColor(Color.BLUE);
                            break;
                    }
                    g.fillRect(j * cellSize, i * cellSize, cellSize, cellSize);
                    g.setColor(Color.BLACK);
                    g.drawRect(j * cellSize, i * cellSize, cellSize, cellSize);
                }
            }
        }
    }
}