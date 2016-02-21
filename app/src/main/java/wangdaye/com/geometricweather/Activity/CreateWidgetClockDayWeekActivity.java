package wangdaye.com.geometricweather.Activity;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.WallpaperManager;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Message;
import android.provider.AlarmClock;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.RemoteViews;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.location.Poi;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import wangdaye.com.geometricweather.Data.GsonResult;
import wangdaye.com.geometricweather.Data.JuheWeather;
import wangdaye.com.geometricweather.Data.Location;
import wangdaye.com.geometricweather.Data.MyDatabaseHelper;
import wangdaye.com.geometricweather.R;
import wangdaye.com.geometricweather.Receiver.WidgetProviderClockDayWeek;
import wangdaye.com.geometricweather.Widget.HandlerContainer;
import wangdaye.com.geometricweather.Widget.SafeHandler;

/**
 * Created by WangDaYe on 2016/2/16.
 */

public class CreateWidgetClockDayWeekActivity extends Activity
        implements HandlerContainer {
    // widget
    private ImageView imageViewCard;

    private TextClock clock;
    private TextView dateText;
    private TextView weatherText;
    private TextView[] week;
    private TextView[] temp;

    //data
    private List<Location> locationList;
    private String locationName;
    private boolean showCard = false;

    private MyDatabaseHelper databaseHelper;

    private GsonResult gsonResult;

    private final int REFRESH_DATA_SUCCEED = 1;
    private final int REFRESH_DATA_FAILED = 0;

    // baidu location
    public LocationClient mLocationClient = null;
    public BDLocationListener myListener = new MyLocationListener();

    // handler
    private SafeHandler<CreateWidgetClockDayWeekActivity> safeHandler;

    //TAG
//    private final String TAG = "CreateWidgetClockDayWeekActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        setContentView(R.layout.activity_create_widget_clock_day_week);
    }

    @Override
    protected void onStart() {
        super.onStart();

        this.safeHandler = new SafeHandler<>(this);

        this.locationName = getString(R.string.local);

        ImageView imageViewWall = (ImageView) this.findViewById(R.id.create_widget_clock_day_week_wall);
        imageViewWall.setImageDrawable(WallpaperManager.getInstance(CreateWidgetClockDayWeekActivity.this).getDrawable());

        RelativeLayout relativeLayoutWidgetContainer = (RelativeLayout) this.findViewById(R.id.widget_clock_day_week) ;
        this.imageViewCard = (ImageView) relativeLayoutWidgetContainer.findViewById(R.id.widget_clock_day_week_card);

        this.clock = (TextClock) findViewById(R.id.widget_clock_day_week_clock);
        this.dateText = (TextView) findViewById(R.id.widget_clock_day_week_date);
        this.weatherText = (TextView) findViewById(R.id.widget_clock_day_week_weather);

        week = new TextView[4];
        week[0] = (TextView) findViewById(R.id.widget_clock_day_week_week_1);
        week[1] = (TextView) findViewById(R.id.widget_clock_day_week_week_2);
        week[2] = (TextView) findViewById(R.id.widget_clock_day_week_week_3);
        week[3] = (TextView) findViewById(R.id.widget_clock_day_week_week_4);

        temp = new TextView[4];
        temp[0] = (TextView) findViewById(R.id.widget_clock_day_week_temp_1);
        temp[1] = (TextView) findViewById(R.id.widget_clock_day_week_temp_2);
        temp[2] = (TextView) findViewById(R.id.widget_clock_day_week_temp_3);
        temp[3] = (TextView) findViewById(R.id.widget_clock_day_week_temp_4);

        this.initDatabaseHelper();
        this.readLocation();
        if (locationList.size() < 1) {
            locationList.add(new Location(getString(R.string.local)));
        }
        String[] items = new String[locationList.size()];
        for (int i = 0; i < locationList.size(); i ++) {
            items[i] = this.locationList.get(i).location;
        }
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, R.layout.spinner_text, items);
        spinnerAdapter.setDropDownViewResource(R.layout.spinner_text);
        Spinner spinnerCity = (Spinner) this.findViewById(R.id.create_widget_clock_day_week_spinner);
        spinnerCity.setAdapter(spinnerAdapter);
        spinnerCity.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                locationName = parent.getItemAtPosition(position).toString();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                locationName = getString(R.string.local);
            }
        });

        Switch switchCard = (Switch) this.findViewById(R.id.create_widget_clock_day_week_switch_card);
        switchCard.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    imageViewCard.setVisibility(View.VISIBLE);
                    showCard = true;
                    clock.setTextColor(ContextCompat.getColor(CreateWidgetClockDayWeekActivity.this, R.color.colorTextDark));
                    dateText.setTextColor(ContextCompat.getColor(CreateWidgetClockDayWeekActivity.this, R.color.colorTextDark));
                    weatherText.setTextColor(ContextCompat.getColor(CreateWidgetClockDayWeekActivity.this, R.color.colorTextDark));
                    for (int i = 0; i < 4; i ++) {
                        week[i].setTextColor(ContextCompat.getColor(CreateWidgetClockDayWeekActivity.this, R.color.colorTextDark));
                        temp[i].setTextColor(ContextCompat.getColor(CreateWidgetClockDayWeekActivity.this, R.color.colorTextDark));
                    }
                } else {
                    imageViewCard.setVisibility(View.GONE);
                    showCard = false;
                    clock.setTextColor(ContextCompat.getColor(CreateWidgetClockDayWeekActivity.this, R.color.colorTextLight));
                    dateText.setTextColor(ContextCompat.getColor(CreateWidgetClockDayWeekActivity.this, R.color.colorTextLight));
                    weatherText.setTextColor(ContextCompat.getColor(CreateWidgetClockDayWeekActivity.this, R.color.colorTextLight));
                    for (int i = 0; i < 4; i ++) {
                        week[i].setTextColor(ContextCompat.getColor(CreateWidgetClockDayWeekActivity.this, R.color.colorTextLight));
                        temp[i].setTextColor(ContextCompat.getColor(CreateWidgetClockDayWeekActivity.this, R.color.colorTextLight));
                    }
                }
            }
        });

        final Button buttonDone = (Button) this.findViewById(R.id.create_widget_clock_day_week_done);
        buttonDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences.Editor editor = getSharedPreferences(
                        getString(R.string.sp_widget_clock_day_week_setting),
                        MODE_PRIVATE
                ).edit();
                editor.putString(getString(R.string.key_location), locationName);
                editor.putBoolean(getString(R.string.key_show_card), showCard);
                editor.apply();

                Intent intent = getIntent();
                Bundle extras = intent.getExtras();
                int appWidgetId = 0;
                if (extras != null) {
                    appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
                            AppWidgetManager.INVALID_APPWIDGET_ID);
                }

                buttonDone.setText(getString(R.string.first_refresh_widget));
                buttonDone.setEnabled(true);

                refreshUIFromLocalData();
                refreshWidget();

                Intent resultValue = new Intent();
                resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                setResult(RESULT_OK, resultValue);
                finish();
            }
        });
    }

    private void initDatabaseHelper() {
        this.databaseHelper = new MyDatabaseHelper(this,
                MyDatabaseHelper.DATABASE_NAME,
                null,
                1);
    }

    private void readLocation() {
        this.locationList = new ArrayList<>();
        SQLiteDatabase database = databaseHelper.getWritableDatabase();
        Cursor cursor = database.query(MyDatabaseHelper.TABLE_LOCATION,
                null, null, null, null, null, null);

        if(cursor.moveToFirst()) {
            do {
                String location = cursor.getString(cursor.getColumnIndex(MyDatabaseHelper.COLUMN_LOCATION));
                this.locationList.add(new Location(location));
            } while (cursor.moveToNext());
        }
        cursor.close();
        database.close();
    }

    private void refreshWidget() {
        if(this.locationName.equals(getString(R.string.local))) {
            mLocationClient = new LocationClient(this); // 声明LocationClient类
            mLocationClient.registerLocationListener( myListener ); // 注册监听函数

            this.initBaiduMap();
        } else {
            getWeather(locationName);
        }
    }

    private void getWeather(final String searchLocation) {
        Thread thread=new Thread(new Runnable()
        {
            @Override
            public void run()
            { // TODO Auto-generated method stub
                gsonResult = JuheWeather.getRequest(searchLocation);
                Message message=new Message();
                if (gsonResult == null) {
                    message.what = REFRESH_DATA_FAILED;
                } else {
                    message.what = REFRESH_DATA_SUCCEED;
                }
                safeHandler.sendMessage(message);
            }
        });
        thread.start();
    }

    private void initBaiduMap() {
        // initialize baidu location
        mLocationClient.registerLocationListener( myListener );    //注册监听函数
        LocationClientOption option = new LocationClientOption();
        option.setLocationMode(LocationClientOption.LocationMode.Battery_Saving);//可选，默认高精度，设置定位模式，高精度，低功耗，仅设备
        option.setCoorType("bd09ll");//可选，默认gcj02，设置返回的定位结果坐标系
        option.setScanSpan(0);//可选，默认0，即仅定位一次，设置发起定位请求的间隔需要大于等于1000ms才是有效的
        option.setIsNeedAddress(true);//可选，设置是否需要地址信息，默认不需要
        option.setOpenGps(true);//可选，默认false,设置是否使用gps
        option.setLocationNotify(true);//可选，默认false，设置是否当gps有效时按照1S1次频率输出GPS结果
        option.setIsNeedLocationDescribe(true);//可选，默认false，设置是否需要位置语义化结果，可以在BDLocation.getLocationDescribe里得到，结果类似于“在北京天安门附近”
        option.setIsNeedLocationPoiList(true);//可选，默认false，设置是否需要POI结果，可以在BDLocation.getPoiList里得到
        option.setIgnoreKillProcess(false);//可选，默认false，定位SDK内部是一个SERVICE，并放到了独立进程，设置是否在stop的时候杀死这个进程，默认杀死
        option.SetIgnoreCacheException(false);//可选，默认false，设置是否收集CRASH信息，默认收集
        option.setEnableSimulateGps(false);//可选，默认false，设置是否需要过滤gps仿真结果，默认需要
        mLocationClient.setLocOption(option);
        mLocationClient.start();
    }

    private void refreshUI() {
        if(this.gsonResult != null) {
            this.refreshUIFromInternet();
        } else {
            Toast.makeText(this, getString(R.string.refresh_widget_error), Toast.LENGTH_SHORT).show();
        }
    }

    private void refreshUIFromInternet() {
        boolean isDay;
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (5 < hour && hour < 19) {
            isDay = true;
        } else {
            isDay = false;
        }
        RemoteViews views = new RemoteViews(this.getPackageName(), R.layout.widget_clock_day_week);

        GsonResult.WeatherNow weatherNow = this.gsonResult.result.data.realtime.weatherNow;
        String weatherKindToday = JuheWeather.getWeatherKind(weatherNow.weatherInfo);
        int[] imageId = JuheWeather.getWeatherIcon(weatherKindToday, isDay);
        views.setImageViewResource(R.id.widget_clock_day_week_image, imageId[3]);
        String[] solar = this.gsonResult.result.data.realtime.date.split("-");
        String dateText = solar[1] + "-" + solar[2]
                + " " + getString(R.string.week) + this.gsonResult.result.data.weather.get(0).week
                + " / "
                + this.gsonResult.result.data.realtime.moon;
        views.setTextViewText(R.id.widget_clock_day_week_date, dateText);
        String weatherText = this.gsonResult.result.data.realtime.city_name
                + " / "
                + weatherNow.weatherInfo + " " + weatherNow.temperature + "℃";
        views.setTextViewText(R.id.widget_clock_day_week_weather, weatherText);

        List<GsonResult.Weather> weather = gsonResult.result.data.weather;
        // icon
        if (isDay) {
            imageId = JuheWeather.getWeatherIcon(JuheWeather.getWeatherKind(weather.get(1).info.day.get(1)), true);
        } else {
            imageId = JuheWeather.getWeatherIcon(JuheWeather.getWeatherKind(weather.get(1).info.night.get(1)), false);
        }
        views.setImageViewResource(R.id.widget_clock_day_week_image_1, imageId[3]);
        // 2
        if (isDay) {
            imageId = JuheWeather.getWeatherIcon(JuheWeather.getWeatherKind(weather.get(2).info.day.get(1)), true);
        } else {
            imageId = JuheWeather.getWeatherIcon(JuheWeather.getWeatherKind(weather.get(2).info.night.get(1)), false);
        }
        views.setImageViewResource(R.id.widget_clock_day_week_image_2, imageId[3]);
        // 3
        if (isDay) {
            imageId = JuheWeather.getWeatherIcon(JuheWeather.getWeatherKind(weather.get(3).info.day.get(1)), true);
        } else {
            imageId = JuheWeather.getWeatherIcon(JuheWeather.getWeatherKind(weather.get(3).info.night.get(1)), false);
        }
        views.setImageViewResource(R.id.widget_clock_day_week_image_3, imageId[3]);
        // 4
        if (isDay) {
            imageId = JuheWeather.getWeatherIcon(JuheWeather.getWeatherKind(weather.get(4).info.day.get(1)), true);
        } else {
            imageId = JuheWeather.getWeatherIcon(JuheWeather.getWeatherKind(weather.get(4).info.night.get(1)), false);
        }
        views.setImageViewResource(R.id.widget_clock_day_week_image_4, imageId[3]);
        // temperature
        String temp;
        // 1
        temp = weather.get(1).info.night.get(2)
                + "/"
                + weather.get(1).info.day.get(2)
                + "°";
        views.setTextViewText(R.id.widget_clock_day_week_temp_1, temp);
        // 2
        temp = weather.get(2).info.night.get(2)
                + "/"
                + weather.get(2).info.day.get(2)
                + "°";
        views.setTextViewText(R.id.widget_clock_day_week_temp_2, temp);
        // 3
        temp = weather.get(3).info.night.get(2)
                + "/"
                + weather.get(3).info.day.get(2)
                + "°";
        views.setTextViewText(R.id.widget_clock_day_week_temp_3, temp);
        // 4
        temp = weather.get(4).info.night.get(2)
                + "/"
                + weather.get(4).info.day.get(2)
                + "°";
        views.setTextViewText(R.id.widget_clock_day_week_temp_4, temp);
        // week
        String week;
        // 1
        week = getString(R.string.week) + weather.get(1).week;
        views.setTextViewText(R.id.widget_clock_day_week_week_1, week);
        // 2
        week = getString(R.string.week) + weather.get(2).week;
        views.setTextViewText(R.id.widget_clock_day_week_week_2, week);
        // 3
        week = getString(R.string.week) + weather.get(3).week;
        views.setTextViewText(R.id.widget_clock_day_week_week_3, week);
        // 4
        week = getString(R.string.week) + weather.get(4).week;
        views.setTextViewText(R.id.widget_clock_day_week_week_4, week);

        if(this.showCard) { // show card
            views.setViewVisibility(R.id.widget_clock_day_week_card, View.VISIBLE);
            views.setTextColor(R.id.widget_clock_day_week_clock, ContextCompat.getColor(this, R.color.colorTextDark));
            views.setTextColor(R.id.widget_clock_day_week_date, ContextCompat.getColor(this, R.color.colorTextDark));
            views.setTextColor(R.id.widget_clock_day_week_weather, ContextCompat.getColor(this, R.color.colorTextDark));
            views.setTextColor(R.id.widget_clock_day_week_week_1, ContextCompat.getColor(this, R.color.colorTextDark));
            views.setTextColor(R.id.widget_clock_day_week_week_2, ContextCompat.getColor(this, R.color.colorTextDark));
            views.setTextColor(R.id.widget_clock_day_week_week_3, ContextCompat.getColor(this, R.color.colorTextDark));
            views.setTextColor(R.id.widget_clock_day_week_week_4, ContextCompat.getColor(this, R.color.colorTextDark));
            views.setTextColor(R.id.widget_clock_day_week_temp_1, ContextCompat.getColor(this, R.color.colorTextDark));
            views.setTextColor(R.id.widget_clock_day_week_temp_2, ContextCompat.getColor(this, R.color.colorTextDark));
            views.setTextColor(R.id.widget_clock_day_week_temp_3, ContextCompat.getColor(this, R.color.colorTextDark));
            views.setTextColor(R.id.widget_clock_day_week_temp_4, ContextCompat.getColor(this, R.color.colorTextDark));
        } else { // do not show card
            views.setViewVisibility(R.id.widget_clock_day_week_card, View.GONE);
            views.setTextColor(R.id.widget_clock_day_week_clock, ContextCompat.getColor(this, R.color.colorTextLight));
            views.setTextColor(R.id.widget_clock_day_week_date, ContextCompat.getColor(this, R.color.colorTextLight));
            views.setTextColor(R.id.widget_clock_day_week_weather, ContextCompat.getColor(this, R.color.colorTextLight));
            views.setTextColor(R.id.widget_clock_day_week_week_1, ContextCompat.getColor(this, R.color.colorTextLight));
            views.setTextColor(R.id.widget_clock_day_week_week_2, ContextCompat.getColor(this, R.color.colorTextLight));
            views.setTextColor(R.id.widget_clock_day_week_week_3, ContextCompat.getColor(this, R.color.colorTextLight));
            views.setTextColor(R.id.widget_clock_day_week_week_4, ContextCompat.getColor(this, R.color.colorTextLight));
            views.setTextColor(R.id.widget_clock_day_week_temp_1, ContextCompat.getColor(this, R.color.colorTextLight));
            views.setTextColor(R.id.widget_clock_day_week_temp_2, ContextCompat.getColor(this, R.color.colorTextLight));
            views.setTextColor(R.id.widget_clock_day_week_temp_3, ContextCompat.getColor(this, R.color.colorTextLight));
            views.setTextColor(R.id.widget_clock_day_week_temp_4, ContextCompat.getColor(this, R.color.colorTextLight));
        }

        Intent intentClock = new Intent(AlarmClock.ACTION_SHOW_ALARMS);
        PendingIntent pendingIntentClock = PendingIntent.getActivity(this, 0, intentClock, 0);
        views.setOnClickPendingIntent(R.id.widget_clock_day_week_clock_button, pendingIntentClock);

        Intent intentWeather = new Intent(this, MainActivity.class);
        PendingIntent pendingIntentWeather = PendingIntent.getActivity(this, 0, intentWeather, 0);
        views.setOnClickPendingIntent(R.id.widget_clock_day_week_weather_button, pendingIntentWeather);

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        appWidgetManager.updateAppWidget(new ComponentName(this, WidgetProviderClockDayWeek.class), views);

        SharedPreferences.Editor editor = getSharedPreferences(
                getString(R.string.sp_widget_clock_day_setting), Context.MODE_PRIVATE).edit();
        editor.putBoolean(getString(R.string.key_saved_data), true);
        editor.putString(getString(R.string.key_weather_kind_today), weatherKindToday);
        editor.putString(getString(R.string.key_city_time), dateText);
        editor.putString(getString(R.string.key_weather_today), weatherText);
        editor.putString(getString(R.string.key_week_2), getString(R.string.week) + weather.get(1).week);
        editor.putString(getString(R.string.key_week_3), getString(R.string.week) + weather.get(2).week);
        editor.putString(getString(R.string.key_week_4), getString(R.string.week) + weather.get(3).week);
        editor.putString(getString(R.string.key_week_5), getString(R.string.week) + weather.get(4).week);
        if (isDay) {
            editor.putString(getString(R.string.key_weather_2), JuheWeather.getWeatherKind(weather.get(1).info.day.get(1)));
            editor.putString(getString(R.string.key_weather_3), JuheWeather.getWeatherKind(weather.get(2).info.day.get(1)));
            editor.putString(getString(R.string.key_weather_4), JuheWeather.getWeatherKind(weather.get(3).info.day.get(1)));
            editor.putString(getString(R.string.key_weather_5), JuheWeather.getWeatherKind(weather.get(4).info.day.get(1)));
        } else {
            editor.putString(getString(R.string.key_weather_2), JuheWeather.getWeatherKind(weather.get(1).info.night.get(1)));
            editor.putString(getString(R.string.key_weather_3), JuheWeather.getWeatherKind(weather.get(2).info.night.get(1)));
            editor.putString(getString(R.string.key_weather_4), JuheWeather.getWeatherKind(weather.get(3).info.night.get(1)));
            editor.putString(getString(R.string.key_weather_5), JuheWeather.getWeatherKind(weather.get(4).info.night.get(1)));
        }
        editor.putString(getString(R.string.key_temperature_2), weather.get(1).info.night.get(2)
                + "/"
                + weather.get(1).info.day.get(2)
                + "°");
        editor.putString(getString(R.string.key_temperature_3), weather.get(2).info.night.get(2)
                + "/"
                + weather.get(2).info.day.get(2)
                + "°");
        editor.putString(getString(R.string.key_temperature_4), weather.get(3).info.night.get(2)
                + "/"
                + weather.get(3).info.day.get(2)
                + "°");
        editor.putString(getString(R.string.key_temperature_5), weather.get(4).info.night.get(2)
                + "/"
                + weather.get(4).info.day.get(2)
                + "°");
        editor.apply();
    }

    private void refreshUIFromLocalData() {
        SharedPreferences sharedPreferences = this.getSharedPreferences(
                getString(R.string.sp_widget_clock_day_week_setting), Context.MODE_PRIVATE);
        if (! sharedPreferences.getBoolean(getString(R.string.key_saved_data), false)) {
            return;
        }
        String weatherKindToday = sharedPreferences.getString(getString(R.string.key_weather_kind_today), "阴");
        String weatherText = sharedPreferences.getString(getString(R.string.key_weather_today), getString(R.string.ellipsis));
        String dateText = sharedPreferences.getString(getString(R.string.key_city_time), getString(R.string.wait_refresh));
        String[] week = new String[4];
        week[0] = sharedPreferences.getString(getString(R.string.key_week_2), getString(R.string.ellipsis));
        week[1] = sharedPreferences.getString(getString(R.string.key_week_3), getString(R.string.ellipsis));
        week[2] = sharedPreferences.getString(getString(R.string.key_week_4), getString(R.string.ellipsis));
        week[3] = sharedPreferences.getString(getString(R.string.key_week_5), getString(R.string.ellipsis));
        String[] weatherKind = new String[4];
        weatherKind[0] = sharedPreferences.getString(getString(R.string.key_weather_2), "阴");
        weatherKind[1] = sharedPreferences.getString(getString(R.string.key_weather_3), "阴");
        weatherKind[2] = sharedPreferences.getString(getString(R.string.key_weather_4), "阴");
        weatherKind[3] = sharedPreferences.getString(getString(R.string.key_weather_5), "阴");
        String[] temp = new String[4];
        temp[0] = sharedPreferences.getString(getString(R.string.key_temperature_2), getString(R.string.ellipsis));
        temp[1] = sharedPreferences.getString(getString(R.string.key_temperature_3), getString(R.string.ellipsis));
        temp[2] = sharedPreferences.getString(getString(R.string.key_temperature_4), getString(R.string.ellipsis));
        temp[3] = sharedPreferences.getString(getString(R.string.key_temperature_5), getString(R.string.ellipsis));

        boolean isDay;
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (5 < hour && hour < 19) {
            isDay = true;
        } else {
            isDay = false;
        }

        RemoteViews views = new RemoteViews(this.getPackageName(), R.layout.widget_clock_day);
        int[] imageId = JuheWeather.getWeatherIcon(weatherKindToday, isDay);
        views.setImageViewResource(R.id.widget_clock_day_week_image, imageId[3]);
        views.setTextViewText(R.id.widget_clock_day_week_date, dateText);
        views.setTextViewText(R.id.widget_clock_day_week_weather, weatherText);

        views.setTextViewText(R.id.widget_clock_day_week_week_1, week[0]);
        views.setTextViewText(R.id.widget_clock_day_week_week_2, week[1]);
        views.setTextViewText(R.id.widget_clock_day_week_week_3, week[2]);
        views.setTextViewText(R.id.widget_clock_day_week_week_4, week[3]);

        views.setTextViewText(R.id.widget_clock_day_week_temp_1, temp[0]);
        views.setTextViewText(R.id.widget_clock_day_week_temp_2, temp[1]);
        views.setTextViewText(R.id.widget_clock_day_week_temp_3, temp[2]);
        views.setTextViewText(R.id.widget_clock_day_week_temp_4, temp[3]);

        imageId = JuheWeather.getWeatherIcon(weatherKind[0], isDay);
        views.setImageViewResource(R.id.widget_clock_day_week_image_1, imageId[3]);
        imageId = JuheWeather.getWeatherIcon(weatherKind[1], isDay);
        views.setImageViewResource(R.id.widget_clock_day_week_image_2, imageId[3]);
        imageId = JuheWeather.getWeatherIcon(weatherKind[2], isDay);
        views.setImageViewResource(R.id.widget_clock_day_week_image_3, imageId[3]);
        imageId = JuheWeather.getWeatherIcon(weatherKind[3], isDay);
        views.setImageViewResource(R.id.widget_clock_day_week_image_4, imageId[3]);

        if(this.showCard) { // show card
            views.setViewVisibility(R.id.widget_clock_day_week_card, View.VISIBLE);
            views.setTextColor(R.id.widget_clock_day_week_clock, ContextCompat.getColor(this, R.color.colorTextDark));
            views.setTextColor(R.id.widget_clock_day_week_date, ContextCompat.getColor(this, R.color.colorTextDark));
            views.setTextColor(R.id.widget_clock_day_week_weather, ContextCompat.getColor(this, R.color.colorTextDark));
            views.setTextColor(R.id.widget_clock_day_week_week_1, ContextCompat.getColor(this, R.color.colorTextDark));
            views.setTextColor(R.id.widget_clock_day_week_week_2, ContextCompat.getColor(this, R.color.colorTextDark));
            views.setTextColor(R.id.widget_clock_day_week_week_3, ContextCompat.getColor(this, R.color.colorTextDark));
            views.setTextColor(R.id.widget_clock_day_week_week_4, ContextCompat.getColor(this, R.color.colorTextDark));
            views.setTextColor(R.id.widget_clock_day_week_temp_1, ContextCompat.getColor(this, R.color.colorTextDark));
            views.setTextColor(R.id.widget_clock_day_week_temp_2, ContextCompat.getColor(this, R.color.colorTextDark));
            views.setTextColor(R.id.widget_clock_day_week_temp_3, ContextCompat.getColor(this, R.color.colorTextDark));
            views.setTextColor(R.id.widget_clock_day_week_temp_4, ContextCompat.getColor(this, R.color.colorTextDark));
        } else { // do not show card
            views.setViewVisibility(R.id.widget_clock_day_week_card, View.GONE);
            views.setTextColor(R.id.widget_clock_day_week_clock, ContextCompat.getColor(this, R.color.colorTextLight));
            views.setTextColor(R.id.widget_clock_day_week_date, ContextCompat.getColor(this, R.color.colorTextLight));
            views.setTextColor(R.id.widget_clock_day_week_weather, ContextCompat.getColor(this, R.color.colorTextLight));
            views.setTextColor(R.id.widget_clock_day_week_week_1, ContextCompat.getColor(this, R.color.colorTextLight));
            views.setTextColor(R.id.widget_clock_day_week_week_2, ContextCompat.getColor(this, R.color.colorTextLight));
            views.setTextColor(R.id.widget_clock_day_week_week_3, ContextCompat.getColor(this, R.color.colorTextLight));
            views.setTextColor(R.id.widget_clock_day_week_week_4, ContextCompat.getColor(this, R.color.colorTextLight));
            views.setTextColor(R.id.widget_clock_day_week_temp_1, ContextCompat.getColor(this, R.color.colorTextLight));
            views.setTextColor(R.id.widget_clock_day_week_temp_2, ContextCompat.getColor(this, R.color.colorTextLight));
            views.setTextColor(R.id.widget_clock_day_week_temp_3, ContextCompat.getColor(this, R.color.colorTextLight));
            views.setTextColor(R.id.widget_clock_day_week_temp_4, ContextCompat.getColor(this, R.color.colorTextLight));
        }

        Intent intentClock = new Intent(AlarmClock.ACTION_SHOW_ALARMS);
        PendingIntent pendingIntentClock = PendingIntent.getActivity(this, 0, intentClock, 0);
        views.setOnClickPendingIntent(R.id.widget_clock_day_week_clock_button, pendingIntentClock);

        Intent intentWeather = new Intent(this, MainActivity.class);
        PendingIntent pendingIntentWeather = PendingIntent.getActivity(this, 0, intentWeather, 0);
        views.setOnClickPendingIntent(R.id.widget_clock_day_week_weather_button, pendingIntentWeather);

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        appWidgetManager.updateAppWidget(new ComponentName(this, WidgetProviderClockDayWeek.class), views);
    }

    // inner class
    private class MyLocationListener implements BDLocationListener {
        // baidu location listener
        @Override
        public void onReceiveLocation(BDLocation location) {
            //Receive Location
            String locationName = null;

            StringBuffer sb = new StringBuffer(256);
            sb.append("time : ");
            sb.append(location.getTime());
            if (location.getLocType() == BDLocation.TypeGpsLocation){// GPS定位结果
                sb.append("\naddr : ");
                sb.append(location.getAddrStr());
                sb.append("\ndescribe : ");
                sb.append("gps定位成功");
                locationName = location.getCity();
            } else if (location.getLocType() == BDLocation.TypeNetWorkLocation){// 网络定位结果
                sb.append("\naddr : ");
                sb.append(location.getAddrStr());
                sb.append("\ndescribe : ");
                sb.append("网络定位成功");
                locationName = location.getCity();
            } else if (location.getLocType() == BDLocation.TypeOffLineLocation) {// 离线定位结果
                sb.append("\ndescribe : ");
                sb.append("离线定位成功，离线定位结果也是有效的");
                locationName = location.getCity();
            } else if (location.getLocType() == BDLocation.TypeServerError) {
                sb.append("\ndescribe : ");
                sb.append("服务端网络定位失败，可以反馈IMEI号和大体定位时间到loc-bugs@baidu.com，会有人追查原因");
            } else if (location.getLocType() == BDLocation.TypeNetWorkException) {
                sb.append("\ndescribe : ");
                sb.append("网络不同导致定位失败，请检查网络是否通畅");
            } else if (location.getLocType() == BDLocation.TypeCriteriaException) {
                sb.append("\ndescribe : ");
                sb.append("无法获取有效定位依据导致定位失败，一般是由于手机的原因，处于飞行模式下一般会造成这种结果，可以试着重启手机");
            }

            getWeather(locationName);

            sb.append("\nlocationdescribe : ");
            sb.append(location.getLocationDescribe());// 位置语义化信息
            List<Poi> list = location.getPoiList();// POI数据
            if (list != null) {
                sb.append("\npoilist size = : ");
                sb.append(list.size());
                for (Poi p : list) {
                    sb.append("\npoi= : ");
                    sb.append(p.getId() + " " + p.getName() + " " + p.getRank());
                }
            }
            Log.i("BaiduLocationApiDem", sb.toString());
            mLocationClient.stop();
        }
    }

    @Override
    public void handleMessage(Message message) {
        switch(message.what)
        {
            case REFRESH_DATA_SUCCEED:
                refreshUI();
                break;
            default:
                Toast.makeText(this,
                        getString(R.string.refresh_widget_error),
                        Toast.LENGTH_SHORT).show();
                break;
        }
    }
}