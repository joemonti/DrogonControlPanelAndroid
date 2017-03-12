package org.joemonti.drogon.controlpanel;

import org.joemonti.util.ByteUtil;
import org.zeromq.ZMQ;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by joe on 6/13/16.
 */
public class DrogonClient implements Runnable {
    public static final int MSG_DATA_TYPE_BYTE     = 0;
    public static final int MSG_DATA_TYPE_INT      = 1;
    public static final int MSG_DATA_TYPE_LONG     = 2;
    public static final int MSG_DATA_TYPE_FLOAT    = 3;
    public static final int MSG_DATA_TYPE_DOUBLE   = 4;
    public static final int MSG_DATA_TYPE_STRING   = 5;
    public static final int MSG_DATA_TYPE_BYTEA    = 6;
    public static final int MSG_DATA_TYPE_JSON     = 7;

    public static final short EVT_TYPE_MGT_REGISTER_EVENT_TYPE       = 1;
    public static final short EVT_TYPE_MGT_REGISTER_EVENT_TYPE_RESP  = 2;
    public static final short EVT_TYPE_MGT_READ_EVENT_TYPE           = 3;
    public static final short EVT_TYPE_MGT_READ_EVENT_TYPE_RESP      = 4;
    public static final short EVT_TYPE_MGT_EVENT_RESP                = 5;

    public static final String EVENT_TYPE_FLIGHT_ARM = "flight_arm";
    public static final String EVENT_TYPE_FLIGHT_MOTOR = "flight_motor";

    private volatile boolean running;
    private volatile boolean connected;

    private final ControlPanelActivity activity;

    private ZMQ.Context context;
    private ZMQ.Socket reqSocket;
    //private ZMQ.Socket subSocket;

    private final ConcurrentMap<String, Short> eventTypes = new ConcurrentHashMap<String, Short>();
    private final BlockingQueue<Runnable> actionQueue = new ArrayBlockingQueue<Runnable>(1);

    private volatile Thread thread;

    public DrogonClient(ControlPanelActivity activity) {
        this.activity = activity;
        connected = false;

        context = ZMQ.context(1);
    }

    public void connect(String host) {
        if (!running) {

            disconnect();
            connected = false;
        }
        actionQueue.add(new ConnectRunner(host));
    }

    public void disconnect() {
        if (running) {
            running = false;
            actionQueue.clear();
            actionQueue.add(new Runnable() {
                @Override
                public void run() {
                    // nothing
                }
            });
        }
    }

    public void updateArmed(boolean armed) {

    }

    public void updateMotor(double motor) {

    }

    private void readEventType(String name) {
        byte[] msg = new byte[ByteUtil.SIZEOF_SHORT + name.length()];

        int off = 0;
        off = ByteUtil.writeShort(msg, off, EVT_TYPE_MGT_READ_EVENT_TYPE);
        off = ByteUtil.writeString(msg, off, name);

        reqSocket.send(msg);

        byte[] resp = reqSocket.recv();

        off = ByteUtil.SIZEOF_SHORT; // skip event type, read next int
        short eventTypeId = (short) ByteUtil.readInt(resp, off);

        activity.writeDebugMessage("Got event type id " + eventTypeId + " for name " + name);

        eventTypes.put(name, eventTypeId);
    }

    @Override
    public void run() {
        while (running) {
            try {
                Runnable runnable = actionQueue.take();
                runnable.run();
            } catch (InterruptedException ex) {
                // how rude...
            }
        }
    }

    class ConnectRunner implements Runnable {
        private final String host;

        ConnectRunner(String host) { this.host = host; }

        @Override
        public void run() {
            activity.writeDebugMessage("Connecting to " + host);

            reqSocket = context.socket(ZMQ.REQ);
            String reqAddr = "tcp://" + host + ":12210";
            reqSocket.connect(reqAddr);

            //subSocket = context.socket(ZMQ.SUB);
            //String subAddr = "tcp://" + host + ":12211";
            //subSocket.connect(subAddr);

            readEventType(EVENT_TYPE_FLIGHT_ARM);
            readEventType(EVENT_TYPE_FLIGHT_MOTOR);

            activity.writeDebugMessage("Connected to " + host);

            connected = true;
        }
    }

    class DisconnectRunner implements Runnable {
        @Override
        public void run() {
            reqSocket.close();
            reqSocket = null;
        }
    }

    class ActionRunner implements Runnable {
        @Override
        public void run() {

        }
    }
}
