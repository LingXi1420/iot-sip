package com.cmiot.sip_demo.test;

import javax.media.*;



public class StateHelper implements javax.media.ControllerListener {

    Player player = null;

    boolean configured = false;

    boolean realized = false;

    boolean prefetched = false;

    boolean eom = false;//End of media.

    boolean failed = false;

    boolean closed = false;

    public StateHelper(Player p) {

       player = p;

       p.addControllerListener(this);

    }

    /**

     * To judge whether the processor is configured.

       * Configure the processor in the given time which is limited

       * by the timeOutMillis.Once a Processor is Configured, you

       * can set its output format and TrackControl options.

       */

    public boolean configure(int timeOutMillis) {

       long startTime = System.currentTimeMillis();

       synchronized (this) {

           if (player instanceof Processor)

            ((Processor)player).configure();

           else

            return false;

           while (!configured && !failed) {

            try {

                wait(timeOutMillis);

            } catch (InterruptedException ie) {

            }

            if (System.currentTimeMillis() - startTime > timeOutMillis)

                break;

           }

       }

       return configured;

    }

    /**

     * To judge whether the playerr is realized.

     */

    public boolean realize(int timeOutMillis) {

       long startTime = System.currentTimeMillis();

       synchronized (this) {

           player.realize();

           while (!realized && !failed) {

            try {

                wait(timeOutMillis);

            } catch (InterruptedException ie) {

            }

            if (System.currentTimeMillis() - startTime > timeOutMillis)

                break;

           }

       }

       return realized;

    }

    /**

     * To judge whether the player is prefetched.

     */

    public boolean prefetch(int timeOutMillis) {

       long startTime = System.currentTimeMillis();

       synchronized (this) {

           player.prefetch();

           while (!prefetched && !failed) {

            try {

                wait(timeOutMillis);

            } catch (InterruptedException ie) {

            }

            if (System.currentTimeMillis() - startTime > timeOutMillis)

                break;

           }

       }

       return prefetched && !failed;

    }

    /**

     * To judge whether the player has finished.

     */

    public boolean playToEndOfMedia(int timeOutMillis) {

       long startTime = System.currentTimeMillis();

       eom = false;

       synchronized (this) {

           player.start();

           while (!eom && !failed) {

            try {

                wait(timeOutMillis);

            } catch (InterruptedException ie) {

            }

            if (System.currentTimeMillis() - startTime > timeOutMillis)

                break;

           }

       }

       return eom && !failed;

    }

    public void close() {

       synchronized (this) {

           player.close();

           while (!closed) {

            try {

                wait(100);

            } catch (InterruptedException ie) {

            }

           }

       }

       player.removeControllerListener(this);

    }

    public synchronized void controllerUpdate(ControllerEvent ce) {

       if (ce instanceof RealizeCompleteEvent) {

           realized = true;

       } else if (ce instanceof ConfigureCompleteEvent) {

           configured = true;

       } else if (ce instanceof PrefetchCompleteEvent) {

           prefetched = true;

       } else if (ce instanceof EndOfMediaEvent) {

           eom = true;

       } else if (ce instanceof ControllerErrorEvent) {

           failed = true;

       } else if (ce instanceof ControllerClosedEvent) {

           closed = true;

       } else {

           return;

       }

       notifyAll();

    }

}