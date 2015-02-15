package net.glsk.wpgen;

import android.app.AlertDialog;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
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
import java.io.IOException;
import java.util.ArrayList;

public class ColorsActivity extends ActionBarActivity {

    public
    GridView gridview;
    ArrayList<String> favsList = new ArrayList<>();
    ArrayList<String> allList = new ArrayList<>();
    ArrayList<String> selectedList = new ArrayList<>();

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
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks.
        switch (item.getItemId()) {
            case R.id.action_settings: // Not implemented yet.
                return true;
            case R.id.action_add_color: // Show add color dialog.
                AlertDialog.Builder addColorDialog = new AlertDialog.Builder(this);
                addColorDialog.setTitle(R.string.dialog_add_color_title);
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
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // Set wallpaper to color.
    protected void setColorWallpaper(String color) {
        // Create small solid color bitmap.
        Bitmap bitmap = createBitmap(color, 512);
        WallpaperManager wpManager = WallpaperManager.getInstance(this.getApplicationContext());
        try {
            wpManager.setBitmap(bitmap);
            String text = getString(R.string.toast_wallpaper_set_to_color);
            Toast toast = Toast.makeText(this, text, Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Set wallpaper to blurred image.
    protected void setBlurWallpaper(ArrayList<String> colors) {
        WallpaperManager wpManager = WallpaperManager.getInstance(this.getApplicationContext());
        // Use half screen size to reduce memory usage.
        int height = (int) (wpManager.getDesiredMinimumHeight() / 2);
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
        try {
            // Set wallpaper.
            wpManager.setBitmap(wallpaperBitmap);
            String text = getString(R.string.toast_wallpaper_set_to_blur);
            Toast toast = Toast.makeText(this, text, Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // Cleanup.
        wallpaperBitmap.recycle();
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

    public class MultiChoiceModeListener implements
            GridView.MultiChoiceModeListener {

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            if (selectedList.size() > 1)
                menu.findItem(R.id.action_create_blur).setVisible(true);
            else
                menu.findItem(R.id.action_create_blur).setVisible(false);
            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.action_create_blur:
                    setBlurWallpaper(selectedList);
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
}
