package edu.hm.dako.chat.client;

/*
 * Thread wartet auf ankommende Nachrichten vom Server und bearbeitet diese.
 * 
 * @author
 *
 */

import edu.hm.dako.chat.common.ChatPDU;
import edu.hm.dako.chat.common.ClientConversationStatus;
import edu.hm.dako.chat.common.ExceptionHandler;
import edu.hm.dako.chat.common.PduType;
import edu.hm.dako.chat.connection.Connection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jgroups.demos.Chat;

import java.io.IOException;

/**
 * Thread wartet auf ankommende Nachrichten vom Server und bearbeitet diese.
 *
 * @author Peter Mandl
 *
 */
public class AdvancedMessageListenerThreadImpl extends AbstractMessageListenerThread {

    private static Log log = LogFactory.getLog(AdvancedMessageListenerThreadImpl.class);

    public AdvancedMessageListenerThreadImpl(ClientUserInterface userInterface,
                                           Connection con, SharedClientData sharedData) {

        super(userInterface, con, sharedData);
    }

    @Override
    protected void loginResponseAction(ChatPDU receivedPdu) {

        if (receivedPdu.getErrorCode() == ChatPDU.LOGIN_ERROR) {

            // Login hat nicht funktioniert
            log.error("Login-Response-PDU fuer Client " + receivedPdu.getUserName()+ " mit Login-Error empfangen");
            userInterface.setErrorMessage(
                    "Chat-Server", "Anmelden beim Server nicht erfolgreich, Benutzer "
                            + receivedPdu.getUserName() + " vermutlich schon angemeldet",
                    receivedPdu.getErrorCode());
            sharedClientData.status = ClientConversationStatus.UNREGISTERED;

            // Verbindung wird gleich geschlossen
            try {
                connection.close();
            } catch (Exception e) {
            }

        } else {
            // Login hat funktioniert
            log.error("Login erfolgreich abgeschlossen.");

            userInterface.loginComplete();
            sharedClientData.status = ClientConversationStatus.REGISTERED;

            Thread.currentThread().setName("Listener" + "-" + sharedClientData.userName);
            log.error("Login-Response-PDU fuer Client " + receivedPdu.getUserName() + " empfangen");
            log.error("---------------------------------------------------------------------------------");
        }
    }

