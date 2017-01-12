package edu.hm.dako.chat.server;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Globale Zaehler fuer Logouts, gesendete Events und empfangene Confirms nur zum Testen
 * @author Peter Mandl
 */
public class SharedServerCounter {
	public AtomicInteger logoutCounter;
	public AtomicInteger eventCounter;
	public AtomicInteger confirmCounter;
}
