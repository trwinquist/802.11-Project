package wifi;
import java.util.*;
import java.util.concurrent.*;
import rf.RF;

public class Sender implements Runnable {
    BlockingQueue<Packet> messageQueue;
    RF theRF;
    public Sender( BlockingQueue<Packet> theQueue, RF theRF ){
        this.queue = theQueue;
        this.ourRF = theRF;
    }
    //
    public void run(){
        while(true) {
            Packet packet = queue.take();
            attemptSend(packet);
        }
    }
    /**
    Listens to the RF layer, when the layer is not in use it transmits the frame. if the layer is in use it sleeps for 1 second. // this sleep will be replaced with a better wait time implementation later on.
    @param Packet a packet to transmit
     */
    private attemptSend( Packet packetToSend ) {
        Boolean packetSent = false;
        while ( packetSent == false ) {
            if( !theRF.inUse() ){
                try {
                    theRF.tranmit(packetToSend.getPacket());
                } catch (Exception e){
                    System.out.println("Uh Oh... Something went terribly wrong when we tried to send your packet");
                }
                packetSent = true;
            } else {
                Thread.sleep(1000);
            }
        }

    }
}