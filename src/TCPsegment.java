/*------------TCP报文段------------*/
public class TCPsegment {
    public byte[] src_port = new byte[2];           //源端口号
    public byte[] dest_port = new byte[2];          //目的端口号
    public byte[] seq = new byte[4];                   //序号
    public byte[] ack = new byte[4];                   //确认号
    public byte[] offset = new byte[2];        //数据偏移+保留+控制位
    public byte[] wnd = {0x00, 0x05};            //窗口大小为5
    public byte[] checksum = new byte[2];            //校验和
    public byte[] Upointer = {0x00, 0x00};            //紧急指针
    public byte[] data = new byte[1];                 //数据，设定为一个字节

    public void setSrc_port(byte[] src_port) {
        this.src_port = src_port;
    }

    public void setDest_port(byte[] dest_port) {
        this.dest_port = dest_port;
    }

    public void setSeq(byte[] seq) {
        this.seq = seq;
    }

    public void setAck(byte[] ack) {
        this.ack = ack;
    }

    public void setOffset(byte offset) {
        this.offset[0] = 0x50;
        this.offset[1] = offset;       //设置ACK和FIN控制位
    }

    public void setChecksum(byte[] checksum) {
        this.checksum = checksum;
    }

    public void setData(byte data) {
        this.data[0] = data;
    }

    //计算TCP报文段的校验和
    public void CheckSum(pseudoHeader pHeader, TCPsegment tcpSeg) {
        bytesUtil util = new bytesUtil();
        int tcplen = util.byte2ToInt(pHeader.tcpl);
        int len = tcplen + 12;
        if (len%2 != 0) {
            len++;
        }
        byte[] bytes = new byte[len];
        int num = 0;
        byte[] byte1 = util.pHeaderTobytes(pHeader);
        byte[] byte2 = util.tcpToBytes(tcpSeg, tcplen);
        for (int i = 0; i < byte1.length; i++) {
            bytes[num] = byte1[i];
            num++;
        }
        for (int i = 0; i < byte2.length; i++) {
            bytes[num] = byte2[i];
            num++;
        }
        if (num < bytes.length) {
            bytes[num] = 0;
        }
        int sum = 0;
        byte[] temp_sum;
        byte[] temp_byte2 = new byte[2];
        for (int i = 0; i < len; i = i+2) {
            int temp;
            temp_byte2[0] = bytes[i];
            temp_byte2[1] = bytes[i+1];
            temp = util.byte2ToInt(temp_byte2);
            sum = sum + temp;
            temp_sum = util.intToByte4(sum);
            if (temp_sum[1] != 0) {
                temp_sum[1] = 0;
                temp_sum[3]++;
                sum = util.byte4ToInt(temp_sum);
            }
        }
        sum = ~sum;
        temp_sum = util.intToByte4(sum);
        temp_byte2[0] = temp_sum[2];
        temp_byte2[1] = temp_sum[3];
        tcpSeg.setChecksum(temp_byte2);
    }

    //验证校验和
    public boolean testCheckSum(pseudoHeader pHeader, TCPsegment tcpSeg) {
        bytesUtil util = new bytesUtil();
        int tcplen = util.byte2ToInt(pHeader.tcpl);
        int len = tcplen + 12;
        if (len%2 != 0) {
            len++;
        }
        byte[] bytes = new byte[len];
        int num = 0;
        byte[] byte1 = util.pHeaderTobytes(pHeader);
        byte[] byte2 = util.tcpToBytes(tcpSeg, tcplen);
        for (int i = 0; i < byte1.length; i++) {
            bytes[num] = byte1[i];
            num++;
        }
        for (int i = 0; i < byte2.length; i++) {
            bytes[num] = byte2[i];
            num++;
        }
        if (num < bytes.length) {
            bytes[num] = 0;
        }
        int sum = 0;
        byte[] temp_sum;
        byte[] temp_byte2 = new byte[2];
        for (int i = 0; i < len; i = i+2) {
            int temp;
            temp_byte2[0] = bytes[i];
            temp_byte2[1] = bytes[i+1];
            temp = util.byte2ToInt(temp_byte2);
            sum = sum + temp;
            temp_sum = util.intToByte4(sum);
            if (temp_sum[1] != 0) {
                temp_sum[1] = 0;
                temp_sum[3]++;
                sum = util.byte4ToInt(temp_sum);
            }
        }
        if (sum == 65535) {
            return true;
        }
        else {
            return false;
        }
    }
}

//TCP伪首部
class pseudoHeader {
    public byte[] saddr = new byte[4];               //32位源IP地址
    public byte[] daddr = new byte[4];               //32位目的IP地址
    public byte[] mbz = {0};            //保留字节置0
    public byte[] ptcl = {0x06};        //协议类型
    public byte[] tcpl = new byte[2];              //TCP报文长度（报头+数据）

    public void setSaddr(byte[] saddr) {
        this.saddr = saddr;
    }

    public void setDaddr(byte[] daddr) {
        this.daddr = daddr;
    }

    public void setTcpl(byte[] tcpl) {
        this.tcpl = tcpl;
    }

/*    public static void main(String[] args) {
        System.out.println(Arrays.toString(tcpl));
    }*/
}