package wifi;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import rf.RF;

public class Sender implements Runnable {
    BlockingQueue<Packet> messageQueue;
    BlockingQueue<Packet> ackQueue;
    Hashtable<Short, Short> seqNums;
    AtomicBoolean maxCW;
    RF theRF;
    Integer status;
    LinkLayer ll;


    public Sender(BlockingQueue<Packet> theQueue, BlockingQueue<Packet> acks, RF theRF, Hashtable<Short, Short> seqNums, Integer statusObj, AtomicBoolean maxCW, LinkLayer ll) {
        this.messageQueue = theQueue;
        this.ackQueue = acks;
        this.theRF = theRF;
        this.seqNums = seqNums;
        this.status = statusObj;
        this.ll = ll;
        this.maxCW = maxCW;
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
                try {
                    sendData(packet);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }


        }
    }

    public enum State {
        WAITFORDATA, WAITIFS, WAITIFSWITHBACKOFF, WAITFORTRANSMISSIONTOEND, TRANSMIT, WAITFORACK
    }

    ;
    private State currentState;


    private int collisionWindow(){
        if( maxCW.get() == true ) {
            return theRF.aCWmax;
        }
        else {
            return new Random().nextInt(theRF.aCWmax);
        }
    }

    private void sendData(Packet packetToSend) throws InterruptedException {
        int backoffWindowSize = 0;
        int retransmissionAttempts = 0;
        long startTime = theRF.clock();
        long timeOutInterval = 20000l;
        long timeOut = startTime + timeOutInterval;
        boolean retrySend = true;
        boolean acked = false;
        currentState = State.WAITFORDATA;
        while(retrySend == true && retransmissionAttempts <= theRF.dot11RetryLimit && acked == false){
            //ll.debugs("Start Switch Statement, tranmission attempt: " + retransmissionAttempts);
            //ll.debugs("sender ack queue size: " + ackQueue.size());

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
                            ll.debugs("waiting difs to send packet");
                            Thread.sleep(theRF.aSIFSTime + 2 * theRF.aSlotTime * collisionWindow() );
                        }
                    } catch (Exception e) {
                        ll.debugs("something went wrong sleeping: 1");
                    }

                    if (theRF.inUse()) {
                        currentState = State.WAITFORTRANSMISSIONTOEND;
                    } else {
                        currentState = State.TRANSMIT;
                    }
                    break;
                case WAITFORTRANSMISSIONTOEND:

                    ll.debugs("Waiting for a transmission to end before trying to send data");
                    while (theRF.inUse()) {
                    }
                    ;
                    currentState = State.WAITIFSWITHBACKOFF;
                    break;
                case WAITIFSWITHBACKOFF:
                    // System.out.println("waitifswithbackoff");
                    if (packetToSend.getFrameType() == (byte) 1) {
                        try {
                            ll.debugs("wait sifs to send acks");
                            Thread.sleep(theRF.aSIFSTime);
                        } catch (Exception e) {
                            ll.debugs("Something went wrong Sleeping: 2");
                        }
                    }
                    if (theRF.inUse()) {
                        backoffWindowSize++;
                        currentState = State.WAITFORTRANSMISSIONTOEND;
                    } else {
                        try {
                            ll.debugs("wait difs with backoff");
                            Thread.sleep((2 ^ backoffWindowSize*retransmissionAttempts) * theRF.aSlotTime * collisionWindow());
                        } catch (Exception e) {
                            ll.debugs("Something went wrong Sleeping: 4");
                        }

                    }
                    currentState = State.TRANSMIT;
                    break;
                // transmit a frame then transition to waiting for the ack.
                case TRANSMIT:
                    ll.debugs("Transmit, transmission attempt: " + retransmissionAttempts);
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
                    } else if(retransmissionAttempts >= theRF.dot11RetryLimit){
                        ll.debugs("Retry limit reached, abandoning retransmission");
                        //System.out.println("done trying to transmit");
                        status = 5;
                        retrySend = false;
                        break;
                    } else if (theRF.clock() >= timeOut) {
                        ll.debugs("Timeout, retransmit");
						packetToSend.setRetry();
						timeOut = theRF.clock() + timeOutInterval;
                        currentState = State.WAITFORDATA;
                        ll.debugs("go back to transmit");
                        retransmissionAttempts ++;

                    }
                    ll.debugs("sender ack queue size: " + ackQueue.size());
                    try {
                        Thread.sleep(5000);
                    } catch (Exception e){
                        ll.debugs("sleep to wait for ack didn't work");
                    }
                    break;
            }
        }
    }
}
