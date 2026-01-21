package com.treewalker.rfidapp;


import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.SoundPool;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

//import com.treewalker.rfidapp.db.BarcodeDao;
//import com.treewalker.rfidapp.model.Barcode;
import com.treewalker.rfidapp.util.BaseUtil;
import com.seuic.scankey.IKeyEventCallback;
import com.seuic.scankey.ScanKeyService;
import com.seuic.scanner.DecodeInfo;
import com.seuic.scanner.DecodeInfoCallBack;
import com.seuic.scanner.Scanner;
import com.seuic.scanner.ScannerFactory;
import com.seuic.uhf.EPC;
import com.seuic.uhf.UHFService;
import com.treewalker.rfidapp.databinding.FragmentInventoryBinding;
import com.treewalker.rfidapp.databinding.ItemEpcBinding;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


public class InventoryFragment extends Fragment implements DecodeInfoCallBack {

	public static final int MAX_LEN = 64;


//	private List<Barcode> barcodeList;

	public static final int ItemSelectColor = 0x44000000;
	private static final String TAG = InventoryFragment.class.getSimpleName();

	private FragmentInventoryBinding mFragmentInventoryBinding;

	private UHFService mDevice;

	private InventoryRunable mInventoryRunable;
	public boolean mInventoryStart = false;
	private Thread mInventoryThread;

	private Button btn_once;
	private Button btn_continue;
	private Button btn_stop;

	private Button bt_save_data;

	private ListView lv_id;

	private List<EPC> mEPCList;
	private InventoryAdapter mAdapter;
	private InventoryViewModel mInventoryViewModel;
	private MutableLiveData<List<EPC>> mEPCListLiveData;
	private MutableLiveData<Integer> mSelectedIndex;
	HandlerClick handlerClick;

	View currentView;

	static int m_count = 0;

	private static InventoryFragment inventoryfragment;

	Scanner scanner;

	public static InventoryFragment getInstance() {
		if (inventoryfragment == null)
			inventoryfragment = new InventoryFragment();
		return inventoryfragment;
	}


	// sound
	private static SoundPool mSoundPool;
	private static int soundID;
	/*
	 * static { mSoundPool = new SoundPool(3, AudioManager.STREAM_MUSIC, 20);
	 * soundID = mSoundPool.load(getContext(),R.raw.scan, 1); }
	 */
	private ToneGenerator toneGen = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
	private long lastBeepTime = 0;

	//SharedPreferences userData ;

	private String tagType = "TEMP";


	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mInventoryViewModel = new ViewModelProvider(requireActivity()).get(InventoryViewModel.class);
		mEPCListLiveData = mInventoryViewModel.getEPCListLiveData();
		mSelectedIndex = mInventoryViewModel.getSelectedIndex();

	}
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		mDevice = UHFService.getInstance();
		scanner = ScannerFactory.getScanner(MyApp.getInstance());
		mFragmentInventoryBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_inventory, container, false);
		mFragmentInventoryBinding.setVm(mInventoryViewModel);
		handlerClick = new HandlerClick();
		mFragmentInventoryBinding.setHandler(handlerClick);

		currentView = mFragmentInventoryBinding.getRoot();
		initUI(currentView);

		mEPCList = new ArrayList<EPC>();
		if(mEPCListLiveData.getValue()!=null){
			mEPCList = mEPCListLiveData.getValue();
		}
		mAdapter = new InventoryAdapter(mEPCList);
		mInventoryRunable = new InventoryRunable();
		lv_id.setAdapter(mAdapter);

		lv_id.setOnItemClickListener(new MyItemClickListener());

		mEPCListLiveData.observe(getViewLifecycleOwner(), new Observer<List<EPC>>() {
			@Override
			public void onChanged(List<EPC> epcs) {
				mAdapter.setEPCList(epcs);
				mAdapter.notifyDataSetChanged();
			}
		});

		/* Load all the mapped data*/

//		barcodeList = BarcodeDao.getBarcodes(requireContext(), 1);
//		Log.d("Barcode List", barcodeList.toString());
		SharedPreferences userData = requireActivity().getSharedPreferences("userData", Context.MODE_PRIVATE);
		if(userData != null) {
			tagType = userData.getString("tag_type", "TEMP");
		}
		Log.d("TAGTYPE", tagType);
