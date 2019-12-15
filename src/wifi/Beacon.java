package wifi;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.*;
import rf.RF;

public class Beacon implements Runnable{
    RF theRF;
    Integer offset;
    long fudgeFactor = 0;
    int interval;
    int sendTime;
    short localMac;
    LinkLayer ll;



    public Beacon(final Integer off, final int interval, final short localMac, final RF rf, final LinkLayer ll){
        theRF = rf;
        this.offset = off;
        this.interval = interval;
        this.localMac = localMac;
        this.ll = ll;
        //fudgeFactor = setFudge();
    }

    public void addToOffset(){
        offset++;
    }

    public void setInterval(final int interval){
        this.interval = interval;
        ll.debugs("Set beacon interval to " + interval);
    }

    public int getInterval(){
        return this.interval;
    }

    @Override
    public void run() {
        //check interval, if -1 skip,
        while(true){
            if(interval > -1){
                //else wait for interval
                if(theRF.clock() >= sendTime){
                    //make new timepacket with calculated timestamp
                    final Packet timePacket = new Packet((byte)-1, localMac);
                    timePacket.setData(timePacket.longToBytes(theRF.clock()+fudgeFactor+offset));
                    timePacket.setFrameType((byte)2);
                    //send new timepacket on theRF
                    if(!theRF.inUse()){
                        theRF.transmit(timePacket.getPacket());
                    }
                    else{
                        while(theRF.inUse());
                        theRF.transmit(timePacket.getPacket());
                    }
                    resetSendTime();
                }
            }
            try{
                Thread.sleep((long)(interval)*1000);
            }
            catch(InterruptedException e){
                ll.debugs(e.toString());
            }
            
        }
    }

    public long setFudge(){
        final Packet timePacket = new Packet((byte)-1, localMac);
        timePacket.setData(timePacket.longToBytes(theRF.clock()+fudgeFactor+offset));
        timePacket.setFrameType((byte)2);
        final long startTime = theRF.clock();
        for(int i = 0; i < 10; i++){
            theRF.transmit(timePacket.getPacket());
        }
        return (theRF.clock() - startTime)/10;
    }

    public void resetSendTime(){
        sendTime = sendTime + interval;
    }

    
    public static void main (final String[]args){
    }
}