package edu.hm.dako.chat.server;

import java.util.Vector;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import edu.hm.dako.chat.common.ChatPDU;
import edu.hm.dako.chat.common.ClientConversationStatus;
import edu.hm.dako.chat.common.ClientListEntry;
import edu.hm.dako.chat.common.ExceptionHandler;
import edu.hm.dako.chat.connection.Connection;
import edu.hm.dako.chat.connection.ConnectionTimeoutException;
import edu.hm.dako.chat.connection.EndOfFileException;
import org.jgroups.demos.Chat;

/**
 * Worker-Thread zur serverseitigen Bedienung einer Session mit einem Client.
 * Jedem Chat-Client wird serverseitig ein Worker-Thread zugeordnet.
 * @author NONAME
 */
public class AdvancedChatWorkerThreadImpl extends AbstractWorkerThread {

    private static Log log = LogFactory.getLog(AdvancedChatWorkerThreadImpl.class);

    public AdvancedChatWorkerThreadImpl(Connection con, SharedChatClientList clients,
                                        SharedServerCounter counter, ChatServerGuiInterface
                                        serverGuiInterface) {
        super(con, clients, counter, serverGuiInterface);
    }

    @Override
    public void run() {

    }
}
