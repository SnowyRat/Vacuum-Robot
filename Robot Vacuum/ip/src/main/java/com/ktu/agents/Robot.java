package com.ktu.agents;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

import java.util.*;

public class Robot extends Agent {
    private AID actuatorAgent;
    private int mapWidth;
    private int mapHeight;
    private boolean bigGrid = false; // Add this line to define the bigGrid parameter

    @Override
    protected void setup() {
        registerWithDF();
        actuatorAgent = findAgent("actuator-agent");
        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
                ACLMessage msg = receive(mt);
                if (msg != null) {
                    String content = msg.getContent();
                    int[][] grid = parseGrid(content);
                    String moves = calculateMinimumPath(grid);
                    sendMoveQueueToActuator(moves);
                } else {
                    block();
                }
            }
        });
    }

    private int[][] parseGrid(String gridData) {
        String[] rows = gridData.split("\n");
        if(rows.length > 3)
        {
            bigGrid = true;
        }
        else bigGrid = false;
        
        int[][] grid = new int[rows.length][];
        mapHeight = rows.length;
        mapWidth = rows[0].split(" ").length;

        for (int i = 0; i < rows.length; i++) {
            String[] cells = rows[i].trim().split(" ");
            grid[i] = new int[cells.length];
            for (int j = 0; j < cells.length; j++) {
                grid[i][j] = Integer.parseInt(cells[j]);
            }
        }
        return grid;
    }

    private void registerWithDF() {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("robot-agent");
        sd.setName(getLocalName() + "-robot-agent");
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

    private String calculateMinimumPath(int[][] grid) {
        int startX = 0, startY = 0;
        List<int[]> dirtySquares = new ArrayList<>();
        for (int i = 0; i < grid.length; i++) {
            for (int j = 0; j < grid[i].length; j++) {
                if (grid[i][j] == -1) {
                    startX = i;
                    startY = j;
                } else if (grid[i][j] == 1) {
                    dirtySquares.add(new int[]{i, j});
                }
            }
        }
    
        if (bigGrid) {
            int[] closest = findClosest(new int[]{startX, startY}, dirtySquares, grid);
            List<int[]> route = bfs(grid, new int[]{startX, startY}, closest);
            return convertPathToMoves(route);
        } else {
            // Find paths to all dirty squares
            StringBuilder fullPath = new StringBuilder();
            int[] currentPos = new int[]{startX, startY};
        
            while (!dirtySquares.isEmpty()) {
                int[] closest = findClosest(currentPos, dirtySquares, grid);
                List<int[]> route = bfs(grid, currentPos, closest);
                fullPath.append(convertPathToMoves(route)).append(" ");
                currentPos = closest;
                dirtySquares.removeIf(pos -> Arrays.equals(pos, closest));
            }
            if (fullPath.length() > 0) {
                fullPath.setLength(fullPath.length() - 1);
            }
        
            return fullPath.toString();
        }
    }

    private int[] findClosest(int[] start, List<int[]> targets, int[][] grid) {
        int[] closest = null;
        int minDistance = Integer.MAX_VALUE;

        for (int[] target : targets) {
            int distance = bfs(grid, start, target).size();
            if (distance < minDistance) {
                minDistance = distance;
                closest = target;
            }
        }

        return closest;
    }

    private List<int[]> bfs(int[][] grid, int[] start, int[] goal) {
        int[] directions = {-1, 0, 1, 0, 0, -1, 0, 1}; // Up, Down, Left, Right
        Queue<int[]> queue = new LinkedList<>();
        Map<int[], int[]> cameFrom = new HashMap<>();
        queue.add(start);
        cameFrom.put(start, null);

        while (!queue.isEmpty()) {
            int[] current = queue.poll();

            if (Arrays.equals(current, goal)) {
                List<int[]> path = new ArrayList<>();
                for (int[] at = current; at != null; at = cameFrom.get(at)) {
                    path.add(at);
                }
                Collections.reverse(path);
                return path;
            }

            for (int i = 0; i < directions.length; i += 2) {
                int[] next = {current[0] + directions[i], current[1] + directions[i + 1]};

                if (isValidMove(grid, next) && !cameFrom.containsKey(next)) {
                    queue.add(next);
                    cameFrom.put(next, current);
                }
            }
        }

        return Collections.emptyList();
    }

    private boolean isValidMove(int[][] grid, int[] position) {
        int x = position[0], y = position[1];
        return x >= 0 && y >= 0 && x < mapHeight && y < mapWidth && grid[x][y] != 2;
    }

    private String convertPathToMoves(List<int[]> path) {
        StringBuilder moves = new StringBuilder();
        for (int i = 1; i < path.size(); i++) {
            int[] current = path.get(i - 1);
            int[] next = path.get(i);
            if (next[0] == current[0] - 1) {
                moves.append("UP ");
            } else if (next[0] == current[0] + 1) {
                moves.append("DOWN ");
            } else if (next[1] == current[1] - 1) {
                moves.append("LEFT ");
            } else if (next[1] == current[1] + 1) {
                moves.append("RIGHT ");
            }
        }
        if (moves.length() > 0) {
            moves.setLength(moves.length() - 1);
        }
        return moves.toString();
    }

    private void sendMoveQueueToActuator(String moves) {
        ACLMessage msg = new ACLMessage(ACLMessage.QUERY_IF);
        System.out.println(moves);
        msg.addReceiver(actuatorAgent);
        msg.setContent(moves);
        send(msg);
    }
}