package wifi;
import java.util.*;
import java.util.concurrent.*;
import rf.RF;

public class Sender implements Runnable {
    BlockingQueue<Packet> messageQueue;
    BlockingQueue<Packet> ackQueue;
    Hashtable<Short, Short> seqNums;
    RF theRF;
    Integer status;
    LinkLayer ll;

    public Sender(BlockingQueue<Packet> theQueue, BlockingQueue<Packet> acks, RF theRF, Hashtable<Short, Short> seqNums, Integer statusObj, LinkLayer ll) {
        this.messageQueue = theQueue;
        this.ackQueue = acks;
        this.theRF = theRF;
        this.seqNums = seqNums;
        this.status = statusObj;
        this.ll = ll;
    }

    //
    public void run() {
        while (true) {
            Packet packet = null;
            try {
                packet = messageQueue.take();
            } catch (Exception e) {
                ll.debugs("getting the packet from the queue failed");
            }
            ll.debugs("Successfully took a packet from the queue");
            if (packet != null) {
                sendData(packet);
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
        long startTime = theRF.clock();
        long timeOut = startTime + 20000L;
        boolean retrySend = true;
        boolean acked = false;
        currentState = State.WAITFORDATA;
        while(retrySend == true && retransmissionAttemps <= theRF.dot11RetryLimit && acked == false){
            ll.debugs("Start Switch Statement, tranmission attempt: " + retransmissionAttemps);
            ll.debugs("sender ack queue size: " + ackQueue.size());

            switch (currentState) {

                case WAITFORDATA:
                    //System.out.println("waitfordata");
                    if (theRF.inUse()) {
                        currentState = State.WAITFORTRANSMISSIONTOEND;
                    } else {
                        currentState = State.WAITIFS;
                    }
                    break;

                case WAITIFS:
                    try {
                        if (packetToSend.getFrameType() == (byte) 1) {
                            ll.debugs("waiting sifs to send ack");
                            Thread.sleep(theRF.aSIFSTime);

                        } else {
                            Thread.sleep(theRF.aSIFSTime + 2 * theRF.aSlotTime);
                        }
                    } catch (Exception e) {
                        ll.debugs("something went wrong sleeping: 1");
                    }
                    //System.out.println("waitifs");
                    if (theRF.inUse()) {
                        currentState = State.WAITFORTRANSMISSIONTOEND;
                    } else {
                        currentState = State.TRANSMIT;
                    }
                    break;
                case WAITFORTRANSMISSIONTOEND:
                    //System.out.println("waitfortransmissiontoend");
                    while (theRF.inUse()) {
                    }
                    ;
                    currentState = State.WAITIFSWITHBACKOFF;
                    break;
                case WAITIFSWITHBACKOFF:
                    // System.out.println("waitifswithbackoff");
                    if (packetToSend.getFrameType() == (byte) 1) {
                        try {
                            Thread.sleep(theRF.aSIFSTime);
                        } catch (Exception e) {
                            ll.debugs("Something went wrong Sleeping: 2");
                        }
                    } else {
                        try {
                            Thread.sleep(theRF.aSIFSTime + 2 * theRF.aSlotTime * retransmissionAttemps);
                        } catch (Exception e) {
                            ll.debugs("Something went wrong Sleeping: 3");
                        }
                    }
                    if (theRF.inUse()) {
                        backoffWindowSize++;
                        currentState = State.WAITFORTRANSMISSIONTOEND;
                    } else {
                        try {
                            Thread.sleep((2 ^ backoffWindowSize*retransmissionAttemps) * theRF.aSlotTime);
                        } catch (Exception e) {
                            ll.debugs("Something went wrong Sleeping: 4");
                        }
                        currentState = State.TRANSMIT;
                    }
                    break;
                // transmit a frame then transition to waiting for the ack.
                case TRANSMIT:
                    ll.debugs("Transmit");
                    theRF.transmit(packetToSend.getPacket());
                    ll.debugs("Sent packet");
                    if(packetToSend.getFrameType() == (byte) 1){
                        retrySend = false; // don't try to resend acks,  just break.
                        break;
                    } else if (packetToSend.getFrameType() == (byte) 2) {
                        retrySend = false; //don't try to retry sending beacons because we won't receive an ack for these.
                        break;
                    } else if ( packetToSend.getDestShort()== (short) -1 ) {
                        retrySend = false; //don't try to resend broadcast messages. cuz we don't need to receive acks for these
                        break;
                    } else {
                        //wait for acks
                        currentState = State.WAITFORACK;
                    }
                    break;
                //wait for an ack
                case WAITFORACK:
                	ll.debugs("Waiting for ack");
                    // System.out.println("Waitforack");

                    if (ackQueue.size() > 0) {
                        // System.out.println("sender sees the ack!");
                        acked = true;
                        ackQueue.clear();
                    } else if(retransmissionAttemps >= theRF.dot11RetryLimit){
                        //System.out.println("done trying to transmit");
                        status = 5;
                        retrySend = false;
                        break;
                    } else if (theRF.clock() >= timeOut) {
                        ll.debugs("Timeout, retransmit");
                        currentState = State.WAITFORDATA;
                        ll.debugs("go back to transmit");
                        retransmissionAttemps ++;
                    }

                    break;
            }
        }
    }
}
