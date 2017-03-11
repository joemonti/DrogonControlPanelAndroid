/**
 * 
 */
package org.joemonti.drogon.controlpanel;

/**
 * @author joe
 *
 */
public class ControlPanelEvent {
    final byte servo0;
    final byte servo1;
    
    /**
     * Servo event.
     * 
     * @param servo0  servo 0 value, -100 to 100
     * @param servo1  servo 1 value, -100 to 100
     */
    ControlPanelEvent( byte servo0, byte servo1 ) {
       this.servo0 = servo0;
       this.servo1 = servo1;
    }
}
