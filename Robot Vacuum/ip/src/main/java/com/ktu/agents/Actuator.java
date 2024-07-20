package com.ktu.agents;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class Actuator extends Agent {
    private AID environmentAgent;
    private AID sensorAgent;
    private String[] moveOrder;
    private int moveIndex = 0;

    @Override
    protected void setup() {
        registerWithDF();
        moveOrder = new String[0];

        addBehaviour(new OneShotBehaviour() {
            @Override
            public void action() {
                if (environmentAgent == null) {
                    environmentAgent = findAgent("environment-agent");
                }
                if (sensorAgent == null) {
                    sensorAgent = findAgent("sensor-agent");
                }
                if(sensorAgent != null && environmentAgent != null)
                {
                    executeMoveOrder();
                }
            }
        });

        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.QUERY_IF);
                ACLMessage msg = receive(mt);
                if (msg != null) {
                    String moveOrderStr = msg.getContent();
                    System.out.println("Actuator received: " + moveOrderStr);
                    moveOrder = moveOrderStr.split(" ");
                    moveIndex = 0;
                    executeMoveOrder();
                } else {
                    block();
                }
            }
        });
    }

    private void executeMoveOrder() {
        if (moveIndex < moveOrder.length) {
            String move = moveOrder[moveIndex];
            moveIndex++;
            ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
            request.addReceiver(environmentAgent);
            request.setContent(move);
            System.out.println("Sending move: " + move);
            send(request);

            addBehaviour(new WakerBehaviour(this, 500) {
                @Override
                protected void onWake() {
                    MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
                    ACLMessage reply = receive(mt);
                    if (reply != null) {
                        executeMoveOrder();
                    } else {
                        moveIndex--;
                        executeMoveOrder();
                    }
                }
            });
        } else {
            ACLMessage notifySensor = new ACLMessage(ACLMessage.INFORM);
            notifySensor.addReceiver(sensorAgent);
            notifySensor.setContent("MOVE_COMPLETE");
            send(notifySensor);
        }
    }

    private void registerWithDF() {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("actuator-agent");
        sd.setName(getLocalName() + "-actuator-agent");
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
}