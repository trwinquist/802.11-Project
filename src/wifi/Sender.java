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
            try {
                Packet packet = messageQueue.take();
                System.out.println("took packet from the queue");
                if (packet != null) {
                    sendData(packet);
                }
            } catch (Exception e) {
                System.out.println("getting the packet from the queue failed");
            }

        }
    }

    public enum State {
        WAITFORDATA, WAITIFS, WAITIFSWITHBACKOFF, WAITFORTRANSMISSIONTOEND, TRANSMIT, WAITFORACK
    }

    ;
    private State currentState;

    private void sendData(Packet packetToSend) {
        int backoffWindowSize = 0;
        int retransmissionAttemps = 0;
        switch (currentState) {

            case WAITFORDATA:
                if (messageQueue.size() > 0) {
                    if (theRF.inUse()) {
                        currentState = State.WAITFORTRANSMISSIONTOEND;
                    } else {
                        if (packetToSend.getFrameType() == (byte) 1) {
                            Thread.sleep(theRF.aSIFSTime);
                        } else {
                            Thread.sleep(theRF.aSIFSTime + 2 * theRF.aSlotTime);
                        }
                        currentState = State.WAITIFS;
                    }
                }

            case WAITIFS:
                if (theRF.inUse()) {
                    currentState = State.WAITFORTRANSMISSIONTOEND;
                } else {
                    currentState = State.TRANSMIT;
                }

            case WAITFORTRANSMISSIONTOEND:
                while (theRF.inUse()) {
                }
                ;
                currentState = State.WAITIFSWITHBACKOFF;

            case WAITIFSWITHBACKOFF:
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
            case TRANSMIT:
                theRF.transmit(packetToSend.getPacket());
                currentState = State.WAITFORACK;
            case WAITFORACK:
                boolean acked = false;
                long startTime = theRF.clock();
                long timeOut = startTime + 20L;
                short ackSeqNum = 0;
                while (acked == false) {
                    if (theRF.clock() >= timeOut) {
                        currentState = State.TRANSMIT;
                    }
                    if (ackQueue.size() == 1) {
                        try{
                            ackSeqNum = ackQueue.take().getSeqNumShort();
                        } catch ( Exception e) {
                            System.out.println("something went wrong getting seqnum from ack");
                        }
                        if (ackSeqNum == packetToSend.getSeqNumShort()) {
                            ackQueue.clear();
                            currentState = State.WAITFORDATA;
                            acked = true;
                        }
                    }
                }
        }
    }
}
