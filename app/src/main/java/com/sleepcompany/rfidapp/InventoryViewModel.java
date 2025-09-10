package com.sleepcompany.rfidapp;

import androidx.databinding.ObservableField;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.seuic.uhf.EPC;
import com.seuic.uhf.UHFService;

import java.util.List;

public class InventoryViewModel extends ViewModel {
    private MutableLiveData<List<EPC>> mEPCListLiveData = new MutableLiveData<>();
    private MutableLiveData<Integer> mSelectedIndex = new MutableLiveData<>(-1);

    public ObservableField<Integer>  EPC_total = new ObservableField<>(0);

    public ObservableField<String> bank = new ObservableField<>("3");
    public ObservableField<String> address = new ObservableField<>("0");
    public ObservableField<String> length = new ObservableField<>("1");
    public ObservableField<String> password = new ObservableField<>("00000000");
    public ObservableField<String> data = new ObservableField<>();

    public UHFService mDevice;


    public MutableLiveData<List<EPC>> getEPCListLiveData(){
        return mEPCListLiveData;
    }

    public MutableLiveData<Integer> getSelectedIndex(){
        return mSelectedIndex;
    }


}
