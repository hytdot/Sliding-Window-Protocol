import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/*------------发送/接收缓存------------*/
public class buffers {
    public volatile List<buffer> bufferList = new ArrayList<buffer>(); //缓冲区
    public volatile boolean flag = false;      //是否有重发数据                 //是否发送应答帧
    public volatile int p_buf = 0;              //缓冲区中最小未发送数据标号     //缓冲区中最小未被应用程序读取数据标号
    public volatile int bufferLen = 10;         //缓冲区长度
    public volatile int win_1 = 0;
    public volatile int win_2 = 0;
    public volatile int win_3 = 4;
    public volatile int num = 1;         //期望得到应答帧的序号，发送报文的ack     //应答帧的序号
    public volatile boolean off = false;
    public volatile boolean off2 = false;
    public volatile int end = -99;               //最后一个数据帧的序号


    public synchronized void setFlag(boolean flag) {
        this.flag = flag;
    }

    public synchronized void moveWin(int num) {
        this.win_1 = this.win_1 + num;
        this.win_3 = this.win_3 + num;
    }

    public synchronized void move(int num) {
        this.win_1 = this.win_1 + num;
        this.win_3 = this.win_3 + num;
        this.p_buf = this.p_buf + num;
    }

    public synchronized int getX(buffers bufs, int length) {
        int x = bufs.p_buf+bufs.win_3-bufs.win_2;
        if (bufs.win_3 >= length) {
            x = bufs.bufferList.size()-1;
        }
        return x;
    }

    public synchronized void setNum(int num) {
        this.num = num;
    }

    public synchronized void add() {
        this.win_2++;
        this.p_buf++;
    }

    public synchronized void decP_buf() {
        this.p_buf--;
    }
}

class buffer {
    public byte data = 0;
    public int num1 = -1;         //发送数据的序号                     //收到数据的序号
    public boolean flag1 = false;    //是否发送                          //是否按序收到
    public boolean flag2 = false;    //是否重传                          //是否收到
    public Timer timer;
    TimerTask task;

    public void startTimer() {
        timer = new Timer();                       //TODO:问题五：计数器每次都要新new一个实例
        task = new TimerTask() {
            @Override
            public void run() {
                flag1 = false;
                flag2 = true;
            }
        };
        timer.schedule(task, 500);
    }
}
