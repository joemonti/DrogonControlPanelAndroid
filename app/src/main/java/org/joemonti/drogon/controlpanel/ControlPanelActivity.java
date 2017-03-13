/**
 * 
 */
package org.joemonti.drogon.controlpanel;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/**
 * @author joe
 *
 */
public class ControlPanelActivity extends Activity implements ControlPanelEventHandler {

    boolean mConnected = false;
    boolean mArmed = false;
    EditText txtHost;
    Button btnConnect;
    Button btnArm;

    TextView tvDebug;
    String debugMessage;

    DrogonClient drogonClient;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow( ).setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_drogon_control_panel);

        txtHost = (EditText) this.findViewById( R.id.txtHost );

        SharedPreferences sharedPref = PreferenceManager
                .getDefaultSharedPreferences(ControlPanelActivity.this);
        if ( sharedPref.contains( "host" ) ) {
            txtHost.setText( sharedPref.getString( "host", "" ) );
        }

        PowerSlider powerSlider = (PowerSlider) this.findViewById( R.id.powerSlider1 );
        powerSlider.setHandler(this);

        drogonClient = new DrogonClient(new DrogonClient.IDrogonClientLogger() {
            @Override
            public void debug(String msg) {
                writeDebugMessage(msg);
            }
        });

        debugMessage = "";
        tvDebug = (TextView) this.findViewById(R.id.tvDebug);
        writeDebugMessage("Welcome to Drogon Control Panel!");

        btnConnect = (Button) this.findViewById( R.id.btnConnect );

        btnConnect.setOnClickListener( new View.OnClickListener( ) {
            public void onClick( View v ) {
                if (mConnected) {
                    disconnect();
                } else {
                    connect();
                }
            }
        } );

        btnArm = (Button) this.findViewById( R.id.btnArm );
        btnArm.setOnClickListener( new View.OnClickListener( ) {
            public void onClick( View v ) {
                if (!mConnected) return;

                if (mArmed) {
                    btnArm.setBackgroundResource(R.drawable.red_button);
                    btnArm.setText("Arm");
                    writeDebugMessage("Disarming...");
                    drogonClient.updateArmed(false);
                } else {
                    btnArm.setBackgroundResource(R.drawable.green_button);
                    btnArm.setText("Disarm");
                    writeDebugMessage("Arming...");
                    drogonClient.updateArmed(true);
                }

                mArmed = !mArmed;
            }
        } );

        //Intent intent = getIntent( );

        //String host = intent.getExtras( ).getString( "host" );
        
        //RPiVideoView video = (RPiVideoView) this
        //        .findViewById( R.id.video );
        //video.setHost( host );
        //video.setEventHandler( this );
        //video.setShowFps( true );
    }

    private void connect() {
        if (!mConnected) {
            String host = txtHost.getText().toString();
            if (host.length() > 0) {
                SharedPreferences sharedPref = PreferenceManager
                        .getDefaultSharedPreferences(ControlPanelActivity.this);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString("host", host);
                editor.commit();

                //writeDebugMessage("Connecting to " + host);

                drogonClient.connect(host);

                btnConnect.setText("Disconnect");
                mConnected = true;
            }
        }
    }

    private void disconnect() {
        if (mConnected) {
            drogonClient.disconnect();
            //writeDebugMessage("Disconnected");

            btnConnect.setText("Connect");
            mConnected = false;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        disconnect();
    }

    private void showShortToast(String msg ) {
        int duration = Toast.LENGTH_SHORT;
        Toast toast = Toast.makeText( getApplicationContext( ), msg, duration );
        toast.show( );
    }

    @Override
    public void onError( final String msg, final Throwable t ) {
        Log.e( "GremlinControlActivity", msg, t );

        runOnUiThread( new Runnable( ) {
            public void run() {
                showShortToast( msg );
                //finish( );
            }
        } );
    }

    @Override
    public void onMotor(double motor) {
        if (!mConnected) return;

        drogonClient.updateMotor(motor, motor > 0.0);
    }

    @Override
    public void onEvent( final ControlPanelEvent event ) {
        /*
        runOnUiThread( new Runnable( ) {
            public void run() {
                txtServo0.setText( Integer.toString( event.servo0 ) );
                txtServo1.setText( Integer.toString( event.servo1 ) );
            }
        } );
        */
    }

    public void writeDebugMessage(final String msg) {
        runOnUiThread( new Runnable( ) {
            public void run() {
                debugMessage += "\n" + msg;
                tvDebug.setText(debugMessage);
            }
        } );
    }
}