    @Override
    protected void loginEventAction(ChatPDU receivedPdu) {
        log.error(receivedPdu.toString());
        log.error("Login Event PDU empfangen.");
        // Eventzaehler fuer Testzwecke erhoehen
        sharedClientData.eventCounter.getAndIncrement();

        try {
            handleUserListEvent(receivedPdu);
        } catch (Exception e) {
            ExceptionHandler.logException(e);
        }
        try {
            sendLoginMessageConfirm(receivedPdu);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void logoutResponseAction(ChatPDU receivedPdu) {
        log.error("Logout Response PDU empfangen.");
        log.error(sharedClientData.userName + " empfaengt Logout-Response-PDU fuer Client "+ receivedPdu.getUserName());
        sharedClientData.status = ClientConversationStatus.UNREGISTERED;
        log.error("Logout Vorgang abgeschlossen.");
        log.error("------------------------------------------------------------------");
        sharedClientData.messageCounter.getAndDecrement();
        userInterface.setSessionStatisticsCounter(sharedClientData.eventCounter.longValue(),
                sharedClientData.confirmCounter.longValue(), 0, 0, 0);

        log.error("Vom Client gesendete Chat-Nachrichten:  "+ sharedClientData.messageCounter.get());

        finished = true;
        userInterface.logoutComplete();
    }

    @Override
    protected void logoutEventAction(ChatPDU receivedPdu) {

        // Eventzaehler fuer Testzwecke erhoehen
        sharedClientData.eventCounter.getAndIncrement();
        log.error("Logout Event PDU empfangen.");

        try {
            handleUserListEvent(receivedPdu);
        } catch (Exception e) {
            ExceptionHandler.logException(e);
        }

        try{
            sendLogoutMessageConfirm(receivedPdu);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void chatMessageResponseAction(ChatPDU receivedPdu) {

        log.error("Sequenznummer der Chat-Response-PDU " + receivedPdu.getUserName() + ": "+ receivedPdu.getSequenceNumber() + ", Messagecounter: "+ sharedClientData.messageCounter.get());

        log.error(Thread.currentThread().getName()+ ", Benoetigte Serverzeit gleich nach Empfang der Response-Nachricht: "+ receivedPdu.getServerTime() + " ns = " + receivedPdu.getServerTime() / 1000000+ " ms");

        //if (receivedPdu.getSequenceNumber() == sharedClientData.messageCounter.get()) {

            // Zuletzt gemessene Serverzeit fuer das Benchmarking
            // merken
            userInterface.setLastServerTime(receivedPdu.getServerTime());

            // Naechste Chat-Nachricht darf eingegeben werden
            userInterface.setLock(false);

            log.error("Chat-Response-PDU fuer Client " + receivedPdu.getUserName() + " empfangen");
            log.error("Chat Message Vorgang erfolgreich abgeschlossen.");
            log.error("-----------------------------------------------------------------------------");

        /*} else {
            log.error("Sequenznummer der Chat-Response-PDU " + receivedPdu.getUserName()
                    + " passt nicht: " + receivedPdu.getSequenceNumber() + "/"
                    + sharedClientData.messageCounter.get());
        }*/
    }

    @Override
    protected void chatMessageEventAction(ChatPDU receivedPdu) {
        sharedClientData.confirmCounter.getAndIncrement();

        log.error("Chat-Message-Event-PDU von " + receivedPdu.getEventUserName() + " empfangen");

        // Eventzaehler fuer Testzwecke erhoehen
        sharedClientData.eventCounter.getAndIncrement();

        // Empfangene Chat-Nachricht an User Interface zur
        // Darstellung uebergeben
        userInterface.setMessageLine(receivedPdu.getEventUserName(),
                (String) receivedPdu.getMessage());
        try {
            sendChatMessageConfirm(receivedPdu);
        }
        catch (IOException e){
            ExceptionHandler.logException(e);
        }

    }



    /**
     * Bearbeitung aller vom Server ankommenden Nachrichten
     */
    public void run() {

        ChatPDU receivedPdu = null;

        log.error("AdvancedMessageListenerThread gestartet");

        while (!finished) {

            try {
                // Naechste ankommende Nachricht empfangen
                log.error("Auf die naechste Nachricht vom Server warten");
                receivedPdu = receive();
                log.error("Nach receive Aufruf, ankommende PDU mit PduType = "+ receivedPdu.getPduType());
            } catch (Exception e) {
                finished = true;
            }

            if (receivedPdu != null) {

                switch (sharedClientData.status) {

                    case REGISTERING:

                        switch (receivedPdu.getPduType()) {

                            case LOGIN_RESPONSE:
                                // Login-Bestaetigung vom Server angekommen
                                loginResponseAction(receivedPdu);

                                break;

                            case LOGOUT_RESPONSE:
                                // Logout-Bestaetigung vom Server angekommen
                                logoutResponseAction(receivedPdu);

                                break;

                            case CHAT_MESSAGE_RESPONSE:
                                // Chat Message Bestaetigung vom Server angekommen
                                chatMessageResponseAction(receivedPdu);

                                break;

                            case LOGIN_EVENT:
                                // Meldung vom Server, dass sich die Liste der
                                // angemeldeten User erweitert hat
                                loginEventAction(receivedPdu);


                                break;
                            case LOGOUT_EVENT:
                                // Meldung vom Server, dass sich die Liste der
                                // angemeldeten User veraendert hat
                                logoutEventAction(receivedPdu);

                                break;

                            case CHAT_MESSAGE_EVENT:
                                // Chat-Nachricht vom Server gesendet
                                chatMessageEventAction(receivedPdu);
                                log.error("Ankommende PDU im Zustand " +receivedPdu.getPduType() + " von Client " +receivedPdu.getUserName());
                                break;

                            default:
                                log.error("Ankommende PDU im Zustand " + sharedClientData.status+ " wird verworfen " +receivedPdu.getUserName());
                        }
                        break;

                    case REGISTERED:

                        switch (receivedPdu.getPduType()) {

                            case CHAT_MESSAGE_RESPONSE:

                                // Die eigene zuletzt gesendete Chat-Nachricht wird vom
                                // Server bestaetigt.
                                chatMessageResponseAction(receivedPdu);
                                break;

                            case CHAT_MESSAGE_EVENT:
                                // Chat-Nachricht vom Server gesendet
                                chatMessageEventAction(receivedPdu);
                                break;

                            case LOGIN_EVENT:
                                // Meldung vom Server, dass sich die Liste der
                                // angemeldeten User erweitert hat
                                loginEventAction(receivedPdu);

                                break;

                            case LOGOUT_EVENT:
                                // Meldung vom Server, dass sich die Liste der
                                // angemeldeten User veraendert hat
                                logoutEventAction(receivedPdu);

                                break;

                            case LOGOUT_RESPONSE:
                                // Logout-Bestaetigung vom Server angekommen
                                logoutResponseAction(receivedPdu);

                                break;


                            default:
                                log.error("Ankommende PDU im Zustand " + sharedClientData.status+ " wird verworfen");
                        }
                        break;

                    case UNREGISTERING:

                        switch (receivedPdu.getPduType()) {

                            case CHAT_MESSAGE_EVENT:
                                // Chat-Nachricht vom Server gesendet
                                chatMessageEventAction(receivedPdu);
                                break;

                            case LOGOUT_RESPONSE:
                                // Bestaetigung des eigenen Logout
                                logoutResponseAction(receivedPdu);
                                break;

                            case LOGIN_EVENT:
                                // Meldung vom Server, dass sich die Liste der
                                // angemeldeten User erweitert hat
                                loginEventAction(receivedPdu);

                                break;

                            case LOGOUT_EVENT:
                                // Meldung vom Server, dass sich die Liste der
                                // angemeldeten User veraendert hat
                                logoutEventAction(receivedPdu);

                                break;

                            default:
                                log.error("Ankommende PDU im Zustand " + sharedClientData.status+ " wird verworfen");
                                break;
                        }
                        break;

                    case UNREGISTERED:
                        log.error("Ankommende PDU im Zustand " + sharedClientData.status + " wird verworfen");

                        break;

                    default:
                        log.error("Unzulaessiger Zustand " + sharedClientData.status);
                }
            }
        }

        // Verbindung noch schliessen
        try {
            connection.close();
        } catch (Exception e) {
            ExceptionHandler.logException(e);
        }
        log.error("Ordnungsgemaesses Ende des AdvancedMessageListener-Threads fuer User"+ sharedClientData.userName + ", Status: " + sharedClientData.status);
    } // run

    protected void sendChatMessageConfirm(ChatPDU receivedPdu)throws IOException{

        ChatPDU confirmPdu = new ChatPDU();
        confirmPdu= ChatPDU.createChatMessageConfirmPdu(receivedPdu.getUserName(), receivedPdu);
        sharedClientData.messageCounter.getAndIncrement();
        try {
            connection.send(confirmPdu);
            log.error("Chat-Message-Confirm-PDU fuer Client " + confirmPdu.getUserName()+ " an Server gesendet");
            log.error("MessageCounter: " + sharedClientData.messageCounter.get()+ ", SequenceNumber: " + confirmPdu.getSequenceNumber());
        } catch (Exception e) {
            log.error("Senden der Chat-Nachricht nicht moeglich");
            throw new IOException();
        }
    }

    protected void sendLoginMessageConfirm(ChatPDU receivedPdu) throws IOException{
        ChatPDU confirmPdu;
        confirmPdu = ChatPDU.createLoginConfirmPdu(receivedPdu.getUserName(),receivedPdu);
        sharedClientData.messageCounter.getAndIncrement();
        try {
            connection.send(confirmPdu);
            log.error("Login-Confirm-PDU fuer Client " + confirmPdu.getUserName()+ " an Server gesendet");
            log.error("MessageCounter: " + sharedClientData.messageCounter.get()+ ", SequenceNumber: " + confirmPdu.getSequenceNumber());
        } catch (Exception e) {
            log.error("Senden der Chat-Nachricht nicht moeglich");
            throw new IOException();
        }
    }

    protected void sendLogoutMessageConfirm(ChatPDU receivedPdu)throws IOException{
        ChatPDU confirmPdu = new ChatPDU();
        confirmPdu.setPduType(PduType.LOGOUT_EVENT_CONFIRM);
        confirmPdu.setClientStatus(ClientConversationStatus.UNREGISTERING);
        confirmPdu.setServerThreadName(receivedPdu.getServerThreadName());
        confirmPdu.setClientThreadName(Thread.currentThread().getName());
        confirmPdu.setUserName(receivedPdu.getUserName());
        confirmPdu.setEventUserName(receivedPdu.getEventUserName());
        try {
            connection.send(confirmPdu);
            log.error("Logout-Confirm-PDU fuer Client " + confirmPdu.getUserName()+ " an Server gesendet");
            log.error("MessageCounter: " + sharedClientData.messageCounter.get()+ ", SequenceNumber: " + confirmPdu.getSequenceNumber());
        } catch (Exception e) {
            log.error("Senden der Chat-Nachricht nicht moeglich");
            throw new IOException();
        }
    }
}