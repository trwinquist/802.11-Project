package wifi;
//import java.lang;

public class Packet{
    /*
        Packet Class 
        
        Fields
            Control field : 2 bytes
                Frame type: 3 bits
                Retry: 1 bit
                Sequence Num: 12 bits
            Destination address : 2 bytes
            Source Address : 2 bytes
            Data : 2038 bytes lmao
            CRC : 4 bytes
            ByteArray : 2048 array of all bytes

        
        Methods
            toString()
                returns ByteArray to send on rf layer
            setters and getters for all fields
            
    */
    public byte[] myBytes;
    private final int controlFieldIndex = 2;
    //private final Byte frameTypeByte = new Byte(224);
    //private final byte retryByte = (byte)16;
    //private final byte seqNumByte = (byte)15;
    private final int destIndex = 2;
    private final int srcIndex = 4;
    private final int dataIndex = 6;
    private final int crcIndex = 2044;
    public boolean isRetry;
    public String controlField;

    public Packet(byte[] byteArray){
        myBytes = byteArray;
    }

    public Packet(byte[] data, byte[] src, byte[] dest, byte[] seq){
        
    }
    /*
    public byte[] getControlField(){
        byte[] controlField = new byte[controlFieldIndex];
        for(int i = 0; i < controlFieldIndex; i++){
            controlField[i] = myBytes[i];
        }
        return controlField;
    }

    public byte getFrameType(){
        //byte frameType = getControlField()[0];
        //frame data is controlField & 11100000
        frameType = frameTypeByte & frameType;
        return frameType;
    }

    public byte getRetry(){
        byte retry = getControlField()[0];
        retry = retry & retryByte;
        if(retry == 0){
            return retry;
        }
        return 1;
    }

    public byte[] getSeqNum(){
        byte [] controlField = getControlField();
        return new byte[]{seqNumByte & controlField[0], controlField[1]};
    }
    */
    public byte[] getDest(){
        return new byte[]{myBytes[destIndex], myBytes[destIndex+1]};
    }

    public byte[] getSrc(){
        return new byte[]{myBytes[srcIndex], myBytes[srcIndex+1]};
    }
    
    public byte[] getCRC(){
        byte[] crc = new byte[4];
        for(int i = 0; i < crc.length; i++){
            crc[i] = myBytes[crcIndex+i];
        }
        return crc;
    }

    public byte[] getData(){
        byte[] data = new byte[2038];
        for(int i = 0; i < data.length; i++){
            data[i] = myBytes[dataIndex+i];
        }
        return data;
    }

    
    public static void main(String[] args){
        byte[] arr = new byte[2048];
        Packet packet = new Packet(arr);
        System.out.print(packet.getData()+""+packet.getDest()+" "+packet.getSrc()+" "+packet.getCRC());
    }
}
    