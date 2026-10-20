package dev.ryuya.galaxyupdateguard;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.textview.MaterialTextView;

final class Ui {
  static int dp(Context context, int value) {
    return Math.round(value * context.getResources().getDisplayMetrics().density);
  }

  static LinearLayout column(Context context, int padding) {
    var view = new LinearLayout(context);
    view.setOrientation(LinearLayout.VERTICAL);
    int p = dp(context, padding);
    view.setPadding(p, p, p, p);
    return view;
  }

  static int color(View view, int token) {
    return MaterialColors.getColor(view, token);
  }

  static LinearLayout card(LinearLayout parent, int color) {
    Context context = parent.getContext();
    var card = new MaterialCardView(context);
    card.setRadius(dp(context, 28));
    card.setStrokeWidth(0);
    card.setCardBackgroundColor(color(card, color));
    var params = new LinearLayout.LayoutParams(-1, -2);
    params.bottomMargin = dp(context, 16);
    parent.addView(card, params);
    LinearLayout body = column(context, 24);
    card.addView(body);
    return body;
  }

  static MaterialTextView text(LinearLayout parent, CharSequence text, int style) {
    var label = new MaterialTextView(parent.getContext());
    label.setTextAppearance(style);
    label.setText(text);
    label.setLineSpacing(dp(parent.getContext(), 2), 1);
    var params = new LinearLayout.LayoutParams(-1, -2);
    params.topMargin = dp(parent.getContext(), 6);
    params.bottomMargin = dp(parent.getContext(), 6);
    parent.addView(label, params);
    return label;
  }
}
