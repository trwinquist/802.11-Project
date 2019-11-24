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
    /**
    Listens to the RF layer, when the layer is not in use it transmits the frame. if the layer is in use it sleeps for 1 second. // this sleep will be replaced with a better wait time implementation later on.
    @param Packet a packet to transmit
     */
    private void attemptSend( Packet packetToSend ) {
        Boolean packetSent = false;
        while ( packetSent == false ) {
            if( !theRF.inUse() ){
                try {
                    theRF.transmit(packetToSend.getPacket());
                    System.out.println("Transmitted a packet");
                    packetSent = true;
                } catch (Exception e){
                    System.out.println("Uh Oh... Something went terribly wrong when we tried to send your packet");
                }
            } else {
                try {
                    System.out.println("thinks we should put thread to sleep");
                    Thread.sleep(1000);
                } catch(Exception e){
                    System.out.println("Something went wrong sleeping");
                }
            }
        }
    }
}