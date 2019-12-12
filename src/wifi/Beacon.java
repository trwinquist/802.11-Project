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



    public Beacon(Integer off, int interval, short localMac, RF rf, LinkLayer ll){
        theRF = rf;
        this.offset = off;
        this.interval = interval;
        this.localMac = localMac;
        this.ll = ll;
        fudgeFactor = setFudge();
    }

    public void addToOffset(){
        offset++;
    }

    public void setInterval(int interval){
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
                    Packet timePacket = new Packet((byte)-1, localMac);
                    timePacket.setData(timePacket.longToBytes(theRF.clock()+fudgeFactor+offset));
                    timePacket.setFrameType((byte)2);
                    //send new timepacket on theRF
                    theRF.transmit(timePacket.getPacket());
                }
            }
        } 
    }

    public long setFudge(){
        Packet timePacket = new Packet((byte)-1, localMac);
        timePacket.setData(timePacket.longToBytes(theRF.clock()+fudgeFactor+offset));
        timePacket.setFrameType((byte)2);
        long startTime = theRF.clock();
        for(int i = 0; i < 10; i++){
            theRF.transmit(timePacket.getPacket());
        }
        return (theRF.clock() - startTime)/10;
    }

    
    public static void main (String[]args){
    }
}