import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.*;

public class AgentPersonne extends Agent {

    private String[] availableRestaurants;
    private String[] allPersonAgents;
    private int pollSizeP;

    private final Random random = new Random();
    private int myReservationAttempts = 0;
    private String finalChoice = null;
    private String currentIntention = null;

    @Override
    protected void setup() {
        Object[] args = getArguments();

        if (args != null
                && args.length == 3
                && args[0] instanceof String[]
                && args[1] instanceof String[]
                && args[2] instanceof Integer) {

            availableRestaurants = (String[]) args[0];
            allPersonAgents = (String[]) args[1];
            pollSizeP = (Integer) args[2];
            
            currentIntention = availableRestaurants[random.nextInt(availableRestaurants.length)];
            System.out.println(getLocalName() + ": Initial choice is " + currentIntention);

            System.out.println("Person Agent " + getLocalName()
                    + " started. Knows " + availableRestaurants.length
                    + " restaurants. Poll size P=" + pollSizeP);

            addBehaviour(new PollReplyBehaviour());

            addBehaviour(new OneShotBehaviour() {
                @Override
                public void action() {
                	doWait(5000 + random.nextInt(3000));
                    myAgent.addBehaviour(new DeliberationAndReservationBehaviour());
                }
            });

        } else {
            System.err.println("Person Agent " + getLocalName()
                    + " requires 3 arguments: restaurants, personNames, pollSizeP");
            doDelete();
        }
    }

    private class PollReplyBehaviour extends CyclicBehaviour {
        private final MessageTemplate template = MessageTemplate.and(
                MessageTemplate.MatchConversationId("poll-intention"),
                MessageTemplate.MatchPerformative(ACLMessage.QUERY_REF)
        );

        @Override
        public void action() {
            ACLMessage msg = myAgent.receive(template);
            if (msg != null) {
                ACLMessage reply = msg.createReply();
                reply.setPerformative(ACLMessage.INFORM);
                reply.setContent(currentIntention == null ? "NONE" : currentIntention);
                myAgent.send(reply);

                System.out.println(getLocalName() + ": Sent intention reply -> "
                        + reply.getContent() + " to " + msg.getSender().getLocalName());
            } else {
                block();
            }
        }
    }

