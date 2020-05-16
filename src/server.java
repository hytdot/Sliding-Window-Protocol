import javax.swing.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class server {
    public JTextArea textPrint;
    public void open(JTextArea textArea) {
         this.textPrint = textArea;
        //指定监听的端口
        int port = 5555;
        ServerSocket senderSocket;
        buffers bufs = new buffers();

        try {
            //创建ServerSocket,绑定并监听5555端口
            senderSocket = new ServerSocket(port);
            //阻塞，等待客户端连接。accept()方法返回一个Socket类
            textPrint.append("服务器等待连接.....\n");
            //System.out.println("server将一直等待连接的到来");
            Socket socket = senderSocket.accept();
            textPrint.append("连接成功\n");
            //System.out.println("连接成功");

            S_sender send = new S_sender(socket, bufs, textPrint);
            S_receiver receive = new S_receiver(socket, bufs, textPrint);
            send.start();
            receive.start();
            //socket.close();
            //senderSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        //指定监听的端口
        int port = 5555;
        ServerSocket senderSocket;
        buffers bufs = new buffers();
        JTextArea textPrint = new JTextArea();
        try {
            //创建ServerSocket,绑定并监听5555端口
            senderSocket = new ServerSocket(port);
            //阻塞，等待客户端连接。accept()方法返回一个Socket类
            System.out.println("server将一直等待连接的到来");
            Socket socket = senderSocket.accept();
            System.out.println("连接成功");
          //  InetAddress ip = InetAddress.getLocalHost();
           // System.out.println(ip.getHostAddress());
           // System.out.println(ip.getHostName());
            S_sender send = new S_sender(socket, bufs, textPrint);
            S_receiver receive = new S_receiver(socket, bufs, textPrint);
            send.start();            //TODO：问题二：不能共享数据，用了volatile
            receive.start();
            //socket.close();
            //senderSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

//向客户端发送数据
class S_sender extends Thread{
    public Socket socket;
    public buffers  bufs;//缓冲区
    public DataOutputStream out;
    public pseudoHeader pHeader = new pseudoHeader();    //TCP伪首部
    public TCPsegment tcpSeg = new TCPsegment();
    public bytesUtil util = new bytesUtil();
    public byte[] datas = {1,3,5,7,9,11,13,15,17,19,21,23,25,27,29,31,33,35,37,39,41,43,45,47,49,51};  //发送数据24个
    public int p_data = 0;                                                       //未进入缓冲区数据首编号
    public JTextArea textPrint;

    public S_sender(Socket socket, buffers bufs, JTextArea textPrint){
        this.socket = socket;
        this.bufs = bufs;
        try {
            this.out = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        //获取TCP伪首部
        InetAddress ip1 = socket.getLocalAddress(); //获取发送ip
        InetAddress ip2 = socket.getInetAddress();  //获取目的ip
        this.pHeader.setSaddr(ip1.getAddress());
        this.pHeader.setDaddr(ip2.getAddress());
        this.pHeader.setTcpl(util.shortToByte2((short) 21));
        //设置TCP报文段端口
        int p1 = socket.getLocalPort();  //发送端口
        int p2 = socket.getPort();       //目的端口
        this.tcpSeg.setSrc_port(util.shortToByte2((short)p1));
        this.tcpSeg.setDest_port(util.shortToByte2((short)p2));
        this.textPrint = textPrint;
    }

    //打包数据
    public void tcpPacket(buffer temp, byte sign) {
        tcpSeg.setSeq(util.intToByte4(temp.num1));
        tcpSeg.setAck(util.intToByte4(bufs.num));
        tcpSeg.setOffset(sign); //ACK=1,FIN=0;
        tcpSeg.setChecksum(util.shortToByte2((short) 0));
        tcpSeg.setData(temp.data);
        tcpSeg.CheckSum(pHeader, tcpSeg);
    }

    public void run(){
        while(true){
            //缓存中有需要重发的数据
            bufs.setFlag(false);
            for (int i = 0; i < bufs.p_buf && i < bufs.bufferList.size(); i++) {
                buffer temp = bufs.bufferList.get(i);
                if (temp.flag1 == false && temp.flag2 == true) {
                    bufs.setFlag(true);
                    if (temp.num1 == datas.length-1) {
                        tcpPacket(temp, (byte)17);    //最后一个报文段
                    }
                    else {
                        tcpPacket(temp, (byte)16);
                    }
                    byte[] tcp = util.tcpToBytes(tcpSeg, 21);
                    try {
                        textPrint.append("发送数据:"+temp.data+" 本次帧序号:"+temp.num1+" 重发数据\n");
                       // System.out.println("发送数据:"+temp.data+" 本次帧序号:"+temp.num1+" 重发数据");
                        out.write(tcp);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    temp.flag1 = true;
                    //TODO:timer
                    temp.startTimer();
                }
            }
            if (bufs.flag == true) {
                continue;
            }
            //发送缓存不满
            if (bufs.bufferList.size() < bufs.bufferLen) {
                int x = bufs.bufferLen-bufs.bufferList.size();
                for (int i = 0; i < x && p_data < datas.length; i++) {
                    buffer temp = new buffer();        //TODO:问题一：没有新new一个元素导致list中所有元素相同
                    temp.num1 = p_data;
                    temp.data = datas[p_data];
                    p_data++;
                    temp.flag1 = false;
                    temp.flag2 = false;
                    bufs.bufferList.add(temp);
                }
            }
            //向服务器发送数据
            int x = bufs.getX(bufs, datas.length);
            for (int i = bufs.p_buf; i <= x && i < bufs.bufferList.size(); i++) {
                buffer buf = bufs.bufferList.get(i);
                int num = bufs.bufferList.get(i).num1;
                //允许发送但尚未发送的数据
                if (bufs.win_2 <= num && num <= bufs.win_3) {
                    if (num == datas.length-1) {
                        tcpPacket(buf, (byte)17);
                    }
                    else {
                        tcpPacket(buf, (byte)16);
                    }
                    byte[] tcp = util.tcpToBytes(tcpSeg, 21);
                    try {
                        textPrint.append("发送数据:"+buf.data+" 本次帧序号:"+buf.num1+"\n");
                       // System.out.println("发送数据:"+buf.data+" 本次帧序号:"+buf.num1);
                        out.write(tcp);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    buf.flag1 = true;
                    //TODO:timer
                    buf.startTimer();
                    bufs.add();
                }
            }
            if (bufs.off == true) {
                break;
            }
        }
    }
}

//从客户端接收数据
class S_receiver extends Thread {
    public Socket socket;
    public buffers bufs;//缓冲区
    public DataInputStream in;
    public pseudoHeader pHeader = new pseudoHeader();    //TCP伪首部
    public TCPsegment tcpSeg;
    public bytesUtil util = new bytesUtil();
    public JTextArea textPrint;

    public S_receiver(Socket socket, buffers bufs, JTextArea textPrint) {
        this.socket = socket;
        this.bufs = bufs;
        try {
            in = new DataInputStream(socket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        //获取TCP伪首部
        InetAddress ip1 = socket.getLocalAddress(); //获取发送ip
        InetAddress ip2 = socket.getInetAddress();  //获取目的ip
        this.pHeader.setSaddr(ip2.getAddress());
        this.pHeader.setDaddr(ip1.getAddress());
        this.textPrint = textPrint;
    }

    public void run() {
        while (true) {
            try {
                byte[] recv = new byte[20];
                int len = in.read(recv);
                //接收到数据后进行确认工作
                if (len != 0) {
                    pHeader.setTcpl(util.shortToByte2((short)len));
                    tcpSeg = util.byteToTcp(recv);
                    //收到的确认帧出错
                    if (tcpSeg.testCheckSum(pHeader, tcpSeg) == false) {
                        continue;
                    }
                    //收到的确认帧正常
                    int ack = util.byte4ToInt(tcpSeg.ack);
                    int seq = util.byte4ToInt(tcpSeg.seq);
                    bufs.setNum(seq+1);
                    textPrint.append("收到"+seq+"号应答帧"+" 下次希望收到的帧序号:"+ack+"\n");
                  //  System.out.println("收到"+seq+"号应答帧"+" 下次希望收到的帧序号:"+ack);
                    if (tcpSeg.offset[1] == (byte)17) {
                        bufs.off = true;
                    }
                    //已经发送但未确认的数据
                    for (int i = 0; i < bufs.p_buf; i++) {
                        buffer buf = bufs.bufferList.get(i);
                        //发送数据收到确认，从发送缓存中删除，移动窗口
                        if (buf.num1 < ack) {
                            bufs.bufferList.remove(0);
                            bufs.moveWin(1);
                            bufs.decP_buf();
                            i--;
                        }
                        //要求重传的一组数据
                        if (buf.num1 == ack) {
                            buf.flag1 = false;
                            buf.flag2 = true;
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (bufs.off == true) {
                textPrint.append("传输结束\n");
                break;
            }
        }
    }
}