//		Log.d("size", barcodeList.size() + "");
//		if(barcodeList.size() > 0){
//			for (final Barcode barcode : barcodeList) {
//
//			}
//		}

		scanner.open();
		scanner.setDecodeInfoCallBack(this);
		return currentView;
	}


	@Override
	public void onResume() {

		super.onResume();
		mScanKeyService.registerCallback(mCallback, "248,249,250");

		// handler.sendEmptyMessage(2);

	}

	@Override
	public void onPause() {
		super.onPause();
		mScanKeyService.unregisterCallback(mCallback);
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		handler.removeCallbacksAndMessages(null); // avoid lingering messages
	}


	// init UI
	private View initUI(View currentView) {

		lv_id = (ListView) currentView.findViewById(R.id.lv_id);

		btn_once = (Button) currentView.findViewById(R.id.bt_once);

		btn_continue = (Button) currentView.findViewById(R.id.bt_continue);

		btn_stop = (Button) currentView.findViewById(R.id.bt_stop);

		bt_save_data = (Button) currentView.findViewById(R.id.bt_save_data);


		mSoundPool = new SoundPool(3, AudioManager.STREAM_MUSIC, 20);
		soundID = mSoundPool.load(currentView.getContext(), R.raw.scan, 1);

		return currentView;
	}

	// 条码list刷新
	private void refreshData(List<EPC> updatedList) {
		mEPCList = updatedList;

		//Log.d("INVENTORY","Data "+mEPCList.toString());
		if (mEPCList != null) {
			// Gets the number inside the list of all labels
			int count = 0;
			int itr = 0;
			for (EPC item : mEPCList) {

				// verify if the item code exist
				//Log.d("Barcode List", "Refresh the tag data");
				// ⬇️ NEW: Play sound based on RSSI if available
				int rssi = item.rssi; // Make sure EPC class has getRssi()
				//Log.d("RSSI Sound", "RSSI for " + item.getId() + " = " + rssi);
				//Log.d("TAGTYPE", "Tag Type " + tagType);
//
//				if (barcodeList != null && !barcodeList.isEmpty()) {
//					//Log.d("Barcode List", "Start the iteration");
//					for (Barcode barcode : barcodeList) {
//						//Log.d("Barcode List", "Record Match:"+item.getId()+" -- "+barcode.getCode());
//						//Log.d("Barcode List", "EPC :"+item.getId()+" -- "+barcode.getCode());
//						String str_password = mInventoryViewModel.password.get().toString().trim();
//						//byte[] btPassword = new byte[16];
//						byte[] btPassword  = BaseUtil.hexStringToByteArray(str_password);
//
//						//Log.d("Barcode List", "Password :"+str_password+" -- "+BaseUtil.getHexString(btPassword,btPassword.length));
//						if (item.getId().equals(barcode.getCode()) && tagType.equals("LED")) { // assuming getCode() returns the item code
//							//Toast.makeText(requireContext(), "Item Found: " + scannedCode, Toast.LENGTH_SHORT).show();
//							Log.d("Barcode List", "LED TAG FOUND");
//							mAdapter.setHighlightedPositions(item.getId());
////							byte[] embd = new byte[255];
////							int rfidLength = Integer.parseInt(String.valueOf(item.getId().length()/2));
//////							embd[0] = (byte)Integer.parseInt(0+"");//0.toByte();
//////							embd[1] = (byte)Integer.parseInt(8+"");//8.toByte()  length
//////							embd[2] = (byte)Integer.parseInt(2+"");//2.toByte()
////							embd[0] = (byte)Integer.parseInt(1+"");//0.toByte();
////							embd[1] = (byte)Integer.parseInt(0+"");//8.toByte()  length
////							embd[2] = (byte)Integer.parseInt(String.valueOf(item.getId().length()/2));//2.toByte()
////							embd[3] = (byte)Integer.parseInt(0+"");
////							byte[] data = BaseUtil.getHexByteArray2(item.getId());
////							if(embd[2] != data.length) {
////								Log.d("INVENTORY","length does not match "+data.length+" -- "+embd[2]);
////							}
////
////							//System.arraycopy(BaseUtil.getHexByteArray2("00000000"), 0, embd, 3, 4);
////							System.arraycopy(data, 0, embd, 4, rfidLength);
////							//mDevice.setParamBytes(UHFService.PARAMETER_TAG_EMBEDEDDATA, embd);
////							boolean isLedread = mDevice.setParamBytes(UHFService.PARAMETER_TAG_FILTER, embd);
//
//							//boolean isLedread = mDevice.readTagLED(BaseUtil.getHexByteArray(item.getId()), btPassword, 0);
//							//Log.d("Barcode List", "LED RED :"+isLedread);
//							playBeepBasedOnRSSI(rssi);
//							//break; // optional: stop after first match
//						}else if(item.getId().equals(barcode.getCode()) && tagType.equals("TEMP")){
//							mAdapter.setHighlightedPositions(item.getId());
//							//mAdapter.notifyDataSetChanged();
//						    double tempDbl = mDevice.readTagTemperature(BaseUtil.getHexByteArray(item.getId()), btPassword, 0);
//							playBeepBasedOnRSSI(rssi);
//							//Log.d("Barcode List", "Temperature TAG :"+tempDbl);
//						}else{
//							//mDevice.setParamBytes(UHFService.PARAMETER_TAG_EMBEDEDDATA, null);
//							//Log.d("Barcode List", "ELSE");
//
//						}
//					}
//				}
				count += item.count;
				itr++;
			}
			if (count > m_count) {

				//playSound();
			}
			mInventoryViewModel.EPC_total.set(mEPCList.size());
			m_count = count;
		}
	}

	private void playBeepBasedOnRSSI(int rssi) {
		long now = System.currentTimeMillis();
		if (now - lastBeepTime < 300) return; // prevent rapid beeps
		lastBeepTime = now;

		int durationMs;
		if (rssi >= -10) {
			durationMs = 30;
		}
		else if (rssi >= -30) {
			durationMs = 100;
		}
		else if (rssi >= -40) {
			durationMs = 130;
		} else if (rssi >= -60) {
			durationMs = 200;
		} else if (rssi >= -70) {
			durationMs = 300;
		} else {
			durationMs = 400;
		}

		toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, durationMs);
	}

	/* save and download the csv */
	public void saveCSVToFileAndDownload(List<EPC> mEPCList, Context context) {
		String fileName = "rfid_data.csv";
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
			// Android 10+ (API 29+): Use MediaStore
			ContentValues values = new ContentValues();
			values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
			values.put(MediaStore.Downloads.MIME_TYPE, "text/csv");
			values.put(MediaStore.Downloads.IS_PENDING, 1);
			values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

			ContentResolver resolver = context.getContentResolver();
			Uri collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
			Uri fileUri = resolver.insert(collection, values);

			if (fileUri != null) {
				try (OutputStream out = resolver.openOutputStream(fileUri);
					 BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out))) {

					for (EPC epcData : mEPCList) {
						String line = epcData.getId()+","+epcData.count+","+epcData.rssi;
						writer.write(line);
						writer.newLine();
					}

					writer.flush();
					values.clear();
					values.put(MediaStore.Downloads.IS_PENDING, 0);
					resolver.update(fileUri, values, null, null);

					Toast.makeText(context, "CSV saved to Downloads", Toast.LENGTH_LONG).show();

				} catch (IOException e) {
					e.printStackTrace();
					Toast.makeText(context, "Failed to save CSV", Toast.LENGTH_SHORT).show();
				}
			}

		} else {
			// For Android 9 and below
			File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
			File file = new File(downloadsDir, fileName);

			try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
				for (EPC epcData : mEPCList) {
					String line = epcData.getId()+","+epcData.count+","+epcData.rssi;

					writer.write(line);
					writer.newLine();
				}

				writer.flush();
				Toast.makeText(context, "CSV saved to Downloads", Toast.LENGTH_LONG).show();

			} catch (IOException e) {
				e.printStackTrace();
				Toast.makeText(context, "Failed to save CSV", Toast.LENGTH_SHORT).show();
			}
		}
	}

	@Override
	public void onDecodeComplete(DecodeInfo decodeInfo) {
		Log.i("seuic", "barcode：" + decodeInfo.barcode);
	}

	/* scan handler methods */
	private ScanKeyService mScanKeyService = ScanKeyService.getInstance();
	private IKeyEventCallback mCallback = new IKeyEventCallback.Stub() {
		@Override
		public void onKeyDown(int keyCode) throws RemoteException {
			Log.d(TAG, "onKeyDown: keyCode=" + keyCode);
			//Press to perform a continuous search
			Message msg = new Message();
			msg.what = 2;
			handler.sendMessage(msg);
			scanner.startScan();
		}

		@Override
		public void onKeyUp(int keyCode) throws RemoteException {
			Log.d(TAG, "onKeyUp: keyCode=" + keyCode);
			//Lift stop continuous card search
			Message msg = new Message();
			msg.what = 3;
			handler.sendMessage(msg);
			scanner.stopScan();
		}
	};


	private static class MyHandler extends Handler {
		private final WeakReference<InventoryFragment> fragmentRef;

		MyHandler(InventoryFragment fragment) {
			super(Looper.getMainLooper());
			this.fragmentRef = new WeakReference<>(fragment);
		}

		@Override
		public void handleMessage(@NonNull Message msg) {
			InventoryFragment fragment = fragmentRef.get();
			if (fragment == null || !fragment.isAdded()) return;

			switch (msg.what) {
				case 1:
					fragment.handlerClick.BtnOnce();
					break;
				case 2:
					fragment.handlerClick.BtnContinue();
					break;
				case 3:
					fragment.handlerClick.BtnStop();
					break;
			}
		}
	}
	private final MyHandler handler = new MyHandler(this);

