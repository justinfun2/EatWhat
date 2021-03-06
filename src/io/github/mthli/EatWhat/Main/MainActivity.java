package io.github.mthli.EatWhat.Main;

import android.app.ActionBar;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;

import io.github.mthli.EatWhat.About.AboutActivity;
import io.github.mthli.EatWhat.Database.RDBAction;
import io.github.mthli.EatWhat.Database.Restaurant;
import io.github.mthli.EatWhat.Database.UDBAction;
import io.github.mthli.EatWhat.Database.Usual;
import io.github.mthli.EatWhat.Usual.UsualActivity;
import io.github.mthli.EatWhat.R;

import java.io.*;
import java.sql.SQLException;
import java.util.List;
import java.util.Random;

public class MainActivity extends Activity implements SensorEventListener, ActionBar.OnNavigationListener {
    private ImageView background;
    private Bitmap bitmap;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    private SensorManager sensorManager;
    private SoundPool soundPool;
    private int soundID;

    private PopupWindow popupWindow;
    private View popupView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        String DB_PATH = MainActivity.this.getFilesDir().getParent() + "/databases/";
        String DB_NAME = "restaurant.db";
        if (!(new File(DB_PATH + DB_NAME)).exists()) {
            File file = new File(DB_PATH);
            if (!file.exists()) {
                file.mkdir();
            }
            try {
                InputStream inputStream = getBaseContext().getAssets().open(DB_NAME);
                OutputStream outputStream = new FileOutputStream(DB_PATH + DB_NAME);
                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, length);
                }
                outputStream.flush();
                outputStream.close();
                inputStream.close();
            } catch (Exception e) {
                Toast.makeText(
                        MainActivity.this,
                        getString(R.string.error_database_copy),
                        Toast.LENGTH_SHORT
                ).show();
            }
        }

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        actionBar.setListNavigationCallbacks(
                new ArrayAdapter<String>(
                        MainActivity.this,
                        android.R.layout.simple_list_item_1,
                        android.R.id.text1,
                        new String[] {
                                getString(R.string.main_dropdown_random),
                                getString(R.string.main_dropdown_usual)
                        }
                ),
                this
        );

        sharedPreferences = getSharedPreferences("setting", MODE_PRIVATE);
        editor = sharedPreferences.edit();

        String path = sharedPreferences.getString("bitmap_background", null);
        background = (ImageView) findViewById(R.id.main_background_image);
        if (path != null) {
            Uri uri = Uri.parse(path);
            background.setImageURI(uri);
        } else {
            background.setImageResource(R.drawable.main_background);
        }

        popupView = getLayoutInflater().inflate(R.layout.popup, null);
        popupWindow = new PopupWindow(popupView, WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        /* Do something */
        soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 5);
        soundID = soundPool.load(this, R.raw.dang, 1);
    }

    @Override
    public boolean onNavigationItemSelected(int position, long id) {
        editor.putInt("dropdown", position);
        editor.commit();
        return true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(
                this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL
        );
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        /* Do nothing */
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        int sensorType = sensorEvent.sensor.getType();
        float[] values= sensorEvent.values;
        RDBAction rdbAction = new RDBAction(MainActivity.this);
        UDBAction udbAction = new UDBAction(MainActivity.this);

        if (sensorType == Sensor.TYPE_ACCELEROMETER) {
            if ((Math.abs(values[0]) > 19) || Math.abs(values[1]) > 19 || Math.abs(values[2]) > 19){
                if (popupWindow.isShowing()) {
                    popupWindow.dismiss();
                }

                int position = sharedPreferences.getInt("dropdown", 0);

                if (position == 0) {
                    try {
                        rdbAction.openDatabase();
                        List<Restaurant> restaurantList = rdbAction.restaurantList();
                        if (restaurantList.size() == 0) {
                            Toast.makeText(
                                    MainActivity.this,
                                    getString(R.string.error_database_open),
                                    Toast.LENGTH_SHORT
                            ).show();
                        } else {
                            Random random = new Random();
                            int getRandom = random.nextInt(restaurantList.size());
                            Restaurant restaurant = restaurantList.get(getRandom);
                            TextView textView = (TextView) popupView.findViewById(R.id.popup_restaurant);
                            textView.setText(restaurant.getRestaurant());
                            textView = (TextView) popupView.findViewById(R.id.popup_path);
                            textView.setText(restaurant.getPath());
                            popupWindow.setAnimationStyle(R.style.popup_show);
                            soundPool.play(soundID, 1, 1, 0, 0, 1);
                            popupWindow.showAtLocation(background, Gravity.CENTER, 0, 200);
                        }
                    } catch (SQLException s) {
                        Toast.makeText(
                                MainActivity.this,
                                getString(R.string.error_database_open),
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                } else {
                    try {
                        udbAction.openDatabase(false);
                        List<Usual> usualList = udbAction.usualList();
                        if (usualList.size() == 0) {
                            Toast.makeText(
                                    MainActivity.this,
                                    getString(R.string.warning_usual_404),
                                    Toast.LENGTH_SHORT
                            ).show();
                        } else {
                            Random random = new Random();
                            int getRandom = random.nextInt(usualList.size());
                            Usual usual = usualList.get(getRandom);
                            TextView textView = (TextView) popupView.findViewById(R.id.popup_restaurant);
                            textView.setText(usual.getRestaurant());
                            textView = (TextView) popupView.findViewById(R.id.popup_path);
                            textView.setText(usual.getPath());
                            popupWindow.setAnimationStyle(R.style.popup_show);
                            soundPool.play(soundID, 1, 1, 0, 0, 1);
                            popupWindow.showAtLocation(background, Gravity.CENTER, 0, 200);
                        }
                    } catch (SQLException s) {
                        Toast.makeText(
                                MainActivity.this,
                                getString(R.string.error_database_open),
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                    udbAction.closeDatabase();
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.main_menu_share:
                /*
                 * Need check share information is existing,
                 * and what app should share to, tencent, renren, and other?
                 */
                Intent intent_share = new Intent(Intent.ACTION_SEND);
                intent_share.setType("text/plain");
                intent_share.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_title));
                /* Do something */
                TextView textView = (TextView) popupView.findViewById(R.id.popup_restaurant);
                CharSequence getRestaurant = textView.getText();
                textView = (TextView) popupView.findViewById(R.id.popup_path);
                CharSequence getPath = textView.getText();
                if (getRestaurant.length() == 0 && getPath.length() == 0) {
                    Toast.makeText(
                            MainActivity.this,
                            getString(R.string.warning_share_void),
                            Toast.LENGTH_SHORT
                    ).show();
                    break;
                } else {
                    String share = getString(R.string.share_restaurant)
                            + getRestaurant
                            + getString(R.string.share_path)
                            + getPath + " " //Dirty
                            + getString(R.string.share_last);
                    intent_share.putExtra(Intent.EXTRA_TEXT, share);
                    startActivity(Intent.createChooser(intent_share, "Share"));
                    break;
                }
            case R.id.main_menu_background:
                Intent intent_background = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                intent_background.setType("image/*");
                intent_background.putExtra("return-data", true);
                startActivityForResult(intent_background, 1);
                break;
            case R.id.main_menu_usual:
                Intent intent_add_usual = new Intent(MainActivity.this, UsualActivity.class);
                startActivity(intent_add_usual);
                return true;
            case R.id.main_menu_about:
                Intent intent_about = new Intent(MainActivity.this, AboutActivity.class);
                startActivity(intent_about);
                return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == 1 && data != null) {
            Uri uri = data.getData();
            System.out.println(uri.getPath());
            ContentResolver contentResolver = this.getContentResolver();
            try {
                bitmap = BitmapFactory.decodeStream(contentResolver.openInputStream(uri));
                background.setImageBitmap(bitmap);
                editor.putString("bitmap_background", uri.toString());
                editor.commit();
            } catch (FileNotFoundException f) {
                Toast.makeText(
                        MainActivity.this,
                        getString(R.string.error_background_404),
                        Toast.LENGTH_SHORT
                ).show();
            }
        }
    }
}
