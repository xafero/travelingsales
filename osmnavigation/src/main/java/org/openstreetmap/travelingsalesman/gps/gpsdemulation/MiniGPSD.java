/**
 * MiniGPSD.java
 * (c) 2008 by <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 * This file is part of osmnavigation by Marcus Wolschon <a href="mailto:Marcus@Wolscon.biz">Marcus@Wolscon.biz</a>.
 *
 *  osmnavigation is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  osmnavigation is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with osmnavigation.  If not, see <http://www.gnu.org/licenses/>.
 *
 ***********************************
 * Editing this file:
 *  -For consistent code-quality this file should be checked with the
 *   checkstyle-ruleset enclosed in this project.
 *  -After the design of this file has settled it should get it's own
 *   JUnit-Test that shall be executed regularly. It is best to write
 *   the test-case BEFORE writing this class and to run it on every build
 *   as a regression-test.
 */
package org.openstreetmap.travelingsalesman.gps.gpsdemulation;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;


import org.openstreetmap.osm.Settings;
import org.openstreetmap.travelingsalesman.gps.GpsDProvider;
import org.openstreetmap.travelingsalesman.gps.IGPSProvider.IExtendedGPSListener;

/**
 * This class emulates a GPSD.
 * It is fairly useful to have Traveling Salesman provide
 * a stripepd down GPSD on platforms like Windows, where no
 * native GPSD exists. This way we can e.g. start JOSM with
 * the LiveGPS+Surveyor-Plugin.
 *
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class MiniGPSD extends Thread implements IExtendedGPSListener {

    /**
     * Queues with data to write will have no more then this many lines waiting.
     */
    private static final int MAXQUEUELENGTH = 10;

    /**
     * my logger for debug and error-output.
     */
    static final Logger LOG = Logger.getLogger(MiniGPSD.class.getName());

    /**
     * The size of {@link #readBuffer}.
     */
    private static final int READBUFFERSIZE = 8192;

    /**
     * Key for {@link Settings} for the port we listen to.
     * If the value is <1 this class is disabled.
     */
    public static final String SETTINGS_GPSDEMULATION_PORT = "emulatedgpsd.port";

    /**
     * The channel on which we'll accept connections.
     */
    private ServerSocketChannel serverChannel;

    /**
     * The selector we'll be monitoring.
     */
    private Selector selector;

    /**
     * Pending data to send to our clients.
     * Maps a SocketChannel to a list of ByteBuffer instances.
     */
    private Map<Socket, LinkedList<ByteBuffer>> pendingData = new HashMap<Socket, LinkedList<ByteBuffer>>();

    /**
     * The buffer into which we'll read data when it's available.
     */
    private ByteBuffer readBuffer = ByteBuffer.allocate(READBUFFERSIZE);

    /**
     * The last Latitude we have seen.
     */
    private double lastLat;

    /**
     * The last Longitude we have seen.
     */
    private double lastLon;

    /**
     * Read the configuration and if we are to run,
     * start the gps-daemon as a daemon-thread.
     */
    public MiniGPSD() {
        startListening();
    }

    /**
     * Did we already report that an error starting the gpsd happened?
     */
    private boolean isErrorStartingReported = false;

    /**
     * Get the port and start listening to incomming tcp-connections.
     * It is save to call this method multiple times.
     */
    private void startListening() {
        if (this.selector == null)
        try {
            int port = Settings.getInstance().getInteger(SETTINGS_GPSDEMULATION_PORT, GpsDProvider.DEFAULTPORT);
            if (port > 0) {
                this.selector = this.initSelector(port);
                this.setDaemon(true);
                this.start();
                LOG.log(Level.INFO, "Our internal GPSD-server is now listening for connections.");
            } // else {
            //    LOG.log(Level.INFO, "Our internal GPSD-server is disabled. Not starting");
            //}
        } catch (IOException e) {
            if (!isErrorStartingReported) {
                LOG.log(Level.SEVERE, "Error starting internal GPSD-server", e);
                isErrorStartingReported = true;
            }
        }
    }

    /**
     * set up but do not yet start the gpsd-server as using async IO.
     * @param port the port to listen on
     * @return the selector for async IO
     * @throws IOException if we cannot accept connections.
     */
    private Selector initSelector(final int port) throws IOException {
        // Create a new selector
        Selector socketSelector = SelectorProvider.provider().openSelector();

        // Create a new non-blocking server socket channel
        this.serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);

        // Bind the server socket to the specified address and port
        InetSocketAddress isa = new InetSocketAddress(port);
        serverChannel.socket().bind(isa);

        // Register the server socket channel, indicating an interest in
        // accepting new connections
        serverChannel.register(socketSelector, SelectionKey.OP_ACCEPT);

        return socketSelector;
    }

    /**
     * As long as we are not interrupted,
     * work on async-IO -requests.
     */
    public void run() {
        while (!isInterrupted()) {
          try {
            // Wait for an event one of the registered channels
            this.selector.select();

            // Iterate over the set of keys for which events are available
            Iterator<SelectionKey> selectedKeys = this.selector.selectedKeys().iterator();
            while (selectedKeys.hasNext()) {
              SelectionKey key = selectedKeys.next();
              selectedKeys.remove();

              if (!key.isValid()) {
                continue;
              }

              // Check what event is available and deal with it
              if (key.isAcceptable()) {
                this.accept(key);
              }

              if (key.isReadable()) {
                  this.read(key);
              }

              if (key.isWritable()) {
                  this.write(key);
             }
            }
          } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error in internal GPSD-server", e);
          }
        }
        LOG.log(Level.INFO, "internal GPSD-server was interrupted and is shutting down");
        try {
             // close all connections
             this.serverChannel.close();
        } catch (IOException e) {
             LOG.log(Level.SEVERE, "Cannot close ServerChannel of MiniGPSD", e);
        }
      }

    /**
     * Handle new incomming connections.
     * @param key the key for the selector
     * @throws IOException if we cannot accept connections
     */
    private void accept(final SelectionKey key) throws IOException {
        // For an accept to be pending the channel must be a server socket channel.
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();

        // Accept the connection and make it non-blocking
        SocketChannel socketChannel = serverSocketChannel.accept();
        LOG.log(Level.FINE, "accepted new client");
        Socket socket = socketChannel.socket();
        socketChannel.configureBlocking(false);

        synchronized (pendingData) {
            LinkedList<ByteBuffer> queue = new LinkedList<ByteBuffer>();
            //GPSD-greeting
//            byte[] greeting = ("GPSD").getBytes();
//            queue.push(ByteBuffer.wrap(greeting));
            // give the new client an initial location-fix
            if (this.lastLat != 0 && this.lastLon != 0) {
                byte[] dataToBroadcast = ("GPSD,P=" + this.lastLat + " " + this.lastLon + "\n").getBytes();
                queue.addLast(ByteBuffer.wrap(dataToBroadcast));
            }
            this.pendingData.put(socket, queue);
        }

        // Register the new SocketChannel with our Selector, indicating
        // we'd like to be notified when there's data waiting to be read
        socketChannel.register(this.selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
        this.selector.wakeup();
    }

    /**
     * Write pending data to the SocketChannel belonging to the given key.
     * @param key the key to work on.
     * @throws IOException if we cannot write our data
     * @see {@link #pendingData}
     */
    private void write(final SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();

        synchronized (this.pendingData) {
          LinkedList<ByteBuffer> queue = this.pendingData.get(socketChannel.socket());

          if (queue == null) {
              queue = new LinkedList<ByteBuffer>();
              this.pendingData.put(socketChannel.socket(), queue);
          }
          // Write until there's not more data ...
          while (!queue.isEmpty()) {
            ByteBuffer buf = queue.peek();
            String sending = new String(buf.array());
            LOG.log(Level.FINE, "Writing some data to our client '"
                    + sending + "'");
            socketChannel.write(buf);
            if (buf.remaining() > 0) {
              // ... or the socket's buffer fills up
              break;
            }
            // we have written all of the buffer,
            // remove it from the queue
            queue.poll();
          }

          if (queue.isEmpty()) {
            // We wrote away all data, so we're no longer interested
            // in writing on this socket. Switch back to waiting for
            // data.
            key.interestOps(SelectionKey.OP_READ);
          }
        }
      }

    /**
     * There are some bytes to be reat from the Socket.
     * @param key the key for the connection to read from
     * @throws IOException if we cannot read
     */
    private void read(final SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();

        // Clear out our read buffer so it's ready for new data
        this.readBuffer.clear();

        // Attempt to read off the channel
        int numRead;
        try {
          numRead = socketChannel.read(this.readBuffer);
        } catch (IOException e) {
          // The remote forcibly closed the connection, cancel
          // the selection key and close the channel.
          key.cancel();
          socketChannel.close();
          synchronized (this.pendingData) {
              this.pendingData.remove(key);
          }
          LOG.log(Level.FINE, "connection to client broken");
          return;
        }

        if (numRead == -1) {
          // Remote entity shut the socket down cleanly. Do the
          // same from our end and cancel the channel.
          key.channel().close();
          key.cancel();
          synchronized (this.pendingData) {
              this.pendingData.remove(key);
          }
          LOG.log(Level.FINE, "connection to client closed");
          return;
        }

        LOG.log(Level.FINEST, "reat data from client");
        // Hand the data off to our worker thread
        //we ignore all received commands
        //but we may need to actually handle the reat data later
        //this.worker.processData(this, socketChannel, this.readBuffer.array(), numRead);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void gpsCourseChanged(final double aCourse) {
        // ignored
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void gpsLocationChanged(final double aLat, final double aLon) {

        if (LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.FINE, "new location: lat=" + aLat + " lon=" + aLon
                    + " #clients=" + this.pendingData.keySet().size());
        }
        this.lastLat = aLat;
        this.lastLon = aLon;
        startListening();
        //byte[] dataToBroadcast = ("GPSD,O=? ? " + aLat + " " + aLon + " ? ? ? ? ? ? ? ? ? ?").getBytes();
        byte[] dataToBroadcast = ("GPSD,P=" + aLat + " " + aLon + "\n").getBytes();
        broadcastData(dataToBroadcast);
    }

    /**
     * @param dataToBroadcast send this data to all listening clients.
     */
    private void broadcastData(final byte[] dataToBroadcast) {
        if (this.selector != null)
        try {
            synchronized (this.pendingData) {
                Set<Socket> channels = this.pendingData.keySet();
                for (Socket socket : channels) {
                    SelectionKey selectionKey = socket.getChannel().keyFor(this.selector);
                    LinkedList<ByteBuffer> queue = this.pendingData.get(socket);
                    // prevent excessive growth of the queue
                    if (queue.size() < MAXQUEUELENGTH) {
                        queue.addLast(ByteBuffer.wrap(dataToBroadcast));
                    }
                    if (selectionKey != null && selectionKey.channel() != null) {
                        selectionKey.interestOps((SelectionKey.OP_READ | SelectionKey.OP_WRITE) & selectionKey.channel().validOps());
                    }
                }
            }
            this.selector.wakeup();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error broadcasting some data", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void gpsLocationLost() {
        byte[] dataToBroadcast = "GPSD,O=?\n".getBytes();
        broadcastData(dataToBroadcast);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void gpsLocationObtained() {
        // ignored
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void gpsAltitudeChanged(final double aAltitude) {
     // ignored
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void gpsDateTimeChanged(final long aDate, final long aTime) {
     // ignored
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void gpsDopChanged(final double aHdop, final double aVdop, final double aPdop) {
     // ignored
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void gpsFixQualityChanged(final int aFixQuality) {
     // ignored
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void gpsSpeedChanged(final double aSpeed) {
     // ignored
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void gpsUsedSattelitesChanged(final int aSatellites) {
        // ignored
    }

}
