/**
 * 
 */
package org.joemonti.drogon.controlpanel;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;

/**
 * @author joe
 * 
 */
public class RPiVideoView extends SurfaceView implements Callback {
    private static final int PORT = 42112;
    private static final long VIDEO_SLEEP_TIME = 50;
    
    private static final byte CMD_NEXT = 0x02;
    private static final byte CMD_CLOSE = 0x04;
    
    private ControlPanelEventHandler eventHandler;
    
    private GremlinVideoClient client;
    private GremlinVideoThread thread;
    private volatile boolean showFps;
    
    Paint overlayPaint;
    int overlayTextColor;
    int overlayBackgroundColor;
    
    public RPiVideoView( Context context, AttributeSet attrs, int defStyle ) {
        super( context, attrs, defStyle );
        init( context );
    }

    public RPiVideoView( Context context ) {
        super( context );
        init( context );
    }

    public RPiVideoView( Context context, AttributeSet attrs ) {
        super( context, attrs );
        init( context );
    }

    private void init( Context context ) {
        SurfaceHolder holder = getHolder( );
        holder.addCallback( this );

        setFocusable( true );
        
        client = new GremlinVideoClient( );

        overlayTextColor = Color.WHITE;
        overlayBackgroundColor = Color.BLACK;
        
        overlayPaint = new Paint();
        overlayPaint.setTextAlign(Paint.Align.LEFT);
        overlayPaint.setTextSize(14);
        overlayPaint.setTypeface(Typeface.DEFAULT);
    }
    
    public void setShowFps( boolean showFps ) {
        this.showFps = showFps;
    }
    
    public void setHost( String host ) {
        client.setHost( host );
    }
    
    public void setEventHandler( ControlPanelEventHandler eventHandler ) {
        this.eventHandler = eventHandler;
    }
    
    @Override
    public void surfaceChanged( SurfaceHolder holder, int format, int width,
            int height ) {
        Log.d( "GremlinVideoView", "Surface Changed" );
        thread.setDimensions( width, height );
    }

    @Override
    public void surfaceCreated( SurfaceHolder holder ) {
        Log.d( "GremlinVideoView", "Surface Created" );
        thread = new GremlinVideoThread( client, holder );
        thread.start( );
    }

    @Override
    public void surfaceDestroyed( SurfaceHolder holder ) {
        Log.d( "GremlinVideoView", "Surface Destroyed" );
        thread.shutdown( );
        thread = null;
    }

    class GremlinVideoThread extends Thread {
        private final SurfaceHolder mHolder;
        private final GremlinVideoClient client;
        
        private int frameCounter = 0;
        private long start;
        private Bitmap ovl;
        
        private int viewWidth;
        private int viewHeight;

        private volatile boolean running;

        GremlinVideoThread( GremlinVideoClient client,
                SurfaceHolder surfaceHolder ) {
            super();
            
            this.client = client;
            this.mHolder = surfaceHolder;
            
            this.running = true;
        }
        
        void setDimensions( int viewWidth, int viewHeight ) {
            synchronized ( mHolder ) {
                this.viewWidth = viewWidth;
                this.viewHeight = viewHeight;
            }
        }
        
        void shutdown() {
            this.running = false;
        }
        
        private Rect destRect( int bmw, int bmh ) {
            float bmasp = (float) bmw / (float) bmh;
            bmw = viewWidth;
            bmh = (int) (viewWidth / bmasp);
            if (bmh > viewHeight) {
                bmh = viewHeight;
                bmw = (int) (viewHeight * bmasp);
            }
            int tempx = (viewWidth / 2) - (bmw / 2);
            int tempy = (viewHeight / 2) - (bmh / 2);
            return new Rect(tempx, tempy, bmw + tempx, bmh + tempy);
        }
        
        private Bitmap makeFpsOverlay( Paint p, String text ) {
            Rect b = new Rect( );
            p.getTextBounds( text, 0, text.length( ), b );
            int bwidth = b.width( ) + 4;
            int bheight = b.height( ) + 2;
            Bitmap bm = Bitmap.createBitmap( bwidth, bheight,
                    Bitmap.Config.ARGB_8888 );
            Canvas c = new Canvas( bm );
            p.setColor( overlayBackgroundColor );
            c.drawRect( 0, 0, bwidth, bheight, p );
            p.setColor( overlayTextColor );
            c.drawText( text, -b.left + 1,
                    (bheight / 2) - ((p.ascent( ) + p.descent( )) / 2) + 1, p );
            return bm;
        }

