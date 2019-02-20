package de.appwerft.audionotification;

import java.util.HashMap;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollFunction;
import org.appcelerator.kroll.KrollModule;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.TiC;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;


@Kroll.proxy(creatableInModule = TiaudionotificationModule.class)
public class NotificationProxy extends KrollProxy{
	// A reference to the service used to get location updates.
		private NotificationForegroundService notificationForegroundService = null;
		// Standard Debugging variables
		public static final String LCAT = "🎈TiAudioNot";
		private Context ctx;
		// Tracks the bound state of the service.
		private boolean boundState = false;
		private Messenger messenger;
		// You can define constants with @Kroll.constant, for example:
		// @Kroll.constant public static final String EXTERNAL_NAME = value;

		private MyServiceReceiver receiver;
		private KrollDict notificationOpts;

		private class MyServiceReceiver extends BroadcastReceiver {
			@Override
			public void onReceive(Context context, Intent intent) {
				intent.getParcelableExtra(NotificationForegroundService.EXTRA_ACTION);
				boolean isforeground = intent.getBooleanExtra("INFOREGROUND", false);

				KrollDict result = new KrollDict();
				// if (!isforeground) {

				// }
			}
		}

		// Monitors the state of the connection to the service.
		private ServiceConnection serviceConnection = new ServiceConnection() {
			

			@Override
			public void onServiceConnected(ComponentName name, IBinder service) {
				Log.i(LCAT, "ServiceConnection: notificationForegroundService connected");
				notificationForegroundService = ((NotificationForegroundService.LocalBinder) service).getService();
				Log.i(LCAT, service.toString());

				messenger = new Messenger(service);
				Log.i(LCAT, messenger.toString());

				boundState = true;

			}

			@Override
			public void onServiceDisconnected(ComponentName name) {
				notificationForegroundService = null;
				boundState = false;
				messenger = null;
				KrollDict res = new KrollDict();
				res.put("connected", false);
				if (hasListeners("ServiceConnectionChanged"))
					fireEvent("ServiceConnectionChanged", res);
			}
		};

		public NotificationProxy() {
			super();
			ctx = TiApplication.getInstance().getApplicationContext();

		}

		@Kroll.onAppCreate
		public static void onAppCreate(TiApplication app) {

		}

		// Methods
		@Kroll.method
		public void create(KrollDict opts) {
			notificationOpts = opts;
			Intent intent = new Intent(ctx, NotificationForegroundService.class);
			Log.i(LCAT, "bindService in onStart of module was successful: "
					+ ctx.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE));

		}

		// https://stackoverflow.com/questions/43736714/how-to-pass-data-from-activity-to-running-service
		@Kroll.method
		public void show(KrollDict opts) {
			if (opts.containsKeyAndNotNull(TiC.PROPERTY_TITLE))
				notificationOpts.put(TiC.PROPERTY_TITLE, opts.getString(TiC.PROPERTY_TITLE));
			if (opts.containsKeyAndNotNull(TiC.PROPERTY_SUBTITLE))
				notificationOpts.put(TiC.PROPERTY_SUBTITLE, opts.getString(TiC.PROPERTY_SUBTITLE));
			if (!boundState)
				return;
			Message msg = Message.obtain(null, Constants.MSG.UPDATE, 0, 0);
			try {
				messenger.send(msg);
			} catch (RemoteException e) {
				e.printStackTrace();
			}

		}

		@Kroll.method
		public void update(KrollDict opts) {
			if (notificationForegroundService != null && boundState) {
				notificationForegroundService.updateNotification(opts);
			}
		}

		@Kroll.method
		public void hide() {

		}

		private static String getApplicationName(Context context) {
			ApplicationInfo applicationInfo = context.getApplicationInfo();
			int stringId = applicationInfo.labelRes;
			return stringId == 0 ? applicationInfo.nonLocalizedLabel.toString() : context.getString(stringId);
		}

		@Override
		public void onStart(Activity activity) {
			super.onStart(activity);
			Log.d(LCAT, ">>>>> onStart called");
			// Bind to the service. If the service is in foreground mode, this
			// signals to the service
			// that since this activity is in the foreground, the service can exit
			// foreground mode.
			ctx.bindService(new Intent(ctx, NotificationForegroundService.class), serviceConnection,
					Context.BIND_AUTO_CREATE);
		}

		@Override
		public void onResume(Activity activity) {
			super.onResume(activity);
			Log.d(LCAT, ">>>>> onResume called");
			// LocalBroadcastManager.getInstance(ctx).registerReceiver(receiver,
			// new IntentFilter(NotificationForegroundService.ACTION_BROADCAST));
		}

		@Override
		public void onPause(Activity activity) {
			Log.d(LCAT, "<<<<<< onPause called");
			LocalBroadcastManager.getInstance(ctx).unregisterReceiver(receiver);
			super.onPause(activity);
		}

		@Override
		public void onStop(Activity activity) {
			Log.d(LCAT, "<<<<<< onStop called");
			if (boundState) {
				// Unbind from the service. This signals to the service that this
				// activity is no longer
				// in the foreground, and the service can respond by promoting
				// itself to a foreground
				// service.
				ctx.unbindService(serviceConnection);
				boundState = false;
			}
			super.onStop(activity);
		}

		@Override
		public void onDestroy(Activity activity) {
			Log.d(LCAT, "<<<<<< onDestroy called");
			super.onDestroy(activity);
		}

	
	

}