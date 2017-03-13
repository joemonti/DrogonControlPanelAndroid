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

    private final IDrogonClientLogger logger;

    private ZMQ.Context context;
    private ZMQ.Socket reqSocket;
    //private ZMQ.Socket subSocket;

    private final ConcurrentMap<String, Short> eventTypes = new ConcurrentHashMap<String, Short>();

    private short eventTypeArm;
    private short eventTypeMotor;

    private volatile Thread thread;

    private final BlockingQueue<Runnable> actionQueue = new ArrayBlockingQueue<Runnable>(1);

    public DrogonClient(IDrogonClientLogger logger) {
        this.logger = logger;

        connected = false;
        running = false;

        context = ZMQ.context(1);
    }

    public void connect(String host) {
        if (!running) {
            running = true;
            thread = new Thread(this);
            thread.start();
        }
        actionQueue.add(new ConnectRunner(host));
    }

    public void disconnect() {
        if (running) {
            actionQueue.clear();
            actionQueue.add(new DisconnectRunner());
        }
    }

    public void updateArmed(boolean armed) {
        actionQueue.add(new ArmedRunner(armed));
    }

    public void updateMotor(double motor, boolean lazy) {
        if (lazy) {
            actionQueue.offer(new MotorRunner(motor));
        } else {
            actionQueue.add(new MotorRunner(motor));
        }
    }

    private short readEventType(String name) {
        byte[] msg = new byte[ByteUtil.SIZEOF_SHORT + name.length()];

        int off = 0;
        off = ByteUtil.writeShort(msg, off, EVT_TYPE_MGT_READ_EVENT_TYPE);
        off = ByteUtil.writeString(msg, off, name);

        reqSocket.send(msg);

        byte[] resp = reqSocket.recv();

        off = ByteUtil.SIZEOF_SHORT; // skip event type, read next int
        short eventTypeId = (short) ByteUtil.readInt(resp, off);

        logger.debug("Got event type id " + eventTypeId + " for name " + name);

        eventTypes.put(name, eventTypeId);

        return eventTypeId;
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
            logger.debug("Connecting to " + host);

            if (reqSocket != null) {
                reqSocket.close();
                reqSocket = null;
            }

            reqSocket = context.socket(ZMQ.REQ);
            String reqAddr = "tcp://" + host + ":12210";
            reqSocket.connect(reqAddr);

            //subSocket = context.socket(ZMQ.SUB);
            //String subAddr = "tcp://" + host + ":12211";
            //subSocket.connect(subAddr);

            eventTypeArm = readEventType(EVENT_TYPE_FLIGHT_ARM);
            eventTypeMotor = readEventType(EVENT_TYPE_FLIGHT_MOTOR);

            logger.debug("Connected to " + host);

            connected = true;
        }
    }

    class DisconnectRunner implements Runnable {
        @Override
        public void run() {
            if (reqSocket != null) {
                reqSocket.close();
                reqSocket = null;
            }
            connected = false;
            running = false;

            logger.debug("Disconnected");
        }
    }

    class ArmedRunner implements Runnable {
        final boolean armed;
        ArmedRunner(boolean armed) { this.armed = armed; }

        @Override
        public void run() {
            if (!connected) return;

            byte[] msg = new byte[ByteUtil.SIZEOF_SHORT + ByteUtil.SIZEOF_BYTE];

            int off = 0;
            off = ByteUtil.writeShort(msg, off, eventTypeArm);
            off = ByteUtil.writeByte(msg, off, (byte)(armed ? 1 : 0));

            reqSocket.send(msg);

            reqSocket.recv();

            logger.debug("Armed Updated to " + (armed ? 1 : 0));
        }
    }

    class MotorRunner implements Runnable {
        final double motor;
        MotorRunner(double motor) { this.motor = motor; }

        @Override
        public void run() {
            if (!connected) return;

            byte[] msg = new byte[ByteUtil.SIZEOF_SHORT + ByteUtil.SIZEOF_LONG];

            int off = 0;
            off = ByteUtil.writeShort(msg, off, eventTypeMotor);
            off = ByteUtil.writeLong(msg, off, Double.doubleToLongBits(motor));

            reqSocket.send(msg);

            reqSocket.recv();
        }
    }

    public static interface IDrogonClientLogger {
        public void debug(String msg);
    }
}
