package be.uclouvain.multipathcontrol.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Switch;
import android.widget.Toast;
import be.uclouvain.multipathcontrol.MPCtrl;
import be.uclouvain.multipathcontrol.R;
import be.uclouvain.multipathcontrol.global.Config;
import be.uclouvain.multipathcontrol.global.Manager;
import be.uclouvain.multipathcontrol.services.MainService;
import be.uclouvain.multipathcontrol.stats.JSONSender;
import be.uclouvain.multipathcontrol.stats.SaveDataApp;
import be.uclouvain.multipathcontrol.system.Sysctl;

public class MainActivity extends Activity {

	private MPCtrl mpctrl;
	private Switch multiIfaceSwitch;
	private Switch defaultDataSwitch;
	private Switch dataBackupSwitch;
	private Switch saveBatterySwitch;
	private Switch ipv6Switch;
	private Button tcpCCButton;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		multiIfaceSwitch = (Switch) findViewById(R.id.switch_multiiface);
		defaultDataSwitch = (Switch) findViewById(R.id.switch_default_data);
		dataBackupSwitch = (Switch) findViewById(R.id.switch_data_backup);
		saveBatterySwitch = (Switch) findViewById(R.id.switch_save_battery);
		ipv6Switch = (Switch) findViewById(R.id.switch_ipv6);
		tcpCCButton = (Button) findViewById(R.id.button_tcp_cc);

		mpctrl = Manager.create(this);
		if (mpctrl == null) {
			Toast.makeText(this, "It seems this is not a rooted device",
					Toast.LENGTH_LONG).show();
			moveTaskToBack(true);
			return;
		}

		// do that now, to avoid useless call to onCheckedChangeListerner
		setChecked();
		multiIfaceSwitch
				.setOnCheckedChangeListener(onCheckedChangeListernerMultiIface);
		defaultDataSwitch
				.setOnCheckedChangeListener(onCheckedChangeListernerDefaultData);
		dataBackupSwitch
				.setOnCheckedChangeListener(onCheckedChangeListernerDataBackup);
		saveBatterySwitch
				.setOnCheckedChangeListener(onCheckedChangeListernerSaveBattery);
		ipv6Switch.setOnCheckedChangeListener(onCheckedChangeListernerIPv6);
		tcpCCButton.setOnClickListener(onClickListenerTcpCC);

		Button testButton = (Button) findViewById(R.id.button_send_data);
		testButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.d(Manager.TAG, "New data app: "
						+ new SaveDataApp(MainActivity.this).getTimestamp());
				JSONSender.sendAll(getApplicationContext());
			}
		});

		// start a new service if needed
		startService(new Intent(this, MainService.class));
	}

	@Override
	protected void onResume() {
		super.onResume();

		Config.getDynamicConfig();
		setChecked();
	}

	protected void onDestroy() {
		super.onDestroy();
		Manager.destroy(this);
	}

	private void setChecked() {
		multiIfaceSwitch.setChecked(Config.mEnabled);
		defaultDataSwitch.setChecked(Config.defaultRouteData);
		dataBackupSwitch.setChecked(Config.dataBackup);
		saveBatterySwitch.setChecked(Config.saveBattery);
		ipv6Switch.setChecked(Config.ipv6);
		tcpCCButton.setText(getText(R.string.button_tcp_cc) + ": "
				+ Config.tcpcc);
	}

	private OnCheckedChangeListener onCheckedChangeListernerMultiIface = new OnCheckedChangeListener() {
		@Override
		public void onCheckedChanged(CompoundButton buttonView,
				boolean isChecked) {
			if (mpctrl.setStatus(isChecked) && !isChecked)
				Toast.makeText(
						MainActivity.this,
						"The second interface will be disabled in a few seconds",
						Toast.LENGTH_LONG).show();
		}
	};

	private OnCheckedChangeListener onCheckedChangeListernerDefaultData = new OnCheckedChangeListener() {
		@Override
		public void onCheckedChanged(CompoundButton buttonView,
				boolean isChecked) {
			mpctrl.setDefaultData(isChecked);
		}
	};

	private OnCheckedChangeListener onCheckedChangeListernerDataBackup = new OnCheckedChangeListener() {
		@Override
		public void onCheckedChanged(CompoundButton buttonView,
				boolean isChecked) {
			mpctrl.setDataBackup(isChecked);
		}
	};

	private OnCheckedChangeListener onCheckedChangeListernerSaveBattery = new OnCheckedChangeListener() {
		@Override
		public void onCheckedChanged(CompoundButton buttonView,
				boolean isChecked) {
			if (mpctrl.setSaveBattery(isChecked))
				Toast.makeText(
						MainActivity.this,
						"Please disconnect/reconnect cellular interface or reboot",
						Toast.LENGTH_LONG).show();
		}
	};

	private OnCheckedChangeListener onCheckedChangeListernerIPv6 = new OnCheckedChangeListener() {
		@Override
		public void onCheckedChanged(CompoundButton buttonView,
				boolean isChecked) {
			if (Sysctl.setIPv6(isChecked)) {
				Config.ipv6 = isChecked;
				Config.saveStatus(MainActivity.this);
			} else
				Toast.makeText(MainActivity.this,
						"Not able to change IPv6 settings",
						Toast.LENGTH_LONG).show();
		}
	};

	private OnClickListener onClickListenerTcpCC = new OnClickListener() {
		@Override
		public void onClick(View v) {
			Intent intent = new Intent(MainActivity.this, TCPCCActivity.class);
			startActivity(intent);
		}
	};
}
