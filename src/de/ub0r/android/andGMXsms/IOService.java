/*
 * Copyright (C) 2009 Felix Bechstein
 * 
 * This file is part of AndGMXsms.
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; If not, see <http://www.gnu.org/licenses/>.
 */
package de.ub0r.android.andGMXsms;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

/**
 * IOService handles all IO as a service. Call it with RPC!
 * 
 * @author flx
 */
public class IOService extends Service {
	/** Tag for output. */
	private static final String TAG = "WebSMS.IO";

	/** Ref to single instance. */
	private static IOService me = null;

	/** Number of jobs running. */
	private static int currentIOOps = 0;

	/** Notification ID of this Service. */
	private static int NOTIFICATION_PENDING = 0;

	/**
	 * Is some client bound to this service? IO Tasks can kill this service, if
	 * no Client is bound and all IO is done.
	 */
	private static boolean isBound = false;

	/** The IBinder RPC Interface. */
	private final IIOOp.Stub mBinder = new IIOOp.Stub() {
		public void sendMessage(final int connector, final String[] params) {
			Connector.send(IOService.this, (short) connector, params);
		}
	};

	/**
	 * Called on bind().
	 * 
	 * @param intent
	 *            intend called
	 * @return RPC callback
	 */
	@Override
	public final IBinder onBind(final Intent intent) {
		Log.d(TAG, "onBind()");
		isBound = true;
		return this.mBinder;
	}

	/**
	 * Called when all clients have disconnected from a particular interface
	 * published by the service.
	 * 
	 * @param intent
	 *            The Intent that was used to bind to this service, as given to
	 *            Context.bindService. Note that any extras that were included
	 *            with the Intent at that point will not be seen here.
	 * @return Return true if you would like to have the service's
	 *         onRebind(Intent) method later called when new clients bind to it.
	 */
	@Override
	public final boolean onUnbind(final Intent intent) {
		Log.d(TAG, "onUnbind()");
		Log.d(TAG, "currentIOOps=" + currentIOOps);
		isBound = false;
		if (currentIOOps <= 0) {
			this.stopSelf();
		}
		Log.d(TAG, "onUnbind() return true");
		return true;
	}

	/**
	 * Called when new clients have connected to the service, after it had
	 * previously been notified that all had disconnected in its
	 * onUnbind(Intent). This will only be called if the implementation of
	 * onUnbind(Intent) was overridden to return true.
	 * 
	 * @param intent
	 *            The Intent that was used to bind to this service, as given to
	 *            Context.bindService. Note that any extras that were included
	 *            with the Intent at that point will not be seen here.
	 */
	@Override
	public final void onRebind(final Intent intent) {
		isBound = true;
	}

	/**
	 * Called on Service start.
	 */
	@Override
	public final void onCreate() {
		super.onCreate();
		Log.d(TAG, "onCreate()");
		// Don't kill me!
		this.setForeground(true);
		// use startForeground() / sopForeground() // FIXME: for API5
		me = this;
	}

	/**
	 * Called on Service destroy.
	 */
	@Override
	public final void onDestroy() {
		super.onDestroy();
		Log.d(TAG, "onDestroy()");
		Log.d(TAG, "currentIOOps=" + currentIOOps);
		// FIXME: unsent messages should be cought here
	}

	/**
	 * Register/unregister a IO task.
	 * 
	 * @param unregister
	 *            unregister task?
	 */
	public static final synchronized void register(final boolean unregister) {
		Log.d(TAG, "register(" + unregister + ")");
		Log.d(TAG, "currentIOOps=" + currentIOOps);
		if (unregister) {
			--currentIOOps;
		} else {
			++currentIOOps;
		}
		me.displayNotification(currentIOOps);
		if (currentIOOps == 0 && !isBound && me != null) {
			me.stopSelf();
		}

		Log.d(TAG, "currentIOOps=" + currentIOOps);
	}

	/**
	 * Display a notification for pending send.
	 * 
	 * @param count
	 *            number of pending messages?
	 */
	private void displayNotification(final int count) {
		Log.d(TAG, "displayNotification(" + count + ")");
		NotificationManager mNotificationMgr = (NotificationManager) this
				.getSystemService(Context.NOTIFICATION_SERVICE);
		if (count == 0) {
			mNotificationMgr.cancel(NOTIFICATION_PENDING);
			Log.d(TAG, "displayNotification(" + count + ") return");
			return;
		}

		final Notification notification = new Notification(
				R.drawable.stat_notify_sms_pending, "", System
						.currentTimeMillis());
		final PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				new Intent(this, WebSMS.class), 0);
		notification.setLatestEventInfo(this, this
				.getString(R.string.notify_sending), "", contentIntent);
		notification.defaults |= Notification.FLAG_NO_CLEAR;
		mNotificationMgr.notify(NOTIFICATION_PENDING, notification);
		Log.d(TAG, "displayNotification(" + count + ") return");
	}
}