/**
 * 
 */
package org.joemonti.drogon.controlpanel;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.graphics.drawable.shapes.RoundRectShape;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.Arrays;

/**
 * @author joe
 *
 */
public class PowerSlider extends SurfaceView implements SurfaceHolder.Callback {
    private static double CENTER_BUFFER = 2.5;
    //private static int EDGE_BUFFER = 4;

    private static double BALL_H_RATIO = 0.5;
    private static double GUTTER_W_RATIO = 0.25;
    private static double GUTTER_MARK_HEIGHT_RATIO = 0.02;

    private SurfaceHolder mHolder;

    //private Bitmap imgBallOrig;
    //private Bitmap imgBall;
    //private Bitmap imgBgOrig;
    //private Bitmap imgBg;

    //private int ballOrigW;
    //private int ballOrigH;
    private int ballW;
    private int ballH;

    private double value;

    private int w;
    private int h;
    private int gutterW;

    private int ballLeft;
    private int ballTop;

    private float valueX;
    private float valueY;

    private int gutterMarkH;

    private int touchStartY;

    private Paint valuePaint = new Paint();

    private ShapeDrawable bgShape;
    private ShapeDrawable gutterBgShape;
    private ShapeDrawable ballShape;
    private ShapeDrawable gutterMarkShape;
    private ShapeDrawable[] gutterTickShapes = new ShapeDrawable[21];

    private ControlPanelEventHandler handler;

    public PowerSlider( Context context ) {
        super( context );

        init(context);
    }

    public PowerSlider( Context context, AttributeSet attrs, int defStyle ) {
        super( context, attrs, defStyle );

        init(context);
    }

    public PowerSlider( Context context, AttributeSet attrs ) {
        super( context, attrs );

        init(context);
    }

    private void init( Context context ) {
        Resources res = context.getResources();

        DisplayMetrics dm = res.getDisplayMetrics();

        //CENTER_BUFFER = (int) ( CENTER_BUFFER * dm.density );
        //EDGE_BUFFER = (int) ( EDGE_BUFFER * dm.density );

        //imgBgOrig = BitmapFactory.decodeResource(res, R.drawable.power_bg);

        //imgBallOrig = BitmapFactory.decodeResource(res, R.drawable.power_handle);
        //ballOrigW = imgBallOrig.getWidth();
        //ballOrigH = imgBallOrig.getHeight();

        //ballW = ballOrigW;
        //ballH = ballOrigH;

        ballTop = 0;
        ballLeft = 0;

        valueX = 10;
        valueY = 10;

        mHolder = getHolder();
        mHolder.addCallback(this);

        valuePaint.setARGB(255, 222, 168, 151);
        valuePaint.setTextSize(30f);

        bgShape = new ShapeDrawable(new RectShape());
        bgShape.getPaint().setARGB(255, 50, 59, 97);
        //setBackground(bgShape);

        gutterBgShape = new ShapeDrawable(new RectShape());
        gutterBgShape.getPaint().setARGB(255, 31, 40, 82);

        gutterMarkShape = new ShapeDrawable(new RectShape());
        gutterMarkShape.getPaint().setARGB(255, 140, 142, 64);

        for ( int i = 0; i < gutterTickShapes.length; i++ ) {
            gutterTickShapes[i] = new ShapeDrawable(new RectShape());
            gutterTickShapes[i].getPaint().setARGB(255, 73, 82, 124);
        }

        float[] corners = new float[8];
        Arrays.fill(corners, 15.0f);
        ballShape = new ShapeDrawable(new RoundRectShape(corners, null, null));
        ballShape.getPaint().setARGB(255, 140, 142, 64);

        setFocusable(true);
    }
    /* (non-Javadoc)
     * @see android.view.SurfaceView#onMeasure(int, int)
     */
    @Override
    protected void onMeasure( int widthMeasureSpec, int heightMeasureSpec )
    {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        // re-measure height and width to match
        setMeasuredDimension(getMeasuredWidth(), getMeasuredWidth());
    }

    public void setHandler(ControlPanelEventHandler handler) {
        this.handler = handler;
    }

