package edu.hm.dako.chat.connection;

import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.hm.dako.chat.common.ChatPDU;

/**
 * Stattet ein {@link Connection} Objekt mit automatischem Logging aus. Umschliesst eine
 * beliebige Connection-Instanz und bietet dieselbe Schnittstelle an. Beim Aufruf einer
 * Methode wird zunaechst eine Log-Ausgabe getaetigt und danach die Methode der
 * umschlossenen Connection aufgerufen. Anschliessend erfolgt eine weitere Log-Ausgabe.
 *
 */
public class LoggingConnectionDecorator implements Connection {

  private static Log log = LogFactory
	.getLog(LoggingConnectionDecorator.class);

  private Connection wrappedConnection;

  public LoggingConnectionDecorator(Connection wrappedConnection) {
    this.wrappedConnection = wrappedConnection;
  }

  @Override
  public synchronized void send(Serializable message) throws Exception {
    ChatPDU pdu = (ChatPDU) message;
    log.error("Sende Nachricht, Chat-Inhalt: " + pdu.getMessage()+ ", Chat-User: " + pdu.getUserName());
    log.error("Nachrichten Typ: " + pdu.getPduType());
    wrappedConnection.send(message);
    log.trace(pdu);
    log.error("Nachricht gesendet");
  }

  @Override
  public Serializable receive() throws Exception {
    log.error("Empfange Nachricht...");
    ChatPDU pdu = (ChatPDU) wrappedConnection.receive();
    if (pdu != null) {
	log.error("Nachricht empfangen, Chat-Inhalt: " + pdu.getMessage()+ ", Chat-User: " + pdu.getUserName());
    log.error("Nachrichten Typ: " + pdu.getPduType());
	log.trace(pdu);
    }
    return pdu;
  }

  @Override
  public Serializable receive(int timeout) throws Exception {
    log.error("Empfange Nachricht...");
    ChatPDU pdu = (ChatPDU) wrappedConnection.receive(timeout);
    if (pdu != null) {
	log.error("Nachricht empfangen, Chat-Inhalt: " + pdu.getMessage()+ ", Chat-User: " + pdu.getUserName());
    log.error("Nachrichten Typ: " + pdu.getPduType());
	log.trace(pdu);
    }
    return pdu;
  }

  @Override
  public void close() throws Exception {
    log.error("Schliesse Connection...");
    wrappedConnection.close();
    log.error("Connection geschlossen!");
  }
}
