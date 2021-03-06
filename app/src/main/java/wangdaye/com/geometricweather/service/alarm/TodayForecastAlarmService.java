package wangdaye.com.geometricweather.service.alarm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.widget.Toast;

import wangdaye.com.geometricweather.R;
import wangdaye.com.geometricweather.basic.GeoAlarmService;
import wangdaye.com.geometricweather.data.entity.model.Location;
import wangdaye.com.geometricweather.data.entity.model.weather.Weather;
import wangdaye.com.geometricweather.utils.remoteView.ForecastNotificationUtils;

/**
 * Today forecast alarm service.
 * */

public class TodayForecastAlarmService extends GeoAlarmService {
    // data
    public static final int ALARM_CODE = 2;

    /** <br> life cycle. */

    @Override
    protected void doRefresh(Location location) {
        if (ForecastNotificationUtils.isEnable(this, true)) {
            setAlarmIntent(this, TodayForecastAlarmService.class);
            if (ForecastNotificationUtils.isForecastTime(this, true)) {
                requestData(location);
            }
        }
    }

    @Override
    public void updateView(Context context, Location location, Weather weather) {
        ForecastNotificationUtils.buildForecastAndSendIt(context, weather, true);
    }

    public void setAlarmIntent(Context context, Class<?> cls) {
        Intent target = new Intent(context, cls);
        PendingIntent pendingIntent = PendingIntent.getService(
                context,
                ALARM_CODE,
                target,
                PendingIntent.FLAG_UPDATE_CURRENT);

        ((AlarmManager) getSystemService(Context.ALARM_SERVICE))
                .set(
                        AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        SystemClock.elapsedRealtime() + ForecastNotificationUtils.calcForecastDuration(context, true, false),
                        pendingIntent);
    }

    /** <br> interface. */

    @Override
    public void requestWeatherFailed(Location requestLocation) {
        Toast.makeText(
                this,
                getString(R.string.feedback_get_weather_failed),
                Toast.LENGTH_SHORT).show();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
