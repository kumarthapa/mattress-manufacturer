package com.treewalker.rfidapp.util;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;

import com.google.android.material.snackbar.Snackbar;
import com.treewalker.rfidapp.R;

public class SnackbarHelper {

    private static int dpToPx(Activity activity, int dp) {
        return Math.round(activity.getResources().getDisplayMetrics().density * dp);
    }

    private static int getStatusBarHeight(Activity activity) {
        int result = 0;
        int resourceId = activity.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) result = activity.getResources().getDimensionPixelSize(resourceId);
        return result;
    }

    public static void showTopCenter(Activity activity, String message, boolean success) {
        View rootView = activity.findViewById(android.R.id.content);
        final Snackbar snackbar = Snackbar.make(rootView, message, Snackbar.LENGTH_LONG);
        View snackbarView = snackbar.getView();

        // Rounded background
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(dpToPx(activity, 12));
        bg.setColor(ContextCompat.getColor(activity,
                success ? R.color.snackbar_success : R.color.snackbar_error));
        snackbarView.setBackground(bg);

        // Elevation
        ViewCompat.setElevation(snackbarView, dpToPx(activity, 6));

        // Style text
        TextView textView = snackbarView.findViewById(com.google.android.material.R.id.snackbar_text);
        if (textView != null) {
            textView.setTextColor(Color.WHITE);
            textView.setMaxLines(3);
            textView.setText(message);
        }

        // Position to top-center
        ViewGroup.LayoutParams lp = snackbarView.getLayoutParams();
        int sideMargin = dpToPx(activity, 24);
        int topMargin = getStatusBarHeight(activity) + dpToPx(activity, 16);

        if (lp instanceof CoordinatorLayout.LayoutParams) {
            CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) lp;
            params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
            params.setMargins(sideMargin, topMargin, sideMargin, 0);
            snackbarView.setLayoutParams(params);
        } else if (lp instanceof FrameLayout.LayoutParams) {
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) lp;
            params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
            params.setMargins(sideMargin, topMargin, sideMargin, 0);
            snackbarView.setLayoutParams(params);
        }

        snackbar.show();
    }
}