    private class DeliberationAndReservationBehaviour extends OneShotBehaviour {
        @Override
        public void action() {
            if (availableRestaurants == null || availableRestaurants.length == 0) {
                System.err.println(getLocalName() + ": No restaurants available.");
                return;
            }

            String currentChoice = currentIntention;

            List<String> candidates = new ArrayList<>();
            for (String agentName : allPersonAgents) {
                if (!agentName.equals(getLocalName())) {
                    candidates.add(agentName);
                }
            }

            Collections.shuffle(candidates);
            int effectiveP = Math.min(pollSizeP, candidates.size());
            List<String> polledAgents = candidates.subList(0, effectiveP);

            for (String target : polledAgents) {
                ACLMessage pollMsg = new ACLMessage(ACLMessage.QUERY_REF);
                pollMsg.addReceiver(new AID(target, AID.ISLOCALNAME));
                pollMsg.setConversationId("poll-intention");
                pollMsg.setContent("What is your current intention?");
                myAgent.send(pollMsg);

                System.out.println(getLocalName() + ": Poll sent to " + target);
            }

            Map<String, Integer> choiceCounts = new HashMap<>();
            for (String restaurant : availableRestaurants) {
                choiceCounts.put(restaurant, 0);
            }

            long deadline = System.currentTimeMillis() + 4000;
            int repliesReceived = 0;

            while (repliesReceived < effectiveP && System.currentTimeMillis() < deadline) {
                MessageTemplate mt = MessageTemplate.and(
                        MessageTemplate.MatchConversationId("poll-intention"),
                        MessageTemplate.MatchPerformative(ACLMessage.INFORM)
                );

                ACLMessage reply = myAgent.blockingReceive(mt, 500);
                if (reply != null) {
                    String intendedRestaurant = reply.getContent();
                    if (choiceCounts.containsKey(intendedRestaurant)) {
                        choiceCounts.put(intendedRestaurant, choiceCounts.get(intendedRestaurant) + 1);
                    }
                    repliesReceived++;
                }
            }

            System.out.println(getLocalName() + ": Polled intentions (counts): " + choiceCounts);

            String adjustedChoice = currentChoice;
            int minCount = Integer.MAX_VALUE;

            for (Integer count : choiceCounts.values()) {
                if (count < minCount) minCount = count;
            }

            List<String> leastPopularOptions = new ArrayList<>();
            for (Map.Entry<String, Integer> entry : choiceCounts.entrySet()) {
                if (entry.getValue() == minCount) {
                    leastPopularOptions.add(entry.getKey());
                }
            }

            if (!leastPopularOptions.isEmpty()) {
                adjustedChoice = leastPopularOptions.get(random.nextInt(leastPopularOptions.size()));
            }

            currentChoice = adjustedChoice;
            currentIntention = adjustedChoice;

            System.out.println(getLocalName() + ": Adjusted choice (least popular) is " + adjustedChoice);

            boolean reserved = false;
            long timeLimit = System.currentTimeMillis() + 30000;

            while (!reserved && System.currentTimeMillis() < timeLimit) {
                System.out.println(getLocalName() + ": Attempting to reserve at " + currentChoice);

                ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
                request.addReceiver(new AID(currentChoice, AID.ISLOCALNAME));
                request.setContent("Reservation request");
                request.setConversationId("dinner-reservation");
                request.setReplyWith("req" + System.currentTimeMillis());

                myAgent.send(request);
                myReservationAttempts++;

                MessageTemplate replyTemplate = MessageTemplate.and(
                        MessageTemplate.MatchConversationId("dinner-reservation"),
                        MessageTemplate.MatchInReplyTo(request.getReplyWith())
                );

                ACLMessage reply = myAgent.blockingReceive(replyTemplate, 2000);

                if (reply != null) {
                    if (reply.getPerformative() == ACLMessage.CONFIRM) {
                        System.out.println("******************************************************");
                        System.out.println(getLocalName() + ": SUCCESS! Reserved at " + currentChoice + ".");
                        System.out.println(getLocalName() + ": Total reservation attempts: " + myReservationAttempts);
                        System.out.println("******************************************************");
                        reserved = true;
                        finalChoice = currentChoice;
                        currentIntention = currentChoice;
                    } else {
                        System.out.println(getLocalName() + ": FAILED at " + currentChoice + ". Reason: " + reply.getContent());
                        String previousChoice = currentChoice;

                        if (availableRestaurants.length > 1) {
                            do {
                                currentChoice = availableRestaurants[random.nextInt(availableRestaurants.length)];
                            } while (currentChoice.equals(previousChoice));
                        }

                        currentIntention = currentChoice;
                        System.out.println(getLocalName() + ": Trying next: " + currentChoice);
                    }
                } else {
                    System.out.println(getLocalName() + ": FAILED at " + currentChoice + ". No reply received.");
                    String previousChoice = currentChoice;

                    if (availableRestaurants.length > 1) {
                        do {
                            currentChoice = availableRestaurants[random.nextInt(availableRestaurants.length)];
                        } while (currentChoice.equals(previousChoice));
                        }

                    currentIntention = currentChoice;
                    System.out.println(getLocalName() + ": Trying next after timeout: " + currentChoice);
                }
            }

            if (!reserved) {
                System.out.println(getLocalName() + ": FAILED to reserve after " + myReservationAttempts + " attempts.");
                currentIntention = "FAILED_TO_RESERVE";
            }
        }
    }

    @Override
    protected void takeDown() {
        System.out.println("Person Agent " + getLocalName() + " terminating. Final status: "
                + (finalChoice != null ? "Reserved at " + finalChoice : "Failed reservation")
                + ". Attempts: " + myReservationAttempts);
    }
}