    @Override
    public void surfaceChanged( SurfaceHolder holder, int format, int width,
                                int height ) {
        Log.d( "PowerSlider", "Surface Changed" );

        w = width;
        h = height;

        //imgBg = Bitmap.createScaledBitmap(imgBgOrig, w, h, true);

        gutterW = (int) ( w * GUTTER_W_RATIO );

        ballW = w - gutterW;
        ballH = (int) ( w * BALL_H_RATIO );

        //imgBall = Bitmap.createScaledBitmap(imgBallOrig, ballW, ballH, true);

        bgShape.setBounds(0, 0, w, h);
        gutterBgShape.setBounds(0, 0, gutterW, h);

        gutterMarkH = (int) ( w * GUTTER_MARK_HEIGHT_RATIO );

        for ( int i = 0; i < gutterTickShapes.length; i++ ) {
            double m = h - ballH;
            double v = m * i / (double) (gutterTickShapes.length-1);

            int tickTop = (int) ( m - v );

            int left = 0;
            int right = gutterW;
            if ( ( i % 5 ) != 0 ) {
                left = (int) (gutterW * 0.25);
                right = gutterW - (int) (gutterW * 0.25);
            }
            gutterTickShapes[i].setBounds(left, tickTop + (ballH / 2) - gutterMarkH, right, tickTop + (ballH/2) );
        }

        centerBall();
        doDraw();
    }

    private void centerBall() {
        moveBall(0.0);
    }

    private void moveBall( double value ) {
        if ( Math.abs( value ) < CENTER_BUFFER ) {
            this.value = 0;
            ballTop = (int) ( h - ballH );
        } else {
            value = Math.max(0.0, Math.min(100.0, value));

            double m = h - ballH;
            double v = m * value / 100.0;

            this.value = value;
            ballTop = (int) ( m - v );
        }

        ballShape.setBounds(gutterW + 10, ballTop + 10, gutterW + ballW - 10, ballTop + ballH - 10);

        gutterMarkShape.setBounds(0, ballTop + (ballH/2) - gutterMarkH, gutterW, ballTop + (ballH/2) );

        valueY = ballTop + (int) (ballH*0.4);

        if (handler != null) {
            handler.onMotor(this.value);
        }
    }

    @Override
    public void surfaceCreated( SurfaceHolder holder ) {
        Log.d("PowerSlider", "Surface Created");
    }

    @Override
    public void surfaceDestroyed( SurfaceHolder holder ) {
        Log.d( "PowerSlider", "Surface Destroyed" );
    }

     private void doDraw() {
        Canvas c = null;
        try {
           c = mHolder.lockCanvas( null );
           synchronized ( mHolder ) {
              doDraw( c );
           }
        } finally {
           if ( c != null ) mHolder.unlockCanvasAndPost( c );
        }
     }
     
    private void doDraw( Canvas canvas ) {
        //Log.d( "GremlinDPadView", "draw(...)" );
        //canvas.drawRect(0, 0, w, h, bgPaint);

        //canvas.drawBitmap(imgBg, 0, 0, null);
        bgShape.draw(canvas);
        gutterBgShape.draw(canvas);

        for ( int i = 0; i < gutterTickShapes.length; i++ ) {
            gutterTickShapes[i].draw(canvas);
        }

        gutterMarkShape.draw(canvas);

        canvas.drawText(Integer.toString((int)value), valueX, valueY, valuePaint);

        ballShape.draw(canvas);

        //canvas.drawBitmap(imgBall, ballLeft, ballTop, null);
    }

    /* (non-Javadoc)
     * @see android.view.View#onTouchEvent(android.view.MotionEvent)
     */
    @Override
    public boolean onTouchEvent( MotionEvent event ) {
        switch ( event.getAction() ) {
            case MotionEvent.ACTION_DOWN:
                touchStartY = (int) event.getY();
                return true;
            case MotionEvent.ACTION_MOVE:
                int y = (int) event.getY();
                moveBall( 100.0 * ( touchStartY - y ) / ((double) (h - ballH)) );
                doDraw();
                return true;
            case MotionEvent.ACTION_UP:
                centerBall();
                doDraw();
                return true;
        }

        return false;
    }

}
