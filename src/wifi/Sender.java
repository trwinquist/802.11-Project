package wifi;
import java.util.*;
import java.util.concurrent.*;
import rf.RF;

public class Sender implements Runnable {
    BlockingQueue<Packet> messageQueue;
    BlockingQueue<Packet> ackQueue;
    RF theRF;

    public Sender(BlockingQueue<Packet> theQueue, BlockingQueue<Packet> acks, RF theRF) {
        this.messageQueue = theQueue;
        this.ackQueue = acks;
        this.theRF = theRF;
    }

    //
    public void run() {
        while (true) {
            Packet packet = null;
            try {
                packet = messageQueue.take();
            } catch (Exception e) {
                System.out.println("getting the packet from the queue failed");
            }
                System.out.println("took packet from the queue");
                if (packet != null) {
                    sendData(packet);
                }


        }
    }

    public enum State {
        WAITFORDATA, WAITIFS, WAITIFSWITHBACKOFF, WAITFORTRANSMISSIONTOEND, TRANSMIT, WAITFORACK, DONETRYINGTRANSMIT
    }

    ;
    private State currentState;

    private void sendData(Packet packetToSend) {
        int backoffWindowSize = 0;
        int retransmissionAttemps = 0;
        long startTime = theRF.clock();
        long timeOut = startTime + 2000L;
        boolean retrySend = true;
        boolean acked = false;
        currentState = State.WAITFORDATA;
        while(retrySend == true && retransmissionAttemps <= theRF.dot11RetryLimit && acked == false){
            System.out.println("Start Switch Statement, tranmission attempt: " + retransmissionAttemps);
            System.out.println("sender ack queue size: " + ackQueue.size());

        switch (currentState) {

            case WAITFORDATA:
                System.out.println("waitfordata");
                    if (theRF.inUse()) {
                        currentState = State.WAITFORTRANSMISSIONTOEND;
                    } else {
                        try {
                            if (packetToSend.getFrameType() == (byte) 1) {
                                System.out.println("waiting sifs to send ack");
                                Thread.sleep(theRF.aSIFSTime);

                            } else {
                                Thread.sleep(theRF.aSIFSTime + 2 * theRF.aSlotTime);
                            }
                        } catch (Exception e) {
                            System.out.println("something went wrong sleeping");
                        }
                        currentState = State.WAITIFS;
                    }
                break;

            case WAITIFS:
                System.out.println("waitifs");
                if (theRF.inUse()) {
                    currentState = State.WAITFORTRANSMISSIONTOEND;
                } else {
                    currentState = State.TRANSMIT;
                }
                break;
            case WAITFORTRANSMISSIONTOEND:
                System.out.println("waitfortransmissiontoend");
                while (theRF.inUse()) {
                }
                ;
                currentState = State.WAITIFSWITHBACKOFF;
                break;
            case WAITIFSWITHBACKOFF:
                System.out.println("waitifswithbackoff");
                if (packetToSend.getFrameType() == (byte) 1) {
                    try {
                        Thread.sleep(theRF.aSIFSTime);
                    } catch (Exception e) {
                        System.out.println("Something went wrong Sleeping");
                    }
                } else {
                    try {
                        Thread.sleep(theRF.aSIFSTime + 2 * theRF.aSlotTime);
                    } catch (Exception e) {
                        System.out.println("Something went wrong Sleeping");
                    }
                }
                if (theRF.inUse()) {
                    backoffWindowSize++;
                    currentState = State.WAITFORTRANSMISSIONTOEND;
                } else {
                    try {
                        Thread.sleep((2 ^ backoffWindowSize) * theRF.aSlotTime);
                    } catch (Exception e) {
                        System.out.println("Something went wrong Sleeping");
                    }
                    currentState = State.TRANSMIT;
                }
                break;
            case TRANSMIT:
                System.out.println("Transmit");
                theRF.transmit(packetToSend.getPacket());
                System.out.println("Sent packet");
                if(packetToSend.getFrameType() == (byte) 1){
                    retrySend = false;
                    break;
                } else {
                    currentState = State.WAITFORACK;
                }
            case WAITFORACK:
                System.out.println("Waitforack");


                short ackSeqNum = 0;
                if (ackQueue.size() > 0) {
                    System.out.println("sender sees the ack!");
                    acked = true;
                    ackQueue.clear();
                } else if(retransmissionAttemps >= theRF.dot11RetryLimit){
                        System.out.println("done trying to transmit");
                        retrySend = false;
                        break;
                    } else if (theRF.clock() >= timeOut) {
                        System.out.println("Timeout, retransmit");
                        currentState = State.WAITFORDATA;
                        System.out.println("go back to transmit");
                        retransmissionAttemps ++;
                    }

                    break;
        }
        }
    }
}
