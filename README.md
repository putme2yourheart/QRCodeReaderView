# QRCodeReaderView
#### Modification of ZXING Barcode Scanner project for easy Android QR-Code detection. ####

Usage
-----

- Add a "QRCodeReaderView" in the layout editor like you actually do with a button for example.
- In your onCreate method, you can find the view as usual, using findViewById() function.
- Create an Activity which implements `OnQRCodeReadListener`, and let implements required methods or set a `OnQRCodeReadListener` to the QRCodeReaderView object
- Make sure you have camera permissions in order to use the library. (https://developer.android.com/training/permissions/requesting.html)

```xml

<io.github.putme2yourheart.qrcodereaderview.QRCodeReaderView
        android:id="@+id/QRCodeReaderView"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />

```
- Start & Stop camera preview in onPause() and onResume() overriden methods.

```java

@Override
protected void onResume() {
    super.onResume();

    if (mQRCodeReaderView != null) {
        mQRCodeReaderView.startCamera();
    }
}

@Override
protected void onPause() {
    super.onPause();

    if (mQRCodeReaderView != null) {
        mQRCodeReaderView.stopCamera();
    }
}

```

- You can place ScannerCoverLayout or widgets or views over QRCodeReaderView.

```xml

<io.github.putme2yourheart.qrcodereaderview.ScannerCoverLayout
        android:id="@+id/ScannerCoverLayout"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        app:corner_color="@color/corner_color"
        app:frame_color="@color/viewfinder_frame"
        app:label_text_color="@color/colorAccent"
        app:laser_color="@color/laser_color"
        app:mask_color="@color/viewfinder_mask"
        app:result_color="@color/result_view"
        app:result_point_color="@color/result_point_color">

        <!--The view above the scaning rectangle-->
        <RelativeLayout
            android:layout_width="match_parent"
            android:layout_height="match_parent">

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_alignParentBottom="true"
                android:layout_centerHorizontal="true"
                android:layout_marginBottom="16dp"
                android:text="Scan QR code"
                android:textColor="#ffffff" />

        </RelativeLayout>

        <!--The view below the scanning rectangle-->
        <RelativeLayout
            android:layout_width="match_parent"
            android:layout_height="match_parent">

            <TextView
                android:id="@+id/tv_flashlight"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_alignParentBottom="true"
                android:background="#20000000"
                android:gravity="center"
                android:paddingBottom="16dp"
                android:paddingTop="16dp"
                android:text="Open Flashlight"
                android:textColor="#ffffff"
                android:textSize="16sp" />

        </RelativeLayout>

    </io.github.putme2yourheart.qrcodereaderview.ScannerCoverLayout>

```

- Methods

```java

    // Use this function to enable/disable decoding
    mQRCodeReaderView.setQRDecodingEnabled(true);
    
    // Use this function to change the autofocus interval
    mQRCodeReaderView.setAutofocusInterval(500L);
    
    // Use this function to set front camera preview
    mQRCodeReaderView.setFrontCamera();
    
    // Use this function to set back camera preview
    mQRCodeReaderView.setBackCamera();
    
    // Use this function to keep screen on
    mQRCodeReaderView.setKeepScreenOn(this, true);
    
    // Use this function to turn on sound and vibratioin
    mQRCodeReaderView.setBeepAndVibrate(true, true);
    
    // Use this function to enable/disable Torch
    mQRCodeReaderView.setTorchEnabled(true);

    // Must bind it if ScannerCoverLayout is used
    mQRCodeReaderView.touchScannerLayout(mScannerCoverLayout);
    
    // Called when a QR is decoded
    // "text" : the text encoded in QR
    mQRCodeReaderView.setOnQRCodeReadListener(this);

```

Libraries used in this project
------------------------------

* [ZXING][1]

Fork https://github.com/dlazaro66/QRCodeReaderView

[1]: https://github.com/zxing/zxing/
