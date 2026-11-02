package dev.lcdclock;

import android.app.ActivityOptions;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * External-display Application Launcher.
 * Lists installed launchable applications, renders icons and labels,
 * and launches activities using supported Android multi-display intents.
 */
public final class AppsView extends CoverPage {
    private static final String TAG = "AppsView";

    public record AppItem(String packageName, String activityName, String label, Drawable icon) {
    }

    private static List<AppItem> cachedApps;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF itemBounds = new RectF();

    public AppsView(Context context) {
        super(context);
        if (cachedApps == null) {
            loadApps(context);
        }
    }

    private synchronized void loadApps(Context context) {
        PackageManager pm = context.getPackageManager();
        Intent main = new Intent(Intent.ACTION_MAIN, null);
        main.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> resolved = pm.queryIntentActivities(main, 0);
        List<AppItem> list = new ArrayList<>();

        for (ResolveInfo ri : resolved) {
            if (ri.activityInfo == null || context.getPackageName().equals(ri.activityInfo.packageName)) {
                continue;
            }
            String pkg = ri.activityInfo.packageName;
            String act = ri.activityInfo.name;
            String label = ri.loadLabel(pm).toString();
            Drawable icon = ri.loadIcon(pm);
            list.add(new AppItem(pkg, act, label, icon));
        }

        list.sort((a, b) -> a.label().compareToIgnoreCase(b.label()));
        cachedApps = Collections.unmodifiableList(list);
    }

    @Override
    void drawPage(Canvas canvas, int color) {
        hits.clear();
        if (cachedApps == null || cachedApps.isEmpty()) {
            paint.setTextSize(bh * 0.09f);
            paint.setColor(DesignSystem.COLOR_TEXT_SECONDARY);
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("No applications found", bx + bw / 2f, by + bh / 2f, paint);
            paint.setTextAlign(Paint.Align.LEFT);
            return;
        }

        // 3 items visible per column, 2 columns per view = 6 items per page
        float contentTop = top + pad * 0.5f;
        float rowH = bh * 0.32f;
        int cols = 2;
        float colW = (bw - pad * 3f) / cols;

        int totalRows = (int) Math.ceil((double) cachedApps.size() / cols);
        maxScroll = Math.max(0, totalRows * rowH - (by + bh - contentTop));

        canvas.save();
        canvas.clipRect(bx, contentTop, bx + bw, by + bh);
        canvas.translate(0, -scroll);

        for (int i = 0; i < cachedApps.size(); i++) {
            int row = i / cols;
            int col = i % cols;

            float x = bx + pad + col * (colW + pad);
            float y = contentTop + row * rowH;

            itemBounds.set(x, y, x + colW, y + rowH - pad * 0.4f);
            hits.put(i, new RectF(itemBounds));

            int fill = pressed == i ? DesignSystem.COLOR_CARD_PRESSED : DesignSystem.COLOR_CARD;
            DesignSystem.drawCard(canvas, itemBounds.left, itemBounds.top, itemBounds.right, itemBounds.bottom,
                    DesignSystem.RADIUS_SMALL, fill, DesignSystem.COLOR_BORDER_SUBTLE);

            AppItem app = cachedApps.get(i);

            // Icon
            float iconSize = rowH * 0.55f;
            float iconX = x + pad * 0.6f;
            float iconY = y + (itemBounds.height() - iconSize) / 2f;
            if (app.icon() != null) {
                app.icon().setBounds((int) iconX, (int) iconY, (int) (iconX + iconSize), (int) (iconY + iconSize));
                app.icon().draw(canvas);
            }

            // App Label
            paint.setTextSize(bh * 0.075f);
            paint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            paint.setColor(DesignSystem.COLOR_TEXT_PRIMARY);

            float labelX = iconX + iconSize + pad * 0.5f;
            float labelMaxW = itemBounds.right - labelX - pad * 0.4f;
            String text = DesignSystem.ellipsize(app.label(), paint, labelMaxW);
            canvas.drawText(text, labelX, y + itemBounds.height() / 2f + bh * 0.025f, paint);
        }

        canvas.restore();
    }

    @Override
    protected boolean perform(int control) {
        if (cachedApps == null || control < 0 || control >= cachedApps.size()) {
            return false;
        }

        AppItem app = cachedApps.get(control);
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setClassName(app.packageName(), app.activityName());
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try {
            ActivityOptions opts = ActivityOptions.makeBasic();
            opts.setLaunchDisplayId(CoverScreen.DISPLAY_ID);
            getContext().startActivity(intent, opts.toBundle());
            return true;
        } catch (SecurityException | ActivityNotFoundException e) {
            Log.w(TAG, "Application could not launch on external display: " + app.packageName(), e);
            Toast.makeText(getContext(), app.label() + " requires opening the phone", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Failed to launch " + app.packageName(), e);
            Toast.makeText(getContext(), "Cannot open " + app.label(), Toast.LENGTH_SHORT).show();
        }
        return false;
    }
}
