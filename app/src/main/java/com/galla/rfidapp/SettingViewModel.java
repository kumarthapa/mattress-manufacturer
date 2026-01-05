package com.galla.rfidapp;

import androidx.databinding.ObservableField;
import androidx.lifecycle.ViewModel;

public class SettingViewModel extends ViewModel {
    public ObservableField<String> power = new ObservableField<>();
    public ObservableField<String> region = new ObservableField<>();
    public ObservableField<String> temp = new ObservableField<>();
    public ObservableField<String> version = new ObservableField<>();


}
