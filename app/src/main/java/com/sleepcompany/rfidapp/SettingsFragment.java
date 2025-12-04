package com.sleepcompany.rfidapp;


import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.seuic.uhf.UHFService;
import com.sleepcompany.rfidapp.databinding.FragmentSettingsBinding;

public class SettingsFragment extends Fragment {

	private UHFService mDevice;
	private SettingViewModel mSettingViewModel;
	private FragmentSettingsBinding mSettingsBinding;
	private HandlerClick mHandlerClick;

	private Spinner spinner;

	View currentView;

	private static SettingsFragment settingsfragment;

	public static SettingsFragment getInstance() {
		if (settingsfragment == null)
			settingsfragment = new SettingsFragment();
		return settingsfragment;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mSettingViewModel = new ViewModelProvider(requireActivity()).get(SettingViewModel.class);
	}

	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		mDevice = UHFService.getInstance();
		mSettingsBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_settings, container, false);
		mSettingsBinding.setVm(mSettingViewModel);
		currentView = mSettingsBinding.getRoot();

		spinner = (Spinner) currentView.findViewById(R.id.tagtype);

//		SharedPreferences userData = requireActivity().getSharedPreferences("userData", Context.MODE_PRIVATE);
//		if(userData != null) {
//			String[] tagTypeArray = getResources().getStringArray(R.array.tagtypeoptions);
//			String tagType = userData.getString("tag_type", "TEMP");
//			int selectedIndex = 0;
//			for (int i = 0; i < tagTypeArray.length; i++) {
//				if (tagTypeArray[i].equals(tagType)) {
//					selectedIndex = i;
//					break;
//				}
//			}
//			spinner.setSelection(selectedIndex);
//		}

		mHandlerClick = new HandlerClick();
		mSettingsBinding.setListener(mHandlerClick);

		// Get the firmware version number
		GetFirmwareVersion();
		// Get temperature
		mHandlerClick.GetTemperature();
		// Get power
		mHandlerClick.GetPower();
		// Set region
		SetRegion();
		// Get region
		mHandlerClick.GetRegion();

		return currentView;
	}


	public class HandlerClick{

		public void setTagType(){
			String selectedValue = spinner.getSelectedItem().toString();
			SharedPreferences prefs = requireActivity().getSharedPreferences("userData", MODE_PRIVATE);
			SharedPreferences.Editor editor = prefs.edit();
			editor.putString("tag_type", selectedValue);
			editor.apply(); // or editor.commit()

		}
		// Get temperature
		public void GetTemperature() {
			String temperature = mDevice.getTemperature();
			if (temperature == null || temperature == "") {
				System.out.println(getString(R.string.RfidGetTemperature_faild));
				return;
			}
			mSettingViewModel.temp.set(temperature);
			System.out.println(getString(R.string.temperature) + temperature);
		}

		// Get power
		public void GetPower() {
			int power = mDevice.getPower();
			if (power == 0) {
				System.out.println(getString(R.string.RfidGetPower_faild));
			}
			mSettingViewModel.power.set(power + "");
			System.out.println(getString(R.string.power) + power);
		}

		// Set power
		public void SetPower() {
			String value = mSettingViewModel.power.get().toString().trim();
			if (!value.isEmpty()) {
				int power = Integer.valueOf(value);
				System.out.println(getString(R.string.RfidSetPower) + power);
				boolean ret = mDevice.setPower(power);
				if (!ret) {
					System.out.println(getString(R.string.RfidGetPower_faild));
				}
			} else {
				Toast.makeText(getActivity(), R.string.please_input_power, Toast.LENGTH_SHORT).show();
			}

		}

		// Get region
		public void GetRegion() {
			String region = mDevice.getRegion();
			if (region == null) {
				System.out.println(getString(R.string.RfidGetRegion_faild));
				return;
			}

			mSettingViewModel.region.set(region);
			System.out.println(getString(R.string.region) + region);
		}

	}


	//  Get the firmware version number
	public void GetFirmwareVersion() {
		String version = mDevice.getFirmwareVersion();// .trim();
		if (version == null || version == "") {
			return;
		}
		mSettingViewModel.version.set(version.trim());
		System.out.println(version);
	}



	// Set region
	public void SetRegion() {
		String region = getString(R.string.fcc);
		boolean ret = mDevice.setRegion(region);
		if (!ret) {
			System.out.println(getString(R.string.RfidGetRegion_faild));
		}
	}

}
