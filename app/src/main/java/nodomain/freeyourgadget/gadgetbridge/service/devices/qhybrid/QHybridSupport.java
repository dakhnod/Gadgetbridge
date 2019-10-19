package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.TimeZone;
import java.util.UUID;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceManager;
import nodomain.freeyourgadget.gadgetbridge.devices.qhybrid.PackageConfig;
import nodomain.freeyourgadget.gadgetbridge.devices.qhybrid.PackageConfigHelper;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryState;
import nodomain.freeyourgadget.gadgetbridge.model.GenericItem;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.RecordedDataTypes;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceStateAction;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.ActivityPointGetRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.AnimationRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.BatteryLevelRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.DownloadFileRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.EraseFileRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.FileRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.GetCountdownSettingsRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.GetCurrentStepCountRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.GetStepGoalRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.GetVibrationStrengthRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.GoalTrackingGetRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.ListFilesRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.MoveHandsRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.OTAEnterRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.OTAEraseRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.PlayNotificationRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.ReleaseHandsControlRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.Request;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.RequestHandControlRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.SetCurrentStepCountRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.SetStepGoalRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.SetTimeRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.SetVibrationStrengthRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.UploadFileRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.VibrateRequest;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class QHybridSupport extends QHybridBaseSupport {
    public static final String QHYBRID_COMMAND_CONTROL = "qhybrid_command_control";
    public static final String QHYBRID_COMMAND_UNCONTROL = "qhybrid_command_uncontrol";
    public static final String QHYBRID_COMMAND_SET = "qhybrid_command_set";
    public static final String QHYBRID_COMMAND_VIBRATE = "qhybrid_command_vibrate";
    public static final String QHYBRID_COMMAND_UPDATE = "qhybrid_command_update";
    public static final String QHYBRID_COMMAND_NOTIFICATION = "qhybrid_command_notification";
    public static final String QHYBRID_COMMAND_UPDATE_SETTINGS = "nodomain.freeyourgadget.gadgetbridge.Q_UPDATE_SETTINGS";
    public static final String QHYBRID_COMMAND_OVERWRITE_BUTTONS = "nodomain.freeyourgadget.gadgetbridge.Q_OVERWRITE_BUTTONS";

    public static final String QHYBRID_EVENT_SETTINGS_UPDATED = "nodomain.freeyourgadget.gadgetbridge.Q_SETTINGS_UPDATED";
    public static final String QHYBRID_EVENT_FILE_UPLOADED = "nodomain.freeyourgadget.gadgetbridge.Q_FILE_UPLOADED";

    public static final String QHYBRID_EVENT_BUTTON_PRESS = "nodomain.freeyourgadget.gadgetbridge.Q_BUTTON_PRESSED";

    public static final String ITEM_STEP_GOAL = "STEP_GOAL";
    public static final String ITEM_STEP_COUNT = "STEP_COUNT";
    public static final String ITEM_VIBRATION_STRENGTH = "VIBRATION_STRENGTH";
    public static final String ITEM_ACTIVITY_POINT = "ACTIVITY_POINT";
    public static final String ITEM_EXTENDED_VIBRATION_SUPPORT = "EXTENDED_VIBRATION";
    public static final String ITEM_HAS_ACTIVITY_HAND = "HAS_ACTIVITY_HAND";
    public static final String ITEM_USE_ACTIVITY_HAND = "USE_ACTIVITY_HAND";

    private static final Logger logger = LoggerFactory.getLogger(QHybridSupport.class);

    private PackageConfigHelper helper;

    private volatile boolean searchDevice = false;

    private int lastButtonIndex = -1;

    private final SparseArray<Request> responseFilters = new SparseArray<>();

    private Request fileRequest = null;

    private long timeOffset;

    private UploadFileRequest uploadFileRequest;

    private Queue<Request> requestQueue = new ArrayDeque<>();

    private String modelNumber;

    private boolean useActivityHand;

    ArrayList<Integer> notificationStack = new ArrayList<>();

    public QHybridSupport() {
        super(logger);
        addSupportedService(UUID.fromString("3dda0001-957f-7d4a-34a6-74696673696d"));
        addSupportedService(UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb"));
        addSupportedService(UUID.fromString("00001800-0000-1000-8000-00805f9b34fb"));
        addSupportedService(UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb"));
        IntentFilter commandFilter = new IntentFilter(QHYBRID_COMMAND_CONTROL);
        commandFilter.addAction(QHYBRID_COMMAND_UNCONTROL);
        commandFilter.addAction(QHYBRID_COMMAND_SET);
        commandFilter.addAction(QHYBRID_COMMAND_VIBRATE);
        commandFilter.addAction(QHYBRID_COMMAND_UPDATE);
        commandFilter.addAction(QHYBRID_COMMAND_NOTIFICATION);
        commandFilter.addAction(QHYBRID_COMMAND_UPDATE_SETTINGS);
        commandFilter.addAction(QHYBRID_COMMAND_OVERWRITE_BUTTONS);
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(commandReceiver, commandFilter);
        fillResponseList();
    }

    private boolean supportsActivityHand() {
        switch (modelNumber) {
            case "HL.0.0":
                return false;
            case "HW.0.0":
                return true;
        }
        throw new UnsupportedOperationException();
    }

    private boolean supportsExtendedVibration() {
        switch (modelNumber) {
            case "HL.0.0":
                return false;
            case "HW.0.0":
                return true;
        }
        throw new UnsupportedOperationException();
    }

    private void fillResponseList() {
        Class<? extends Request>[] classes = new Class[]{
                BatteryLevelRequest.class,
                GetStepGoalRequest.class,
                GetVibrationStrengthRequest.class,
                GetCurrentStepCountRequest.class,
                OTAEnterRequest.class,
                GoalTrackingGetRequest.class,
                ActivityPointGetRequest.class,
                GetCountdownSettingsRequest.class
        };
        for (Class<? extends Request> c : classes) {
            try {
                c.getSuperclass().getDeclaredMethod("handleResponse", BluetoothGattCharacteristic.class);
                Request object = c.newInstance();
                byte[] sequence = object.getStartSequence();
                if (sequence.length > 1) {
                    responseFilters.put((int) object.getStartSequence()[1], object);
                    Log.d("Service", "response filter " + object.getStartSequence()[1] + ": " + c.getSimpleName());
                }
            } catch (NoSuchMethodException | IllegalAccessException | InstantiationException e) {
                Log.d("Service", "skipping class " + c.getName());
            }
        }
    }

    private void getTimeOffset() {
        timeOffset = getContext().getSharedPreferences(getContext().getPackageName(), Context.MODE_PRIVATE).getInt("QHYBRID_TIME_OFFSET", 0);
    }

    private void queueWrite(Request request) {
        new TransactionBuilder(request.getClass().getSimpleName()).write(getCharacteristic(request.getRequestUUID()), request.getRequestData()).queue(getQueue());
        if (request instanceof FileRequest) this.fileRequest = request;

        if (!request.expectsResponse()) {
            try {
                queueWrite(requestQueue.remove());
            } catch (NoSuchElementException e) {
            }
        }
    }

    @Override
    protected TransactionBuilder initializeDevice(TransactionBuilder builder) {
        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZING, getContext()));

        this.useActivityHand = GBApplication.getPrefs().getBoolean("QHYBRID_USE_ACTIVITY_HAND", false);
        getDevice().addDeviceInfo(new GenericItem(ITEM_USE_ACTIVITY_HAND, String.valueOf(this.useActivityHand)));

        getDevice().setNotificationIconConnected(R.drawable.ic_notification_qhybrid);
        getDevice().setNotificationIconDisconnected(R.drawable.ic_notification_disconnected_qhybrid);

        for (int i = 2; i <= 7; i++)
            builder.notify(getCharacteristic(UUID.fromString("3dda000" + i + "-957f-7d4a-34a6-74696673696d")), true);

        requestQueue.add(new GetCurrentStepCountRequest());
        requestQueue.add(new GetVibrationStrengthRequest());
        requestQueue.add(new ActivityPointGetRequest());
        requestQueue.add(prepareSetTimeRequest());
        requestQueue.add(new AnimationRequest());

        Request initialRequest = new GetStepGoalRequest();

        builder.read(getCharacteristic(UUID.fromString("00002a00-0000-1000-8000-00805f9b34fb")))
                .read(getCharacteristic(UUID.fromString("00002a24-0000-1000-8000-00805f9b34fb")))
                .read(getCharacteristic(UUID.fromString("00002a26-0000-1000-8000-00805f9b34fb")))
                .read(getCharacteristic(UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb")))
                .notify(getCharacteristic(UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb")), true)
                .write(getCharacteristic(initialRequest.getRequestUUID()), initialRequest.getRequestData())
        ;

        helper = new PackageConfigHelper(getContext());
        getTimeOffset();

        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZED, getContext()));

        return builder;
    }

    @Override
    public void onFetchRecordedData(int dataTypes) {
        if ((dataTypes & RecordedDataTypes.TYPE_ACTIVITY) != 0) {
            requestQueue.add(new BatteryLevelRequest());
            requestQueue.add(new GetCurrentStepCountRequest());
            requestQueue.add(new ListFilesRequest());
            queueWrite(new ActivityPointGetRequest());
        }
    }

    @Override
    public void onNotification(NotificationSpec notificationSpec) {
        Log.d("Service", "notif from " + notificationSpec.sourceAppId + "  " + notificationSpec.sender + "   " + notificationSpec.phoneNumber);
        //new Exception().printStackTrace();
        String packageName = notificationSpec.sourceName;

        PackageConfig config = helper.getSetting(packageName);
        if (config == null) return;

        Log.d("Service", "handling notification");

        if (config.getRespectSilentMode()) {
            int mode = ((AudioManager) getContext().getApplicationContext().getSystemService(Context.AUDIO_SERVICE)).getRingerMode();
            if (mode == AudioManager.RINGER_MODE_SILENT) return;
        }

        playNotification(config);

        notificationStack.remove(Integer.valueOf(notificationSpec.getId()));
        notificationStack.add(Integer.valueOf(notificationSpec.getId()));
        showNotificationCountOnActivityHand();
    }

    @Override
    public void onDeleteNotification(int id) {
        super.onDeleteNotification(id);

        notificationStack.remove(Integer.valueOf(id));
        showNotificationCountOnActivityHand();
    }

    private void showNotificationCountOnActivityHand(){
        if(useActivityHand){
            setActivityHand(notificationStack.size() / 4.0);
        }
    }

    private void playNotification(PackageConfig config) {
        queueWrite(new PlayNotificationRequest(config.getVibration(), config.getHour(), config.getMin()));
    }

    @Override
    public void onSetTime() {
        queueWrite(prepareSetTimeRequest());
    }

    private SetTimeRequest prepareSetTimeRequest() {
        long millis = System.currentTimeMillis();
        TimeZone zone = new GregorianCalendar().getTimeZone();
        SetTimeRequest request = new SetTimeRequest(
                (int) (millis / 1000 + timeOffset * 60),
                (short) (millis % 1000),
                (short) ((zone.getRawOffset() + zone.getDSTSavings()) / 60000));
        return request;
    }

    @Override
    public void onFindDevice(boolean start) {
        try {
            if (start && !supportsExtendedVibration()) {
                GB.toast("Device does not support brr brr", Toast.LENGTH_SHORT, GB.INFO);
                return;
            }
        } catch (UnsupportedOperationException e) {
            GB.toast("Please contact dakhnod@gmail.com\n", Toast.LENGTH_SHORT, GB.INFO);
        }

        if (start && searchDevice) return;

        searchDevice = start;

        if (start) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    VibrateRequest request = new VibrateRequest(false, (short) 4, (short) 1);
                    BluetoothGattCharacteristic chara = getCharacteristic(request.getRequestUUID());
                    int i = 0;
                    while (searchDevice) {
                        new TransactionBuilder("findDevice#" + i++).write(chara, request.getRequestData()).queue(getQueue());
                        try {
                            Thread.sleep(2500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }).start();
        }
    }

    @Override
    public void onTestNewFunction() {
        //downloadActivityFiles();
        //queueWrite(new GetCurrentStepCountRequest());
        // queueWrite(new EventStreamRequest((short)4));
        // queueWrite(new OTAEraseRequest(0));
        // queueWrite(new OTAResetRequest());
        // new UploadFileRequest((short)00, new byte[]{0x01, 0x00, 0x08, 0x01, 0x01, 0x0C, 0x00, (byte)0xBD, 0x01, 0x30, 0x71, (byte)0xFF, 0x05, 0x00, 0x01, 0x00});
        // queueWrite(new ActivityPointGetRequest());
        long millis = System.currentTimeMillis();
        int secs = (int) (millis / 1000 * 60);
        //queueWrite(new SetCountdownSettings(secs, secs + 10, (short) 120));
        //queueWrite(new GetCountdownSettingsRequest());

        // queueWrite(new AnimationRequest());
        queueWrite(new SetCurrentStepCountRequest(1000000));
    }

    private void overwriteButtons() {
        uploadFileRequest = new UploadFileRequest((short) 0x0800, new byte[]{
                (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x03, (byte) 0x10, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x0C, (byte) 0x00, (byte) 0x00, (byte) 0x20, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x0C, (byte) 0x00, (byte) 0x00,
                (byte) 0x30, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x0C, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x0C, (byte) 0x2E, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01,
                (byte) 0x00, (byte) 0x06, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x03, (byte) 0x00, (byte) 0x02, (byte) 0x01, (byte) 0x0F, (byte) 0x00, (byte) 0x8B, (byte) 0x00, (byte) 0x00, (byte) 0x93, (byte) 0x00, (byte) 0x01,
                (byte) 0x08, (byte) 0x01, (byte) 0x14, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0xFE, (byte) 0x08, (byte) 0x00, (byte) 0x93, (byte) 0x00, (byte) 0x02, (byte) 0x01, (byte) 0x00, (byte) 0xBF, (byte) 0xD5, (byte) 0x54, (byte) 0xD1,
                (byte) 0x00
        });
        queueWrite(uploadFileRequest);
    }

    private void backupFile(DownloadFileRequest request) {
        try {
            File f = new File("/sdcard/qFiles/");
            if (!f.exists()) f.mkdir();

            File file = new File("/sdcard/qFiles/" + request.timeStamp);
            if (file.exists()) {
                throw new Exception("file " + file.getPath() + " exists");
            }
            logger.debug("Writing file " + file.getPath());
            file.createNewFile();
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(request.file);
            fos.close();
            logger.debug("file written.");

            FileOutputStream fos2 = new FileOutputStream("/sdcard/qFiles/steps", true);
            fos2.write(("file " + request.timeStamp + " cut\n\n").getBytes());
            fos2.close();

            queueWrite(new EraseFileRequest((short) request.fileHandle));
        } catch (Exception e) {
            e.printStackTrace();
            if (request.fileHandle > 257) {
                queueWrite(new DownloadFileRequest((short) (request.fileHandle - 1)));
            }
        }
    }


    @Override
    public boolean onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        switch (characteristic.getUuid().toString()) {
            case "00002a00-0000-1000-8000-00805f9b34fb": {
                String deviceName = characteristic.getStringValue(0);
                break;
            }
            case "00002a24-0000-1000-8000-00805f9b34fb": {
                modelNumber = characteristic.getStringValue(0);
                gbDevice.setModel(modelNumber);
                gbDevice.setName(getModelNameByModelNumber(modelNumber));
                try {
                    gbDevice.addDeviceInfo(new GenericItem(ITEM_EXTENDED_VIBRATION_SUPPORT, String.valueOf(supportsExtendedVibration())));
                    gbDevice.addDeviceInfo(new GenericItem(ITEM_HAS_ACTIVITY_HAND, String.valueOf(supportsActivityHand())));
                } catch (UnsupportedOperationException e) {
                    GB.toast("Please contact dakhnod@gmail.com\n", Toast.LENGTH_SHORT, GB.INFO);
                    gbDevice.addDeviceInfo(new GenericItem(ITEM_EXTENDED_VIBRATION_SUPPORT, "false"));
                }
                break;
            }
            case "00002a26-0000-1000-8000-00805f9b34fb": {
                String firmwareVersion = characteristic.getStringValue(0);
                gbDevice.setFirmwareVersion(firmwareVersion);

                checkFirmwareVersion(firmwareVersion);

                break;
            }
            case "00002a19-0000-1000-8000-00805f9b34fb": {
                short level = characteristic.getValue()[0];
                gbDevice.setBatteryLevel(level);

                gbDevice.setBatteryThresholdPercent((short) 2);

                GBDeviceEventBatteryInfo batteryInfo = new GBDeviceEventBatteryInfo();
                batteryInfo.level = gbDevice.getBatteryLevel();
                batteryInfo.state = BatteryState.BATTERY_NORMAL;
                handleGBDeviceEvent(batteryInfo);
                break;
            }
        }

        return true;
    }

    private String getModelNameByModelNumber(String modelNumber) {
        switch (modelNumber) {
            case "HW.0.0":
                return "Q Commuter";
            case "HL.0.0":
                return "Q Activist";
        }
        return "unknwon Q";
    }

    private void checkFirmwareVersion(String firmwareVersion) {
        if (!isFirmwareCompatible(firmwareVersion)) {
            GB.toast("Firmware " + firmwareVersion + " not compatible", Toast.LENGTH_LONG, GB.ERROR);
            getDevice().setName("incopatible firmware");
        }
    }

    private boolean isFirmwareCompatible(String firmwareVersion) {
        int major = Integer.parseInt(firmwareVersion.substring(6, 7));

        return major == 1;
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        switch (characteristic.getUuid().toString()) {
            case "3dda0004-957f-7d4a-34a6-74696673696d":
            case "3dda0003-957f-7d4a-34a6-74696673696d": {
                return handleFileDownloadCharacteristic(characteristic);
            }
            case "3dda0007-957f-7d4a-34a6-74696673696d": {
                return handleFileUploadCharacteristic(characteristic);
            }
            case "3dda0002-957f-7d4a-34a6-74696673696d": {
                return handleBasicCharacteristic(characteristic);
            }
            case "3dda0006-957f-7d4a-34a6-74696673696d": {
                return handleButtonCharacteristic(characteristic);
            }
            case "00002a19-0000-1000-8000-00805f9b34fb": {
                short level = characteristic.getValue()[0];
                gbDevice.setBatteryLevel(level);

                gbDevice.setBatteryThresholdPercent((short) 2);

                GBDeviceEventBatteryInfo batteryInfo = new GBDeviceEventBatteryInfo();
                batteryInfo.level = gbDevice.getBatteryLevel();
                batteryInfo.state = BatteryState.BATTERY_NORMAL;
                handleGBDeviceEvent(batteryInfo);
                break;
            }
            default: {
                Log.d("Service", "unknown shit on " + characteristic.getUuid().toString() + ":  " + arrayToString(characteristic.getValue()));
                try {
                    File charLog = new File("/sdcard/qFiles/charLog.txt");
                    if (!charLog.exists()) {
                        charLog.createNewFile();
                    }

                    FileOutputStream fos = new FileOutputStream(charLog, true);
                    fos.write((new Date().toString() + ": " + characteristic.getUuid().toString() + ": " + arrayToString(characteristic.getValue())).getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            }
        }
        return super.onCharacteristicChanged(gatt, characteristic);
    }

    private void setActivityHand(double progress){
        queueWrite(new SetCurrentStepCountRequest(Math.min((int)(1000000 * progress), 999999)));
    }

    private boolean handleFileUploadCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (uploadFileRequest == null) {
            logger.debug("no uploadFileRequest to handle response");
            return true;
        }

        uploadFileRequest.handleResponse(characteristic);

        switch (uploadFileRequest.state) {
            case ERROR:
                Intent fileIntent = new Intent(QHYBRID_EVENT_FILE_UPLOADED);
                fileIntent.putExtra("EXTRA_ERROR", true);
                LocalBroadcastManager.getInstance(getContext()).sendBroadcast(fileIntent);
                uploadFileRequest = null;
                break;
            case UPLOAD:
                for (byte[] packet : this.uploadFileRequest.packets) {
                    new TransactionBuilder("File upload").write(characteristic, packet).queue(getQueue());
                }
                break;
            case UPLOADED:
                fileIntent = new Intent(QHYBRID_EVENT_FILE_UPLOADED);
                LocalBroadcastManager.getInstance(getContext()).sendBroadcast(fileIntent);
                uploadFileRequest = null;
                break;
        }
        return true;
    }

    private boolean handleButtonCharacteristic(BluetoothGattCharacteristic characteristic) {
        byte[] value = characteristic.getValue();
        if (value.length != 11) {
            logger.debug("wrong button message");
            return true;
        }
        int index = value[6] & 0xFF;
        int button = value[8] >> 4 & 0xFF;

        if (index != this.lastButtonIndex) {
            lastButtonIndex = index;
            logger.debug("Button press on button " + button);

            Intent i = new Intent(QHYBRID_EVENT_BUTTON_PRESS);
            i.putExtra("BUTTON", button);

            //ByteBuffer buffer = ByteBuffer.allocate(16);
            //buffer.put(new byte[]{0x01, 0x00, 0x08});
            //buffer.put(value, 2, 8);
            //buffer.put(new byte[]{(byte)0xFF, 0x05, 0x00, 0x01, 0x00});

            //UploadFileRequest request = new UploadFileRequest((short)0, buffer.array());
            //for(byte[] packet : request.packets){
            //    new TransactionBuilder("File upload").write(getCharacteristic(UUID.fromString("3dda0007-957f-7d4a-34a6-74696673696d")), packet).queue(getQueue());
            //}

            getContext().sendBroadcast(i);
        }
        return true;
    }

    private boolean handleBasicCharacteristic(BluetoothGattCharacteristic characteristic) {
        byte[] values = characteristic.getValue();
        Request request = resolveAnswer(characteristic);

        if (request == null) {
            StringBuilder valueString = new StringBuilder(String.valueOf(values[0]));
            for (int i = 1; i < characteristic.getValue().length; i++) {
                valueString.append(", ").append(values[i]);
            }
            Log.d("Service", "unable to resolve " + characteristic.getUuid().toString() + ": " + valueString);
            return true;
        }
        Log.d("Service", "response: " + request.getClass().getSimpleName());
        request.handleResponse(characteristic);

        if (request instanceof GetStepGoalRequest) {
            gbDevice.addDeviceInfo(new GenericItem(ITEM_STEP_GOAL, String.valueOf(((GetStepGoalRequest) request).stepGoal)));
        } else if (request instanceof GetVibrationStrengthRequest) {
            int strength = ((GetVibrationStrengthRequest) request).strength;
            gbDevice.addDeviceInfo(new GenericItem(ITEM_VIBRATION_STRENGTH, String.valueOf(strength)));
        } else if (request instanceof GetCurrentStepCountRequest) {
            int steps = ((GetCurrentStepCountRequest) request).steps;
            logger.debug("get current steps: " + steps);
            try {
                File f = new File("/sdcard/qFiles/");
                if (!f.exists()) f.mkdir();

                File file = new File("/sdcard/qFiles/steps");
                if (!file.exists()) {
                    file.createNewFile();
                }
                logger.debug("Writing file " + file.getPath());
                FileOutputStream fos = new FileOutputStream(file, true);
                fos.write((System.currentTimeMillis() + ": " + steps + "\n").getBytes());
                fos.close();
                logger.debug("file written.");
            } catch (Exception e) {
                e.printStackTrace();
            }
            gbDevice.addDeviceInfo(new GenericItem(ITEM_STEP_COUNT, String.valueOf(((GetCurrentStepCountRequest) request).steps)));
        } else if (request instanceof OTAEnterRequest) {
            if (((OTAEnterRequest) request).success) {
                fileRequest = new OTAEraseRequest(1024 << 16);
                queueWrite(fileRequest);
            }
        } else if (request instanceof ActivityPointGetRequest) {
            gbDevice.addDeviceInfo(new GenericItem(ITEM_ACTIVITY_POINT, String.valueOf(((ActivityPointGetRequest) request).activityPoint)));
        }
        try {
            queueWrite(requestQueue.remove());
        } catch (NoSuchElementException e) {
        }
        LocalBroadcastManager.getInstance(getContext()).sendBroadcast(new Intent(DeviceManager.ACTION_DEVICES_CHANGED));
        return true;
    }

    private boolean handleFileDownloadCharacteristic(BluetoothGattCharacteristic characteristic) {
        Request request;
        request = fileRequest;
        request.handleResponse(characteristic);
        if (request instanceof ListFilesRequest) {
            if (((ListFilesRequest) request).completed) {
                logger.debug("File count: " + ((ListFilesRequest) request).fileCount + "  size: " + ((ListFilesRequest) request).size);
                if (((ListFilesRequest) request).fileCount == 0) return true;
                // queueWrite(new DownloadFileRequest((short) (256 + ((ListFilesRequest) request).fileCount)));
            }
        } else if (request instanceof DownloadFileRequest) {
            if (((FileRequest) request).completed) {
                logger.debug("file " + ((DownloadFileRequest) request).fileHandle + " completed: " + ((DownloadFileRequest) request).size);
                backupFile((DownloadFileRequest) request);
            }
        } else if (request instanceof EraseFileRequest) {
            if (((EraseFileRequest) request).fileHandle > 257) {
                queueWrite(new DownloadFileRequest((short) (((EraseFileRequest) request).fileHandle - 1)));
            }
        }
        return true;
    }

    private String arrayToString(byte[] bytes) {
        if (bytes.length == 0) return "";
        StringBuilder s = new StringBuilder();
        final String chars = "0123456789ABCDEF";
        for (byte b : bytes) {
            s.append(chars.charAt((b >> 4) & 0xF)).append(chars.charAt(b & 0xF)).append(" ");
        }
        return s.substring(0, s.length() - 1) + "\n";
    }

    private Request resolveAnswer(BluetoothGattCharacteristic characteristic) {
        byte[] values = characteristic.getValue();
        if (values[0] != 3) return null;
        return responseFilters.get(values[1]);
    }

    private void setHands(short hour, short minute) {
        queueWrite(new MoveHandsRequest(false, minute, hour, (short) -1));
    }

    private void vibrate(int vibration) {
        queueWrite(new PlayNotificationRequest(vibration, -1, -1));
    }

    private final BroadcastReceiver commandReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle extras = intent.getExtras();
            PackageConfig config = extras == null ? null : (PackageConfig) intent.getExtras().get("CONFIG");
            switch (intent.getAction()) {
                case QHYBRID_COMMAND_CONTROL: {
                    Log.d("Service", "sending control request");
                    queueWrite(new RequestHandControlRequest());
                    if (config != null) {
                        setHands(config.getHour(), config.getMin());
                    } else {
                        setHands((short) 0, (short) 0);
                    }
                    break;
                }
                case QHYBRID_COMMAND_UNCONTROL: {
                    queueWrite(new ReleaseHandsControlRequest());
                    break;
                }
                case QHYBRID_COMMAND_SET: {
                    setHands(config.getHour(), config.getMin());

                    break;
                }
                case QHYBRID_COMMAND_VIBRATE: {
                    vibrate(config.getVibration());
                    break;
                }
                case QHYBRID_COMMAND_NOTIFICATION: {
                    queueWrite(new PlayNotificationRequest(config.getVibration(), config.getHour(), config.getMin()));
                    break;
                }
                case QHYBRID_COMMAND_UPDATE: {
                    getTimeOffset();
                    onSetTime();
                    break;
                }
                case QHYBRID_COMMAND_UPDATE_SETTINGS: {
                    String newSetting = intent.getStringExtra("EXTRA_SETTING");
                    switch (newSetting) {
                        case ITEM_VIBRATION_STRENGTH: {
                            queueWrite(new SetVibrationStrengthRequest(Short.parseShort(gbDevice.getDeviceInfo(ITEM_VIBRATION_STRENGTH).getDetails())));
                            // queueWrite(new VibrateRequest(false, (short)4, (short)1));
                            break;
                        }
                        case ITEM_STEP_GOAL: {
                            queueWrite(new SetStepGoalRequest(Integer.parseInt(gbDevice.getDeviceInfo(ITEM_STEP_GOAL).getDetails())));
                            break;
                        }
                        case ITEM_USE_ACTIVITY_HAND: {
                            QHybridSupport.this.useActivityHand = gbDevice.getDeviceInfo(ITEM_USE_ACTIVITY_HAND).getDetails().equals("true");
                            GBApplication.getPrefs().getPreferences().edit().putBoolean("QHYBRID_USE_ACTIVITY_HAND", useActivityHand).apply();
                            break;
                        }
                    }

                    LocalBroadcastManager.getInstance(getContext()).sendBroadcast(new Intent(QHYBRID_EVENT_SETTINGS_UPDATED));
                    break;
                }
                case QHYBRID_COMMAND_OVERWRITE_BUTTONS: {
                    overwriteButtons();
                    break;
                }
            }
        }
    };
}