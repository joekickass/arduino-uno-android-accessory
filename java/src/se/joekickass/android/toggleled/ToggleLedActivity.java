package se.joekickass.android.toggleled;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;

import com.android.future.usb.UsbAccessory;
import com.android.future.usb.UsbManager;

public class ToggleLedActivity extends Activity {

	private static final byte TOGGLE_LED_COMMAND = 15;

	private UsbManager mUsbManager = UsbManager.getInstance(this);

	private UsbAccessory mAccessory;

	private ParcelFileDescriptor mFileDescriptor;

	private FileOutputStream mOutputStream;

	private ImageView mStatusLed;

	private BroadcastReceiver mReceiver;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		setupStatusLed();
		setupToggleButton();
	}

	@Override
	protected void onResume() {
		super.onResume();
		setupDetachingAccessoryHandler();
		reOpenAccessoryIfNecessary();
	}
	
	@Override
	public void onPause() {
		super.onPause();
		closeAccessory();
		unregisterReceiver(mReceiver);
	}
	
	private void setupStatusLed() {
		mStatusLed = (ImageView) findViewById(R.id.status_led);
	}

	private void setupToggleButton() {
		Button button = (Button) findViewById(R.id.toggle_button);
		button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				sendCommand(TOGGLE_LED_COMMAND);
			}
		});
	}

	private void setupDetachingAccessoryHandler() {
		mReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				String action = intent.getAction();
				if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) {
					closeAccessory();
					finish();
				}
			}
		};
		IntentFilter filter = new IntentFilter(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
		registerReceiver(mReceiver, filter);
	}

	private void reOpenAccessoryIfNecessary() {
		setStatusWaiting();
		if (mOutputStream != null) {
			setStatusConnected();
			return;
		}
		Intent intent = getIntent();
		String action = intent.getAction();
		if (UsbManager.ACTION_USB_ACCESSORY_ATTACHED.equals(action)) {
				mAccessory = UsbManager.getAccessory(intent);
				openAccessory();
		}
	}

	private void openAccessory() {
		try {
			mFileDescriptor = mUsbManager.openAccessory(mAccessory);
			if (mFileDescriptor != null) {
				FileDescriptor fd = mFileDescriptor.getFileDescriptor();
				mOutputStream = new FileOutputStream(fd);
				setStatusConnected();
			}
		} catch (IllegalArgumentException ex) {
			// Accessory detached while activity was inactive
			closeAccessory();
		}
	}

	private void closeAccessory() {
		try {
			if (mOutputStream != null) {
				mOutputStream.close();
			}
			if (mFileDescriptor != null) {
				mFileDescriptor.close();
			}
		} catch (IOException e) {
		} finally {
			mOutputStream = null;
			mFileDescriptor = null;
			mAccessory = null;
		}
		setStatusDisconnected();
	}

	private void sendCommand(byte command) {
		if (mOutputStream != null) {
			try {
				mOutputStream.write(command);
			} catch (IOException e) {
				// Do nothing
			}
		} else {
			closeAccessory();
		}
	}

	private void setStatusConnected() {
		mStatusLed.setImageResource(R.drawable.green_led);
	}

	private void setStatusWaiting() {
		mStatusLed.setImageResource(R.drawable.yellow_led);
	}

	private void setStatusDisconnected() {
		mStatusLed.setImageResource(R.drawable.red_led);
	}	
}