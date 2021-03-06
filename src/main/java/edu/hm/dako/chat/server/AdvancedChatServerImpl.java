package edu.hm.dako.chat.server;

import edu.hm.dako.chat.common.ClientListEntry;
import edu.hm.dako.chat.common.ExceptionHandler;
import edu.hm.dako.chat.connection.Connection;
import edu.hm.dako.chat.connection.ServerSocketInterface;
import javafx.concurrent.Task;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Advanced-Chat-Server-Implementierung Der ChatServer wird in einem eigenen
 * Thread gestartet. Er nimmt alle Verbindungsaufbauwuensche der ChatClients
 * entgegen und startet fuer jede Verbindung jeweils einen eigenen Worker-Thread
 * @author NONAME
 */
public class AdvancedChatServerImpl extends AbstractChatServer {

    /**
     * the log data contains the info about the protocoll for debug purpose
     */
    private static Log log = LogFactory.getLog(AdvancedChatServerImpl.class);

    /**
     * Threadpool fuer Worker-Threads
     */
    private final ExecutorService executorService;

    /**
     * Socket fuer den Listener, der alle Verbindungsaufbauwuensche der Clients entgegennimmt
     */
    private ServerSocketInterface socket;

    /**
     * contains important infos about the whole server infrastructure
     * @param executorService threadpool for worker threads
     * @param socket socket for listener
     * @param serverGuiInterface The GUI for the server
     */
    public AdvancedChatServerImpl(ExecutorService executorService,
                                ServerSocketInterface socket, ChatServerGuiInterface serverGuiInterface) {
        //log.debug("AdvancedChatServerImpl konstruiert");
        this.executorService = executorService;
        this.socket = socket;
        this.serverGuiInterface = serverGuiInterface;
        counter = new SharedServerCounter();
        counter.logoutCounter = new AtomicInteger(0);
        counter.eventCounter = new AtomicInteger(0);
        counter.confirmCounter = new AtomicInteger(0);

    }

    /**
     * The method starts the server
     */
    @Override
    public void start() {
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                // Clientliste erzeugen
                clients = SharedChatClientList.getInstance();

                while (!Thread.currentThread().isInterrupted() && !socket.isClosed()) {
                    try {
                        // Auf ankommende Verbindungsaufbauwuensche warten
                        System.out.println(
                                "AdvancedChatServer wartet auf Verbindungsanfragen von Clients...");

                        Connection connection = socket.accept();
                        //log.debug("Neuer Verbindungsaufbauwunsch empfangen");

                        // Neuen Workerthread starten
                        executorService.submit(new AdvancedChatWorkerThreadImpl(connection, clients,
                                counter, serverGuiInterface));
                    } catch (Exception e) {
                        if (socket.isClosed()) {
                            //log.debug("Socket wurde geschlossen");
                        } else {
                            //log.error("Exception beim Entgegennehmen von Verbindungsaufbauwuenschen: " + e);
                            ExceptionHandler.logException(e);
                        }
                    }
                }
                return null;
            }
        };

        Thread th = new Thread(task);
        th.setDaemon(true);
        th.start();

    }

    /**
     *  the method shuts the server down
     */
    @Override
    public void stop() throws Exception {

        // Alle Verbindungen zu aktiven Clients abbauen
        Vector<String> sendList = clients.getClientNameList();
        for (String s : new Vector<String>(sendList)) {
            ClientListEntry client = clients.getClient(s);
            try {
                if (client != null) {
                    client.getConnection().close();
                    //log.error("Verbindung zu Client " + client.getUserName() + " geschlossen");
                }
            } catch (Exception e) {
                //log.debug("Fehler beim Schliessen der Verbindung zu Client " + client.getUserName());
                ExceptionHandler.logException(e);
            }
        }

        // Loeschen der Userliste
        clients.deleteAll();
        Thread.currentThread().interrupt();
        socket.close();
        //log.debug("Listen-Socket geschlossen");
        executorService.shutdown();
        //log.debug("Threadpool freigegeben");

        System.out.println("AdvancedChatServer beendet sich");
    }
}