//	Handler handler = new Handler() {
//		@Override
//		public void handleMessage(@NonNull Message msg) {
//			super.handleMessage(msg);
//			switch (msg.what) {
//				case 1:
//					handlerClick.BtnOnce();
//					break;
//				case 2:
//					handlerClick.BtnContinue();
//					break;
//				case 3:
//					handlerClick.BtnStop();
//					break;
//			}
//		}
//	};


	// EPC list item listener
	private class MyItemClickListener implements OnItemClickListener {
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

			mSelectedIndex.setValue(position);
			mAdapter.notifyDataSetInvalidated();
			/*
			 * ListView listview = (ListView) parent; HashMap<String, Object>
			 * data = (HashMap<String, Object>)
			 * listview.getItemAtPosition(position); String epc =
			 * data.get("epc").toString(); Toast.makeText(getActivity(), epc,
			 * 0).show();
			 */
		}
	}

	private void clearList() {
		mSelectedIndex.setValue(-1);
		if (mEPCList != null) {
			mEPCList.clear();
			mEPCListLiveData.setValue(mEPCList);
			m_count = 0;
		}
	}

	public class HandlerClick{




		public HandlerClick onTagClick(String  epcId){
			// pick the selected data and add it to the list of data
			Log.d("INVENTORY","MAP Clicked :"+epcId);
//			final Barcode barcode = new Barcode();
//			barcode.setSyncStatus("N");
//			barcode.setQty(1);
//			barcode.setCode(epcId);
//			BarcodeDao.insertOrUpdate(requireContext(), barcode);
			//Toast.makeText(requireContext(),"Record has been mapped",Toast.LENGTH_LONG).show();
			return null;
		}
		public void BtnOnce() {
			EPC epc = new EPC();
			if (mDevice.inventoryOnce(epc, 100)) {
				String id = epc.getId();
				System.out.println("" + id);
				if (id != null && !"".equals(id)) {
					playSound();
					boolean exist = false;
					for (EPC item : mEPCList) {
						if (item.equals(epc)) {
							item.count++;
							exist = true;
							break;
						}
					}
					if (!exist) {
						mEPCList.add(epc);
						mEPCListLiveData.setValue(mEPCList);
					}
					//refreshData();
					mAdapter.notifyDataSetChanged();
				}
				System.out.println("OK!!!");
			}
			return;
		}

		public void BtnContinue() {
			clearList();
			Log.d("INVENTORY","Start the reading");
			if (mInventoryThread != null && mInventoryThread.isAlive()) {
				System.out.println("Thread not null");
				return;
			}

//			if (barcodeList != null && !barcodeList.isEmpty()) {
//				//Log.d("Barcode List", "Start the iteration");
//				for (Barcode barcode : barcodeList) {
				byte[] embd = new byte[255];
				//int rfidLength = Integer.parseInt(String.valueOf(barcode.getCode().length()/2));
				embd[0] = (byte)Integer.parseInt(0+"");//0.toByte();
				embd[1] = (byte)Integer.parseInt(8+"");//8.toByte()  length
				embd[2] = (byte)Integer.parseInt(2+"");
				//(byte)Integer.parseInt(String.valueOf(barcode.getCode().length()/2));//2.toByte()
				//embd[3] = (byte)Integer.parseInt(0+"");
				byte[] data = BaseUtil.getHexByteArray2("00000000");
//					if(embd[2] != data.length) {
//						Log.d("INVENTORY","length does not match "+data.length+" -- "+embd[2]);
//					}
				System.arraycopy(data, 0, embd, 3, 4);
				mDevice.setParamBytes(UHFService.PARAMETER_TAG_EMBEDEDDATA, embd);



//				byte[] FILTER = new byte[255];
//				FILTER[0] = (byte) 1;//0.toByte();
//				FILTER[1] = (byte) 0;//8.toByte()  length
//				FILTER[2] = (byte) 12;//2.toByte()
//				FILTER[3] = (byte) 0;
//				//byte[] data = BaseUtil.getHexByteArray2("000000000000000000000000");
//				byte[] data2 = BaseUtil.getHexByteArray2("A4452548D987650000000000");
//				System.arraycopy(data2, 0, embd, 4, FILTER[2]);
//				boolean isLedread = mDevice.setParamBytes(UHFService.PARAMETER_TAG_FILTER, FILTER);


//				}
//			}

			if (mDevice.inventoryStart()) {
				System.out.println("RfidInventoryStart sucess.");

				mInventoryStart = true;
				mInventoryThread = new Thread(mInventoryRunable);
				mInventoryThread.start();

				btn_continue.setEnabled(false);
				btn_once.setEnabled(false);
				btn_stop.setEnabled(true);
				bt_save_data.setEnabled(true);

			} else {
				//Toast.makeText(this,"RfidInventoryStart faild", Toast.LENGTH_SHORT).show();
				Toast.makeText(requireContext(),"RfidInventoryStart faild.",Toast.LENGTH_LONG).show();
			}
			return;
		}

		public void BtnStop() {
			mInventoryStart = false;

			if (mInventoryThread != null) {
				mInventoryThread.interrupt();
				mInventoryThread = null;
			}
			System.out.println("begin stop!!");
			if (mDevice.inventoryStop()) {
				System.out.println("end stop!!");
				btn_once.setEnabled(true);
				btn_continue.setEnabled(true);
				btn_stop.setEnabled(false);

			} else {
				System.out.println("RfidInventoryStop faild.");
			}





		}
		public void BtnSave() {
			// sync the data with the

			saveCSVToFileAndDownload(mEPCList,getContext());
			BtnStop();

		}


		public void BtnRead() {
			int index = -1;
			if ((index=mSelectedIndex.getValue()) >= 0) {

				if (mInventoryViewModel.bank.get().isEmpty() ||mInventoryViewModel.address.get().isEmpty() || mInventoryViewModel.length.get().isEmpty()) {
					Toast.makeText(getActivity(), R.string.the_parameter_cannot_be_empty, Toast.LENGTH_SHORT).show();
					return;
				}
				int bank = Integer.parseInt(mInventoryViewModel.bank.get().toString());
				int address = Integer.parseInt(mInventoryViewModel.address.get().toString());
				int length = Integer.parseInt(mInventoryViewModel.length.get().toString());

				String str_password = mInventoryViewModel.password.get().toString().trim();

				String Epc = mEPCList.get(index).getId();

				byte[] btPassword = new byte[16];
				BaseUtil.getHexByteArray(str_password, btPassword, btPassword.length);
				byte[] buffer = new byte[MAX_LEN];
				if (length > MAX_LEN) {
					buffer = new byte[length];
				}

				if (!mDevice.readTagData(BaseUtil.getHexByteArray(Epc), btPassword, bank, address, length, buffer)) {

					Toast.makeText(getActivity(), R.string.readTagData_faild, Toast.LENGTH_SHORT).show();
				} else {
					Toast.makeText(getActivity(), R.string.readTagData_sucess, Toast.LENGTH_SHORT).show();
					String data = BaseUtil.getHexString(buffer, length, " ");
					mInventoryViewModel.data.set(data);
				}

			} else {
				Toast.makeText(getActivity(), R.string.please_select_a_tag, Toast.LENGTH_SHORT).show();
			}

		}

		public void BtnWrite() {
			int index = -1;
			if ((index=mSelectedIndex.getValue()) >= 0) {
				if (mInventoryViewModel.bank.get().isEmpty() ||mInventoryViewModel.address.get().isEmpty() || mInventoryViewModel.length.get().isEmpty()) {
					Toast.makeText(getActivity(), R.string.the_parameter_cannot_be_empty, Toast.LENGTH_SHORT).show();
					return;
				}
				int bank = Integer.parseInt(mInventoryViewModel.bank.get().toString());
				int address = Integer.parseInt(mInventoryViewModel.address.get().toString());
				int length = Integer.parseInt(mInventoryViewModel.length.get().toString());

				String str_password = mInventoryViewModel.password.get().toString().trim();

				String Epc = mEPCList.get(index).getId();

				byte[] btPassword = new byte[16];
				BaseUtil.getHexByteArray(str_password, btPassword, btPassword.length);

				String str_data = mInventoryViewModel.data.get().toString().replace(" ", "");
				if (str_data.isEmpty()) {
					Toast.makeText(getActivity(), R.string.writeData_cannot_be_empty, Toast.LENGTH_SHORT).show();
					return;
				}
				byte[] buffer = new byte[MAX_LEN];
				if (length > MAX_LEN) {
					buffer = new byte[length];
				}
				BaseUtil.getHexByteArray(str_data, buffer, length);

				if (!mDevice.writeTagData(BaseUtil.getHexByteArray(Epc), btPassword, bank, address, length, buffer)) {

					Toast.makeText(getActivity(), R.string.writeTagData_faild, Toast.LENGTH_SHORT).show();
				} else {
					Toast.makeText(getActivity(), R.string.writeTagData_sucess, Toast.LENGTH_SHORT).show();

				}
			} else {
				Toast.makeText(getActivity(), R.string.please_select_a_tag, Toast.LENGTH_SHORT).show();
			}
		}

		public void BtnClear() {
			if (mEPCList != null) {
				mEPCList.clear();
				mEPCListLiveData.setValue(mEPCList);
				m_count = 0;
				mAdapter.setEPCList(mEPCList);
				mAdapter.notifyDataSetChanged();
			}

			//mInventoryViewModel.data.set(null);
		}

	}

	private void playSound() {
		if (mSoundPool == null) {
			mSoundPool = new SoundPool(3, AudioManager.STREAM_MUSIC, 20);
			soundID = mSoundPool.load(currentView.getContext(), R.raw.scan, 1);// "/system/media/audio/notifications/Antimony.ogg"
		}
		mSoundPool.play(soundID, 1, 1, 0, 0, 1);
	}

	public class InventoryAdapter extends BaseAdapter {
		private List<EPC> mEPCList;

		private int selectedPosition = -1; // initially no selection
		private Set<String> highlightedPositions = new HashSet<>();

		public InventoryAdapter(List<EPC> list) {
			mEPCList = list;
		}

		public void setEPCList(List<EPC> epcs){
			mEPCList = epcs;
		}

		@Override
		public int getCount() {

			// return 0;
			if (mEPCList != null) {
				return mEPCList.size();
			}
			return 0;
		}

		@Override
		public Object getItem(int position) {

			// return null;
			return mEPCList.get(position);
		}

		@Override
		public long getItemId(int position) {

			// return 0;
			return position;
		}

		@Override
		public void notifyDataSetChanged() {
			super.notifyDataSetChanged();
			//mSelectedIndex.setValue(-1);
		}

		public void setSelectedPosition(int position) {
			this.selectedPosition = position;
			//notifyDataSetChanged(); // refresh list to update highlight
		}

		public void setHighlightedPositions(String epcId) {
			//this.selectedPosition = position;
			highlightedPositions.add(epcId);

			//notifyDataSetChanged(); // refresh list to update highlight
		}

		public boolean isMapped(String epcId) {
			return highlightedPositions.contains(epcId);
		}


		//highlightedPositions

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ItemEpcBinding epcBinding;
			if(convertView == null){
				epcBinding = DataBindingUtil.inflate(LayoutInflater.from(parent.getContext()), R.layout.item_epc, parent ,false);
				convertView = epcBinding.getRoot();
			}else{
				epcBinding = DataBindingUtil.getBinding(convertView);
			}

			EPC epc = mEPCList.get(position);
			epcBinding.setEpc(epc);
			//epcBinding.setHandler(handlerClick.onTagClick(epc.getId()));
			epcBinding.setHandler(handlerClick); // binds the handler (do not call method!)
			//if (position == mSelectedIndex.getValue()) {
			//if (position == selectedPosition) {
			if (highlightedPositions.contains(epc.getId())) {
				//mEPCList.get(position).
				convertView.setBackgroundColor(Color.parseColor("#FFCC80"));
			}else{
				convertView.setBackgroundColor(Color.WHITE);
			}
			return convertView;
		}

	}


	private class InventoryRunable implements Runnable {

		// Map to hold tag info with last seen timestamp and RSSI
		private final Map<String, EPC> tagMap = new ConcurrentHashMap<>();
		//private final long TAG_EXPIRY_MS = 3000; // 3 seconds
		private final int RSSI_THRESHOLD = -24;  // dBm

		@Override
		public void run() {
			Log.d("INVENTORY", "Read with continuous mode");
			while (mInventoryStart) {
				Log.d("INVENTORY", "Loop started");

				List<EPC> currentScanList = mDevice.getTagIDs();
				//long now = System.currentTimeMillis();

				// Update map with current tags
				for (EPC epc : currentScanList) {
					//epc.setLastSeen(now);
					tagMap.put(epc.getId(), epc);
				}

				// Remove tags that are weak signal or not seen recently
				Iterator<Map.Entry<String, EPC>> iterator = tagMap.entrySet().iterator();
				while (iterator.hasNext()) {
					Map.Entry<String, EPC> entry = iterator.next();
					EPC tag = entry.getValue();
					Log.d("Loop",tag.rssi+" --- "+RSSI_THRESHOLD);
					if (tag.rssi < RSSI_THRESHOLD) {
						iterator.remove(); // remove if outdated or out of range
					}
				}

				// Convert to list and push to LiveData
				List<EPC> displayList = new ArrayList<>(tagMap.values());

				//mEPCListLiveData.postValue(displayList);
				mEPCList = displayList;
				mEPCListLiveData.postValue(displayList); // if you're using LiveData in other parts
				Log.d("Loop",displayList.toString());
				refreshData(displayList);



				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

//		  @Override
//		  public void run() {
//
//			Log.d("INVENTORY","Read with continuous mode");
//			while (mInventoryStart) {
//				Log.d("INVENTORY","Loop started");
//				mEPCList = mDevice.getTagIDs();
//				mEPCListLiveData.postValue(mEPCList);
//				refreshData();
//				try {
//					Thread.sleep(100);
//				} catch (InterruptedException e) {
//					e.printStackTrace();
//				}
//
//			}
//			Log.d("INVENTORY", "Read with continuous mode");
//
//			int noTagCount = 0;
//			int maxNoTagCount = 30; // e.g., stop after 3 seconds (100ms * 30)
//			boolean isScanning = true;
//
//			while (mInventoryStart) {
//				if (isScanning) {
//					List<EPC> tags = mDevice.getTagIDs();
//					Log.d("INVENTORY", "Tags found: " + (tags == null ? "null" : tags.size()));
//					if (tags != null && !tags.isEmpty()) {
//						noTagCount = 0; // reset counter
//						mEPCList = tags;
//						mEPCListLiveData.postValue(mEPCList);
//						refreshData();
//					} else {
//						noTagCount++;
//						if (noTagCount >= maxNoTagCount) {
//							Log.d("INVENTORY", "No tags detected, entering paused mode");
//							isScanning = false;  // pause scanning
//						}
//					}
//				} else {
//					// In paused mode, periodically check if tags are back
//					List<EPC> tags = mDevice.getTagIDs();
//					Log.d("INVENTORY", "Tags else found: " + (tags == null ? "null" : tags.size()));
//					if (tags != null && !tags.isEmpty()) {
//						Log.d("INVENTORY", "Tags detected, resuming scanning");
//						noTagCount = 0;
//						isScanning = true;  // resume scanning
//						mEPCList = tags;
//						mEPCListLiveData.postValue(mEPCList);
//						refreshData();
//					} else {
//						// Sleep longer while paused to reduce resource use
//						try {
//							Thread.sleep(500);  // or longer if needed
//						} catch (InterruptedException e) {
//							e.printStackTrace();
//						}
//						continue;
//					}
//				}
//
//				// Normal scan loop delay
//				try {
//					Thread.sleep(100);
//				} catch (InterruptedException e) {
//					e.printStackTrace();
//				}
//			}

		} // end of run
	}
}
