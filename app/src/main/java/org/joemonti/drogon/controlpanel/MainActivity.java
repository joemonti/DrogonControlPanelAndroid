package org.joemonti.drogon.controlpanel;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class MainActivity extends Activity {
    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_main );

        SharedPreferences sharedPref = PreferenceManager
                .getDefaultSharedPreferences( MainActivity.this );
        if ( sharedPref.contains( "host" ) ) {
            EditText txtHost = (EditText) MainActivity.this
                    .findViewById( R.id.txtHost );
            txtHost.setText( sharedPref.getString( "host", "" ) );
        }

        Button btnConnect = (Button) this.findViewById( R.id.btnConnect );

        btnConnect.setOnClickListener( new OnClickListener( ) {
            public void onClick( View v ) {
                EditText txtHost = (EditText) MainActivity.this
                        .findViewById( R.id.txtHost );
                String host = txtHost.getText( ).toString( );
                if ( host.length( ) > 0 ) {

                    SharedPreferences sharedPref = PreferenceManager
                            .getDefaultSharedPreferences( MainActivity.this );
                    Editor editor = sharedPref.edit( );
                    editor.putString( "host", host );
                    editor.commit( );

                    Intent intent = new Intent( MainActivity.this,
                            ControlPanelActivity.class );
                    intent.putExtra( "host", host );
                    startActivity( intent );
                }
            }
        } );
    }

    @Override
    public boolean onCreateOptionsMenu( Menu menu ) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater( ).inflate( R.menu.main, menu );
        return true;
    }
}
