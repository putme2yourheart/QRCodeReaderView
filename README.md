# QRCodeReaderView
QRCodeReaderView

Usage
-----

- Add a "QRCodeReaderView" in the layout editor like you actually do with a button for example.
- In your onCreate method, you can find the view as usual, using findViewById() function.
- Create an Activity which implements `onQRCodeReadListener`, and let implements required methods or set a `onQRCodeReadListener` to the QRCodeReaderView object
- Make sure you have camera permissions in order to use the library. (https://developer.android.com/training/permissions/requesting.html)

```xml

<io.github.putme2yourheart.qrcodereaderview.QRCodeReaderView
        android:id="@+id/QRCodeReaderView"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />

```
