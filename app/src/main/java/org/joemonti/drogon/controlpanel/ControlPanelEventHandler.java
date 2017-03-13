/**
 * 
 */
package org.joemonti.drogon.controlpanel;


/**
 * @author joe
 *
 */
public interface ControlPanelEventHandler {
    public void onMotor( double motor );
    public void onEvent( ControlPanelEvent event );
    public void onError( String msg, Throwable t );
}
