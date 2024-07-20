package com.ktu.agents;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

/*
 * INFO APIE AGENTA
 * 
 * Gauna info is actuatorius kai tas atlieka visus veiksmus, veliau bus kad gauna info is roboto
 * Kai gauna info kad baigesi aktuatoriaus darbas, pranesa environment agentui kad reikia isscoutint area
 * Area gauna kaip stringa, td pavercia i 2d masyva, veliau atgal i stringa ir nusiuncia smegenims
 * 
 * Jeigu gautoj area is environment nera dirty langeliu, tai pranesa environment agentui kad reikia isscoutint didesne area
 * Tada ta area processina ir nusiuncia smegenim.
 * PO AREA PROCESSING LIEKA TOKIA INFO:
 * 0 - clean, 1 - dirty, 2 - blocked, -1 - robot
 */


public class Sensor extends Agent {
    private AID environmentAgent;
    private AID robotAgent;

    @Override
    protected void setup() {
        registerWithDF();
        environmentAgent = findAgent("environment-agent");
        robotAgent = findAgent("robot-agent");

        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
                ACLMessage msg = receive(mt);
                if (msg != null && "MOVE_COMPLETE".equals(msg.getContent())) {
                    System.out.println("Received MOVE_COMPLETE from " + msg.getSender().getLocalName());
                    if (environmentAgent != null) {
                        ACLMessage request = new ACLMessage(ACLMessage.QUERY_IF);
                        request.addReceiver(environmentAgent);
                        request.setContent("INFORMATION");
                        send(request);
        
                        // Wait for response
                        ACLMessage reply = blockingReceive();
                        if (reply != null && reply.getPerformative() == ACLMessage.INFORM) {
                            String data = reply.getContent();
                            int[][] processedGrid = processGrid(data);
                            //printProcessedGrid(processedGrid);
                            if (!containsDirtySquares(processedGrid)) {
                                sendDifferentRequestToEnvironmentAgent();
                                ACLMessage differentReply = blockingReceive();
                                if (differentReply != null && differentReply.getPerformative() == ACLMessage.INFORM) {
                                    String differentData = differentReply.getContent();
                                    int[][] differentProcessedGrid = processGrid(differentData);
                                    //printProcessedGrid(differentProcessedGrid);
                                    sendGridToMainRobotAgent(differentProcessedGrid);
                                }
                            } else {
                                sendGridToMainRobotAgent(processedGrid);
                            }
                        }
                    }
                } else {
                    block();
                }
            }
        });
    }

    /*
     * Sends a request to the EnvironmentAgent for more information
     */
    private void sendDifferentRequestToEnvironmentAgent() {
        if (environmentAgent != null) {
            ACLMessage request = new ACLMessage(ACLMessage.QUERY_IF);
            request.addReceiver(environmentAgent);
            request.setContent("DIFFERENT_INFORMATION"); // Replace with actual different information
            send(request);
            System.out.println("Sent different request to EnvironmentAgent");
        } else {
            System.err.println("EnvironmentAgent not found");
        }
    }

    /*
     * Processes the grid data received from the EnvironmentAgent
    */
    private int[][] processGrid(String gridData) {
        String[] rows = gridData.split("\n");
        int[][] grid = new int[rows.length][];
        for (int i = 0; i < rows.length; i++) {
            String[] cells = rows[i].trim().split(" ");
            grid[i] = new int[cells.length];
            for (int j = 0; j < cells.length; j++) {
                int cellValue = Integer.parseInt(cells[j]);
                switch (cellValue) {
                    case 1:
                        grid[i][j] = 0; // Clean
                        break;
                    case 2:
                        grid[i][j] = 1; // Dirty
                        break;
                    case 3:
                        grid[i][j] = 2; // Blocked
                        break;
                    default:
                        grid[i][j] = -1; // Robot position
                        break;
                }
            }
        }
        return grid;
    }
    /* 
     * Checks if the grid contains any dirty squares
    */
    private boolean containsDirtySquares(int[][] grid) {
        for (int[] row : grid) {
            for (int cell : row) {
                if (cell == 1) { 
                    return true;
                }
            }
        }
        return false;
    }
    /* 
     * Sends the processed grid to the Main Robot Agent
     */
    private void sendGridToMainRobotAgent(int[][] grid) {
        if (robotAgent != null) {
            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
            msg.addReceiver(robotAgent);
            msg.setContent(gridToString(grid));
            send(msg);
            System.out.println("Sent processed grid to MainRobotAgent");
        } else {
            System.err.println("MainRobotAgent not found");
        }
    }
    /*
     * Converts the grid to a string representation
     */
    private String gridToString(int[][] grid) {
        StringBuilder sb = new StringBuilder();
        for (int[] row : grid) {
            for (int cell : row) {
                sb.append(cell).append(" ");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    // private void printProcessedGrid(int[][] grid) {
    //     for (int[] row : grid) {
    //         for (int cell : row) {
    //             System.out.print(cell + " ");
    //         }
    //         System.out.println();
    //     }
    // }

    /*
     * Registers the agent with the DF
     */
    private void registerWithDF() {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("sensor-agent");
        sd.setName(getLocalName() + "-sensor-agent");
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
    /*
     * Finds an agent of the specified type
     */
    private AID findAgent(String agentType) {
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType(agentType);
        template.addServices(sd);
        try {
            DFAgentDescription[] result = DFService.search(this, template);
            if (result.length > 0) {
                return result[0].getName();
            }
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        return null;
    }
}
