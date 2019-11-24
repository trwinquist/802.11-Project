package wifi;
import java.util.*;
import java.util.concurrent.*;
import rf.RF;

public class Sender implements Runnable {
    BlockingQueue<Packet> messageQueue;
    RF theRF;
    public Sender( BlockingQueue<Packet> theQueue, RF theRF ){
        this.messageQueue = theQueue;
        this.theRF = theRF;
    }
    //
    public void run(){
        while(true) {
            try {
                Packet packet = messageQueue.take();
                System.out.println("took packet from the queue");
                if(packet != null) {
                    attemptSend(packet);
                }
            } catch (Exception e){
                System.out.println("getting the packet from the queue failed");
            }

        }
    }
    public enum State {
        WAITFORDATA , WAITIFS , WAITIFSWITHBACKOFF, WAITFORTRANSMISSIONTOEND, TRANSMIT, WAITFORACK
    };
    private State currentState;
    private void sendData (Packet packetToSend) {
        int backoffWindowSize = 0;
        switch (currentState){

            case WAITFORDATA:
                if(messageQueue.size() > 0) {
                    if(theRF.inUse()){
                        currentState = State.WAITFORTRANSMISSIONTOEND;
                    }
                    else {
                        if (packetToSend.getFrameType() == (byte) 1) {
                            Thread.sleep(theRF.aSIFSTime);
                        } else {
                            Thread.sleep(theRF.aSIFSTime + 2 * theRF.aSlotTime);
                        }
                        currentState = State.WAITIFS;
                    }
                }

            case WAITIFS:
                if (theRF.inUse()){
                    currentState = State.WAITFORTRANSMISSIONTOEND;
                } else{
                    currentState = State.TRANSMIT;
                }

            case WAITFORTRANSMISSIONTOEND:
                while (theRF.inUse()){};
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
                if (theRF.inUse()){
                    backoffWindowSize ++;
                    currentState = State.WAITFORTRANSMISSIONTOEND;
                }
                else{
                    try {
                        Thread.sleep((2 ^ backoffWindowSize) * theRF.aSlotTime);
                    } catch (Exception e){
                       System.out.println("Something went wrong Sleeping");
                    }
                    currentState = State.TRANSMIT;
                }
            case TRANSMIT:
                theRF.transmit(packetToSend.getPacket());
                currentState = State.WAITFORACK;
            case WAITFORACK:
                
        }
    }
    /**
    Listens to the RF layer, when the layer is not in use it transmits the frame. if the layer is in use it sleeps for 1 second. // this sleep will be replaced with a better wait time implementation later on.
    @param Packet a packet to transmit
     */
    private void attemptSend( Packet packetToSend ) {
        Boolean packetSent = false;
        int difs =  theRF.aSIFSTime + 2*theRF.aSlotTime;
        int numRetries = 0;


        while ( packetSent == false && numRetries <= theRF.dot11RetryLimit ) {
            if( !theRF.inUse() ){
                try {
                    Thread.sleep(theRF.aSIFSTime);
                    if ( !theRF.inUse()) {
                        theRF.transmit(packetToSend.getPacket());
                        System.out.println("Transmitted a packet");
                        packetSent = true;
                    }
                } catch (Exception e) {
                    System.out.println("Uh Oh... Something went terribly wrong when we tried to send your packet");
                }
            } else {
                try {
                    Thread.sleep();
                } catch(Exception e){
                    System.out.println("Something went wrong sleeping");
                }
            }
        }
    }
}