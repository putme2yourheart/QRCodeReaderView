package io.github.putme2yourheart.sample;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import io.github.putme2yourheart.qrcodereaderview.QRCodeReaderView;
import io.github.putme2yourheart.qrcodereaderview.ScannerCoverLayout;

public class DecoderActivity extends AppCompatActivity implements QRCodeReaderView.OnQRCodeReadListener {
    private QRCodeReaderView mQRCodeReaderView;
    private ScannerCoverLayout mScannerCoverLayout;
    private TextView tvFlashlight;

    private boolean mIsOpenFlashlight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_decoder);

        initView();
    }

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

    @Override
    public void onQRCodeRead(String text) {
        setResult(RESULT_OK, new Intent().putExtra("data", text));
        finish();
    }

    private void initView() {
        mQRCodeReaderView = findViewById(R.id.QRCodeReaderView);
        mScannerCoverLayout = findViewById(R.id.ScannerCoverLayout);
        tvFlashlight = findViewById(R.id.tv_flashlight);

        tvFlashlight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mIsOpenFlashlight) {
                    mQRCodeReaderView.setTorchEnabled(false);
                    tvFlashlight.setText("Open Flashlight");
                } else {
                    mQRCodeReaderView.setTorchEnabled(true);
                    tvFlashlight.setText("Close Flashlight");
                }
                mIsOpenFlashlight = !mIsOpenFlashlight;
            }
        });

        mQRCodeReaderView.setAutofocusInterval(500L);
        mQRCodeReaderView.setOnQRCodeReadListener(this);
        mQRCodeReaderView.setBackCamera();
        mQRCodeReaderView.setKeepScreenOn(this, true);
        mQRCodeReaderView.setBeepAndVibrate(true, true);
        mQRCodeReaderView.startCamera();

        // Must bind ScannerCoverLayout
        mQRCodeReaderView.touchScannerLayout(mScannerCoverLayout);
    }
}
