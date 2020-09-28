package com.example.ui.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class HomeViewModel extends ViewModel {

    private MutableLiveData<String> mText;

    public HomeViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("Purchase");
    }

    public void setText(String txt) {
        mText.setValue(txt);
    }

    public LiveData<String> getText() {
        return mText;
    }
}