        @Override
        public void run() {
            Log.d( "GremlinVideoView", "Video Thread Started" );
            start = System.currentTimeMillis();
            
            PorterDuffXfermode mode = new PorterDuffXfermode(PorterDuff.Mode.DST_OVER);
            
            int width;
            int height;
            
            Paint p = new Paint();
            Canvas c = null;
            Bitmap bm = null;
            Rect destRect;
            
            String fps = "";

            try {
                client.connect( );
                
                while ( running ) {
                    Thread.sleep( VIDEO_SLEEP_TIME );
                    
                    //Log.d( "GremlinVideoView", "Video Thread Running" );
                    
                    bm = client.read( );
                    
                    if ( !running ) break;
                    
                    try {
                        c = mHolder.lockCanvas( );
                        synchronized ( mHolder ) {
                            destRect = destRect( bm.getWidth( ), bm.getHeight( ) );
                            c.drawColor( Color.BLACK );
                            c.drawBitmap(bm, null, destRect, p);
                            
                            if ( showFps ) {
                                p.setXfermode(mode);
                                if ( ovl != null ) {
                                    height = destRect.top;
                                    width  = destRect.left;
                                    c.drawBitmap(ovl, width, height, null);
                                }
                                p.setXfermode(null);
                            }
                            
                            frameCounter++;
                            if ( (System.currentTimeMillis() - start) >= 1000 ) {
                                fps = String.valueOf(frameCounter) + "fps";
                                frameCounter = 0; 
                                start = System.currentTimeMillis();
                                
                                Log.d( "GremlinVideoView", "Video FPS: " + fps );
                                
                                if ( showFps ) {
                                    ovl = makeFpsOverlay(overlayPaint, fps);
                                }
                            }
                        }
                    } finally {
                        if ( c != null ) {
                            mHolder.unlockCanvasAndPost( c );
                        }
                    }
                }
            } catch ( UnknownHostException ex ) {
                eventHandler.onError( "Unknown host: " + client.host, ex );
            } catch ( IOException ex ) {
                eventHandler.onError( "Network I/O error", ex );
            } catch ( Exception ex ) {
                eventHandler.onError( "Internal error", ex );
            }
            
            if ( client != null ) {
                client.disconnect( );
            }
            
            this.running = false;
        }
    }

    public static class GremlinVideoClient {
        private String host;
        private Socket sock;
        private DataInputStream in;
        private OutputStream out;
        
        private VideoInputStream videoStream = new VideoInputStream();

        GremlinVideoClient( ) {
            this.host = null;
        }
        
        public String getHost() {
            return host;
        }
        
        public void setHost( String host ) {
            this.host = host;
        }

        public void connect( ) throws IOException {
            sock = new Socket( );
            sock.connect( new InetSocketAddress( host, PORT ) );
            in = new DataInputStream( sock.getInputStream( ) );
            out = sock.getOutputStream();
            Log.d( "GremlinVideoView", "Video Client Connected to " + host + ":" + PORT );
        }
        
        public void disconnect() {
            try {
                if ( sock.isConnected( ) ) {
                    sock.close( );
                }
            } catch ( IOException ex ) {
                Log.w( "GremlinVideoView", "Error closing socket", ex );
            }
        }

        public Bitmap read() throws IOException {
            videoStream.readFromStream( in );
            
            out.write(CMD_NEXT);
            out.flush();

            return BitmapFactory.decodeStream( videoStream );
        }
    }
    
    public static class VideoInputStream extends ByteArrayInputStream {
        public VideoInputStream() {
            super( new byte[] {} );
        }
        
        public void readFromStream( DataInputStream in ) throws IOException {
            int contentLength = in.readInt();
            
            if ( contentLength <= 0 ) {
                throw new IOException("No content [" + contentLength + "]");
            }
            
            if ( this.buf == null || this.buf.length < contentLength ) {
                this.buf = new byte[contentLength];
            }
            
            Log.d( "GremlinVideoView", "Video Client Read " + contentLength );
            
            in.readFully( buf, 0, contentLength );
            
            this.count = contentLength;
            this.pos = 0;
            this.mark = 0;
        }
    }
}
