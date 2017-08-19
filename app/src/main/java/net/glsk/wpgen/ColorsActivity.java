/*
 * This file is part of WPGen.
 *
 * WPGen is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * WPGen is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with WPGen.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.glsk.wpgen;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.ActionMode;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Random;

public class ColorsActivity extends AppCompatActivity {

    private
    GridView gridview;
    ArrayList<String> favsList = new ArrayList<>();
    ArrayList<String> allList = new ArrayList<>();
    ArrayList<String> selectedList = new ArrayList<>();

    static final int NOISE_SPREAD = 6;
    static final int NOISE_BITMAP_SIZE = 64;

    static final int SET_GRADIENT = 1;
    static final int SET_PLASMA = 2;
    static final int SET_STRIPES = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_colors);
        // Load colors and refresh view.
        loadFavorites();
        updateColors();
        gridview = (GridView) findViewById(R.id.gridview);
        gridview.setAdapter(new ImageAdapter(this));
        gridview.setChoiceMode(GridView.CHOICE_MODE_MULTIPLE_MODAL);
        gridview.setMultiChoiceModeListener(new MultiChoiceModeListener());

        gridview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                // Color is clicked - set wallpaper.
                setColorWallpaper(allList.get(position));
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_color_list, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu (Menu menu) {
        MenuItem saveLastItem = menu.findItem(R.id.action_save_last);

        // Show Save Last menu if there's wallpaper to save.
        if (new File(this.getFilesDir(), "lastwlp.png").exists()) {
            saveLastItem.setEnabled(true);
        } else {
            saveLastItem.setEnabled(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks.
        switch (item.getItemId()) {
            case R.id.action_settings: // Not implemented yet.
                return true;
            case R.id.action_add_color: // Show add color dialog.
                AlertDialog.Builder addColorDialog = new AlertDialog.Builder(this);
                //addColorDialog.setTitle(R.string.dialog_add_color_title);
                addColorDialog.setMessage(R.string.dialog_add_color_msg);
                final EditText input = new EditText(this);
                addColorDialog.setView(input);
                addColorDialog.setPositiveButton(R.string.label_ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String value = input.getText().toString();
                        if (!value.equals("")) {
                            try {
                                Color.parseColor(value);
                                // Color is valid.
                                favsList.add(value.toUpperCase());
                                saveFavorites();
                                updateColors();
                                gridview.invalidateViews();
                                Toast.makeText(ColorsActivity.this, getString(R.string.toast_fav_added), Toast.LENGTH_SHORT).show();
                            } catch (IllegalArgumentException iae) {
                                // Color is invalid.
                                Toast.makeText(ColorsActivity.this, getString(R.string.toast_invalid_color), Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });
                addColorDialog.setNegativeButton(R.string.label_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Cancelled.
                    }
                });
                addColorDialog.show();
                return true;
            case R.id.action_save_last: // Save last wallpaper to a file.
                // Check for write permission.
                if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this,
                                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 123);
                } else {
                    // Permission was already granted.
                    saveLastWallpaper();
                }

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 123: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    saveLastWallpaper();
                } else {
                    Toast.makeText(ColorsActivity.this, getString(R.string.toast_permission_not_granted), Toast.LENGTH_SHORT).show();
                }
                return;
            }
        }
    }

    private void saveLastWallpaper() {
        File source = new File(this.getFilesDir(), "lastwlp.png");
        File picturesDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "WPGen");
        if (!picturesDir.exists()) {
            if (!picturesDir.mkdirs()) {
                Toast.makeText(ColorsActivity.this, getString(R.string.toast_cannot_mkdir), Toast.LENGTH_SHORT).show();
            }
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        String filename = "wpgen_" + sdf.format(new Date()) + ".png";
        File dest = new File(picturesDir, filename);
        try {
            copyFile(source, dest);
            sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(dest)));
            Toast.makeText(ColorsActivity.this, getString(R.string.toast_saved_to_pics), Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Set wallpaper to color.
    protected void setColorWallpaper(String color) {
        // Create small solid color bitmap.
        Bitmap bitmap = createBitmap(color, 512);
        WallpaperManager wpManager = WallpaperManager.getInstance(this.getApplicationContext());
        setBitmapAsWallpaper(wpManager, bitmap);
        String text = getString(R.string.toast_wallpaper_set_to_color);
        Toast toast = Toast.makeText(this, text, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }

    // Set wallpaper to gradient image.
    protected void setGradientWallpaper(ArrayList<String> colors) {
        WallpaperManager wpManager = WallpaperManager.getInstance(this.getApplicationContext());
        // Use full screen size so wallpaper is movable.
        int height = wpManager.getDesiredMinimumHeight();
        // Create square bitmap for wallpaper.
        Bitmap wallpaperBitmap = Bitmap.createBitmap(height, height, Bitmap.Config.ARGB_8888);
        // Prepare colors for gradient.
        int[] colorsInt = new int[colors.size()];
        for (int i = 0; i < colors.size(); i++) {
            colorsInt[i] = Color.parseColor(colors.get(i));
        }
        // Create gradient shader.
        Paint paint = new Paint();
        Shader gradientShader = new LinearGradient(0, 0, height, height, colorsInt, null, Shader.TileMode.CLAMP);
        Canvas c = new Canvas(wallpaperBitmap);
        paint.setShader(gradientShader);
        // Draw gradient on bitmap.
        c.drawRect(0, 0, height, height, paint);
        // Add noise.
        //addNoise(wallpaperBitmap);
        setBitmapAsWallpaper(wpManager, wallpaperBitmap);
        // Cleanup.
        wallpaperBitmap.recycle();
    }

    // Set wallpaper to plasma image.
    protected void setPlasmaWallpaper(ArrayList<String> colors) {
        WallpaperManager wpManager = WallpaperManager.getInstance(this.getApplicationContext());
        // Use half screen size for speed.
        int height = wpManager.getDesiredMinimumHeight()/4;
        int width = height;
        // Create wallpaper bitmap.
        Bitmap wallpaperBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        // Prepare colors for gradient.
        int[] colorsInt = new int[colors.size()];
        for (int i = 0; i < colors.size(); i++) {
            colorsInt[i] = Color.parseColor(colors.get(i));
        }
        // Create gradient to construct palette.
        Paint paint = new Paint();
        Bitmap gradientBitmap = Bitmap.createBitmap(256, 1, Bitmap.Config.ARGB_8888);
        Shader gradientShader = new LinearGradient(0, 0, 255, 0, colorsInt, null, Shader.TileMode.MIRROR);
        Canvas c = new Canvas(gradientBitmap);
        paint.setShader(gradientShader);
        // Draw gradient on bitmap.
        c.drawRect(0, 0, 256, 1, paint);
        int[] palette = new int[256];
        //
        for(int x = 0; x < 256; x++) {
            palette[x] = gradientBitmap.getPixel(x, 0);
        }
        // Cleanup.
        gradientBitmap.recycle();

        // Generate plasma.
        int[][] plasma = new int[width][height];
        Random random = new Random();
        // TODO: add n (and maybe spread) as parameter in Settings.
        double n = 1.3;  // Number of periods per wallpaper width.
        double period = width / (n * 2 * 3.14);
        double spread = period * 0.3;
        double period1 = period - spread + spread*random.nextFloat();
        double period2 = period - spread + spread*random.nextFloat();
        double period3 = period - spread + spread*random.nextFloat();
        for (int x = 0; x < width; x++)
            for (int y = 0; y < height; y++) {
                // Adding sines to get plasma value.
                int value = (int)
                        (
                                128.0 + (128.0 * Math.sin(x / period1))
                                        + 128.0 + (128.0 * Math.sin(y / period2))
                                        + 128.0 + (128.0 * Math.sin((x + y) / period1))
                                        + 128.0 + (128.0 * Math.sin(Math.sqrt((double) (x * x + y * y)) / period3))
                        ) / 4;
                plasma[x][y] = value;
            }
        for (int x = 0; x < width; x++)
            for (int y = 0; y < height; y++) {
                int color = palette[plasma[x][y] % 256];
                wallpaperBitmap.setPixel(x, y, color);
            }
        // TODO: Add noise as option in Settings.
        //addNoise(wallpaperBitmap);
        wallpaperBitmap = Bitmap.createScaledBitmap(wallpaperBitmap, wpManager.getDesiredMinimumHeight(), wpManager.getDesiredMinimumHeight(), true);
        setBitmapAsWallpaper(wpManager, wallpaperBitmap);
        // Cleanup.
        wallpaperBitmap.recycle();
    }

    // Set wallpaper to diagonal stripes pattern.
    protected void setStripesWallpaper(ArrayList<String> colors) {
        WallpaperManager wpManager = WallpaperManager.getInstance(this.getApplicationContext());
        // Use full screen size so wallpaper is movable.
        int smallHeight = wpManager.getDesiredMinimumHeight();
        int smallWidth = smallHeight;
        // Big height to account for rotation.
        int bigHeight = 2*smallHeight;
        int bigWidth = bigHeight;
        // Medium height for color distribution.
        int middleHeight = (int) ((2*smallHeight)/Math.sqrt(2));
        // Offset for cropping.
        int offset = (bigHeight - middleHeight) / 2;
        // Create square bitmap for wallpaper.
        Bitmap bigBitmap = Bitmap.createBitmap(bigWidth, bigHeight, Bitmap.Config.ARGB_8888);
        // Prepare colors.
        int[] colorsInt = new int[colors.size()];
        for (int i = 0; i < colors.size(); i++) {
            colorsInt[i] = Color.parseColor(colors.get(i));
        }
        Canvas c = new Canvas(bigBitmap);
        // Rotate canvas before drawing.
        c.save();
        c.rotate(-45, c.getWidth() / 2, c.getHeight() / 2);
        Paint paint = new Paint();
        float initStripeHeight = middleHeight / colors.size();
        float initShadowHeight = (float) (middleHeight * 0.012);
        int stripeSpread = (int) (initStripeHeight * 0.25);  // Vary stripe height a bit.
        float shadowSpread = initShadowHeight * 0.5f;  // Vary shadow thickness too.
        for (int i = colors.size() - 1; i >= 0; i--) {  // Going upwards.
            int stripeHeight;
            float shadowThickness;
            if (i == colors.size() - 1) {  // Fill whole canvas with last color.
                stripeHeight = bigHeight;
                shadowThickness = 0;
            } else {
                stripeHeight = Math.round((i + 1) * initStripeHeight);
                int dh = (int) (stripeSpread * Math.random() - stripeSpread / 2);
                stripeHeight = offset + stripeHeight + dh;
                if (stripeHeight < 0) stripeHeight = 0;
                if (stripeHeight > bigHeight) stripeHeight = bigHeight;
                float ds = (float) (shadowSpread * Math.random() - shadowSpread / 2);
                shadowThickness = Math.max(1, initShadowHeight + ds);
            }
            paint.setColor(colorsInt[i]);
            paint.setStyle(Paint.Style.FILL);
            paint.setShadowLayer(shadowThickness, 0.0f, 0.0f, 0xFF000000);
            c.drawRect(0, 0, bigWidth, stripeHeight, paint);
        }
        // Rotate canvas back.
        c.restore();
        //  Crop to screen size.
        int x = (c.getWidth() - smallWidth)/2;
        int y = (c.getHeight() - smallHeight)/2;
        Bitmap wallpaperBitmap = Bitmap.createBitmap(bigBitmap, x, y, smallWidth, smallHeight);
        // Add noise.
        //addNoise(wallpaperBitmap);
        setBitmapAsWallpaper(wpManager, wallpaperBitmap);
        // Cleanup.
        bigBitmap.recycle();
        wallpaperBitmap.recycle();
    }

    public void setBitmapAsWallpaper(WallpaperManager wpManager, Bitmap wallpaperBitmap) {
        try {
            // Set wallpaper.
            wpManager.setBitmap(wallpaperBitmap);
            // Write to temp file (to enable saving).
            File tempfile = new File(this.getFilesDir(), "lastwlp.png");
            FileOutputStream outStream = new FileOutputStream(tempfile);
            wallpaperBitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream);
            // Enable Save menu option if inactive.
            invalidateOptionsMenu();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Load favorites from app settings.
    protected void loadFavorites() {
        SharedPreferences settings = getSharedPreferences("WPGenPrefs", 0);
        int favsCount = settings.getInt("favorites", 0);
        for (int i = 0; i < favsCount; i++) {
            String fav = settings.getString("fav" + Integer.toString(i), "");
            if (!fav.isEmpty())
                favsList.add(fav);
        }
    }

    // Save favorites to app settings.
    protected void saveFavorites() {
        SharedPreferences settings = getSharedPreferences("WPGenPrefs", 0);
        int oldCount = settings.getInt("favorites", 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt("favorites", favsList.size());
        for (int i = 0; i < favsList.size(); i++) {
            editor.putString("fav" + Integer.toString(i), favsList.get(i));
        }
        // Clean up old favorites.
        for (int j = favsList.size(); j < oldCount; j++) {
            editor.remove("fav" + Integer.toString(j));
        }
        editor.apply();
    }

    // Put favorites and resource colors in one list.
    protected void updateColors() {
        allList.clear();
        for (String s : favsList) {
            allList.add(s);
        }
        Resources res = getResources();
        String[] allColors = res.getStringArray(R.array.colors_array);
        for (String sa : allColors) {
            if (!favsList.contains(sa)) {
                allList.add(sa);
            }
        }
    }

    private class MultiChoiceModeListener implements
            GridView.MultiChoiceModeListener {

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            if (selectedList.size() > 1) {
                menu.findItem(R.id.action_create_gradient).setVisible(true);
                menu.findItem(R.id.action_create_plasma).setVisible(true);
                menu.findItem(R.id.action_create_stripes).setVisible(true);
            } else {
                menu.findItem(R.id.action_create_gradient).setVisible(false);
                menu.findItem(R.id.action_create_plasma).setVisible(false);
                menu.findItem(R.id.action_create_stripes).setVisible(false);
            }
            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            AsyncTaskParams params;
            SetWallpaperTask asyncTask;
            String[] selectedArray = selectedList.toArray(new String[selectedList.size()]);
            switch (item.getItemId()) {
                case R.id.action_create_gradient:
                    //setGradientWallpaper(selectedList);
                    params = new AsyncTaskParams(selectedArray, SET_GRADIENT);
                    asyncTask = new SetWallpaperTask(ColorsActivity.this);
                    asyncTask.execute(params);
                    mode.finish();
                    return true;
                case R.id.action_create_plasma:
                    params = new AsyncTaskParams(selectedArray, SET_PLASMA);
                    asyncTask = new SetWallpaperTask(ColorsActivity.this);
                    asyncTask.execute(params);
                    mode.finish();
                    return true;
                case R.id.action_create_stripes:
                    params = new AsyncTaskParams(selectedArray, SET_STRIPES);
                    asyncTask = new SetWallpaperTask(ColorsActivity.this);
                    asyncTask.execute(params);
                    mode.finish();
                    return true;
                default:
                    return true;
            }
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // Inflate the menu for the CAB.
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.menu_context, menu);
            Resources res = getResources();
            mode.setTitle(res.getQuantityString(R.plurals.plurals_colors, 1, 1));
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            selectedList.clear();
        }

        @Override
        public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
            int selectCount = gridview.getCheckedItemCount();
            String color = allList.get(position);
            if (checked) {
                selectedList.add(color);
            } else {
                selectedList.remove(color);
            }
            Resources res = getResources();
            mode.setTitle(res.getQuantityString(R.plurals.plurals_colors, selectCount, selectCount));
            mode.invalidate(); // Invalidate to call onPrepareActionMode.
        }
    }

    private static class AsyncTaskParams {
        String[] colors;
        int operation;
        AsyncTaskParams(String[] colors, int operation) {
            this.colors = colors;
            this.operation = operation;
        }
    }

    private class SetWallpaperTask extends AsyncTask<AsyncTaskParams, Void, String> {
        private ProgressDialog progressDialog;
        Context context;

        public SetWallpaperTask(Context context) {
            this.context=context;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new ProgressDialog(context);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progressDialog.setIndeterminate(true);
            progressDialog.setMessage(getString(R.string.setting_wp));
            progressDialog.show();
        }

        @Override
        protected String doInBackground(AsyncTaskParams... params) {
            String result = null;
            String[] colors = params[0].colors;
            ArrayList<String> colorsList = new ArrayList(Arrays.asList(colors));
            int operation = params[0].operation;
            if (operation == SET_GRADIENT) {
                setGradientWallpaper(colorsList);
                result = getString(R.string.toast_wallpaper_set_to_gradient);
            }
            if (operation == SET_PLASMA) {
                setPlasmaWallpaper(colorsList);
                result = getString(R.string.toast_wallpaper_set_to_plasma);
            }
            if (operation == SET_STRIPES) {
                setStripesWallpaper(colorsList);
                result = getString(R.string.toast_wallpaper_set_to_stripes);
            }
            return result;
        }

        protected void onProgressUpdate() {
            //setProgressPercent(progress[0]);
        }

        protected void onPostExecute(String result) {
            progressDialog.dismiss();
            Toast toast = Toast.makeText(ColorsActivity.this, result, Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
        }
    }

    // Adapter for GridView.
    public class ImageAdapter extends BaseAdapter {
        public
        int squareSize, squarePadding;
        private LayoutInflater inflater;
        private Context context;

        public ImageAdapter(Context cntxt) {
            context = cntxt;
            inflater = LayoutInflater.from(cntxt);
        }

        public int getCount() {
            return allList.size();
        }

        public Object getItem(int position) {
            return null;
        }

        public long getItemId(int position) {
            return 0;
        }

        // Create a view for each item in adapter.
        @Override
        public View getView(final int position, View convertView, ViewGroup viewGroup) {
            ImageView imageView;
            View view = convertView;
            ImageButton button;
            ImageView image;
            TextView name;
            final boolean isFavorite;
            String color;
            if (convertView == null) {  // If not recycled, initialize some attributes.
                imageView = new ImageView(context);
                squareSize = gridview.getColumnWidth();
                squarePadding = (int) Math.round(squareSize * 0.05); // 5% thick frame.
                squareSize = squareSize - (squarePadding * 2);
                imageView.setLayoutParams(new GridView.LayoutParams(squareSize, squareSize));
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                imageView.setPadding(squarePadding, squarePadding, squarePadding, squarePadding);
                imageView.setCropToPadding(true);
                view = inflater.inflate(R.layout.grid_item, viewGroup, false);
                view.setTag(R.id.picture, view.findViewById(R.id.picture));
                view.setTag(R.id.text, view.findViewById(R.id.text));
                view.setTag(R.id.button, view.findViewById(R.id.button));
            }
            image = (ImageView) view.getTag(R.id.picture);
            name = (TextView) view.getTag(R.id.text);
            button = (ImageButton) view.getTag(R.id.button);
            color = allList.get(position);
            if (position < favsList.size()) {
                isFavorite = true;
                button.setImageResource(R.drawable.ic_action_toggle_star);
            } else {
                isFavorite = false;
                button.setImageResource(R.drawable.ic_action_toggle_star_outline);
            }

            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (selectedList.size() == 0) {
                        if (isFavorite) {
                            Toast.makeText(ColorsActivity.this, getString(R.string.toast_fav_removed), Toast.LENGTH_SHORT).show();
                            favsList.remove(allList.get(position));
                            saveFavorites();
                            updateColors();
                            gridview.invalidateViews();
                        } else {
                            Toast.makeText(ColorsActivity.this, getString(R.string.toast_fav_added), Toast.LENGTH_SHORT).show();
                            favsList.add(allList.get(position));
                            saveFavorites();
                            updateColors();
                            gridview.invalidateViews();
                        }
                    }
                }
            });
            image.setImageBitmap(createBitmap(color, squareSize));
            name.setText(color);
            return view;
        }
    }

    // Create bitmap of specified color and size.
    public Bitmap createBitmap(String color, int size) {
        int[] bitmapArray = new int[size * size];
        int colorValue = Color.parseColor(color);
        for (int i = 0; i < size * size; i++) {
            bitmapArray[i] = colorValue;
        }
        return Bitmap.createBitmap(bitmapArray, size, size, Bitmap.Config.ARGB_8888);
    }

    public Bitmap generateNoise() {
        Bitmap resultBitmap;
        String fileName = "noise.png";
        String filePath = this.getFilesDir().getAbsolutePath() + File.separator + fileName;
        File file = new File(filePath);
        if (file.exists()) {
            // File exists, use it.
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            resultBitmap = BitmapFactory.decodeFile(filePath, options);
        } else {
            // File does not exist, generate new one.
            int width = NOISE_BITMAP_SIZE;
            int height = NOISE_BITMAP_SIZE;
            int[] pixels = new int[width * height];
            Random random = new Random();
            for (int i = 0; i < width * height; i++) {
                // Fill with black pixels with random opacity ranging from 0 to NOISE_SPREAD.
                int noise = random.nextInt(NOISE_SPREAD);
                pixels[i] = Color.argb(noise, 0, 0, 0);
            }
            resultBitmap = Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888);
            // Save noise bitmap for future use.
            FileOutputStream out = null;
            try {
                out = new FileOutputStream(filePath);
                resultBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (out != null) {
                        out.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return resultBitmap;
    }

    public Bitmap addNoise(Bitmap source) {
        Canvas canvas = new Canvas(source);
        Bitmap noise = generateNoise();
        Shader noiseShader = new BitmapShader(noise, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
        Paint paint = new Paint();
        paint.setShader(noiseShader);
        canvas.drawRect(0, 0, source.getWidth(), source.getHeight(), paint);
        return source;
    }

    private void copyFile(File sourceFile, File destFile) throws IOException {
        if (!sourceFile.exists()) {
            return;
        }
        FileChannel source = null;
        FileChannel destination = null;
        source = new FileInputStream(sourceFile).getChannel();
        destination = new FileOutputStream(destFile).getChannel();
        if (destination != null && source != null) {
            destination.transferFrom(source, 0, source.size());
        }
        if (source != null) {
            source.close();
        }
        if (destination != null) {
            destination.close();
        }
    }

}
