package nodomain.freeyourgadget.gadgetbridge.devices.qhybrid;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.ArrayList;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.GBException;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.database.DBOpenHelper;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit.PlayNotificationRequest;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class PackageConfigHelper {
    public static final String DB_NAME = "qhybridNotifications.db";
    public static final String DB_ID = "id";
    public static final String DB_TABLE = "notifications";
    public static final String DB_PACKAGE = "package";
    public static final String DB_APPNAME = "appName";
    public static final String DB_VIBRATION = "vibrationTtype";
    public static final String DB_MINUTE = "minDegress";
    public static final String DB_HOUR = "hourDegrees";
    public static final String DB_RESPECT_SILENT = "respectSilent";


    public PackageConfigHelper(Context context) throws GBException {
        initDB();
    }

    public void saveNotificationConfiguration(NotificationConfiguration settings) throws GBException {
        ContentValues values = new ContentValues(6);
        values.put(DB_PACKAGE, settings.getPackageName());
        values.put(DB_APPNAME, settings.getAppName());
        values.put(DB_HOUR, settings.getHour());
        values.put(DB_MINUTE, settings.getMin());
        values.put(DB_VIBRATION, settings.getVibration().getValue());
        values.put(DB_RESPECT_SILENT, settings.getRespectSilentMode());

        SQLiteDatabase database = GBApplication.acquireDB().getDatabase();

        if(settings.getId() == -1) {
            settings.setId(database.insert(DB_TABLE, null, values));
        }else{
            database.update(DB_TABLE, values, DB_ID + "=?", new String[]{String.valueOf(settings.getId())});
        }

        GBApplication.releaseDB();
        //LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent());
    }

    public ArrayList<NotificationConfiguration> getNotificationConfigurations() throws GBException {
        SQLiteDatabase database = GBApplication.acquireDB().getDatabase();

        Cursor cursor = database.query(DB_TABLE, new String[]{"*"}, null, null, null, null, null);

        GBApplication.releaseDB();

        int size = cursor.getCount();
        ArrayList<NotificationConfiguration> list = new ArrayList<>(size);
        if(size > 0){
            int appNamePos = cursor.getColumnIndex(DB_APPNAME);
            int packageNamePos = cursor.getColumnIndex(DB_PACKAGE);
            int hourPos = cursor.getColumnIndex(DB_HOUR);
            int minPos = cursor.getColumnIndex(DB_MINUTE);
            int silentPos = cursor.getColumnIndex(DB_RESPECT_SILENT);
            int vibrationPos = cursor.getColumnIndex(DB_VIBRATION);
            int idPos = cursor.getColumnIndex(DB_ID);
            cursor.moveToFirst();
            do {
                list.add(new NotificationConfiguration(
                        (short)cursor.getInt(minPos),
                        (short)cursor.getInt(hourPos),
                        cursor.getString(packageNamePos),
                        cursor.getString(appNamePos),
                        cursor.getInt(silentPos) == 1,
                        PlayNotificationRequest.VibrationType.fromValue((byte)cursor.getInt(vibrationPos)),
                        cursor.getInt(idPos)
                ));
                Log.d("Settings", "setting #" + cursor.getPosition() + ": " + cursor.getInt(silentPos));
            }while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    public NotificationConfiguration getNotificationConfiguration(String appName) throws GBException {
        if(appName == null) return null;

        SQLiteDatabase database = GBApplication.acquireDB().getDatabase();

        Cursor c = database.query(DB_TABLE, new String[]{"*"}, DB_APPNAME + "=?", new String[]{appName}, null, null, null);

        GBApplication.releaseDB();

        if(c.getCount() == 0){
            c.close();
            return null;
        }
        c.moveToFirst();
        NotificationConfiguration settings = new NotificationConfiguration(
                (short)c.getInt(c.getColumnIndex(DB_MINUTE)),
                (short)c.getInt(c.getColumnIndex(DB_HOUR)),
                c.getString(c.getColumnIndex(DB_PACKAGE)),
                c.getString(c.getColumnIndex(DB_APPNAME)),
                c.getInt(c.getColumnIndex(DB_RESPECT_SILENT)) == 1,
                PlayNotificationRequest.VibrationType.fromValue((byte)c.getInt(c.getColumnIndex(DB_VIBRATION))),
                c.getInt(c.getColumnIndex(DB_ID))
        );
        c.close();
        return settings;
    }

    private void initDB() throws GBException {
        SQLiteDatabase database = GBApplication.acquireDB().getDatabase();

        database.execSQL("CREATE TABLE IF NOT EXISTS notifications(" +
                DB_ID + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                DB_PACKAGE + " TEXT, " +
                DB_VIBRATION + " INTEGER, " +
                DB_MINUTE + " INTEGER DEFAULT -1, " +
                DB_APPNAME + " TEXT," +
                DB_RESPECT_SILENT + " INTEGER," +
                DB_HOUR + " INTEGER DEFAULT -1);");

        GBApplication.releaseDB();
    }

    public void deleteNotificationConfiguration(NotificationConfiguration packageSettings) throws GBException {
        Log.d("DB", "deleting id " + packageSettings.getId());
        if(packageSettings.getId() == -1) return;

        SQLiteDatabase database = GBApplication.acquireDB().getDatabase();

        database.delete(DB_TABLE, DB_ID + "=?", new String[]{String.valueOf(packageSettings.getId())});

        GBApplication.releaseDB();
    }
}
