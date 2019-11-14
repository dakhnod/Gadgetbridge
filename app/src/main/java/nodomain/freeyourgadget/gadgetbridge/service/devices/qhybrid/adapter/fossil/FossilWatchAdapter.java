package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.fossil;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBException;
import nodomain.freeyourgadget.gadgetbridge.Logging;
import nodomain.freeyourgadget.gadgetbridge.devices.qhybrid.NotificationConfiguration;
import nodomain.freeyourgadget.gadgetbridge.devices.qhybrid.PackageConfigHelper;
import nodomain.freeyourgadget.gadgetbridge.entities.NotificationFilter;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.GenericItem;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.QHybridSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.WatchAdapter;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.Request;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.FossilRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.RequestMtuRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.SetDeviceStateRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.configuration.ConfigurationGetRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.configuration.ConfigurationPutRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.connection.SetConnectionParametersRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.file.FileCloseRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.file.FileDeleteRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.file.FilePutRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.file.FileVerifyRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.notification.NotificationFilterPutRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.notification.PlayNotificationRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit.AnimationRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit.MoveHandsRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit.ReleaseHandsControlRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit.RequestHandControlRequest;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class FossilWatchAdapter extends WatchAdapter {
    private ArrayList<Request> requestQueue = new ArrayList<>();

    private FossilRequest fossilRequest;

    public FossilWatchAdapter(QHybridSupport deviceSupport) {
        super(deviceSupport);
    }


    @Override
    public void initialize() {
        // playPairingAnimation();
        // queueWrite(new FileDeleteRequest((short) 0x0200));
        queueWrite(new RequestMtuRequest(512));
        queueWrite(new ConfigurationGetRequest(this));
        // queueWrite(new SetConnectionParametersRequest());

        syncNotificationSettings();

        queueWrite(new SetDeviceStateRequest(GBDevice.State.INITIALIZED));
    }

    @Override
    public void playPairingAnimation() {
        queueWrite(new AnimationRequest());
    }

    @Override
    public void playNotification(NotificationConfiguration config) {
        if (config.getPackageName() == null) {
            log("package name in notification not set");
            return;
        }
        queueWrite(new PlayNotificationRequest(config.getPackageName(), this));
    }

    @Override
    public void setTime() {
        long millis = System.currentTimeMillis();
        TimeZone zone = new GregorianCalendar().getTimeZone();

        queueWrite(
                new ConfigurationPutRequest(
                        new ConfigurationPutRequest.TimeConfigItem(
                                (int) (millis / 1000 + getDeviceSupport().getTimeOffset() * 60),
                                (short) (millis % 1000),
                                (short) ((zone.getRawOffset() + (zone.inDaylightTime(new Date()) ? 1 : 0)) / 60000)
                        ),
                        this)
        );
    }

    @Override
    public void overwriteButtons() {
        FilePutRequest uploadFileRequest = new FilePutRequest((short) 0x0600, new byte[]{
                (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x03, (byte) 0x10, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x0C, (byte) 0x00, (byte) 0x00, (byte) 0x20, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x0C, (byte) 0x00, (byte) 0x00,
                (byte) 0x30, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x0C, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x0C, (byte) 0x2E, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01,
                (byte) 0x00, (byte) 0x06, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x03, (byte) 0x00, (byte) 0x02, (byte) 0x01, (byte) 0x0F, (byte) 0x00, (byte) 0x8B, (byte) 0x00, (byte) 0x00, (byte) 0x93, (byte) 0x00, (byte) 0x01,
                (byte) 0x08, (byte) 0x01, (byte) 0x14, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0xFE, (byte) 0x08, (byte) 0x00, (byte) 0x93, (byte) 0x00, (byte) 0x02, (byte) 0x01, (byte) 0x00, (byte) 0xBF, (byte) 0xD5, (byte) 0x54, (byte) 0xD1,
                (byte) 0x00
        }, this);
        queueWrite(uploadFileRequest);
    }

    @Override
    public void setActivityHand(double progress) {
        queueWrite(new ConfigurationPutRequest(
                new ConfigurationPutRequest.CurrentStepCountConfigItem(Math.min(999999, (int) (1000000 * progress))),
                this
        ));
    }

    @Override
    public void setHands(short hour, short minute) {
        queueWrite(new MoveHandsRequest(false, minute, hour, (short) -1));
    }


    public void vibrate(nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit.PlayNotificationRequest.VibrationType vibration) {
        // queueWrite(new nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit.PlayNotificationRequest(vibration, -1, -1));
    }

    @Override
    public void vibrateFindMyDevicePattern() {

    }


    @Override
    public void requestHandsControl() {
        queueWrite(new RequestHandControlRequest());
    }

    @Override
    public void releaseHandsControl() {
        queueWrite(new ReleaseHandsControlRequest());
    }

    @Override
    public void setStepGoal(int stepGoal) {
        queueWrite(new ConfigurationPutRequest(new ConfigurationPutRequest.DailyStepGoalConfigItem(stepGoal), this));
    }

    @Override
    public void setVibrationStrength(short strength) {
        ConfigurationPutRequest.ConfigItem vibrationItem = new ConfigurationPutRequest.VibrationStrengthConfigItem((byte)strength);


        queueWrite(
                new ConfigurationPutRequest(new ConfigurationPutRequest.ConfigItem[]{vibrationItem, vibrationItem, vibrationItem}, this)
        );
        // queueWrite(new FileVerifyRequest((short) 0x0800));
    }

    @Override
    public void syncNotificationSettings() {
        log("syncing notification settings...");
        try {
            PackageConfigHelper helper = new PackageConfigHelper(getContext());
            final ArrayList<NotificationConfiguration> configurations = helper.getNotificationConfigurations();
            if (configurations.size() == 1) configurations.add(configurations.get(0));
            queueWrite(new NotificationFilterPutRequest(configurations, FossilWatchAdapter.this) {
                @Override
                public void onFilePut(boolean success) {
                    super.onFilePut(success);

                    if (!success) {
                        GB.toast("error writing notification settings", Toast.LENGTH_SHORT, GB.ERROR);

                        getDeviceSupport().getDevice().setState(GBDevice.State.NOT_CONNECTED);
                        getDeviceSupport().getDevice().sendDeviceUpdateIntent(getContext());
                    }

                    getDeviceSupport().getDevice().setState(GBDevice.State.INITIALIZED);
                    getDeviceSupport().getDevice().sendDeviceUpdateIntent(getContext());
                }
            });
        } catch (GBException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onTestNewFunction() {
        queueWrite(new ConfigurationPutRequest(new ConfigurationPutRequest.ConfigItem[0], this));
    }

    @Override
    public boolean supportsFindDevice() {
        return false;
    }

    @Override
    public boolean supportsExtendedVibration() {
        String modelNumber = getDeviceSupport().getDevice().getModel();
        switch (modelNumber) {
            case "HW.0.0":
                return true;
            case "HL.0.0":
                return false;
        }
        throw new UnsupportedOperationException("model " + modelNumber + " not supported");
    }

    @Override
    public boolean supportsActivityHand() {
        String modelNumber = getDeviceSupport().getDevice().getModel();
        switch (modelNumber) {
            case "HW.0.0":
                return true;
            case "HL.0.0":
                return false;
        }
        throw new UnsupportedOperationException("Model " + modelNumber + " not supported");
    }

    @Override
    public String getModelName() {
        String modelNumber = getDeviceSupport().getDevice().getModel();
        switch (modelNumber) {
            case "HW.0.0":
                return "Q Commuter";
            case "HL.0.0":
                return "Q Activist";
        }
        return "unknwon Q";
    }

    @Override
    public void onFetchActivityData() {

        // queueWrite(new ConfigurationPutRequest(new ConfigurationPutRequest.ConfigItem[0], this));
        setVibrationStrength((byte) 50);
        // queueWrite(new FileCloseRequest((short) 0x0800));
        // queueWrite(new ConfigurationGetRequest(this));
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        switch (characteristic.getUuid().toString()) {
            case "3dda0006-957f-7d4a-34a6-74696673696d": {
                handleBackgroundCharacteristic(characteristic);
                break;
            }
            case "3dda0002-957f-7d4a-34a6-74696673696d":
            case "3dda0004-957f-7d4a-34a6-74696673696d":
            case "3dda0003-957f-7d4a-34a6-74696673696d": {
                if (fossilRequest != null) {
                    boolean requestFinished;
                    try {
                        if(characteristic.getUuid().toString().equals("3dda0003-957f-7d4a-34a6-74696673696d")){
                            byte requestType = (byte)(characteristic.getValue()[0] & 0x0F);

                            if(requestType != 0x0A && requestType != fossilRequest.getType()){
                                // throw new RuntimeException("Answer type " + requestType + " does not match current request " + fossilRequest.getType());
                            }
                        }

                        fossilRequest.handleResponse(characteristic);
                        requestFinished = fossilRequest.isFinished();
                    } catch (RuntimeException e) {
                        e.printStackTrace();
                        getDeviceSupport().notifiyException(e);
                        GB.toast(fossilRequest.getName() + " failed", Toast.LENGTH_SHORT, GB.ERROR);
                        requestFinished = true;
                    }

                    if (requestFinished) {
                        log(fossilRequest.getName() + " finished");
                        fossilRequest = null;
                    } else {
                        return true;
                    }
                }
                try {
                    queueWrite(requestQueue.remove(0));
                } catch (IndexOutOfBoundsException e) {
                    log("requestsQueue empty");
                }
            }
        }
        return true;
    }

    private void handleBackgroundCharacteristic(BluetoothGattCharacteristic characteristic){
        byte[] value = characteristic.getValue();
        switch (value[1]){
            case 2: {
                byte syncId = value[2];
                getDeviceSupport().getDevice().addDeviceInfo(new GenericItem(QHybridSupport.ITEM_LAST_HEARTBEAT, DateFormat.getTimeInstance().format(new Date())));
                break;
            }
            case 8: {

                break;
            }
        }
    }

    private void log(String message) {
        Log.d("FossilWatchAdapter", message);
    }

    public void queueWrite(Request request) {
        this.queueWrite(request, false);
    }

    @Override
    public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
        super.onMtuChanged(gatt, mtu, status);
        ((RequestMtuRequest)fossilRequest).setFinished(true);
        try {
            queueWrite(requestQueue.remove(0));
        } catch (IndexOutOfBoundsException e) {
        }
    }

    //TODO split to multiple methods instead of switch
    public void queueWrite(Request request, boolean priorise) {
        if(request instanceof RequestMtuRequest){
            //TODO mtu on older devices
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                new TransactionBuilder("requestMtu")
                        .requestMtu(512)
                        .queue(getDeviceSupport().getQueue());

                this.fossilRequest = (FossilRequest) request;
            }
            return;
        }else if(request instanceof SetDeviceStateRequest){
            log("setting device state: " + ((SetDeviceStateRequest)request).getDeviceState());
            getDeviceSupport().getDevice().setState(((SetDeviceStateRequest)request).getDeviceState());
            getDeviceSupport().getDevice().sendDeviceUpdateIntent(getContext());
            try {
                queueWrite(requestQueue.remove(0));
            } catch (IndexOutOfBoundsException e) {
            }
            return;
        } else if (request.isBasicRequest()) {
            try {
                queueWrite(requestQueue.remove(0));
            } catch (IndexOutOfBoundsException e) {
            }
        } else {
            if (fossilRequest != null && !fossilRequest.isFinished()) {
                log("queing request: " + request.getName());
                if (priorise) {
                    requestQueue.add(0, request);
                } else {
                    requestQueue.add(request);
                }
                return;
            }
            log("executing request: " + request.getName());

            if (request instanceof FossilRequest) this.fossilRequest = (FossilRequest) request;
        }

        new TransactionBuilder(request.getClass().getSimpleName()).write(getDeviceSupport().getCharacteristic(request.getRequestUUID()), request.getRequestData()).queue(getDeviceSupport().getQueue());
    }
}
